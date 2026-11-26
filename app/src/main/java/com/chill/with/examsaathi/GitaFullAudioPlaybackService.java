package com.chill.with.examsaathi;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.media3.common.ForwardingPlayer;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaStyleNotificationHelper;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Foreground audio playback for full Gita — stays alive when app is in
 * background.
 */
public class GitaFullAudioPlaybackService extends Service {

    public static final String ACTION_TOGGLE = "com.chill.with.examsaathi.GITA_AUDIO_TOGGLE";
    public static final String ACTION_STOP = "com.chill.with.examsaathi.GITA_AUDIO_STOP";
    public static final String ACTION_PREVIOUS = "com.chill.with.examsaathi.GITA_AUDIO_PREVIOUS";
    public static final String ACTION_NEXT = "com.chill.with.examsaathi.GITA_AUDIO_NEXT";

    private static final String CHANNEL_ID = "gita_full_audio";
    private static final int NOTIFICATION_ID = 4101;
    private static final long NOTIFICATION_PROGRESS_INTERVAL_MS = 1000L;
    private static final long SAVE_PROGRESS_INTERVAL_MS = 2500L;
    private static final long RESTART_CURRENT_MS = 3000L;

    public static class PlaybackSnapshot {
        public boolean active;
        /** Chapter metadata loaded for home UI without starting playback. */
        public boolean preloaded;
        public boolean playing;
        public boolean loadingChapter;
        public boolean loadingNextChapter;
        /** ExoPlayer buffering while loading a track. */
        public boolean bufferingMedia;
        public String title = "";
        public String imageUrl = "";
        public String chapterLabel = "";
        public String positionLabel = "";
        public int chapterId;
        /** Display chapter number (1–18), not API id. */
        public int chapterNumber;
        public int shlokaInChapter;
        public int shlokasInChapter;
        public long positionMs;
        public long durationMs;
    }

    public interface UiListener {
        void onPlaybackUpdate(PlaybackSnapshot snapshot);

        void onPlaybackStopped();
    }

    public final class LocalBinder extends Binder {
        public GitaFullAudioPlaybackService getService() {
            return GitaFullAudioPlaybackService.this;
        }
    }

    private final IBinder binder = new LocalBinder();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<GitaChapterShlokas.Shloka> playlist = new ArrayList<>();
    private final List<GitaChapterShlokas.Shloka> currentChapterShlokas = new ArrayList<>();
    private final PlaybackSnapshot snapshot = new PlaybackSnapshot();

    private ExoPlayer exoPlayer;
    private MediaSession mediaSession;
    private UiListener uiListener;
    private int currentIndex = -1;
    private int currentChapterId;
    private int currentChapterNumber;
    private int chapterStartIndex;
    private int chapterShlokaCount;
    private String chapterTitle = "";
    private boolean loadingNextChapter;
    private boolean progressTickerRunning;
    private long lastNotificationUpdateElapsed;
    private long lastProgressSaveElapsed;
    private long pendingSeekMs;
    private boolean chapterPreloaded;
    private boolean preloadInFlight;
    private String loadedMediaUrl = "";

    @Nullable
    private Bitmap notificationArtwork;
    @Nullable
    private String loadedArtworkUrl;
    @Nullable
    private CustomTarget<Bitmap> artworkTarget;

    private final Runnable progressTicker = new Runnable() {
        @Override
        public void run() {
            updateProgressFromPlayer();
            dispatchUi();
            updateNotification(false);
            persistProgress(false);
            if (progressTickerRunning) {
                mainHandler.postDelayed(this, 400L);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        exoPlayer = FastPlaybackExoPlayer.create(this);
        ForwardingPlayer forwardingPlayer = new ForwardingPlayer(exoPlayer) {
            @Override
            public Player.Commands getAvailableCommands() {
                return super.getAvailableCommands().buildUpon()
                        .add(Player.COMMAND_SEEK_TO_NEXT)
                        .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                        .build();
            }

            @Override
            public boolean hasNextMediaItem() {
                return true;
            }

            @Override
            public boolean hasPreviousMediaItem() {
                return true;
            }

            @Override
            public void seekToNextMediaItem() {
                skipToNext();
            }

            @Override
            public void seekToPreviousMediaItem() {
                skipToPrevious();
            }
        };

        mediaSession = new MediaSession.Builder(this, forwardingPlayer)
                .setId("gita_full_audio_session")
                .setSessionActivity(buildContentPendingIntent())
                .build();
        exoPlayer.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_ENDED) {
                    onCurrentItemEnded();
                } else if (playbackState == Player.STATE_BUFFERING) {
                    if (snapshot.active) {
                        refreshMediaBufferingState();
                        dispatchUi();
                    }
                } else if (playbackState == Player.STATE_READY) {
                    clearTrackLoadingFlags();
                    applyPendingSeek();
                    persistProgress(true);
                    dispatchUi();
                    updateNotification(true);
                } else {
                    refreshMediaBufferingState();
                    dispatchUi();
                }
            }

            @Override
            public void onIsLoadingChanged(boolean isLoading) {
                if (!snapshot.active) {
                    return;
                }
                refreshMediaBufferingState();
                dispatchUi();
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                snapshot.playing = isPlaying;
                setProgressTicker(isPlaying);
                if (!isPlaying) {
                    persistProgress(true);
                }
                dispatchUi();
                updateNotification(true);
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                snapshot.loadingChapter = false;
                playNextOrLoadChapter();
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_TOGGLE.equals(action)) {
                togglePlayPause();
            } else if (ACTION_STOP.equals(action)) {
                stopPlaybackAndSelf();
            } else if (ACTION_PREVIOUS.equals(action)) {
                skipToPrevious();
            } else if (ACTION_NEXT.equals(action)) {
                skipToNext();
            }
        }
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        setProgressTicker(false);
        cancelArtworkLoad();
        if (mediaSession != null) {
            mediaSession.release();
            mediaSession = null;
        }
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
        super.onDestroy();
    }

    public void setUiListener(@Nullable UiListener listener) {
        uiListener = listener;
        if (listener != null && (snapshot.active || snapshot.preloaded)) {
            listener.onPlaybackUpdate(copySnapshot());
        }
    }

    public PlaybackSnapshot copySnapshot() {
        PlaybackSnapshot copy = new PlaybackSnapshot();
        copy.active = snapshot.active;
        copy.preloaded = snapshot.preloaded;
        copy.playing = snapshot.playing;
        copy.loadingChapter = snapshot.loadingChapter;
        copy.loadingNextChapter = snapshot.loadingNextChapter;
        copy.bufferingMedia = snapshot.bufferingMedia;
        copy.title = snapshot.title;
        copy.imageUrl = snapshot.imageUrl;
        copy.chapterLabel = snapshot.chapterLabel;
        copy.positionLabel = snapshot.positionLabel;
        copy.chapterId = snapshot.chapterId;
        copy.chapterNumber = snapshot.chapterNumber;
        copy.shlokaInChapter = snapshot.shlokaInChapter;
        copy.shlokasInChapter = snapshot.shlokasInChapter;
        copy.positionMs = snapshot.positionMs;
        copy.durationMs = snapshot.durationMs;
        return copy;
    }

    /**
     * Load chapter 1 (or saved chapter) for home UI without playing until user taps
     * Play.
     */
    public void preloadFirstChapter() {
        if (snapshot.active || chapterPreloaded || preloadInFlight) {
            return;
        }
        preloadInFlight = true;
        snapshot.loadingChapter = true;
        dispatchUiPreload();

        GitaFullAudioProgressStore.SavedProgress saved = GitaFullAudioProgressStore.load(this);
        if (saved != null && saved.apiChapterId > 0) {
            GitaFullAudioQueue.loadChapter(saved.apiChapterId,
                    new GitaFullAudioQueue.ChapterPlaylistCallback() {
                        @Override
                        public void onLoaded(GitaFullAudioQueue.ChapterPlaylist chapterPlaylist) {
                            finishPreload(chapterPlaylist, saved.shlokaInChapter, saved.positionMs);
                        }

                        @Override
                        public void onError() {
                            preloadFromChapterOne();
                        }
                    });
            return;
        }
        preloadFromChapterOne();
    }

    private void preloadFromChapterOne() {
        GitaFullAudioQueue.loadFirstChapter(new GitaFullAudioQueue.FirstChapterCallback() {
            @Override
            public void onLoaded(GitaFullAudioQueue.ChapterPlaylist chapterPlaylist) {
                finishPreload(chapterPlaylist, 1, 0L);
            }

            @Override
            public void onError() {
                preloadInFlight = false;
                snapshot.loadingChapter = false;
                dispatchUiPreload();
            }
        });
    }

    private void finishPreload(@Nullable GitaFullAudioQueue.ChapterPlaylist chapterPlaylist,
            int shlokaInChapterOneBased,
            long displayPositionMs) {
        preloadInFlight = false;
        if (chapterPlaylist == null || chapterPlaylist.shlokas == null
                || chapterPlaylist.shlokas.isEmpty()) {
            snapshot.loadingChapter = false;
            dispatchUiPreload();
            return;
        }
        setChapterPlaylist(chapterPlaylist, false);
        int shlokaIndex = Math.min(
                Math.max(1, shlokaInChapterOneBased),
                chapterPlaylist.shlokas.size());
        applyShlokaDisplay(chapterPlaylist.shlokas.get(shlokaIndex - 1), shlokaIndex);
        snapshot.positionMs = Math.max(0L, displayPositionMs);
        snapshot.durationMs = 0L;
        snapshot.active = false;
        snapshot.preloaded = true;
        snapshot.loadingChapter = false;
        snapshot.bufferingMedia = false;
        chapterPreloaded = true;
        dispatchUiPreload();
    }

    public void startFullGitaPlayback() {
        if (snapshot.active && currentIndex >= 0) {
            if (exoPlayer != null) {
                exoPlayer.play();
            }
            return;
        }
        if (chapterPreloaded && !playlist.isEmpty()) {
            snapshot.active = true;
            snapshot.preloaded = false;
            chapterPreloaded = false;
            startForeground(NOTIFICATION_ID, buildNotification());
            dispatchUi();
            GitaFullAudioProgressStore.SavedProgress saved = GitaFullAudioProgressStore.load(this);
            int playIndex = chapterStartIndex;
            pendingSeekMs = 0L;
            if (saved != null && saved.apiChapterId == currentChapterId) {
                int shlokaIdx = Math.min(
                        Math.max(1, saved.shlokaInChapter),
                        chapterShlokaCount);
                playIndex = chapterStartIndex + shlokaIdx - 1;
                pendingSeekMs = saved.positionMs;
            }
            playAtIndex(playIndex);
            return;
        }
        snapshot.active = true;
        snapshot.loadingChapter = true;
        snapshot.loadingNextChapter = false;
        playlist.clear();
        currentIndex = -1;
        currentChapterId = 0;
        chapterStartIndex = 0;
        chapterShlokaCount = 0;
        startForeground(NOTIFICATION_ID, buildNotification());
        dispatchUi();

        GitaFullAudioProgressStore.SavedProgress saved = GitaFullAudioProgressStore.load(this);
        if (saved != null) {
            resumeFromSavedProgress(saved);
            return;
        }
        startFromFirstChapter();
    }

    private void startFromFirstChapter() {
        GitaFullAudioQueue.loadFirstChapter(new GitaFullAudioQueue.FirstChapterCallback() {
            @Override
            public void onLoaded(GitaFullAudioQueue.ChapterPlaylist chapterPlaylist) {
                if (chapterPlaylist == null || chapterPlaylist.shlokas == null
                        || chapterPlaylist.shlokas.isEmpty()) {
                    stopPlaybackAndSelf();
                    return;
                }
                beginChapter(chapterPlaylist);
                pendingSeekMs = 0L;
                playAtIndex(chapterStartIndex);
            }

            @Override
            public void onError() {
                stopPlaybackAndSelf();
            }
        });
    }

    private void resumeFromSavedProgress(GitaFullAudioProgressStore.SavedProgress saved) {
        GitaFullAudioQueue.loadChapter(saved.apiChapterId,
                new GitaFullAudioQueue.ChapterPlaylistCallback() {
                    @Override
                    public void onLoaded(GitaFullAudioQueue.ChapterPlaylist chapterPlaylist) {
                        if (chapterPlaylist == null || chapterPlaylist.shlokas == null
                                || chapterPlaylist.shlokas.isEmpty()) {
                            startFromFirstChapter();
                            return;
                        }
                        setChapterPlaylist(chapterPlaylist, false);
                        int shlokaIndex = Math.min(
                                Math.max(1, saved.shlokaInChapter),
                                chapterPlaylist.shlokas.size());
                        pendingSeekMs = saved.positionMs;
                        playAtIndex(shlokaIndex - 1);
                    }

                    @Override
                    public void onError() {
                        startFromFirstChapter();
                    }
                });
    }

    public void togglePlayPause() {
        if (exoPlayer == null || !snapshot.active) {
            return;
        }
        if (exoPlayer.isPlaying()) {
            exoPlayer.pause();
        } else {
            exoPlayer.play();
        }
    }

    public List<GitaChapterShlokas.Shloka> getCurrentChapterShlokas() {
        return new ArrayList<>(currentChapterShlokas);
    }

    /** Jump to a chapter and play its first playable shloka. */
    public void playChapterFromStart(int apiChapterId, @Nullable Runnable onError) {
        if (exoPlayer == null) {
            if (onError != null) {
                onError.run();
            }
            return;
        }
        if (!snapshot.active) {
            snapshot.active = true;
            startForeground(NOTIFICATION_ID, buildNotification());
        }
        loadingNextChapter = false;
        snapshot.loadingNextChapter = false;
        snapshot.loadingChapter = true;
        dispatchUi();
        GitaFullAudioQueue.loadChapter(apiChapterId, new GitaFullAudioQueue.ChapterPlaylistCallback() {
            @Override
            public void onLoaded(GitaFullAudioQueue.ChapterPlaylist chapterPlaylist) {
                setChapterPlaylist(chapterPlaylist, false);
                pendingSeekMs = 0L;
                playAtIndex(0);
            }

            @Override
            public void onError() {
                snapshot.loadingChapter = false;
                dispatchUi();
                if (onError != null) {
                    onError.run();
                }
            }
        });
    }

    /** Skip to the next chapter (first shloka). */
    public void playNextChapter(@Nullable Runnable onNoMore) {
        if (!snapshot.active || currentChapterId <= 0 || loadingNextChapter) {
            if (onNoMore != null) {
                onNoMore.run();
            }
            return;
        }
        loadingNextChapter = true;
        snapshot.loadingNextChapter = true;
        dispatchUi();
        updateNotification(true);
        GitaFullAudioQueue.loadNextChapterAfter(currentChapterId,
                new GitaFullAudioQueue.NextChapterCallback() {
                    @Override
                    public void onLoaded(GitaFullAudioQueue.ChapterPlaylist chapterPlaylist) {
                        setChapterPlaylist(chapterPlaylist, false);
                        pendingSeekMs = 0L;
                        playAtIndex(0);
                    }

                    @Override
                    public void onNoMoreChapters() {
                        clearTrackLoadingFlags();
                        dispatchUi();
                        updateNotification(true);
                        if (onNoMore != null) {
                            onNoMore.run();
                        }
                    }
                });
    }

    /** Skip to the previous chapter (first shloka). */
    public void playPreviousChapter(@Nullable Runnable onNoPrevious) {
        if (!snapshot.active || currentChapterId <= 0) {
            if (onNoPrevious != null) {
                onNoPrevious.run();
            }
            return;
        }
        snapshot.loadingChapter = true;
        dispatchUi();
        GitaFullAudioQueue.loadPreviousChapterBefore(currentChapterId,
                new GitaFullAudioQueue.PreviousChapterCallback() {
                    @Override
                    public void onLoaded(GitaFullAudioQueue.ChapterPlaylist chapterPlaylist) {
                        setChapterPlaylist(chapterPlaylist, false);
                        pendingSeekMs = 0L;
                        playAtIndex(0);
                    }

                    @Override
                    public void onNoPreviousChapter() {
                        snapshot.loadingChapter = false;
                        dispatchUi();
                        if (onNoPrevious != null) {
                            onNoPrevious.run();
                        }
                    }
                });
    }

    /** Play a shloka in the current chapter (1-based index). */
    public void playShlokaInChapter(int shlokaIndexOneBased) {
        if (!snapshot.active || shlokaIndexOneBased < 1) {
            return;
        }
        int index = chapterStartIndex + shlokaIndexOneBased - 1;
        if (index >= chapterStartIndex && index < chapterStartIndex + chapterShlokaCount) {
            pendingSeekMs = 0L;
            playAtIndex(index);
        }
    }

    public void seekTo(long positionMs) {
        if (exoPlayer == null || !snapshot.active) {
            return;
        }
        exoPlayer.seekTo(Math.max(0L, positionMs));
        updateProgressFromPlayer();
        refreshMediaBufferingState();
        dispatchUi();
        updateNotification(true);
        persistProgress(true);
    }

    public void stopPlaybackAndSelf() {
        persistProgress(true);
        setProgressTicker(false);
        snapshot.active = false;
        snapshot.preloaded = false;
        snapshot.playing = false;
        clearTrackLoadingFlags();
        chapterPreloaded = false;
        preloadInFlight = false;
        playlist.clear();
        currentChapterShlokas.clear();
        currentIndex = -1;
        currentChapterNumber = 0;
        cancelArtworkLoad();
        notificationArtwork = null;
        loadedArtworkUrl = null;
        loadedMediaUrl = "";
        if (exoPlayer != null) {
            exoPlayer.stop();
            exoPlayer.clearMediaItems();
        }
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
        if (uiListener != null) {
            uiListener.onPlaybackStopped();
        }
    }

    private void skipToPrevious() {
        if (exoPlayer == null || !snapshot.active || currentIndex < 0) {
            return;
        }
        if (exoPlayer.getCurrentPosition() > RESTART_CURRENT_MS) {
            exoPlayer.seekTo(0L);
            updateProgressFromPlayer();
            dispatchUi();
            updateNotification(true);
            return;
        }
        if (currentIndex > 0) {
            playAtIndex(currentIndex - 1);
        } else {
            exoPlayer.seekTo(0L);
            updateNotification(true);
        }
    }

    private void skipToNext() {
        if (!snapshot.active || loadingNextChapter) {
            return;
        }
        playNextOrLoadChapter();
    }

    private void beginChapter(GitaFullAudioQueue.ChapterPlaylist chapterPlaylist) {
        setChapterPlaylist(chapterPlaylist, true);
    }

    private void setChapterPlaylist(GitaFullAudioQueue.ChapterPlaylist chapterPlaylist, boolean append) {
        if (chapterPlaylist == null || chapterPlaylist.shlokas == null) {
            return;
        }
        if (!append) {
            playlist.clear();
            chapterStartIndex = 0;
        } else {
            chapterStartIndex = playlist.size();
        }
        playlist.addAll(chapterPlaylist.shlokas);
        chapterShlokaCount = chapterPlaylist.shlokas.size();
        currentChapterId = chapterPlaylist.chapterId;
        currentChapterNumber = chapterPlaylist.chapterNumber > 0
                ? chapterPlaylist.chapterNumber
                : chapterPlaylist.chapterId;
        chapterTitle = chapterPlaylist.chapterTitle != null ? chapterPlaylist.chapterTitle : "";
        currentChapterShlokas.clear();
        currentChapterShlokas.addAll(chapterPlaylist.shlokas);
    }

    private void clearTrackLoadingFlags() {
        snapshot.loadingChapter = false;
        snapshot.loadingNextChapter = false;
        snapshot.bufferingMedia = false;
        loadingNextChapter = false;
    }

    private void refreshMediaBufferingState() {
        if (!snapshot.active || exoPlayer == null) {
            snapshot.bufferingMedia = false;
            return;
        }
        int state = exoPlayer.getPlaybackState();
        if (state == Player.STATE_READY) {
            snapshot.bufferingMedia = false;
        } else if (state == Player.STATE_BUFFERING) {
            snapshot.bufferingMedia = true;
        } else if (state == Player.STATE_IDLE && exoPlayer.isLoading()) {
            snapshot.bufferingMedia = true;
        }
    }

    private void playAtIndex(int index) {
        long startMs = pendingSeekMs;
        pendingSeekMs = 0L;
        playAtIndex(index, startMs);
    }

    private void playAtIndex(int index, long startPositionMs) {
        if (index < 0 || index >= playlist.size() || exoPlayer == null) {
            return;
        }
        currentIndex = index;
        GitaChapterShlokas.Shloka shloka = playlist.get(index);
        applyShlokaToSnapshot(shloka);
        String url = shloka.videoUrl != null ? shloka.videoUrl.trim() : "";
        if (url.isEmpty()) {
            playNextOrLoadChapter();
            return;
        }
        long seekMs = Math.max(0L, startPositionMs);
        if (url.equals(loadedMediaUrl) && exoPlayer.getMediaItemCount() > 0) {
            exoPlayer.seekTo(seekMs);
            exoPlayer.play();
            refreshMediaBufferingState();
            loadNotificationArtwork(snapshot.imageUrl);
            updateNotification(true);
            dispatchUi();
            persistProgress(true);
            return;
        }
        loadedMediaUrl = url;
        snapshot.bufferingMedia = true;
        MediaMetadata metadata = buildMediaMetadata(shloka);
        exoPlayer.setMediaItem(new MediaItem.Builder()
                .setUri(Uri.parse(url))
                .setMediaMetadata(metadata)
                .build());
        exoPlayer.prepare();
        if (seekMs > 0L) {
            exoPlayer.seekTo(seekMs);
        }
        exoPlayer.play();
        loadNotificationArtwork(snapshot.imageUrl);
        updateNotification(true);
        dispatchUi();
        persistProgress(true);
    }

    private void applyPendingSeek() {
        if (exoPlayer == null || pendingSeekMs <= 0L) {
            pendingSeekMs = 0L;
            return;
        }
        long duration = exoPlayer.getDuration();
        long seekTo = pendingSeekMs;
        if (duration > 0L) {
            seekTo = Math.min(pendingSeekMs, Math.max(0L, duration - 500L));
        }
        exoPlayer.seekTo(seekTo);
        pendingSeekMs = 0L;
        updateProgressFromPlayer();
        refreshMediaBufferingState();
    }

    private void persistProgress(boolean force) {
        if (!snapshot.active || currentIndex < 0 || currentChapterId <= 0) {
            return;
        }
        long now = SystemClock.elapsedRealtime();
        if (!force && now - lastProgressSaveElapsed < SAVE_PROGRESS_INTERVAL_MS) {
            return;
        }
        lastProgressSaveElapsed = now;
        updateProgressFromPlayer();
        GitaFullAudioProgressStore.save(
                this,
                currentChapterId,
                currentChapterNumber,
                snapshot.shlokaInChapter,
                snapshot.positionMs,
                snapshot.title);
    }

    private MediaMetadata buildMediaMetadata(GitaChapterShlokas.Shloka shloka) {
        String title = shloka.title != null ? shloka.title : "";
        String artist = snapshot.chapterLabel;
        if (snapshot.positionLabel != null && !snapshot.positionLabel.isEmpty()) {
            artist = artist + " · " + snapshot.positionLabel;
        }
        MediaMetadata.Builder builder = new MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(chapterTitle != null ? chapterTitle : "");
        String image = shloka.image != null ? shloka.image.trim() : "";
        if (!image.isEmpty()) {
            builder.setArtworkUri(Uri.parse(image));
        }
        return builder.build();
    }

    private void applyShlokaToSnapshot(GitaChapterShlokas.Shloka shloka) {
        int shlokaInChapter = Math.max(1, currentIndex - chapterStartIndex + 1);
        applyShlokaDisplay(shloka, shlokaInChapter);
        snapshot.positionMs = 0L;
        snapshot.durationMs = 0L;
    }

    private void applyShlokaDisplay(GitaChapterShlokas.Shloka shloka, int shlokaInChapterOneBased) {
        snapshot.title = shloka.title != null ? shloka.title : "";
        snapshot.imageUrl = shloka.image != null ? shloka.image : "";
        snapshot.chapterId = currentChapterId;
        snapshot.chapterNumber = currentChapterNumber;
        snapshot.shlokasInChapter = Math.max(chapterShlokaCount, 1);
        snapshot.shlokaInChapter = Math.max(1, shlokaInChapterOneBased);
        snapshot.chapterLabel = formatChapterLabel(currentChapterNumber);
        snapshot.positionLabel = formatPositionLabel(currentChapterNumber, snapshot.shlokaInChapter);
    }

    private void dispatchUiPreload() {
        if (uiListener != null) {
            uiListener.onPlaybackUpdate(copySnapshot());
        }
    }

    private void onCurrentItemEnded() {
        if (currentIndex + 1 < playlist.size()) {
            playAtIndex(currentIndex + 1);
        } else {
            loadNextGitaChapter();
        }
    }

    private void playNextOrLoadChapter() {
        if (currentIndex + 1 < playlist.size()) {
            playAtIndex(currentIndex + 1);
        } else {
            loadNextGitaChapter();
        }
    }

    private void loadNextGitaChapter() {
        if (loadingNextChapter || currentChapterId <= 0) {
            stopPlaybackAndSelf();
            return;
        }
        loadingNextChapter = true;
        snapshot.loadingNextChapter = true;
        dispatchUi();
        updateNotification(true);
        GitaFullAudioQueue.loadNextChapterAfter(currentChapterId,
                new GitaFullAudioQueue.NextChapterCallback() {
                    @Override
                    public void onLoaded(GitaFullAudioQueue.ChapterPlaylist chapterPlaylist) {
                        if (chapterPlaylist == null || chapterPlaylist.shlokas == null
                                || chapterPlaylist.shlokas.isEmpty()) {
                            clearTrackLoadingFlags();
                            if (exoPlayer != null) {
                                exoPlayer.pause();
                            }
                            snapshot.playing = false;
                            dispatchUi();
                            updateNotification(true);
                            return;
                        }
                        beginChapter(chapterPlaylist);
                        playAtIndex(chapterStartIndex);
                    }

                    @Override
                    public void onNoMoreChapters() {
                        clearTrackLoadingFlags();
                        if (exoPlayer != null) {
                            exoPlayer.pause();
                        }
                        snapshot.playing = false;
                        dispatchUi();
                        updateNotification(true);
                    }
                });
    }

    private void updateProgressFromPlayer() {
        if (exoPlayer == null) {
            return;
        }
        snapshot.positionMs = Math.max(0L, exoPlayer.getCurrentPosition());
        long duration = exoPlayer.getDuration();
        snapshot.durationMs = duration > 0 ? duration : 0L;
    }

    private void setProgressTicker(boolean run) {
        if (run == progressTickerRunning) {
            return;
        }
        progressTickerRunning = run;
        mainHandler.removeCallbacks(progressTicker);
        if (run) {
            mainHandler.post(progressTicker);
        }
    }

    private void dispatchUi() {
        if (snapshot.active) {
            updateProgressFromPlayer();
        }
        if (uiListener != null) {
            uiListener.onPlaybackUpdate(copySnapshot());
        }
    }

    private static String formatChapterLabel(int chapterNumber) {
        if (chapterNumber > 0) {
            return "Chapter " + chapterNumber;
        }
        return "Chapter";
    }

    private static String formatPositionLabel(int chapterNumber, int shlokaInChapter) {
        if (chapterNumber > 0 && shlokaInChapter > 0) {
            return chapterNumber + "." + shlokaInChapter;
        }
        return "";
    }

    public static String formatTime(long ms) {
        if (ms <= 0L) {
            return "0:00";
        }
        long totalSeconds = ms / 1000L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return String.format(Locale.US, "%d:%02d", minutes, seconds);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                getString(R.string.gita_audio_notification_channel),
                NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription(getString(R.string.gita_audio_notification_channel_desc));
        channel.setShowBadge(false);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    private PendingIntent buildContentPendingIntent() {
        Intent openIntent = new Intent(this, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(
                this, 0, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private PendingIntent buildServiceActionPendingIntent(String action, int requestCode) {
        Intent intent = new Intent(this, GitaFullAudioPlaybackService.class);
        intent.setAction(action);
        return PendingIntent.getService(
                this, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    @UnstableApi
    private Notification buildNotification() {
        String title = snapshot.title != null && !snapshot.title.isEmpty()
                ? snapshot.title
                : getString(R.string.home_full_gita_audio_title);
        String subtitle = snapshot.chapterLabel != null ? snapshot.chapterLabel : "";
        if (snapshot.positionLabel != null && !snapshot.positionLabel.isEmpty()) {
            subtitle = subtitle + " · " + snapshot.positionLabel;
        }
        String statusLine = chapterTitle != null && !chapterTitle.isEmpty()
                ? chapterTitle
                : getString(R.string.full_gita_audio_chapter_title);
        if (snapshot.loadingNextChapter) {
            statusLine = getString(R.string.full_gita_audio_loading_next_chapter);
        } else if (snapshot.loadingChapter) {
            statusLine = getString(R.string.home_full_gita_audio_loading);
        }

        NotificationCompat.Action previousAction = new NotificationCompat.Action(
                android.R.drawable.ic_media_previous,
                getString(R.string.gita_audio_notification_previous),
                buildServiceActionPendingIntent(ACTION_PREVIOUS, 10));

        int playPauseIcon = snapshot.playing
                ? android.R.drawable.ic_media_pause
                : android.R.drawable.ic_media_play;
        String playPauseLabel = getString(snapshot.playing
                ? R.string.gita_audio_notification_pause
                : R.string.gita_audio_notification_play);
        NotificationCompat.Action playPauseAction = new NotificationCompat.Action(
                playPauseIcon,
                playPauseLabel,
                buildServiceActionPendingIntent(ACTION_TOGGLE, 11));

        NotificationCompat.Action nextAction = new NotificationCompat.Action(
                android.R.drawable.ic_media_next,
                getString(R.string.gita_audio_notification_next),
                buildServiceActionPendingIntent(ACTION_NEXT, 12));

        NotificationCompat.Action stopAction = new NotificationCompat.Action(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.gita_audio_notification_stop),
                buildServiceActionPendingIntent(ACTION_STOP, 13));

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_audio)
                .setContentTitle(title)
                .setContentText(subtitle)
                .setSubText(statusLine)
                .setContentIntent(buildContentPendingIntent())
                .setLargeIcon(notificationArtwork)
                .setOnlyAlertOnce(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                .setOngoing(snapshot.active)
                .setShowWhen(false)
                .addAction(previousAction)
                .addAction(playPauseAction)
                .addAction(nextAction)
                .addAction(stopAction)
                .setStyle(new MediaStyleNotificationHelper.MediaStyle(mediaSession)
                        .setShowActionsInCompactView(0, 1, 2));

        return builder.build();
    }

    private void updateNotification(boolean force) {
        if (!snapshot.active) {
            return;
        }
        long now = SystemClock.elapsedRealtime();
        if (!force && now - lastNotificationUpdateElapsed < NOTIFICATION_PROGRESS_INTERVAL_MS) {
            return;
        }
        lastNotificationUpdateElapsed = now;
        Notification notification = buildNotification();
        startForeground(NOTIFICATION_ID, notification);
    }

    private void loadNotificationArtwork(@Nullable String imageUrl) {
        String url = imageUrl != null ? imageUrl.trim() : "";
        if (url.isEmpty()) {
            cancelArtworkLoad();
            notificationArtwork = null;
            loadedArtworkUrl = null;
            return;
        }
        if (url.equals(loadedArtworkUrl)) {
            return;
        }
        loadedArtworkUrl = url;
        cancelArtworkLoad();
        artworkTarget = new CustomTarget<Bitmap>() {
            @Override
            public void onResourceReady(@NonNull Bitmap resource,
                    @Nullable Transition<? super Bitmap> transition) {
                notificationArtwork = resource;
                updateNotification(true);
            }

            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {
            }
        };
        Glide.with(getApplicationContext())
                .asBitmap()
                .load(url)
                .centerCrop()
                .into(artworkTarget);
    }

    private void cancelArtworkLoad() {
        if (artworkTarget != null) {
            Glide.with(getApplicationContext()).clear(artworkTarget);
            artworkTarget = null;
        }
    }
}

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

/** Foreground shloka audio for {@link GitaAudioPlayerActivity} — survives app background. */
public class GitaShlokaAudioPlaybackService extends Service {

    public static final String ACTION_START = "com.chill.with.examsaathi.SHLoka_AUDIO_START";
    public static final String ACTION_TOGGLE = "com.chill.with.examsaathi.SHLoka_AUDIO_TOGGLE";
    public static final String ACTION_STOP = "com.chill.with.examsaathi.SHLoka_AUDIO_STOP";
    public static final String ACTION_PREVIOUS = "com.chill.with.examsaathi.SHLoka_AUDIO_PREVIOUS";
    public static final String ACTION_NEXT = "com.chill.with.examsaathi.SHLoka_AUDIO_NEXT";

    public static final String EXTRA_PLAYLIST_JSON = "playlist_json";
    public static final String EXTRA_CHAPTER_TITLE = "chapter_title";
    public static final String EXTRA_START_INDEX = "start_index";
    public static final String EXTRA_POSTER_URL = "poster_url";

    private static final String CHANNEL_ID = "gita_shloka_audio";
    private static final int NOTIFICATION_ID = 4102;
    private static final long RESTART_CURRENT_MS = 3000L;

    public static class PlaybackSnapshot {
        public boolean active;
        public boolean playing;
        public boolean bufferingMedia;
        public String title = "";
        public String imageUrl = "";
        public String chapterLabel = "";
        public String positionLabel = "";
        public int chapterNumber;
        public int shlokaInChapter;
        public int shlokasInChapter;
        public long positionMs;
        public long durationMs;
        public int currentIndex = -1;
    }

    public interface UiListener {
        void onPlaybackUpdate(PlaybackSnapshot snapshot);

        void onPlaybackStopped();
    }

    public final class LocalBinder extends Binder {
        public GitaShlokaAudioPlaybackService getService() {
            return GitaShlokaAudioPlaybackService.this;
        }
    }

    private final IBinder binder = new LocalBinder();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<GitaChapterShlokas.Shloka> playlist = new ArrayList<>();
    private final PlaybackSnapshot snapshot = new PlaybackSnapshot();

    private ExoPlayer exoPlayer;
    private MediaSession mediaSession;
    @Nullable
    private UiListener uiListener;
    private int currentIndex = -1;
    private int chapterNumber;
    private String chapterTitle = "";
    private String loadedMediaUrl = "";
    private boolean progressTickerRunning;
    private long lastNotificationUpdateElapsed;

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
        mediaSession = new MediaSession.Builder(this, exoPlayer)
                .setId("gita_shloka_audio_session")
                .setSessionActivity(buildContentPendingIntent())
                .build();
        exoPlayer.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_ENDED) {
                    onTrackEnded();
                } else if (playbackState == Player.STATE_READY) {
                    refreshMediaBufferingState();
                    dispatchUi();
                    updateNotification(true);
                } else {
                    refreshMediaBufferingState();
                    dispatchUi();
                }
            }

            @Override
            public void onIsLoadingChanged(boolean isLoading) {
                refreshMediaBufferingState();
                dispatchUi();
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                snapshot.playing = isPlaying;
                setProgressTicker(isPlaying);
                dispatchUi();
                updateNotification(true);
            }

            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
                playNextAuto();
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_START.equals(action)) {
                handleStart(intent);
            } else if (ACTION_TOGGLE.equals(action)) {
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

    private void handleStart(Intent intent) {
        String json = intent.getStringExtra(EXTRA_PLAYLIST_JSON);
        List<GitaChapterShlokas.Shloka> items = GitaPlayerPlaylist.parsePlayable(json);
        if (items.isEmpty()) {
            stopPlaybackAndSelf();
            return;
        }
        chapterTitle = intent.getStringExtra(EXTRA_CHAPTER_TITLE) != null
                ? intent.getStringExtra(EXTRA_CHAPTER_TITLE) : "";
        int startIndex = intent.getIntExtra(EXTRA_START_INDEX, 0);
        String poster = intent.getStringExtra(EXTRA_POSTER_URL);
        setPlaylist(items, chapterTitle, Math.max(0, startIndex), poster);
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
        if (listener != null && snapshot.active) {
            listener.onPlaybackUpdate(copySnapshot());
        }
    }

    public PlaybackSnapshot copySnapshot() {
        PlaybackSnapshot copy = new PlaybackSnapshot();
        copy.active = snapshot.active;
        copy.playing = snapshot.playing;
        copy.bufferingMedia = snapshot.bufferingMedia;
        copy.title = snapshot.title;
        copy.imageUrl = snapshot.imageUrl;
        copy.chapterLabel = snapshot.chapterLabel;
        copy.positionLabel = snapshot.positionLabel;
        copy.chapterNumber = snapshot.chapterNumber;
        copy.shlokaInChapter = snapshot.shlokaInChapter;
        copy.shlokasInChapter = snapshot.shlokasInChapter;
        copy.positionMs = snapshot.positionMs;
        copy.durationMs = snapshot.durationMs;
        copy.currentIndex = currentIndex;
        return copy;
    }

    public void setPlaylist(List<GitaChapterShlokas.Shloka> items, String title, int startIndex,
                            @Nullable String fallbackPosterUrl) {
        playlist.clear();
        if (items != null) {
            playlist.addAll(items);
        }
        chapterTitle = title != null ? title : "";
        snapshot.active = !playlist.isEmpty();
        if (!snapshot.active) {
            stopPlaybackAndSelf();
            return;
        }
        startForeground(NOTIFICATION_ID, buildNotification());
        if (startIndex < 0 || startIndex >= playlist.size()) {
            startIndex = 0;
        }
        if (fallbackPosterUrl != null && !fallbackPosterUrl.trim().isEmpty()) {
            snapshot.imageUrl = fallbackPosterUrl.trim();
        }
        playAtIndex(startIndex);
    }

    public void playAtIndex(int index) {
        playAtIndex(index, 0L);
    }

    public void playAtIndex(int index, long startPositionMs) {
        if (exoPlayer == null || index < 0 || index >= playlist.size()) {
            return;
        }
        currentIndex = index;
        GitaChapterShlokas.Shloka shloka = playlist.get(index);
        applyShlokaToSnapshot(shloka, index);
        String url = shloka.videoUrl != null ? shloka.videoUrl.trim() : "";
        if (url.isEmpty()) {
            playNextAuto();
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
            return;
        }
        loadedMediaUrl = url;
        snapshot.bufferingMedia = true;
        exoPlayer.setMediaItem(new MediaItem.Builder()
                .setUri(Uri.parse(url))
                .setMediaMetadata(buildMediaMetadata(shloka))
                .build());
        exoPlayer.prepare();
        if (seekMs > 0L) {
            exoPlayer.seekTo(seekMs);
        }
        exoPlayer.play();
        loadNotificationArtwork(snapshot.imageUrl);
        updateNotification(true);
        dispatchUi();
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

    public void seekTo(long positionMs) {
        if (exoPlayer == null || !snapshot.active) {
            return;
        }
        exoPlayer.seekTo(Math.max(0L, positionMs));
        updateProgressFromPlayer();
        refreshMediaBufferingState();
        dispatchUi();
        updateNotification(true);
    }

    public void skipToPreviousShloka() {
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

    public void skipToNextShloka() {
        if (!snapshot.active || currentIndex < 0) {
            return;
        }
        if (currentIndex < playlist.size() - 1) {
            playAtIndex(currentIndex + 1);
        }
    }

    public void stopPlaybackAndSelf() {
        setProgressTicker(false);
        snapshot.active = false;
        snapshot.playing = false;
        snapshot.bufferingMedia = false;
        playlist.clear();
        currentIndex = -1;
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

    private void onTrackEnded() {
        if (currentIndex < playlist.size() - 1) {
            playAtIndex(currentIndex + 1);
        } else {
            snapshot.playing = false;
            dispatchUi();
            updateNotification(true);
        }
    }

    private void playNextAuto() {
        if (currentIndex < playlist.size() - 1) {
            playAtIndex(currentIndex + 1);
        }
    }

    private void skipToPrevious() {
        skipToPreviousShloka();
    }

    private void skipToNext() {
        skipToNextShloka();
    }

    private void applyShlokaToSnapshot(GitaChapterShlokas.Shloka shloka, int index) {
        snapshot.title = shloka.title != null ? shloka.title : "";
        if (shloka.image != null && !shloka.image.trim().isEmpty()) {
            snapshot.imageUrl = shloka.image.trim();
        }
        chapterNumber = parseChapterFromTitle(snapshot.title);
        if (chapterNumber <= 0) {
            chapterNumber = parseChapterFromTitle(chapterTitle);
        }
        int shlokaNum = parseShlokaFromTitle(snapshot.title);
        if (shlokaNum <= 0 && shloka.sequence > 0) {
            shlokaNum = shloka.sequence;
        }
        if (shlokaNum <= 0) {
            shlokaNum = index + 1;
        }
        snapshot.chapterNumber = chapterNumber > 0 ? chapterNumber : 1;
        snapshot.shlokaInChapter = shlokaNum;
        snapshot.shlokasInChapter = playlist.size();
        snapshot.chapterLabel = getString(R.string.chapter_number_label, snapshot.chapterNumber);
        snapshot.positionLabel = snapshot.chapterNumber + "." + shlokaNum;
        snapshot.currentIndex = index;
    }

    private static int parseChapterFromTitle(@Nullable String title) {
        if (title == null) {
            return 0;
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("(\\d+)\\.(\\d+)").matcher(title.trim());
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private static int parseShlokaFromTitle(@Nullable String title) {
        if (title == null) {
            return 0;
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("(\\d+)\\.(\\d+)").matcher(title.trim());
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(2));
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private MediaMetadata buildMediaMetadata(GitaChapterShlokas.Shloka shloka) {
        String title = shloka.title != null ? shloka.title : "";
        return new MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(chapterTitle != null && !chapterTitle.isEmpty()
                        ? chapterTitle
                        : getString(R.string.gita_audio_player_title))
                .build();
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
                getString(R.string.gita_shloka_audio_notification_channel),
                NotificationManager.IMPORTANCE_LOW);
        channel.setDescription(getString(R.string.gita_shloka_audio_notification_channel_desc));
        channel.setShowBadge(false);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    private PendingIntent buildContentPendingIntent() {
        Intent openIntent = new Intent(this, GitaAudioPlayerActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(
                this, 1, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private PendingIntent buildServiceActionPendingIntent(String action, int requestCode) {
        Intent intent = new Intent(this, GitaShlokaAudioPlaybackService.class);
        intent.setAction(action);
        return PendingIntent.getService(
                this, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    @UnstableApi
    private Notification buildNotification() {
        String title = snapshot.title != null && !snapshot.title.isEmpty()
                ? snapshot.title
                : getString(R.string.gita_audio_player_title);
        String subtitle = snapshot.chapterLabel != null ? snapshot.chapterLabel : "";
        if (snapshot.positionLabel != null && !snapshot.positionLabel.isEmpty()) {
            subtitle = subtitle + " · " + snapshot.positionLabel;
        }
        NotificationCompat.Action previousAction = new NotificationCompat.Action(
                android.R.drawable.ic_media_previous,
                getString(R.string.gita_audio_notification_previous),
                buildServiceActionPendingIntent(ACTION_PREVIOUS, 20));

        int playPauseIcon = snapshot.playing
                ? android.R.drawable.ic_media_pause
                : android.R.drawable.ic_media_play;
        String playPauseLabel = getString(snapshot.playing
                ? R.string.gita_audio_notification_pause
                : R.string.gita_audio_notification_play);
        NotificationCompat.Action playPauseAction = new NotificationCompat.Action(
                playPauseIcon,
                playPauseLabel,
                buildServiceActionPendingIntent(ACTION_TOGGLE, 21));

        NotificationCompat.Action nextAction = new NotificationCompat.Action(
                android.R.drawable.ic_media_next,
                getString(R.string.gita_audio_notification_next),
                buildServiceActionPendingIntent(ACTION_NEXT, 22));

        NotificationCompat.Action stopAction = new NotificationCompat.Action(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.gita_audio_notification_stop),
                buildServiceActionPendingIntent(ACTION_STOP, 23));

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_audio)
                .setContentTitle(title)
                .setContentText(subtitle)
                .setSubText(chapterTitle != null && !chapterTitle.isEmpty()
                        ? chapterTitle
                        : getString(R.string.gita_audio_player_title))
                .setLargeIcon(notificationArtwork)
                .setContentIntent(buildContentPendingIntent())
                .setOngoing(snapshot.playing)
                .setOnlyAlertOnce(true)
                .addAction(previousAction)
                .addAction(playPauseAction)
                .addAction(nextAction)
                .addAction(stopAction)
                .setStyle(new MediaStyleNotificationHelper.MediaStyle(mediaSession)
                        .setShowActionsInCompactView(0, 1, 2))
                .build();
    }

    private void updateNotification(boolean force) {
        if (!snapshot.active) {
            return;
        }
        long now = SystemClock.elapsedRealtime();
        if (!force && now - lastNotificationUpdateElapsed < 1000L) {
            return;
        }
        lastNotificationUpdateElapsed = now;
        Notification notification = buildNotification();
        startForeground(NOTIFICATION_ID, notification);
    }

    private void loadNotificationArtwork(@Nullable String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            notificationArtwork = null;
            return;
        }
        String url = imageUrl.trim();
        if (url.equals(loadedArtworkUrl) && notificationArtwork != null) {
            return;
        }
        cancelArtworkLoad();
        loadedArtworkUrl = url;
        artworkTarget = Glide.with(getApplicationContext())
                .asBitmap()
                .load(url)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource,
                                               @Nullable Transition<? super Bitmap> transition) {
                        notificationArtwork = resource;
                        updateNotification(true);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                    }
                });
    }

    private void cancelArtworkLoad() {
        if (artworkTarget != null) {
            Glide.with(getApplicationContext()).clear(artworkTarget);
            artworkTarget = null;
        }
    }
}

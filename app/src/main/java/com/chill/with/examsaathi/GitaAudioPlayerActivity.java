package com.chill.with.examsaathi;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dedicated shloka audio player opened from Movieview (and similar screens).
 */
public class GitaAudioPlayerActivity extends AppCompatActivity {

    public static final String EXTRA_CHAPTER_SHLOKAS_JSON = MovieviewActivity.EXTRA_CHAPTER_SHLOKAS_JSON;
    public static final String EXTRA_CHAPTER_TITLE = MovieviewActivity.EXTRA_CHAPTER_TITLE;
    public static final String EXTRA_CHAPTER_ID = MovieviewActivity.EXTRA_CHAPTER_ID;

    /**
     * Matches chapter.shloka anywhere in title, e.g. "11.8" or "Shloka 11.8 — …".
     */
    private static final Pattern POSITION_IN_TITLE = Pattern.compile("(\\d+)\\.(\\d+)");

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<GitaChapterShlokas.Shloka> chapterShlokasAll = new ArrayList<>();
    private final List<GitaChapterShlokas.Shloka> playlist = new ArrayList<>();

    private GitaShlokaAudioPlaybackService audioService;
    private boolean audioServiceBound;
    private boolean stopPlaybackOnExit;
    private int currentIndex = -1;
    private int chapterNumber;
    private int currentChapterApiId;
    private String chapterTitle = "";
    private boolean userSeeking;
    private boolean playOverlayLoadingVisible;
    private boolean switchingChapter;

    private com.makeramen.roundedimageview.RoundedImageView imgPoster;
    private TextView txtChapterBadge;
    private TextView txtPositionBadge;
    private TextView txtNowPlaying;
    private TextView txtChapterLine;
    private TextView txtElapsed;
    private TextView txtDuration;
    private SeekBar seekBar;
    private ImageButton btnPlayPause;
    private ImageButton btnPrev;
    private ImageButton btnNext;
    private ProgressBar progressBuffering;

    private final ServiceConnection audioServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            GitaShlokaAudioPlaybackService.LocalBinder binder = (GitaShlokaAudioPlaybackService.LocalBinder) service;
            audioService = binder.getService();
            audioServiceBound = true;
            audioService.setUiListener(playbackUiListener);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            audioService = null;
            audioServiceBound = false;
        }
    };

    private final GitaShlokaAudioPlaybackService.UiListener playbackUiListener = new GitaShlokaAudioPlaybackService.UiListener() {
        @Override
        public void onPlaybackUpdate(GitaShlokaAudioPlaybackService.PlaybackSnapshot snapshot) {
            mainHandler.post(() -> applyPlaybackSnapshot(snapshot));
        }

        @Override
        public void onPlaybackStopped() {
            mainHandler.post(() -> {
                if (!stopPlaybackOnExit && !isFinishing()) {
                    finish();
                }
            });
        }
    };

    public static Intent newIntent(Context context, @Nullable Intent source) {
        Intent intent = new Intent(context, GitaAudioPlayerActivity.class);
        if (source != null) {
            intent.putExtra(EXTRA_CHAPTER_SHLOKAS_JSON,
                    source.getStringExtra(EXTRA_CHAPTER_SHLOKAS_JSON));
            intent.putExtra(EXTRA_CHAPTER_TITLE,
                    source.getStringExtra(EXTRA_CHAPTER_TITLE));
            intent.putExtra(EXTRA_CHAPTER_ID, source.getIntExtra(EXTRA_CHAPTER_ID, 0));
            intent.putExtra("videoid", source.getStringExtra("videoid"));
            intent.putExtra("title", source.getStringExtra("title"));
            intent.putExtra("posterUrl", source.getStringExtra("posterUrl"));
            intent.putExtra("description", source.getStringExtra("description"));
            intent.putExtra("videoUrl", source.getStringExtra("videoUrl"));
        }
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gita_audio_player);

        bindViews();
        stopHomeFullGitaAudio();
        findViewById(R.id.btnAudioPlayerBack).setOnClickListener(v -> exitAndStopPlayback());
        wireControls();

        String extraChapterTitle = getIntent().getStringExtra(EXTRA_CHAPTER_TITLE);
        chapterTitle = extraChapterTitle != null ? extraChapterTitle : "";

        loadPlaylistFromIntent();
        if (needsChapterLoadFromApi()) {
            loadChapterThenStartPlayback();
            return;
        }
        if (playlist.isEmpty()) {
            AppToast.show(this, R.string.home_full_gita_audio_unavailable);
            finish();
            return;
        }
        startPlaybackAtCurrentShloka();
    }

    private boolean needsChapterLoadFromApi() {
        String json = getIntent().getStringExtra(EXTRA_CHAPTER_SHLOKAS_JSON);
        boolean noChapterList = json == null || json.isEmpty();
        String videoUrl = getIntent().getStringExtra("videoUrl");
        boolean hasVideo = videoUrl != null && !videoUrl.trim().isEmpty();

        int apiChapterId = getIntent().getIntExtra(EXTRA_CHAPTER_ID, 0);
        int parsedNumber = parseChapterFromTitle(getIntent().getStringExtra("title"));

        return noChapterList && hasVideo && (apiChapterId > 0 || parsedNumber > 0);
    }

    private void loadChapterThenStartPlayback() {
        switchingChapter = true;
        updateBufferingUi();

        GitaFullAudioQueue.ChapterPlaylistCallback loadCallback = new GitaFullAudioQueue.ChapterPlaylistCallback() {
            @Override
            public void onLoaded(GitaFullAudioQueue.ChapterPlaylist chapterPlaylist) {
                if (isFinishing()) {
                    return;
                }
                if (chapterPlaylist == null || chapterPlaylist.shlokas == null
                        || chapterPlaylist.shlokas.isEmpty()) {
                    switchingChapter = false;
                    AppToast.show(GitaAudioPlayerActivity.this,
                            R.string.home_full_gita_audio_unavailable);
                    finish();
                    return;
                }
                applyChapterPlaylist(chapterPlaylist);
                switchingChapter = false;
                startPlaybackAtCurrentShloka();
            }

            @Override
            public void onError() {
                if (!isFinishing()) {
                    switchingChapter = false;
                    AppToast.show(GitaAudioPlayerActivity.this,
                            R.string.gita_audio_chapters_load_error);
                    finish();
                }
            }
        };

        int apiChapterId = getIntent().getIntExtra(EXTRA_CHAPTER_ID, 0);
        if (apiChapterId > 0) {
            GitaFullAudioQueue.loadChapter(apiChapterId, loadCallback);
        } else {
            int parsedChapter = parseChapterFromTitle(getIntent().getStringExtra("title"));
            GitaFullAudioQueue.loadChapterByNumber(parsedChapter, loadCallback);
        }
    }

    private void applyChapterPlaylist(GitaFullAudioQueue.ChapterPlaylist chapterPlaylist) {
        currentChapterApiId = chapterPlaylist.chapterId;
        chapterNumber = chapterPlaylist.chapterNumber > 0
                ? chapterPlaylist.chapterNumber
                : 0;
        if (chapterNumber <= 0) {
            chapterNumber = parseChapterFromTitle(getIntent().getStringExtra("title"));
        }
        if (chapterTitle.isEmpty() && chapterPlaylist.chapterTitle != null) {
            chapterTitle = chapterPlaylist.chapterTitle;
        }
        chapterShlokasAll.clear();
        chapterShlokasAll.addAll(chapterPlaylist.shlokas);
        playlist.clear();
        playlist.addAll(GitaPlayerPlaylist.parsePlayable(
                GitaPlayerPlaylist.toJson(chapterPlaylist.shlokas)));
    }

    private void startPlaybackAtCurrentShloka() {
        resolveChapterNumber();
        resolveCurrentChapterApiId();

        currentIndex = GitaPlayerPlaylist.findCurrentIndex(
                playlist,
                getIntent().getStringExtra("videoid"),
                getIntent().getStringExtra("title"));
        if (currentIndex < 0) {
            currentIndex = 0;
        }

        GitaChapterShlokas.Shloka current = playlist.get(currentIndex);
        applyShlokaUi(current);
        playAtIndex(currentIndex);
        LocalRecentViews.add(this, LocalRecentViews.fromShloka(current));
    }

    private void bindViews() {
        imgPoster = findViewById(R.id.imgAudioPoster);
        txtChapterBadge = findViewById(R.id.txtAudioChapterBadge);
        txtPositionBadge = findViewById(R.id.txtAudioPositionBadge);
        txtNowPlaying = findViewById(R.id.txtAudioNowPlaying);
        txtChapterLine = findViewById(R.id.txtAudioChapterLine);
        txtElapsed = findViewById(R.id.txtAudioElapsed);
        txtDuration = findViewById(R.id.txtAudioDuration);
        seekBar = findViewById(R.id.seekAudio);
        btnPlayPause = findViewById(R.id.btnAudioPlayPause);
        btnPrev = findViewById(R.id.btnAudioPrev);
        btnNext = findViewById(R.id.btnAudioNext);
        progressBuffering = findViewById(R.id.progressAudioBuffering);

    }

    @Override
    public void onBackPressed() {
        exitAndStopPlayback();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!stopPlaybackOnExit && !audioServiceBound) {
            Intent intent = new Intent(this, GitaShlokaAudioPlaybackService.class);
            bindService(intent, audioServiceConnection, Context.BIND_AUTO_CREATE);
        } else if (audioService != null) {
            audioService.setUiListener(playbackUiListener);
        }
    }

    private void loadPlaylistFromIntent() {
        playlist.clear();
        chapterShlokasAll.clear();
        String json = getIntent().getStringExtra(EXTRA_CHAPTER_SHLOKAS_JSON);
        if (json != null && !json.isEmpty()) {
            chapterShlokasAll.addAll(parseAllShlokas(json));
            playlist.addAll(GitaPlayerPlaylist.parsePlayable(json));
        }
        if (playlist.isEmpty()) {
            GitaChapterShlokas.Shloka single = singleFromIntent();
            if (single != null && hasPlayableUrl(single)) {
                playlist.add(single);
                chapterShlokasAll.add(single);
            }
        }
    }

    @Nullable
    private GitaChapterShlokas.Shloka singleFromIntent() {
        String url = getIntent().getStringExtra("videoUrl");
        if (url == null || url.trim().isEmpty()) {
            return null;
        }
        GitaChapterShlokas.Shloka shloka = new GitaChapterShlokas.Shloka();
        shloka.id = getIntent().getStringExtra("videoid") != null
                ? getIntent().getStringExtra("videoid")
                : "";
        shloka.title = getIntent().getStringExtra("title") != null
                ? getIntent().getStringExtra("title")
                : "";
        shloka.description = getIntent().getStringExtra("description");
        shloka.image = getIntent().getStringExtra("posterUrl");
        shloka.videoUrl = url;
        shloka.chapterId = getIntent().getIntExtra(EXTRA_CHAPTER_ID, 0);
        return shloka;
    }

    private static List<GitaChapterShlokas.Shloka> parseAllShlokas(String json) {
        try {
            Type type = new TypeToken<List<GitaChapterShlokas.Shloka>>() {
            }.getType();
            List<GitaChapterShlokas.Shloka> list = new Gson().fromJson(json, type);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private void wireControls() {
        btnPlayPause.setOnClickListener(v -> {
            if (audioService != null) {
                audioService.togglePlayPause();
            }
        });

        btnPrev.setOnClickListener(v -> playPreviousShloka());
        btnNext.setOnClickListener(v -> playNextShloka(true));

        txtChapterBadge.setOnClickListener(v -> showChapterPicker());
        txtPositionBadge.setOnClickListener(v -> showShlokaPicker());
        txtNowPlaying.setOnClickListener(v -> showShlokaPicker());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && audioService != null) {
                    GitaShlokaAudioPlaybackService.PlaybackSnapshot snap = audioService.copySnapshot();
                    if (snap.durationMs > 0L) {
                        long pos = (long) ((progress / 1000f) * snap.durationMs);
                        txtElapsed.setText(formatTime(pos));
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                userSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                userSeeking = false;
                if (audioService != null) {
                    GitaShlokaAudioPlaybackService.PlaybackSnapshot snap = audioService.copySnapshot();
                    if (snap.durationMs > 0L) {
                        long pos = (long) ((seekBar.getProgress() / 1000f) * snap.durationMs);
                        audioService.seekTo(pos);
                    }
                }
            }
        });
    }

    private void stopHomeFullGitaAudio() {
        Intent stop = new Intent(this, GitaFullAudioPlaybackService.class);
        stop.setAction(GitaFullAudioPlaybackService.ACTION_STOP);
        startService(stop);
    }

    private void exitAndStopPlayback() {
        stopPlaybackOnExit = true;
        stopShlokaAudioService();
        ActivityNav.finish(this);
    }

    private void stopShlokaAudioService() {
        if (audioService != null) {
            audioService.setUiListener(null);
        }
        Intent stop = new Intent(this, GitaShlokaAudioPlaybackService.class);
        stop.setAction(GitaShlokaAudioPlaybackService.ACTION_STOP);
        startService(stop);
        unbindAudioService();
    }

    private void unbindAudioService() {
        if (!audioServiceBound) {
            return;
        }
        try {
            unbindService(audioServiceConnection);
        } catch (Exception ignored) {
        }
        audioServiceBound = false;
        audioService = null;
    }

    private void startShlokaAudioService(int startIndex) {
        if (playlist.isEmpty()) {
            return;
        }
        String poster = getIntent().getStringExtra("posterUrl");
        Intent intent = new Intent(this, GitaShlokaAudioPlaybackService.class);
        intent.setAction(GitaShlokaAudioPlaybackService.ACTION_START);
        intent.putExtra(GitaShlokaAudioPlaybackService.EXTRA_PLAYLIST_JSON,
                GitaPlayerPlaylist.toJson(playlist));
        intent.putExtra(GitaShlokaAudioPlaybackService.EXTRA_CHAPTER_TITLE, chapterTitle);
        intent.putExtra(GitaShlokaAudioPlaybackService.EXTRA_START_INDEX, startIndex);
        if (poster != null && !poster.trim().isEmpty()) {
            intent.putExtra(GitaShlokaAudioPlaybackService.EXTRA_POSTER_URL, poster.trim());
        }
        ContextCompat.startForegroundService(this, intent);
        if (!audioServiceBound) {
            bindService(intent, audioServiceConnection, Context.BIND_AUTO_CREATE);
        } else if (audioService != null) {
            audioService.setPlaylist(playlist, chapterTitle, startIndex, poster);
        }
    }

    private void applyPlaybackSnapshot(GitaShlokaAudioPlaybackService.PlaybackSnapshot snapshot) {
        if (snapshot == null || !snapshot.active) {
            return;
        }
        switchingChapter = false;
        currentIndex = snapshot.currentIndex;
        if (currentIndex >= 0 && currentIndex < playlist.size()) {
            applyShlokaUi(playlist.get(currentIndex));
        }
        txtElapsed.setText(GitaShlokaAudioPlaybackService.formatTime(snapshot.positionMs));
        txtDuration.setText(GitaShlokaAudioPlaybackService.formatTime(snapshot.durationMs));
        if (!userSeeking && snapshot.durationMs > 0L) {
            int progress = (int) ((snapshot.positionMs * 1000L) / snapshot.durationMs);
            seekBar.setProgress(Math.min(1000, Math.max(0, progress)));
        }
        updatePlayPauseIcon(snapshot.playing);
        updateBufferingUi(snapshot.bufferingMedia);
        updateSkipButtonsEnabled();
    }

    private void resolveChapterNumber() {
        chapterNumber = parseChapterFromTitle(getIntent().getStringExtra("title"));
        if (chapterNumber > 0) {
            return;
        }
        for (GitaChapterShlokas.Shloka shloka : playlist) {
            int parsed = parseChapterFromTitle(shloka.title);
            if (parsed > 0) {
                chapterNumber = parsed;
                return;
            }
        }
        for (GitaChapterShlokas.Shloka shloka : chapterShlokasAll) {
            int parsed = parseChapterFromTitle(shloka.title);
            if (parsed > 0) {
                chapterNumber = parsed;
                return;
            }
        }
    }

    private void resolveCurrentChapterApiId() {
        currentChapterApiId = getIntent().getIntExtra(EXTRA_CHAPTER_ID, 0);
        if (currentChapterApiId <= 0 && currentIndex >= 0 && currentIndex < playlist.size()) {
            currentChapterApiId = playlist.get(currentIndex).chapterId;
        }
        if (currentChapterApiId <= 0 && !playlist.isEmpty()) {
            currentChapterApiId = playlist.get(0).chapterId;
        }
    }

    private static int parseChapterFromTitle(@Nullable String title) {
        if (title == null) {
            return 0;
        }
        Matcher matcher = POSITION_IN_TITLE.matcher(title.trim());
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
        Matcher matcher = POSITION_IN_TITLE.matcher(title.trim());
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(2));
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private int resolveShlokaNumber(GitaChapterShlokas.Shloka shloka, String title) {
        int shlokaNum = parseShlokaFromTitle(title);
        if (shlokaNum <= 0) {
            shlokaNum = parseShlokaFromTitle(getIntent().getStringExtra("title"));
        }
        if (shlokaNum <= 0 && shloka.sequence > 0) {
            shlokaNum = shloka.sequence;
        }
        if (shlokaNum <= 0 && !chapterShlokasAll.isEmpty()) {
            for (int i = 0; i < chapterShlokasAll.size(); i++) {
                if (isSameShloka(shloka, chapterShlokasAll.get(i))) {
                    shlokaNum = chapterShlokasAll.get(i).sequence > 0
                            ? chapterShlokasAll.get(i).sequence
                            : i + 1;
                    break;
                }
            }
        }
        if (shlokaNum <= 0 && currentIndex >= 0) {
            shlokaNum = currentIndex + 1;
        }
        return Math.max(1, shlokaNum);
    }

    private static boolean isSameShloka(GitaChapterShlokas.Shloka a, GitaChapterShlokas.Shloka b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.id != null && b.id != null && !a.id.isEmpty() && a.id.equals(b.id)) {
            return true;
        }
        String titleA = a.title != null ? a.title.trim() : "";
        String titleB = b.title != null ? b.title.trim() : "";
        return !titleA.isEmpty() && titleA.equalsIgnoreCase(titleB);
    }

    private void applyShlokaUi(GitaChapterShlokas.Shloka shloka) {
        String title = shloka.title != null ? shloka.title : "";
        txtNowPlaying.setText(title);
        if (!chapterTitle.isEmpty()) {
            txtChapterLine.setText(chapterTitle);
        } else {
            txtChapterLine.setText(title);
        }

        int displayChapter = parseChapterFromTitle(title);
        if (displayChapter <= 0) {
            displayChapter = parseChapterFromTitle(getIntent().getStringExtra("title"));
        }
        if (displayChapter <= 0) {
            displayChapter = chapterNumber;
        }
        if (displayChapter > 0) {
            chapterNumber = displayChapter;
        }

        int shlokaNum = resolveShlokaNumber(shloka, title);
        txtChapterBadge.setVisibility(View.VISIBLE);
        txtChapterBadge.setText(getString(R.string.chapter_number_label,
                displayChapter > 0 ? displayChapter : Math.max(chapterNumber, 1)));
        txtPositionBadge.setText(
                (displayChapter > 0 ? displayChapter : Math.max(chapterNumber, 1)) + "." + shlokaNum);

        String image = shloka.image != null ? shloka.image.trim() : "";
        if (!image.isEmpty()) {
            Glide.with(this)
                    .load(image)
                    .centerCrop()
                    .placeholder(R.color.view_bg_color)
                    .error(R.color.view_bg_color)
                    .into(imgPoster);
        } else {
            String poster = getIntent().getStringExtra("posterUrl");
            if (poster != null && !poster.trim().isEmpty()) {
                Glide.with(this)
                        .load(poster)
                        .centerCrop()
                        .placeholder(R.color.view_bg_color)
                        .error(R.color.view_bg_color)
                        .into(imgPoster);
            } else {
                imgPoster.setImageResource(R.color.view_bg_color);
            }
        }
        updateSkipButtonsEnabled();
    }

    private void updateSkipButtonsEnabled() {
        btnPrev.setEnabled(currentIndex > 0);
        btnNext.setEnabled(currentIndex >= 0 && currentIndex < playlist.size() - 1);
        float alpha = 1f;
        float disabledAlpha = 0.4f;
        btnPrev.setAlpha(currentIndex > 0 ? alpha : disabledAlpha);
        btnNext.setAlpha(currentIndex < playlist.size() - 1 ? alpha : disabledAlpha);
    }

    private void playAtIndex(int index) {
        if (index < 0 || index >= playlist.size()) {
            return;
        }
        currentIndex = index;
        GitaChapterShlokas.Shloka shloka = playlist.get(index);
        applyShlokaUi(shloka);
        LocalRecentViews.add(this, LocalRecentViews.fromShloka(shloka));

        if (!hasPlayableUrl(shloka)) {
            playNextShloka(false);
            return;
        }

        if (audioService != null) {
            audioService.playAtIndex(index);
        } else {
            startShlokaAudioService(index);
        }
        updateBufferingUi(true);
    }

    private void playPreviousShloka() {
        if (audioService != null) {
            if (currentIndex <= 0 && audioService.copySnapshot().positionMs <= 3000L) {
                AppToast.show(this, R.string.gita_audio_no_previous_shloka);
                return;
            }
            audioService.skipToPreviousShloka();
            return;
        }
        if (currentIndex <= 0) {
            AppToast.show(this, R.string.gita_audio_no_previous_shloka);
            return;
        }
        playAtIndex(currentIndex - 1);
    }

    private void playNextShloka(boolean fromUser) {
        if (currentIndex < 0 || currentIndex >= playlist.size() - 1) {
            if (fromUser) {
                AppToast.show(this, R.string.gita_audio_no_next_shloka);
            }
            return;
        }
        if (audioService != null) {
            audioService.skipToNextShloka();
        } else {
            playAtIndex(currentIndex + 1);
        }
    }

    private void showChapterPicker() {
        GitaFullAudioQueue.loadAllChapters(new GitaFullAudioQueue.ChapterListCallback() {
            @Override
            public void onLoaded(List<GitaChapters.Chapter> chapters) {
                if (isFinishing()) {
                    return;
                }
                GitaAudioPickerSheets.showChapterPicker(
                        GitaAudioPlayerActivity.this,
                        chapters,
                        currentChapterApiId,
                        chapterTitle,
                        chapter -> loadChapterAndPlay(chapter.chapterId));
            }

            @Override
            public void onError() {
                AppToast.show(GitaAudioPlayerActivity.this, R.string.gita_audio_chapters_load_error);
            }
        });
    }

    private void loadChapterAndPlay(int apiChapterId) {
        if (apiChapterId <= 0) {
            return;
        }
        switchingChapter = true;
        updateBufferingUi();
        GitaFullAudioQueue.loadChapter(apiChapterId, new GitaFullAudioQueue.ChapterPlaylistCallback() {
            @Override
            public void onLoaded(GitaFullAudioQueue.ChapterPlaylist chapterPlaylist) {
                if (isFinishing()) {
                    return;
                }
                if (chapterPlaylist == null || chapterPlaylist.shlokas == null
                        || chapterPlaylist.shlokas.isEmpty()) {
                    switchingChapter = false;
                    updateBufferingUi();
                    AppToast.show(GitaAudioPlayerActivity.this,
                            R.string.home_full_gita_audio_unavailable);
                    return;
                }
                applyChapterPlaylist(chapterPlaylist);
                if (playlist.isEmpty()) {
                    switchingChapter = false;
                    updateBufferingUi();
                    AppToast.show(GitaAudioPlayerActivity.this,
                            R.string.home_full_gita_audio_unavailable);
                    return;
                }
                resolveChapterNumber();
                resolveCurrentChapterApiId();
                currentIndex = GitaPlayerPlaylist.findCurrentIndex(
                        playlist,
                        getIntent().getStringExtra("videoid"),
                        getIntent().getStringExtra("title"));
                if (currentIndex < 0) {
                    currentIndex = 0;
                }
                startShlokaAudioService(currentIndex);
            }

            @Override
            public void onError() {
                if (!isFinishing()) {
                    switchingChapter = false;
                    updateBufferingUi(false);
                    AppToast.show(GitaAudioPlayerActivity.this,
                            R.string.gita_audio_chapters_load_error);
                }
            }
        });
    }

    private void showShlokaPicker() {
        List<GitaChapterShlokas.Shloka> pickerList = !chapterShlokasAll.isEmpty()
                ? chapterShlokasAll
                : playlist;
        int pickerIndex = findIndexInChapterList(pickerList, playlist.get(currentIndex));
        GitaAudioPickerSheets.showShlokaPicker(
                this,
                pickerList,
                chapterTitle,
                Math.max(0, pickerIndex),
                indexInChapter -> {
                    GitaChapterShlokas.Shloka selected = pickerList.get(indexInChapter);
                    int playableIndex = findIndexInPlayable(selected);
                    if (playableIndex >= 0) {
                        playAtIndex(playableIndex);
                    } else {
                        AppToast.show(this, R.string.video_unavailable);
                    }
                });
    }

    private int findIndexInChapterList(List<GitaChapterShlokas.Shloka> list,
            GitaChapterShlokas.Shloka target) {
        if (target == null) {
            return 0;
        }
        String id = target.id != null ? target.id : "";
        for (int i = 0; i < list.size(); i++) {
            GitaChapterShlokas.Shloka item = list.get(i);
            if (item.id != null && item.id.equals(id)) {
                return i;
            }
        }
        String title = target.title != null ? target.title.trim() : "";
        for (int i = 0; i < list.size(); i++) {
            GitaChapterShlokas.Shloka item = list.get(i);
            if (item.title != null && item.title.trim().equalsIgnoreCase(title)) {
                return i;
            }
        }
        return 0;
    }

    private int findIndexInPlayable(GitaChapterShlokas.Shloka shloka) {
        return GitaPlayerPlaylist.findCurrentIndex(
                playlist,
                shloka.id != null ? shloka.id : "",
                shloka.title);
    }

    private void updatePlayPauseIcon(boolean playing) {
        if (btnPlayPause.getVisibility() != View.VISIBLE) {
            return;
        }
        btnPlayPause.setImageResource(
                playing ? android.R.drawable.ic_media_pause : R.drawable.ic_play_circle);
        btnPlayPause.setColorFilter(ContextCompat.getColor(this, R.color.white));
    }

    private void updateBufferingUi() {
        updateBufferingUi(switchingChapter);
    }

    private void updateBufferingUi(boolean bufferingMedia) {
        setMediaBufferingUi(switchingChapter || bufferingMedia);
    }

    private void setMediaBufferingUi(boolean loading) {
        if (playOverlayLoadingVisible == loading) {
            return;
        }
        playOverlayLoadingVisible = loading;
        btnPlayPause.setVisibility(loading ? View.INVISIBLE : View.VISIBLE);
        progressBuffering.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private static boolean hasPlayableUrl(GitaChapterShlokas.Shloka shloka) {
        return shloka != null
                && shloka.videoUrl != null
                && !shloka.videoUrl.trim().isEmpty();
    }

    private static String formatTime(long ms) {
        if (ms <= 0L) {
            return "0:00";
        }
        long totalSeconds = ms / 1000L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return String.format(Locale.US, "%d:%02d", minutes, seconds);
    }

    @Override
    protected void onStop() {
        if (audioService != null) {
            audioService.setUiListener(null);
        }
        if (!stopPlaybackOnExit) {
            unbindAudioService();
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (audioService != null) {
            audioService.setUiListener(null);
        }
        unbindAudioService();
        super.onDestroy();
    }
}

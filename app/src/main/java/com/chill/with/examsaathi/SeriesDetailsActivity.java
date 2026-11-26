package com.chill.with.examsaathi;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Chapter/series details — hero + Video/Audio/MindMap tabs + topic list. */
public class SeriesDetailsActivity extends AppCompatActivity implements VideoDownloadHelper.Listener {

    public static final String EXTRA_SERIES_ID = "series_id";
    public static final String EXTRA_SERIES_TITLE = "series_title";
    public static final String EXTRA_SUBJECT_NAME = "subject_name";
    public static final String EXTRA_EXAM_NAME = "exam_name";
    public static final String EXTRA_EXAM_THUMB = "exam_thumb";
    public static final String EXTRA_BANNER = "banner";

    public static void open(
            @NonNull Context context,
            long seriesId,
            @Nullable String seriesTitle,
            @Nullable String subjectName,
            @Nullable String examName,
            @Nullable String examThumb,
            @Nullable String banner) {
        Intent intent = new Intent(context, SeriesDetailsActivity.class);
        intent.putExtra(EXTRA_SERIES_ID, seriesId);
        intent.putExtra(EXTRA_SERIES_TITLE, seriesTitle != null ? seriesTitle : "");
        intent.putExtra(EXTRA_SUBJECT_NAME, subjectName != null ? subjectName : "");
        intent.putExtra(EXTRA_EXAM_NAME, examName != null ? examName : "");
        intent.putExtra(EXTRA_EXAM_THUMB, examThumb != null ? examThumb : "");
        intent.putExtra(EXTRA_BANNER, banner != null ? banner : "");
        ActivityNav.start(context, intent);
    }

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private ImageView imgSeriesBanner;
    private ImageView imgExamThumb;
    private ImageView btnBookmark;
    private TextView txtExamChip;
    private TextView txtBreadcrumb;
    private TextView txtSeriesTitle;
    private TextView txtProgressLabel;
    private TextView txtSectionTitle;
    private TextView txtSeriesError;
    private TextView btnWatchSeries;
    private ProgressBar progressSeries;
    private ProgressBar progressSeriesLoad;
    private ProgressBar progressTopicsInline;
    private View layoutSeriesLoader;
    @Nullable
    private LottieAnimationView lottieSeriesLoader;
    private LinearLayout layoutContentChips;
    private LinearLayout tabVideo;
    private LinearLayout tabAudio;
    private LinearLayout tabMindMap;
    private ImageView iconTabVideo;
    private ImageView iconTabAudio;
    private ImageView iconTabMindMap;
    private TextView txtTabVideo;
    private TextView txtTabAudio;
    private TextView txtTabMindMap;
    private RecyclerView recyclerTopics;
    private WebView webMindMap;
    private TopicsAdapter topicsAdapter;

    private long seriesId;
    private String seriesTitle = "";
    private String subjectName = "";
    private String examName = "";
    private String examThumb = "";
    private String bannerHint = "";
    private String selectedTab = "video";
    private boolean bookmarked;
    private boolean loading;
    private String mindMapUrl = "";
    @Nullable
    private PrepSeriesRepository.Result currentResult;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_series_details);
        setupStatusBar();
        readExtras();
        bindViews();
        findViewById(R.id.btnBack).setOnClickListener(v -> ActivityNav.finish(this));
        findViewById(R.id.btnShare).setOnClickListener(v -> shareSeries());
        btnBookmark.setOnClickListener(v -> toggleBookmark());
        btnWatchSeries.setOnClickListener(v -> playFirstTopic());
        tabVideo.setOnClickListener(v -> selectTab("video"));
        tabAudio.setOnClickListener(v -> selectTab("audio"));
        tabMindMap.setOnClickListener(v -> selectTab("notes"));
        topicsAdapter = new TopicsAdapter();
        recyclerTopics.setLayoutManager(new LinearLayoutManager(this));
        recyclerTopics.setAdapter(topicsAdapter);
        bindPlaceholders();
        loadDetails(false);
    }

    @Override
    protected void onStart() {
        super.onStart();
        VideoDownloadHelper.addListener(this);
        if (topicsAdapter != null) {
            topicsAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onStop() {
        VideoDownloadHelper.removeListener(this);
        super.onStop();
    }

    @Override
    public void onDownloadProgress(@NonNull VideoDownloadHelper.Progress progress) {
        if (topicsAdapter != null) {
            topicsAdapter.notifyDownloadProgress(progress);
        }
        if (progress.success) {
            AppToast.show(this, getString(R.string.prep_download_saved_in,
                    VideoDownloadHelper.publicFolderHint()));
            offerShareAfterDownload(progress.topicId);
        } else if (progress.failed) {
            AppToast.show(this, R.string.prep_download_failed);
        }
    }

    private void readExtras() {
        Intent intent = getIntent();
        seriesId = intent != null ? intent.getLongExtra(EXTRA_SERIES_ID, 0L) : 0L;
        seriesTitle = safe(intent != null ? intent.getStringExtra(EXTRA_SERIES_TITLE) : null);
        subjectName = safe(intent != null ? intent.getStringExtra(EXTRA_SUBJECT_NAME) : null);
        examName = safe(intent != null ? intent.getStringExtra(EXTRA_EXAM_NAME) : null);
        examThumb = safe(intent != null ? intent.getStringExtra(EXTRA_EXAM_THUMB) : null);
        bannerHint = safe(intent != null ? intent.getStringExtra(EXTRA_BANNER) : null);
    }

    private void bindViews() {
        imgSeriesBanner = findViewById(R.id.imgSeriesBanner);
        imgExamThumb = findViewById(R.id.imgExamThumb);
        btnBookmark = findViewById(R.id.btnBookmark);
        txtExamChip = findViewById(R.id.txtExamChip);
        txtBreadcrumb = findViewById(R.id.txtBreadcrumb);
        txtSeriesTitle = findViewById(R.id.txtSeriesTitle);
        txtProgressLabel = findViewById(R.id.txtProgressLabel);
        txtSectionTitle = findViewById(R.id.txtSectionTitle);
        txtSeriesError = findViewById(R.id.txtSeriesError);
        btnWatchSeries = findViewById(R.id.btnWatchSeries);
        progressSeries = findViewById(R.id.progressSeries);
        progressSeriesLoad = findViewById(R.id.progressSeriesLoad);
        progressTopicsInline = findViewById(R.id.progressTopicsInline);
        layoutSeriesLoader = findViewById(R.id.layoutSeriesLoader);
        lottieSeriesLoader = findViewById(R.id.lottieSeriesLoader);
        layoutContentChips = findViewById(R.id.layoutContentChips);
        tabVideo = findViewById(R.id.tabVideo);
        tabAudio = findViewById(R.id.tabAudio);
        tabMindMap = findViewById(R.id.tabMindMap);
        iconTabVideo = findViewById(R.id.iconTabVideo);
        iconTabAudio = findViewById(R.id.iconTabAudio);
        iconTabMindMap = findViewById(R.id.iconTabMindMap);
        txtTabVideo = findViewById(R.id.txtTabVideo);
        txtTabAudio = findViewById(R.id.txtTabAudio);
        txtTabMindMap = findViewById(R.id.txtTabMindMap);
        recyclerTopics = findViewById(R.id.recyclerTopics);
        webMindMap = findViewById(R.id.webMindMap);
        View topBar = findViewById(R.id.layoutSeriesTopBar);
        ViewCompat.setOnApplyWindowInsetsListener(topBar, (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(v.getPaddingLeft(), top + dp(4), v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });
        WebSettings settings = webMindMap.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        webMindMap.setWebViewClient(new WebViewClient());
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }
        Window window = getWindow();
        window.setStatusBarColor(Color.TRANSPARENT);
        // Default nav: darkest black; loader overrides to darkest red while visible
        window.setNavigationBarColor(ContextCompat.getColor(this, R.color.app_black));
        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    private void applyLoaderSystemBars(boolean loading) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }
        Window window = getWindow();
        // Same as home bottom nav (#100A0E / colorPrimary)
        int loaderBg = ContextCompat.getColor(this, R.color.colorPrimary);
        if (loading) {
            window.setStatusBarColor(loaderBg);
            window.setNavigationBarColor(loaderBg);
        } else {
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(loaderBg);
        }
    }

    private void bindPlaceholders() {
        if (!seriesTitle.isEmpty()) {
            txtSeriesTitle.setText(seriesTitle);
        }
        if (!examName.isEmpty()) {
            txtExamChip.setText(examName);
        } else {
            txtExamChip.setText(R.string.prep_full_course_for);
        }
        updateBreadcrumb();
        if (!examThumb.isEmpty()) {
            Glide.with(this).load(examThumb).circleCrop().into(imgExamThumb);
        }
        if (!bannerHint.isEmpty()) {
            Glide.with(this).load(bannerHint).centerCrop().into(imgSeriesBanner);
        }
        updateTabUi();
    }

    private void updateBreadcrumb() {
        String left = !subjectName.isEmpty() ? subjectName : examName;
        String right = !seriesTitle.isEmpty() ? seriesTitle : getString(R.string.prep_tab_video);
        if (left.isEmpty()) {
            txtBreadcrumb.setText(right);
        } else {
            txtBreadcrumb.setText(getString(R.string.prep_breadcrumb, left, right));
        }
    }

    private void selectTab(@NonNull String tab) {
        if (tab.equals(selectedTab)) {
            return;
        }
        selectedTab = tab;
        updateTabUi();
        if ("notes".equals(tab)) {
            showMindMap();
            return;
        }
        webMindMap.setVisibility(View.GONE);
        recyclerTopics.setVisibility(View.VISIBLE);
        txtSectionTitle.setVisibility(View.VISIBLE);
        // Tab switches: inline list loader only — never full-screen again
        loadDetails(true);
    }

    private void updateTabUi() {
        styleTab(tabVideo, iconTabVideo, txtTabVideo, "video".equals(selectedTab));
        styleTab(tabAudio, iconTabAudio, txtTabAudio, "audio".equals(selectedTab));
        styleTab(tabMindMap, iconTabMindMap, txtTabMindMap, "notes".equals(selectedTab));
        if ("audio".equals(selectedTab)) {
            btnWatchSeries.setText(R.string.prep_listen_now);
        } else {
            btnWatchSeries.setText(R.string.prep_watch_video_series);
        }
    }

    private void styleTab(
            @NonNull LinearLayout tab,
            @NonNull ImageView icon,
            @NonNull TextView label,
            boolean selected) {
        tab.setBackgroundResource(selected ? R.drawable.bg_series_tab_selected : 0);
        int color = ContextCompat.getColor(this, selected ? R.color.white : R.color.series_tab_muted);
        icon.setColorFilter(color);
        label.setTextColor(color);
        Typeface face = ResourcesCompat.getFont(this,
                selected ? R.font.outfit_semi_bold : R.font.outfit_medium);
        if (face != null) {
            label.setTypeface(face);
        }
    }

    private void showFullScreenLoader() {
        hideInlineLoader();
        applyLoaderSystemBars(true);
        if (layoutSeriesLoader != null) {
            layoutSeriesLoader.setVisibility(View.VISIBLE);
            layoutSeriesLoader.bringToFront();
        }
        if (lottieSeriesLoader != null) {
            lottieSeriesLoader.setVisibility(View.VISIBLE);
            if (!lottieSeriesLoader.isAnimating()) {
                lottieSeriesLoader.playAnimation();
            }
        }
    }

    private void hideFullScreenLoader() {
        if (layoutSeriesLoader != null) {
            layoutSeriesLoader.setVisibility(View.GONE);
        }
        if (lottieSeriesLoader != null) {
            lottieSeriesLoader.pauseAnimation();
        }
        if (progressSeriesLoad != null) {
            progressSeriesLoad.setVisibility(View.GONE);
        }
        applyLoaderSystemBars(false);
    }

    private void showInlineLoader() {
        hideFullScreenLoader();
        if (progressTopicsInline != null) {
            progressTopicsInline.setVisibility(View.VISIBLE);
        }
    }

    private void hideInlineLoader() {
        if (progressTopicsInline != null) {
            progressTopicsInline.setVisibility(View.GONE);
        }
    }

    private void hideAllLoaders() {
        hideFullScreenLoader();
        hideInlineLoader();
    }

    private void loadDetails(boolean quiet) {
        if (loading) {
            return;
        }
        if (seriesId <= 0) {
            hideAllLoaders();
            txtSeriesError.setVisibility(View.VISIBLE);
            return;
        }
        loading = true;
        if (quiet) {
            showInlineLoader();
        } else {
            showFullScreenLoader();
        }
        txtSeriesError.setVisibility(View.GONE);
        final String contentType = "audio".equals(selectedTab) ? "audio" : "video";
        new Thread(() -> {
            try {
                PrepSeriesRepository.Result result =
                        PrepSeriesRepository.loadDetails(seriesId, contentType);
                mainHandler.post(() -> {
                    loading = false;
                    hideAllLoaders();
                    if ("notes".equals(selectedTab)) {
                        return;
                    }
                    bindResult(result);
                });
            } catch (Exception e) {
                android.util.Log.e("SeriesDetails", "load failed", e);
                mainHandler.post(() -> {
                    loading = false;
                    hideAllLoaders();
                    recyclerTopics.setVisibility(View.GONE);
                    txtSeriesError.setVisibility(View.VISIBLE);
                    if (e.getMessage() != null && !e.getMessage().isEmpty()) {
                        txtSeriesError.setText(e.getMessage());
                    }
                });
            }
        }).start();
    }

    private void bindResult(@NonNull PrepSeriesRepository.Result result) {
        currentResult = result;
        if (!result.seriesTitle.isEmpty()) {
            seriesTitle = result.seriesTitle;
            txtSeriesTitle.setText(seriesTitle);
        }
        updateBreadcrumb();
        mindMapUrl = result.mindMapUrl != null ? result.mindMapUrl : "";
        bookmarked = result.isBookmarked;
        updateBookmarkIcon();
        if (result.bannerUrl != null && !result.bannerUrl.isEmpty()) {
            Glide.with(this).load(result.bannerUrl).centerCrop().into(imgSeriesBanner);
        }
        bindChips(result.contentChips);
        txtProgressLabel.setText(getString(
                R.string.prep_topics_completed, result.completedTopics, result.totalTopics));
        int pct = result.progressPercent;
        if (pct <= 0 && result.totalTopics > 0) {
            pct = Math.round((result.completedTopics * 100f) / result.totalTopics);
        }
        progressSeries.setProgress(Math.max(0, Math.min(100, pct)));
        txtSectionTitle.setText(getString(
                R.string.prep_section_topics, result.sectionTitle, result.topics.size()));
        if (result.topics.isEmpty()) {
            recyclerTopics.setVisibility(View.GONE);
            txtSeriesError.setVisibility(View.VISIBLE);
            return;
        }
        txtSeriesError.setVisibility(View.GONE);
        recyclerTopics.setVisibility(View.VISIBLE);
        topicsAdapter.setItems(result.topics);
    }

    private void bindChips(@NonNull List<String> chips) {
        layoutContentChips.removeAllViews();
        for (String chip : chips) {
            TextView tv = new TextView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(dp(8));
            tv.setLayoutParams(lp);
            tv.setBackgroundResource(R.drawable.bg_series_chip);
            tv.setText(chip);
            tv.setTextColor(Color.WHITE);
            tv.setTextSize(12f);
            Typeface face = ResourcesCompat.getFont(this, R.font.outfit_medium);
            if (face != null) {
                tv.setTypeface(face);
            }
            tv.setPadding(dp(12), dp(6), dp(12), dp(6));
            layoutContentChips.addView(tv);
        }
    }

    private void showMindMap() {
        recyclerTopics.setVisibility(View.GONE);
        txtSectionTitle.setVisibility(View.GONE);
        txtSeriesError.setVisibility(View.GONE);
        if (mindMapUrl.isEmpty() && currentResult != null) {
            mindMapUrl = currentResult.mindMapUrl != null ? currentResult.mindMapUrl : "";
        }
        if (mindMapUrl.isEmpty()) {
            webMindMap.setVisibility(View.GONE);
            txtSeriesError.setVisibility(View.VISIBLE);
            txtSeriesError.setText(R.string.prep_mindmap_missing);
            return;
        }
        webMindMap.setVisibility(View.VISIBLE);
        webMindMap.loadUrl(mindMapUrl);
    }

    private void playFirstTopic() {
        if (currentResult == null || currentResult.topics.isEmpty()) {
            AppToast.show(this, R.string.prep_local_video_error);
            return;
        }
        playTopic(currentResult.topics.get(0));
    }

    private void playTopic(@NonNull PrepSeriesRepository.Topic topic) {
        String url = PrepSeriesRepository.playUrl(topic);
        if (url.isEmpty()) {
            AppToast.show(this, R.string.prep_local_video_error);
            return;
        }
        boolean audioMode = "audio".equals(selectedTab) || topic.isAudio;
        if (audioMode) {
            openAudioPlayer(topic, url);
            return;
        }
        openVerticalVideoPlayer(String.valueOf(topic.id), topic.title, topic.thumbnailUrl, url);
    }

    private void playExample(@NonNull PrepSeriesRepository.Example example) {
        String url = PrepSeriesRepository.playUrl(example);
        if (url.isEmpty()) {
            AppToast.show(this, R.string.prep_local_video_error);
            return;
        }
        String title = example.title != null ? example.title : "";
        if (example.label != null && !example.label.isEmpty()) {
            title = example.label + (title.isEmpty() ? "" : " · " + title);
        }
        openVerticalVideoPlayer(String.valueOf(example.id), title, example.thumbnailUrl, url);
    }

    private void openVerticalVideoPlayer(
            @NonNull String startId,
            @Nullable String title,
            @Nullable String thumb,
            @NonNull String url) {
        String playlistJson = buildVideoPlaylistJson();
        if (playlistJson == null || playlistJson.isEmpty()) {
            Intent player = new Intent(this, ExoPlayerActivity.class);
            player.putExtra("link", url);
            player.putExtra("title", title != null ? title : "");
            player.putExtra("img", thumb != null ? thumb : "");
            player.putExtra("videoid", startId);
            player.putExtra("type", "Gita");
            ActivityNav.start(this, player);
            return;
        }
        PrepReelsPlayerActivity.open(this, playlistJson, startId, seriesTitle);
    }

    /**
     * Flat vertical playlist: each topic, then its examples immediately after,
     * so swipe continues into examples before the next topic.
     */
    @Nullable
    private String buildVideoPlaylistJson() {
        if (currentResult == null || currentResult.topics.isEmpty()) {
            return null;
        }
        try {
            org.json.JSONArray arr = new org.json.JSONArray();
            for (PrepSeriesRepository.Topic t : currentResult.topics) {
                String playUrl = PrepSeriesRepository.playUrl(t);
                if (!playUrl.isEmpty()) {
                    arr.put(playlistItem(
                            t.id,
                            t.title,
                            seriesTitle,
                            t.thumbnailUrl,
                            playUrl,
                            arr.length() + 1));
                }
                for (PrepSeriesRepository.Example ex : t.examples) {
                    String exUrl = PrepSeriesRepository.playUrl(ex);
                    if (exUrl.isEmpty()) {
                        continue;
                    }
                    String exTitle = ex.title != null ? ex.title : "";
                    String label = ex.label != null ? ex.label : getString(R.string.prep_example_badge);
                    if (!label.isEmpty()) {
                        exTitle = label + (exTitle.isEmpty() ? "" : " · " + exTitle);
                    }
                    arr.put(playlistItem(
                            ex.id,
                            exTitle,
                            seriesTitle,
                            ex.thumbnailUrl,
                            exUrl,
                            arr.length() + 1));
                }
            }
            return arr.length() > 0 ? arr.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    @NonNull
    private org.json.JSONObject playlistItem(
            long id,
            @Nullable String title,
            @Nullable String description,
            @Nullable String image,
            @NonNull String videoUrl,
            int sequence) throws Exception {
        org.json.JSONObject obj = new org.json.JSONObject();
        obj.put("id", String.valueOf(id));
        obj.put("sequence", sequence);
        obj.put("chapterId", seriesId);
        obj.put("title", title != null ? title : "");
        obj.put("name", title != null ? title : "");
        obj.put("description", description != null ? description : "");
        obj.put("image", image != null ? image : "");
        obj.put("videoUrl", videoUrl);
        return obj;
    }

    private void openAudioPlayer(@NonNull PrepSeriesRepository.Topic topic, @NonNull String url) {
        Intent player = new Intent(this, GitaAudioPlayerActivity.class);
        player.putExtra("videoUrl", url);
        player.putExtra("title", topic.title != null ? topic.title : "");
        player.putExtra("posterUrl", topic.thumbnailUrl != null ? topic.thumbnailUrl : "");
        player.putExtra("videoid", String.valueOf(topic.id));
        player.putExtra("description", seriesTitle);
        player.putExtra(GitaAudioPlayerActivity.EXTRA_CHAPTER_TITLE, seriesTitle);
        player.putExtra(GitaAudioPlayerActivity.EXTRA_CHAPTER_ID, (int) Math.min(Integer.MAX_VALUE, seriesId));
        String playlistJson = buildAudioPlaylistJson(topic.id);
        if (playlistJson != null) {
            player.putExtra(GitaAudioPlayerActivity.EXTRA_CHAPTER_SHLOKAS_JSON, playlistJson);
        }
        ActivityNav.start(this, player);
    }

    @Nullable
    private String buildAudioPlaylistJson(long startTopicId) {
        if (currentResult == null || currentResult.topics.isEmpty()) {
            return null;
        }
        try {
            org.json.JSONArray arr = new org.json.JSONArray();
            int startIndex = 0;
            for (int i = 0; i < currentResult.topics.size(); i++) {
                PrepSeriesRepository.Topic t = currentResult.topics.get(i);
                String playUrl = PrepSeriesRepository.playUrl(t);
                if (playUrl.isEmpty()) {
                    continue;
                }
                org.json.JSONObject obj = new org.json.JSONObject();
                obj.put("id", String.valueOf(t.id));
                obj.put("sequence", arr.length() + 1);
                obj.put("chapterId", seriesId);
                obj.put("title", t.title != null ? t.title : "");
                obj.put("name", t.title != null ? t.title : "");
                obj.put("description", seriesTitle);
                obj.put("image", t.thumbnailUrl != null ? t.thumbnailUrl : "");
                obj.put("videoUrl", playUrl);
                if (t.id == startTopicId) {
                    startIndex = arr.length();
                }
                arr.put(obj);
            }
            if (arr.length() == 0) {
                return null;
            }
            // Rotate so selected track is first (audio player starts at index 0).
            if (startIndex > 0) {
                org.json.JSONArray rotated = new org.json.JSONArray();
                for (int i = startIndex; i < arr.length(); i++) {
                    rotated.put(arr.get(i));
                }
                for (int i = 0; i < startIndex; i++) {
                    rotated.put(arr.get(i));
                }
                return rotated.toString();
            }
            return arr.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private void shareSeries() {
        String text = !seriesTitle.isEmpty() ? seriesTitle : getString(R.string.app_name);
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(share, getString(R.string.share)));
    }

    private void toggleBookmark() {
        LocalWatchlist.Entry entry = new LocalWatchlist.Entry();
        entry.title = !seriesTitle.isEmpty() ? seriesTitle : getString(R.string.app_name);
        if (currentResult != null && currentResult.bannerUrl != null && !currentResult.bannerUrl.isEmpty()) {
            entry.posterUrl = currentResult.bannerUrl;
        } else {
            entry.posterUrl = bannerHint != null ? bannerHint : "";
        }
        entry.description = examName != null ? examName : "";
        entry.videoid = String.valueOf(seriesId);
        entry.mediaType = "audio".equals(selectedTab) ? "audio" : "video";
        if (currentResult != null && !currentResult.topics.isEmpty()) {
            PrepSeriesRepository.Topic first = currentResult.topics.get(0);
            entry.videoUrl = PrepSeriesRepository.playUrl(first);
            if (entry.posterUrl.isEmpty() && first.thumbnailUrl != null) {
                entry.posterUrl = first.thumbnailUrl;
            }
        } else {
            entry.videoUrl = "";
        }
        boolean added = LocalWatchlist.toggle(this, entry);
        bookmarked = added;
        updateBookmarkIcon();
        AppToast.show(this, added
                ? R.string.prep_banner_added_list
                : R.string.prep_banner_my_list);
    }

    private void updateBookmarkIcon() {
        btnBookmark.setImageResource(bookmarked ? R.drawable.ic_bookmark : R.drawable.ic_bookmark_not);
        btnBookmark.setColorFilter(Color.WHITE);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @NonNull
    private static String safe(@Nullable String value) {
        return value != null ? value : "";
    }

    private class TopicsAdapter extends RecyclerView.Adapter<TopicsAdapter.Holder> {
        private final List<PrepSeriesRepository.Topic> items = new ArrayList<>();
        private final Set<Long> expandedIds = new HashSet<>();

        void setItems(List<PrepSeriesRepository.Topic> next) {
            items.clear();
            expandedIds.clear();
            if (next != null) {
                items.addAll(next);
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_series_topic, parent, false);
            return new Holder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            PrepSeriesRepository.Topic topic = items.get(position);
            holder.txtTopicIndex.setText(String.valueOf(position + 1));
            holder.txtTopicTitle.setText(topic.title != null ? topic.title : "");
            int mins = Math.max(1, Math.round(topic.durationSec / 60f));
            if (topic.durationSec > 0 && topic.durationSec < 60) {
                mins = 1;
            } else if (topic.durationSec <= 0) {
                mins = 1;
            }
            holder.txtTopicDuration.setText(getString(R.string.prep_duration_min, mins));
            holder.progressTopic.setProgress(Math.max(0, Math.min(100, topic.viewedPercentage)));
            if (topic.badge != null && !topic.badge.isEmpty()) {
                holder.txtTopicBadge.setVisibility(View.VISIBLE);
                holder.txtTopicBadge.setText(topic.badge);
            } else {
                holder.txtTopicBadge.setVisibility(View.GONE);
            }
            boolean audioMode = "audio".equals(selectedTab);
            ViewGroup.LayoutParams thumbLp = holder.frameTopicThumb.getLayoutParams();
            thumbLp.width = dp(audioMode ? 78 : 72);
            thumbLp.height = dp(audioMode ? 112 : 96);
            holder.frameTopicThumb.setLayoutParams(thumbLp);
            if (topic.thumbnailUrl != null && !topic.thumbnailUrl.isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(topic.thumbnailUrl)
                        .centerCrop()
                        .into(holder.imgTopicThumb);
            } else {
                holder.imgTopicThumb.setImageResource(R.drawable.default_image);
            }

            View.OnClickListener play = v -> playTopic(topic);
            holder.rowTopicMain.setOnClickListener(play);
            holder.btnPlayTopic.setOnClickListener(play);

            boolean showDownload = !audioMode && !PrepSeriesRepository.downloadUrl(topic).isEmpty();
            holder.frameDownloadTopic.setVisibility(showDownload ? View.VISIBLE : View.GONE);
            holder.btnDownloadTopic.setOnClickListener(v -> downloadTopic(topic));
            bindDownloadUi(holder, topic.id);

            boolean hasExamples = !topic.examples.isEmpty();
            boolean expanded = expandedIds.contains(topic.id);
            if (hasExamples && !audioMode) {
                holder.btnTopicExamplesToggle.setVisibility(View.VISIBLE);
                int count = topic.examplesCount > 0 ? topic.examplesCount : topic.examples.size();
                holder.txtTopicExamplesToggle.setText(count == 1
                        ? getString(R.string.prep_examples_one)
                        : getString(R.string.prep_examples_many, count));
                holder.iconTopicExamplesChevron.setRotation(expanded ? 180f : 0f);
                holder.panelTopicExamples.setVisibility(expanded ? View.VISIBLE : View.GONE);
                holder.btnTopicExamplesToggle.setOnClickListener(v -> {
                    if (expandedIds.contains(topic.id)) {
                        expandedIds.remove(topic.id);
                    } else {
                        expandedIds.add(topic.id);
                    }
                    notifyItemChanged(holder.getBindingAdapterPosition());
                });
                if (expanded) {
                    bindExamplesPanel(holder, topic);
                } else {
                    holder.layoutTopicExamples.removeAllViews();
                }
            } else {
                holder.btnTopicExamplesToggle.setVisibility(View.GONE);
                holder.panelTopicExamples.setVisibility(View.GONE);
                holder.btnTopicExamplesToggle.setOnClickListener(null);
                holder.layoutTopicExamples.removeAllViews();
            }
        }

        private void bindExamplesPanel(@NonNull Holder holder, @NonNull PrepSeriesRepository.Topic topic) {
            holder.layoutTopicExamples.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(holder.itemView.getContext());
            for (PrepSeriesRepository.Example example : topic.examples) {
                View card = inflater.inflate(R.layout.item_series_example, holder.layoutTopicExamples, false);
                TextView label = card.findViewById(R.id.txtExampleLabel);
                TextView title = card.findViewById(R.id.txtExampleTitle);
                ImageView thumb = card.findViewById(R.id.imgExampleThumb);
                label.setText(example.label != null && !example.label.isEmpty()
                        ? example.label
                        : getString(R.string.prep_example_badge));
                title.setText(example.title != null ? example.title : "");
                if (example.thumbnailUrl != null && !example.thumbnailUrl.isEmpty()) {
                    Glide.with(card.getContext())
                            .load(example.thumbnailUrl)
                            .centerCrop()
                            .into(thumb);
                } else {
                    thumb.setImageResource(R.drawable.default_image);
                }
                card.setOnClickListener(v -> playExample(example));
                holder.layoutTopicExamples.addView(card);
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        void notifyDownloadProgress(@NonNull VideoDownloadHelper.Progress progress) {
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).id == progress.topicId) {
                    notifyItemChanged(i);
                    return;
                }
            }
        }

        private void bindDownloadUi(@NonNull Holder holder, long topicId) {
            VideoDownloadHelper.Progress snap = VideoDownloadHelper.snapshot(
                    holder.itemView.getContext(), topicId);
            if (snap != null && snap.running) {
                holder.progressDownloadTopic.setVisibility(View.VISIBLE);
                holder.progressDownloadTopic.setProgress(Math.max(1, snap.percent));
                holder.txtDownloadPercent.setVisibility(View.VISIBLE);
                holder.txtDownloadPercent.setText(snap.percent + "%");
                holder.btnDownloadTopic.setImageResource(R.drawable.ic_download_box);
                holder.btnDownloadTopic.setAlpha(0.35f);
                holder.btnDownloadTopic.setEnabled(false);
            } else if (VideoDownloadHelper.hasLocalFile(holder.itemView.getContext(), topicId)) {
                holder.progressDownloadTopic.setVisibility(View.GONE);
                holder.progressDownloadTopic.setProgress(0);
                holder.txtDownloadPercent.setVisibility(View.GONE);
                holder.btnDownloadTopic.setImageResource(R.drawable.ic_share);
                holder.btnDownloadTopic.setAlpha(1f);
                holder.btnDownloadTopic.setEnabled(true);
            } else {
                holder.progressDownloadTopic.setVisibility(View.GONE);
                holder.progressDownloadTopic.setProgress(0);
                holder.txtDownloadPercent.setVisibility(View.GONE);
                holder.btnDownloadTopic.setImageResource(R.drawable.ic_download_box);
                holder.btnDownloadTopic.setAlpha(1f);
                holder.btnDownloadTopic.setEnabled(true);
            }
        }

        class Holder extends RecyclerView.ViewHolder {
            final View rowTopicMain;
            final TextView txtTopicIndex;
            final View frameTopicThumb;
            final ImageView imgTopicThumb;
            final TextView txtTopicBadge;
            final TextView txtTopicTitle;
            final TextView txtTopicDuration;
            final ProgressBar progressTopic;
            final View frameDownloadTopic;
            final ProgressBar progressDownloadTopic;
            final TextView txtDownloadPercent;
            final ImageView btnDownloadTopic;
            final ImageView btnPlayTopic;
            final View btnTopicExamplesToggle;
            final TextView txtTopicExamplesToggle;
            final ImageView iconTopicExamplesChevron;
            final View panelTopicExamples;
            final LinearLayout layoutTopicExamples;

            Holder(View itemView) {
                super(itemView);
                rowTopicMain = itemView.findViewById(R.id.rowTopicMain);
                txtTopicIndex = itemView.findViewById(R.id.txtTopicIndex);
                frameTopicThumb = itemView.findViewById(R.id.frameTopicThumb);
                imgTopicThumb = itemView.findViewById(R.id.imgTopicThumb);
                txtTopicBadge = itemView.findViewById(R.id.txtTopicBadge);
                txtTopicTitle = itemView.findViewById(R.id.txtTopicTitle);
                txtTopicDuration = itemView.findViewById(R.id.txtTopicDuration);
                progressTopic = itemView.findViewById(R.id.progressTopic);
                frameDownloadTopic = itemView.findViewById(R.id.frameDownloadTopic);
                progressDownloadTopic = itemView.findViewById(R.id.progressDownloadTopic);
                txtDownloadPercent = itemView.findViewById(R.id.txtDownloadPercent);
                btnDownloadTopic = itemView.findViewById(R.id.btnDownloadTopic);
                btnPlayTopic = itemView.findViewById(R.id.btnPlayTopic);
                btnTopicExamplesToggle = itemView.findViewById(R.id.btnTopicExamplesToggle);
                txtTopicExamplesToggle = itemView.findViewById(R.id.txtTopicExamplesToggle);
                iconTopicExamplesChevron = itemView.findViewById(R.id.iconTopicExamplesChevron);
                panelTopicExamples = itemView.findViewById(R.id.panelTopicExamples);
                layoutTopicExamples = itemView.findViewById(R.id.layoutTopicExamples);
            }
        }
    }

    private void downloadTopic(@NonNull PrepSeriesRepository.Topic topic) {
        if (VideoDownloadHelper.isDownloading(topic.id)) {
            AppToast.show(this, R.string.prep_download_already);
            return;
        }
        String url = PrepSeriesRepository.downloadUrl(topic);
        if (url.isEmpty()) {
            AppToast.show(this, R.string.prep_download_missing);
            return;
        }
        if (VideoDownloadHelper.hasLocalFile(this, topic.id)) {
            DownloadQualitySheet.showDownloadedActions(
                    this,
                    () -> shareDownloadedTopic(topic.id),
                    () -> showQualityAndStart(topic, url));
            return;
        }
        showQualityAndStart(topic, url);
    }

    private void showQualityAndStart(
            @NonNull PrepSeriesRepository.Topic topic,
            @NonNull String url) {
        DownloadQualitySheet.show(this, option -> startDownload(topic, url, option.tag));
    }

    private void startDownload(
            @NonNull PrepSeriesRepository.Topic topic,
            @NonNull String url,
            @NonNull String qualityTag) {
        try {
            VideoDownloadHelper.enqueue(this, topic.id, url, topic.title, qualityTag);
            AppToast.show(this, R.string.prep_download_started);
            if (topicsAdapter != null) {
                topicsAdapter.notifyDataSetChanged();
            }
        } catch (Exception e) {
            AppToast.show(this, R.string.prep_download_failed);
        }
    }

    private void shareDownloadedTopic(long topicId) {
        try {
            VideoDownloadHelper.shareLocalVideo(this, topicId);
        } catch (Exception e) {
            AppToast.show(this, R.string.prep_share_failed);
        }
    }

    private void offerShareAfterDownload(long topicId) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        PrepSeriesRepository.Topic topic = findTopicById(topicId);
        DownloadQualitySheet.showDownloadedActions(
                this,
                () -> shareDownloadedTopic(topicId),
                () -> {
                    if (topic == null) {
                        return;
                    }
                    String url = PrepSeriesRepository.downloadUrl(topic);
                    if (!url.isEmpty()) {
                        showQualityAndStart(topic, url);
                    }
                });
    }

    @Nullable
    private PrepSeriesRepository.Topic findTopicById(long topicId) {
        if (currentResult == null) {
            return null;
        }
        for (PrepSeriesRepository.Topic topic : currentResult.topics) {
            if (topic.id == topicId) {
                return topic;
            }
        }
        return null;
    }
}

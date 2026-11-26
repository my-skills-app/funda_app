package com.chill.with.examsaathi;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.facebook.shimmer.ShimmerFrameLayout;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ChapterShlokasActivity extends AppCompatActivity {

    private static final String STATE_SCROLL_POSITION = "chapter_shloks_scroll_position";
    private static final String STATE_SCROLL_OFFSET = "chapter_shloks_scroll_offset";
    private static final String STATE_CHAPTER_ID = "chapter_id";

    public static final String EXTRA_CHAPTER_ID = "chapter_id";
    public static final String EXTRA_CHAPTER_TITLE = "chapter_title";
    public static final String EXTRA_CHAPTER_SUBTITLE = "chapter_subtitle";

    private enum UiState {
        LOADING,
        CONTENT,
        ERROR
    }

    public static void open(Context context, int chapterId, String title, String subtitle) {
        Intent intent = new Intent(context, ChapterShlokasActivity.class);
        intent.putExtra(EXTRA_CHAPTER_ID, chapterId);
        intent.putExtra(EXTRA_CHAPTER_TITLE, title);
        intent.putExtra(EXTRA_CHAPTER_SUBTITLE, subtitle);
        ActivityNav.start(context, intent);
    }

    private SwipeRefreshLayout swipeRefreshChapterShlokas;
    private View chapterShloksToolbar;
    private TextView txtToolbarTitle;
    private TextView txtToolbarSubtitle;
    private ShimmerFrameLayout shimmerShlokaList;
    private TextView txtChapterShlokasError;
    private RecyclerView recyclerShlokas;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<GitaChapterShlokas.Shloka> shlokaList = new ArrayList<>();
    private ShlokasAdapter shlokasAdapter;
    private LinearLayoutManager layoutManager;

    private UiState uiState = UiState.LOADING;
    private int chapterId;
    private String chapterTitle;
    private String chapterSubtitle;
    private int savedScrollPosition;
    private int savedScrollOffset;
    private boolean isLoading;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chapter_shlokas);
        if (savedInstanceState != null) {
            savedScrollPosition = savedInstanceState.getInt(STATE_SCROLL_POSITION, 0);
            savedScrollOffset = savedInstanceState.getInt(STATE_SCROLL_OFFSET, 0);
            chapterId = savedInstanceState.getInt(STATE_CHAPTER_ID, 0);
        }
        bindViews();
        setupStatusBar();
        readIntent();
        setupRecycler();
        swipeRefreshChapterShlokas.setColorSchemeColors(getColor(R.color.app_color));
        swipeRefreshChapterShlokas.setOnRefreshListener(this::refreshShlokas);
        findViewById(R.id.btnBack).setOnClickListener(v -> ActivityNav.finish(this));

        if (shlokaList.isEmpty()) {
            applyUiState(UiState.LOADING);
            loadShlokas();
        } else {
            updateToolbarSubtitle();
            applyUiState(UiState.CONTENT);
            shlokasAdapter.notifyDataSetChanged();
            restoreScrollPosition();
        }
    }

    @Override
    protected void onPause() {
        saveScrollPosition();
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        saveScrollPosition();
        outState.putInt(STATE_SCROLL_POSITION, savedScrollPosition);
        outState.putInt(STATE_SCROLL_OFFSET, savedScrollOffset);
        outState.putInt(STATE_CHAPTER_ID, chapterId);
        super.onSaveInstanceState(outState);
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(Color.TRANSPARENT);
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
        final int horizontalPad = dp(16);
        ViewCompat.setOnApplyWindowInsetsListener(chapterShloksToolbar, (v, insets) -> {
            int statusTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            int bottom = v.getPaddingBottom();
            v.setPadding(horizontalPad, statusTop + dp(10), horizontalPad, bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(chapterShloksToolbar);
    }

    private void bindViews() {
        swipeRefreshChapterShlokas = findViewById(R.id.swipeRefreshChapterShlokas);
        chapterShloksToolbar = findViewById(R.id.chapterShloksToolbar);
        txtToolbarTitle = chapterShloksToolbar.findViewById(R.id.txtToolbarTitle);
        txtToolbarSubtitle = chapterShloksToolbar.findViewById(R.id.txtToolbarSubtitle);
        shimmerShlokaList = (ShimmerFrameLayout) findViewById(R.id.shimmerShlokaList);
        txtChapterShlokasError = findViewById(R.id.txtChapterShlokasError);
        recyclerShlokas = findViewById(R.id.recyclerShlokas);
    }

    private void readIntent() {
        if (chapterId <= 0) {
            chapterId = getIntent().getIntExtra(EXTRA_CHAPTER_ID, 0);
        }
        chapterTitle = getIntent().getStringExtra(EXTRA_CHAPTER_TITLE);
        chapterSubtitle = getIntent().getStringExtra(EXTRA_CHAPTER_SUBTITLE);
        txtToolbarTitle.setText(chapterTitle != null ? chapterTitle : getString(R.string.shloks_screen_title));
        updateToolbarSubtitle();
    }

    private void setupRecycler() {
        layoutManager = new GridLayoutManager(this, 2);
        recyclerShlokas.setLayoutManager(layoutManager);
        shlokasAdapter = new ShlokasAdapter();
        recyclerShlokas.setAdapter(shlokasAdapter);
    }

    private void refreshShlokas() {
        swipeRefreshChapterShlokas.setRefreshing(false);
        saveScrollPosition();
        applyUiState(UiState.LOADING);
        loadShlokas();
    }

    private void loadShlokas() {
        if (chapterId <= 0) {
            applyUiState(UiState.ERROR);
            return;
        }
        if (isLoading) {
            swipeRefreshChapterShlokas.setRefreshing(false);
            return;
        }
        isLoading = true;

        new Thread(() -> {
            try {
                String json = SecureHttpClient.get(
                        getApplicationContext(),
                        GitaChapterShlokas.shlokasUrl(chapterId));
                List<GitaChapterShlokas.Shloka> shlokas = GitaChapterShlokas.parse(json);
                mainHandler.post(() -> onShlokasLoaded(shlokas));
            } catch (Exception e) {
                mainHandler.post(this::onShlokasError);
            }
        }).start();
    }

    private void onShlokasLoaded(List<GitaChapterShlokas.Shloka> shlokas) {
        isLoading = false;
        swipeRefreshChapterShlokas.setRefreshing(false);

        shlokaList.clear();
        shlokaList.addAll(shlokas);
        shlokasAdapter.notifyDataSetChanged();

        if (shlokaList.isEmpty()) {
            applyUiState(UiState.ERROR);
        } else {
            updateToolbarSubtitle();
            applyUiState(UiState.CONTENT);
            restoreScrollPosition();
        }
    }

    private void onShlokasError() {
        isLoading = false;
        swipeRefreshChapterShlokas.setRefreshing(false);
        if (shlokaList.isEmpty()) {
            applyUiState(UiState.ERROR);
        } else {
            applyUiState(UiState.CONTENT);
            restoreScrollPosition();
            Toast.makeText(this, R.string.shlokas_load_error, Toast.LENGTH_SHORT).show();
        }
    }

    private void updateToolbarSubtitle() {
        if (shlokaList.isEmpty()) {
            txtToolbarSubtitle.setText(
                    chapterSubtitle != null ? chapterSubtitle : "");
        } else {
            txtToolbarSubtitle.setText(getString(R.string.shlokas_count, shlokaList.size()));
        }
    }

    private void applyUiState(UiState state) {
        uiState = state;
        switch (state) {
            case LOADING:
                txtChapterShlokasError.setVisibility(View.GONE);
                recyclerShlokas.setVisibility(View.GONE);
                showShimmer(true);
                break;
            case CONTENT:
                txtChapterShlokasError.setVisibility(View.GONE);
                showShimmer(false);
                recyclerShlokas.setVisibility(View.VISIBLE);
                break;
            case ERROR:
                showShimmer(false);
                recyclerShlokas.setVisibility(View.GONE);
                txtChapterShlokasError.setVisibility(View.VISIBLE);
                break;
            default:
                break;
        }
    }

    private void showShimmer(boolean show) {
        if (shimmerShlokaList == null) {
            return;
        }
        if (show) {
            shimmerShlokaList.setVisibility(View.VISIBLE);
            shimmerShlokaList.startShimmer();
        } else {
            shimmerShlokaList.stopShimmer();
            shimmerShlokaList.setVisibility(View.GONE);
        }
    }

    private void saveScrollPosition() {
        if (layoutManager == null || uiState != UiState.CONTENT) {
            return;
        }
        savedScrollPosition = layoutManager.findFirstVisibleItemPosition();
        View firstVisible = layoutManager.findViewByPosition(savedScrollPosition);
        savedScrollOffset = firstVisible != null ? firstVisible.getTop() : 0;
    }

    private void restoreScrollPosition() {
        if (layoutManager == null || shlokaList.isEmpty()) {
            return;
        }
        final int position = Math.min(savedScrollPosition, Math.max(0, shlokaList.size() - 1));
        final int offset = savedScrollOffset;
        recyclerShlokas.post(() ->
                layoutManager.scrollToPositionWithOffset(position, offset));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void openMovieView(GitaChapterShlokas.Shloka shloka) {
        if (shloka.videoUrl == null || shloka.videoUrl.isEmpty()) {
            Toast.makeText(this, R.string.video_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }
        ActivityNav.start(this, GitaShlokaViewActivity.newGitaShlokaIntent(
                this,
                shloka,
                shlokaList,
                chapterTitle != null ? chapterTitle : "",
                chapterId));
    }

    @Override
    public void onBackPressed() {
        ActivityNav.finish(this);
    }

    private final class ShlokasAdapter extends RecyclerView.Adapter<ShlokasAdapter.ShlokaHolder> {

        @NonNull
        @Override
        public ShlokaHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View item = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_gita_shloka_grid, parent, false);
            return new ShlokaHolder(item);
        }

        @Override
        public void onBindViewHolder(@NonNull ShlokaHolder holder, int position) {
            GitaChapterShlokas.Shloka shloka = shlokaList.get(position);
            holder.txtTitle.setText(shloka.title);
            holder.txtDescription.setText(GitaChapterShlokas.shortDescription(shloka));
            if (shloka.image != null && !shloka.image.isEmpty()) {
                Glide.with(ChapterShlokasActivity.this)
                        .load(shloka.image)
                        .centerCrop()
                        .placeholder(R.color.view_bg_color)
                        .into(holder.imgShloka);
            } else {
                holder.imgShloka.setImageResource(R.color.view_bg_color);
            }
            holder.itemView.setOnClickListener(v -> openMovieView(shloka));
        }

        @Override
        public int getItemCount() {
            return shlokaList.size();
        }

        class ShlokaHolder extends RecyclerView.ViewHolder {
            final android.widget.ImageView imgShloka;
            final TextView txtTitle;
            final TextView txtDescription;

            ShlokaHolder(@NonNull View itemView) {
                super(itemView);
                imgShloka = itemView.findViewById(R.id.imgShloka);
                txtTitle = itemView.findViewById(R.id.txtTitle);
                txtDescription = itemView.findViewById(R.id.txtDescription);
            }
        }
    }
}

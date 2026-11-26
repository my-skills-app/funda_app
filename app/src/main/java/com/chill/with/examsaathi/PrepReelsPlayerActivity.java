package com.chill.with.examsaathi;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

/**
 * Vertical Reels-style player: clean full-bleed video; chrome (back, title, seek, controls)
 * only appears on tap with the Media3 controller — no permanent overlays.
 */
public class PrepReelsPlayerActivity extends AppCompatActivity {

    public static final String EXTRA_PLAYLIST_JSON = "prep_reels_playlist_json";
    public static final String EXTRA_START_ID = "prep_reels_start_id";
    public static final String EXTRA_SERIES_TITLE = "prep_reels_series_title";

    public static void open(
            @NonNull Context context,
            @NonNull String playlistJson,
            @Nullable String startId,
            @Nullable String seriesTitle) {
        Intent intent = new Intent(context, PrepReelsPlayerActivity.class);
        intent.putExtra(EXTRA_PLAYLIST_JSON, playlistJson);
        intent.putExtra(EXTRA_START_ID, startId != null ? startId : "");
        intent.putExtra(EXTRA_SERIES_TITLE, seriesTitle != null ? seriesTitle : "");
        ActivityNav.start(context, intent);
    }

    private ViewPager2 pagerReels;
    private ExoPlayer player;
    private ReelsAdapter adapter;
    private final List<GitaChapterShlokas.Shloka> items = new ArrayList<>();
    private String seriesTitle = "";
    private int currentIndex = 0;
    private boolean advancingFromEnd;
    private int resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT;
    @Nullable
    private PlayerView attachedPlayerView;

    private final ViewPager2.OnPageChangeCallback pageCallback = new ViewPager2.OnPageChangeCallback() {
        @Override
        public void onPageSelected(int position) {
            currentIndex = position;
            playAt(position);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prep_reels_player);
        setupSystemBars();
        seriesTitle = safe(getIntent().getStringExtra(EXTRA_SERIES_TITLE));
        items.clear();
        items.addAll(GitaPlayerPlaylist.parsePlayable(getIntent().getStringExtra(EXTRA_PLAYLIST_JSON)));
        if (items.isEmpty()) {
            AppToast.show(this, R.string.prep_local_video_error);
            finish();
            return;
        }
        String startId = safe(getIntent().getStringExtra(EXTRA_START_ID));
        currentIndex = Math.max(0, GitaPlayerPlaylist.findCurrentIndex(items, startId, null));
        if (currentIndex < 0) {
            currentIndex = 0;
        }

        pagerReels = findViewById(R.id.pagerReels);

        player = FastPlaybackExoPlayer.create(this);
        player.setRepeatMode(Player.REPEAT_MODE_OFF);
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState != Player.STATE_ENDED || advancingFromEnd) {
                    return;
                }
                if (currentIndex + 1 < items.size()) {
                    advancingFromEnd = true;
                    pagerReels.setCurrentItem(currentIndex + 1, true);
                    pagerReels.postDelayed(() -> advancingFromEnd = false, 400);
                }
            }

            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
                AppToast.show(PrepReelsPlayerActivity.this, R.string.playback_failed);
            }
        });

        adapter = new ReelsAdapter();
        pagerReels.setOrientation(ViewPager2.ORIENTATION_VERTICAL);
        pagerReels.setOffscreenPageLimit(1);
        pagerReels.setAdapter(adapter);
        pagerReels.registerOnPageChangeCallback(pageCallback);
        pagerReels.setCurrentItem(currentIndex, false);
        pagerReels.post(() -> playAt(currentIndex));
    }

    private void playAt(int position) {
        if (player == null || position < 0 || position >= items.size()) {
            return;
        }
        GitaChapterShlokas.Shloka item = items.get(position);
        String url = item.videoUrl != null ? item.videoUrl.trim() : "";
        if (url.isEmpty()) {
            return;
        }
        PlayerView target = findPlayerViewForPosition(position);
        if (attachedPlayerView != null && attachedPlayerView != target) {
            attachedPlayerView.setPlayer(null);
        }
        attachedPlayerView = target;
        if (target != null) {
            target.setPlayer(player);
            target.setResizeMode(resizeMode);
            bindControllerChrome(target, item, position);
            // Clean first look — chrome only after tap
            target.setControllerAutoShow(false);
            target.hideController();
        }
        MediaItem mediaItem = MediaItem.fromUri(Uri.parse(url));
        player.setMediaItem(mediaItem);
        player.prepare();
        player.seekTo(0L);
        player.setPlayWhenReady(true);
    }

    private void bindControllerChrome(
            @NonNull PlayerView playerView,
            @NonNull GitaChapterShlokas.Shloka item,
            int position) {
        View topBar = playerView.findViewById(R.id.player_top_bar);
        if (topBar != null) {
            ViewCompat.setOnApplyWindowInsetsListener(topBar, (v, insets) -> {
                Insets bars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
                v.setPadding(v.getPaddingLeft(), bars.top + dp(4), v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
            ViewCompat.requestApplyInsets(topBar);
        }

        TextView fileName = playerView.findViewById(R.id.file_name_tv);
        TextView txtSeries = playerView.findViewById(R.id.txtReelSeries);
        TextView txtTitle = playerView.findViewById(R.id.txtReelTitle);
        TextView txtIndex = playerView.findViewById(R.id.txtReelIndex);
        String title = item.title != null ? item.title : "";
        if (fileName != null) {
            fileName.setText(!seriesTitle.isEmpty() ? seriesTitle : title);
        }
        if (txtSeries != null) {
            txtSeries.setText(seriesTitle);
            txtSeries.setVisibility(seriesTitle.isEmpty() ? View.GONE : View.VISIBLE);
        }
        if (txtTitle != null) {
            txtTitle.setText(title);
        }
        if (txtIndex != null) {
            txtIndex.setText((position + 1) + " / " + items.size());
        }

        ImageButton back = playerView.findViewById(R.id.back_button);
        if (back != null) {
            back.setOnClickListener(v -> finish());
        }

        ImageButton backward = playerView.findViewById(R.id.backward_10);
        if (backward != null) {
            backward.setOnClickListener(v -> {
                if (player != null) {
                    player.seekTo(Math.max(0, player.getCurrentPosition() - 10_000));
                }
            });
        }
        ImageButton forward = playerView.findViewById(R.id.forward_10);
        if (forward != null) {
            forward.setOnClickListener(v -> {
                if (player != null) {
                    long dur = player.getDuration();
                    long next = player.getCurrentPosition() + 10_000;
                    if (dur > 0) {
                        next = Math.min(next, dur);
                    }
                    player.seekTo(next);
                }
            });
        }

        ImageButton fit = playerView.findViewById(R.id.fit_screen_btn);
        if (fit != null) {
            fit.setOnClickListener(v -> {
                if (playerView.getResizeMode() == AspectRatioFrameLayout.RESIZE_MODE_FIT) {
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM;
                    fit.setImageResource(R.drawable.crop_5_4);
                    Toast.makeText(this, R.string.player_zoom, Toast.LENGTH_SHORT).show();
                } else if (playerView.getResizeMode() == AspectRatioFrameLayout.RESIZE_MODE_ZOOM) {
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL;
                    fit.setImageResource(R.drawable.fit_screen);
                    Toast.makeText(this, R.string.player_fill, Toast.LENGTH_SHORT).show();
                } else {
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT;
                    fit.setImageResource(R.drawable.crop_free);
                    Toast.makeText(this, R.string.player_fit, Toast.LENGTH_SHORT).show();
                }
                playerView.setResizeMode(resizeMode);
            });
        }

        ImageButton quality = playerView.findViewById(R.id.quality_selection_btn);
        if (quality != null) {
            quality.setOnClickListener(v ->
                    VideoQualityDialog.show(this, player, null));
        }

        ImageButton rotate = playerView.findViewById(R.id.screen_rotate_btn);
        if (rotate != null) {
            rotate.setVisibility(View.VISIBLE);
            rotate.setOnClickListener(v -> {
                int orientation = getResources().getConfiguration().orientation;
                if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                } else {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                }
            });
        }

        // Custom speed bottom sheet (same pattern as quality) — overrides Media3 settings popup
        View settings = playerView.findViewById(androidx.media3.ui.R.id.exo_settings);
        if (settings != null) {
            settings.setVisibility(View.VISIBLE);
            settings.setOnClickListener(v ->
                    PlaybackSpeedDialog.show(this, player, null));
        }
    }

    @Nullable
    private PlayerView findPlayerViewForPosition(int position) {
        RecyclerView rv = (RecyclerView) pagerReels.getChildAt(0);
        if (rv == null) {
            return null;
        }
        RecyclerView.ViewHolder holder = rv.findViewHolderForAdapterPosition(position);
        if (holder instanceof ReelHolder) {
            return ((ReelHolder) holder).playerView;
        }
        return null;
    }

    private void setupSystemBars() {
        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);
        window.setBackgroundDrawableResource(android.R.color.black);
        window.getDecorView().setBackgroundColor(Color.BLACK);
        window.setStatusBarColor(Color.BLACK);
        window.setNavigationBarColor(Color.BLACK);
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.getAttributes().layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        // Edge-to-edge layout, solid black chrome — no see-through to previous activity
        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        WindowInsetsControllerCompat insetsController =
                WindowCompat.getInsetsController(window, window.getDecorView());
        if (insetsController != null) {
            insetsController.setAppearanceLightStatusBars(false);
            insetsController.setAppearanceLightNavigationBars(false);
            insetsController.show(WindowInsetsCompat.Type.statusBars());
            insetsController.hide(WindowInsetsCompat.Type.navigationBars());
            insetsController.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) {
            player.setPlayWhenReady(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (player != null) {
            player.setPlayWhenReady(true);
        }
        setupSystemBars();
    }

    @Override
    protected void onDestroy() {
        if (pagerReels != null) {
            pagerReels.unregisterOnPageChangeCallback(pageCallback);
        }
        if (attachedPlayerView != null) {
            attachedPlayerView.setPlayer(null);
            attachedPlayerView = null;
        }
        if (player != null) {
            player.release();
            player = null;
        }
        super.onDestroy();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @NonNull
    private static String safe(@Nullable String value) {
        return value != null ? value : "";
    }

    private class ReelsAdapter extends RecyclerView.Adapter<ReelHolder> {
        @NonNull
        @Override
        public ReelHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_prep_reel_page, parent, false);
            return new ReelHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ReelHolder holder, int position) {
            GitaChapterShlokas.Shloka item = items.get(position);
            String image = item.image != null ? item.image : "";
            if (!image.isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(image)
                        .centerCrop()
                        .into(holder.imgReelThumb);
            } else {
                holder.imgReelThumb.setImageResource(R.color.view_bg_color);
            }
            holder.playerView.setPlayer(null);
            holder.playerView.setBackgroundColor(Color.BLACK);
            holder.playerView.setUseController(true);
            holder.playerView.setControllerHideOnTouch(true);
            holder.playerView.setControllerAutoShow(false);
            holder.playerView.setControllerShowTimeoutMs(2500);
            holder.playerView.setResizeMode(resizeMode);
            holder.playerView.hideController();
            ImageButton fit = holder.playerView.findViewById(R.id.fit_screen_btn);
            if (fit != null) {
                if (resizeMode == AspectRatioFrameLayout.RESIZE_MODE_FIT) {
                    fit.setImageResource(R.drawable.crop_free);
                } else if (resizeMode == AspectRatioFrameLayout.RESIZE_MODE_ZOOM) {
                    fit.setImageResource(R.drawable.crop_5_4);
                } else {
                    fit.setImageResource(R.drawable.fit_screen);
                }
            }
        }

        @Override
        public void onViewRecycled(@NonNull ReelHolder holder) {
            if (attachedPlayerView == holder.playerView) {
                holder.playerView.setPlayer(null);
                attachedPlayerView = null;
            }
            super.onViewRecycled(holder);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    private static class ReelHolder extends RecyclerView.ViewHolder {
        final ImageView imgReelThumb;
        final PlayerView playerView;

        ReelHolder(@NonNull View itemView) {
            super(itemView);
            imgReelThumb = itemView.findViewById(R.id.imgReelThumb);
            playerView = itemView.findViewById(R.id.playerReel);
        }
    }
}

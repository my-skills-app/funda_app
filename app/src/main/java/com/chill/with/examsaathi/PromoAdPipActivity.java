package com.chill.with.examsaathi;

import android.app.PictureInPictureParams;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.util.Rational;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.Player;
import androidx.media3.common.VideoSize;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;

import java.lang.ref.WeakReference;
import java.util.Locale;

/** Plays dashboard promo ad in PiP with correct video aspect ratio (black background). */
@UnstableApi
public class PromoAdPipActivity extends AppCompatActivity {

    public static final String EXTRA_VIDEO_URL = "promo_video_url";

    private static final Rational DEFAULT_PIP_RATIO = new Rational(16, 9);

    @Nullable
    private static WeakReference<PromoAdPipActivity> active;

    private ExoPlayer exoPlayer;
    private PlayerView playerView;
    private boolean pipEntered;
    private boolean pipRequested;
    private int videoWidth;
    private int videoHeight;

    public static void stopIfRunning() {
        PromoAdPipActivity instance = active != null ? active.get() : null;
        if (instance != null && !instance.isFinishing()) {
            instance.finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(0, 0);
        active = new WeakReference<>(this);

        String url = getIntent().getStringExtra(EXTRA_VIDEO_URL);
        if (url == null || url.trim().isEmpty() || !supportsPictureInPicture()) {
            finish();
            return;
        }

        setContentView(R.layout.activity_promo_ad_pip);

        playerView = findViewById(R.id.promo_player_view);
        playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);

        exoPlayer = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(exoPlayer);
        exoPlayer.setMediaItem(buildMediaItem(url.trim()));
        exoPlayer.setPlayWhenReady(true);
        exoPlayer.prepare();
        exoPlayer.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_READY && !pipRequested) {
                    enterPictureInPictureModeSafely();
                }
            }

            @Override
            public void onVideoSizeChanged(VideoSize videoSize) {
                videoWidth = videoSize.width;
                videoHeight = videoSize.height;
                applyPipAspectRatioParams();
            }
        });

        applyPipAspectRatioParams();
    }

    @Override
    public void onPictureInPictureModeChanged(
            boolean isInPictureInPictureMode, Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
        if (isInPictureInPictureMode) {
            pipEntered = true;
            if (playerView != null && exoPlayer != null && playerView.getPlayer() != exoPlayer) {
                playerView.setPlayer(exoPlayer);
            }
            if (playerView != null) {
                playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
                playerView.requestLayout();
            }
        } else if (pipEntered && !isFinishing()) {
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        if (active != null && active.get() == this) {
            active = null;
        }
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
        if (playerView != null) {
            playerView.setPlayer(null);
        }
        super.onDestroy();
    }

    private void enterPictureInPictureModeSafely() {
        if (pipRequested || !supportsPictureInPicture() || exoPlayer == null) {
            return;
        }
        pipRequested = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                enterPictureInPictureMode(buildPipParams());
            } catch (IllegalStateException e) {
                pipRequested = false;
                Toast.makeText(this, R.string.player_pip_unavailable, Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            finish();
        }
    }

    private void applyPipAspectRatioParams() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setPictureInPictureParams(buildPipParams());
        }
    }

    private PictureInPictureParams buildPipParams() {
        PictureInPictureParams.Builder builder = new PictureInPictureParams.Builder()
                .setAspectRatio(resolvePipAspectRatio());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(true);
        }
        return builder.build();
    }

    private Rational resolvePipAspectRatio() {
        if (videoWidth > 0 && videoHeight > 0) {
            return toPipRational(videoWidth, videoHeight);
        }
        return DEFAULT_PIP_RATIO;
    }

    private static Rational toPipRational(int width, int height) {
        int w = width;
        int h = height;
        int g = gcd(w, h);
        w /= g;
        h /= g;
        while (w > 239 || h > 239) {
            w = Math.max(1, w / 2);
            h = Math.max(1, h / 2);
        }
        return new Rational(w, h);
    }

    private static int gcd(int a, int b) {
        while (b != 0) {
            int t = b;
            b = a % b;
            a = t;
        }
        return Math.max(1, a);
    }

    private boolean supportsPictureInPicture() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && getPackageManager().hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE);
    }

    private static MediaItem buildMediaItem(String url) {
        MediaItem.Builder builder = new MediaItem.Builder().setUri(url);
        String lower = url.toLowerCase(Locale.ROOT);
        if (lower.contains(".m3u8")) {
            builder.setMimeType(MimeTypes.APPLICATION_M3U8);
        }
        return builder.build();
    }
}

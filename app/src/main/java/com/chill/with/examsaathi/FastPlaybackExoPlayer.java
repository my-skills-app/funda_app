package com.chill.with.examsaathi;

import android.content.Context;

import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.SeekParameters;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;

/** ExoPlayer tuned for fast start and faster in-track seeks. */
@UnstableApi
public final class FastPlaybackExoPlayer {

    private static final int MIN_BUFFER_MS = 1_500;
    private static final int MAX_BUFFER_MS = 50_000;
    private static final int BUFFER_FOR_PLAYBACK_MS = 350;
    private static final int BUFFER_AFTER_REBUFFER_MS = 500;

    private FastPlaybackExoPlayer() {}

    public static ExoPlayer create(Context context) {
        DefaultLoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                        MIN_BUFFER_MS,
                        MAX_BUFFER_MS,
                        BUFFER_FOR_PLAYBACK_MS,
                        BUFFER_AFTER_REBUFFER_MS)
                .setPrioritizeTimeOverSizeThresholds(true)
                .build();
        DefaultMediaSourceFactory mediaSourceFactory = new DefaultMediaSourceFactory(
                PlaybackCacheHelper.getCacheDataSourceFactory(context));
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                .build();
        ExoPlayer player = new ExoPlayer.Builder(context)
                .setMediaSourceFactory(mediaSourceFactory)
                .setLoadControl(loadControl)
                .setAudioAttributes(audioAttributes, /* handleAudioFocus= */ true)
                .setSeekBackIncrementMs(10_000L)
                .setSeekForwardIncrementMs(10_000L)
                .build();
        player.setSeekParameters(SeekParameters.CLOSEST_SYNC);
        return player;
    }
}

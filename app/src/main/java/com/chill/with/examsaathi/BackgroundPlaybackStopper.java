package com.chill.with.examsaathi;

import android.content.Context;
import android.content.Intent;

/** Stops background Gita audio services before video playback starts. */
public final class BackgroundPlaybackStopper {

    private BackgroundPlaybackStopper() {}

    public static void stopAllBackgroundAudio(Context context) {
        if (context == null) {
            return;
        }
        Context app = context.getApplicationContext();
        Intent stopFull = new Intent(app, GitaFullAudioPlaybackService.class);
        stopFull.setAction(GitaFullAudioPlaybackService.ACTION_STOP);
        app.startService(stopFull);

        Intent stopShloka = new Intent(app, GitaShlokaAudioPlaybackService.class);
        stopShloka.setAction(GitaShlokaAudioPlaybackService.ACTION_STOP);
        app.startService(stopShloka);
    }
}

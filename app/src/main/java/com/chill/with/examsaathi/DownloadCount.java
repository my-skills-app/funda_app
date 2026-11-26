package com.chill.with.examsaathi;

import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.util.Log;

import org.json.JSONObject;

/**
 * DownloadCount
 * -------------
 * Sends app + device info to API endpoint (Android 8–15)
 *
 * Usage:
 *     DownloadCount.send(context);
 */
public class DownloadCount {

    private static final String TAG = "DownloadCount";

    public static void send(Context context) {
        new Thread(() -> {
            try {
                String packageName = context.getPackageName();
                PackageManager pm = context.getPackageManager();
                CharSequence appNameSeq = pm.getApplicationLabel(context.getApplicationInfo());
                String appName = appNameSeq != null ? appNameSeq.toString() : "Unknown App";

                String deviceId = Settings.Secure.getString(
                        context.getContentResolver(),
                        Settings.Secure.ANDROID_ID
                );

                JSONObject json = new JSONObject();
                json.put("deviceId", deviceId);
                json.put("packageName", packageName);
                json.put("appName", appName);

                Log.d(TAG, "Sending JSON: " + json);

                String response = SecureHttpClient.postJson(context, ApiConfig.downloadTrackUrl(), json.toString());
                Log.i(TAG, "DownloadCount POST done: " + response);

            } catch (Exception e) {
                Log.e(TAG, "DownloadCount Error: ", e);
            }
        }).start();
    }
}

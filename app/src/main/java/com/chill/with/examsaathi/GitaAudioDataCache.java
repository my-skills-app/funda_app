package com.chill.with.examsaathi;

import android.content.Context;
import android.content.SharedPreferences;

/** Caches Gita chapters and per-chapter shloka JSON for home audio preload. */
public final class GitaAudioDataCache {

    private static final String PREFS = "gita_audio_data_cache";
    private static final String KEY_CHAPTERS_JSON = "chapters_json";
    private static final String KEY_SHLOKAS_PREFIX = "shlokas_chapter_";

    private GitaAudioDataCache() {}

    public static void saveChaptersJson(Context context, String json) {
        if (context == null || json == null || json.isEmpty()) {
            return;
        }
        prefs(context).edit().putString(KEY_CHAPTERS_JSON, json).apply();
    }

    @androidx.annotation.Nullable
    public static String loadChaptersJson(Context context) {
        if (context == null) {
            return null;
        }
        String json = prefs(context).getString(KEY_CHAPTERS_JSON, null);
        return json == null || json.isEmpty() ? null : json;
    }

    public static void saveShlokasJson(Context context, int chapterId, String json) {
        if (context == null || chapterId <= 0 || json == null || json.isEmpty()) {
            return;
        }
        prefs(context).edit().putString(KEY_SHLOKAS_PREFIX + chapterId, json).apply();
    }

    @androidx.annotation.Nullable
    public static String loadShlokasJson(Context context, int chapterId) {
        if (context == null || chapterId <= 0) {
            return null;
        }
        String json = prefs(context).getString(KEY_SHLOKAS_PREFIX + chapterId, null);
        return json == null || json.isEmpty() ? null : json;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}

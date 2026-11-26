package com.chill.with.examsaathi;

import android.content.Context;
import android.content.SharedPreferences;

/** Persists full-Gita audio position so playback can resume after app restart. */
public final class GitaFullAudioProgressStore {

    private static final String PREFS = "gita_full_audio_progress";
    private static final String KEY_API_CHAPTER_ID = "api_chapter_id";
    private static final String KEY_CHAPTER_NUMBER = "chapter_number";
    private static final String KEY_SHLOKA_IN_CHAPTER = "shloka_in_chapter";
    private static final String KEY_POSITION_MS = "position_ms";
    private static final String KEY_SHLOKA_TITLE = "shloka_title";
    private static final String KEY_HAS_PROGRESS = "has_progress";

    private GitaFullAudioProgressStore() {}

    public static final class SavedProgress {
        public int apiChapterId;
        public int chapterNumber;
        public int shlokaInChapter;
        public long positionMs;
        public String shlokaTitle = "";
    }

    public static boolean hasSavedProgress(Context context) {
        return context != null
                && context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_HAS_PROGRESS, false);
    }

    public static SavedProgress load(Context context) {
        if (context == null || !hasSavedProgress(context)) {
            return null;
        }
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        SavedProgress saved = new SavedProgress();
        saved.apiChapterId = prefs.getInt(KEY_API_CHAPTER_ID, 0);
        saved.chapterNumber = prefs.getInt(KEY_CHAPTER_NUMBER, 0);
        saved.shlokaInChapter = prefs.getInt(KEY_SHLOKA_IN_CHAPTER, 1);
        saved.positionMs = prefs.getLong(KEY_POSITION_MS, 0L);
        saved.shlokaTitle = prefs.getString(KEY_SHLOKA_TITLE, "");
        if (saved.apiChapterId <= 0 || saved.shlokaInChapter < 1) {
            return null;
        }
        return saved;
    }

    public static void save(
            Context context,
            int apiChapterId,
            int chapterNumber,
            int shlokaInChapter,
            long positionMs,
            String shlokaTitle) {
        if (context == null || apiChapterId <= 0 || shlokaInChapter < 1) {
            return;
        }
        context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_HAS_PROGRESS, true)
                .putInt(KEY_API_CHAPTER_ID, apiChapterId)
                .putInt(KEY_CHAPTER_NUMBER, chapterNumber)
                .putInt(KEY_SHLOKA_IN_CHAPTER, shlokaInChapter)
                .putLong(KEY_POSITION_MS, Math.max(0L, positionMs))
                .putString(KEY_SHLOKA_TITLE, shlokaTitle != null ? shlokaTitle : "")
                .apply();
    }

    public static void clear(Context context) {
        if (context == null) {
            return;
        }
        context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply();
    }
}

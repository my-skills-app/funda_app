package com.chill.with.examsaathi;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Local-only Gita shloka watchlist (no server). */
public final class LocalWatchlist {

    private static final String PREFS = "local_watchlist_prefs";
    private static final String KEY_ITEMS = "gita_watchlist_items";

    private LocalWatchlist() {}

    public static class Entry {
        public String title;
        public String posterUrl;
        public String description;
        public String videoUrl;
        public String videoid;
        /** "video" or "audio" — used when opening from Saved/Watchlist. */
        public String mediaType = "video";
        public long savedAt;
    }

    public static List<Entry> getAll(Context context) {
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_ITEMS, "[]");
        try {
            Type type = new TypeToken<List<Entry>>() {}.getType();
            List<Entry> list = new Gson().fromJson(json, type);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private static void saveAll(Context context, List<Entry> entries) {
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_ITEMS, new Gson().toJson(entries)).apply();
    }

    public static boolean isSaved(Context context, String title) {
        if (title == null || title.trim().isEmpty()) {
            return false;
        }
        String key = title.trim();
        for (Entry entry : getAll(context)) {
            if (entry.title != null && entry.title.trim().equals(key)) {
                return true;
            }
        }
        return false;
    }

    public static boolean toggle(Context context, Entry entry) {
        if (entry == null || entry.title == null || entry.title.trim().isEmpty()) {
            return false;
        }
        List<Entry> items = new ArrayList<>(getAll(context));
        String key = entry.title.trim();
        Iterator<Entry> it = items.iterator();
        while (it.hasNext()) {
            Entry existing = it.next();
            if (existing.title != null && existing.title.trim().equals(key)) {
                it.remove();
                saveAll(context, items);
                return false;
            }
        }
        entry.title = key;
        entry.savedAt = System.currentTimeMillis();
        items.add(0, entry);
        saveAll(context, items);
        return true;
    }

    public static boolean remove(Context context, String title) {
        if (title == null || title.trim().isEmpty()) {
            return false;
        }
        List<Entry> items = new ArrayList<>(getAll(context));
        boolean removed = false;
        Iterator<Entry> it = items.iterator();
        String key = title.trim();
        while (it.hasNext()) {
            Entry existing = it.next();
            String existingTitle = existing.title != null ? existing.title.trim() : "";
            if (key.equals(existingTitle)) {
                it.remove();
                removed = true;
                break;
            }
        }
        if (removed) {
            saveAll(context, items);
        }
        return removed;
    }

    public static Entry fromShloka(GitaChapterShlokas.Shloka shloka) {
        Entry entry = new Entry();
        if (shloka == null) {
            return entry;
        }
        entry.title = shloka.title != null ? shloka.title : "";
        entry.posterUrl = shloka.image != null ? shloka.image : "";
        entry.description = GitaChapterShlokas.displayDescription(shloka);
        entry.videoUrl = shloka.videoUrl != null ? shloka.videoUrl : "";
        entry.videoid = shloka.id != null ? shloka.id : "";
        return entry;
    }

    public static Entry fromIntent(android.content.Intent intent) {
        Entry entry = new Entry();
        if (intent == null) {
            return entry;
        }
        entry.title = intent.getStringExtra("title") != null ? intent.getStringExtra("title") : "";
        entry.posterUrl = intent.getStringExtra("posterUrl") != null ? intent.getStringExtra("posterUrl") : "";
        entry.description = intent.getStringExtra("description") != null ? intent.getStringExtra("description") : "";
        entry.videoUrl = intent.getStringExtra("videoUrl") != null ? intent.getStringExtra("videoUrl") : "";
        entry.videoid = intent.getStringExtra("videoid") != null ? intent.getStringExtra("videoid") : "";
        return entry;
    }
}

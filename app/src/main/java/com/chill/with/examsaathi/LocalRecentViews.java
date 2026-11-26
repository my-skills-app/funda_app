package com.chill.with.examsaathi;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Local recently viewed Gita shloka videos (no server). */
public final class LocalRecentViews {

    private static final String PREFS = "local_recent_views_prefs";
    private static final String KEY_ITEMS = "gita_recent_view_items";
    private static final int MAX_ITEMS = 20;

    private LocalRecentViews() {}

    public static class Entry {
        public String title;
        public String posterUrl;
        public String description;
        public String videoUrl;
        public String videoid;
        public long viewedAt;
    }

    public static List<Entry> getAll(Context context) {
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
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
        context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_ITEMS, new Gson().toJson(entries))
                .apply();
    }

    public static void add(Context context, Entry entry) {
        if (entry == null || entry.title == null || entry.title.trim().isEmpty()) {
            return;
        }
        List<Entry> items = new ArrayList<>(getAll(context));
        String key = entry.title.trim();
        Iterator<Entry> it = items.iterator();
        while (it.hasNext()) {
            Entry existing = it.next();
            if (existing.title != null && existing.title.trim().equalsIgnoreCase(key)) {
                it.remove();
                break;
            }
        }
        entry.title = key;
        entry.viewedAt = System.currentTimeMillis();
        items.add(0, entry);
        while (items.size() > MAX_ITEMS) {
            items.remove(items.size() - 1);
        }
        saveAll(context, items);
    }

    public static Entry fromIntent(Intent intent) {
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
}

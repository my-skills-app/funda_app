package com.chill.with.examsaathi;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Local recent shloka search history. */
public final class LocalRecentSearch {

    private static final String PREFS = "recent_search_prefs";
    private static final String KEY_ITEMS = "recent_search_titles";
    private static final int MAX_ITEMS = 15;

    private LocalRecentSearch() {}

    public static List<String> getAll(Context context) {
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_ITEMS, "[]");
        try {
            Type type = new TypeToken<List<String>>() {}.getType();
            List<String> list = new Gson().fromJson(json, type);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private static void saveAll(Context context, List<String> items) {
        context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_ITEMS, new Gson().toJson(items))
                .apply();
    }

    public static void add(Context context, String title) {
        if (title == null) {
            return;
        }
        String key = title.trim();
        if (key.isEmpty()) {
            return;
        }
        List<String> items = new ArrayList<>(getAll(context));
        Iterator<String> it = items.iterator();
        while (it.hasNext()) {
            String existing = it.next();
            if (existing != null && existing.trim().equalsIgnoreCase(key)) {
                it.remove();
                break;
            }
        }
        items.add(0, key);
        while (items.size() > MAX_ITEMS) {
            items.remove(items.size() - 1);
        }
        saveAll(context, items);
    }

    public static void remove(Context context, String title) {
        if (title == null || title.trim().isEmpty()) {
            return;
        }
        String key = title.trim();
        List<String> items = new ArrayList<>(getAll(context));
        boolean removed = false;
        Iterator<String> it = items.iterator();
        while (it.hasNext()) {
            String existing = it.next();
            if (existing != null && existing.trim().equalsIgnoreCase(key)) {
                it.remove();
                removed = true;
                break;
            }
        }
        if (removed) {
            saveAll(context, items);
        }
    }
}

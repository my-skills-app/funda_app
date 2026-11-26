package com.chill.with.examsaathi;

import android.content.Context;
import android.content.SharedPreferences;

/** Caches home dashboard JSON so returning to Home does not refetch every time. */
public final class GitaDashboardCache {

    private static final String PREFS = "gita_dashboard_cache";
    private static final String KEY_JSON = "dashboard_json";
    private static final String KEY_SCHEMA = "dashboard_schema";
    /** Bump when API host/format changes so stale GitHub JSON is not reused. */
    private static final int SCHEMA_VERSION = 2;

    private GitaDashboardCache() {}

    private static void ensureSchema(Context context) {
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (prefs.getInt(KEY_SCHEMA, 0) != SCHEMA_VERSION) {
            prefs.edit().clear().putInt(KEY_SCHEMA, SCHEMA_VERSION).apply();
        }
    }

    public static void save(Context context, String json) {
        if (context == null || json == null || json.isEmpty()) {
            return;
        }
        Context app = context.getApplicationContext();
        ensureSchema(app);
        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_JSON, json)
                .putInt(KEY_SCHEMA, SCHEMA_VERSION)
                .apply();
    }

    @androidx.annotation.Nullable
    public static String loadJson(Context context) {
        if (context == null) {
            return null;
        }
        Context app = context.getApplicationContext();
        ensureSchema(app);
        SharedPreferences prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_JSON, null);
        if (json == null || json.isEmpty()) {
            return null;
        }
        return json;
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

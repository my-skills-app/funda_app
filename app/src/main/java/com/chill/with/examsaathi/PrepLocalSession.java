package com.chill.with.examsaathi;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Local-only session flag for Prep backend emulator testing (no token changes). */
public final class PrepLocalSession {

    private static final String PREFS = "prep_local_session";
    private static final String KEY_LOGGED_IN = "prep_local_logged_in";
    private static final String KEY_DISPLAY_NAME = "prep_local_display_name";

    private PrepLocalSession() {}

    @NonNull
    private static SharedPreferences prefs(@NonNull Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean isLoggedIn(@NonNull Context context) {
        return prefs(context).getBoolean(KEY_LOGGED_IN, false);
    }

    public static void setLoggedIn(@NonNull Context context, boolean loggedIn) {
        prefs(context).edit().putBoolean(KEY_LOGGED_IN, loggedIn).apply();
    }

    public static void setDisplayName(@NonNull Context context, @Nullable String name) {
        prefs(context).edit().putString(KEY_DISPLAY_NAME, name != null ? name : "").apply();
    }

    @NonNull
    public static String getDisplayName(@NonNull Context context) {
        return prefs(context).getString(KEY_DISPLAY_NAME, "");
    }

    public static void clear(@NonNull Context context) {
        prefs(context).edit().clear().apply();
    }
}

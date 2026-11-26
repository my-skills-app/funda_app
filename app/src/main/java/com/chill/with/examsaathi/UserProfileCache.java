package com.chill.with.examsaathi;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

/** Instant home/profile name from disk; synced from Firebase in background. */
public final class UserProfileCache {

    private static final String PREFS = "user_profile_cache";
    private static final String KEY_UID = "uid";
    private static final String KEY_NAME = "name";
    private static final String KEY_PROFILE = "profile_url";

    public static final class CachedProfile {
        public final String name;
        @Nullable
        public final String profileUrl;

        CachedProfile(String name, @Nullable String profileUrl) {
            this.name = name;
            this.profileUrl = profileUrl;
        }
    }

    private UserProfileCache() {}

    public static void save(Context context, String uid, String name, @Nullable String profileUrl) {
        if (context == null || uid == null || uid.isEmpty()) {
            return;
        }
        String trimmedName = name != null ? name.trim() : "";
        if (trimmedName.isEmpty()) {
            return;
        }
        context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_UID, uid)
                .putString(KEY_NAME, trimmedName)
                .putString(KEY_PROFILE, profileUrl != null ? profileUrl.trim() : "")
                .apply();
    }

    @Nullable
    public static CachedProfile load(Context context, String uid) {
        if (context == null || uid == null || uid.isEmpty()) {
            return null;
        }
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!uid.equals(prefs.getString(KEY_UID, ""))) {
            return null;
        }
        String name = prefs.getString(KEY_NAME, "");
        if (name == null || name.isEmpty()) {
            return null;
        }
        String profile = prefs.getString(KEY_PROFILE, "");
        return new CachedProfile(name, profile != null && !profile.isEmpty() ? profile : null);
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

package com.chill.with.examsaathi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/** App update prompt from Firebase {@code app_update_config}. */
public final class AppUpdateHelper {

    public static final String PATH = "app_update_config";

    public interface ConfigCallback {
        void onConfig(@NonNull Config config);
    }

    public static final class Config {
        public boolean enabled;
        public String latestVersion = "";
        public String title = "";
        public String message = "";
        public String storeUrl = "";
        public boolean forceUpdate;

        @NonNull
        public static Config fromSnapshot(@NonNull DataSnapshot snapshot) {
            Config c = new Config();
            c.enabled = Boolean.TRUE.equals(snapshot.child("enabled").getValue(Boolean.class));
            c.latestVersion = str(snapshot, "latestVersion");
            c.title = str(snapshot, "title");
            c.message = str(snapshot, "message");
            c.storeUrl = str(snapshot, "storeUrl");
            c.forceUpdate = Boolean.TRUE.equals(snapshot.child("forceUpdate").getValue(Boolean.class));
            return c;
        }

        public boolean shouldPromptUpdate(@NonNull String currentVersion) {
            return enabled
                    && !latestVersion.isEmpty()
                    && VersionCompare.isOlderThan(currentVersion, latestVersion);
        }
    }

    private AppUpdateHelper() {}

    public static void readOnce(@NonNull ConfigCallback callback) {
        FirebaseDatabase.getInstance()
                .getReference(PATH)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        callback.onConfig(Config.fromSnapshot(snapshot));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onConfig(new Config());
                    }
                });
    }

    @NonNull
    private static String str(@NonNull DataSnapshot snapshot, @NonNull String key) {
        String value = snapshot.child(key).getValue(String.class);
        return value != null ? value.trim() : "";
    }

    /** Compares dotted version strings (e.g. 2.3 vs 2.10). */
    public static final class VersionCompare {

        private VersionCompare() {}

        public static boolean isOlderThan(@NonNull String current, @NonNull String required) {
            if (required.isEmpty()) {
                return false;
            }
            int[] c = parseParts(current);
            int[] r = parseParts(required);
            int len = Math.max(c.length, r.length);
            for (int i = 0; i < len; i++) {
                int cv = i < c.length ? c[i] : 0;
                int rv = i < r.length ? r[i] : 0;
                if (cv < rv) {
                    return true;
                }
                if (cv > rv) {
                    return false;
                }
            }
            return false;
        }

        private static int[] parseParts(@Nullable String version) {
            if (version == null || version.trim().isEmpty()) {
                return new int[] {0};
            }
            String[] parts = version.trim().split("\\.");
            int[] out = new int[parts.length];
            for (int i = 0; i < parts.length; i++) {
                try {
                    out[i] = Integer.parseInt(parts[i].replaceAll("[^0-9].*", ""));
                } catch (NumberFormatException ignored) {
                    out[i] = 0;
                }
            }
            return out;
        }
    }
}

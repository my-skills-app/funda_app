package com.chill.with.examsaathi;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

/** Disk cache for home/page JSON so Home paints instantly on reopen. */
public final class PrepHomeCache {

    private static final String FILE = "prep_home_page_cache.json";
    private static final String ME_FILE = "prep_home_me_name.txt";
    private static final long MAX_AGE_MS = 6L * 60L * 60L * 1000L; // 6h soft freshness hint

    private PrepHomeCache() {}

    public static void saveHomeJson(@NonNull Context context, @NonNull String json) {
        write(file(context, FILE), json);
    }

    @Nullable
    public static String loadHomeJson(@NonNull Context context) {
        return read(file(context, FILE));
    }

    public static boolean isFresh(@NonNull Context context) {
        File f = file(context, FILE);
        return f.exists() && (System.currentTimeMillis() - f.lastModified()) < MAX_AGE_MS;
    }

    public static void saveDisplayName(@NonNull Context context, @Nullable String name) {
        if (name == null || name.isEmpty()) {
            return;
        }
        write(file(context, ME_FILE), name);
    }

    @Nullable
    public static String loadDisplayName(@NonNull Context context) {
        return read(file(context, ME_FILE));
    }

    public static void clear(@NonNull Context context) {
        //noinspection ResultOfMethodCallIgnored
        file(context, FILE).delete();
        //noinspection ResultOfMethodCallIgnored
        file(context, ME_FILE).delete();
    }

    @NonNull
    private static File file(@NonNull Context context, @NonNull String name) {
        return new File(context.getApplicationContext().getCacheDir(), name);
    }

    private static void write(@NonNull File file, @NonNull String value) {
        try (FileOutputStream out = new FileOutputStream(file, false)) {
            out.write(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {
            // cache is best-effort
        }
    }

    @Nullable
    private static String read(@NonNull File file) {
        if (!file.exists() || file.length() == 0) {
            return null;
        }
        try (FileInputStream in = new FileInputStream(file)) {
            byte[] buf = new byte[(int) file.length()];
            int read = in.read(buf);
            if (read <= 0) {
                return null;
            }
            return new String(buf, 0, read, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }
}

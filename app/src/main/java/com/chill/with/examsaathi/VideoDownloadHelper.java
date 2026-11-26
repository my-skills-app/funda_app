package com.chill.with.examsaathi;

import android.app.DownloadManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/** Download Prep videos into Download/prep exam with % progress + share support. */
public final class VideoDownloadHelper {

    public static final String FOLDER_NAME = "prep exam";
    public static final String FILE_PROVIDER_AUTHORITY = "com.chill.with.examsaathi.prepfileprovider";

    public static final class Progress {
        public final long topicId;
        public final long downloadId;
        public final int percent;
        public final boolean running;
        public final boolean success;
        public final boolean failed;

        Progress(long topicId, long downloadId, int percent, boolean running, boolean success, boolean failed) {
            this.topicId = topicId;
            this.downloadId = downloadId;
            this.percent = percent;
            this.running = running;
            this.success = success;
            this.failed = failed;
        }
    }

    public interface Listener {
        void onDownloadProgress(@NonNull Progress progress);
    }

    private static final String PREFS = "prep_video_downloads";
    private static final String KEY_FILE_PREFIX = "file_";
    private static final String CHANNEL_ID = "prep_video_downloads";
    private static final int NOTIF_BASE = 72001;

    private static final Map<Long, Long> topicToDownload = new HashMap<>();
    private static final Map<Long, Long> downloadToTopic = new HashMap<>();
    private static final Map<Long, String> downloadTitles = new HashMap<>();
    private static final Map<Long, String> downloadRelativePaths = new HashMap<>();
    private static final Set<Listener> listeners = new CopyOnWriteArraySet<>();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static boolean polling;

    private VideoDownloadHelper() {}

    public static void addListener(@NonNull Listener listener) {
        listeners.add(listener);
    }

    public static void removeListener(@NonNull Listener listener) {
        listeners.remove(listener);
    }

    public static boolean isDownloading(long topicId) {
        return topicToDownload.containsKey(topicId);
    }

    @Nullable
    public static Progress snapshot(@NonNull Context context, long topicId) {
        Long downloadId = topicToDownload.get(topicId);
        if (downloadId == null) {
            return null;
        }
        return query(context.getApplicationContext(), topicId, downloadId);
    }

    @Nullable
    public static File getLocalFile(@NonNull Context context, long topicId) {
        String relative = prefs(context).getString(KEY_FILE_PREFIX + topicId, null);
        if (relative == null || relative.isEmpty()) {
            return null;
        }
        File file = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                relative);
        if (file.exists() && file.isFile() && file.length() > 0L) {
            return file;
        }
        prefs(context).edit().remove(KEY_FILE_PREFIX + topicId).apply();
        return null;
    }

    public static boolean hasLocalFile(@NonNull Context context, long topicId) {
        return getLocalFile(context, topicId) != null;
    }

    @NonNull
    public static String publicFolderHint() {
        return "Download / " + FOLDER_NAME;
    }

    public static void shareLocalVideo(@NonNull Context context, long topicId) {
        File file = getLocalFile(context, topicId);
        if (file == null) {
            throw new IllegalStateException("missing local file");
        }
        Uri uri = FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, file);
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("video/*");
        share.putExtra(Intent.EXTRA_STREAM, uri);
        share.putExtra(Intent.EXTRA_SUBJECT, file.getName());
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        Intent chooser = Intent.createChooser(share, context.getString(R.string.prep_share_video));
        if (!(context instanceof android.app.Activity)) {
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(chooser);
    }

    public static long enqueue(
            @NonNull Context context,
            long topicId,
            @NonNull String url,
            @Nullable String title,
            @Nullable String qualityTag) throws IllegalArgumentException {
        Context app = context.getApplicationContext();
        ensureChannel(app);

        String clean = url.trim();
        if (clean.isEmpty()) {
            throw new IllegalArgumentException("empty url");
        }
        String quality = qualityTag != null && !qualityTag.trim().isEmpty()
                ? qualityTag.trim()
                : "720p";
        String name = safeFileName(title);
        if (!name.toLowerCase().contains(quality.toLowerCase())) {
            name = name + "_" + quality;
        }
        String ext = guessExtension(clean);
        if (!name.toLowerCase().endsWith("." + ext)) {
            name = name + "." + ext;
        }
        String relativePath = FOLDER_NAME + "/" + name;
        String displayTitle = title != null && !title.isEmpty() ? title : "Prep Shorts video";

        // Ensure destination folder exists before DownloadManager writes into it
        File folder = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                FOLDER_NAME);
        //noinspection ResultOfMethodCallIgnored
        folder.mkdirs();

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(clean));
        request.setTitle(displayTitle);
        request.setDescription(app.getString(R.string.prep_download_percent, 0));
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
        request.setAllowedOverMetered(true);
        request.setAllowedOverRoaming(true);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, relativePath);
        request.allowScanningByMediaScanner();
        request.setMimeType("video/*");

        DownloadManager dm = (DownloadManager) app.getSystemService(Context.DOWNLOAD_SERVICE);
        if (dm == null) {
            throw new IllegalStateException("DownloadManager missing");
        }
        long downloadId = dm.enqueue(request);

        synchronized (VideoDownloadHelper.class) {
            topicToDownload.put(topicId, downloadId);
            downloadToTopic.put(downloadId, topicId);
            downloadTitles.put(downloadId, displayTitle);
            downloadRelativePaths.put(downloadId, relativePath);
        }

        postProgressNotification(app, topicId, displayTitle, 0, true);
        notifyListeners(new Progress(topicId, downloadId, 0, true, false, false));
        startPolling(app);
        return downloadId;
    }

    private static void startPolling(@NonNull Context app) {
        if (polling) {
            return;
        }
        polling = true;
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                Set<Long> topics;
                synchronized (VideoDownloadHelper.class) {
                    topics = new HashSet<>(topicToDownload.keySet());
                }
                if (topics.isEmpty()) {
                    polling = false;
                    return;
                }
                for (Long topicId : topics) {
                    Long downloadId;
                    String relativePath;
                    synchronized (VideoDownloadHelper.class) {
                        downloadId = topicToDownload.get(topicId);
                        relativePath = downloadId != null ? downloadRelativePaths.get(downloadId) : null;
                    }
                    if (downloadId == null) {
                        continue;
                    }
                    Progress p = query(app, topicId, downloadId);
                    if (p == null) {
                        continue;
                    }
                    String title;
                    synchronized (VideoDownloadHelper.class) {
                        title = downloadTitles.get(downloadId);
                    }
                    if (title == null) {
                        title = "Prep Shorts video";
                    }
                    if (p.running) {
                        postProgressNotification(app, topicId, title, p.percent, true);
                    } else if (p.success) {
                        if (relativePath != null) {
                            prefs(app).edit().putString(KEY_FILE_PREFIX + topicId, relativePath).apply();
                        }
                        postProgressNotification(app, topicId, title, 100, false);
                        clearMaps(topicId, downloadId);
                    } else if (p.failed) {
                        cancelProgressNotification(app, topicId);
                        clearMaps(topicId, downloadId);
                    }
                    notifyListeners(p);
                }
                if (polling) {
                    mainHandler.postDelayed(this, 400);
                }
            }
        });
    }

    private static void clearMaps(long topicId, long downloadId) {
        synchronized (VideoDownloadHelper.class) {
            topicToDownload.remove(topicId);
            downloadToTopic.remove(downloadId);
            downloadTitles.remove(downloadId);
            downloadRelativePaths.remove(downloadId);
            if (topicToDownload.isEmpty()) {
                polling = false;
            }
        }
    }

    @Nullable
    private static Progress query(@NonNull Context app, long topicId, long downloadId) {
        DownloadManager dm = (DownloadManager) app.getSystemService(Context.DOWNLOAD_SERVICE);
        if (dm == null) {
            return null;
        }
        DownloadManager.Query q = new DownloadManager.Query().setFilterById(downloadId);
        try (Cursor c = dm.query(q)) {
            if (c == null || !c.moveToFirst()) {
                return new Progress(topicId, downloadId, 0, false, false, true);
            }
            int status = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
            long soFar = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
            long total = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
            int percent = 0;
            if (total > 0) {
                percent = (int) Math.min(100, Math.round((soFar * 100.0) / total));
            } else if (status == DownloadManager.STATUS_SUCCESSFUL) {
                percent = 100;
            }
            boolean running = status == DownloadManager.STATUS_RUNNING
                    || status == DownloadManager.STATUS_PENDING
                    || status == DownloadManager.STATUS_PAUSED;
            boolean success = status == DownloadManager.STATUS_SUCCESSFUL;
            boolean failed = status == DownloadManager.STATUS_FAILED;
            return new Progress(topicId, downloadId, percent, running, success, failed);
        } catch (Exception e) {
            return null;
        }
    }

    private static void notifyListeners(@NonNull Progress progress) {
        for (Listener listener : listeners) {
            try {
                listener.onDownloadProgress(progress);
            } catch (Exception ignored) {
                // ignore
            }
        }
    }

    private static void ensureChannel(@NonNull Context app) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager nm = app.getSystemService(NotificationManager.class);
        if (nm == null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Video downloads",
                NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Shows video download progress");
        channel.setShowBadge(false);
        nm.createNotificationChannel(channel);
    }

    private static void postProgressNotification(
            @NonNull Context app,
            long topicId,
            @NonNull String title,
            int percent,
            boolean ongoing) {
        NotificationCompat.Builder b = new NotificationCompat.Builder(app, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_download_box)
                .setContentTitle(title)
                .setContentText(ongoing
                        ? app.getString(R.string.prep_download_percent, percent)
                        : app.getString(R.string.prep_download_saved_in, publicFolderHint()))
                .setOnlyAlertOnce(true)
                .setOngoing(ongoing)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS);
        if (ongoing) {
            b.setProgress(100, Math.max(0, Math.min(100, percent)), percent <= 0);
        } else {
            b.setProgress(0, 0, false);
            b.setAutoCancel(true);
        }
        try {
            NotificationManagerCompat.from(app).notify(notifId(topicId), b.build());
        } catch (SecurityException ignored) {
            // POST_NOTIFICATIONS may be denied on Android 13+
        }
    }

    private static void cancelProgressNotification(@NonNull Context app, long topicId) {
        NotificationManagerCompat.from(app).cancel(notifId(topicId));
    }

    private static int notifId(long topicId) {
        return NOTIF_BASE + (int) (topicId % 100000);
    }

    @NonNull
    private static SharedPreferences prefs(@NonNull Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    @NonNull
    private static String safeFileName(@Nullable String title) {
        String base = title != null ? title.trim() : "video";
        if (base.isEmpty()) {
            base = "video";
        }
        base = base.replaceAll("[\\\\/:*?\"<>|]+", "_");
        if (base.length() > 50) {
            base = base.substring(0, 50);
        }
        return base;
    }

    @NonNull
    private static String guessExtension(@NonNull String url) {
        String path = Uri.parse(url).getLastPathSegment();
        if (path != null) {
            int dot = path.lastIndexOf('.');
            if (dot > 0 && dot < path.length() - 1) {
                String ext = path.substring(dot + 1).toLowerCase();
                int q = ext.indexOf('?');
                if (q >= 0) {
                    ext = ext.substring(0, q);
                }
                if (ext.length() <= 5 && ext.matches("[a-z0-9]+")) {
                    return ext;
                }
            }
        }
        String mime = MimeTypeMap.getFileExtensionFromUrl(url);
        if (mime != null && !mime.isEmpty()) {
            return mime;
        }
        return "mp4";
    }
}

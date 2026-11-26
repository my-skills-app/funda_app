package com.chill.with.examsaathi;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.FutureTarget;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** FCM push — token, permission, and notification display (image + link). */
public final class PushNotificationHelper {

    private static final String TAG = "PushNotification";
    static final String CHANNEL_ID = "app_push_hidden";
    private static final String TOPIC_ALL = "all_users";
    private static final int PERMISSION_REQUEST = 9101;
    private static final int NOTIFICATION_ID_BASE = 5000;
    private static final int IMAGE_LOAD_TIMEOUT_SEC = 8;

    private PushNotificationHelper() {}

    public static void init(@NonNull Activity activity) {
        ensureChannel(activity.getApplicationContext());
        requestPermissionSilently(activity);
        registerToken(activity.getApplicationContext());
        PushBroadcastListener.start(activity.getApplicationContext());
    }

    public static void ensureChannel(@NonNull Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.push_notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription(context.getString(R.string.push_notification_channel_desc));
        channel.enableVibration(true);
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    public static void requestPermissionSilently(@NonNull Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }
        if (ContextCompat.checkSelfPermission(activity, android.Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        ActivityCompat.requestPermissions(
                activity,
                new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                PERMISSION_REQUEST);
    }

    public static void registerToken(@NonNull Context context) {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful() || task.getResult() == null) {
                        Log.w(TAG, "FCM token fetch failed", task.getException());
                        return;
                    }
                    saveToken(context, task.getResult());
                });
        FirebaseMessaging.getInstance().subscribeToTopic(TOPIC_ALL)
                .addOnFailureListener(e -> Log.w(TAG, "Topic subscribe failed", e));
    }

    public static void saveToken(@NonNull Context context, @NonNull String token) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || token.isEmpty()) {
            return;
        }
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("fcmToken", token);
        map.put("updatedAt", System.currentTimeMillis());
        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(user.getUid())
                .updateChildren(map)
                .addOnFailureListener(e -> Log.w(TAG, "Failed to save FCM token", e));
    }

    public static boolean showPush(
            @NonNull Context context,
            @NonNull PushPayload payload,
            @Nullable Uri localImageUri) {
        if (!hasNotificationPermission(context)) {
            return false;
        }
        new Thread(() -> {
            Bitmap bitmap = loadBitmap(context, payload.imageUrl, localImageUri);
            postNotification(context, payload, bitmap);
        }).start();
        return true;
    }

    /** @return false if notification permission is missing (Android 13+). */
    public static boolean showLocalNotification(
            @NonNull Context context, @NonNull String title, @NonNull String body) {
        return showPush(context, new PushPayload(title, body, null, null), null);
    }

    public static void showPushNotification(@NonNull Context context, @NonNull RemoteMessage message) {
        if (!hasNotificationPermission(context)) {
            return;
        }
        PushPayload payload = payloadFromRemoteMessage(message);
        new Thread(() -> {
            Bitmap bitmap = loadBitmap(context, payload.imageUrl, null);
            postNotification(context, payload, bitmap);
        }).start();
    }

    @NonNull
    private static PushPayload payloadFromRemoteMessage(@NonNull RemoteMessage message) {
        String title = null;
        String body = null;
        if (message.getNotification() != null) {
            title = message.getNotification().getTitle();
            body = message.getNotification().getBody();
            String image = message.getNotification().getImageUrl() != null
                    ? message.getNotification().getImageUrl().toString()
                    : null;
            Map<String, String> data = message.getData();
            String link = data.get(PushPayload.DATA_LINK_URL);
            if (title == null) {
                title = data.get(PushPayload.DATA_TITLE);
            }
            if (body == null) {
                body = data.get(PushPayload.DATA_BODY);
            }
            String imageFromData = data.get(PushPayload.DATA_IMAGE_URL);
            return new PushPayload(
                    title != null ? title : "",
                    body != null ? body : "",
                    imageFromData != null ? imageFromData : image,
                    link);
        }
        Map<String, String> data = message.getData();
        return new PushPayload(
                data.get(PushPayload.DATA_TITLE),
                data.get(PushPayload.DATA_BODY),
                data.get(PushPayload.DATA_IMAGE_URL),
                data.get(PushPayload.DATA_LINK_URL));
    }

    @Nullable
    private static Bitmap loadBitmap(
            @NonNull Context context,
            @NonNull String imageUrl,
            @Nullable Uri localImageUri) {
        Object source = null;
        if (localImageUri != null) {
            source = localImageUri;
        } else if (!imageUrl.isEmpty()) {
            source = imageUrl;
        }
        if (source == null) {
            return null;
        }
        FutureTarget<Bitmap> target = Glide.with(context.getApplicationContext())
                .asBitmap()
                .load(source)
                .submit();
        try {
            return target.get(IMAGE_LOAD_TIMEOUT_SEC, TimeUnit.SECONDS);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            Log.w(TAG, "Notification image load failed", e);
            Glide.with(context.getApplicationContext()).clear(target);
            return null;
        }
    }

    private static void postNotification(
            @NonNull Context context,
            @NonNull PushPayload payload,
            @Nullable Bitmap imageBitmap) {
        Context app = context.getApplicationContext();
        if (!hasNotificationPermission(app)) {
            return;
        }
        ensureChannel(app);
        String title = payload.title.isEmpty() ? app.getString(R.string.app_name) : payload.title;
        String body = payload.body;

        Intent intent = buildTapIntent(app, payload.linkUrl);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                app,
                (int) (System.currentTimeMillis() % 100000),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(app, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_audio)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        if (imageBitmap != null) {
            builder.setLargeIcon(imageBitmap);
            builder.setStyle(new NotificationCompat.BigPictureStyle()
                    .bigPicture(imageBitmap)
                    .bigLargeIcon((Bitmap) null));
        }

        NotificationManager manager = (NotificationManager) app.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            int id = NOTIFICATION_ID_BASE + (int) (System.currentTimeMillis() % 10000);
            manager.notify(id, builder.build());
        }
    }

    @NonNull
    private static Intent buildTapIntent(@NonNull Context context, @NonNull String linkUrl) {
        if (!linkUrl.isEmpty()) {
            Intent browser = new Intent(Intent.ACTION_VIEW, Uri.parse(linkUrl));
            browser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            return browser;
        }
        Intent main = new Intent(context, MainActivity.class);
        main.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return main;
    }

    private static boolean hasNotificationPermission(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }
}

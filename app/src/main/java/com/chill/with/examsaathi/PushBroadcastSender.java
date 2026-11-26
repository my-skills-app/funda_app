package com.chill.with.examsaathi;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

/**
 * All users: writes to {@code push_broadcasts}; other signed-in apps receive via {@link PushBroadcastListener}.
 */
public final class PushBroadcastSender {

    private static final String TAG = "PushBroadcast";
    private static final String TOPIC_ALL = "all_users";

    public interface Callback {
        void onSuccess();

        void onError(@NonNull String message);
    }

    private PushBroadcastSender() {}

    public static void send(
            @NonNull Context context,
            @NonNull PushPayload payload,
            boolean allUsers,
            @Nullable Uri localImageUri,
            @NonNull Callback callback) {
        if (!allUsers) {
            if (!PushNotificationHelper.showPush(context, payload, localImageUri)) {
                callback.onError(context.getString(R.string.send_notification_permission_denied));
                return;
            }
            callback.onSuccess();
            return;
        }
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            callback.onError(context.getString(R.string.send_notification_not_signed_in));
            return;
        }
        if (!PushAdminHelper.isPushAdmin(user)) {
            callback.onError(context.getString(R.string.send_notification_admin_only));
            return;
        }
        if (localImageUri != null && payload.imageUrl.isEmpty()) {
            AdminSecretsLoader.fetchImgbbApiKey(context, new AdminSecretsLoader.ImgbbKeyCallback() {
                @Override
                public void onKey(@NonNull String apiKey) {
                    ImgbbUploader.upload(context, localImageUri, apiKey, new ImgbbUploader.Callback() {
                        @Override
                        public void onSuccess(@NonNull String downloadUrl) {
                            PushPayload withImage = new PushPayload(
                                    payload.title, payload.body, downloadUrl, payload.linkUrl);
                            queueBroadcast(context, withImage, callback);
                        }

                        @Override
                        public void onError(@NonNull String message) {
                            callback.onError(message);
                        }
                    });
                }

                @Override
                public void onError(@NonNull String message) {
                    callback.onError(message);
                }
            });
            return;
        }
        queueBroadcast(context, payload, callback);
    }

    private static void queueBroadcast(
            @NonNull Context context,
            @NonNull PushPayload payload,
            @NonNull Callback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            callback.onError(context.getString(R.string.send_notification_not_signed_in));
            return;
        }
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("push_broadcasts").push();
        ref.setValue(payload.toBroadcastRow(TOPIC_ALL, user.getUid()))
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Queued broadcast " + ref.getKey());
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "push_broadcasts write failed", e);
                    callback.onError(mapWriteError(context, e));
                });
    }

    @NonNull
    private static String mapWriteError(@NonNull Context context, @NonNull Exception e) {
        String msg = e.getMessage();
        if (msg != null && (msg.contains("PERMISSION_DENIED") || msg.toLowerCase().contains("permission"))) {
            return context.getString(R.string.send_notification_admin_only);
        }
        return msg != null ? msg : context.getString(R.string.send_notification_failed);
    }
}

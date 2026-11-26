package com.chill.with.examsaathi;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/** Loads ImgBB API key from {@code admin_config} (Firebase rules: admin UID only). */
public final class AdminSecretsLoader {

    private static final String PATH = "admin_config";
    private static final String KEY_IMGBB = "imgbbApiKey";

    @Nullable
    private static String cachedImgbbKey;

    public interface ImgbbKeyCallback {
        void onKey(@NonNull String apiKey);

        void onError(@NonNull String message);
    }

    private AdminSecretsLoader() {}

    public static void fetchImgbbApiKey(@NonNull Context context, @NonNull ImgbbKeyCallback callback) {
        if (!PushAdminHelper.isCurrentUserPushAdmin()) {
            callback.onError(context.getString(R.string.send_notification_admin_only));
            return;
        }
        if (cachedImgbbKey != null && !cachedImgbbKey.isEmpty()) {
            callback.onKey(cachedImgbbKey);
            return;
        }
        FirebaseDatabase.getInstance()
                .getReference(PATH)
                .child(KEY_IMGBB)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String key = snapshot.getValue(String.class);
                        if (key == null || key.trim().isEmpty()) {
                            callback.onError(context.getString(R.string.send_notification_imgbb_key_missing));
                            return;
                        }
                        cachedImgbbKey = key.trim();
                        callback.onKey(cachedImgbbKey);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (error.getCode() == DatabaseError.PERMISSION_DENIED) {
                            callback.onError(context.getString(R.string.send_notification_admin_only));
                        } else {
                            String msg = error.getMessage();
                            callback.onError(msg != null ? msg : context.getString(R.string.send_notification_failed));
                        }
                    }
                });
    }

    public static void clearCache() {
        cachedImgbbKey = null;
    }
}

package com.chill.with.examsaathi;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/** Uploads push images via ImgBB API v1 (POST multipart). */
public final class ImgbbUploader {

    private static final String TAG = "ImgbbUpload";
    private static final String UPLOAD_URL = "https://api.imgbb.com/1/upload";
    private static final OkHttpClient CLIENT = new OkHttpClient();

    public interface Callback {
        void onSuccess(@NonNull String imageUrl);

        void onError(@NonNull String message);
    }

    private ImgbbUploader() {}

    public static void upload(
            @NonNull Context context,
            @NonNull Uri imageUri,
            @NonNull String apiKey,
            @NonNull Callback callback) {
        if (apiKey.trim().isEmpty()) {
            callback.onError(context.getString(R.string.send_notification_imgbb_key_missing));
            return;
        }
        Context app = context.getApplicationContext();
        final String key = apiKey.trim();
        new Thread(() -> {
            try {
                byte[] bytes = readImageBytes(app, imageUri);
                if (bytes.length == 0) {
                    postError(callback, context.getString(R.string.send_notification_image_read_failed));
                    return;
                }
                String url = uploadBytes(key, bytes);
                postSuccess(callback, url);
            } catch (Exception e) {
                Log.e(TAG, "ImgBB upload failed", e);
                String msg = e.getMessage();
                postError(callback, msg != null ? msg : context.getString(R.string.send_notification_image_upload_failed));
            }
        }).start();
    }

    @NonNull
    private static byte[] readImageBytes(@NonNull Context context, @NonNull Uri uri) throws Exception {
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            if (in == null) {
                throw new IllegalStateException("Cannot open image");
            }
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int read;
            while ((read = in.read(chunk)) != -1) {
                buffer.write(chunk, 0, read);
            }
            return buffer.toByteArray();
        }
    }

    @NonNull
    private static String uploadBytes(@NonNull String apiKey, @NonNull byte[] imageBytes) throws Exception {
        RequestBody imageBody = RequestBody.create(imageBytes, MediaType.parse("image/*"));
        MultipartBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("key", apiKey)
                .addFormDataPart("image", "push.jpg", imageBody)
                .build();
        Request request = new Request.Builder()
                .url(UPLOAD_URL)
                .post(body)
                .build();
        try (Response response = CLIENT.newCall(request).execute()) {
            ResponseBody responseBody = response.body();
            String json = responseBody != null ? responseBody.string() : "";
            if (!response.isSuccessful()) {
                throw new IllegalStateException(parseError(json, response.code()));
            }
            return parseImageUrl(json);
        }
    }

    @NonNull
    private static String parseImageUrl(@NonNull String json) throws Exception {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        if (!root.has("success") || !root.get("success").getAsBoolean()) {
            throw new IllegalStateException(parseError(json, 0));
        }
        JsonObject data = root.getAsJsonObject("data");
        if (data == null) {
            throw new IllegalStateException("ImgBB: empty response");
        }
        if (data.has("url") && !data.get("url").getAsString().isEmpty()) {
            return data.get("url").getAsString();
        }
        if (data.has("display_url") && !data.get("display_url").getAsString().isEmpty()) {
            return data.get("display_url").getAsString();
        }
        if (data.has("image") && data.getAsJsonObject("image").has("url")) {
            return data.getAsJsonObject("image").get("url").getAsString();
        }
        throw new IllegalStateException("ImgBB: no image URL in response");
    }

    @NonNull
    private static String parseError(@NonNull String json, int httpCode) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            if (root.has("error") && root.getAsJsonObject("error").has("message")) {
                return root.getAsJsonObject("error").get("message").getAsString();
            }
            if (root.has("status_txt")) {
                return root.get("status_txt").getAsString();
            }
        } catch (Exception ignored) {
            // fall through
        }
        if (httpCode > 0) {
            return "ImgBB HTTP " + httpCode;
        }
        return "ImgBB upload failed";
    }

    private static void postSuccess(@NonNull Callback callback, @NonNull String url) {
        android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
        handler.post(() -> callback.onSuccess(url));
    }

    private static void postError(@NonNull Callback callback, @NonNull String message) {
        android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
        handler.post(() -> callback.onError(message));
    }
}

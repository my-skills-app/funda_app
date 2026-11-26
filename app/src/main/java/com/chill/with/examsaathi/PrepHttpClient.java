package com.chill.with.examsaathi;

import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/** OkHttp client for Prep backend (AWS/Cloudflare HTTPS). */
public final class PrepHttpClient {

    private static final String TAG = "PrepHttp";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private static OkHttpClient client;

    private PrepHttpClient() {}

    @NonNull
    public static synchronized OkHttpClient getClient() {
        if (client == null) {
            client = new OkHttpClient.Builder()
                    .connectTimeout(12, TimeUnit.SECONDS)
                    .readTimeout(25, TimeUnit.SECONDS)
                    .writeTimeout(20, TimeUnit.SECONDS)
                    .callTimeout(30, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true)
                    .followRedirects(true)
                    .build();
        }
        return client;
    }

    @NonNull
    public static String get(@NonNull String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .get()
                .build();
        try (Response response = getClient().newCall(request).execute()) {
            if (response.body() == null) {
                throw new IOException("Empty response");
            }
            String body = response.body().string();
            if (!response.isSuccessful()) {
                Log.e(TAG, "GET failed " + response.code() + " " + url + " body=" + body);
                throw new IOException("HTTP " + response.code() + ": " + body);
            }
            Log.d(TAG, "GET ok " + response.code() + " " + url + " bytes=" + body.length());
            return body;
        } catch (IOException e) {
            Log.e(TAG, "GET error " + url + " — " + e.getMessage());
            throw e;
        }
    }

    @NonNull
    public static String postJson(@NonNull String url, @NonNull String json) throws IOException {
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .post(body)
                .build();
        try (Response response = getClient().newCall(request).execute()) {
            if (response.body() == null) {
                throw new IOException("Empty response");
            }
            String text = response.body().string();
            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code() + ": " + text);
            }
            return text;
        }
    }
}

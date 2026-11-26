package com.chill.with.examsaathi;

import android.content.Context;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/** Shared OkHttp client for app API calls (HTTPS only, auth headers, system TLS). */
public final class SecureHttpClient {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private static OkHttpClient client;

    private SecureHttpClient() {}

    @NonNull
    public static synchronized OkHttpClient getClient() {
        if (client == null) {
            client = new OkHttpClient.Builder()
                    .connectTimeout(20, TimeUnit.SECONDS)
                    .readTimeout(25, TimeUnit.SECONDS)
                    .writeTimeout(25, TimeUnit.SECONDS)
                    .followRedirects(true)
                    .followSslRedirects(true)
                    .addInterceptor(SecureHttpClient::addSecurityHeaders)
                    .build();
        }
        return client;
    }

    @NonNull
    private static Response addSecurityHeaders(@NonNull Interceptor.Chain chain) throws IOException {
        Request original = chain.request();
        if (!isHttps(original.url().scheme())) {
            throw new IOException("Blocked non-HTTPS request");
        }
        Request.Builder builder = original.newBuilder()
                .header("Accept", "application/json")
                .header("X-App-Key", StringVault.apiKey())
                .header("X-Requested-With", "BhagavatGita-Android");
        Context ctx = MyApp.getContext();
        if (ctx != null) {
            builder.header("X-App-Package", ctx.getPackageName());
        }
        return chain.proceed(builder.build());
    }

    private static boolean isHttps(@NonNull String scheme) {
        return "https".equalsIgnoreCase(scheme);
    }

    @NonNull
    public static String get(@NonNull Context context, @NonNull String url) throws IOException {
        requireHttps(url);
        Request request = new Request.Builder().url(url).get().build();
        try (Response response = getClient().newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("HTTP " + response.code());
            }
            return response.body().string();
        }
    }

    @NonNull
    public static String postJson(@NonNull Context context, @NonNull String url, @NonNull String json)
            throws IOException {
        requireHttps(url);
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        try (Response response = getClient().newCall(request).execute()) {
            if (response.body() == null) {
                throw new IOException("Empty response");
            }
            return response.body().string();
        }
    }

    private static void requireHttps(@NonNull String url) throws IOException {
        if (!url.regionMatches(true, 0, "https://", 0, 8)) {
            throw new IOException("Only HTTPS URLs are allowed");
        }
    }
}

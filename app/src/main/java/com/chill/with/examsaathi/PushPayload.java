package com.chill.with.examsaathi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.database.DataSnapshot;

import java.util.HashMap;
import java.util.Map;

/** Title, body, optional image URL and tap link for push broadcasts. */
public final class PushPayload {

    public static final String DATA_TITLE = "title";
    public static final String DATA_BODY = "body";
    public static final String DATA_IMAGE_URL = "imageUrl";
    public static final String DATA_LINK_URL = "linkUrl";

    @NonNull public final String title;
    @NonNull public final String body;
    @NonNull public final String imageUrl;
    @NonNull public final String linkUrl;

    public PushPayload(
            @NonNull String title,
            @NonNull String body,
            @Nullable String imageUrl,
            @Nullable String linkUrl) {
        this.title = title.trim();
        this.body = body.trim();
        this.imageUrl = imageUrl != null ? imageUrl.trim() : "";
        this.linkUrl = linkUrl != null ? linkUrl.trim() : "";
    }

    @NonNull
    public static PushPayload fromSnapshot(@NonNull DataSnapshot snapshot) {
        String title = snapshot.child("title").getValue(String.class);
        String body = snapshot.child("body").getValue(String.class);
        String imageUrl = snapshot.child("imageUrl").getValue(String.class);
        String linkUrl = snapshot.child("linkUrl").getValue(String.class);
        return new PushPayload(
                title != null ? title : "",
                body != null ? body : "",
                imageUrl,
                linkUrl);
    }

    @NonNull
    public Map<String, Object> toBroadcastRow(@NonNull String topic, @NonNull String createdBy) {
        Map<String, Object> row = new HashMap<>();
        row.put("title", title);
        row.put("body", body);
        row.put("topic", topic);
        row.put("createdBy", createdBy);
        row.put("createdAt", System.currentTimeMillis());
        row.put("status", "pending");
        if (!imageUrl.isEmpty()) {
            row.put("imageUrl", imageUrl);
        }
        if (!linkUrl.isEmpty()) {
            row.put("linkUrl", linkUrl);
        }
        return row;
    }

    @NonNull
    public Map<String, String> toDataMap() {
        Map<String, String> data = new HashMap<>();
        data.put(DATA_TITLE, title);
        data.put(DATA_BODY, body);
        if (!imageUrl.isEmpty()) {
            data.put(DATA_IMAGE_URL, imageUrl);
        }
        if (!linkUrl.isEmpty()) {
            data.put(DATA_LINK_URL, linkUrl);
        }
        return data;
    }

    public boolean hasImage() {
        return !imageUrl.isEmpty();
    }

    public boolean hasLink() {
        return !linkUrl.isEmpty();
    }
}

package com.chill.with.examsaathi;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Delivers "All users" broadcasts via Realtime Database — works without deployed Cloud Functions.
 * Devices with the app open (or in background with active connection) show a notification.
 * FCM from {@code onPushBroadcastQueued} still applies when Functions are deployed (app killed).
 */
public final class PushBroadcastListener {

    private static final String TAG = "PushBroadcast";
    private static final String PREFS = "push_broadcast_listener";
    private static final String KEY_LAST_CREATED_AT = "last_created_at";

    @Nullable
    private static ChildEventListener activeListener;
    @Nullable
    private static DatabaseReference activeRef;

    private PushBroadcastListener() {}

    public static void start(@NonNull Context context) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            stop();
            return;
        }
        if (activeListener != null) {
            return;
        }
        Context app = context.getApplicationContext();
        long lastSeen = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getLong(KEY_LAST_CREATED_AT, -1L);
        if (lastSeen < 0) {
            lastSeen = System.currentTimeMillis();
            saveLastSeen(app, lastSeen);
        }
        final AtomicLong lastSeenMs = new AtomicLong(lastSeen);

        activeRef = FirebaseDatabase.getInstance().getReference("push_broadcasts");
        activeListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Long createdAt = snapshot.child("createdAt").getValue(Long.class);
                if (createdAt == null || createdAt <= lastSeenMs.get()) {
                    return;
                }
                PushPayload payload = PushPayload.fromSnapshot(snapshot);
                if (payload.title.isEmpty() || payload.body.isEmpty()) {
                    return;
                }
                if ("failed".equals(snapshot.child("status").getValue(String.class))) {
                    return;
                }
                lastSeenMs.set(createdAt);
                saveLastSeen(app, createdAt);
                PushNotificationHelper.showPush(app, payload, null);
                Log.d(TAG, "Delivered broadcast via RTDB: " + snapshot.getKey());
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {}

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "push_broadcasts listener cancelled", error.toException());
            }
        };
        activeRef.addChildEventListener(activeListener);
    }

    public static void stop() {
        if (activeRef != null && activeListener != null) {
            activeRef.removeEventListener(activeListener);
        }
        activeRef = null;
        activeListener = null;
    }

    private static void saveLastSeen(@NonNull Context context, long createdAt) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putLong(KEY_LAST_CREATED_AT, createdAt)
                .apply();
    }
}

package com.chill.with.examsaathi;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/** Receives FCM messages — no UI in app for users. */
public class AppFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onNewToken(@NonNull String token) {
        PushNotificationHelper.saveToken(getApplicationContext(), token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        PushNotificationHelper.showPushNotification(getApplicationContext(), remoteMessage);
    }
}

package com.chill.with.examsaathi;

import android.app.Application;
import android.content.Intent;

import java.io.PrintWriter;
import java.io.StringWriter;
import android.content.Context;

import com.google.firebase.database.FirebaseDatabase;


public class MyApp extends Application {

    private static Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        try {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        } catch (Exception ignored) {
            // Already enabled or called after DB use
        }
        PushNotificationHelper.ensureChannel(this);

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            throwable.printStackTrace(pw);
            String crashLog = sw.toString();

            Intent intent = new Intent(getApplicationContext(), CrashActivity.class);
            intent.putExtra("crash_log", crashLog);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);

            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
        });
    }

    public static Context getContext() {
        return mContext;
    }
}
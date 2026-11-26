package com.chill.with.examsaathi;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.StringRes;

/** App-wide custom toast (matches existing dark toast style). */
public final class AppToast {

    private AppToast() {}

    public static void show(Context context, String message) {
        if (context == null || message == null || message.trim().isEmpty()) {
            return;
        }
        Context appContext = context.getApplicationContext();
        LayoutInflater inflater = LayoutInflater.from(appContext);
        View layout = inflater.inflate(R.layout.custom_toast, null);
        TextView text = layout.findViewById(R.id.toast_text);
        text.setText(message);

        Toast toast = new Toast(appContext);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.setGravity(Gravity.BOTTOM, 0, 100);
        toast.show();
    }

    public static void show(Context context, @StringRes int messageRes) {
        if (context == null) {
            return;
        }
        show(context, context.getString(messageRes));
    }

    public static void showComingSoon(Context context) {
        show(context, R.string.feature_coming_soon);
    }
}

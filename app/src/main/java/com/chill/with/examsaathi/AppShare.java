package com.chill.with.examsaathi;

import android.content.Context;
import android.content.Intent;

/** Shares the app via the system share sheet (static message for now). */
public final class AppShare {

    private AppShare() {}

    public static void shareApp(Context context) {
        if (context == null) {
            return;
        }
        String appName = context.getString(R.string.app_name);
        String message = context.getString(R.string.share_app_message, appName);
        String storeLink = "https://play.google.com/store/apps/details?id="
                + context.getPackageName();

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_app_subject, appName));
        shareIntent.putExtra(Intent.EXTRA_TEXT, message + "\n\n" + storeLink);

        Intent chooser = Intent.createChooser(
                shareIntent,
                context.getString(R.string.share_app_chooser));
        if (shareIntent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(chooser);
        }
    }
}

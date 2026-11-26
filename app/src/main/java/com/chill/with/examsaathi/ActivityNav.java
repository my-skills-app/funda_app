package com.chill.with.examsaathi;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;

/** Instant activity transitions — same feel as home tab switching (no slide animation). */
public final class ActivityNav {

    private ActivityNav() {}

    public static void start(Context context, Intent intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        context.startActivity(intent);
        Activity activity = findActivity(context);
        if (activity != null) {
            activity.overridePendingTransition(0, 0);
        }
    }

    public static void finish(Activity activity) {
        activity.finish();
        activity.overridePendingTransition(0, 0);
    }

    private static Activity findActivity(Context context) {
        Context current = context;
        while (current instanceof ContextWrapper) {
            if (current instanceof Activity) {
                return (Activity) current;
            }
            current = ((ContextWrapper) current).getBaseContext();
        }
        return null;
    }
}

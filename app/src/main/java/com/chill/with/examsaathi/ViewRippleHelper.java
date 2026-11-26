package com.chill.with.examsaathi;

import android.content.Context;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.NonNull;

/** Applies theme ripple / selectable background without invalid resource id 0. */
public final class ViewRippleHelper {

    private ViewRippleHelper() {}

    public static void applySelectableBackground(@NonNull Context context, @NonNull View view) {
        TypedValue typedValue = new TypedValue();
        boolean resolved = context.getTheme().resolveAttribute(
                android.R.attr.selectableItemBackground,
                typedValue,
                true);
        if (resolved && typedValue.resourceId != 0) {
            view.setBackgroundResource(typedValue.resourceId);
        }
        view.setClickable(true);
    }
}

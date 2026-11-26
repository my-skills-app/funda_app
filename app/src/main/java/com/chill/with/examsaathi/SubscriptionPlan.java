package com.chill.with.examsaathi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.database.DataSnapshot;

/** Plan row from {@code subscription_plans/{planId}}. */
public final class SubscriptionPlan {

    @NonNull public final String planId;
    @NonNull public final String title;
    @NonNull public final String subtitle;
    @NonNull public final String priceLabel;
    @NonNull public final String badge;
    public final boolean active;
    public final int sortOrder;

    public SubscriptionPlan(
            @NonNull String planId,
            @NonNull String title,
            @NonNull String subtitle,
            @NonNull String priceLabel,
            @Nullable String badge,
            boolean active,
            int sortOrder) {
        this.planId = planId;
        this.title = title;
        this.subtitle = subtitle;
        this.priceLabel = priceLabel;
        this.badge = badge != null ? badge.trim() : "";
        this.active = active;
        this.sortOrder = sortOrder;
    }

    @Nullable
    public static SubscriptionPlan fromSnapshot(@NonNull String planId, @NonNull DataSnapshot snapshot) {
        Boolean activeVal = snapshot.child("active").getValue(Boolean.class);
        boolean active = activeVal == null || activeVal;
        if (!active) {
            return null;
        }
        String title = stringVal(snapshot, "title");
        String price = stringVal(snapshot, "priceLabel");
        if (title.isEmpty() || price.isEmpty()) {
            return null;
        }
        return new SubscriptionPlan(
                planId,
                title,
                stringVal(snapshot, "subtitle"),
                price,
                stringVal(snapshot, "badge"),
                true,
                intVal(snapshot.child("sortOrder").getValue()));
    }

    @NonNull
    private static String stringVal(@NonNull DataSnapshot snapshot, @NonNull String key) {
        String v = snapshot.child(key).getValue(String.class);
        return v != null ? v.trim() : "";
    }

    private static int intVal(@Nullable Object v) {
        if (v instanceof Long) {
            return ((Long) v).intValue();
        }
        if (v instanceof Integer) {
            return (Integer) v;
        }
        return 0;
    }
}

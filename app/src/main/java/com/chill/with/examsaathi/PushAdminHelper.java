package com.chill.with.examsaathi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/** UID allowed to queue {@code push_broadcasts} (must match database.rules.json). */
public final class PushAdminHelper {

    public static final String ADMIN_UID = "dldcxUuFW9Qz00Vh7tBhgydn7qq2";

    private PushAdminHelper() {}

    public static boolean isPushAdmin(@Nullable FirebaseUser user) {
        return user != null && ADMIN_UID.equals(user.getUid());
    }

    public static boolean isCurrentUserPushAdmin() {
        return isPushAdmin(FirebaseAuth.getInstance().getCurrentUser());
    }

    public static boolean isPushAdminUid(@NonNull String uid) {
        return ADMIN_UID.equals(uid);
    }
}

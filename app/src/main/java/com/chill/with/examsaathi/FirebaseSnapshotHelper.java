package com.chill.with.examsaathi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.GenericTypeIndicator;

import java.util.HashMap;

/** Single shared {@link GenericTypeIndicator} — safe with R8 (anonymous classes break in release). */
public final class FirebaseSnapshotHelper {

    private static final GenericTypeIndicator<HashMap<String, Object>> MAP_TYPE =
            new GenericTypeIndicator<HashMap<String, Object>>() {};

    private FirebaseSnapshotHelper() {}

    @Nullable
    public static HashMap<String, Object> asMap(@Nullable DataSnapshot snapshot) {
        if (snapshot == null || !snapshot.exists()) {
            return null;
        }
        return snapshot.getValue(MAP_TYPE);
    }

    @NonNull
    public static HashMap<String, Object> asMapOrEmpty(@Nullable DataSnapshot snapshot) {
        HashMap<String, Object> map = asMap(snapshot);
        return map != null ? map : new HashMap<>();
    }
}

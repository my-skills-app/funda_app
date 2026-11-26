package com.chill.with.examsaathi;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/** PRO status from Firebase {@code users/{uid}/proActive}. */
public final class ProSubscriptionHelper {

    public interface ProStatusCallback {
        void onStatus(boolean active, @NonNull String planId);
    }

    public interface SimpleCallback {
        void onSuccess();

        void onError(@NonNull String message);
    }

    private ProSubscriptionHelper() {}

    public static void listenProStatus(@NonNull ProStatusCallback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            callback.onStatus(false, "");
            return;
        }
        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(user.getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        boolean active = Boolean.TRUE.equals(snapshot.child("proActive").getValue(Boolean.class));
                        String planId = snapshot.child("proPlanId").getValue(String.class);
                        callback.onStatus(active, planId != null ? planId : "");
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onStatus(false, "");
                    }
                });
    }

    public static void activatePro(@NonNull String planId, @NonNull SimpleCallback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            callback.onError("Please sign in first");
            return;
        }
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("proActive", true);
        map.put("proPlanId", planId);
        map.put("proActivatedAt", System.currentTimeMillis());
        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(user.getUid())
                .updateChildren(map)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(
                        e.getMessage() != null ? e.getMessage() : "Could not activate PRO"));
    }

    public static void clearPro(@NonNull SimpleCallback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            callback.onError("Not signed in");
            return;
        }
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("proActive", false);
        map.put("proPlanId", null);
        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(user.getUid())
                .updateChildren(map)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(
                        e.getMessage() != null ? e.getMessage() : "Could not remove PRO"));
    }
}

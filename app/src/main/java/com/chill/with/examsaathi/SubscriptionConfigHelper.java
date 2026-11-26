package com.chill.with.examsaathi;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/** Reads {@code subscription_config/premiumEnabled} from Firebase. */
public final class SubscriptionConfigHelper {

    public interface PremiumEnabledCallback {
        void onResult(boolean enabled);
    }

    private SubscriptionConfigHelper() {}

    public static void readPremiumEnabledOnce(@NonNull PremiumEnabledCallback callback) {
        FirebaseDatabase.getInstance()
                .getReference("subscription_config")
                .child("premiumEnabled")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        callback.onResult(parseEnabled(snapshot));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onResult(true);
                    }
                });
    }

    public static boolean parseEnabled(@NonNull DataSnapshot snapshot) {
        if (!snapshot.exists()) {
            return true;
        }
        Boolean value = snapshot.getValue(Boolean.class);
        return value == null || value;
    }
}

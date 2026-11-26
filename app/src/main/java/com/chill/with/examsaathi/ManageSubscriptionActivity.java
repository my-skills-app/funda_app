package com.chill.with.examsaathi;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.widget.SwitchCompat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Admin: add/remove subscription plans in Firebase. */
public class ManageSubscriptionActivity extends AppCompatActivity {

    private TextView textTotalUsersCount;
    private LinearLayout adminPlansList;
    private EditText editPlanId;
    private EditText editPlanTitle;
    private EditText editPlanSubtitle;
    private EditText editPlanPrice;
    private EditText editPlanBadge;

    private SwitchCompat switchPremiumEnabled;
    private SwitchCompat switchUpdateEnabled;
    private SwitchCompat switchUpdateForce;
    private EditText editUpdateVersion;
    private EditText editUpdateTitle;
    private EditText editUpdateMessage;
    private EditText editUpdateUrl;

    private DatabaseReference plansRef;
    private DatabaseReference usersRef;
    private DatabaseReference subscriptionConfigRef;
    private DatabaseReference appUpdateConfigRef;
    private ValueEventListener plansListener;
    private ValueEventListener usersListener;
    private ValueEventListener subscriptionConfigListener;
    private ValueEventListener appUpdateConfigListener;
    private final List<SubscriptionPlan> plans = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!PushAdminHelper.isCurrentUserPushAdmin()) {
            AppToast.show(this, R.string.send_notification_admin_only);
            finish();
            return;
        }
        setContentView(R.layout.activity_manage_subscription);
        bindViews();
        setupWindowInsets();
        attachPlansListener();
        attachConfigListeners();
        attachUserCountListener();
    }

    private void bindViews() {
        findViewById(R.id.back_btn).setOnClickListener(v -> finish());
        textTotalUsersCount = findViewById(R.id.text_total_users_count);
        adminPlansList = findViewById(R.id.admin_plans_list);
        editPlanId = findViewById(R.id.edit_plan_id);
        editPlanTitle = findViewById(R.id.edit_plan_title);
        editPlanSubtitle = findViewById(R.id.edit_plan_subtitle);
        editPlanPrice = findViewById(R.id.edit_plan_price);
        editPlanBadge = findViewById(R.id.edit_plan_badge);
        switchPremiumEnabled = findViewById(R.id.switch_premium_enabled);
        switchUpdateEnabled = findViewById(R.id.switch_update_enabled);
        switchUpdateForce = findViewById(R.id.switch_update_force);
        editUpdateVersion = findViewById(R.id.edit_update_version);
        editUpdateTitle = findViewById(R.id.edit_update_title);
        editUpdateMessage = findViewById(R.id.edit_update_message);
        editUpdateUrl = findViewById(R.id.edit_update_url);
        findViewById(R.id.btn_add_plan).setOnClickListener(v -> onAddPlan());
        findViewById(R.id.btn_save_app_settings).setOnClickListener(v -> onSaveAppSettings());
    }

    private void attachUserCountListener() {
        usersRef = FirebaseDatabase.getInstance().getReference("users");
        usersListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long count = snapshot.getChildrenCount();
                runOnUiThread(() -> {
                    if (textTotalUsersCount != null) {
                        textTotalUsersCount.setText(String.format(Locale.getDefault(), "%,d", count));
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                runOnUiThread(() -> {
                    if (textTotalUsersCount != null) {
                        textTotalUsersCount.setText(getString(R.string.manage_total_users_error));
                    }
                });
            }
        };
        usersRef.addValueEventListener(usersListener);
    }

    private void attachConfigListeners() {
        subscriptionConfigRef = FirebaseDatabase.getInstance().getReference("subscription_config");
        subscriptionConfigListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean enabled = SubscriptionConfigHelper.parseEnabled(snapshot.child("premiumEnabled"));
                runOnUiThread(() -> {
                    if (switchPremiumEnabled != null) {
                        switchPremiumEnabled.setChecked(enabled);
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        subscriptionConfigRef.addValueEventListener(subscriptionConfigListener);

        appUpdateConfigRef = FirebaseDatabase.getInstance().getReference(AppUpdateHelper.PATH);
        appUpdateConfigListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                AppUpdateHelper.Config config = AppUpdateHelper.Config.fromSnapshot(snapshot);
                runOnUiThread(() -> bindAppUpdateConfig(config));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        appUpdateConfigRef.addValueEventListener(appUpdateConfigListener);
    }

    private void bindAppUpdateConfig(@NonNull AppUpdateHelper.Config config) {
        if (switchUpdateEnabled != null) {
            switchUpdateEnabled.setChecked(config.enabled);
        }
        if (switchUpdateForce != null) {
            switchUpdateForce.setChecked(config.forceUpdate);
        }
        if (editUpdateVersion != null) {
            editUpdateVersion.setText(config.latestVersion);
        }
        if (editUpdateTitle != null) {
            editUpdateTitle.setText(config.title);
        }
        if (editUpdateMessage != null) {
            editUpdateMessage.setText(config.message);
        }
        if (editUpdateUrl != null) {
            editUpdateUrl.setText(config.storeUrl);
        }
    }

    private void onSaveAppSettings() {
        boolean premiumEnabled = switchPremiumEnabled != null && switchPremiumEnabled.isChecked();
        subscriptionConfigRef.child("premiumEnabled").setValue(premiumEnabled);

        Map<String, Object> update = new HashMap<>();
        update.put("enabled", switchUpdateEnabled != null && switchUpdateEnabled.isChecked());
        update.put("latestVersion", editUpdateVersion.getText().toString().trim());
        update.put("title", editUpdateTitle.getText().toString().trim());
        update.put("message", editUpdateMessage.getText().toString().trim());
        update.put("storeUrl", editUpdateUrl.getText().toString().trim());
        update.put("forceUpdate", switchUpdateForce != null && switchUpdateForce.isChecked());

        appUpdateConfigRef.updateChildren(update)
                .addOnSuccessListener(unused -> AppToast.show(this, R.string.manage_app_settings_saved))
                .addOnFailureListener(e -> AppToast.show(this,
                        e.getMessage() != null ? e.getMessage() : getString(R.string.send_notification_failed)));
    }

    private void attachPlansListener() {
        plansRef = FirebaseDatabase.getInstance().getReference("subscription_plans");
        plansListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                plans.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    SubscriptionPlan plan = SubscriptionPlan.fromSnapshot(child.getKey(), child);
                    if (plan != null) {
                        plans.add(plan);
                    } else {
                        String title = child.child("title").getValue(String.class);
                        if (title != null) {
                            plans.add(new SubscriptionPlan(
                                    child.getKey(),
                                    title,
                                    str(child, "subtitle"),
                                    str(child, "priceLabel"),
                                    str(child, "badge"),
                                    false,
                                    intOrder(child)));
                        }
                    }
                }
                Collections.sort(plans, Comparator.comparingInt(p -> p.sortOrder));
                runOnUiThread(() -> rebuildAdminList());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                runOnUiThread(() -> AppToast.show(ManageSubscriptionActivity.this, error.getMessage()));
            }
        };
        plansRef.addValueEventListener(plansListener);
    }

    private void rebuildAdminList() {
        adminPlansList.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (SubscriptionPlan plan : plans) {
            View row = inflater.inflate(R.layout.item_admin_subscription_plan, adminPlansList, false);
            TextView title = row.findViewById(R.id.text_admin_plan_title);
            TextView meta = row.findViewById(R.id.text_admin_plan_meta);
            TextView remove = row.findViewById(R.id.btn_remove_plan);
            String status = plan.active ? "" : " • hidden";
            title.setText(plan.title + status);
            meta.setText(plan.planId + " · " + plan.priceLabel
                    + (plan.badge.isEmpty() ? "" : " · " + plan.badge));
            remove.setOnClickListener(v -> confirmRemovePlan(plan.planId));
            adminPlansList.addView(row);
        }
    }

    private void confirmRemovePlan(@NonNull String planId) {
        plansRef.child(planId).removeValue()
                .addOnSuccessListener(unused -> AppToast.show(this, R.string.manage_subscription_removed))
                .addOnFailureListener(e -> AppToast.show(this,
                        e.getMessage() != null ? e.getMessage() : getString(R.string.send_notification_failed)));
    }

    private void onAddPlan() {
        String planId = editPlanId.getText().toString().trim().toLowerCase().replace(" ", "_");
        String title = editPlanTitle.getText().toString().trim();
        String price = editPlanPrice.getText().toString().trim();
        if (planId.isEmpty()) {
            editPlanId.setError(getString(R.string.manage_plan_id_hint));
            return;
        }
        if (title.isEmpty()) {
            editPlanTitle.setError(getString(R.string.manage_plan_title_hint));
            return;
        }
        if (price.isEmpty()) {
            editPlanPrice.setError(getString(R.string.manage_plan_price_hint));
            return;
        }
        Map<String, Object> row = new HashMap<>();
        row.put("title", title);
        row.put("subtitle", editPlanSubtitle.getText().toString().trim());
        row.put("priceLabel", price);
        String badge = editPlanBadge.getText().toString().trim();
        if (!badge.isEmpty()) {
            row.put("badge", badge);
        }
        row.put("active", true);
        row.put("sortOrder", plans.size() + 1);
        plansRef.child(planId).setValue(row)
                .addOnSuccessListener(unused -> {
                    editPlanId.setText("");
                    editPlanTitle.setText("");
                    editPlanSubtitle.setText("");
                    editPlanPrice.setText("");
                    editPlanBadge.setText("");
                    AppToast.show(this, R.string.manage_subscription_added);
                })
                .addOnFailureListener(e -> AppToast.show(this,
                        e.getMessage() != null ? e.getMessage() : getString(R.string.send_notification_failed)));
    }

    @NonNull
    private static String str(@NonNull DataSnapshot s, @NonNull String key) {
        String v = s.child(key).getValue(String.class);
        return v != null ? v.trim() : "";
    }

    private static int intOrder(@NonNull DataSnapshot s) {
        Long v = s.child("sortOrder").getValue(Long.class);
        return v != null ? v.intValue() : 0;
    }

    private void setupWindowInsets() {
        View root = findViewById(R.id.manage_subscription_root);
        View toolbar = findViewById(R.id.manage_subscription_toolbar);
        View scrollInner = findViewById(R.id.manage_scroll_inner);
        if (root == null || toolbar == null || scrollInner == null) {
            return;
        }
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            int bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            toolbar.setPadding(toolbar.getPaddingLeft(), top + dp(8), toolbar.getPaddingRight(), toolbar.getPaddingBottom());
            scrollInner.setPadding(scrollInner.getPaddingLeft(), scrollInner.getPaddingTop(), scrollInner.getPaddingRight(), bottom + dp(24));
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
    }

    @Override
    protected void onDestroy() {
        if (plansRef != null && plansListener != null) {
            plansRef.removeEventListener(plansListener);
        }
        if (usersRef != null && usersListener != null) {
            usersRef.removeEventListener(usersListener);
        }
        if (subscriptionConfigRef != null && subscriptionConfigListener != null) {
            subscriptionConfigRef.removeEventListener(subscriptionConfigListener);
        }
        if (appUpdateConfigRef != null && appUpdateConfigListener != null) {
            appUpdateConfigRef.removeEventListener(appUpdateConfigListener);
        }
        super.onDestroy();
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }
}

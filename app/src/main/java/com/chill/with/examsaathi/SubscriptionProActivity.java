package com.chill.with.examsaathi;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
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
import java.util.List;

public class SubscriptionProActivity extends AppCompatActivity {

    private LinearLayout plansContainer;
    private LinearLayout featuresList;
    private TextView btnSubscribe;
    private TextView btnRestore;
    private View activeProBanner;
    private FrameLayout overlayLoading;
    private TextView textHeroTitle;
    private TextView textHeroSubtitle;

    private final List<SubscriptionPlan> plans = new ArrayList<>();
    private final List<View> planViews = new ArrayList<>();
    private String selectedPlanId = "";
    private boolean userIsPro;

    private ValueEventListener plansListener;
    private DatabaseReference plansRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SubscriptionConfigHelper.readPremiumEnabledOnce(enabled -> runOnUiThread(() -> {
            if (!enabled) {
                AppToast.show(this, R.string.manage_premium_disabled_user);
                finish();
                return;
            }
            bindSubscriptionUi(savedInstanceState);
        }));
    }

    private void bindSubscriptionUi(Bundle savedInstanceState) {
        setContentView(R.layout.activity_subscription_pro);
        bindViews();
        setupWindowInsets();
        populateFeatures();
        attachPlansListener();
        ProSubscriptionHelper.listenProStatus((active, planId) -> runOnUiThread(() -> {
            userIsPro = active;
            if (active && planId != null && !planId.isEmpty()) {
                selectedPlanId = planId;
            }
            refreshProStateUi();
            updatePlanSelectionUi();
        }));
    }

    private void bindViews() {
        findViewById(R.id.back_btn).setOnClickListener(v -> finish());
        plansContainer = findViewById(R.id.plans_container);
        featuresList = findViewById(R.id.features_list);
        btnSubscribe = findViewById(R.id.btn_subscribe);
        btnRestore = findViewById(R.id.btn_restore);
        activeProBanner = findViewById(R.id.active_pro_banner);
        overlayLoading = findViewById(R.id.overlay_loading);
        textHeroTitle = findViewById(R.id.text_hero_title);
        textHeroSubtitle = findViewById(R.id.text_hero_subtitle);
        btnSubscribe.setOnClickListener(v -> onSubscribeClicked());
        btnRestore.setOnClickListener(v -> onRestoreClicked());
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
                    }
                }
                Collections.sort(plans, Comparator.comparingInt(p -> p.sortOrder));
                runOnUiThread(() -> rebuildPlanCards());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                runOnUiThread(() -> AppToast.show(SubscriptionProActivity.this, error.getMessage()));
            }
        };
        plansRef.addValueEventListener(plansListener);

        FirebaseDatabase.getInstance()
                .getReference("subscription_config")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String title = snapshot.child("heroTitle").getValue(String.class);
                        String subtitle = snapshot.child("heroSubtitle").getValue(String.class);
                        if (title != null && !title.trim().isEmpty()) {
                            textHeroTitle.setText(title.trim());
                        }
                        if (subtitle != null && !subtitle.trim().isEmpty()) {
                            textHeroSubtitle.setText(subtitle.trim());
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void rebuildPlanCards() {
        plansContainer.removeAllViews();
        planViews.clear();
        LayoutInflater inflater = LayoutInflater.from(this);
        if (plans.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(R.string.subscription_no_plans);
            empty.setTextColor(getColor(R.color.text_color_light));
            plansContainer.addView(empty);
            btnSubscribe.setEnabled(false);
            return;
        }
        if (selectedPlanId.isEmpty()) {
            selectedPlanId = plans.get(0).planId;
        }
        for (SubscriptionPlan plan : plans) {
            View card = inflater.inflate(R.layout.item_subscription_plan, plansContainer, false);
            View root = card.findViewById(R.id.plan_root);
            TextView title = card.findViewById(R.id.text_plan_title);
            TextView subtitle = card.findViewById(R.id.text_plan_subtitle);
            TextView price = card.findViewById(R.id.text_plan_price);
            TextView badge = card.findViewById(R.id.text_plan_badge);
            title.setText(plan.title);
            subtitle.setText(plan.subtitle);
            price.setText(plan.priceLabel);
            if (plan.badge.isEmpty()) {
                badge.setVisibility(View.GONE);
            } else {
                badge.setVisibility(View.VISIBLE);
                badge.setText(plan.badge);
            }
            root.setOnClickListener(v -> selectPlan(plan.planId));
            planViews.add(root);
            plansContainer.addView(card);
        }
        btnSubscribe.setEnabled(true);
        updatePlanSelectionUi();
    }

    private void populateFeatures() {
        @StringRes int[] defaults = {
                R.string.subscription_feature_1,
                R.string.subscription_feature_2,
                R.string.subscription_feature_3,
                R.string.subscription_feature_4,
                R.string.subscription_feature_5
        };
        LayoutInflater inflater = LayoutInflater.from(this);
        for (@StringRes int resId : defaults) {
            View row = inflater.inflate(R.layout.item_subscription_feature, featuresList, false);
            ((TextView) row.findViewById(R.id.text_feature)).setText(resId);
            featuresList.addView(row);
        }
        FirebaseDatabase.getInstance()
                .getReference("subscription_config")
                .child("benefits")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            return;
                        }
                        featuresList.removeAllViews();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            String text = child.getValue(String.class);
                            if (text == null || text.trim().isEmpty()) {
                                continue;
                            }
                            View row = inflater.inflate(R.layout.item_subscription_feature, featuresList, false);
                            ((TextView) row.findViewById(R.id.text_feature)).setText(text.trim());
                            featuresList.addView(row);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void selectPlan(@NonNull String planId) {
        selectedPlanId = planId;
        updatePlanSelectionUi();
    }

    private void updatePlanSelectionUi() {
        for (int i = 0; i < planViews.size() && i < plans.size(); i++) {
            planViews.get(i).setSelected(plans.get(i).planId.equals(selectedPlanId));
        }
        SubscriptionPlan selected = findPlan(selectedPlanId);
        if (selected != null) {
            btnSubscribe.setText(getString(R.string.subscription_subscribe_plan, selected.title));
        }
    }

    @Nullable
    private SubscriptionPlan findPlan(@NonNull String planId) {
        for (SubscriptionPlan plan : plans) {
            if (plan.planId.equals(planId)) {
                return plan;
            }
        }
        return null;
    }

    private void refreshProStateUi() {
        activeProBanner.setVisibility(userIsPro ? View.VISIBLE : View.GONE);
        btnSubscribe.setVisibility(userIsPro ? View.GONE : View.VISIBLE);
        for (View v : planViews) {
            v.setEnabled(!userIsPro);
        }
    }

    private void onSubscribeClicked() {
        if (userIsPro) {
            AppToast.show(this, R.string.subscription_already_active);
            return;
        }
        if (selectedPlanId.isEmpty()) {
            AppToast.show(this, R.string.subscription_no_plans);
            return;
        }
        setLoading(true);
        ProSubscriptionHelper.activatePro(selectedPlanId, new ProSubscriptionHelper.SimpleCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    setLoading(false);
                    AppToast.show(SubscriptionProActivity.this, R.string.subscription_success);
                });
            }

            @Override
            public void onError(@NonNull String message) {
                runOnUiThread(() -> {
                    setLoading(false);
                    AppToast.show(SubscriptionProActivity.this, message);
                });
            }
        });
    }

    private void onRestoreClicked() {
        if (userIsPro) {
            AppToast.show(this, R.string.subscription_restore_found);
            return;
        }
        AppToast.show(this, R.string.subscription_restore_none);
    }

    private void setLoading(boolean loading) {
        overlayLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSubscribe.setEnabled(!loading && !plans.isEmpty());
        btnRestore.setEnabled(!loading);
    }

    private void setupWindowInsets() {
        View root = findViewById(R.id.subscription_pro_root);
        View toolbar = findViewById(R.id.subscription_toolbar);
        View scrollInner = findViewById(R.id.scroll_subscription_inner);
        if (root == null || toolbar == null || scrollInner == null) {
            return;
        }
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            int bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            toolbar.setPadding(
                    toolbar.getPaddingLeft(),
                    top + dp(8),
                    toolbar.getPaddingRight(),
                    toolbar.getPaddingBottom());
            scrollInner.setPadding(
                    scrollInner.getPaddingLeft(),
                    scrollInner.getPaddingTop(),
                    scrollInner.getPaddingRight(),
                    bottom + dp(24));
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
    }

    @Override
    protected void onDestroy() {
        if (plansRef != null && plansListener != null) {
            plansRef.removeEventListener(plansListener);
        }
        super.onDestroy();
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }
}

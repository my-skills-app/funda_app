package com.chill.with.examsaathi;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** PRO subscription offer as a bottom sheet on the home dashboard (with optional promo PiP video). */
public final class SubscriptionProBottomSheet {

    private SubscriptionProBottomSheet() {}

    /**
     * Shows subscription bottom sheet for non-PRO users when home opens after app launch.
     */
    public static void showForNonProUser(
            @NonNull AppCompatActivity activity,
            @Nullable GitaDashboard.Data data) {
        SubscriptionConfigHelper.readPremiumEnabledOnce(enabled -> {
            if (!enabled) {
                return;
            }
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                return;
            }
            String adUrl = null;
            if (data != null && data.ad != null && data.ad.videoUrl != null) {
                adUrl = data.ad.videoUrl.trim();
                if (adUrl.isEmpty()) {
                    adUrl = null;
                }
            }
            final String promoVideoUrl = adUrl;

            FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(user.getUid())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (Boolean.TRUE.equals(snapshot.child("proActive").getValue(Boolean.class))) {
                                return;
                            }
                            activity.runOnUiThread(() -> show(activity, promoVideoUrl));
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            activity.runOnUiThread(() -> show(activity, promoVideoUrl));
                        }
                    });
        });
    }

    public static void show(@NonNull AppCompatActivity activity, @Nullable String promoVideoUrl) {
        if (promoVideoUrl != null && !promoVideoUrl.isEmpty()) {
            Intent pip = new Intent(activity, PromoAdPipActivity.class);
            pip.putExtra(PromoAdPipActivity.EXTRA_VIDEO_URL, promoVideoUrl);
            pip.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            activity.startActivity(pip);
            activity.overridePendingTransition(0, 0);
        }

        BottomSheetDialog dialog = new BottomSheetDialog(activity, R.style.VideoQualityBottomSheetTheme);
        View sheetView = LayoutInflater.from(activity).inflate(R.layout.bottom_sheet_subscription_pro, null);
        SheetController controller = new SheetController(activity, sheetView, dialog);
        controller.bind();

        dialog.setContentView(sheetView);
        dialog.setOnShowListener(d -> {
            BottomSheetDialog sheetDialog = (BottomSheetDialog) d;
            FrameLayout bottomSheet = sheetDialog.findViewById(
                    com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setSkipCollapsed(true);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });
        dialog.setOnDismissListener(d -> {
            controller.destroy();
            PromoAdPipActivity.stopIfRunning();
        });
        dialog.show();
    }

    private static final class SheetController {

        private final AppCompatActivity activity;
        private final BottomSheetDialog dialog;
        private final LinearLayout plansContainer;
        private final LinearLayout featuresList;
        private final TextView btnSubscribe;
        private final TextView btnRestore;
        private final View activeProBanner;
        private final View overlayLoading;
        private final TextView textHeroTitle;
        private final TextView textHeroSubtitle;

        private final List<SubscriptionPlan> plans = new ArrayList<>();
        private final List<View> planViews = new ArrayList<>();
        private String selectedPlanId = "";
        private boolean userIsPro;

        private ValueEventListener plansListener;
        private DatabaseReference plansRef;

        SheetController(AppCompatActivity activity, View root, BottomSheetDialog dialog) {
            this.activity = activity;
            this.dialog = dialog;
            plansContainer = root.findViewById(R.id.plans_container);
            featuresList = root.findViewById(R.id.features_list);
            btnSubscribe = root.findViewById(R.id.btn_subscribe);
            btnRestore = root.findViewById(R.id.btn_restore);
            activeProBanner = root.findViewById(R.id.active_pro_banner);
            overlayLoading = root.findViewById(R.id.overlay_loading);
            textHeroTitle = root.findViewById(R.id.text_hero_title);
            textHeroSubtitle = root.findViewById(R.id.text_hero_subtitle);
            TextView skip = root.findViewById(R.id.btn_sheet_skip);
            if (skip != null) {
                skip.setOnClickListener(v -> dialog.dismiss());
            }
        }

        void bind() {
            btnSubscribe.setOnClickListener(v -> onSubscribeClicked());
            btnRestore.setOnClickListener(v -> onRestoreClicked());
            populateFeatures();
            attachPlansListener();
            ProSubscriptionHelper.listenProStatus((active, planId) -> activity.runOnUiThread(() -> {
                userIsPro = active;
                if (active && planId != null && !planId.isEmpty()) {
                    selectedPlanId = planId;
                }
                refreshProStateUi();
                updatePlanSelectionUi();
            }));
        }

        void destroy() {
            if (plansRef != null && plansListener != null) {
                plansRef.removeEventListener(plansListener);
            }
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
                    activity.runOnUiThread(() -> rebuildPlanCards());
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    activity.runOnUiThread(() -> AppToast.show(activity, error.getMessage()));
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
                            activity.runOnUiThread(() -> {
                                if (title != null && !title.trim().isEmpty()) {
                                    textHeroTitle.setText(title.trim());
                                }
                                if (subtitle != null && !subtitle.trim().isEmpty()) {
                                    textHeroSubtitle.setText(subtitle.trim());
                                }
                            });
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
        }

        private void rebuildPlanCards() {
            plansContainer.removeAllViews();
            planViews.clear();
            LayoutInflater inflater = LayoutInflater.from(activity);
            if (plans.isEmpty()) {
                TextView empty = new TextView(activity);
                empty.setText(R.string.subscription_no_plans);
                empty.setTextColor(activity.getColor(R.color.text_color_light));
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
            LayoutInflater inflater = LayoutInflater.from(activity);
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
                            activity.runOnUiThread(() -> {
                                featuresList.removeAllViews();
                                for (DataSnapshot child : snapshot.getChildren()) {
                                    String text = child.getValue(String.class);
                                    if (text == null || text.trim().isEmpty()) {
                                        continue;
                                    }
                                    View row = inflater.inflate(
                                            R.layout.item_subscription_feature, featuresList, false);
                                    ((TextView) row.findViewById(R.id.text_feature)).setText(text.trim());
                                    featuresList.addView(row);
                                }
                            });
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
                btnSubscribe.setText(activity.getString(
                        R.string.subscription_subscribe_plan, selected.title));
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
            if (userIsPro) {
                dialog.dismiss();
            }
        }

        private void onSubscribeClicked() {
            if (userIsPro) {
                AppToast.show(activity, R.string.subscription_already_active);
                return;
            }
            if (selectedPlanId.isEmpty()) {
                AppToast.show(activity, R.string.subscription_no_plans);
                return;
            }
            setLoading(true);
            ProSubscriptionHelper.activatePro(selectedPlanId, new ProSubscriptionHelper.SimpleCallback() {
                @Override
                public void onSuccess() {
                    activity.runOnUiThread(() -> {
                        setLoading(false);
                        AppToast.show(activity, R.string.subscription_success);
                        dialog.dismiss();
                    });
                }

                @Override
                public void onError(@NonNull String message) {
                    activity.runOnUiThread(() -> {
                        setLoading(false);
                        AppToast.show(activity, message);
                    });
                }
            });
        }

        private void onRestoreClicked() {
            if (userIsPro) {
                AppToast.show(activity, R.string.subscription_restore_found);
                return;
            }
            AppToast.show(activity, R.string.subscription_restore_none);
        }

        private void setLoading(boolean loading) {
            overlayLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
            btnSubscribe.setEnabled(!loading && !plans.isEmpty());
            btnRestore.setEnabled(!loading);
        }
    }
}

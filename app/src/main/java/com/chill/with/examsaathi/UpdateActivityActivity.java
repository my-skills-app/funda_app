package com.chill.with.examsaathi;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

/** Shown when Firebase {@code app_update_config} requires a newer app version. */
public class UpdateActivityActivity extends AppCompatActivity {

    public static final String EXTRA_TITLE = "update_title";
    public static final String EXTRA_MESSAGE = "update_message";
    public static final String EXTRA_STORE_URL = "update_store_url";
    public static final String EXTRA_FORCE = "update_force";

    private String storeUrl = "";
    private boolean forceUpdate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update);

        String title = getIntent().getStringExtra(EXTRA_TITLE);
        String message = getIntent().getStringExtra(EXTRA_MESSAGE);
        storeUrl = getIntent().getStringExtra(EXTRA_STORE_URL);
        forceUpdate = getIntent().getBooleanExtra(EXTRA_FORCE, false);
        if (storeUrl == null) {
            storeUrl = "";
        }

        TextView textTitle = findViewById(R.id.text_update_title);
        TextView textMessage = findViewById(R.id.text_update_message);
        TextView btnUpdate = findViewById(R.id.btn_update_now);
        TextView btnLater = findViewById(R.id.btn_update_later);

        if (title != null && !title.trim().isEmpty()) {
            textTitle.setText(title.trim());
        }
        if (message != null && !message.trim().isEmpty()) {
            textMessage.setText(message.trim());
        }

        btnUpdate.setOnClickListener(v -> openStoreUrl());
        btnLater.setOnClickListener(v -> continueToApp());

        if (forceUpdate) {
            btnLater.setVisibility(View.GONE);
            getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    // Block back when update is mandatory.
                }
            });
        } else {
            btnLater.setVisibility(View.VISIBLE);
        }
    }

    private void openStoreUrl() {
        if (storeUrl.isEmpty()) {
            AppToast.show(this, R.string.app_update_no_link);
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(storeUrl));
            startActivity(intent);
        } catch (Exception e) {
            AppToast.show(this, R.string.app_update_open_failed);
        }
    }

    private void continueToApp() {
        startActivity(new Intent(this, SplashScreenActivity.class)
                .putExtra(SplashScreenActivity.EXTRA_SKIP_UPDATE_CHECK, true)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
        finish();
    }
}

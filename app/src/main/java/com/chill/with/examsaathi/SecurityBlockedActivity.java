package com.chill.with.examsaathi;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/** Shown when emulator, MITM tooling, proxy, or tampering is detected (release builds). */
public class SecurityBlockedActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_security_blocked);

        TextView messageView = findViewById(R.id.security_message);
        int reasonRes = getIntent().getIntExtra(AppSecurityManager.EXTRA_BLOCK_REASON, 0);
        if (reasonRes != 0) {
            messageView.setText(reasonRes);
        } else {
            messageView.setText(R.string.security_block_generic);
        }

        findViewById(R.id.security_exit_btn).setOnClickListener(v -> finishAffinity());
    }

    @Override
    public void onBackPressed() {
        finishAffinity();
    }
}

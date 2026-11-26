package com.chill.with.examsaathi;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/** Static local login for Prep backend emulator testing (no token changes). */
public class SignInActivity extends AppCompatActivity {

    private boolean doubleBackToExitPressedOnce;
    private LinearLayout btnContinue;
    private LinearLayout btnSecondary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sign_in);
        initialize();
        initializeLogic();
    }

    private void initialize() {
        btnContinue = findViewById(R.id.btn_google);
        btnSecondary = findViewById(R.id.linear4);

        TextView title = findViewById(R.id.textview2);
        TextView subtitle = findViewById(R.id.textview1);
        TextView continueLabel = findViewById(R.id.textview3);
        TextView secondaryLabel = findViewById(R.id.textview4);
        ImageView continueIcon = findViewById(R.id.imageview1);
        ImageView secondaryIcon = findViewById(R.id.imageview2);

        if (title != null) {
            title.setText(R.string.prep_local_sign_in_title);
        }
        if (subtitle != null) {
            subtitle.setText(R.string.prep_local_sign_in_subtitle);
        }
        if (continueLabel != null) {
            continueLabel.setText(R.string.prep_local_enter_app);
        }
        if (secondaryLabel != null) {
            secondaryLabel.setText(R.string.prep_local_enter_app_hint);
        }
        if (continueIcon != null) {
            continueIcon.setImageResource(R.drawable.logo);
        }
        if (secondaryIcon != null) {
            secondaryIcon.setVisibility(View.GONE);
        }
        if (btnSecondary != null) {
            btnSecondary.setVisibility(View.GONE);
        }

        if (btnContinue != null) {
            btnContinue.setOnClickListener(v -> enterApp());
        }
    }

    private void initializeLogic() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setNavigationBarColor(getColor(R.color.colorPrimary));
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
        doubleBackToExitPressedOnce = false;

        if (PrepLocalSession.isLoggedIn(this)) {
            openMainAndFinish();
        }
    }

    private void enterApp() {
        PrepLocalSession.setLoggedIn(this, true);
        openMainAndFinish();
    }

    private void openMainAndFinish() {
        android.content.Intent intent;
        if (!PrepCourseStore.isOnboardingDone(getApplicationContext())) {
            intent = new android.content.Intent(getApplicationContext(), ExamOnboardingActivity.class);
        } else {
            intent = new android.content.Intent(getApplicationContext(), MainActivity.class);
        }
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }
        doubleBackToExitPressedOnce = true;
        _showDarkToast("Press back again to exit");
        new Handler().postDelayed(() -> doubleBackToExitPressedOnce = false, 2000);
    }

    public void _showDarkToast(final String message) {
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.custom_toast, null);
        TextView text = layout.findViewById(R.id.toast_text);
        text.setText(message);
        Animation fadeIn = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_in);
        layout.startAnimation(fadeIn);
        Toast toast = new Toast(getApplicationContext());
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.setGravity(Gravity.BOTTOM, 0, 100);
        toast.show();
    }
}

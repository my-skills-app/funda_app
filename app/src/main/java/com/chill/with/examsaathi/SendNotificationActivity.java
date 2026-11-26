package com.chill.with.examsaathi;

import android.Manifest;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.widget.CompoundButtonCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;

public class SendNotificationActivity extends AppCompatActivity {

    private static final int REQ_PICK_IMAGE = 9102;
    private static final int REQ_MEDIA_READ = 9103;

    private EditText editTitle;
    private EditText editBody;
    private EditText editLink;
    private ImageView imgPreview;
    private LinearLayout layoutPickPlaceholder;
    private FrameLayout frameImagePick;
    private TextView btnRemoveImage;
    private RadioGroup radioTarget;
    private TextView btnSend;
    private FrameLayout overlaySending;

    @Nullable
    private Uri selectedImageUri;

    private final Intent pickImageIntent = new Intent(Intent.ACTION_GET_CONTENT);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!PushAdminHelper.isCurrentUserPushAdmin()) {
            AppToast.show(this, R.string.send_notification_admin_only);
            finish();
            return;
        }
        setContentView(R.layout.activity_send_notification);
        bindViews();
        setupWindowInsets();
        PushNotificationHelper.requestPermissionSilently(this);
        pickImageIntent.setType("image/*");
        pickImageIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
    }

    private void bindViews() {
        findViewById(R.id.back_btn).setOnClickListener(v -> finish());
        editTitle = findViewById(R.id.edit_title);
        editBody = findViewById(R.id.edit_body);
        editLink = findViewById(R.id.edit_link);
        imgPreview = findViewById(R.id.img_preview);
        layoutPickPlaceholder = findViewById(R.id.layout_pick_placeholder);
        frameImagePick = findViewById(R.id.frame_image_pick);
        btnRemoveImage = findViewById(R.id.btn_remove_image);
        radioTarget = findViewById(R.id.radio_target);
        btnSend = findViewById(R.id.btn_send);
        overlaySending = findViewById(R.id.overlay_sending);
        btnSend.setOnClickListener(v -> onSendClicked());
        frameImagePick.setOnClickListener(v -> openImagePicker());
        btnRemoveImage.setOnClickListener(v -> clearSelectedImage());
        applyRadioButtonTint();
    }

    private void applyRadioButtonTint() {
        android.content.res.ColorStateList tint = ContextCompat.getColorStateList(
                this, R.color.radio_app_color_tint);
        RadioButton allUsers = findViewById(R.id.radio_all_users);
        RadioButton thisDevice = findViewById(R.id.radio_this_device);
        CompoundButtonCompat.setButtonTintList(allUsers, tint);
        CompoundButtonCompat.setButtonTintList(thisDevice, tint);
    }

    private void setupWindowInsets() {
        View root = findViewById(R.id.send_notification_root);
        View toolbar = findViewById(R.id.send_notification_toolbar);
        View scrollInner = findViewById(R.id.scroll_content_inner);
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

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }

    private void openImagePicker() {
        if (!hasMediaReadPermission()) {
            requestMediaReadPermission();
            return;
        }
        startActivityForResult(pickImageIntent, REQ_PICK_IMAGE);
    }

    private boolean hasMediaReadPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestMediaReadPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                    this, new String[]{Manifest.permission.READ_MEDIA_IMAGES}, REQ_MEDIA_READ);
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    },
                    REQ_MEDIA_READ);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_MEDIA_READ && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startActivityForResult(pickImageIntent, REQ_PICK_IMAGE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_PICK_IMAGE || resultCode != RESULT_OK || data == null) {
            return;
        }
        Uri uri = data.getData();
        if (uri == null && data.getClipData() != null && data.getClipData().getItemCount() > 0) {
            ClipData.Item item = data.getClipData().getItemAt(0);
            uri = item != null ? item.getUri() : null;
        }
        if (uri != null) {
            setSelectedImage(uri);
        }
    }

    private void setSelectedImage(@NonNull Uri uri) {
        selectedImageUri = uri;
        imgPreview.setVisibility(View.VISIBLE);
        layoutPickPlaceholder.setVisibility(View.GONE);
        btnRemoveImage.setVisibility(View.VISIBLE);
        Glide.with(this).load(uri).centerCrop().into(imgPreview);
    }

    private void clearSelectedImage() {
        selectedImageUri = null;
        imgPreview.setVisibility(View.GONE);
        imgPreview.setImageDrawable(null);
        layoutPickPlaceholder.setVisibility(View.VISIBLE);
        btnRemoveImage.setVisibility(View.GONE);
    }

    private void onSendClicked() {
        String title = editTitle.getText().toString().trim();
        String body = editBody.getText().toString().trim();
        String link = editLink.getText().toString().trim();
        if (title.isEmpty()) {
            editTitle.setError(getString(R.string.send_notification_title_required));
            editTitle.requestFocus();
            return;
        }
        if (body.isEmpty()) {
            editBody.setError(getString(R.string.send_notification_body_required));
            editBody.requestFocus();
            return;
        }
        if (!link.isEmpty() && !isValidHttpUrl(link)) {
            editLink.setError(getString(R.string.send_notification_link_invalid));
            editLink.requestFocus();
            return;
        }
        PushPayload payload = new PushPayload(title, body, null, link.isEmpty() ? null : link);
        boolean allUsers = radioTarget.getCheckedRadioButtonId() == R.id.radio_all_users;
        setSending(true);
        PushBroadcastSender.send(this, payload, allUsers, selectedImageUri, new PushBroadcastSender.Callback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    setSending(false);
                    AppToast.show(SendNotificationActivity.this, R.string.send_notification_success);
                    if (allUsers) {
                        finish();
                    } else {
                        clearSelectedImage();
                        editTitle.setText("");
                        editBody.setText("");
                        editLink.setText("");
                    }
                });
            }

            @Override
            public void onError(@NonNull String message) {
                runOnUiThread(() -> {
                    setSending(false);
                    AppToast.show(SendNotificationActivity.this, message);
                });
            }
        });
    }

    private static boolean isValidHttpUrl(@NonNull String url) {
        return url.startsWith("https://") || url.startsWith("http://");
    }

    private void setSending(boolean sending) {
        btnSend.setEnabled(!sending);
        radioTarget.setEnabled(!sending);
        editTitle.setEnabled(!sending);
        editBody.setEnabled(!sending);
        editLink.setEnabled(!sending);
        frameImagePick.setEnabled(!sending);
        btnRemoveImage.setEnabled(!sending);
        overlaySending.setVisibility(sending ? View.VISIBLE : View.GONE);
        btnSend.setAlpha(sending ? 0.7f : 1f);
        btnSend.setText(sending
                ? getString(R.string.send_notification_sending)
                : getString(R.string.send_notification_button));
    }
}

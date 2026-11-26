package com.chill.with.examsaathi;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Utils {

    /** UTF-8 URL encoding compatible with API 21+ (Charset overload is API 33+). */
    public static String urlEncodeUtf8(String value) {
        try {
            return URLEncoder.encode(value != null ? value : "", "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError("UTF-8 not supported", e);
        }
    }

    private static AlertDialog maintenanceDialog;
    private static ValueEventListener maintenanceListener;

    public static void checkMaintenance(Activity activity) {
        DatabaseReference maintenanceRef = FirebaseDatabase.getInstance().getReference("maintenance");

        if (maintenanceListener != null) {
            maintenanceRef.removeEventListener(maintenanceListener);
        }

        maintenanceListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Boolean isActive = snapshot.child("active").getValue(Boolean.class);

                    if (isActive != null && isActive) {
                        String title = snapshot.child("title").getValue(String.class);
                        String message = snapshot.child("message").getValue(String.class);
                        String buttonText = snapshot.child("button_text").getValue(String.class);

                        showDialog(activity, title, message, buttonText);
                    } else {
                        dismissDialog();
                    }
                } else {
                    dismissDialog();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                dismissDialog();
            }
        };

        maintenanceRef.addValueEventListener(maintenanceListener);
    }

    private static void showDialog(Activity activity, String title, String message, String buttonText) {
        if (maintenanceDialog != null && maintenanceDialog.isShowing()) {
            return;
        }

        LayoutInflater inflater = activity.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.maintenance, null);

        TextView titleView = dialogView.findViewById(R.id.title);
        TextView messageView = dialogView.findViewById(R.id.message_text);
        TextView btnOk = dialogView.findViewById(R.id.btn_ok);
        LinearLayout btnCancel = dialogView.findViewById(R.id.btn_cancel);

        titleView.setText(title != null ? title : "Maintenance");
        messageView.setText(message != null ? message : "App is under maintenance.");
        btnOk.setText(buttonText != null ? buttonText : "Exit");

        btnCancel.setOnClickListener(v -> {
            
        });

        maintenanceDialog = new AlertDialog.Builder(activity, R.style.CustomDialogTheme)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        maintenanceDialog.setCanceledOnTouchOutside(false);

        btnOk.setOnClickListener(v -> {
            maintenanceDialog.dismiss();
            activity.finishAffinity();
        });

        maintenanceDialog.show();
    }

    private static void dismissDialog() {
        if (maintenanceDialog != null && maintenanceDialog.isShowing()) {
            maintenanceDialog.dismiss();
        }
    }

    public static void removeListener() {
        if (maintenanceListener != null) {
            DatabaseReference maintenanceRef = FirebaseDatabase.getInstance().getReference("maintenance");
            maintenanceRef.removeEventListener(maintenanceListener);
            maintenanceListener = null;
        }
    }
}
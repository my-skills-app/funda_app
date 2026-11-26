package com.chill.with.examsaathi;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.Player;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Bottom sheet for playback speed — same UX pattern as {@link VideoQualityDialog}.
 */
public final class PlaybackSpeedDialog {

    public interface Listener {
        void onDismiss();
    }

    private static final float[] SPEEDS = {
            0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f
    };

    private PlaybackSpeedDialog() {}

    public static void show(
            @NonNull AppCompatActivity activity,
            @Nullable Player player,
            @Nullable Listener listener) {
        if (player == null) {
            if (listener != null) {
                listener.onDismiss();
            }
            return;
        }

        BottomSheetDialog bottomSheet = new BottomSheetDialog(activity, R.style.VideoQualityBottomSheetTheme);
        View sheetView = LayoutInflater.from(activity).inflate(R.layout.bottom_sheet_playback_speed, null);
        RecyclerView recyclerView = sheetView.findViewById(R.id.recyclerSpeedOptions);

        float current = player.getPlaybackParameters().speed;
        int selectedIndex = findClosestIndex(current);

        bottomSheet.setContentView(sheetView);
        bottomSheet.setOnDismissListener(dialog -> {
            if (listener != null) {
                listener.onDismiss();
            }
        });

        List<Float> speeds = Arrays.asList(
                0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f);
        SpeedAdapter adapter = new SpeedAdapter(speeds, selectedIndex, speed -> {
            player.setPlaybackSpeed(speed);
            bottomSheet.dismiss();
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        recyclerView.setAdapter(adapter);

        bottomSheet.setOnShowListener(dialog -> {
            FrameLayout bottomSheetContainer = bottomSheet.findViewById(
                    com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheetContainer != null) {
                BottomSheetBehavior.from(bottomSheetContainer).setSkipCollapsed(true);
                BottomSheetBehavior.from(bottomSheetContainer).setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });

        bottomSheet.show();
    }

    private static int findClosestIndex(float speed) {
        int best = 2; // 1.0x
        float bestDiff = Float.MAX_VALUE;
        for (int i = 0; i < SPEEDS.length; i++) {
            float diff = Math.abs(SPEEDS[i] - speed);
            if (diff < bestDiff) {
                bestDiff = diff;
                best = i;
            }
        }
        return best;
    }

    private interface OnSpeedClick {
        void onClick(float speed);
    }

    private static final class SpeedAdapter extends RecyclerView.Adapter<SpeedAdapter.Holder> {
        private final List<Float> speeds;
        private final int selectedIndex;
        private final OnSpeedClick listener;

        SpeedAdapter(List<Float> speeds, int selectedIndex, OnSpeedClick listener) {
            this.speeds = speeds;
            this.selectedIndex = selectedIndex;
            this.listener = listener;
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_video_quality_option, parent, false);
            return new Holder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            float speed = speeds.get(position);
            String label = formatSpeed(speed);
            holder.label.setText(label);
            holder.check.setVisibility(position == selectedIndex ? View.VISIBLE : View.GONE);
            holder.itemView.setOnClickListener(v -> listener.onClick(speed));
        }

        @Override
        public int getItemCount() {
            return speeds.size();
        }

        private static String formatSpeed(float speed) {
            if (Math.abs(speed - 1.0f) < 0.001f) {
                return "Normal (1x)";
            }
            if (speed == (long) speed) {
                return String.format(Locale.US, "%.0fx", speed);
            }
            return String.format(Locale.US, "%.2fx", speed).replaceAll("0+$", "").replaceAll("\\.$", "");
        }

        static final class Holder extends RecyclerView.ViewHolder {
            final TextView label;
            final ImageView check;

            Holder(@NonNull View itemView) {
                super(itemView);
                label = itemView.findViewById(R.id.txtQualityLabel);
                check = itemView.findViewById(R.id.imgQualitySelected);
            }
        }
    }
}

package com.chill.with.examsaathi;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.TrackSelectionParameters;
import androidx.media3.common.Tracks;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

@OptIn(markerClass = UnstableApi.class)
public final class VideoQualityDialog {

    public interface Listener {
        void onDismiss();
    }

    private VideoQualityDialog() {}

    private static final class QualityOption {
        final String label;
        @Nullable
        final TrackGroup trackGroup;
        final int trackIndex;
        final boolean auto;

        QualityOption(String label, @Nullable TrackGroup trackGroup, int trackIndex, boolean auto) {
            this.label = label;
            this.trackGroup = trackGroup;
            this.trackIndex = trackIndex;
            this.auto = auto;
        }
    }

    public static void show(AppCompatActivity activity, ExoPlayer player, @Nullable Listener listener) {
        List<QualityOption> options = buildQualityOptions(player.getCurrentTracks());
        if (options.size() <= 1) {
            Toast.makeText(activity, R.string.video_quality_unavailable, Toast.LENGTH_SHORT).show();
            if (listener != null) {
                listener.onDismiss();
            }
            return;
        }

        BottomSheetDialog bottomSheet = new BottomSheetDialog(activity, R.style.VideoQualityBottomSheetTheme);
        View sheetView = LayoutInflater.from(activity).inflate(R.layout.bottom_sheet_video_quality, null);
        RecyclerView recyclerView = sheetView.findViewById(R.id.recyclerQualityOptions);
        int selectedIndex = findSelectedIndex(options, player.getTrackSelectionParameters());

        bottomSheet.setContentView(sheetView);
        bottomSheet.setOnDismissListener(dialog -> {
            if (listener != null) {
                listener.onDismiss();
            }
        });

        QualityAdapter adapter = new QualityAdapter(options, selectedIndex, option -> {
            applySelection(player, option);
            bottomSheet.dismiss();
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        recyclerView.setAdapter(adapter);
        recyclerView.setNestedScrollingEnabled(options.size() > 6);

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

    private static List<QualityOption> buildQualityOptions(Tracks tracks) {
        List<QualityOption> options = new ArrayList<>();
        options.add(new QualityOption("Auto", null, -1, true));

        TreeMap<Integer, QualityOption> byHeight = new TreeMap<>(Collections.reverseOrder());
        for (Tracks.Group group : tracks.getGroups()) {
            if (group.getType() != C.TRACK_TYPE_VIDEO) {
                continue;
            }
            TrackGroup trackGroup = group.getMediaTrackGroup();
            for (int i = 0; i < trackGroup.length; i++) {
                Format format = trackGroup.getFormat(i);
                int height = format.height;
                if (height <= 0) {
                    continue;
                }
                QualityOption existing = byHeight.get(height);
                if (existing == null || format.bitrate > trackGroup.getFormat(existing.trackIndex).bitrate) {
                    byHeight.put(height, new QualityOption(height + "p", trackGroup, i, false));
                }
            }
        }
        options.addAll(byHeight.values());
        return options;
    }

    private static int findSelectedIndex(List<QualityOption> options, TrackSelectionParameters parameters) {
        for (TrackSelectionOverride override : parameters.overrides.values()) {
            for (int i = 0; i < options.size(); i++) {
                QualityOption option = options.get(i);
                if (option.auto || option.trackGroup == null) {
                    continue;
                }
                if (override.mediaTrackGroup.equals(option.trackGroup)
                        && override.trackIndices.contains(option.trackIndex)) {
                    return i;
                }
            }
        }
        return 0;
    }

    private static void applySelection(ExoPlayer player, QualityOption option) {
        TrackSelectionParameters.Builder builder = player.getTrackSelectionParameters().buildUpon();
        builder.clearOverridesOfType(C.TRACK_TYPE_VIDEO);
        if (!option.auto && option.trackGroup != null) {
            builder.addOverride(new TrackSelectionOverride(
                    option.trackGroup,
                    ImmutableList.of(option.trackIndex)));
        }
        player.setTrackSelectionParameters(builder.build());
    }

    private static final class QualityAdapter extends RecyclerView.Adapter<QualityAdapter.QualityHolder> {

        interface OnQualitySelected {
            void onSelected(QualityOption option);
        }

        private final List<QualityOption> options;
        private int selectedIndex;
        private final OnQualitySelected onQualitySelected;

        QualityAdapter(List<QualityOption> options, int selectedIndex, OnQualitySelected onQualitySelected) {
            this.options = options;
            this.selectedIndex = selectedIndex;
            this.onQualitySelected = onQualitySelected;
        }

        @NonNull
        @Override
        public QualityHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_video_quality_option, parent, false);
            return new QualityHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull QualityHolder holder, int position) {
            QualityOption option = options.get(position);
            holder.txtQualityLabel.setText(option.label);
            boolean selected = position == selectedIndex;
            holder.imgQualitySelected.setVisibility(selected ? View.VISIBLE : View.GONE);
            holder.itemView.setSelected(selected);
            holder.txtQualityLabel.setTextColor(holder.itemView.getContext().getColor(
                    selected ? R.color.white : R.color.text_color));
            holder.itemView.setOnClickListener(v -> {
                int adapterPosition = holder.getBindingAdapterPosition();
                if (adapterPosition == RecyclerView.NO_POSITION) {
                    return;
                }
                int previous = selectedIndex;
                selectedIndex = adapterPosition;
                if (previous != RecyclerView.NO_POSITION) {
                    notifyItemChanged(previous);
                }
                notifyItemChanged(selectedIndex);
                onQualitySelected.onSelected(option);
            });
        }

        @Override
        public int getItemCount() {
            return options.size();
        }

        static final class QualityHolder extends RecyclerView.ViewHolder {
            final TextView txtQualityLabel;
            final ImageView imgQualitySelected;

            QualityHolder(@NonNull View itemView) {
                super(itemView);
                txtQualityLabel = itemView.findViewById(R.id.txtQualityLabel);
                imgQualitySelected = itemView.findViewById(R.id.imgQualitySelected);
            }
        }
    }
}

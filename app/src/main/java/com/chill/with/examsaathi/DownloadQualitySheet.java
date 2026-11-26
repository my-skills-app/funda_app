package com.chill.with.examsaathi;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.Arrays;
import java.util.List;

/** Bottom sheet to pick download quality before enqueueing a video download. */
public final class DownloadQualitySheet {

    public static final class Option {
        public final String label;
        public final String tag;

        public Option(@NonNull String label, @NonNull String tag) {
            this.label = label;
            this.tag = tag;
        }
    }

    public interface Listener {
        void onQualitySelected(@NonNull Option option);
    }

    private DownloadQualitySheet() {}

    public static void show(@NonNull AppCompatActivity activity, @NonNull Listener listener) {
        List<Option> options = Arrays.asList(
                new Option("720p", "720p"),
                new Option("480p", "480p"),
                new Option("360p", "360p"));

        BottomSheetDialog bottomSheet = new BottomSheetDialog(activity, R.style.VideoQualityBottomSheetTheme);
        View sheetView = LayoutInflater.from(activity).inflate(R.layout.bottom_sheet_video_quality, null);
        TextView title = sheetView.findViewById(R.id.txtQualitySheetTitle);
        if (title != null) {
            title.setText(R.string.prep_download_quality_title);
        }
        TextView subtitle = sheetView.findViewById(R.id.txtQualitySheetSubtitle);
        if (subtitle != null) {
            subtitle.setText(R.string.prep_download_quality_subtitle);
        }

        RecyclerView recyclerView = sheetView.findViewById(R.id.recyclerQualityOptions);
        bottomSheet.setContentView(sheetView);

        Adapter adapter = new Adapter(options, option -> {
            listener.onQualitySelected(option);
            bottomSheet.dismiss();
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        recyclerView.setAdapter(adapter);

        bottomSheet.setOnShowListener(dialog -> {
            FrameLayout container = bottomSheet.findViewById(
                    com.google.android.material.R.id.design_bottom_sheet);
            if (container != null) {
                BottomSheetBehavior.from(container).setSkipCollapsed(true);
                BottomSheetBehavior.from(container).setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });
        bottomSheet.show();
    }

    /** Actions when a video is already downloaded. */
    public static void showDownloadedActions(
            @NonNull AppCompatActivity activity,
            @NonNull Runnable onShare,
            @NonNull Runnable onRedownload) {
        List<Option> options = Arrays.asList(
                new Option(activity.getString(R.string.prep_share_video), "share"),
                new Option(activity.getString(R.string.prep_download_again), "again"));

        BottomSheetDialog bottomSheet = new BottomSheetDialog(activity, R.style.VideoQualityBottomSheetTheme);
        View sheetView = LayoutInflater.from(activity).inflate(R.layout.bottom_sheet_video_quality, null);
        TextView title = sheetView.findViewById(R.id.txtQualitySheetTitle);
        if (title != null) {
            title.setText(R.string.prep_download_saved_title);
        }
        TextView subtitle = sheetView.findViewById(R.id.txtQualitySheetSubtitle);
        if (subtitle != null) {
            subtitle.setText(R.string.prep_download_saved_subtitle);
        }
        RecyclerView recyclerView = sheetView.findViewById(R.id.recyclerQualityOptions);
        bottomSheet.setContentView(sheetView);
        Adapter adapter = new Adapter(options, option -> {
            bottomSheet.dismiss();
            if ("share".equals(option.tag)) {
                onShare.run();
            } else {
                onRedownload.run();
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        recyclerView.setAdapter(adapter);
        bottomSheet.setOnShowListener(dialog -> {
            FrameLayout container = bottomSheet.findViewById(
                    com.google.android.material.R.id.design_bottom_sheet);
            if (container != null) {
                BottomSheetBehavior.from(container).setSkipCollapsed(true);
                BottomSheetBehavior.from(container).setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });
        bottomSheet.show();
    }

    private static final class Adapter extends RecyclerView.Adapter<Adapter.Holder> {
        interface OnSelected {
            void onSelected(Option option);
        }

        private final List<Option> options;
        private final OnSelected onSelected;

        Adapter(List<Option> options, OnSelected onSelected) {
            this.options = options;
            this.onSelected = onSelected;
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_video_quality_option, parent, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            Option option = options.get(position);
            holder.txtQualityLabel.setText(option.label);
            holder.imgQualitySelected.setVisibility(View.GONE);
            holder.txtQualityLabel.setTextColor(holder.itemView.getContext().getColor(R.color.text_color));
            holder.itemView.setOnClickListener(v -> onSelected.onSelected(option));
        }

        @Override
        public int getItemCount() {
            return options.size();
        }

        static final class Holder extends RecyclerView.ViewHolder {
            final TextView txtQualityLabel;
            final ImageView imgQualitySelected;

            Holder(@NonNull View itemView) {
                super(itemView);
                txtQualityLabel = itemView.findViewById(R.id.txtQualityLabel);
                imgQualitySelected = itemView.findViewById(R.id.imgQualitySelected);
            }
        }
    }
}

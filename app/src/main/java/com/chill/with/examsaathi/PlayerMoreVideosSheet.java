package com.chill.with.examsaathi;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.List;

/** Bottom sheet listing chapter shlokas in the player queue. */
public final class PlayerMoreVideosSheet {

    public interface Listener {
        void onShlokaSelected(int index);
    }

    public interface ItemSelectListener {
        void onSelect(int index);
    }

    private final BottomSheetDialog dialog;
    private final QueueAdapter adapter;
    private int currentIndex;

    private PlayerMoreVideosSheet(BottomSheetDialog dialog, QueueAdapter adapter, int currentIndex) {
        this.dialog = dialog;
        this.adapter = adapter;
        this.currentIndex = currentIndex;
    }

    @Nullable
    public static PlayerMoreVideosSheet show(
            AppCompatActivity activity,
            List<GitaChapterShlokas.Shloka> playlist,
            String chapterTitle,
            int currentIndex,
            Listener listener) {
        if (playlist == null || playlist.isEmpty()) {
            return null;
        }

        BottomSheetDialog bottomSheet = new BottomSheetDialog(activity, R.style.VideoQualityBottomSheetTheme);
        View sheetView = LayoutInflater.from(activity).inflate(R.layout.bottom_sheet_player_more_videos, null);
        TextView txtTitle = sheetView.findViewById(R.id.txtMoreVideosTitle);
        TextView txtSubtitle = sheetView.findViewById(R.id.txtMoreVideosSubtitle);
        RecyclerView recyclerView = sheetView.findViewById(R.id.recyclerMoreVideos);

        if (chapterTitle != null && !chapterTitle.isEmpty()) {
            txtSubtitle.setText(chapterTitle);
            txtSubtitle.setVisibility(View.VISIBLE);
        } else {
            txtSubtitle.setVisibility(View.GONE);
        }
        txtTitle.setText(R.string.player_more_videos);

        QueueAdapter adapter = new QueueAdapter(new ArrayList<>(playlist), currentIndex, index -> {
            if (listener != null) {
                listener.onShlokaSelected(index);
            }
            bottomSheet.dismiss();
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        recyclerView.setAdapter(adapter);

        bottomSheet.setContentView(sheetView);
        bottomSheet.show();

        PlayerMoreVideosSheet sheet = new PlayerMoreVideosSheet(bottomSheet, adapter, currentIndex);
        bottomSheet.setOnDismissListener(d -> adapter.dismissed = true);
        return sheet;
    }

    public void updateCurrentIndex(int index) {
        if (index < 0) {
            return;
        }
        currentIndex = index;
        adapter.setCurrentIndex(index);
    }

    public void dismiss() {
        dialog.dismiss();
    }

    private static final class QueueAdapter extends RecyclerView.Adapter<QueueAdapter.Holder> {

        private final List<GitaChapterShlokas.Shloka> items;
        private int currentIndex;
        private final ItemSelectListener onSelect;
        private boolean dismissed;

        QueueAdapter(List<GitaChapterShlokas.Shloka> items, int currentIndex,
                     ItemSelectListener onSelect) {
            this.items = items;
            this.currentIndex = currentIndex;
            this.onSelect = onSelect;
        }

        void setCurrentIndex(int index) {
            int old = currentIndex;
            currentIndex = index;
            if (old >= 0 && old < items.size()) {
                notifyItemChanged(old);
            }
            if (currentIndex >= 0 && currentIndex < items.size()) {
                notifyItemChanged(currentIndex);
            }
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_player_queue_shloka, parent, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            GitaChapterShlokas.Shloka shloka = items.get(position);
            boolean playing = position == currentIndex;
            holder.txtTitle.setText(shloka.title != null ? shloka.title : "");
            holder.txtDescription.setText(GitaChapterShlokas.shortDescription(shloka));
            holder.imgPlaying.setVisibility(playing ? View.VISIBLE : View.GONE);
            holder.itemView.setAlpha(playing ? 1f : 0.88f);

            if (shloka.image != null && !shloka.image.isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(shloka.image)
                        .centerCrop()
                        .into(holder.imgThumb);
            }

            holder.itemView.setOnClickListener(v -> {
                int pos = holder.getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && onSelect != null) {
                    onSelect.onSelect(pos);
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class Holder extends RecyclerView.ViewHolder {
            final ImageView imgThumb;
            final TextView txtTitle;
            final TextView txtDescription;
            final ImageView imgPlaying;

            Holder(View itemView) {
                super(itemView);
                imgThumb = itemView.findViewById(R.id.imgQueueThumb);
                txtTitle = itemView.findViewById(R.id.txtQueueTitle);
                txtDescription = itemView.findViewById(R.id.txtQueueDescription);
                imgPlaying = itemView.findViewById(R.id.imgQueuePlaying);
            }
        }
    }
}

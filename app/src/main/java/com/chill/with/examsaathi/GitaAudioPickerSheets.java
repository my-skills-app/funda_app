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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.List;

/** Bottom sheets to pick chapter or shloka for home Gita audio player. */
public final class GitaAudioPickerSheets {

    public interface ChapterSelectListener {
        void onChapterSelected(GitaChapters.Chapter chapter);
    }

    public interface ShlokaSelectListener {
        /** 0-based index within the current chapter list. */
        void onShlokaSelected(int indexInChapter);
    }

    private GitaAudioPickerSheets() {
    }

    public static void showChapterPicker(
            AppCompatActivity activity,
            List<GitaChapters.Chapter> chapters,
            int selectedApiChapterId,
            @Nullable String currentChapterTitle,
            ChapterSelectListener listener) {
        if (chapters == null || chapters.isEmpty()) {
            return;
        }
        BottomSheetDialog dialog = createExpandedSheet(activity);
        View sheetView = LayoutInflater.from(activity).inflate(R.layout.bottom_sheet_gita_audio_picker, null);
        TextView txtTitle = sheetView.findViewById(R.id.txtPickerTitle);
        TextView txtSubtitle = sheetView.findViewById(R.id.txtPickerSubtitle);
        RecyclerView recycler = sheetView.findViewById(R.id.recyclerPicker);

        txtTitle.setText(R.string.gita_audio_pick_chapter_title);
        if (currentChapterTitle != null && !currentChapterTitle.trim().isEmpty()) {
            txtSubtitle.setText(currentChapterTitle);
            txtSubtitle.setVisibility(View.VISIBLE);
        } else {
            txtSubtitle.setText(R.string.gita_audio_pick_chapter_subtitle);
        }

        ChapterPickerAdapter adapter = new ChapterPickerAdapter(
                chapters, selectedApiChapterId, chapter -> {
                    if (listener != null) {
                        listener.onChapterSelected(chapter);
                    }
                    dialog.dismiss();
                });
        recycler.setLayoutManager(new LinearLayoutManager(activity));
        recycler.setAdapter(adapter);
        dialog.setContentView(sheetView);
        dialog.show();
        scrollToSelectedChapter(recycler, chapters, selectedApiChapterId);
    }

    public static void showShlokaPicker(
            AppCompatActivity activity,
            List<GitaChapterShlokas.Shloka> shlokas,
            @Nullable String chapterTitle,
            int currentIndexInChapter,
            ShlokaSelectListener listener) {
        if (shlokas == null || shlokas.isEmpty()) {
            return;
        }
        BottomSheetDialog dialog = createExpandedSheet(activity);
        View sheetView = LayoutInflater.from(activity).inflate(R.layout.bottom_sheet_gita_audio_picker, null);
        TextView txtTitle = sheetView.findViewById(R.id.txtPickerTitle);
        TextView txtSubtitle = sheetView.findViewById(R.id.txtPickerSubtitle);
        RecyclerView recycler = sheetView.findViewById(R.id.recyclerPicker);

        txtTitle.setText(R.string.gita_audio_pick_shloka_title);
        if (chapterTitle != null && !chapterTitle.isEmpty()) {
            txtSubtitle.setText(chapterTitle);
            txtSubtitle.setVisibility(View.VISIBLE);
        } else {
            txtSubtitle.setText(R.string.gita_audio_pick_shloka_subtitle);
        }

        ShlokaPickerAdapter adapter = new ShlokaPickerAdapter(
                shlokas, currentIndexInChapter, index -> {
                    if (listener != null) {
                        listener.onShlokaSelected(index);
                    }
                    dialog.dismiss();
                });
        recycler.setLayoutManager(new LinearLayoutManager(activity));
        recycler.setAdapter(adapter);
        dialog.setContentView(sheetView);
        dialog.show();
        if (currentIndexInChapter > 0) {
            recycler.post(() -> {
                LinearLayoutManager lm = (LinearLayoutManager) recycler.getLayoutManager();
                if (lm != null) {
                    lm.scrollToPositionWithOffset(currentIndexInChapter, 120);
                }
            });
        }
    }

    private static BottomSheetDialog createExpandedSheet(AppCompatActivity activity) {
        BottomSheetDialog dialog = new BottomSheetDialog(activity, R.style.VideoQualityBottomSheetTheme);
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
        return dialog;
    }

    private static void scrollToSelectedChapter(
            RecyclerView recycler,
            List<GitaChapters.Chapter> chapters,
            int selectedApiChapterId) {
        int index = 0;
        for (int i = 0; i < chapters.size(); i++) {
            if (chapters.get(i).chapterId == selectedApiChapterId) {
                index = i;
                break;
            }
        }
        int scrollTo = index;
        recycler.post(() -> {
            LinearLayoutManager lm = (LinearLayoutManager) recycler.getLayoutManager();
            if (lm != null) {
                lm.scrollToPositionWithOffset(scrollTo, 120);
            }
        });
    }

    private static final class ChapterPickerAdapter
            extends RecyclerView.Adapter<ChapterPickerAdapter.Holder> {

        private final List<GitaChapters.Chapter> items;
        private final int selectedApiChapterId;
        private final ChapterSelectListener onSelect;

        ChapterPickerAdapter(List<GitaChapters.Chapter> items, int selectedApiChapterId,
                ChapterSelectListener onSelect) {
            this.items = items;
            this.selectedApiChapterId = selectedApiChapterId;
            this.onSelect = onSelect;
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_gita_chapter, parent, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            GitaChapters.Chapter chapter = items.get(position);
            int number = GitaChapters.chapterNumber(chapter);
            boolean selected = chapter.chapterId == selectedApiChapterId;
            holder.txtChapterNumber.setText(String.valueOf(number > 0 ? number : position + 1));
            String yoga = GitaChapters.yogaTitle(chapter);
            holder.txtChapterTitle.setText(yoga.isEmpty()
                    ? (chapter.name != null ? chapter.name : "")
                    : yoga);
            String subtitle = GitaChapters.yogaSubtitle(chapter);
            if (subtitle.isEmpty()) {
                holder.txtChapterSubtitle.setVisibility(View.GONE);
            } else {
                holder.txtChapterSubtitle.setVisibility(View.VISIBLE);
                holder.txtChapterSubtitle.setText(subtitle);
            }
            holder.itemView.setAlpha(selected ? 1f : 0.92f);
            int accent = holder.itemView.getContext().getColor(R.color.app_color);
            holder.txtChapterNumber.setTextColor(accent);
            holder.itemView.setOnClickListener(v -> {
                if (onSelect != null) {
                    onSelect.onChapterSelected(chapter);
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class Holder extends RecyclerView.ViewHolder {
            final TextView txtChapterNumber;
            final TextView txtChapterTitle;
            final TextView txtChapterSubtitle;

            Holder(View itemView) {
                super(itemView);
                txtChapterNumber = itemView.findViewById(R.id.txtChapterNumber);
                txtChapterTitle = itemView.findViewById(R.id.txtChapterTitle);
                txtChapterSubtitle = itemView.findViewById(R.id.txtChapterSubtitle);
            }
        }
    }

    private static final class ShlokaPickerAdapter
            extends RecyclerView.Adapter<ShlokaPickerAdapter.Holder> {

        private final List<GitaChapterShlokas.Shloka> items;
        private int currentIndex;
        private final ShlokaSelectListener onSelect;

        ShlokaPickerAdapter(List<GitaChapterShlokas.Shloka> items, int currentIndex,
                ShlokaSelectListener onSelect) {
            this.items = new ArrayList<>(items);
            this.currentIndex = currentIndex;
            this.onSelect = onSelect;
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
            holder.itemView.setAlpha(playing ? 1f : 0.9f);
            if (shloka.image != null && !shloka.image.isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(shloka.image)
                        .centerCrop()
                        .into(holder.imgThumb);
            } else {
                holder.imgThumb.setImageResource(R.color.view_bg_color);
            }
            holder.itemView.setOnClickListener(v -> {
                int pos = holder.getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && onSelect != null) {
                    onSelect.onShlokaSelected(pos);
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

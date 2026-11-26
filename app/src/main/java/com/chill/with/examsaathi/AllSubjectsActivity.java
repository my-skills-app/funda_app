package com.chill.with.examsaathi;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

/** Category-wise All Subjects screen (Prepshots users/subjects). */
public class AllSubjectsActivity extends AppCompatActivity {

    public static void open(@NonNull Context context) {
        ActivityNav.start(context, new Intent(context, AllSubjectsActivity.class));
    }

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private SwipeRefreshLayout swipeRefreshSubjects;
    private TextView txtExamChip;
    private ImageView imgExamThumb;
    private RecyclerView recyclerSubjectSections;
    private ProgressBar progressSubjects;
    private TextView txtSubjectsError;
    private SectionsAdapter sectionsAdapter;
    private boolean loading;
    private String examName = "";
    private String examThumb = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_subjects);
        setupStatusBar();
        bindViews();
        findViewById(R.id.btnBack).setOnClickListener(v -> ActivityNav.finish(this));
        swipeRefreshSubjects.setColorSchemeColors(getColor(R.color.app_color));
        swipeRefreshSubjects.setOnRefreshListener(this::loadSubjects);
        sectionsAdapter = new SectionsAdapter();
        recyclerSubjectSections.setLayoutManager(new LinearLayoutManager(this));
        recyclerSubjectSections.setAdapter(sectionsAdapter);
        loadSubjects();
    }

    private void bindViews() {
        swipeRefreshSubjects = findViewById(R.id.swipeRefreshSubjects);
        txtExamChip = findViewById(R.id.txtExamChip);
        imgExamThumb = findViewById(R.id.imgExamThumb);
        recyclerSubjectSections = findViewById(R.id.recyclerSubjectSections);
        progressSubjects = findViewById(R.id.progressSubjects);
        txtSubjectsError = findViewById(R.id.txtSubjectsError);
        ViewCompat.setOnApplyWindowInsetsListener(swipeRefreshSubjects, (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(v.getPaddingLeft(), top, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }
        Window window = getWindow();
        window.setStatusBarColor(Color.TRANSPARENT);
        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    private void loadSubjects() {
        if (loading) {
            return;
        }
        loading = true;
        if (!swipeRefreshSubjects.isRefreshing()) {
            progressSubjects.setVisibility(View.VISIBLE);
        }
        txtSubjectsError.setVisibility(View.GONE);
        new Thread(() -> {
            try {
                PrepSubjectsRepository.Result result = PrepSubjectsRepository.loadAllSubjects();
                mainHandler.post(() -> {
                    loading = false;
                    swipeRefreshSubjects.setRefreshing(false);
                    progressSubjects.setVisibility(View.GONE);
                    bindResult(result);
                });
            } catch (Exception e) {
                android.util.Log.e("AllSubjects", "load failed", e);
                mainHandler.post(() -> {
                    loading = false;
                    swipeRefreshSubjects.setRefreshing(false);
                    progressSubjects.setVisibility(View.GONE);
                    recyclerSubjectSections.setVisibility(View.GONE);
                    txtSubjectsError.setVisibility(View.VISIBLE);
                    if (e.getMessage() != null && !e.getMessage().isEmpty()) {
                        txtSubjectsError.setText(e.getMessage());
                    }
                });
            }
        }).start();
    }

    private void bindResult(@NonNull PrepSubjectsRepository.Result result) {
        examName = result.examName != null ? result.examName.trim() : "";
        examThumb = result.examThumbnailUrl != null ? result.examThumbnailUrl : "";
        if (!examName.isEmpty()) {
            txtExamChip.setText(getString(R.string.prep_full_course_chip, examName));
        } else {
            txtExamChip.setText(R.string.prep_all_subjects_title);
        }
        if (!examThumb.isEmpty()) {
            Glide.with(this).load(examThumb).circleCrop().into(imgExamThumb);
        } else {
            imgExamThumb.setImageResource(R.drawable.default_image);
        }
        if (result.sections.isEmpty()) {
            recyclerSubjectSections.setVisibility(View.GONE);
            txtSubjectsError.setVisibility(View.VISIBLE);
            return;
        }
        txtSubjectsError.setVisibility(View.GONE);
        recyclerSubjectSections.setVisibility(View.VISIBLE);
        sectionsAdapter.setSections(result.sections);
    }

    private class SectionsAdapter extends RecyclerView.Adapter<SectionsAdapter.SectionHolder> {
        private final List<PrepSubjectsRepository.Section> sections = new ArrayList<>();

        void setSections(List<PrepSubjectsRepository.Section> next) {
            sections.clear();
            if (next != null) {
                sections.addAll(next);
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public SectionHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_all_subjects_section, parent, false);
            return new SectionHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull SectionHolder holder, int position) {
            PrepSubjectsRepository.Section section = sections.get(position);
            holder.txtSectionName.setText(section.name != null ? section.name : "");
            holder.recyclerSectionSubjects.setLayoutManager(
                    new GridLayoutManager(holder.itemView.getContext(), 3));
            holder.recyclerSectionSubjects.setAdapter(new SubjectTilesAdapter(section.subjects));
        }

        @Override
        public int getItemCount() {
            return sections.size();
        }

        class SectionHolder extends RecyclerView.ViewHolder {
            final TextView txtSectionName;
            final RecyclerView recyclerSectionSubjects;

            SectionHolder(View itemView) {
                super(itemView);
                txtSectionName = itemView.findViewById(R.id.txtSectionName);
                recyclerSectionSubjects = itemView.findViewById(R.id.recyclerSectionSubjects);
            }
        }
    }

    private class SubjectTilesAdapter
            extends RecyclerView.Adapter<SubjectTilesAdapter.TileHolder> {
        private final List<PrepSubjectsRepository.Subject> items;

        SubjectTilesAdapter(List<PrepSubjectsRepository.Subject> items) {
            this.items = items != null ? items : new ArrayList<>();
        }

        @NonNull
        @Override
        public TileHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_prep_subject_tile, parent, false);
            return new TileHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull TileHolder holder, int position) {
            PrepSubjectsRepository.Subject item = items.get(position);
            holder.layoutSubjectTile.setCardBackgroundColor(0xFF1C1C24);
            holder.imgMoreIcon.setVisibility(View.GONE);
            holder.imgSubject.setVisibility(View.VISIBLE);
            holder.txtSubjectName.setText(item.name != null ? item.name : "");
            if (item.badge != null && !item.badge.isEmpty()) {
                holder.txtSubjectBadge.setVisibility(View.VISIBLE);
                holder.txtSubjectBadge.setText(item.badge);
            } else {
                holder.txtSubjectBadge.setVisibility(View.GONE);
            }
            if (item.thumbnailUrl != null && !item.thumbnailUrl.isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(item.thumbnailUrl)
                        .centerCrop()
                        .into(holder.imgSubject);
            } else {
                holder.imgSubject.setImageResource(R.drawable.default_image);
            }
            holder.itemView.setOnClickListener(v ->
                    SubjectSyllabusActivity.open(
                            AllSubjectsActivity.this,
                            item.id,
                            item.name,
                            examName,
                            examThumb,
                            item.thumbnailUrl));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class TileHolder extends RecyclerView.ViewHolder {
            final CardView layoutSubjectTile;
            final ImageView imgSubject;
            final ImageView imgMoreIcon;
            final TextView txtSubjectBadge;
            final TextView txtSubjectName;

            TileHolder(View itemView) {
                super(itemView);
                layoutSubjectTile = itemView.findViewById(R.id.layoutSubjectTile);
                imgSubject = itemView.findViewById(R.id.imgSubject);
                imgMoreIcon = itemView.findViewById(R.id.imgMoreIcon);
                txtSubjectBadge = itemView.findViewById(R.id.txtSubjectBadge);
                txtSubjectName = itemView.findViewById(R.id.txtSubjectName);
            }
        }
    }
}

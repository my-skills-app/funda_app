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
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

/** Subject syllabus screen — banner hero + grid of series per category. */
public class SubjectSyllabusActivity extends AppCompatActivity {

    public static final String EXTRA_SUBJECT_ID = "subject_id";
    public static final String EXTRA_SUBJECT_NAME = "subject_name";
    public static final String EXTRA_EXAM_NAME = "exam_name";
    public static final String EXTRA_EXAM_THUMB = "exam_thumb";
    public static final String EXTRA_SUBJECT_THUMB = "subject_thumb";

    public static void open(
            @NonNull Context context,
            long subjectId,
            @Nullable String subjectName,
            @Nullable String examName,
            @Nullable String examThumb,
            @Nullable String subjectThumb) {
        Intent intent = new Intent(context, SubjectSyllabusActivity.class);
        intent.putExtra(EXTRA_SUBJECT_ID, subjectId);
        intent.putExtra(EXTRA_SUBJECT_NAME, subjectName != null ? subjectName : "");
        intent.putExtra(EXTRA_EXAM_NAME, examName != null ? examName : "");
        intent.putExtra(EXTRA_EXAM_THUMB, examThumb != null ? examThumb : "");
        intent.putExtra(EXTRA_SUBJECT_THUMB, subjectThumb != null ? subjectThumb : "");
        ActivityNav.start(context, intent);
    }

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private SwipeRefreshLayout swipeRefreshSyllabus;
    private ImageView imgSubjectBanner;
    private ImageView imgExamThumb;
    private TextView txtExamChip;
    private TextView txtSubjectTitle;
    private View layoutSyllabusTopBar;
    private RecyclerView recyclerSyllabusGroups;
    private ProgressBar progressSyllabus;
    private TextView txtSyllabusError;
    private GroupsAdapter groupsAdapter;

    private long subjectId;
    private String subjectName = "";
    private String examName = "";
    private String examThumb = "";
    private String subjectThumb = "";
    private boolean loading;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subject_syllabus);
        setupStatusBar();
        readExtras();
        bindViews();
        findViewById(R.id.btnBack).setOnClickListener(v -> ActivityNav.finish(this));
        swipeRefreshSyllabus.setColorSchemeColors(getColor(R.color.app_color));
        swipeRefreshSyllabus.setOnRefreshListener(this::loadSyllabus);
        groupsAdapter = new GroupsAdapter();
        recyclerSyllabusGroups.setLayoutManager(new LinearLayoutManager(this));
        recyclerSyllabusGroups.setAdapter(groupsAdapter);
        bindHeaderPlaceholders();
        loadSyllabus();
    }

    private void readExtras() {
        Intent intent = getIntent();
        subjectId = intent != null ? intent.getLongExtra(EXTRA_SUBJECT_ID, 0L) : 0L;
        subjectName = intent != null ? intent.getStringExtra(EXTRA_SUBJECT_NAME) : "";
        examName = intent != null ? intent.getStringExtra(EXTRA_EXAM_NAME) : "";
        examThumb = intent != null ? intent.getStringExtra(EXTRA_EXAM_THUMB) : "";
        subjectThumb = intent != null ? intent.getStringExtra(EXTRA_SUBJECT_THUMB) : "";
        if (subjectName == null) subjectName = "";
        if (examName == null) examName = "";
        if (examThumb == null) examThumb = "";
        if (subjectThumb == null) subjectThumb = "";
    }

    private void bindViews() {
        swipeRefreshSyllabus = findViewById(R.id.swipeRefreshSyllabus);
        imgSubjectBanner = findViewById(R.id.imgSubjectBanner);
        imgExamThumb = findViewById(R.id.imgExamThumb);
        txtExamChip = findViewById(R.id.txtExamChip);
        txtSubjectTitle = findViewById(R.id.txtSubjectTitle);
        layoutSyllabusTopBar = findViewById(R.id.layoutSyllabusTopBar);
        recyclerSyllabusGroups = findViewById(R.id.recyclerSyllabusGroups);
        progressSyllabus = findViewById(R.id.progressSyllabus);
        txtSyllabusError = findViewById(R.id.txtSyllabusError);
        ViewCompat.setOnApplyWindowInsetsListener(layoutSyllabusTopBar, (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(v.getPaddingLeft(), top + dp(4), v.getPaddingRight(), v.getPaddingBottom());
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

    private void bindHeaderPlaceholders() {
        if (!subjectName.isEmpty()) {
            txtSubjectTitle.setText(subjectName);
        }
        if (!examName.isEmpty()) {
            txtExamChip.setText(getString(R.string.prep_full_course_chip, examName));
        } else {
            txtExamChip.setText(R.string.prep_full_course_for);
        }
        if (!examThumb.isEmpty()) {
            Glide.with(this).load(examThumb).circleCrop().into(imgExamThumb);
        }
        if (!subjectThumb.isEmpty()) {
            Glide.with(this).load(subjectThumb).centerCrop().into(imgSubjectBanner);
        }
    }

    private void loadSyllabus() {
        if (loading) {
            return;
        }
        if (subjectId <= 0) {
            progressSyllabus.setVisibility(View.GONE);
            txtSyllabusError.setVisibility(View.VISIBLE);
            return;
        }
        loading = true;
        if (!swipeRefreshSyllabus.isRefreshing()) {
            progressSyllabus.setVisibility(View.VISIBLE);
        }
        txtSyllabusError.setVisibility(View.GONE);
        new Thread(() -> {
            try {
                PrepSyllabusRepository.Result result = PrepSyllabusRepository.loadSyllabus(subjectId);
                mainHandler.post(() -> {
                    loading = false;
                    swipeRefreshSyllabus.setRefreshing(false);
                    progressSyllabus.setVisibility(View.GONE);
                    bindResult(result);
                });
            } catch (Exception e) {
                android.util.Log.e("SubjectSyllabus", "load failed", e);
                mainHandler.post(() -> {
                    loading = false;
                    swipeRefreshSyllabus.setRefreshing(false);
                    progressSyllabus.setVisibility(View.GONE);
                    recyclerSyllabusGroups.setVisibility(View.GONE);
                    txtSyllabusError.setVisibility(View.VISIBLE);
                    if (e.getMessage() != null && !e.getMessage().isEmpty()) {
                        txtSyllabusError.setText(e.getMessage());
                    }
                });
            }
        }).start();
    }

    private void bindResult(@NonNull PrepSyllabusRepository.Result result) {
        if (result.subjectName != null && !result.subjectName.isEmpty()) {
            subjectName = result.subjectName;
            txtSubjectTitle.setText(subjectName);
        }
        String banner = result.bannerUrl != null && !result.bannerUrl.isEmpty()
                ? result.bannerUrl
                : result.thumbnailUrl;
        if (banner != null && !banner.isEmpty()) {
            Glide.with(this).load(banner).centerCrop().into(imgSubjectBanner);
        }
        if (result.groups.isEmpty()) {
            recyclerSyllabusGroups.setVisibility(View.GONE);
            txtSyllabusError.setVisibility(View.VISIBLE);
            return;
        }
        txtSyllabusError.setVisibility(View.GONE);
        recyclerSyllabusGroups.setVisibility(View.VISIBLE);
        groupsAdapter.setGroups(result.groups);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private class GroupsAdapter extends RecyclerView.Adapter<GroupsAdapter.GroupHolder> {
        private final List<PrepSyllabusRepository.SyllabusGroup> groups = new ArrayList<>();

        void setGroups(List<PrepSyllabusRepository.SyllabusGroup> next) {
            groups.clear();
            if (next != null) {
                groups.addAll(next);
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public GroupHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_syllabus_group, parent, false);
            return new GroupHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull GroupHolder holder, int position) {
            PrepSyllabusRepository.SyllabusGroup group = groups.get(position);
            holder.txtGroupTitle.setText(group.title != null ? group.title : "");
            holder.recyclerSeries.setLayoutManager(
                    new GridLayoutManager(holder.itemView.getContext(), 2));
            holder.recyclerSeries.setAdapter(new SeriesAdapter(group.series));
        }

        @Override
        public int getItemCount() {
            return groups.size();
        }

        class GroupHolder extends RecyclerView.ViewHolder {
            final TextView txtGroupTitle;
            final RecyclerView recyclerSeries;

            GroupHolder(View itemView) {
                super(itemView);
                txtGroupTitle = itemView.findViewById(R.id.txtGroupTitle);
                recyclerSeries = itemView.findViewById(R.id.recyclerSeries);
            }
        }
    }

    private class SeriesAdapter extends RecyclerView.Adapter<SeriesAdapter.SeriesHolder> {
        private final List<PrepSyllabusRepository.SeriesItem> items;

        SeriesAdapter(List<PrepSyllabusRepository.SeriesItem> items) {
            this.items = items != null ? items : new ArrayList<>();
        }

        @NonNull
        @Override
        public SeriesHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_syllabus_series_card, parent, false);
            return new SeriesHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull SeriesHolder holder, int position) {
            PrepSyllabusRepository.SeriesItem item = items.get(position);
            holder.txtSeriesTitle.setText(item.title != null ? item.title : "");
            if (item.badge != null && !item.badge.isEmpty()) {
                holder.txtSeriesBadge.setVisibility(View.VISIBLE);
                holder.txtSeriesBadge.setText(item.badge);
            } else {
                holder.txtSeriesBadge.setVisibility(View.GONE);
            }
            if (item.thumbnailUrl != null && !item.thumbnailUrl.isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(item.thumbnailUrl)
                        .centerCrop()
                        .into(holder.imgSeries);
            } else {
                holder.imgSeries.setImageResource(R.drawable.default_image);
            }
            holder.itemView.setOnClickListener(v -> SeriesDetailsActivity.open(
                    SubjectSyllabusActivity.this,
                    item.id,
                    item.title,
                    subjectName,
                    examName,
                    examThumb,
                    item.thumbnailUrl));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class SeriesHolder extends RecyclerView.ViewHolder {
            final ImageView imgSeries;
            final TextView txtSeriesBadge;
            final TextView txtSeriesTitle;

            SeriesHolder(View itemView) {
                super(itemView);
                imgSeries = itemView.findViewById(R.id.imgSeries);
                txtSeriesBadge = itemView.findViewById(R.id.txtSeriesBadge);
                txtSeriesTitle = itemView.findViewById(R.id.txtSeriesTitle);
            }
        }
    }
}

package com.chill.with.examsaathi;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * First-launch (and edit) flow: category → multi-select exams → enroll locally + API.
 */
public class ExamOnboardingActivity extends AppCompatActivity {

    public static final String EXTRA_EDIT_MODE = "edit_mode";

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Map<Long, PrepExamRepository.SelectedCourse> selected = new LinkedHashMap<>();
    private final List<PrepExamRepository.ExamItem> rows = new ArrayList<>();

    private TextView txtTitle;
    private TextView txtSubtitle;
    private TextView txtSelectedCount;
    private ImageButton btnBack;
    private ProgressBar progress;
    private RecyclerView recycler;
    private TextView btnEnroll;
    private Adapter adapter;

    private boolean showingExams;
    private boolean editMode;
    private long currentSuperCategoryId;
    private String currentSuperCategoryName = "";

    public static void open(@NonNull Context context, boolean editMode) {
        Intent i = new Intent(context, ExamOnboardingActivity.class);
        i.putExtra(EXTRA_EDIT_MODE, editMode);
        if (!(context instanceof android.app.Activity)) {
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(i);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exam_onboarding);
        editMode = getIntent().getBooleanExtra(EXTRA_EDIT_MODE, false);
        // First-launch screen only once; edit opens only from course switcher
        if (!editMode && PrepCourseStore.isOnboardingDone(this)) {
            Intent i = new Intent(this, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
            return;
        }

        txtTitle = findViewById(R.id.txtOnboardingTitle);
        txtSubtitle = findViewById(R.id.txtOnboardingSubtitle);
        txtSelectedCount = findViewById(R.id.txtSelectedCount);
        btnBack = findViewById(R.id.btnOnboardingBack);
        progress = findViewById(R.id.progressOnboarding);
        recycler = findViewById(R.id.recyclerOnboarding);
        btnEnroll = findViewById(R.id.btnEnrollContinue);

        for (PrepExamRepository.SelectedCourse c : PrepCourseStore.getEnrolled(this)) {
            selected.put(c.examId, c);
        }

        adapter = new Adapter();
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        btnBack.setOnClickListener(v -> loadCategories());
        btnEnroll.setOnClickListener(v -> enrollSelected());

        updateSelectedUi();
        loadCategories();
    }

    @Override
    public void onBackPressed() {
        if (showingExams) {
            loadCategories();
            return;
        }
        if (editMode || PrepCourseStore.hasEnrolledCourses(this)) {
            super.onBackPressed();
            return;
        }
        // First launch: require at least one course
        AppToast.show(this, R.string.prep_onboarding_pick_one);
    }

    private void loadCategories() {
        showingExams = false;
        currentSuperCategoryId = 0;
        currentSuperCategoryName = "";
        txtTitle.setText(R.string.prep_onboarding_title);
        txtSubtitle.setText(R.string.prep_onboarding_subtitle);
        btnBack.setVisibility(View.GONE);
        setLoading(true);
        new Thread(() -> {
            try {
                List<PrepExamRepository.ExamItem> items = PrepExamRepository.loadSuperCategories();
                mainHandler.post(() -> {
                    setLoading(false);
                    rows.clear();
                    rows.addAll(items);
                    adapter.notifyDataSetChanged();
                    updateSelectedUi();
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    setLoading(false);
                    AppToast.show(this, R.string.prep_course_load_error);
                });
            }
        }).start();
    }

    private void loadExams(long superCategoryId, @NonNull String name) {
        showingExams = true;
        currentSuperCategoryId = superCategoryId;
        currentSuperCategoryName = name;
        txtTitle.setText(name);
        txtSubtitle.setText(R.string.prep_onboarding_pick_exams);
        btnBack.setVisibility(View.VISIBLE);
        setLoading(true);
        new Thread(() -> {
            try {
                List<PrepExamRepository.ExamItem> items =
                        PrepExamRepository.loadExamsForSuperCategory(superCategoryId);
                mainHandler.post(() -> {
                    setLoading(false);
                    rows.clear();
                    rows.addAll(items);
                    adapter.notifyDataSetChanged();
                    updateSelectedUi();
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    setLoading(false);
                    AppToast.show(this, R.string.prep_course_load_error);
                });
            }
        }).start();
    }

    private void toggleExam(@NonNull PrepExamRepository.ExamItem exam) {
        if (selected.containsKey(exam.id)) {
            selected.remove(exam.id);
        } else {
            PrepExamRepository.SelectedCourse course = new PrepExamRepository.SelectedCourse();
            course.examId = exam.id;
            course.examName = exam.name;
            course.thumbnailUrl = exam.thumbnailUrl;
            course.superCategoryId =
                    exam.superCategoryId > 0 ? exam.superCategoryId : currentSuperCategoryId;
            course.superCategoryName = currentSuperCategoryName;
            selected.put(exam.id, course);
        }
        adapter.notifyDataSetChanged();
        updateSelectedUi();
    }

    private void updateSelectedUi() {
        int n = selected.size();
        if (n > 0) {
            txtSelectedCount.setVisibility(View.VISIBLE);
            txtSelectedCount.setText(getString(R.string.prep_onboarding_selected_count, n));
            btnEnroll.setVisibility(View.VISIBLE);
            btnEnroll.setText(
                    editMode
                            ? getString(R.string.prep_save_courses)
                            : getString(R.string.prep_enroll_continue));
        } else {
            txtSelectedCount.setVisibility(View.GONE);
            btnEnroll.setVisibility(showingExams ? View.INVISIBLE : View.GONE);
        }
    }

    private void enrollSelected() {
        if (selected.isEmpty()) {
            AppToast.show(this, R.string.prep_onboarding_pick_one);
            return;
        }
        setLoading(true);
        btnEnroll.setEnabled(false);
        btnEnroll.setAlpha(0.6f);
        final List<PrepExamRepository.SelectedCourse> courses = new ArrayList<>(selected.values());
        new Thread(() -> {
            Exception lastError = null;
            PrepExamRepository.SelectedCourse lastOkCourse = null;
            for (PrepExamRepository.SelectedCourse c : courses) {
                try {
                    PrepExamRepository.selectExam(c.superCategoryId, c.examId);
                    lastOkCourse = c;
                } catch (Exception e) {
                    lastError = e;
                }
            }
            final PrepExamRepository.SelectedCourse lastOk = lastOkCourse;
            // Keep local enrollments even if one API select fails
            long activeId = lastOk != null ? lastOk.examId : courses.get(courses.size() - 1).examId;
            PrepCourseStore.saveEnrolled(ExamOnboardingActivity.this, courses, activeId);
            PrepHomeCache.clear(ExamOnboardingActivity.this);

            final Exception err = lastError;
            final PrepExamRepository.SelectedCourse active = lastOk != null ? lastOk : courses.get(0);
            mainHandler.post(() -> {
                setLoading(false);
                btnEnroll.setEnabled(true);
                btnEnroll.setAlpha(1f);
                if (err != null && lastOk == null) {
                    AppToast.show(this, R.string.prep_course_select_error);
                    return;
                }
                AppToast.show(this, R.string.prep_courses_enrolled);
                Intent i = new Intent(this, MainActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                i.putExtra("active_exam_id", active.examId);
                startActivity(i);
                finish();
            });
        }).start();
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        recycler.setVisibility(loading ? View.INVISIBLE : View.VISIBLE);
    }

    private class Adapter extends RecyclerView.Adapter<Adapter.Holder> {

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_exam_onboarding_row, parent, false);
            return new Holder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            PrepExamRepository.ExamItem item = rows.get(position);
            holder.txtName.setText(item.name);
            holder.txtMeta.setText(item.meta != null ? item.meta : "");
            holder.txtMeta.setVisibility(
                    item.meta != null && !item.meta.isEmpty() ? View.VISIBLE : View.GONE);

            if (item.thumbnailUrl != null && !item.thumbnailUrl.isEmpty()) {
                Glide.with(holder.imgThumb.getContext())
                        .load(item.thumbnailUrl.trim())
                        .circleCrop()
                        .into(holder.imgThumb);
            } else {
                holder.imgThumb.setImageResource(R.drawable.default_image);
            }

            boolean multi = showingExams;
            holder.radioExam.setVisibility(multi ? View.VISIBLE : View.GONE);
            holder.imgChevron.setVisibility(multi ? View.GONE : View.VISIBLE);
            if (multi) {
                boolean checked = selected.containsKey(item.id);
                holder.radioExam.setVisibility(View.VISIBLE);
                holder.radioExam.setImageResource(
                        checked ? R.drawable.ic_radio_checked : R.drawable.ic_radio_unchecked);
                String path = currentSuperCategoryName.isEmpty()
                        ? item.name
                        : (currentSuperCategoryName + " → " + item.name);
                holder.txtMeta.setVisibility(View.VISIBLE);
                holder.txtMeta.setText(
                        checked
                                ? getString(R.string.prep_onboarding_selected_path, path)
                                : getString(R.string.prep_onboarding_tap_select));
                holder.txtMeta.setTextColor(
                        getColor(checked ? R.color.app_color : R.color.text_color_light));
            } else {
                holder.txtMeta.setTextColor(getColor(R.color.text_color_light));
            }

            holder.itemView.setOnClickListener(v -> {
                if (!showingExams && item.isCategory) {
                    loadExams(item.id, item.name);
                    return;
                }
                if (!showingExams) {
                    toggleExam(item);
                    return;
                }
                toggleExam(item);
            });
        }

        @Override
        public int getItemCount() {
            return rows.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            final ImageView imgThumb;
            final TextView txtName;
            final TextView txtMeta;
            final ImageView radioExam;
            final ImageView imgChevron;

            Holder(@NonNull View itemView) {
                super(itemView);
                imgThumb = itemView.findViewById(R.id.imgExamThumb);
                txtName = itemView.findViewById(R.id.txtExamName);
                txtMeta = itemView.findViewById(R.id.txtExamMeta);
                radioExam = itemView.findViewById(R.id.radioExam);
                imgChevron = itemView.findViewById(R.id.imgChevron);
            }
        }
    }
}

package com.chill.with.examsaathi;

import android.app.Dialog;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

/** Pick super-category → exam, then POST /api/exams/select. */
public class ExamPickerBottomSheet extends BottomSheetDialogFragment {

    public interface Listener {
        void onExamSelected(@NonNull PrepExamRepository.SelectedCourse course);
    }

    private static final String TAG = "ExamPickerBottomSheet";

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Listener listener;
    private TextView txtTitle;
    private ImageButton btnBack;
    private ProgressBar progress;
    private RecyclerView recycler;
    private final List<PrepExamRepository.ExamItem> rows = new ArrayList<>();
    private Adapter adapter;
    private boolean showingExams;
    private long currentSuperCategoryId;
    private String currentSuperCategoryName = "";

    public static void show(@NonNull androidx.fragment.app.FragmentManager fm, @Nullable Listener listener) {
        ExamPickerBottomSheet sheet = new ExamPickerBottomSheet();
        sheet.listener = listener;
        sheet.show(fm, TAG);
    }

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new BottomSheetDialog(requireContext(), getTheme());
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_exam_picker, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        txtTitle = view.findViewById(R.id.txtExamPickerTitle);
        btnBack = view.findViewById(R.id.btnExamPickerBack);
        progress = view.findViewById(R.id.progressExamPicker);
        recycler = view.findViewById(R.id.recyclerExamPicker);
        adapter = new Adapter();
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);
        btnBack.setOnClickListener(v -> loadCategories());
        loadCategories();
    }

    private void loadCategories() {
        showingExams = false;
        currentSuperCategoryId = 0;
        currentSuperCategoryName = "";
        txtTitle.setText(R.string.prep_choose_course);
        btnBack.setVisibility(View.GONE);
        setLoading(true);
        new Thread(() -> {
            try {
                List<PrepExamRepository.ExamItem> items = PrepExamRepository.loadSuperCategories();
                mainHandler.post(() -> {
                    if (!isAdded()) {
                        return;
                    }
                    setLoading(false);
                    rows.clear();
                    rows.addAll(items);
                    adapter.notifyDataSetChanged();
                    if (items.isEmpty()) {
                        AppToast.show(requireContext(), R.string.prep_course_load_error);
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (!isAdded()) {
                        return;
                    }
                    setLoading(false);
                    AppToast.show(requireContext(), R.string.prep_course_load_error);
                });
            }
        }).start();
    }

    private void loadExams(long superCategoryId, @NonNull String name) {
        showingExams = true;
        currentSuperCategoryId = superCategoryId;
        currentSuperCategoryName = name;
        txtTitle.setText(name);
        btnBack.setVisibility(View.VISIBLE);
        setLoading(true);
        new Thread(() -> {
            try {
                List<PrepExamRepository.ExamItem> items =
                        PrepExamRepository.loadExamsForSuperCategory(superCategoryId);
                mainHandler.post(() -> {
                    if (!isAdded()) {
                        return;
                    }
                    setLoading(false);
                    rows.clear();
                    rows.addAll(items);
                    adapter.notifyDataSetChanged();
                    if (items.isEmpty()) {
                        AppToast.show(requireContext(), R.string.prep_course_load_error);
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (!isAdded()) {
                        return;
                    }
                    setLoading(false);
                    AppToast.show(requireContext(), R.string.prep_course_load_error);
                });
            }
        }).start();
    }

    private void selectExam(@NonNull PrepExamRepository.ExamItem exam) {
        setLoading(true);
        long superId = exam.superCategoryId > 0 ? exam.superCategoryId : currentSuperCategoryId;
        new Thread(() -> {
            try {
                PrepExamRepository.selectExam(superId, exam.id);
                PrepExamRepository.SelectedCourse course = new PrepExamRepository.SelectedCourse();
                course.examId = exam.id;
                course.examName = exam.name;
                course.thumbnailUrl = exam.thumbnailUrl;
                course.superCategoryId = superId;
                course.superCategoryName = currentSuperCategoryName;
                mainHandler.post(() -> {
                    if (!isAdded()) {
                        return;
                    }
                    setLoading(false);
                    AppToast.show(requireContext(), R.string.prep_course_updated);
                    if (listener != null) {
                        listener.onExamSelected(course);
                    }
                    dismissAllowingStateLoss();
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (!isAdded()) {
                        return;
                    }
                    setLoading(false);
                    AppToast.show(requireContext(), R.string.prep_course_select_error);
                });
            }
        }).start();
    }

    private void setLoading(boolean loading) {
        if (progress != null) {
            progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        if (recycler != null) {
            recycler.setVisibility(loading ? View.INVISIBLE : View.VISIBLE);
        }
    }

    private void onRowClick(@NonNull PrepExamRepository.ExamItem item) {
        if (!showingExams && item.isCategory) {
            loadExams(item.id, item.name);
            return;
        }
        if (!showingExams && item.superCategoryId > 0 && item.id > 0 && !item.isCategory) {
            // popular exam style row
            selectExam(item);
            return;
        }
        selectExam(item);
    }

    private class Adapter extends RecyclerView.Adapter<Adapter.Holder> {

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_exam_picker_row, parent, false);
            return new Holder(view);
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
            holder.itemView.setOnClickListener(v -> onRowClick(item));
        }

        @Override
        public int getItemCount() {
            return rows.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            final ImageView imgThumb;
            final TextView txtName;
            final TextView txtMeta;

            Holder(@NonNull View itemView) {
                super(itemView);
                imgThumb = itemView.findViewById(R.id.imgExamThumb);
                txtName = itemView.findViewById(R.id.txtExamName);
                txtMeta = itemView.findViewById(R.id.txtExamMeta);
            }
        }
    }
}

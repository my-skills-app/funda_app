package com.chill.with.examsaathi;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.facebook.shimmer.ShimmerFrameLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Bottom-nav "Subjects" tab — category list (General / Science / …),
 * then subjects inside a category, then SubjectSyllabusActivity.
 */
public class LivetvFragmentActivity extends Fragment {

    private enum UiState { LOADING, CONTENT, ERROR }

    private enum Level { CATEGORIES, SUBJECTS }

    private SwipeRefreshLayout swipeRefreshShloks;
    private TextView txtSubjectsHeaderTitle;
    private TextView txtShloksHeaderSubtitle;
    private ShimmerFrameLayout shimmerChapters;
    private TextView txtShloksError;
    private RecyclerView recyclerChapters;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<PrepSubjectsRepository.Section> sections = new ArrayList<>();
    private final List<PrepSubjectsRepository.Subject> subjectsView = new ArrayList<>();

    private CategoriesAdapter categoriesAdapter;
    private SubjectsAdapter subjectsAdapter;
    private LinearLayoutManager layoutManager;

    private UiState uiState = UiState.LOADING;
    private Level level = Level.CATEGORIES;
    @Nullable
    private PrepSubjectsRepository.Section selectedSection;
    private String examName = "";
    private String examThumb = "";
    private boolean isLoading;
    private OnBackPressedCallback backCallback;

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.livetv_fragment, container, false);
        bindViews(view);
        setupRecycler();
        swipeRefreshShloks.setColorSchemeColors(requireContext().getColor(R.color.app_color));
        swipeRefreshShloks.setOnRefreshListener(this::refresh);

        backCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                showCategories();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), backCallback);

        // Always land on top-level categories when this tab is opened/created
        if (sections.isEmpty()) {
            applyUiState(UiState.LOADING);
            loadSubjects();
        } else {
            showCategories();
        }
        return view;
    }

    @Override
    public void setMenuVisibility(boolean menuVisible) {
        super.setMenuVisibility(menuVisible);
        // Leaving Home/Search/etc. and coming back → reset to full subjects categories
        if (menuVisible && isAdded() && !sections.isEmpty()) {
            showCategories();
        }
    }

    private void bindViews(View view) {
        swipeRefreshShloks = view.findViewById(R.id.swipeRefreshShloks);
        View header = view.findViewById(R.id.shloksHeader);
        txtSubjectsHeaderTitle = header.findViewById(R.id.txtSubjectsHeaderTitle);
        txtShloksHeaderSubtitle = header.findViewById(R.id.txtShloksHeaderSubtitle);
        shimmerChapters = (ShimmerFrameLayout) view.findViewById(R.id.shimmerChapters);
        txtShloksError = view.findViewById(R.id.txtShloksError);
        recyclerChapters = view.findViewById(R.id.recyclerChapters);
    }

    private void setupRecycler() {
        layoutManager = new LinearLayoutManager(requireContext());
        recyclerChapters.setLayoutManager(layoutManager);
        categoriesAdapter = new CategoriesAdapter();
        subjectsAdapter = new SubjectsAdapter();
        recyclerChapters.setAdapter(categoriesAdapter);
    }

    private void refresh() {
        swipeRefreshShloks.setRefreshing(false);
        applyUiState(UiState.LOADING);
        if (level == Level.SUBJECTS && selectedSection != null) {
            // Keep drill-down; reload data then restore subjects view
            final long keepId = selectedSection.id;
            loadSubjects(() -> {
                for (PrepSubjectsRepository.Section s : sections) {
                    if (s.id == keepId) {
                        showSubjects(s);
                        return;
                    }
                }
                showCategories();
            });
        } else {
            loadSubjects(null);
        }
    }

    private void loadSubjects() {
        loadSubjects(null);
    }

    private void loadSubjects(@Nullable Runnable after) {
        if (!isAdded() || isLoading) {
            swipeRefreshShloks.setRefreshing(false);
            return;
        }
        isLoading = true;
        new Thread(() -> {
            try {
                PrepSubjectsRepository.Result result = PrepSubjectsRepository.loadAllSubjects();
                mainHandler.post(() -> onLoaded(result, after));
            } catch (Exception e) {
                mainHandler.post(this::onError);
            }
        }).start();
    }

    private void onLoaded(@NonNull PrepSubjectsRepository.Result result, @Nullable Runnable after) {
        if (!isAdded()) {
            return;
        }
        isLoading = false;
        swipeRefreshShloks.setRefreshing(false);
        examName = result.examName != null ? result.examName : "";
        examThumb = result.examThumbnailUrl != null ? result.examThumbnailUrl : "";
        sections.clear();
        sections.addAll(result.sections);
        applyUiState(UiState.CONTENT);
        if (after != null) {
            after.run();
        } else {
            showCategories();
        }
    }

    private void onError() {
        if (!isAdded()) {
            return;
        }
        isLoading = false;
        swipeRefreshShloks.setRefreshing(false);
        if (sections.isEmpty()) {
            applyUiState(UiState.ERROR);
        } else {
            applyUiState(UiState.CONTENT);
            Toast.makeText(requireContext(), R.string.subjects_load_error, Toast.LENGTH_SHORT).show();
        }
    }

    private void showCategories() {
        level = Level.CATEGORIES;
        selectedSection = null;
        if (backCallback != null) {
            backCallback.setEnabled(false);
        }
        if (txtSubjectsHeaderTitle != null) {
            txtSubjectsHeaderTitle.setText(R.string.shloks_screen_title);
        }
        txtShloksHeaderSubtitle.setText(R.string.subjects_categories_subtitle);
        recyclerChapters.setAdapter(categoriesAdapter);
        categoriesAdapter.notifyDataSetChanged();
        applyUiState(sections.isEmpty() && !isLoading ? UiState.ERROR : UiState.CONTENT);
    }

    private void showSubjects(@NonNull PrepSubjectsRepository.Section section) {
        level = Level.SUBJECTS;
        selectedSection = section;
        if (backCallback != null) {
            backCallback.setEnabled(true);
        }
        if (txtSubjectsHeaderTitle != null) {
            txtSubjectsHeaderTitle.setText(section.name != null ? section.name : getString(R.string.nav_subjects));
        }
        int count = section.subjects.size();
        txtShloksHeaderSubtitle.setText(getString(R.string.subjects_in_category_count, count));
        subjectsView.clear();
        subjectsView.addAll(section.subjects);
        recyclerChapters.setAdapter(subjectsAdapter);
        subjectsAdapter.notifyDataSetChanged();
        applyUiState(UiState.CONTENT);
    }

    private void applyUiState(UiState state) {
        uiState = state;
        switch (state) {
            case LOADING:
                txtShloksError.setVisibility(View.GONE);
                recyclerChapters.setVisibility(View.GONE);
                showShimmer(true);
                break;
            case CONTENT:
                txtShloksError.setVisibility(View.GONE);
                showShimmer(false);
                recyclerChapters.setVisibility(View.VISIBLE);
                break;
            case ERROR:
                showShimmer(false);
                recyclerChapters.setVisibility(View.GONE);
                txtShloksError.setVisibility(View.VISIBLE);
                break;
            default:
                break;
        }
    }

    private void showShimmer(boolean show) {
        if (shimmerChapters == null) {
            return;
        }
        if (show) {
            shimmerChapters.setVisibility(View.VISIBLE);
            shimmerChapters.startShimmer();
        } else {
            shimmerChapters.stopShimmer();
            shimmerChapters.setVisibility(View.GONE);
        }
    }

    private final class CategoriesAdapter extends RecyclerView.Adapter<CategoriesAdapter.Holder> {
        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_gita_chapter, parent, false);
            return new Holder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            PrepSubjectsRepository.Section section = sections.get(position);
            holder.txtNumber.setText(String.valueOf(position + 1));
            holder.txtTitle.setText(section.name != null ? section.name : "");
            holder.txtSubtitle.setVisibility(View.VISIBLE);
            holder.txtSubtitle.setText(
                    getString(R.string.subjects_category_count, section.subjects.size()));
            holder.itemView.setOnClickListener(v -> showSubjects(section));
        }

        @Override
        public int getItemCount() {
            return sections.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            final TextView txtNumber;
            final TextView txtTitle;
            final TextView txtSubtitle;

            Holder(@NonNull View itemView) {
                super(itemView);
                txtNumber = itemView.findViewById(R.id.txtChapterNumber);
                txtTitle = itemView.findViewById(R.id.txtChapterTitle);
                txtSubtitle = itemView.findViewById(R.id.txtChapterSubtitle);
            }
        }
    }

    private final class SubjectsAdapter extends RecyclerView.Adapter<SubjectsAdapter.Holder> {
        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_subject_with_icon, parent, false);
            return new Holder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            PrepSubjectsRepository.Subject item = subjectsView.get(position);
            holder.txtTitle.setText(item.name != null ? item.name : "");
            if (item.badge != null && !item.badge.isEmpty()) {
                holder.txtSubtitle.setVisibility(View.VISIBLE);
                holder.txtSubtitle.setText(item.badge);
            } else {
                holder.txtSubtitle.setVisibility(View.GONE);
            }

            String iconUrl = item.thumbnailUrl != null ? item.thumbnailUrl.trim() : "";
            if (!iconUrl.isEmpty()) {
                holder.txtNumber.setVisibility(View.GONE);
                holder.cardIcon.setVisibility(View.VISIBLE);
                Glide.with(holder.itemView.getContext())
                        .load(iconUrl)
                        .centerCrop()
                        .into(holder.imgIcon);
            } else {
                holder.cardIcon.setVisibility(View.GONE);
                holder.txtNumber.setVisibility(View.VISIBLE);
                holder.txtNumber.setText(String.valueOf(position + 1));
            }

            holder.itemView.setOnClickListener(v ->
                    SubjectSyllabusActivity.open(
                            requireContext(),
                            item.id,
                            item.name,
                            examName,
                            examThumb,
                            item.thumbnailUrl));
        }

        @Override
        public int getItemCount() {
            return subjectsView.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            final TextView txtNumber;
            final CardView cardIcon;
            final ImageView imgIcon;
            final TextView txtTitle;
            final TextView txtSubtitle;

            Holder(@NonNull View itemView) {
                super(itemView);
                txtNumber = itemView.findViewById(R.id.txtSubjectNumber);
                cardIcon = itemView.findViewById(R.id.cardSubjectIcon);
                imgIcon = itemView.findViewById(R.id.imgSubjectIcon);
                txtTitle = itemView.findViewById(R.id.txtSubjectTitle);
                txtSubtitle = itemView.findViewById(R.id.txtSubjectSubtitle);
            }
        }
    }
}

package com.chill.with.examsaathi;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.FirebaseApp;

import java.util.ArrayList;
import java.util.List;

public class SearchFragmentActivity extends Fragment {

    private EditText searchEditText;
    private TextView txtSearchSubtitle;
    private TextView txtSearchEmpty;
    private TextView txtRecentEmpty;
    private ProgressBar progressSearchLoading;
    private LinearLayout layoutRecentSearch;
    private RecyclerView recyclerRecentSearch;
    private RecyclerView recyclerSearchResults;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<GitaSearchIndex.SearchHit> results = new ArrayList<>();
    private final List<String> recentItems = new ArrayList<>();
    private SearchResultsAdapter adapter;
    private RecentSearchAdapter recentAdapter;

    private int searchGeneration;
    private Runnable pendingApiSearch;
    private boolean indexReady;
    private String currentQuery = "";

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.search_fragment, container, false);
        bindViews(view);
        FirebaseApp.initializeApp(requireContext());
        setupRecycler();
        setupSearchInput();
        loadSearchIndex();
        refreshRecentSearch();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (currentQuery.trim().isEmpty()) {
            refreshRecentSearch();
        }
    }

    @Override
    public void onDestroyView() {
        cancelPendingApiSearch();
        super.onDestroyView();
    }

    private void bindViews(View view) {
        searchEditText = view.findViewById(R.id.searchEditText);
        txtSearchSubtitle = view.findViewById(R.id.txtSearchSubtitle);
        txtSearchEmpty = view.findViewById(R.id.txtSearchEmpty);
        txtRecentEmpty = view.findViewById(R.id.txtRecentEmpty);
        progressSearchLoading = view.findViewById(R.id.progressSearchLoading);
        layoutRecentSearch = view.findViewById(R.id.layoutRecentSearch);
        recyclerRecentSearch = view.findViewById(R.id.recyclerRecentSearch);
        recyclerSearchResults = view.findViewById(R.id.recyclerSearchResults);
    }

    private void setupRecycler() {
        recyclerSearchResults.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new SearchResultsAdapter();
        recyclerSearchResults.setAdapter(adapter);

        recyclerRecentSearch.setLayoutManager(new LinearLayoutManager(requireContext()));
        recentAdapter = new RecentSearchAdapter();
        recyclerRecentSearch.setAdapter(recentAdapter);
    }

    private void setupSearchInput() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                onQueryChanged(s != null ? s.toString() : "");
            }
        });

        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                cancelPendingApiSearch();
                String query = searchEditText.getText().toString();
                saveRecentQuery(query);
                runSearch(query, true);
                hideKeyboard();
                searchEditText.clearFocus();
                return true;
            }
            return false;
        });
    }

    private void loadSearchIndex() {
        showIndexLoading(true);
        GitaSearchIndex.getInstance().ensureLoaded(new GitaSearchIndex.LoadCallback() {
            @Override
            public void onLoaded() {
                if (!isAdded()) {
                    return;
                }
                indexReady = true;
                showIndexLoading(false);
                txtSearchSubtitle.setText(R.string.search_screen_subtitle);
                String query = searchEditText.getText().toString();
                if (!query.trim().isEmpty()) {
                    onQueryChanged(query);
                } else {
                    refreshRecentSearch();
                }
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) {
                    return;
                }
                showIndexLoading(false);
                txtSearchSubtitle.setText(R.string.search_index_error);
                Toast.makeText(requireContext(), R.string.search_index_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onQueryChanged(String query) {
        currentQuery = query != null ? query : "";
        cancelPendingApiSearch();

        String trimmed = currentQuery.trim();
        if (trimmed.isEmpty()) {
            searchGeneration++;
            showResults(new ArrayList<>(), false, false);
            refreshRecentSearch();
            return;
        }

        layoutRecentSearch.setVisibility(View.GONE);

        if (!indexReady) {
            return;
        }

        List<GitaSearchIndex.SearchHit> localHits = GitaSearchIndex.getInstance().searchLocal(trimmed);
        if (!localHits.isEmpty()) {
            searchGeneration++;
            showResults(localHits, false, false);
            return;
        }

        showResults(new ArrayList<>(), true, false);
        final int generation = ++searchGeneration;
        pendingApiSearch = () -> runApiSearch(trimmed, generation);
        mainHandler.postDelayed(pendingApiSearch, GitaSearchIndex.API_DEBOUNCE_MS);
    }

    private void runSearch(String query, boolean immediateApi) {
        String trimmed = query != null ? query.trim() : "";
        if (trimmed.isEmpty() || !indexReady) {
            return;
        }

        layoutRecentSearch.setVisibility(View.GONE);

        List<GitaSearchIndex.SearchHit> localHits = GitaSearchIndex.getInstance().searchLocal(trimmed);
        if (!localHits.isEmpty()) {
            searchGeneration++;
            showResults(localHits, false, false);
            return;
        }

        if (immediateApi) {
            final int generation = ++searchGeneration;
            runApiSearch(trimmed, generation);
        }
    }

    private void runApiSearch(String query, int generation) {
        if (!isAdded() || generation != searchGeneration) {
            return;
        }
        showResults(new ArrayList<>(), true, true);

        GitaSearchIndex.getInstance().searchApi(query, new GitaSearchIndex.ApiSearchCallback() {
            @Override
            public void onResults(List<GitaSearchIndex.SearchHit> apiHits) {
                if (!isAdded() || generation != searchGeneration) {
                    return;
                }
                showResults(apiHits, false, false);
            }

            @Override
            public void onError(String message) {
                if (!isAdded() || generation != searchGeneration) {
                    return;
                }
                showResults(new ArrayList<>(), false, false);
                if (currentQuery.trim().length() >= 2) {
                    txtSearchEmpty.setText(R.string.search_no_results);
                }
            }
        });
    }

    private void cancelPendingApiSearch() {
        if (pendingApiSearch != null) {
            mainHandler.removeCallbacks(pendingApiSearch);
            pendingApiSearch = null;
        }
    }

    private void refreshRecentSearch() {
        if (layoutRecentSearch == null || recentAdapter == null) {
            return;
        }
        recentItems.clear();
        recentItems.addAll(LocalRecentSearch.getAll(requireContext()));
        recentAdapter.notifyDataSetChanged();

        if (!currentQuery.trim().isEmpty()) {
            return;
        }

        layoutRecentSearch.setVisibility(View.VISIBLE);
        txtSearchEmpty.setVisibility(View.GONE);
        recyclerSearchResults.setVisibility(View.GONE);

        if (recentItems.isEmpty()) {
            if (txtRecentEmpty != null) {
                txtRecentEmpty.setVisibility(View.VISIBLE);
            }
            if (recyclerRecentSearch != null) {
                recyclerRecentSearch.setVisibility(View.GONE);
            }
        } else {
            if (txtRecentEmpty != null) {
                txtRecentEmpty.setVisibility(View.GONE);
            }
            if (recyclerRecentSearch != null) {
                recyclerRecentSearch.setVisibility(View.VISIBLE);
            }
        }
    }

    private void saveRecentQuery(String query) {
        LocalRecentSearch.add(requireContext(), query);
    }

    private void applyRecentQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            return;
        }
        searchEditText.setText(query);
        searchEditText.setSelection(query.length());
        onQueryChanged(query);
    }

    private void showIndexLoading(boolean loading) {
        progressSearchLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (loading) {
            txtSearchEmpty.setVisibility(View.GONE);
            recyclerSearchResults.setVisibility(View.GONE);
            layoutRecentSearch.setVisibility(View.GONE);
        }
    }

    private void showResults(List<GitaSearchIndex.SearchHit> hits, boolean loading, boolean apiLoading) {
        results.clear();
        if (hits != null) {
            results.addAll(hits);
        }
        adapter.notifyDataSetChanged();

        boolean showLoading = loading || apiLoading;
        progressSearchLoading.setVisibility(showLoading ? View.VISIBLE : View.GONE);

        if (currentQuery.trim().isEmpty()) {
            refreshRecentSearch();
            return;
        }

        layoutRecentSearch.setVisibility(View.GONE);

        if (showLoading && results.isEmpty()) {
            recyclerSearchResults.setVisibility(View.GONE);
            txtSearchEmpty.setVisibility(View.GONE);
            return;
        }

        if (results.isEmpty()) {
            recyclerSearchResults.setVisibility(View.GONE);
            txtSearchEmpty.setVisibility(View.VISIBLE);
            txtSearchEmpty.setText(R.string.search_no_results);
            return;
        }

        recyclerSearchResults.setVisibility(View.VISIBLE);
        txtSearchEmpty.setVisibility(View.GONE);
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) requireContext()
                .getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        if (imm != null && searchEditText != null) {
            imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);
        }
    }

    private void openShloka(String title) {
        if (title == null || title.trim().isEmpty()) {
            return;
        }
        saveRecentQuery(title);
        startActivity(MovieviewActivity.newHomeShlokaIntent(requireContext(), title.trim()));
    }

    private static String formatDescription(String raw) {
        if (raw == null) {
            return "";
        }
        String text = raw.trim();
        int colon = text.indexOf(':');
        if (colon > 0 && colon < 24) {
            text = text.substring(colon + 1).trim();
        }
        return text;
    }

    private class RecentSearchAdapter extends RecyclerView.Adapter<RecentSearchAdapter.Holder> {

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_recent_search, parent, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            String title = recentItems.get(position);
            holder.txtTitle.setText(title);
            holder.itemRoot.setOnClickListener(v -> applyRecentQuery(title));
            holder.btnRemove.setOnClickListener(v -> {
                int adapterPosition = holder.getBindingAdapterPosition();
                if (adapterPosition == RecyclerView.NO_POSITION) {
                    return;
                }
                LocalRecentSearch.remove(requireContext(), recentItems.get(adapterPosition));
                refreshRecentSearch();
            });
        }

        @Override
        public int getItemCount() {
            return recentItems.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            final View itemRoot;
            final TextView txtTitle;
            final View btnRemove;

            Holder(View itemView) {
                super(itemView);
                itemRoot = itemView.findViewById(R.id.itemRoot);
                txtTitle = itemView.findViewById(R.id.txtRecentTitle);
                btnRemove = itemView.findViewById(R.id.btnRemoveRecent);
            }
        }
    }

    private class SearchResultsAdapter extends RecyclerView.Adapter<SearchResultsAdapter.Holder> {

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_search_result, parent, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            GitaSearchIndex.SearchHit hit = results.get(position);
            holder.txtTitle.setText(hit.title != null ? hit.title : "");
            holder.txtDescription.setText(formatDescription(hit.description));

            holder.txtSectionLabel.setVisibility(View.VISIBLE);
            if (hit.apiSuggestion) {
                holder.txtSectionLabel.setText(R.string.search_api_suggested);
            } else if (hit.sectionTitle != null && !hit.sectionTitle.isEmpty()) {
                holder.txtSectionLabel.setText(hit.sectionTitle);
            } else {
                holder.txtSectionLabel.setVisibility(View.GONE);
            }

            if (hit.image != null && !hit.image.isEmpty() && !"-".equals(hit.image)) {
                Glide.with(holder.itemView.getContext())
                        .load(hit.image)
                        .centerCrop()
                        .into(holder.imgThumb);
            }

            holder.itemRoot.setOnClickListener(v -> openShloka(hit.title));
        }

        @Override
        public int getItemCount() {
            return results.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            final View itemRoot;
            final android.widget.ImageView imgThumb;
            final TextView txtSectionLabel;
            final TextView txtTitle;
            final TextView txtDescription;

            Holder(View itemView) {
                super(itemView);
                itemRoot = itemView.findViewById(R.id.itemRoot);
                imgThumb = itemView.findViewById(R.id.imgShlokaThumb);
                txtSectionLabel = itemView.findViewById(R.id.txtSectionLabel);
                txtTitle = itemView.findViewById(R.id.txtShlokaTitle);
                txtDescription = itemView.findViewById(R.id.txtShlokaDesc);
            }
        }
    }
}

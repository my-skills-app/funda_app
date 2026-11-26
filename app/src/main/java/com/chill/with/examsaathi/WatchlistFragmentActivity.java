package com.chill.with.examsaathi;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.google.firebase.FirebaseApp;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class WatchlistFragmentActivity extends Fragment {

    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView txtWatchlistHeaderSubtitle;
    private LinearLayout layoutRecentViewsSection;
    private RecyclerView recyclerRecentViews;
    private LinearLayout layoutSavedSection;
    private LinearLayout layoutSavedEmpty;
    private RecyclerView recyclerWatchlist;
    private LinearLayout layoutWatchlistEmpty;

    private final List<LocalWatchlist.Entry> entries = new ArrayList<>();
    private final List<LocalRecentViews.Entry> recentEntries = new ArrayList<>();
    private WatchlistAdapter watchlistAdapter;
    private RecentViewsAdapter recentViewsAdapter;

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.watchlist_fragment, container, false);
        bindViews(view);
        FirebaseApp.initializeApp(requireContext());
        setupRecycler();
        swipeRefreshLayout.setColorSchemeColors(requireContext().getColor(R.color.app_color));
        swipeRefreshLayout.setOnRefreshListener(this::refreshWatchlist);
        loadWatchlist();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadWatchlist();
    }

    private void bindViews(View view) {
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        View header = view.findViewById(R.id.watchlistHeader);
        txtWatchlistHeaderSubtitle = header.findViewById(R.id.txtWatchlistHeaderSubtitle);
        layoutRecentViewsSection = view.findViewById(R.id.layoutRecentViewsSection);
        recyclerRecentViews = view.findViewById(R.id.recyclerRecentViews);
        layoutSavedSection = view.findViewById(R.id.layoutSavedSection);
        layoutSavedEmpty = view.findViewById(R.id.layoutSavedEmpty);
        recyclerWatchlist = view.findViewById(R.id.recyclerWatchlist);
        layoutWatchlistEmpty = view.findViewById(R.id.layoutWatchlistEmpty);
    }

    private void setupRecycler() {
        recyclerRecentViews.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        recentViewsAdapter = new RecentViewsAdapter();
        recyclerRecentViews.setAdapter(recentViewsAdapter);

        recyclerWatchlist.setLayoutManager(new LinearLayoutManager(requireContext()));
        watchlistAdapter = new WatchlistAdapter();
        recyclerWatchlist.setAdapter(watchlistAdapter);
    }

    private void refreshWatchlist() {
        swipeRefreshLayout.setRefreshing(false);
        loadWatchlist();
    }

    private void loadWatchlist() {
        entries.clear();
        entries.addAll(LocalWatchlist.getAll(requireContext()));
        recentEntries.clear();
        recentEntries.addAll(LocalRecentViews.getAll(requireContext()));
        updateHeaderSubtitle();
        applyUiState();
        if (watchlistAdapter != null) {
            watchlistAdapter.notifyDataSetChanged();
        }
        if (recentViewsAdapter != null) {
            recentViewsAdapter.notifyDataSetChanged();
        }
        swipeRefreshLayout.setRefreshing(false);
    }

    private void updateHeaderSubtitle() {
        int savedCount = entries.size();
        int recentCount = recentEntries.size();
        if (savedCount == 0 && recentCount == 0) {
            txtWatchlistHeaderSubtitle.setText(R.string.watchlist_empty);
        } else if (savedCount == 0) {
            txtWatchlistHeaderSubtitle.setText(
                    getString(R.string.watchlist_recent_count, recentCount));
        } else if (savedCount == 1) {
            txtWatchlistHeaderSubtitle.setText(R.string.watchlist_saved_count_one);
        } else {
            txtWatchlistHeaderSubtitle.setText(
                    getString(R.string.watchlist_saved_count, savedCount, "s"));
        }
    }

    private void applyUiState() {
        boolean hasRecent = !recentEntries.isEmpty();
        boolean hasSaved = !entries.isEmpty();

        layoutRecentViewsSection.setVisibility(hasRecent ? View.VISIBLE : View.GONE);
        layoutSavedSection.setVisibility(View.VISIBLE);
        recyclerWatchlist.setVisibility(hasSaved ? View.VISIBLE : View.GONE);
        if (layoutSavedEmpty != null) {
            layoutSavedEmpty.setVisibility(hasSaved ? View.GONE : View.VISIBLE);
        }
        layoutWatchlistEmpty.setVisibility(hasRecent || hasSaved ? View.GONE : View.VISIBLE);
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

    private void openWatchlistEntry(@NonNull LocalWatchlist.Entry entry) {
        String url = entry.videoUrl != null ? entry.videoUrl.trim() : "";
        String title = entry.title != null ? entry.title.trim() : "";
        String poster = entry.posterUrl != null ? entry.posterUrl : "";
        String id = entry.videoid != null && !entry.videoid.isEmpty() ? entry.videoid : title;
        String type = entry.mediaType != null ? entry.mediaType.trim().toLowerCase() : "video";

        if (url.isEmpty()) {
            if (!title.isEmpty()) {
                startActivity(MovieviewActivity.newHomeShlokaIntent(requireContext(), title));
            }
            return;
        }

        if ("audio".equals(type)) {
            Intent player = new Intent(requireContext(), GitaAudioPlayerActivity.class);
            player.putExtra("videoUrl", url);
            player.putExtra("title", title);
            player.putExtra("posterUrl", poster);
            player.putExtra("videoid", id);
            player.putExtra("description", entry.description != null ? entry.description : "");
            startActivity(player);
            return;
        }

        // Video → vertical reels player with single-item playlist
        try {
            JSONArray arr = new JSONArray();
            JSONObject obj = new JSONObject();
            obj.put("id", id);
            obj.put("sequence", 1);
            obj.put("title", title);
            obj.put("name", title);
            obj.put("description", entry.description != null ? entry.description : "");
            obj.put("image", poster);
            obj.put("videoUrl", url);
            arr.put(obj);
            PrepReelsPlayerActivity.open(requireContext(), arr.toString(), id, title);
        } catch (Exception e) {
            Intent player = new Intent(requireContext(), ExoPlayerActivity.class);
            player.putExtra("link", url);
            player.putExtra("title", title);
            player.putExtra("img", poster);
            player.putExtra("videoid", id);
            player.putExtra("type", "Gita");
            startActivity(player);
        }
    }

    private class RecentViewsAdapter extends RecyclerView.Adapter<RecentViewsAdapter.Holder> {

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_gita_shloka, parent, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            LocalRecentViews.Entry entry = recentEntries.get(position);
            holder.txtTitle.setText(entry.title != null ? entry.title : "");
            holder.txtDescription.setText(formatDescription(entry.description));

            if (entry.posterUrl != null && !entry.posterUrl.isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(entry.posterUrl)
                        .centerCrop()
                        .into(holder.imgThumb);
            }

            holder.itemRoot.setOnClickListener(v -> {
                // Recent views remain title-based for now
                String title = entry.title != null ? entry.title.trim() : "";
                if (title.isEmpty()) {
                    return;
                }
                startActivity(MovieviewActivity.newHomeShlokaIntent(requireContext(), title));
            });
        }

        @Override
        public int getItemCount() {
            return recentEntries.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            final View itemRoot;
            final android.widget.ImageView imgThumb;
            final TextView txtTitle;
            final TextView txtDescription;

            Holder(View itemView) {
                super(itemView);
                itemRoot = itemView;
                imgThumb = itemView.findViewById(R.id.imgShloka);
                txtTitle = itemView.findViewById(R.id.txtTitle);
                txtDescription = itemView.findViewById(R.id.txtDescription);
            }
        }
    }

    private class WatchlistAdapter extends RecyclerView.Adapter<WatchlistAdapter.Holder> {

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_watchlist_shloka, parent, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            LocalWatchlist.Entry entry = entries.get(position);
            holder.txtTitle.setText(entry.title != null ? entry.title : "");
            holder.txtDescription.setText(formatDescription(entry.description));

            if (entry.posterUrl != null && !entry.posterUrl.isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(entry.posterUrl)
                        .centerCrop()
                        .into(holder.imgThumb);
            }

            holder.itemRoot.setOnClickListener(v -> openWatchlistEntry(entry));

            holder.imgRemoveBookmark.setOnClickListener(v -> {
                int adapterPosition = holder.getBindingAdapterPosition();
                if (adapterPosition == RecyclerView.NO_POSITION) {
                    return;
                }
                LocalWatchlist.Entry clicked = entries.get(adapterPosition);
                String title = clicked.title != null ? clicked.title : "";
                if (LocalWatchlist.remove(requireContext(), title)) {
                    Toast.makeText(requireContext(), R.string.watchlist_removed, Toast.LENGTH_SHORT).show();
                    loadWatchlist();
                }
            });
        }

        @Override
        public int getItemCount() {
            return entries.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            final View itemRoot;
            final android.widget.ImageView imgThumb;
            final TextView txtTitle;
            final TextView txtDescription;
            final android.widget.ImageView imgRemoveBookmark;

            Holder(View itemView) {
                super(itemView);
                itemRoot = itemView.findViewById(R.id.itemRoot);
                imgThumb = itemView.findViewById(R.id.imgShlokaThumb);
                txtTitle = itemView.findViewById(R.id.txtShlokaTitle);
                txtDescription = itemView.findViewById(R.id.txtShlokaDesc);
                imgRemoveBookmark = itemView.findViewById(R.id.imgRemoveBookmark);
            }
        }
    }
}

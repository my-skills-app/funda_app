package com.chill.with.examsaathi;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/** Local dashboard index + debounced title API lookup for search. */
public final class GitaSearchIndex {

    public static final long API_DEBOUNCE_MS = 650;
    private static final int MIN_API_QUERY_LENGTH = 2;
    private static final int MAX_LOCAL_RESULTS = 40;
    private static final int MAX_API_RESULTS = 12;

    private static GitaSearchIndex instance;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<GitaDashboard.Section> sections = new ArrayList<>();

    private boolean loaded;
    private boolean loading;

    public static class SearchHit {
        public String title;
        public String description;
        public String image;
        public String sectionTitle;
        public boolean apiSuggestion;
    }

    public interface LoadCallback {
        void onLoaded();

        void onError(String message);
    }

    public interface ApiSearchCallback {
        void onResults(List<SearchHit> results);

        void onError(String message);
    }

    private GitaSearchIndex() {}

    public static synchronized GitaSearchIndex getInstance() {
        if (instance == null) {
            instance = new GitaSearchIndex();
        }
        return instance;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public void ensureLoaded(LoadCallback callback) {
        if (loaded) {
            if (callback != null) {
                callback.onLoaded();
            }
            return;
        }
        if (loading) {
            return;
        }
        loading = true;

        new Thread(() -> {
            try {
                android.content.Context ctx = MyApp.getContext();
                if (ctx == null) {
                    throw new IllegalStateException("App context unavailable");
                }
                String json = SecureHttpClient.get(ctx, GitaDashboard.apiUrl());
                GitaDashboard.Data data = GitaDashboard.parse(json);
                sections.clear();
                if (data != null && data.sections != null) {
                    sections.addAll(data.sections);
                }
                loaded = true;
                loading = false;
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onLoaded();
                    }
                });
            } catch (Exception e) {
                loading = false;
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onError(e.getMessage() != null ? e.getMessage() : "Failed to load");
                    }
                });
            }
        }).start();
    }

    public List<SearchHit> searchLocal(String query) {
        List<SearchHit> hits = new ArrayList<>();
        String needle = normalize(query);
        if (needle.isEmpty()) {
            return hits;
        }

        for (GitaDashboard.Section section : sections) {
            if (section == null || section.shlokas == null) {
                continue;
            }
            String sectionTitle = section.sectionTitle != null ? section.sectionTitle : "";
            for (GitaDashboard.ShlokaItem item : section.shlokas) {
                if (item == null) {
                    continue;
                }
                if (matchesLocal(item, sectionTitle, needle)) {
                    SearchHit hit = new SearchHit();
                    hit.title = item.title != null ? item.title : "";
                    hit.description = item.description != null ? item.description : "";
                    hit.image = item.image != null ? item.image : "";
                    hit.sectionTitle = sectionTitle;
                    hit.apiSuggestion = false;
                    hits.add(hit);
                    if (hits.size() >= MAX_LOCAL_RESULTS) {
                        return hits;
                    }
                }
            }
        }
        return hits;
    }

    public void searchApi(String query, ApiSearchCallback callback) {
        String trimmed = query != null ? query.trim() : "";
        if (trimmed.length() < MIN_API_QUERY_LENGTH) {
            if (callback != null) {
                mainHandler.post(() -> callback.onResults(new ArrayList<>()));
            }
            return;
        }

        new Thread(() -> {
            try {
                List<SearchHit> hits = fetchApiHits(trimmed);
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onResults(hits);
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onError(e.getMessage() != null ? e.getMessage() : "Search failed");
                    }
                });
            }
        }).start();
    }

    private List<SearchHit> fetchApiHits(String query) throws Exception {
        List<SearchHit> hits = new ArrayList<>();
        LinkedHashSet<String> seenTitles = new LinkedHashSet<>();
        String needle = normalize(query);

        android.content.Context ctx = MyApp.getContext();
        if (ctx == null) {
            throw new IllegalStateException("App context unavailable");
        }
        for (String candidate : buildTitleCandidates(query)) {
            String json = SecureHttpClient.get(ctx, GitaChapterShlokas.shlokaByTitleUrl(candidate));
            GitaChapterShlokas.ShlokaDetailResponse response =
                    GitaChapterShlokas.parseByTitleResponse(json);
            if (response == null) {
                continue;
            }

            if (response.shloka != null && response.shloka.title != null) {
                addApiHit(hits, seenTitles, response.shloka, true);
            }

            if (response.chapterShlokas != null) {
                for (GitaChapterShlokas.Shloka shloka : response.chapterShlokas) {
                    if (shloka == null) {
                        continue;
                    }
                    if (matchesApiShloka(shloka, needle)) {
                        addApiHit(hits, seenTitles, shloka, false);
                    }
                    if (hits.size() >= MAX_API_RESULTS) {
                        return hits;
                    }
                }
            }

            if (!hits.isEmpty()) {
                return hits;
            }
        }
        return hits;
    }

    private static void addApiHit(List<SearchHit> hits, LinkedHashSet<String> seenTitles,
                                  GitaChapterShlokas.Shloka shloka, boolean forceAdd) {
        String title = shloka.title != null ? shloka.title.trim() : "";
        if (title.isEmpty() || seenTitles.contains(title.toLowerCase(Locale.US))) {
            return;
        }
        seenTitles.add(title.toLowerCase(Locale.US));
        SearchHit hit = new SearchHit();
        hit.title = title;
        hit.description = GitaChapterShlokas.shortDescription(shloka);
        hit.image = shloka.image != null ? shloka.image : "";
        hit.sectionTitle = forceAdd
                ? "Suggested"
                : "Suggested";
        hit.apiSuggestion = true;
        hits.add(hit);
    }

    private static boolean matchesApiShloka(GitaChapterShlokas.Shloka shloka, String needle) {
        if (needle.isEmpty()) {
            return true;
        }
        String title = shloka.title != null ? normalize(shloka.title) : "";
        String desc = shloka.description != null ? normalize(shloka.description) : "";
        String name = shloka.name != null ? normalize(shloka.name) : "";
        return title.contains(needle) || desc.contains(needle) || name.contains(needle);
    }

    private static boolean matchesLocal(GitaDashboard.ShlokaItem item, String sectionTitle, String needle) {
        String title = item.title != null ? normalize(item.title) : "";
        String desc = item.description != null ? normalize(item.description) : "";
        String section = sectionTitle != null ? normalize(sectionTitle) : "";
        return title.contains(needle) || desc.contains(needle) || section.contains(needle);
    }

    private static String normalize(String value) {
        return value != null ? value.trim().toLowerCase(Locale.US) : "";
    }

    static List<String> buildTitleCandidates(String query) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        String trimmed = query != null ? query.trim() : "";
        if (trimmed.isEmpty()) {
            return new ArrayList<>(candidates);
        }

        candidates.add(trimmed);
        String lower = trimmed.toLowerCase(Locale.US);
        if (!lower.startsWith("shloka")) {
            candidates.add("Shloka " + trimmed);
        }

        if (trimmed.matches("\\d+(\\.\\d+)?")) {
            candidates.add("Shloka " + trimmed);
        } else if (trimmed.matches("(?i)shloka\\s+\\d+(\\.\\d+)?")) {
            candidates.add(trimmed.replaceAll("(?i)^shloka\\s+", "Shloka "));
        }
        return new ArrayList<>(candidates);
    }
}

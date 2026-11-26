package com.chill.with.examsaathi;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/** Loads Prepshots home page from live API (single fast request + disk cache). */
public final class PrepHomeRepository {

    private static final String TAG = "PrepHomeRepo";

    public static final class Result {
        @NonNull
        public final GitaDashboard.Data dashboard;
        @Nullable
        public final String displayName;
        public final boolean fromCache;

        public Result(
                @NonNull GitaDashboard.Data dashboard,
                @Nullable String displayName,
                boolean fromCache) {
            this.dashboard = dashboard;
            this.displayName = displayName;
            this.fromCache = fromCache;
        }
    }

    private PrepHomeRepository() {}

    @Nullable
    public static Result loadCached(@NonNull Context context) {
        String json = PrepHomeCache.loadHomeJson(context);
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            JSONArray cards = PrepHomeMapper.unwrapArray(json);
            if (cards == null || cards.length() == 0) {
                return null;
            }
            GitaDashboard.Data dashboard = PrepHomeMapper.mapHomeCards(cards);
            return new Result(dashboard, PrepHomeCache.loadDisplayName(context), true);
        } catch (Exception e) {
            Log.w(TAG, "cache parse failed", e);
            return null;
        }
    }

    @NonNull
    public static Result loadHome(@Nullable Context context) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<String> homeFuture = pool.submit(() -> fetchAllHomeCardsJson());
            Future<String> meFuture = pool.submit(() -> {
                try {
                    return PrepHttpClient.get(PrepApiConfig.homeMeUrl());
                } catch (Exception e) {
                    return null;
                }
            });

            String homeJson = homeFuture.get(25, TimeUnit.SECONDS);
            JSONArray cards = PrepHomeMapper.unwrapArray(homeJson);
            if (cards == null) {
                cards = new JSONArray();
            }
            GitaDashboard.Data dashboard = PrepHomeMapper.mapHomeCards(cards);

            String displayName = null;
            try {
                String meJson = meFuture.get(8, TimeUnit.SECONDS);
                displayName = PrepHomeMapper.displayNameFromMe(meJson);
            } catch (Exception ignored) {
                // Home can still render without /me
            }

            if (context != null) {
                PrepHomeCache.saveHomeJson(context, homeJson);
                PrepHomeCache.saveDisplayName(context, displayName);
            }

            return new Result(dashboard, displayName, false);
        } finally {
            pool.shutdownNow();
        }
    }

    /** One (or few) large pages instead of many limit=5 round-trips. */
    @NonNull
    private static String fetchAllHomeCardsJson() throws Exception {
        final int pageSize = 50;
        JSONArray all = new JSONArray();
        String lastRaw = null;
        boolean hasMore = true;

        for (int offset = 0; hasMore && offset <= 500; offset += pageSize) {
            String json = PrepHttpClient.get(PrepApiConfig.homePageUrl(pageSize, offset));
            lastRaw = json;
            JSONArray page = PrepHomeMapper.unwrapArray(json);
            if (page == null || page.length() == 0) {
                break;
            }
            for (int i = 0; i < page.length(); i++) {
                all.put(page.get(i));
            }
            hasMore = readHasMore(json) && page.length() >= pageSize;
            if (!hasMore) {
                break;
            }
        }

        if (all.length() == 0 && lastRaw != null) {
            return lastRaw;
        }
        // Rebuild a response-shaped payload for cache consistency
        JSONObject out = new JSONObject();
        out.put("success", true);
        out.put("message", "Success");
        out.put("data", all);
        JSONObject pag = new JSONObject();
        pag.put("offset", 0);
        pag.put("limit", all.length());
        pag.put("hasMore", false);
        out.put("pagination", pag);
        return out.toString();
    }

    private static boolean readHasMore(@Nullable String json) {
        if (json == null || json.isEmpty()) {
            return false;
        }
        try {
            JSONObject root = new JSONObject(json);
            JSONObject pag = root.optJSONObject("pagination");
            if (pag != null) {
                return pag.optBoolean("hasMore", false);
            }
        } catch (Exception ignored) {
            // ignore
        }
        return false;
    }

    @Nullable
    public static PrepHomeMapper.PlayableVideo loadPlayable(long seriesId, long videoId) throws Exception {
        if (seriesId <= 0) {
            return null;
        }
        Long vid = videoId > 0 ? videoId : null;
        String json = PrepHttpClient.get(PrepApiConfig.homeFeedUrl(seriesId, vid));
        return PrepHomeMapper.firstPlayableFromFeed(json);
    }

    @Nullable
    public static PrepHomeMapper.PlayableVideo loadPlayableOrDirect(@NonNull GitaDashboard.ShlokaItem item)
            throws Exception {
        if (item.playUrl != null && !item.playUrl.isEmpty()) {
            PrepHomeMapper.PlayableVideo direct = new PrepHomeMapper.PlayableVideo();
            direct.url = item.playUrl;
            direct.title = item.title != null ? item.title : "";
            direct.image = item.image != null ? item.image : "";
            direct.videoId = String.valueOf(item.videoId > 0 ? item.videoId : item.contentId);
            return direct;
        }
        long seriesId = item.seriesId;
        if (seriesId <= 0 && "category".equalsIgnoreCase(item.contentType)) {
            return null;
        }
        if (seriesId <= 0) {
            seriesId = item.contentId;
        }
        return loadPlayable(seriesId, item.videoId);
    }
}

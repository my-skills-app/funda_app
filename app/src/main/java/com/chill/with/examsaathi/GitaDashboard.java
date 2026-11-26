package com.chill.with.examsaathi;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Parses Bhagavad Gita dashboard JSON from the remote API. */
public final class GitaDashboard {

    public static String apiUrl() {
        return ApiConfig.dashboardUrl();
    }

    private GitaDashboard() {}

    public static class ShlokaItem {
        public String title;
        public String description;
        public String image;
        public int contentId;
        public String chapterName;
        public String levelName;
        /** Prep local: series id for feed playback. */
        public long seriesId;
        /** Prep local: optional video id within series. */
        public long videoId;
        /** Prep local: contentType e.g. video/category/series. */
        public String contentType = "";
        /** Prep local: direct playable URL when already known. */
        public String playUrl = "";
        /** Prep local: PYQ / promo badge text e.g. "15.5K+ PYQs". */
        public String badge = "";
    }

    public static class Section {
        public String sectionTitle;
        public String sectionType;
        /** Prep: "grid" (subjects), "trending" (bg band), else horizontal. */
        public String layout = "";
        public String bgImage = "";
        public String tag = "";
        public String subTitle = "";
        public boolean hasMoreContent;
        public List<ShlokaItem> shlokas = new ArrayList<>();
    }

    public static class Banner {
        public String imageKey;
        public String url;
        public String title = "";
        public long seriesId;
        public long videoId;
        public String playUrl = "";
    }

    public static class DailyShloka {
        public String title;
        public String description;
        public String image;
        public int chapterId;
    }

    public static class Ad {
        public String videoUrl = "";
    }

    public static class Data {
        public DailyShloka dailyShloka;
        public List<Banner> banners = new ArrayList<>();
        @Nullable
        public Ad ad;
        public List<Section> sections = new ArrayList<>();
    }

    public static Data parse(String json) throws Exception {
        JSONObject root = new JSONObject(json);
        if (!root.optBoolean("success", false)) {
            throw new Exception("API returned success=false");
        }
        JSONObject response = root.getJSONObject("response");
        Data data = new Data();

        if (response.has("shloka") && !response.isNull("shloka")) {
            data.dailyShloka = parseDailyShloka(response.getJSONObject("shloka"));
        }

        if (response.has("banner")) {
            JSONArray banners = response.getJSONArray("banner");
            for (int i = 0; i < banners.length(); i++) {
                JSONObject b = banners.getJSONObject(i);
                Banner banner = new Banner();
                banner.imageKey = b.optString("image", "");
                banner.url = b.optString("url", "");
                if (!banner.url.isEmpty()) {
                    data.banners.add(banner);
                }
            }
        }

        if (response.has("shlokaHome")) {
            JSONArray shlokaHome = response.getJSONArray("shlokaHome");
            for (int page = 0; page < shlokaHome.length(); page++) {
                JSONArray pageSections = shlokaHome.getJSONArray(page);
                for (int s = 0; s < pageSections.length(); s++) {
                    data.sections.add(parseSection(pageSections.getJSONObject(s)));
                }
            }
        }

        if (response.has("ads") && !response.isNull("ads")) {
            String videoUrl = parseAdsVideoUrl(response.getJSONArray("ads"));
            if (!videoUrl.isEmpty()) {
                Ad ad = new Ad();
                ad.videoUrl = videoUrl;
                data.ad = ad;
            }
        } else if (response.has("ad") && !response.isNull("ad")) {
            String videoUrl = parseAdObjectVideoUrl(response.getJSONObject("ad"));
            if (!videoUrl.isEmpty()) {
                Ad ad = new Ad();
                ad.videoUrl = videoUrl;
                data.ad = ad;
            }
        }

        return data;
    }

    /** Reads dashboard promo video URL from raw JSON (cache or network). */
    @Nullable
    public static String extractAdVideoUrl(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            JSONObject root = new JSONObject(json);
            if (!root.optBoolean("success", false)) {
                return null;
            }
            JSONObject response = root.optJSONObject("response");
            if (response == null) {
                return null;
            }
            if (response.has("ads") && !response.isNull("ads")) {
                String url = parseAdsVideoUrl(response.getJSONArray("ads"));
                return url.isEmpty() ? null : url;
            }
            if (response.has("ad") && !response.isNull("ad")) {
                String url = parseAdObjectVideoUrl(response.getJSONObject("ad"));
                return url.isEmpty() ? null : url;
            }
            return null;
        } catch (Exception ignored) {
            return null;
        }
    }

    /** Prefers {@code active_ad=true}, otherwise first item with a non-empty {@code url}. */
    private static String parseAdsVideoUrl(JSONArray ads) throws Exception {
        if (ads == null || ads.length() == 0) {
            return "";
        }
        String fallback = "";
        for (int i = 0; i < ads.length(); i++) {
            JSONObject item = ads.getJSONObject(i);
            String url = item.optString("url", "").trim();
            if (url.isEmpty()) {
                url = parseAdObjectVideoUrl(item);
            }
            if (url.isEmpty()) {
                continue;
            }
            if (item.optBoolean("active_ad", false)) {
                return url;
            }
            if (fallback.isEmpty()) {
                fallback = url;
            }
        }
        return fallback;
    }

    private static String parseAdObjectVideoUrl(JSONObject ad) {
        if (ad == null) {
            return "";
        }
        String[] keys = {"video", "videoUrl", "videoLink", "video_url", "link", "url"};
        for (String key : keys) {
            String value = ad.optString(key, "").trim();
            if (!value.isEmpty()) {
                return value;
            }
        }
        return "";
    }

    private static DailyShloka parseDailyShloka(JSONObject o) {
        DailyShloka d = new DailyShloka();
        d.title = o.optString("title", "");
        d.description = firstNonEmpty(o.optString("description", ""), o.optString("name", ""));
        d.image = o.optString("image", "");
        d.chapterId = o.optInt("chapterId", 0);
        return d;
    }

    private static Section parseSection(JSONObject o) throws Exception {
        Section section = new Section();
        section.sectionTitle = o.optString("sectionTitle", "");
        section.sectionType = o.optString("sectionType", "");
        if (o.has("shlokas")) {
            JSONArray arr = o.getJSONArray("shlokas");
            for (int i = 0; i < arr.length(); i++) {
                section.shlokas.add(parseShlokaItem(arr.getJSONObject(i)));
            }
        }
        return section;
    }

    private static ShlokaItem parseShlokaItem(JSONObject o) {
        ShlokaItem item = new ShlokaItem();
        item.title = o.optString("title", "");
        item.description = firstNonEmpty(o.optString("description", ""), o.optString("levelName", ""));
        item.image = o.optString("image", "");
        item.contentId = o.optInt("contentId", 0);
        item.chapterName = o.optString("chapterName", "");
        item.levelName = o.optString("levelName", "");
        return item;
    }

    private static String firstNonEmpty(String a, String b) {
        if (a != null && !a.trim().isEmpty()) return a;
        return b != null ? b : "";
    }
}

package com.chill.with.examsaathi;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Parses shlokas for a chapter from chillwith.examsaathi.site API. */
public final class GitaChapterShlokas {

    private GitaChapterShlokas() {}

    public static String shlokasUrl(int chapterId) {
        return ApiConfig.shlokasUrl(chapterId);
    }

    public static String shlokaByTitleUrl(String title) {
        String encoded = Utils.urlEncodeUtf8(title);
        return ApiConfig.shlokaByTitleUrl(encoded);
    }

    public static class ShlokaDetailResponse {
        public Shloka shloka;
        public String chapterName;
        public List<Shloka> chapterShlokas = new ArrayList<>();
    }

    public static class Shloka {
        public String id;
        public int sequence;
        public int chapterId;
        public String title;
        public String name;
        public String description;
        public String image;
        public String videoUrl;
        public boolean isContentAvailable;
    }

    public static List<Shloka> parse(String json) throws Exception {
        JSONObject root = new JSONObject(json);
        JSONArray data = root.getJSONArray("data");
        List<Shloka> shlokas = new ArrayList<>();
        for (int i = 0; i < data.length(); i++) {
            shlokas.add(parseOne(data.getJSONObject(i)));
        }
        Collections.sort(shlokas, Comparator.comparingInt(s -> s.sequence));
        return shlokas;
    }

    public static ShlokaDetailResponse parseByTitleResponse(String json) throws Exception {
        JSONObject root = new JSONObject(json);
        JSONObject data = root.getJSONObject("data");
        ShlokaDetailResponse response = new ShlokaDetailResponse();
        response.shloka = parseOne(data.getJSONObject("shloka"));
        JSONObject chapter = data.optJSONObject("chapter");
        if (chapter != null) {
            response.chapterName = chapter.optString("name", "");
        }
        JSONArray chapterShlokas = data.getJSONArray("chapterShlokas");
        for (int i = 0; i < chapterShlokas.length(); i++) {
            response.chapterShlokas.add(parseOne(chapterShlokas.getJSONObject(i)));
        }
        Collections.sort(response.chapterShlokas, Comparator.comparingInt(s -> s.sequence));
        return response;
    }

    private static Shloka parseOne(JSONObject o) {
        Shloka shloka = new Shloka();
        shloka.id = o.optString("_id", "");
        shloka.sequence = o.optInt("sequence", 0);
        shloka.chapterId = o.optInt("chapterId", 0);
        shloka.title = o.optString("title", "");
        shloka.name = o.optString("name", "");
        shloka.description = o.optString("description", "");
        shloka.image = o.optString("image", "");
        shloka.videoUrl = o.optString("videoUrl", "");
        shloka.isContentAvailable = o.optBoolean("isContentAvailable", false);
        if ("-".equals(shloka.image)) {
            shloka.image = "";
        }
        return shloka;
    }

    /** Full text for detail / movie view screen. */
    public static String displayDescription(Shloka shloka) {
        if (shloka == null) {
            return "";
        }
        String text = shloka.description != null ? shloka.description.trim() : "";
        if (text.isEmpty()) {
            text = shloka.name != null ? shloka.name.trim() : "";
        }
        return text;
    }

    public static String shortDescription(Shloka shloka) {
        if (shloka == null) {
            return "";
        }
        String text = shloka.description != null ? shloka.description.trim() : "";
        if (text.isEmpty()) {
            text = shloka.name != null ? shloka.name.trim() : "";
        }
        int colon = text.indexOf(':');
        if (colon >= 0 && colon + 1 < text.length()) {
            text = text.substring(colon + 1).trim();
        }
        return text;
    }
}

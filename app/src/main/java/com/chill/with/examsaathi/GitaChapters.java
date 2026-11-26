package com.chill.with.examsaathi;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses chapter list from chillwith.examsaathi.site API. */
public final class GitaChapters {

    public static String apiUrl() {
        return ApiConfig.chaptersUrl();
    }

    private static final Pattern CHAPTER_NUM = Pattern.compile("(?i)chapter\\s+(\\d+)");

    private GitaChapters() {}

    public static class Chapter {
        public String id;
        public int sequence;
        public int chapterId;
        public String name;
        public String description;
        public String image;
    }

    public static List<Chapter> parse(String json) throws Exception {
        JSONObject root = new JSONObject(json);
        JSONArray data = root.getJSONArray("data");
        List<Chapter> chapters = new ArrayList<>();
        for (int i = 0; i < data.length(); i++) {
            JSONObject o = data.getJSONObject(i);
            Chapter chapter = new Chapter();
            chapter.id = o.optString("_id", "");
            chapter.sequence = o.optInt("sequence", 0);
            chapter.chapterId = o.optInt("chapterId", 0);
            chapter.name = o.optString("name", "");
            chapter.description = o.optString("description", "");
            chapter.image = o.optString("image", "");
            chapters.add(chapter);
        }
        Collections.sort(chapters, Comparator.comparingInt(c -> c.sequence));
        return chapters;
    }

    public static int chapterNumber(Chapter chapter) {
        if (chapter == null) {
            return 0;
        }
        if (chapter.sequence > 0) {
            return chapter.sequence;
        }
        String source = chapter.name != null && !chapter.name.isEmpty()
                ? chapter.name
                : chapter.description;
        Matcher matcher = CHAPTER_NUM.matcher(source != null ? source : "");
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    public static String yogaTitle(Chapter chapter) {
        String source = pickLabelSource(chapter);
        if (source.isEmpty()) {
            return "";
        }
        String rest = source.replaceFirst("(?i)chapter\\s+\\d+:\\s*", "").trim();
        int dash = rest.indexOf(" - ");
        if (dash > 0) {
            return rest.substring(0, dash).trim();
        }
        return rest;
    }

    public static String yogaSubtitle(Chapter chapter) {
        String source = pickLabelSource(chapter);
        if (source.isEmpty()) {
            return "";
        }
        String rest = source.replaceFirst("(?i)chapter\\s+\\d+:\\s*", "").trim();
        int dash = rest.indexOf(" - ");
        if (dash > 0 && dash + 3 < rest.length()) {
            return rest.substring(dash + 3).trim();
        }
        return "";
    }

    private static String pickLabelSource(Chapter chapter) {
        if (chapter.name != null && !chapter.name.isEmpty()) {
            return chapter.name;
        }
        return chapter.description != null ? chapter.description : "";
    }
}

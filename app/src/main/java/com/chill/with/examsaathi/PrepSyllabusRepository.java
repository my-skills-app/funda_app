package com.chill.with.examsaathi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Loads subject syllabus (categories/syllabus) from Prep backend. */
public final class PrepSyllabusRepository {

    public static final class SeriesItem {
        public long id;
        public String title = "";
        public String thumbnailUrl = "";
        public String badge = "";
        public int order;
    }

    public static final class SyllabusGroup {
        public long id;
        public String title = "";
        public int order;
        public final List<SeriesItem> series = new ArrayList<>();
    }

    public static final class Result {
        public long subjectId;
        public String subjectName = "";
        public String bannerUrl = "";
        public String thumbnailUrl = "";
        public final List<SyllabusGroup> groups = new ArrayList<>();
    }

    private PrepSyllabusRepository() {}

    @NonNull
    public static Result loadSyllabus(long subjectId) throws Exception {
        String json = PrepHttpClient.get(PrepApiConfig.homeSyllabusUrl(subjectId));
        Result out = new Result();
        JSONObject root = new JSONObject(json);
        JSONObject data = root.optJSONObject("data");
        if (data == null) {
            data = root;
        }
        JSONObject subject = data.optJSONObject("subject");
        if (subject != null) {
            out.subjectId = subject.optLong("id", subjectId);
            out.subjectName = subject.optString("name", "");
            out.bannerUrl = firstNonEmpty(
                    subject.optString("bannerUrl", ""),
                    subject.optString("thumbnailUrl", ""));
            out.thumbnailUrl = firstNonEmpty(
                    subject.optString("thumbnailUrl", ""),
                    subject.optString("bannerUrl", ""));
        } else {
            out.subjectId = subjectId;
        }
        JSONArray syllabus = data.optJSONArray("syllabus");
        if (syllabus == null) {
            return out;
        }
        List<SyllabusGroup> groups = new ArrayList<>();
        for (int i = 0; i < syllabus.length(); i++) {
            JSONObject groupObj = syllabus.optJSONObject(i);
            if (groupObj == null) {
                continue;
            }
            SyllabusGroup group = new SyllabusGroup();
            group.id = groupObj.optLong("id", 0L);
            group.title = groupObj.optString("title", groupObj.optString("name", ""));
            group.order = groupObj.optInt("order", i);
            JSONArray seriesArr = groupObj.optJSONArray("series");
            if (seriesArr != null) {
                List<SeriesItem> items = new ArrayList<>();
                for (int j = 0; j < seriesArr.length(); j++) {
                    JSONObject s = seriesArr.optJSONObject(j);
                    if (s == null) {
                        continue;
                    }
                    SeriesItem item = new SeriesItem();
                    item.id = s.optLong("id", 0L);
                    item.title = s.optString("title", s.optString("name", ""));
                    item.thumbnailUrl = firstNonEmpty(
                            s.optString("iconUrl", ""),
                            s.optString("icon", ""),
                            s.optString("thumbnailUrl", ""),
                            s.optString("thumbnail", ""),
                            s.optString("imageUrl", ""),
                            s.optString("image", ""));
                    item.order = s.optInt("order", j);
                    JSONObject badge = s.optJSONObject("badge");
                    if (badge != null) {
                        item.badge = badge.optString("name", "");
                    }
                    items.add(item);
                }
                Collections.sort(items, Comparator.comparingInt(a -> a.order));
                group.series.addAll(items);
            }
            if (!group.title.isEmpty() || !group.series.isEmpty()) {
                groups.add(group);
            }
        }
        Collections.sort(groups, Comparator.comparingInt(a -> a.order));
        out.groups.addAll(groups);
        return out;
    }

    @NonNull
    private static String firstNonEmpty(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null) {
                String trimmed = value.trim();
                if (!trimmed.isEmpty() && !"null".equalsIgnoreCase(trimmed)) {
                    return trimmed;
                }
            }
        }
        return "";
    }
}

package com.chill.with.examsaathi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Loads category-wise subjects from Prep backend /api/home/subjects. */
public final class PrepSubjectsRepository {

    public static final class Subject {
        public long id;
        public String name = "";
        public String thumbnailUrl = "";
        public String badge = "";
        public int pyqCount;
        public int order;
    }

    public static final class Section {
        public long id;
        public String name = "";
        public int order;
        public final List<Subject> subjects = new ArrayList<>();
    }

    public static final class Result {
        public String examName = "";
        public String examThumbnailUrl = "";
        public long examId;
        public final List<Section> sections = new ArrayList<>();
    }

    private PrepSubjectsRepository() {}

    @NonNull
    public static Result loadAllSubjects() throws Exception {
        String json = PrepHttpClient.get(PrepApiConfig.homeSubjectsUrl());
        Result out = new Result();
        JSONObject root = new JSONObject(json);
        JSONObject data = root.optJSONObject("data");
        if (data == null) {
            data = root;
        }
        JSONObject exam = data.optJSONObject("exam");
        if (exam != null) {
            out.examId = exam.optLong("id", 0L);
            out.examName = exam.optString("name", "");
            out.examThumbnailUrl = firstImage(exam);
        }
        JSONArray sections = data.optJSONArray("sections");
        if (sections == null) {
            return out;
        }
        List<Section> parsed = new ArrayList<>();
        for (int i = 0; i < sections.length(); i++) {
            JSONObject secObj = sections.optJSONObject(i);
            if (secObj == null) {
                continue;
            }
            Section section = new Section();
            section.id = secObj.optLong("id", 0L);
            section.name = secObj.optString("name", "");
            section.order = secObj.optInt("order", i + 1);
            JSONArray subjects = secObj.optJSONArray("subjects");
            if (subjects != null) {
                List<Subject> list = new ArrayList<>();
                for (int j = 0; j < subjects.length(); j++) {
                    JSONObject s = subjects.optJSONObject(j);
                    if (s == null) {
                        continue;
                    }
                    Subject subject = new Subject();
                    subject.id = s.optLong("id", 0L);
                    subject.name = s.optString("name", "");
                    subject.thumbnailUrl = firstNonEmpty(
                            s.optString("iconUrl", ""),
                            s.optString("icon", ""),
                            firstImage(s));
                    subject.pyqCount = s.optInt("pyqCount", 0);
                    subject.order = s.optInt("order", j + 1);
                    JSONObject badge = s.optJSONObject("badge");
                    if (badge != null) {
                        subject.badge = badge.optString("name", "");
                    }
                    if (subject.badge.isEmpty() && subject.pyqCount > 0) {
                        subject.badge = subject.pyqCount + " PYQs";
                    }
                    list.add(subject);
                }
                Collections.sort(list, Comparator.comparingInt(a -> a.order));
                section.subjects.addAll(list);
            }
            if (!section.name.isEmpty() || !section.subjects.isEmpty()) {
                parsed.add(section);
            }
        }
        Collections.sort(parsed, Comparator.comparingInt(a -> a.order));
        out.sections.addAll(parsed);
        return out;
    }

    @NonNull
    private static String firstImage(@Nullable JSONObject obj) {
        if (obj == null) {
            return "";
        }
        for (String key : new String[]{"iconUrl", "icon", "thumbnailUrl", "bannerUrl", "imageUrl", "image"}) {
            String value = obj.optString(key, "").trim();
            if (!value.isEmpty() && !"null".equalsIgnoreCase(value)) {
                return value;
            }
        }
        return "";
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

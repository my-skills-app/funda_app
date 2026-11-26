package com.chill.with.examsaathi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Prepshots exams / super-categories via local FastAPI. */
public final class PrepExamRepository {

    public static class ExamItem {
        public long id;
        public long superCategoryId;
        public String name = "";
        public String thumbnailUrl = "";
        public String meta = "";
        public boolean isCategory;
    }

    public static class SelectedCourse {
        public long examId;
        public long superCategoryId;
        public String examName = "";
        public String thumbnailUrl = "";
        public String superCategoryName = "";
    }

    private PrepExamRepository() {}

    @Nullable
    public static SelectedCourse loadSelectedCourse() throws Exception {
        String meJson = PrepHttpClient.get(PrepApiConfig.homeMeUrl());
        JSONObject root = new JSONObject(meJson);
        JSONObject data = root.optJSONObject("data");
        if (data == null) {
            data = root;
        }
        JSONObject user = data.optJSONObject("user");
        if (user == null) {
            user = data;
        }
        JSONObject exam = user.optJSONObject("exam");
        if (exam == null) {
            return loadSelectedFromMine();
        }
        SelectedCourse course = new SelectedCourse();
        course.examId = exam.optLong("id", 0L);
        course.examName = exam.optString("name", "");
        course.superCategoryId = exam.optLong("superCategoryId", 0L);
        course.thumbnailUrl = firstImage(exam);
        JSONObject superCat = user.optJSONObject("superCategory");
        if (superCat != null) {
            course.superCategoryName = superCat.optString("name", "");
            if (course.superCategoryId <= 0) {
                course.superCategoryId = superCat.optLong("id", 0L);
            }
            if (course.thumbnailUrl.isEmpty()) {
                course.thumbnailUrl = firstImage(superCat);
            }
        }
        if (course.examName.isEmpty() && course.examId <= 0) {
            return loadSelectedFromMine();
        }
        return course;
    }

    @Nullable
    private static SelectedCourse loadSelectedFromMine() throws Exception {
        String json = PrepHttpClient.get(PrepApiConfig.examsMineUrl());
        JSONObject root = new JSONObject(json);
        JSONObject data = root.optJSONObject("data");
        JSONArray exams = data != null ? data.optJSONArray("exams") : root.optJSONArray("exams");
        if (exams == null || exams.length() == 0) {
            return null;
        }
        for (int i = 0; i < exams.length(); i++) {
            JSONObject item = exams.optJSONObject(i);
            if (item == null) {
                continue;
            }
            if (item.optInt("isSelected", 0) == 1 || i == 0) {
                SelectedCourse course = new SelectedCourse();
                course.examId = item.optLong("id", 0L);
                course.examName = item.optString("name", "");
                course.thumbnailUrl = firstImage(item);
                course.superCategoryId = item.optLong("superCategoryId", 0L);
                return course;
            }
        }
        return null;
    }

    @NonNull
    public static List<ExamItem> loadSuperCategories() throws Exception {
        String json = PrepHttpClient.get(PrepApiConfig.examsListingsUrl());
        JSONObject root = new JSONObject(json);
        JSONObject data = root.optJSONObject("data");
        if (data == null) {
            data = root;
        }
        List<ExamItem> out = new ArrayList<>();
        JSONArray popular = data.optJSONArray("popularExams");
        appendCategories(out, popular, false);
        JSONArray categories = data.optJSONArray("categories");
        appendCategories(out, categories, true);
        return out;
    }

    @NonNull
    public static List<ExamItem> loadExamsForSuperCategory(long superCategoryId) throws Exception {
        String json = PrepHttpClient.get(PrepApiConfig.examsBySuperCategoryUrl(superCategoryId));
        JSONObject root = new JSONObject(json);
        Object data = root.opt("data");
        JSONArray arr;
        if (data instanceof JSONArray) {
            arr = (JSONArray) data;
        } else if (data instanceof JSONObject) {
            JSONObject obj = (JSONObject) data;
            arr = obj.optJSONArray("exams");
            if (arr == null) {
                arr = obj.optJSONArray("items");
            }
        } else {
            arr = root.optJSONArray("exams");
        }
        List<ExamItem> out = new ArrayList<>();
        if (arr == null) {
            return out;
        }
        for (int i = 0; i < arr.length(); i++) {
            JSONObject item = arr.optJSONObject(i);
            if (item == null) {
                continue;
            }
            ExamItem row = new ExamItem();
            row.id = item.optLong("id", 0L);
            row.superCategoryId = item.optLong("superCategoryId", superCategoryId);
            row.name = item.optString("name", "");
            row.thumbnailUrl = firstImage(item);
            row.isCategory = false;
            row.meta = row.superCategoryId > 0 ? ("#" + row.id) : "";
            out.add(row);
        }
        return out;
    }

    public static void selectExam(long superCategoryId, long examId) throws Exception {
        JSONObject body = new JSONObject();
        body.put("superCategoryId", superCategoryId);
        body.put("examId", examId);
        PrepHttpClient.postJson(PrepApiConfig.examsSelectUrl(), body.toString());
    }

    private static void appendCategories(List<ExamItem> out, @Nullable JSONArray arr, boolean asCategory) {
        if (arr == null) {
            return;
        }
        for (int i = 0; i < arr.length(); i++) {
            JSONObject item = arr.optJSONObject(i);
            if (item == null) {
                continue;
            }
            ExamItem row = new ExamItem();
            row.id = item.optLong("id", 0L);
            row.superCategoryId = item.optLong("superCategoryId", row.id);
            row.name = firstNonEmpty(
                    item.optString("name", ""),
                    item.optString("examName", ""),
                    item.optString("title", ""));
            row.thumbnailUrl = firstImage(item);
            row.isCategory = asCategory || item.has("examCount");
            int examCount = item.optInt("examCount", -1);
            if (examCount >= 0) {
                row.meta = examCount + " exams";
            } else {
                row.meta = item.optString("description", "");
            }
            if (!row.name.isEmpty()) {
                out.add(row);
            }
        }
    }

    private static String firstImage(JSONObject item) {
        String[] keys = {
                "thumbnailUrl", "bannerUrl", "thumbnail", "imageUrl", "image", "iconUrl"
        };
        for (String key : keys) {
            String value = item.optString(key, "").trim();
            if (!value.isEmpty()) {
                return value;
            }
        }
        return "";
    }

    private static String firstNonEmpty(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }
}

package com.chill.with.examsaathi;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Local enrolled exams/courses (supports multiple enrollments). */
public final class PrepCourseStore {

    private static final String PREFS = "prep_course_store";
    private static final String KEY_COURSES = "enrolled_courses_json";
    private static final String KEY_ACTIVE_ID = "active_exam_id";
    private static final String KEY_ONBOARDING_DONE = "course_onboarding_done";

    private PrepCourseStore() {}

    @NonNull
    private static SharedPreferences prefs(@NonNull Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean hasEnrolledCourses(@NonNull Context context) {
        return !getEnrolled(context).isEmpty();
    }

    public static boolean isOnboardingDone(@NonNull Context context) {
        return prefs(context).getBoolean(KEY_ONBOARDING_DONE, false) && hasEnrolledCourses(context);
    }

    public static void setOnboardingDone(@NonNull Context context, boolean done) {
        prefs(context).edit().putBoolean(KEY_ONBOARDING_DONE, done).apply();
    }

    @NonNull
    public static List<PrepExamRepository.SelectedCourse> getEnrolled(@NonNull Context context) {
        List<PrepExamRepository.SelectedCourse> out = new ArrayList<>();
        String raw = prefs(context).getString(KEY_COURSES, "[]");
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) {
                    continue;
                }
                PrepExamRepository.SelectedCourse c = fromJson(o);
                if (c.examId > 0) {
                    out.add(c);
                }
            }
        } catch (Exception ignored) {
            // empty
        }
        return out;
    }

    public static void saveEnrolled(
            @NonNull Context context,
            @NonNull List<PrepExamRepository.SelectedCourse> courses,
            long activeExamId) {
        Map<Long, PrepExamRepository.SelectedCourse> unique = new LinkedHashMap<>();
        for (PrepExamRepository.SelectedCourse c : courses) {
            if (c != null && c.examId > 0) {
                unique.put(c.examId, c);
            }
        }
        JSONArray arr = new JSONArray();
        for (PrepExamRepository.SelectedCourse c : unique.values()) {
            arr.put(toJson(c));
        }
        long active = activeExamId;
        if (active <= 0 && !unique.isEmpty()) {
            active = unique.values().iterator().next().examId;
        }
        if (!unique.containsKey(active) && !unique.isEmpty()) {
            active = unique.values().iterator().next().examId;
        }
        prefs(context)
                .edit()
                .putString(KEY_COURSES, arr.toString())
                .putLong(KEY_ACTIVE_ID, active)
                .putBoolean(KEY_ONBOARDING_DONE, !unique.isEmpty())
                .apply();
    }

    public static void addOrUpdate(
            @NonNull Context context, @NonNull PrepExamRepository.SelectedCourse course, boolean makeActive) {
        List<PrepExamRepository.SelectedCourse> list = getEnrolled(context);
        boolean found = false;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).examId == course.examId) {
                list.set(i, course);
                found = true;
                break;
            }
        }
        if (!found) {
            list.add(course);
        }
        long active = makeActive ? course.examId : getActiveExamId(context);
        saveEnrolled(context, list, active);
    }

    public static long getActiveExamId(@NonNull Context context) {
        return prefs(context).getLong(KEY_ACTIVE_ID, 0L);
    }

    @Nullable
    public static PrepExamRepository.SelectedCourse getActive(@NonNull Context context) {
        long activeId = getActiveExamId(context);
        List<PrepExamRepository.SelectedCourse> list = getEnrolled(context);
        for (PrepExamRepository.SelectedCourse c : list) {
            if (c.examId == activeId) {
                return c;
            }
        }
        return list.isEmpty() ? null : list.get(0);
    }

    public static void setActive(@NonNull Context context, long examId) {
        prefs(context).edit().putLong(KEY_ACTIVE_ID, examId).apply();
    }

    @NonNull
    private static JSONObject toJson(@NonNull PrepExamRepository.SelectedCourse c) {
        JSONObject o = new JSONObject();
        try {
            o.put("examId", c.examId);
            o.put("superCategoryId", c.superCategoryId);
            o.put("examName", c.examName != null ? c.examName : "");
            o.put("thumbnailUrl", c.thumbnailUrl != null ? c.thumbnailUrl : "");
            o.put("superCategoryName", c.superCategoryName != null ? c.superCategoryName : "");
        } catch (Exception ignored) {
            // ignore
        }
        return o;
    }

    @NonNull
    private static PrepExamRepository.SelectedCourse fromJson(@NonNull JSONObject o) {
        PrepExamRepository.SelectedCourse c = new PrepExamRepository.SelectedCourse();
        c.examId = o.optLong("examId", 0L);
        c.superCategoryId = o.optLong("superCategoryId", 0L);
        c.examName = o.optString("examName", "");
        c.thumbnailUrl = o.optString("thumbnailUrl", "");
        c.superCategoryName = o.optString("superCategoryName", "");
        return c;
    }
}

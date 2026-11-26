package com.chill.with.examsaathi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Loads series/chapter details from Prep backend. */
public final class PrepSeriesRepository {

    public static final class Tag {
        public String displayName = "";
        public String type = "";
        public String selectedIcon = "";
        public String unselectedIcon = "";
    }

    public static final class Example {
        public long id;
        public int order;
        public String label = "";
        public String title = "";
        public String thumbnailUrl = "";
        public String videoUrl = "";
        public String mp4Url = "";
        public String m3u8Url = "";
    }

    public static final class Topic {
        public long id;
        public int order;
        public String title = "";
        public String thumbnailUrl = "";
        public String videoUrl = "";
        public String mp4Url = "";
        public String m3u8Url = "";
        public int durationSec;
        public int pyqCount;
        public int examplesCount;
        public String badge = "";
        public boolean isPublic;
        public boolean isAudio;
        public int watchedDuration;
        public int viewedPercentage;
        public boolean isCompleted;
        public long quizId;
        public int quizQuestionCount;
        public final List<Example> examples = new ArrayList<>();
    }

    public static final class Result {
        public long seriesId;
        public String seriesTitle = "";
        public String bannerUrl = "";
        public String mindMapUrl = "";
        public String btnText = "";
        public boolean isBookmarked;
        public boolean containsAudio;
        public int completedTopics;
        public int totalTopics;
        public int progressPercent;
        public String sectionTitle = "";
        public final List<String> contentChips = new ArrayList<>();
        public final List<Tag> tags = new ArrayList<>();
        public final List<Topic> topics = new ArrayList<>();
    }

    private PrepSeriesRepository() {}

    @NonNull
    public static Result loadDetails(long seriesId, @NonNull String contentType) throws Exception {
        String json = PrepHttpClient.get(PrepApiConfig.seriesDetailsUrl(seriesId, contentType));
        Result out = new Result();
        JSONObject root = new JSONObject(json);
        JSONObject data = root.optJSONObject("data");
        if (data == null) {
            data = root;
        }

        JSONArray tags = data.optJSONArray("tags");
        if (tags != null) {
            for (int i = 0; i < tags.length(); i++) {
                JSONObject t = tags.optJSONObject(i);
                if (t == null) {
                    continue;
                }
                Tag tag = new Tag();
                tag.displayName = t.optString("displayName", "");
                tag.type = t.optString("type", "");
                tag.selectedIcon = t.optString("selectedIcon", "");
                tag.unselectedIcon = t.optString("unSelectedIcon", "");
                out.tags.add(tag);
            }
        }

        JSONObject series = data.optJSONObject("seriesData");
        if (series == null) {
            return out;
        }
        out.seriesId = series.optLong("seriesId", seriesId);
        out.seriesTitle = series.optString("seriesTitle", "");
        out.bannerUrl = series.optString("bannerUrl", "");
        out.mindMapUrl = series.optString("mindMap", "");
        out.btnText = series.optString("btnText", "");
        out.isBookmarked = series.optBoolean("isBookMarked", false);
        out.containsAudio = series.optInt("containsAudioContent", 0) == 1
                || series.optBoolean("containsAudioContent", false);

        JSONObject progress = series.optJSONObject("progress");
        if (progress != null) {
            out.completedTopics = progress.optInt("completedTopics", 0);
            out.totalTopics = progress.optInt("totalTopics", 0);
            out.progressPercent = progress.optInt("percentage", 0);
        }

        JSONObject sections = series.optJSONObject("sections");
        if (sections != null) {
            out.sectionTitle = sections.optString("title", "Theoretical Topics");
            if (out.totalTopics <= 0) {
                out.totalTopics = sections.optInt("totalTopics", 0);
            }
        }
        if (out.sectionTitle.isEmpty()) {
            out.sectionTitle = "Theoretical Topics";
        }

        JSONArray chips = series.optJSONArray("contentChips");
        if (chips != null) {
            for (int i = 0; i < chips.length(); i++) {
                String chip = chips.optString(i, "").trim();
                if (!chip.isEmpty()) {
                    out.contentChips.add(chip);
                }
            }
        }

        JSONArray videos = series.optJSONArray("videos");
        if (videos != null) {
            List<Topic> list = new ArrayList<>();
            for (int i = 0; i < videos.length(); i++) {
                JSONObject v = videos.optJSONObject(i);
                if (v == null) {
                    continue;
                }
                Topic topic = new Topic();
                topic.id = v.optLong("id", 0L);
                topic.order = v.optInt("order", i);
                topic.title = firstNonEmpty(v.optString("videoTitle", ""), v.optString("title", ""));
                topic.thumbnailUrl = v.optString("thumbnail", "");
                topic.videoUrl = v.optString("videoUrl", "");
                topic.mp4Url = v.optString("mp4Url", "");
                topic.m3u8Url = v.optString("m3u8Url", "");
                topic.durationSec = v.optInt("videoDuration", 0);
                topic.pyqCount = v.optInt("pyqCount", 0);
                topic.examplesCount = v.optInt("examplesCount", 0);
                topic.isPublic = v.optInt("isPublic", 0) == 1 || v.optBoolean("isPublic", false);
                topic.isAudio = v.optInt("isAudioContent", 0) == 1 || v.optBoolean("isAudioContent", false);
                Object badgeObj = v.opt("badge");
                if (badgeObj instanceof JSONObject) {
                    topic.badge = ((JSONObject) badgeObj).optString("name", "");
                } else if (badgeObj instanceof String) {
                    topic.badge = ((String) badgeObj).trim();
                }
                if (topic.badge.isEmpty()) {
                    Object badgeV2 = v.opt("badgeV2");
                    if (badgeV2 instanceof JSONObject) {
                        topic.badge = ((JSONObject) badgeV2).optString("name", "");
                    } else if (badgeV2 instanceof String) {
                        topic.badge = ((String) badgeV2).trim();
                    }
                }
                if (topic.badge.isEmpty() && topic.pyqCount > 0) {
                    topic.badge = topic.pyqCount + (topic.pyqCount == 1 ? " PYQ" : " PYQs");
                }
                JSONObject quiz = v.optJSONObject("quiz");
                if (quiz != null) {
                    topic.quizId = quiz.optLong("quizId", 0L);
                    topic.quizQuestionCount = quiz.optInt("totalQuestions", 0);
                }
                JSONArray examples = v.optJSONArray("examples");
                if (examples != null) {
                    for (int e = 0; e < examples.length(); e++) {
                        JSONObject ex = examples.optJSONObject(e);
                        if (ex == null) {
                            continue;
                        }
                        Example example = new Example();
                        example.id = ex.optLong("id", 0L);
                        example.order = ex.optInt("order", e + 1);
                        example.label = firstNonEmpty(ex.optString("label", ""), "Example " + (e + 1));
                        example.title = firstNonEmpty(ex.optString("videoTitle", ""), ex.optString("title", ""));
                        example.thumbnailUrl = ex.optString("thumbnail", "");
                        example.videoUrl = ex.optString("videoUrl", "");
                        example.mp4Url = ex.optString("mp4Url", "");
                        example.m3u8Url = ex.optString("m3u8Url", "");
                        topic.examples.add(example);
                    }
                    Collections.sort(topic.examples, Comparator.comparingInt(a -> a.order));
                }
                if (topic.examplesCount <= 0) {
                    topic.examplesCount = topic.examples.size();
                }
                JSONObject p = v.optJSONObject("progress");
                if (p != null) {
                    topic.watchedDuration = p.optInt("watchedDuration", 0);
                    topic.viewedPercentage = p.optInt("viewedPercentage", 0);
                    topic.isCompleted = p.optBoolean("isCompleted", false);
                }
                list.add(topic);
            }
            Collections.sort(list, Comparator.comparingInt(a -> a.order));
            out.topics.addAll(list);
            if (out.totalTopics <= 0) {
                out.totalTopics = out.topics.size();
            }
        }
        return out;
    }

    @NonNull
    public static String playUrl(@Nullable Topic topic) {
        if (topic == null) {
            return "";
        }
        return firstNonEmpty(topic.m3u8Url, topic.mp4Url, topic.videoUrl);
    }

    /** Prefer progressive MP4 for DownloadManager (HLS/m3u8 is not a single file download). */
    @NonNull
    public static String downloadUrl(@Nullable Topic topic) {
        if (topic == null) {
            return "";
        }
        String mp4 = firstNonEmpty(topic.mp4Url);
        if (!mp4.isEmpty()) {
            return mp4;
        }
        String video = firstNonEmpty(topic.videoUrl);
        if (!video.isEmpty() && video.toLowerCase().contains(".mp4")) {
            return video;
        }
        return firstNonEmpty(topic.videoUrl, topic.mp4Url);
    }

    @NonNull
    public static String playUrl(@Nullable Example example) {
        if (example == null) {
            return "";
        }
        return firstNonEmpty(example.m3u8Url, example.mp4Url, example.videoUrl);
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

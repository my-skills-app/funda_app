package com.chill.with.examsaathi;

/** Prepshots FastAPI base URL (AWS + Cloudflare). */
public final class PrepApiConfig {

    /** Live backend: AWS EC2 via Cloudflare. */
    public static final String BASE_URL = "https://shorts.teachmania.site";

    private PrepApiConfig() {}

    public static String homePageUrl(int limit, int offset) {
        return BASE_URL + "/api/home/page?limit=" + limit + "&offset=" + offset;
    }

    public static String homeMeUrl() {
        return BASE_URL + "/api/home/me";
    }

    public static String homeTabsUrl() {
        return BASE_URL + "/api/home/tabs";
    }

    public static String homeSubjectsUrl() {
        return BASE_URL + "/api/home/subjects";
    }

    public static String homeCategoriesUrl() {
        return BASE_URL + "/api/home/categories";
    }

    public static String homeFeedUrl(long seriesId, Long videoId) {
        String url = BASE_URL + "/api/home/feed?source=series&series_id=" + seriesId
                + "&limit=10&offset=0";
        if (videoId != null && videoId > 0) {
            url += "&video_id=" + videoId;
        }
        return url;
    }

    public static String homeSyllabusUrl(long subjectId) {
        // Upstream Prepshots caps syllabus limit at 20
        return BASE_URL + "/api/home/syllabus?subject_id=" + subjectId + "&limit=20&offset=0";
    }

    /** Series/chapter details. contentType must be video or audio. */
    public static String seriesDetailsUrl(long seriesId, String contentType) {
        String type = contentType == null || contentType.isEmpty() ? "video" : contentType;
        return BASE_URL + "/api/series/" + seriesId + "/details?content_type=" + type;
    }

    public static String healthUrl() {
        return BASE_URL + "/health";
    }

    public static String examsListingsUrl() {
        return BASE_URL + "/api/exams/listings";
    }

    public static String examsMineUrl() {
        return BASE_URL + "/api/exams/mine";
    }

    public static String examsBySuperCategoryUrl(long superCategoryId) {
        return BASE_URL + "/api/exams/by-super-category/" + superCategoryId;
    }

    public static String examsSelectUrl() {
        return BASE_URL + "/api/exams/select";
    }
}

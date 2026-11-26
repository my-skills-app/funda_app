package com.chill.with.examsaathi;

/** API endpoints (resolved at runtime; not plain strings in DEX). */
public final class ApiConfig {

    private ApiConfig() {}

    public static String baseUrl() {
        return StringVault.baseUrl();
    }

    public static String dashboardUrl() {
        return StringVault.baseUrl() + StringVault.dashboardPath();
    }

    public static String chaptersUrl() {
        return StringVault.baseUrl() + StringVault.chaptersPath();
    }

    public static String downloadTrackUrl() {
        return StringVault.baseUrl() + StringVault.trackPath();
    }

    public static String shlokasUrl(int chapterId) {
        return StringVault.baseUrl() + StringVault.chapterShlokasPrefix() + chapterId + "/shlokas";
    }

    public static String shlokaByTitleUrl(String encodedTitle) {
        return StringVault.baseUrl() + StringVault.shlokQueryPrefix() + encodedTitle;
    }
}

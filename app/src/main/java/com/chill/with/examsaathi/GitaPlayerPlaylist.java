package com.chill.with.examsaathi;

import android.content.Intent;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/** Builds a Gita chapter playback queue for ExoPlayer. */
public final class GitaPlayerPlaylist {

    private GitaPlayerPlaylist() {}

    public static String toJson(List<GitaChapterShlokas.Shloka> shlokas) {
        if (shlokas == null || shlokas.isEmpty()) {
            return "[]";
        }
        return new Gson().toJson(shlokas);
    }

    public static void attachToPlayerIntent(Intent source, Intent playerIntent) {
        if (source == null || playerIntent == null) {
            return;
        }
        String json = source.getStringExtra(MovieviewActivity.EXTRA_CHAPTER_SHLOKAS_JSON);
        if (json == null || json.isEmpty()) {
            return;
        }
        playerIntent.putExtra(ExoPlayerActivity.EXTRA_CHAPTER_SHLOKAS_JSON, json);
        String chapterTitle = source.getStringExtra(MovieviewActivity.EXTRA_CHAPTER_TITLE);
        playerIntent.putExtra(ExoPlayerActivity.EXTRA_CHAPTER_TITLE,
                chapterTitle != null ? chapterTitle : "");
        String shlokaId = source.getStringExtra("videoid");
        playerIntent.putExtra(ExoPlayerActivity.EXTRA_CURRENT_SHLOKA_ID,
                shlokaId != null ? shlokaId : "");
    }

    public static List<GitaChapterShlokas.Shloka> parsePlayable(String json) {
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            Type type = new TypeToken<List<GitaChapterShlokas.Shloka>>() {}.getType();
            List<GitaChapterShlokas.Shloka> all = new Gson().fromJson(json, type);
            List<GitaChapterShlokas.Shloka> playable = new ArrayList<>();
            if (all == null) {
                return playable;
            }
            for (GitaChapterShlokas.Shloka shloka : all) {
                if (shloka != null && hasVideo(shloka)) {
                    playable.add(shloka);
                }
            }
            return playable;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public static int findCurrentIndex(List<GitaChapterShlokas.Shloka> playlist,
                                       String shlokaId, String title) {
        if (playlist == null || playlist.isEmpty()) {
            return -1;
        }
        if (shlokaId != null && !shlokaId.isEmpty()) {
            for (int i = 0; i < playlist.size(); i++) {
                GitaChapterShlokas.Shloka item = playlist.get(i);
                if (item.id != null && item.id.equals(shlokaId)) {
                    return i;
                }
            }
        }
        if (title != null && !title.trim().isEmpty()) {
            String key = title.trim();
            for (int i = 0; i < playlist.size(); i++) {
                GitaChapterShlokas.Shloka item = playlist.get(i);
                if (item.title != null && item.title.trim().equalsIgnoreCase(key)) {
                    return i;
                }
            }
        }
        return 0;
    }

    private static boolean hasVideo(GitaChapterShlokas.Shloka shloka) {
        return shloka.videoUrl != null && !shloka.videoUrl.trim().isEmpty();
    }
}

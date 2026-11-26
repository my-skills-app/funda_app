package com.chill.with.examsaathi;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Loads playable shlokas one chapter at a time for full Gita audio playback.
 */
public final class GitaFullAudioQueue {

    public static final class ChapterPlaylist {
        /** API chapter id (for loading shlokas). */
        public int chapterId;
        /** Display number 1–18. */
        public int chapterNumber;
        public String chapterTitle;
        public List<GitaChapterShlokas.Shloka> shlokas;
    }

    public interface ChapterListCallback {
        void onLoaded(List<GitaChapters.Chapter> chapters);

        void onError();
    }

    public interface ChapterPlaylistCallback {
        void onLoaded(ChapterPlaylist chapterPlaylist);

        void onError();
    }

    public interface FirstChapterCallback {
        void onLoaded(ChapterPlaylist chapterPlaylist);

        void onError();
    }

    public interface NextChapterCallback {
        void onLoaded(ChapterPlaylist chapterPlaylist);

        void onNoMoreChapters();
    }

    public interface PreviousChapterCallback {
        void onLoaded(ChapterPlaylist chapterPlaylist);

        void onNoPreviousChapter();
    }

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private GitaFullAudioQueue() {
    }

    /** Loads only the first chapter that has playable shlokas. */
    public static void loadFirstChapter(FirstChapterCallback callback) {
        new Thread(() -> {
            try {
                ChapterPlaylist playlist = fetchFirstPlayableChapter();
                MAIN.post(() -> {
                    if (callback == null) {
                        return;
                    }
                    if (playlist != null) {
                        callback.onLoaded(playlist);
                    } else {
                        callback.onError();
                    }
                });
            } catch (Exception e) {
                MAIN.post(() -> {
                    if (callback != null) {
                        callback.onError();
                    }
                });
            }
        }).start();
    }

    /** All chapters sorted for the chapter picker. */
    public static void loadAllChapters(ChapterListCallback callback) {
        new Thread(() -> {
            try {
                List<GitaChapters.Chapter> chapters = loadSortedChapters();
                MAIN.post(() -> {
                    if (callback != null) {
                        callback.onLoaded(chapters);
                    }
                });
            } catch (Exception e) {
                MAIN.post(() -> {
                    if (callback != null) {
                        callback.onError();
                    }
                });
            }
        }).start();
    }

    /** Loads one chapter's playable shlokas (for jump-to-chapter). */
    public static void loadChapter(int apiChapterId, ChapterPlaylistCallback callback) {
        new Thread(() -> {
            try {
                GitaChapters.Chapter target = null;
                for (GitaChapters.Chapter chapter : loadSortedChapters()) {
                    if (chapter.chapterId == apiChapterId) {
                        target = chapter;
                        break;
                    }
                }
                if (target == null) {
                    postChapterError(callback);
                    return;
                }
                List<GitaChapterShlokas.Shloka> shlokas = fetchPlayableShlokasForChapter(apiChapterId);
                if (shlokas.isEmpty()) {
                    postChapterError(callback);
                    return;
                }
                ChapterPlaylist playlist = buildChapterPlaylist(target, shlokas);
                MAIN.post(() -> {
                    if (callback != null) {
                        callback.onLoaded(playlist);
                    }
                });
            } catch (Exception e) {
                postChapterError(callback);
            }
        }).start();
    }

    /** Loads one chapter's playable shlokas by its display chapter number. */
    public static void loadChapterByNumber(int chapterNumber, ChapterPlaylistCallback callback) {
        new Thread(() -> {
            try {
                GitaChapters.Chapter target = null;
                for (GitaChapters.Chapter chapter : loadSortedChapters()) {
                    if (GitaChapters.chapterNumber(chapter) == chapterNumber) {
                        target = chapter;
                        break;
                    }
                }
                if (target == null) {
                    postChapterError(callback);
                    return;
                }
                List<GitaChapterShlokas.Shloka> shlokas = fetchPlayableShlokasForChapter(target.chapterId);
                if (shlokas.isEmpty()) {
                    postChapterError(callback);
                    return;
                }
                ChapterPlaylist playlist = buildChapterPlaylist(target, shlokas);
                MAIN.post(() -> {
                    if (callback != null) {
                        callback.onLoaded(playlist);
                    }
                });
            } catch (Exception e) {
                postChapterError(callback);
            }
        }).start();
    }

    private static void postChapterError(@Nullable ChapterPlaylistCallback callback) {
        MAIN.post(() -> {
            if (callback != null) {
                callback.onError();
            }
        });
    }

    /**
     * Loads the previous playable chapter before {@code beforeChapterId} in chapter
     * order.
     */
    public static void loadPreviousChapterBefore(int beforeChapterId, PreviousChapterCallback callback) {
        new Thread(() -> {
            try {
                ChapterPlaylist playlist = fetchPreviousPlayableChapter(beforeChapterId);
                MAIN.post(() -> {
                    if (callback == null) {
                        return;
                    }
                    if (playlist != null) {
                        callback.onLoaded(playlist);
                    } else {
                        callback.onNoPreviousChapter();
                    }
                });
            } catch (Exception e) {
                MAIN.post(() -> {
                    if (callback != null) {
                        callback.onNoPreviousChapter();
                    }
                });
            }
        }).start();
    }

    /**
     * Loads the next chapter after {@code afterChapterId}, skipping empty chapters.
     */
    public static void loadNextChapterAfter(int afterChapterId, NextChapterCallback callback) {
        new Thread(() -> {
            try {
                ChapterPlaylist playlist = fetchNextPlayableChapter(afterChapterId);
                MAIN.post(() -> {
                    if (callback == null) {
                        return;
                    }
                    if (playlist != null) {
                        callback.onLoaded(playlist);
                    } else {
                        callback.onNoMoreChapters();
                    }
                });
            } catch (Exception e) {
                MAIN.post(() -> {
                    if (callback != null) {
                        callback.onNoMoreChapters();
                    }
                });
            }
        }).start();
    }

    private static ChapterPlaylist fetchFirstPlayableChapter() throws Exception {
        List<GitaChapters.Chapter> chapters = loadSortedChapters();
        for (GitaChapters.Chapter chapter : chapters) {
            if (chapter.chapterId <= 0) {
                continue;
            }
            List<GitaChapterShlokas.Shloka> shlokas = fetchPlayableShlokasForChapter(chapter.chapterId);
            if (!shlokas.isEmpty()) {
                return buildChapterPlaylist(chapter, shlokas);
            }
        }
        return null;
    }

    private static ChapterPlaylist fetchNextPlayableChapter(int afterChapterId) throws Exception {
        List<GitaChapters.Chapter> chapters = loadSortedChapters();
        for (GitaChapters.Chapter chapter : chapters) {
            if (chapter.chapterId <= afterChapterId) {
                continue;
            }
            List<GitaChapterShlokas.Shloka> shlokas = fetchPlayableShlokasForChapter(chapter.chapterId);
            if (!shlokas.isEmpty()) {
                return buildChapterPlaylist(chapter, shlokas);
            }
        }
        return null;
    }

    @Nullable
    private static ChapterPlaylist fetchPreviousPlayableChapter(int beforeChapterId) throws Exception {
        List<GitaChapters.Chapter> chapters = loadSortedChapters();
        int currentIndex = -1;
        for (int i = 0; i < chapters.size(); i++) {
            if (chapters.get(i).chapterId == beforeChapterId) {
                currentIndex = i;
                break;
            }
        }
        if (currentIndex < 0) {
            for (int i = chapters.size() - 1; i >= 0; i--) {
                if (chapters.get(i).chapterId < beforeChapterId) {
                    currentIndex = i;
                    break;
                }
            }
        }
        if (currentIndex <= 0) {
            return null;
        }
        for (int i = currentIndex - 1; i >= 0; i--) {
            GitaChapters.Chapter chapter = chapters.get(i);
            List<GitaChapterShlokas.Shloka> shlokas = fetchPlayableShlokasForChapter(chapter.chapterId);
            if (!shlokas.isEmpty()) {
                return buildChapterPlaylist(chapter, shlokas);
            }
        }
        return null;
    }

    private static List<GitaChapters.Chapter> loadSortedChapters() throws Exception {
        android.content.Context ctx = MyApp.getContext();
        if (ctx != null) {
            String cached = GitaAudioDataCache.loadChaptersJson(ctx);
            if (cached != null) {
                return sortChapters(GitaChapters.parse(cached));
            }
        }
        String json = fetchUrl(ctx, GitaChapters.apiUrl());
        if (ctx != null) {
            GitaAudioDataCache.saveChaptersJson(ctx, json);
        }
        return sortChapters(GitaChapters.parse(json));
    }

    private static List<GitaChapters.Chapter> sortChapters(List<GitaChapters.Chapter> chapters) {
        Collections.sort(chapters, new Comparator<GitaChapters.Chapter>() {
            @Override
            public int compare(GitaChapters.Chapter a, GitaChapters.Chapter b) {
                return Integer.compare(a.chapterId, b.chapterId);
            }
        });
        return chapters;
    }

    private static ChapterPlaylist buildChapterPlaylist(GitaChapters.Chapter chapter,
            List<GitaChapterShlokas.Shloka> shlokas) {
        ChapterPlaylist result = new ChapterPlaylist();
        result.chapterId = chapter.chapterId;
        result.chapterNumber = GitaChapters.chapterNumber(chapter);
        result.chapterTitle = chapterTitle(chapter);
        result.shlokas = shlokas;
        return result;
    }

    private static String chapterTitle(GitaChapters.Chapter chapter) {
        if (chapter == null) {
            return "";
        }
        int number = GitaChapters.chapterNumber(chapter);
        String yoga = GitaChapters.yogaTitle(chapter);
        if (number > 0 && yoga != null && !yoga.isEmpty()) {
            return "Chapter " + number + " – " + yoga;
        }
        if (chapter.name != null && !chapter.name.trim().isEmpty()) {
            return chapter.name.trim();
        }
        if (number > 0) {
            return "Chapter " + number;
        }
        return "Chapter";
    }

    private static List<GitaChapterShlokas.Shloka> fetchPlayableShlokasForChapter(int chapterId)
            throws Exception {
        android.content.Context ctx = MyApp.getContext();
        String json = null;
        if (ctx != null) {
            json = GitaAudioDataCache.loadShlokasJson(ctx, chapterId);
        }
        if (json == null) {
            json = fetchUrl(ctx, GitaChapterShlokas.shlokasUrl(chapterId));
            if (ctx != null) {
                GitaAudioDataCache.saveShlokasJson(ctx, chapterId, json);
            }
        }
        List<GitaChapterShlokas.Shloka> shlokas = GitaChapterShlokas.parse(json);
        List<GitaChapterShlokas.Shloka> playable = new ArrayList<>();
        for (GitaChapterShlokas.Shloka shloka : shlokas) {
            if (hasPlayableUrl(shloka)) {
                playable.add(shloka);
            }
        }
        Collections.sort(playable, new Comparator<GitaChapterShlokas.Shloka>() {
            @Override
            public int compare(GitaChapterShlokas.Shloka a, GitaChapterShlokas.Shloka b) {
                return Integer.compare(a.sequence, b.sequence);
            }
        });
        return playable;
    }

    private static boolean hasPlayableUrl(GitaChapterShlokas.Shloka shloka) {
        return shloka != null
                && shloka.videoUrl != null
                && !shloka.videoUrl.trim().isEmpty();
    }

    private static String fetchUrl(@Nullable android.content.Context context, String urlString)
            throws Exception {
        android.content.Context app = context != null ? context.getApplicationContext() : MyApp.getContext();
        if (app == null) {
            throw new IllegalStateException("App context unavailable");
        }
        return SecureHttpClient.get(app, urlString);
    }
}

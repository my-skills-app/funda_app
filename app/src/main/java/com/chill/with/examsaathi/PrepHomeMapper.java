package com.chill.with.examsaathi;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Maps Prepshots home JSON into existing {@link GitaDashboard.Data} for UI reuse. */
public final class PrepHomeMapper {

    private PrepHomeMapper() {}

    public static GitaDashboard.Data mapHomeCards(@Nullable JSONArray cards) {
        GitaDashboard.Data data = new GitaDashboard.Data();
        if (cards == null || cards.length() == 0) {
            return data;
        }

        List<JSONObject> sorted = new ArrayList<>();
        for (int i = 0; i < cards.length(); i++) {
            JSONObject card = cards.optJSONObject(i);
            if (card != null) {
                sorted.add(card);
            }
        }
        Collections.sort(sorted, Comparator.comparingInt(PrepHomeMapper::cardOrder));

        for (JSONObject card : sorted) {
            String cardName = card.optString("cardName", "");
            String designType = card.optString("designType", "");
            boolean isHero = "hero".equalsIgnoreCase(cardName) || "HERO".equalsIgnoreCase(designType);
            if (isHero) {
                appendHeroBanners(data, contentArray(card));
                continue;
            }
            GitaDashboard.Section section = mapSection(card);
            if (section != null && (!section.shlokas.isEmpty() || !section.sectionTitle.isEmpty())) {
                data.sections.add(section);
            }
        }
        return data;
    }

    public static void mergeUniqueCards(JSONArray destination, JSONArray pageCards, Set<String> seen) {
        if (pageCards == null) {
            return;
        }
        for (int i = 0; i < pageCards.length(); i++) {
            JSONObject card = pageCards.optJSONObject(i);
            if (card == null) {
                continue;
            }
            String key = card.optString("id", "");
            if (key.isEmpty()) {
                key = card.optString("cardName", "") + "|" + card.optString("order", String.valueOf(i));
            }
            if (seen.contains(key)) {
                continue;
            }
            seen.add(key);
            destination.put(card);
        }
    }

    @Nullable
    public static String displayNameFromMe(@Nullable String meJson) {
        if (meJson == null || meJson.isEmpty()) {
            return null;
        }
        try {
            JSONObject root = new JSONObject(meJson);
            JSONObject data = unwrapObject(root);
            if (data == null) {
                return null;
            }
            JSONObject user = data.optJSONObject("user");
            if (user == null) {
                user = data;
            }
            String name = firstNonEmpty(
                    user.optString("name", ""),
                    user.optString("displayName", ""),
                    user.optString("fullName", ""),
                    user.optString("firstName", ""));
            return name.isEmpty() ? null : name;
        } catch (Exception ignored) {
            return null;
        }
    }

    @Nullable
    public static JSONArray unwrapArray(@Nullable String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            Object value = new JSONObject(json);
            Object data = ((JSONObject) value).opt("data");
            if (data instanceof JSONArray) {
                return (JSONArray) data;
            }
            if (data instanceof JSONObject) {
                JSONObject obj = (JSONObject) data;
                for (String key : new String[]{"content", "items", "list", "data", "cards"}) {
                    JSONArray arr = obj.optJSONArray(key);
                    if (arr != null) {
                        return arr;
                    }
                }
            }
            JSONArray direct = ((JSONObject) value).optJSONArray("content");
            return direct;
        } catch (Exception e) {
            try {
                return new JSONArray(json);
            } catch (Exception ignored) {
                return null;
            }
        }
    }

    @Nullable
    public static JSONObject unwrapObject(@Nullable JSONObject root) {
        if (root == null) {
            return null;
        }
        Object data = root.opt("data");
        if (data instanceof JSONObject) {
            return (JSONObject) data;
        }
        return root;
    }

    @Nullable
    public static PlayableVideo firstPlayableFromFeed(@Nullable String feedJson) {
        if (feedJson == null || feedJson.isEmpty()) {
            return null;
        }
        try {
            JSONObject root = new JSONObject(feedJson);
            JSONObject data = unwrapObject(root);
            if (data == null) {
                return null;
            }
            JSONArray content = data.optJSONArray("content");
            if (content == null) {
                content = data.optJSONArray("items");
            }
            if (content == null) {
                content = data.optJSONArray("list");
            }
            if (content == null) {
                return null;
            }
            for (int i = 0; i < content.length(); i++) {
                JSONObject item = content.optJSONObject(i);
                if (item == null) {
                    continue;
                }
                String url = playUrlOf(item);
                if (url.isEmpty()) {
                    continue;
                }
                PlayableVideo video = new PlayableVideo();
                video.url = url;
                video.title = titleOf(item);
                video.image = imageOf(item);
                video.videoId = String.valueOf(item.opt("id"));
                return video;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public static class PlayableVideo {
        public String url = "";
        public String title = "";
        public String image = "";
        public String videoId = "";
    }

    private static GitaDashboard.Section mapSection(JSONObject card) {
        JSONObject body = card.optJSONObject("cardJson");
        if (body == null) {
            body = card;
        }
        String cardName = firstNonEmpty(
                card.optString("cardName", ""),
                body.optString("cardName", ""));
        String sectionTitleHint = firstNonEmpty(
                titleOf(body),
                card.optString("title", ""),
                cardName);
        // Skip deep-link-only promo banners (not a content row)
        if ("promotional_series_card".equalsIgnoreCase(cardName)) {
            return null;
        }
        // Hide Continue Watching row from home feed
        if (isContinueWatchingCard(cardName, sectionTitleHint)) {
            return null;
        }
        GitaDashboard.Section section = new GitaDashboard.Section();
        section.sectionTitle = firstNonEmpty(
                sectionTitleHint,
                cardName.isEmpty() ? "Section" : cardName);
        section.sectionType = firstNonEmpty(
                card.optString("designType", ""),
                body.optString("designType", ""),
                cardName);
        section.layout = layoutForCardName(cardName);
        section.bgImage = firstNonEmpty(
                body.optString("bgImage", ""),
                card.optString("bgImage", ""));
        section.subTitle = firstNonEmpty(
                body.optString("subTitle", ""),
                card.optString("subTitle", ""));
        section.tag = firstTag(body.opt("tag"));
        if (section.tag.isEmpty()) {
            section.tag = firstTag(card.opt("tag"));
        }
        section.hasMoreContent = truthy(body.opt("hasMoreContent"))
                || truthy(card.opt("hasMoreContent"));
        JSONArray content = contentArray(card);
        if (content == null) {
            content = body.optJSONArray("content");
        }
        if (content != null) {
            for (int i = 0; i < content.length(); i++) {
                JSONObject item = content.optJSONObject(i);
                if (item != null) {
                    section.shlokas.add(mapItem(item));
                }
            }
        }
        return section;
    }

    private static boolean isContinueWatchingCard(String cardName, String title) {
        String name = cardName != null ? cardName.trim().toLowerCase() : "";
        String label = title != null ? title.trim().toLowerCase() : "";
        return "continue_watching".equals(name)
                || name.contains("continue_watching")
                || "continue watching".equals(label)
                || label.contains("continue watching");
    }

    private static String layoutForCardName(String cardName) {
        if ("categories".equalsIgnoreCase(cardName)) {
            return "grid";
        }
        if ("featured_series_drop_card".equalsIgnoreCase(cardName)) {
            return "trending";
        }
        return "";
    }

    private static String firstTag(Object tagValue) {
        if (tagValue instanceof JSONArray) {
            JSONArray arr = (JSONArray) tagValue;
            for (int i = 0; i < arr.length(); i++) {
                String t = String.valueOf(arr.opt(i)).trim();
                if (!t.isEmpty() && !"null".equalsIgnoreCase(t)) {
                    return t;
                }
            }
        } else if (tagValue instanceof String) {
            String t = ((String) tagValue).trim();
            if (!t.isEmpty()) {
                return t;
            }
        }
        return "";
    }

    private static boolean truthy(Object value) {
        if (value == null || value == JSONObject.NULL) {
            return false;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        String s = String.valueOf(value).trim();
        return "1".equals(s) || "true".equalsIgnoreCase(s) || "yes".equalsIgnoreCase(s);
    }

    private static void appendHeroBanners(GitaDashboard.Data data, @Nullable JSONArray slides) {
        if (slides == null) {
            return;
        }
        for (int i = 0; i < slides.length(); i++) {
            JSONObject slide = slides.optJSONObject(i);
            if (slide == null) {
                continue;
            }
            String image = imageOf(slide);
            if (image.isEmpty()) {
                continue;
            }
            GitaDashboard.Banner banner = new GitaDashboard.Banner();
            banner.url = image;
            banner.title = titleOf(slide);
            banner.playUrl = playUrlOf(slide);
            banner.seriesId = optLong(slide, "seriesId");
            banner.videoId = optLong(slide, "id");
            banner.imageKey = String.valueOf(slide.opt("id"));
            if (banner.seriesId > 0) {
                banner.imageKey = "prep:" + banner.seriesId + ":" + banner.videoId;
            }
            data.banners.add(banner);
        }
    }

    private static GitaDashboard.ShlokaItem mapItem(JSONObject item) {
        GitaDashboard.ShlokaItem out = new GitaDashboard.ShlokaItem();
        out.title = titleOf(item);
        out.description = firstNonEmpty(
                item.optString("categoryName", ""),
                item.optString("syllabusName", ""),
                item.optString("subTitle", ""),
                item.optString("contentType", ""));
        out.image = imageOf(item);
        out.contentId = (int) Math.min(Integer.MAX_VALUE, optLong(item, "id"));
        out.seriesId = optLong(item, "seriesId");
        if (out.seriesId <= 0 && "series".equalsIgnoreCase(item.optString("contentType", ""))) {
            out.seriesId = optLong(item, "id");
        }
        out.videoId = optLong(item, "id");
        if (item.has("resumeVideoId")) {
            long resume = optLong(item, "resumeVideoId");
            if (resume > 0) {
                out.videoId = resume;
            }
        }
        out.contentType = item.optString("contentType", "");
        out.playUrl = playUrlOf(item);
        out.chapterName = item.optString("categoryName", "");
        out.levelName = item.optString("contentType", "");
        JSONObject badge = item.optJSONObject("badge");
        if (badge != null) {
            out.badge = firstNonEmpty(badge.optString("name", ""), badge.optString("text", ""));
        }
        if (out.badge.isEmpty()) {
            out.badge = item.optString("badgeName", "");
        }
        return out;
    }

    @Nullable
    private static JSONArray contentArray(JSONObject card) {
        JSONObject body = card.optJSONObject("cardJson");
        if (body != null) {
            JSONArray arr = body.optJSONArray("content");
            if (arr != null) {
                return arr;
            }
        }
        return card.optJSONArray("content");
    }

    private static int cardOrder(JSONObject card) {
        try {
            if (card.has("order")) {
                return Integer.parseInt(String.valueOf(card.opt("order")));
            }
            if (card.has("originalOrder")) {
                return Integer.parseInt(String.valueOf(card.opt("originalOrder")));
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    private static String titleOf(JSONObject item) {
        return firstNonEmpty(
                item.optString("title", ""),
                item.optString("name", ""),
                item.optString("cardName", ""),
                item.optString("examName", ""),
                item.optString("categoryName", ""),
                item.optString("label", ""),
                item.optString("subTitle", ""));
    }

    private static String imageOf(JSONObject item) {
        String[] keys = {
                "bannerUrl", "bgImage", "thumbnailUrl", "thumbnail", "firstFrame",
                "imageUrl", "image", "iconUrl", "categoryImage", "audioThumbnail", "contentUrl"
        };
        for (String key : keys) {
            String value = item.optString(key, "").trim();
            if (!value.isEmpty()) {
                return value;
            }
        }
        return "";
    }

    private static String playUrlOf(JSONObject item) {
        String mp4 = item.optString("mp4Url", "").trim();
        if (!mp4.isEmpty()) {
            return mp4;
        }
        String m3u8 = item.optString("m3u8Url", "").trim();
        if (!m3u8.isEmpty()) {
            return m3u8;
        }
        String url = item.optString("url", "").trim();
        if (url.contains(".mp4") || url.contains(".m3u8") || url.contains("mimeType")) {
            return url;
        }
        String mime = item.optString("mimeType", "");
        if ("video".equalsIgnoreCase(mime) && !url.isEmpty()) {
            return url;
        }
        if (!url.isEmpty() && (url.startsWith("http://") || url.startsWith("https://"))) {
            return url;
        }
        return "";
    }

    private static long optLong(JSONObject obj, String key) {
        if (!obj.has(key) || obj.isNull(key)) {
            return 0L;
        }
        try {
            return Long.parseLong(String.valueOf(obj.opt(key)));
        } catch (Exception ignored) {
            return 0L;
        }
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

    public static Set<String> newSeenSet() {
        return new HashSet<>();
    }
}

package com.chill.with.examsaathi;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.media3.database.StandaloneDatabaseProvider;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.datasource.cache.CacheDataSource;
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor;
import androidx.media3.datasource.cache.SimpleCache;

import java.io.File;

/** Disk cache for streaming audio/video — faster seeks and replays. */
public final class PlaybackCacheHelper {

    private static final long CACHE_MAX_BYTES = 96L * 1024L * 1024L;

    private static SimpleCache cache;
    private static CacheDataSource.Factory cacheDataSourceFactory;

    private PlaybackCacheHelper() {}

    @NonNull
    public static synchronized DataSource.Factory getCacheDataSourceFactory(@NonNull Context context) {
        if (cacheDataSourceFactory == null) {
            Context app = context.getApplicationContext();
            File cacheDir = new File(app.getCacheDir(), "playback_cache");
            if (cache == null) {
                cache = new SimpleCache(
                        cacheDir,
                        new LeastRecentlyUsedCacheEvictor(CACHE_MAX_BYTES),
                        new StandaloneDatabaseProvider(app));
            }
            DefaultHttpDataSource.Factory httpFactory = new DefaultHttpDataSource.Factory()
                    .setAllowCrossProtocolRedirects(true)
                    .setConnectTimeoutMs(12_000)
                    .setReadTimeoutMs(12_000);
            DefaultDataSource.Factory upstream = new DefaultDataSource.Factory(app, httpFactory);
            cacheDataSourceFactory = new CacheDataSource.Factory()
                    .setCache(cache)
                    .setUpstreamDataSourceFactory(upstream)
                    .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);
        }
        return cacheDataSourceFactory;
    }
}

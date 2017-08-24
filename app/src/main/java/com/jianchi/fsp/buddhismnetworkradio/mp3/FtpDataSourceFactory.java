package com.jianchi.fsp.buddhismnetworkradio.mp3;

import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSource.Factory;
import com.google.android.exoplayer2.upstream.FileDataSource;
import com.google.android.exoplayer2.upstream.cache.Cache;
import com.google.android.exoplayer2.upstream.cache.CacheDataSink;
import com.google.android.exoplayer2.upstream.cache.CacheDataSource;

/**
 * Created by fsp on 17-8-4.
 */

public class FtpDataSourceFactory implements Factory {

    private Cache cache;
    public static final int cacheSize = 1024 * 1024 * 100;
    public static final int cacheFileSize = 1024 * 1024 * 2;
    public FtpDataSourceFactory(Cache cache){
        this.cache = cache;
    }
    @Override
    public DataSource createDataSource() {
        FtpDataSource ftpDataSource = new FtpDataSource(null);
        CacheDataSource cacheDataSource = new CacheDataSource(
                cache,
                ftpDataSource,
                new FileDataSource(),
                new CacheDataSink(cache, cacheFileSize),
                CacheDataSource.FLAG_BLOCK_ON_CACHE | CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR,
                null);
        return cacheDataSource;
    }
}

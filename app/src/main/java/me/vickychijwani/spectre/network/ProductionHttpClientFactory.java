package me.vickychijwani.spectre.network;

import android.os.Build;
import android.os.StatFs;
import android.support.annotation.Nullable;

import java.io.File;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.OkHttpClient;

public class ProductionHttpClientFactory implements HttpClientFactory {

    private static final int MIN_DISK_CACHE_SIZE = 5 * 1024 * 1024;     // in bytes
    private static final int MAX_DISK_CACHE_SIZE = 50 * 1024 * 1024;    // in bytes
    private static final int CONNECTION_TIMEOUT = 10 * 1000;            // in milliseconds

    /**
     *
     * @param cacheDir - directory for the HTTP cache, disabled if null
     * @return an HTTP client intended for production use
     */
    @Override
    public OkHttpClient create(@Nullable File cacheDir) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        if (cacheDir != null) {
            long size = calculateDiskCacheSize(cacheDir);
            builder.cache(new Cache(cacheDir, size));
        }
        return builder.connectTimeout(CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS)
                .readTimeout(CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS)
                .writeTimeout(CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS)
                .build();
    }

    private static long calculateDiskCacheSize(File dir) {
        long size = MIN_DISK_CACHE_SIZE;
        try {
            StatFs statFs = new StatFs(dir.getAbsolutePath());
            long available;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                available = statFs.getBlockCountLong() * statFs.getBlockSizeLong();
            } else {
                // checked at runtime
                //noinspection deprecation
                available = statFs.getBlockCount() * statFs.getBlockSize();
            }
            // Target 2% of the total space.
            size = available / 50;
        } catch (IllegalArgumentException ignored) {
        }
        // Bound inside min/max size for disk cache.
        return Math.max(Math.min(size, MAX_DISK_CACHE_SIZE), MIN_DISK_CACHE_SIZE);
    }

}

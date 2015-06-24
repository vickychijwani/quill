package me.vickychijwani.spectre;

import android.app.Application;
import android.app.ApplicationErrorReport;
import android.content.ComponentCallbacks;
import android.content.Context;
import android.os.Build;
import android.os.StatFs;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.squareup.okhttp.Cache;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.otto.DeadEvent;
import com.squareup.otto.Subscribe;
import com.squareup.picasso.OkHttpDownloader;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.WeakHashMap;

import me.vickychijwani.spectre.event.ApiErrorEvent;
import me.vickychijwani.spectre.event.BusProvider;
import me.vickychijwani.spectre.network.NetworkService;

public class SpectreApplication extends Application {

    public static final String TAG = "SpectreApplication";
    private static SpectreApplication sInstance;

    private static final String IMAGE_CACHE_PATH = "images";
    private static final int MIN_DISK_CACHE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final int MAX_DISK_CACHE_SIZE = 50 * 1024 * 1024; // 50MB
    private Picasso mPicasso = null;

    @Override
    public void onCreate() {
        super.onCreate();
        Crashlytics.start(this);
        Crashlytics.log(Log.DEBUG, TAG, "APP LAUNCHED");
        BusProvider.getBus().register(this);
        sInstance = this;
        new NetworkService().start(this, getOkHttpClient());
    }

    public static SpectreApplication getInstance() {
        return sInstance;
    }

    protected OkHttpClient getOkHttpClient() {
        File cacheDir = createCacheDir(this, IMAGE_CACHE_PATH);
        long size = calculateDiskCacheSize(cacheDir);
        Cache cache = new Cache(cacheDir, size);
        return new OkHttpClient().setCache(cache);
    }

    public Picasso getPicasso() {
        if (mPicasso == null) {
            mPicasso = new Picasso.Builder(this)
                    .downloader(new OkHttpDownloader(getOkHttpClient()))
                    .listener((picasso, uri, exception) -> {
                        Log.e("Picasso", "Failed to load image: " + uri + "\n"
                                + Log.getStackTraceString(exception));
                    })
                    .build();
        }
        return mPicasso;
    }

    static long calculateDiskCacheSize(File dir) {
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

    private static File createCacheDir(Context context, String path) {
        File cacheDir = context.getApplicationContext().getExternalCacheDir();
        if (cacheDir == null)
            cacheDir = context.getApplicationContext().getCacheDir();
        File cache = new File(cacheDir, path);
        if (!cache.exists()) {
            //noinspection ResultOfMethodCallIgnored
            cache.mkdirs();
        }
        return cache;
    }

    @Subscribe
    public void onApiErrorEvent(ApiErrorEvent event) {
        if (event.error.getResponse() != null &&
                event.error.getResponse().getStatus() != HttpURLConnection.HTTP_NOT_MODIFIED) {
            Log.e(TAG, Log.getStackTraceString(event.error));
        }
    }

    @Subscribe
    public void onDeadEvent(DeadEvent event) {
        Log.w(TAG, "Dead event ignored: " + event.event.getClass().getName());
    }

    // brilliant hack for #75, courtesy http://stackoverflow.com/a/27253968/504611
    @Override
    public void registerComponentCallbacks(ComponentCallbacks callback) {
        super.registerComponentCallbacks(callback);
        ComponentCallbacksAdjustmentTool.INSTANCE.onComponentCallbacksRegistered(callback);
    }

    @Override
    public void unregisterComponentCallbacks(ComponentCallbacks callback) {
        super.unregisterComponentCallbacks(callback);
        ComponentCallbacksAdjustmentTool.INSTANCE.onComponentCallbacksUnregistered(callback);
    }

    public void forceUnregisterComponentCallbacks() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            ComponentCallbacksAdjustmentTool.INSTANCE.unregisterAll(this);
        }
    }

    private static class ComponentCallbacksAdjustmentTool {
        static ComponentCallbacksAdjustmentTool INSTANCE = new ComponentCallbacksAdjustmentTool();

        private WeakHashMap<ComponentCallbacks, ApplicationErrorReport.CrashInfo> mCallbacks = new WeakHashMap<>();
        private boolean mSuspended = false;

        public void onComponentCallbacksRegistered(ComponentCallbacks callback) {
            Throwable thr = new Throwable("Callback registered here");
            ApplicationErrorReport.CrashInfo ci = new ApplicationErrorReport.CrashInfo(thr);
            if (! mSuspended) {
                if (callback.getClass().getName().startsWith("org.chromium.android_webview.AwContents")) {
                    mCallbacks.put(callback, ci);
                }
            } else {
                Log.e(TAG, "ComponentCallbacks was registered while tracking is suspended!");
            }
        }

        public void onComponentCallbacksUnregistered(ComponentCallbacks callback) {
            if (! mSuspended) {
                mCallbacks.remove(callback);
            }
        }

        public void unregisterAll(Context context) {
            mSuspended = true;
            for (Map.Entry<ComponentCallbacks, ApplicationErrorReport.CrashInfo> entry : mCallbacks.entrySet()) {
                ComponentCallbacks callback = entry.getKey();
                if (callback == null) continue;
                if (BuildConfig.DEBUG) {
                    Log.w(TAG, "Forcibly unregistering a misbehaving ComponentCallbacks: " + entry.getKey());
                    Log.w(TAG, entry.getValue().stackTrace);
                }
                try {
                    context.unregisterComponentCallbacks(entry.getKey());
                } catch (Exception exc) {
                    Log.e(TAG, "Unable to unregister ComponentCallbacks", exc);
                }
            }
            mCallbacks.clear();
            mSuspended = false;
        }
    }

}

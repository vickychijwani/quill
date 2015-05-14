package me.vickychijwani.spectre;

import android.app.Application;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.otto.DeadEvent;
import com.squareup.otto.Subscribe;
import com.squareup.picasso.OkHttpDownloader;
import com.squareup.picasso.Picasso;

import java.net.HttpURLConnection;

import me.vickychijwani.spectre.event.ApiErrorEvent;
import me.vickychijwani.spectre.event.BusProvider;
import me.vickychijwani.spectre.network.NetworkService;

public class SpectreApplication extends Application {

    public static final String TAG = "SpectreApplication";
    private static SpectreApplication sInstance;

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
        return new OkHttpClient();
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

}

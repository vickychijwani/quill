package me.vickychijwani.spectre;

import android.app.Application;
import android.util.Log;

import com.squareup.otto.Subscribe;

import me.vickychijwani.spectre.event.ApiErrorEvent;
import me.vickychijwani.spectre.event.BusProvider;
import me.vickychijwani.spectre.network.NetworkService;

public class SpectreApplication extends Application {

    public static final String TAG = "SpectreApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        BusProvider.getBus().register(this);
        NetworkService networkService = new NetworkService();
        networkService.start();
    }

    @Subscribe
    public void onApiErrorEvent(ApiErrorEvent event) {
        Log.e(TAG, Log.getStackTraceString(event.error));
    }

}

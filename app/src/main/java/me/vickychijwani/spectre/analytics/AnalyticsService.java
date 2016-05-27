package me.vickychijwani.spectre.analytics;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.crashlytics.android.answers.LoginEvent;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import me.vickychijwani.spectre.event.GhostVersionLoadedEvent;
import me.vickychijwani.spectre.event.LoadGhostVersionEvent;
import me.vickychijwani.spectre.event.LoginDoneEvent;
import me.vickychijwani.spectre.event.LoginErrorEvent;

public class AnalyticsService {

    private static final String TAG = AnalyticsService.class.getSimpleName();

    private Bus mEventBus;

    public AnalyticsService(Bus eventBus) {
        mEventBus = eventBus;
    }

    public void start() {
        getBus().register(this);
        getBus().post(new LoadGhostVersionEvent(true));
    }

    public void stop() {
        getBus().unregister(this);
    }

    @Subscribe
    public void onLoginDoneEvent(LoginDoneEvent event) {
        if (event.wasInitiatedByUser) {
            String blogType = getBlogTypeFromUrl(event.blogUrl);
            logLogin(blogType, true);

            // user just logged in, now's a good time to check this
            getBus().post(new LoadGhostVersionEvent(true));
        }
    }

    @Subscribe
    public void onLoginErrorEvent(LoginErrorEvent event) {
        if (event.wasInitiatedByUser) {
            String blogType = getBlogTypeFromUrl(event.blogUrl);
            logLogin(blogType, true);
        }
    }

    @Subscribe
    public void onGhostVersionLoadedEvent(GhostVersionLoadedEvent event) {
        logGhostVersion(event.version);
    }

    public static void logGhostVersion(@Nullable String ghostVersion) {
        if (ghostVersion == null) {
            ghostVersion = "Unknown";
        }
        Crashlytics.log(Log.INFO, TAG, "GHOST VERSION = " + ghostVersion);
        Answers.getInstance().logCustom(new CustomEvent("Ghost Version")
                .putCustomAttribute("version", ghostVersion));
    }

    public static void logLogin(String blogType, boolean success) {
        String successStr = success ? "SUCCEEDED" : "FAILED";
        Crashlytics.log(Log.INFO, TAG, "LOGIN " + successStr + ", blog type = " + blogType);
        Answers.getInstance().logLogin(new LoginEvent()
                .putMethod(blogType)
                .putSuccess(success));
    }

    public static void logDbSchemaVersion(@NonNull String dbSchemaVersion) {
        Crashlytics.log(Log.INFO, TAG, "DB SCHEMA VERSION = " + dbSchemaVersion);
        Answers.getInstance().logCustom(new CustomEvent("DB Schema Version")
                .putCustomAttribute("version", dbSchemaVersion));
    }

    private String getBlogTypeFromUrl(@Nullable String blogUrl) {
        if (blogUrl == null) {
            return "Unknown";
        } else if (blogUrl.matches("ghost.io")) {
            return "Ghost Pro";
        } else {
            return "Self-hosted";
        }
    }

    private Bus getBus() {
        return mEventBus;
    }

}

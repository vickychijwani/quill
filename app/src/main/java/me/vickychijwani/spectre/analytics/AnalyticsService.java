package me.vickychijwani.spectre.analytics;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.crashlytics.android.answers.LoginEvent;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import me.vickychijwani.spectre.event.FileUploadedEvent;
import me.vickychijwani.spectre.event.GhostVersionLoadedEvent;
import me.vickychijwani.spectre.event.LoadGhostVersionEvent;
import me.vickychijwani.spectre.event.LoginDoneEvent;
import me.vickychijwani.spectre.event.LoginErrorEvent;
import me.vickychijwani.spectre.event.LogoutStatusEvent;
import timber.log.Timber;

public class AnalyticsService {

    private static final String TAG = AnalyticsService.class.getSimpleName();

    private final Bus mEventBus;

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
        logLogin(event.blogUrl, true);

        // user just logged in, now's a good time to check this
        getBus().post(new LoadGhostVersionEvent(true));
    }

    @Subscribe
    public void onLoginErrorEvent(LoginErrorEvent event) {
        logLogin(event.blogUrl, false);
    }

    @Subscribe
    public void onGhostVersionLoadedEvent(GhostVersionLoadedEvent event) {
        logGhostVersion(event.version);
    }

    private static void logGhostVersion(@Nullable String ghostVersion) {
        if (ghostVersion == null) {
            ghostVersion = "Unknown";
        }
        Timber.i("GHOST VERSION = " + ghostVersion);
        Answers.getInstance().logCustom(new CustomEvent("Ghost Version")
                .putCustomAttribute("version", ghostVersion));
    }

    private static void logLogin(@Nullable String blogUrl, boolean success) {
        if (blogUrl == null) {
            blogUrl = "Unknown";
        }
        String successStr = success ? "SUCCEEDED" : "FAILED";
        Timber.i("LOGIN " + successStr + ", BLOG URL = " + blogUrl);
        Answers.getInstance().logLogin(new LoginEvent()
                .putCustomAttribute("URL", blogUrl)
                .putSuccess(success));
    }

    public static void logGhostV0Error() {
        Timber.i("GHOST VERSION 0.x ERROR - UPGRADE REQUIRED");
        Answers.getInstance().logCustom(new CustomEvent("Ghost v0.x error"));
    }

    @Subscribe
    public void onLogoutStatusEvent(LogoutStatusEvent logoutEvent) {
        if (logoutEvent.succeeded) {
            Timber.i("LOGOUT SUCCEEDED");
            Answers.getInstance().logCustom(new CustomEvent("Logout"));
        }
    }

    public static void logMetadataDbSchemaVersion(@NonNull String metadataDbSchemaVersion) {
        Timber.i("METADATA DB SCHEMA VERSION = " + metadataDbSchemaVersion);
        Answers.getInstance().logCustom(new CustomEvent("Metadata DB Schema Version")
                .putCustomAttribute("version", metadataDbSchemaVersion));
    }

    public static void logDbSchemaVersion(@NonNull String dbSchemaVersion) {
        Timber.i("DB SCHEMA VERSION = " + dbSchemaVersion);
        Answers.getInstance().logCustom(new CustomEvent("DB Schema Version")
                .putCustomAttribute("version", dbSchemaVersion));
    }


    // post actions
    public static void logNewDraftUploaded() {
        logPostAction("New draft uploaded", null);
    }

    public static void logDraftPublished(String postUrl) {
        logPostAction("Published draft", postUrl);
    }

    public static void logScheduledPostUpdated(String postUrl) {
        logPostAction("Scheduled post updated", postUrl);
    }

    public static void logPublishedPostUpdated(String postUrl) {
        logPostAction("Published post updated", postUrl);
    }

    public static void logPostUnpublished() {
        logPostAction("Unpublished post", null);
    }

    public static void logDraftAutoSaved() {
        logPostAction("Auto-saved draft", null);
    }

    public static void logDraftSavedExplicitly() {
        logPostAction("Explicitly saved draft", null);
    }

    public static void logPostSavedInUnknownScenario() {
        logPostAction("Unknown scenario", null);
    }

    public static void logPublishedPostAutoSavedLocally() {
        logPostAction("Auto-saved edits to published post", null);
    }

    public static void logScheduledPostAutoSavedLocally() {
        logPostAction("Auto-saved edits to scheduled post", null);
    }

    public static void logDraftDeleted() {
        logPostAction("Deleted draft", null);
    }

    public static void logConflictFound() {
        logPostAction("Conflict found", null);
    }

    public static void logConflictResolved() {
        logPostAction("Conflict resolved", null);
    }

    @Subscribe
    public void onFileUploadedEvent(FileUploadedEvent event) {
        logPostAction("Image uploaded", null);
    }

    private static void logPostAction(@NonNull String postAction, @Nullable String postUrl) {
        CustomEvent postStatsEvent = new CustomEvent("Post Actions")
                .putCustomAttribute("Scenario", postAction);
        if (postUrl != null) {
            // FIXME this is a huge hack, also Fabric only shows 10 of these per day
            postStatsEvent.putCustomAttribute("URL", postUrl);
        }
        Timber.i("POST ACTION: " + postAction);
        Answers.getInstance().logCustom(postStatsEvent);
    }


    // misc private methods
    private Bus getBus() {
        return mEventBus;
    }

}

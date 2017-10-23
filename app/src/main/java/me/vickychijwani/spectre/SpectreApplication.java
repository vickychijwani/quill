package me.vickychijwani.spectre;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.jakewharton.picasso.OkHttp3Downloader;
import com.squareup.otto.DeadEvent;
import com.squareup.otto.Subscribe;
import com.squareup.picasso.Picasso;
import com.tsengvn.typekit.Typekit;

import java.io.File;
import java.io.IOException;

import io.fabric.sdk.android.Fabric;
import io.reactivex.plugins.RxJavaPlugins;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.exceptions.RealmMigrationNeededException;
import me.vickychijwani.spectre.analytics.AnalyticsService;
import me.vickychijwani.spectre.auth.LoginOrchestrator;
import me.vickychijwani.spectre.error.UncaughtRxException;
import me.vickychijwani.spectre.event.ApiErrorEvent;
import me.vickychijwani.spectre.event.BusProvider;
import me.vickychijwani.spectre.model.BlogMetadataDBMigration;
import me.vickychijwani.spectre.model.BlogMetadataModule;
import me.vickychijwani.spectre.network.NetworkService;
import me.vickychijwani.spectre.network.ProductionHttpClientFactory;
import me.vickychijwani.spectre.util.CrashReportingTree;
import okhttp3.OkHttpClient;
import retrofit2.Response;
import timber.log.Timber;

public class SpectreApplication extends Application {

    private static final String TAG = "SpectreApplication";
    private static SpectreApplication sInstance;

    // this is named "images" but it actually caches all HTTP responses
    private static final String HTTP_CACHE_PATH = "images";

    protected OkHttpClient mOkHttpClient = null;
    protected Picasso mPicasso = null;

    @SuppressWarnings("FieldCanBeLocal")
    private AnalyticsService mAnalyticsService = null;

    // FIXME hacks
    private LoginOrchestrator.HACKListener mHACKListener;
    private int mHACKOldSchemaVersion = -1;

    @Override
    public void onCreate() {
        super.onCreate();

        Fabric.with(this, new Crashlytics(), new Answers());
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        } else {
            Timber.plant(new CrashReportingTree());
        }
        Crashlytics.log(Log.DEBUG, TAG, "APP LAUNCHED");

        BusProvider.getBus().register(this);
        sInstance = this;

        RxJavaPlugins.setErrorHandler(this::uncaughtRxException);

        setupMetadataRealm();
        setupFonts();
        initOkHttpClient();
        initPicasso();

        NetworkService networkService = new NetworkService();
        mHACKListener = networkService;
        networkService.start(mOkHttpClient);

        mAnalyticsService = new AnalyticsService(BusProvider.getBus());
        mAnalyticsService.start();
    }

    public void setOldRealmSchemaVersion(int oldSchemaVersion) {
        mHACKOldSchemaVersion = oldSchemaVersion;
    }

    private void setupMetadataRealm() {
        final int METADATA_DB_SCHEMA_VERSION = 4;
        Realm.init(this);
        RealmConfiguration config = new RealmConfiguration.Builder()
                .modules(new BlogMetadataModule())
                .schemaVersion(METADATA_DB_SCHEMA_VERSION)
                .migration(new BlogMetadataDBMigration())
                .build();
        Realm.setDefaultConfiguration(config);

        // open the Realm to check if a migration is needed
        try {
            Realm realm = Realm.getDefaultInstance();
            realm.close();
        } catch (RealmMigrationNeededException e) {
            // delete existing Realm if we're below v4
            if (mHACKOldSchemaVersion >= 0 && mHACKOldSchemaVersion < 4) {
                Realm.deleteRealm(config);
                mHACKOldSchemaVersion = -1;
            }
        }

        AnalyticsService.logMetadataDbSchemaVersion(String.valueOf(METADATA_DB_SCHEMA_VERSION));
    }

    private void setupFonts() {
        Typekit.getInstance()
                .addNormal(Typekit.createFromAsset(this, "fonts/OpenSans-Regular.ttf"))
                .addItalic(Typekit.createFromAsset(this, "fonts/OpenSans-Italic.ttf"))
                .addBold(Typekit.createFromAsset(this, "fonts/OpenSans-Bold.ttf"))
                .addBoldItalic(Typekit.createFromAsset(this, "fonts/OpenSans-BoldItalic.ttf"))
                .add("narrow-bold", Typekit.createFromAsset(this, "fonts/OpenSans-CondBold.ttf"));
    }

    public static SpectreApplication getInstance() {
        return sInstance;
    }

    protected void initOkHttpClient() {
        if (mOkHttpClient != null) {
            return;
        }
        File cacheDir = createCacheDir(this);
        mOkHttpClient = new ProductionHttpClientFactory().create(cacheDir);
    }

    @SuppressWarnings("WeakerAccess")
    protected void initPicasso() {
        if (mPicasso != null) {
            return;
        }
        mPicasso = new Picasso.Builder(this)
                .downloader(new OkHttp3Downloader(mOkHttpClient))
                .listener((picasso, uri, exception) -> {
                    Log.e("Picasso", "Failed to load image: " + uri + "\n"
                            + Log.getStackTraceString(exception));
                })
                .build();
    }

    public OkHttpClient getOkHttpClient() {
        return mOkHttpClient;
    }

    public Picasso getPicasso() {
        return mPicasso;
    }

    public LoginOrchestrator.HACKListener getHACKListener() {
        return mHACKListener;
    }

    public void addDebugDrawer(@NonNull Activity activity) {
        // no-op, overridden in debug build
    }

    @Nullable protected static File createCacheDir(Context context) {
        File cacheDir = context.getApplicationContext().getExternalCacheDir();
        if (cacheDir == null) {
            cacheDir = context.getApplicationContext().getCacheDir();
        }

        File cache = new File(cacheDir, HTTP_CACHE_PATH);
        if (cache.exists() || cache.mkdirs()) {
            return cache;
        } else {
            return null;
        }
    }

    @Subscribe
    public void onApiErrorEvent(ApiErrorEvent event) {
        Response errorResponse = event.apiFailure.response;
        Throwable error = event.apiFailure.error;
        if (errorResponse != null) {
            try {
                String responseString = errorResponse.errorBody().string();
                Crashlytics.log(Log.ERROR, TAG, responseString);
            } catch (IOException e) {
                Crashlytics.log(Log.ERROR, TAG, "[onApiErrorEvent] Error while parsing response" +
                        " error body!");
            }
        }
        if (error != null) {
            Crashlytics.log(Log.ERROR, TAG, Log.getStackTraceString(error));
        }
    }

    @Subscribe
    public void onDeadEvent(DeadEvent event) {
        Crashlytics.log(Log.WARN, TAG, "Dead event ignored: " + event.event.getClass().getName());
    }

    private void uncaughtRxException(Throwable e) {
        Crashlytics.logException(new UncaughtRxException(e));
    }

}

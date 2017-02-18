package me.vickychijwani.spectre;

import android.app.Activity;
import android.support.annotation.NonNull;

import com.facebook.stetho.Stetho;
import com.squareup.leakcanary.LeakCanary;
import com.uphyca.stetho_realm.RealmInspectorModulesProvider;

import java.io.File;

import io.palaima.debugdrawer.DebugDrawer;
import io.palaima.debugdrawer.commons.BuildModule;
import io.palaima.debugdrawer.commons.DeviceModule;
import io.palaima.debugdrawer.commons.SettingsModule;
import io.palaima.debugdrawer.okhttp3.OkHttp3Module;
import io.palaima.debugdrawer.picasso.PicassoModule;
import io.palaima.debugdrawer.scalpel.ScalpelModule;
import me.vickychijwani.spectre.network.UnsafeHttpClientFactory;

public class DebugSpectreApplication extends SpectreApplication {

    @Override
    public void onCreate() {
        super.onCreate();

        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return;
        }

        // auto-detect Activity memory leaks!
        LeakCanary.install(this);

        Stetho.initialize(Stetho.newInitializerBuilder(this)
                .enableWebKitInspector(RealmInspectorModulesProvider.builder(this).build())
                .build());
    }

    @Override
    protected void initOkHttpClient() {
        if (mOkHttpClient != null) {
            return;
        }
        File cacheDir = createCacheDir(this);
        mOkHttpClient = new UnsafeHttpClientFactory().create(cacheDir);
    }

    @Override
    public void addDebugDrawer(@NonNull Activity activity) {
        new DebugDrawer.Builder(activity).modules(
                new ScalpelModule(activity),
                new OkHttp3Module(mOkHttpClient),
                new PicassoModule(mPicasso),
                new DeviceModule(activity),
                new BuildModule(activity),
                new SettingsModule(activity)
        ).build();
    }

}

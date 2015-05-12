package me.vickychijwani.spectre;

import com.facebook.stetho.Stetho;
import com.facebook.stetho.okhttp.StethoInterceptor;
import com.squareup.leakcanary.LeakCanary;
import com.squareup.okhttp.OkHttpClient;
import com.uphyca.stetho_realm.RealmInspectorModulesProvider;

public class DebugSpectreApplication extends SpectreApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        Stetho.Initializer initializer = Stetho.newInitializerBuilder(this)
                .enableDumpapp(Stetho.defaultDumperPluginsProvider(this))
                .enableWebKitInspector(RealmInspectorModulesProvider.builder(this)
                        .withMetaTables()
                        .build())
                .build();
        Stetho.initialize(initializer);

        // auto-detect Activity memory leaks!
        LeakCanary.install(this);
    }

    @Override
    protected OkHttpClient getOkHttpClient() {
        OkHttpClient client = super.getOkHttpClient();
        client.networkInterceptors().add(new StethoInterceptor());
        return client;
    }

}

package me.vickychijwani.spectre;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.support.annotation.NonNull;

import com.facebook.stetho.Stetho;
import com.facebook.stetho.okhttp.StethoInterceptor;
import com.squareup.leakcanary.LeakCanary;
import com.uphyca.stetho_realm.RealmInspectorModulesProvider;

import java.security.cert.CertificateException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import io.palaima.debugdrawer.DebugDrawer;
import io.palaima.debugdrawer.module.BuildModule;
import io.palaima.debugdrawer.module.DeviceModule;
import io.palaima.debugdrawer.module.SettingsModule;
import io.palaima.debugdrawer.okhttp.OkHttpModule;
import io.palaima.debugdrawer.picasso.PicassoModule;
import io.palaima.debugdrawer.scalpel.ScalpelModule;

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
    protected void initOkHttpClient() {
        if (mOkHttpClient != null) {
            return;
        }
        super.initOkHttpClient();
        mOkHttpClient.networkInterceptors().add(new StethoInterceptor());

        // trust all SSL certs, for TESTING ONLY!
        mOkHttpClient.setHostnameVerifier((hostname, session) -> true);
        mOkHttpClient.setSslSocketFactory(getUnsafeSslSocketFactory());
    }

    @Override
    public void addDebugDrawer(@NonNull Activity activity) {
        new DebugDrawer.Builder(activity).modules(
                new ScalpelModule(activity),
                new OkHttpModule(mOkHttpClient),
                new PicassoModule(mPicasso),
                new DeviceModule(this),
                new BuildModule(this),
                new SettingsModule(this)
        ).build();
    }

    private SSLSocketFactory getUnsafeSslSocketFactory() {
        try {
            // Create a trust manager that does not validate certificate chains
            final TrustManager[] trustAllCerts = new TrustManager[] { new TrustEveryoneManager() };
            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            // Create an ssl socket factory with our all-trusting manager
            return sslContext.getSocketFactory();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // private stuff
    private static class TrustEveryoneManager implements X509TrustManager {
        @SuppressLint("TrustAllX509TrustManager")
        @Override
        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType)
                throws CertificateException {}

        @SuppressLint("TrustAllX509TrustManager")
        @Override
        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType)
                throws CertificateException {}

        @Override
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    }

}

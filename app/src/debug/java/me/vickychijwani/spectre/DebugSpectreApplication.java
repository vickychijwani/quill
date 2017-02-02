package me.vickychijwani.spectre;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.support.annotation.NonNull;

import com.squareup.leakcanary.LeakCanary;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import io.palaima.debugdrawer.DebugDrawer;
import io.palaima.debugdrawer.commons.BuildModule;
import io.palaima.debugdrawer.commons.DeviceModule;
import io.palaima.debugdrawer.commons.SettingsModule;
import io.palaima.debugdrawer.okhttp3.OkHttp3Module;
import io.palaima.debugdrawer.picasso.PicassoModule;
import io.palaima.debugdrawer.scalpel.ScalpelModule;

public class DebugSpectreApplication extends SpectreApplication {

    @Override
    public void onCreate() {
        super.onCreate();

        // auto-detect Activity memory leaks!
        LeakCanary.install(this);
    }

    @Override
    protected void initOkHttpClient() {
        if (mOkHttpClient != null) {
            return;
        }
        super.initOkHttpClient();

        mOkHttpClient = mOkHttpClient.newBuilder()
                // trust all SSL certs, for TESTING ONLY!
                .hostnameVerifier((hostname, session) -> true)
                .sslSocketFactory(getUnsafeSslSocketFactory(), TrustEveryoneManager.getInstance())
                .build();
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

    private SSLSocketFactory getUnsafeSslSocketFactory() {
        try {
            // Create a trust manager that does not validate certificate chains
            final TrustManager[] trustAllCerts = new TrustManager[] { TrustEveryoneManager.getInstance() };
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

        private static TrustEveryoneManager sInstance = null;

        public static TrustEveryoneManager getInstance() {
            if (sInstance == null) {
                sInstance = new TrustEveryoneManager();
            }
            return sInstance;
        }

        private TrustEveryoneManager() {}

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
            return new X509Certificate[0];
        }
    }

}

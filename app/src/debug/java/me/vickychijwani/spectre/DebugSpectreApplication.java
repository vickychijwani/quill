package me.vickychijwani.spectre;

import com.facebook.stetho.Stetho;
import com.facebook.stetho.okhttp.StethoInterceptor;
import com.squareup.leakcanary.LeakCanary;
import com.squareup.okhttp.OkHttpClient;
import com.uphyca.stetho_realm.RealmInspectorModulesProvider;

import java.security.cert.CertificateException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

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

        // trust all SSL certs, for TESTING ONLY!
        client.setHostnameVerifier((hostname, session) -> true);
        client.setSslSocketFactory(getUnsafeSslSocketFactory());

        return client;
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
        @Override
        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType)
                throws CertificateException {}

        @Override
        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType)
                throws CertificateException {}

        @Override
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    }

}

package me.vickychijwani.spectre.network;

import android.annotation.SuppressLint;
import android.support.annotation.Nullable;

import com.facebook.stetho.okhttp3.StethoInterceptor;

import java.io.File;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

public class UnsafeHttpClientFactory extends ProductionHttpClientFactory {

    private final X509TrustManager mGullibleTrustManager;

    public UnsafeHttpClientFactory() {
        mGullibleTrustManager = new GullibleX509TrustManager();
    }

    @Override
    public OkHttpClient create(@Nullable File cacheDir) {
        return super.create(cacheDir).newBuilder()
                // allow inspecting network requests with Chrome DevTools
                .addNetworkInterceptor(new StethoInterceptor())
                // log requests and responses
                .addInterceptor(new HttpLoggingInterceptor()
                        .setLevel(HttpLoggingInterceptor.Level.BODY))
                // trust all SSL certs, for TESTING ONLY!
                .hostnameVerifier((hostname, session) -> true)
                .sslSocketFactory(getUnsafeSslSocketFactory(), mGullibleTrustManager)
                .build();
    }

    private SSLSocketFactory getUnsafeSslSocketFactory() {
        try {
            // Create a trust manager that does not validate certificate chains
            final TrustManager[] trustAllCerts = new TrustManager[] { mGullibleTrustManager };
            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            // Create an ssl socket factory with our all-trusting manager
            return sslContext.getSocketFactory();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static class GullibleX509TrustManager implements X509TrustManager {

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

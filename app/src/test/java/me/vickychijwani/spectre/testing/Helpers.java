package me.vickychijwani.spectre.testing;

import android.support.annotation.NonNull;

import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

import io.reactivex.Single;
import me.vickychijwani.spectre.network.ProductionHttpClientFactory;
import okhttp3.OkHttpClient;
import okhttp3.internal.tls.SslClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.mock.MockRetrofit;
import retrofit2.mock.NetworkBehavior;

public class Helpers {

    public static final SSLSocketFactory LOCALHOST_SOCKET_FACTORY = SslClient.localhost().socketFactory;
    public static final X509TrustManager LOCALHOST_TRUST_MANAGER = SslClient.localhost().trustManager;

    // helpers
    public static OkHttpClient getProdHttpClient() {
        OkHttpClient httpClient = new ProductionHttpClientFactory().create(null);

        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        return httpClient.newBuilder()
                .sslSocketFactory(LOCALHOST_SOCKET_FACTORY, LOCALHOST_TRUST_MANAGER)
                .addInterceptor(loggingInterceptor)
                .build();
    }

    @NonNull
    public static MockRetrofit getMockRetrofit(Retrofit retrofit, NetworkBehavior networkBehavior) {
        return new MockRetrofit.Builder(retrofit)
                .networkBehavior(networkBehavior)
                .build();
    }

    public static NetworkBehavior getIdealNetworkBehavior() {
        return getNetworkBehavior(0, 0, 0, 0);
    }

    public static NetworkBehavior getFailingNetworkBehaviour() {
        return getNetworkBehavior(0, 0, 100, 0);
    }

    public static <T> T execute(Single<T> single) {
        return single.blockingGet();
    }


    // private methods
    private static NetworkBehavior getNetworkBehavior(int delayMsec, int delayVariance,
                                                      int failurePercent, int errorPercent) {
        NetworkBehavior networkBehavior = NetworkBehavior.create();
        networkBehavior.setDelay(delayMsec, TimeUnit.MILLISECONDS);
        networkBehavior.setVariancePercent(delayVariance);
        // "failure" means network layer failure
        networkBehavior.setFailurePercent(failurePercent);
        // "error" means HTTP error
        networkBehavior.setErrorPercent(errorPercent);
        return networkBehavior;
    }

}

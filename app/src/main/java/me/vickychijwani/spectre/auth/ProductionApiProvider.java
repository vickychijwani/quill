package me.vickychijwani.spectre.auth;

import android.support.annotation.NonNull;

import me.vickychijwani.spectre.network.ApiProvider;
import me.vickychijwani.spectre.network.GhostApiService;
import me.vickychijwani.spectre.network.GhostApiUtils;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;

class ProductionApiProvider implements ApiProvider {

    private final Retrofit mRetrofit;

    public ProductionApiProvider(@NonNull OkHttpClient httpClient, @NonNull String blogUrl) {
        mRetrofit = GhostApiUtils.getRetrofit(blogUrl, httpClient);
    }

    @Override
    public Retrofit getRetrofit() {
        return mRetrofit;
    }

    @Override
    public GhostApiService getGhostApi() {
        return mRetrofit.create(GhostApiService.class);
    }

}

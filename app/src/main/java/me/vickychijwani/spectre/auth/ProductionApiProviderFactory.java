package me.vickychijwani.spectre.auth;

import me.vickychijwani.spectre.network.ApiProvider;
import me.vickychijwani.spectre.network.ApiProviderFactory;
import okhttp3.OkHttpClient;

class ProductionApiProviderFactory implements ApiProviderFactory {

    private final OkHttpClient mHttpClient;

    public ProductionApiProviderFactory(OkHttpClient httpClient) {
        mHttpClient = httpClient;
    }

    @Override
    public ApiProvider create(String blogUrl) {
        return new ProductionApiProvider(mHttpClient, blogUrl);
    }

}

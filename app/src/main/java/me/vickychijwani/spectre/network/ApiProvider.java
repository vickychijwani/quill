package me.vickychijwani.spectre.network;

import retrofit2.Retrofit;

public interface ApiProvider {

    Retrofit getRetrofit();

    GhostApiService getGhostApi();

}

package me.vickychijwani.spectre.event;

import android.support.annotation.Nullable;

import retrofit.RetrofitError;

public class DataRefreshedEvent {

    public final RetrofitError error;

    public DataRefreshedEvent(@Nullable RetrofitError error) {
        this.error = error;
    }

}

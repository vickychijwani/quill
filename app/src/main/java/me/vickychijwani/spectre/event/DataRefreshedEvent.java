package me.vickychijwani.spectre.event;

import android.support.annotation.Nullable;

import me.vickychijwani.spectre.network.ApiFailure;

public class DataRefreshedEvent {

    public final ApiFailure apiFailure;

    public DataRefreshedEvent(@Nullable ApiFailure apiFailure) {
        this.apiFailure = apiFailure;
    }

}

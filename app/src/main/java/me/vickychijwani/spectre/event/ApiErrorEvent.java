package me.vickychijwani.spectre.event;

import android.support.annotation.NonNull;

import retrofit.RetrofitError;

public class ApiErrorEvent {

    public final RetrofitError error;

    public ApiErrorEvent(@NonNull RetrofitError error) {
        this.error = error;
    }

}

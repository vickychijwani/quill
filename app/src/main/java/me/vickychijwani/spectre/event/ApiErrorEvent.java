package me.vickychijwani.spectre.event;

import retrofit.RetrofitError;

public class ApiErrorEvent {

    public final RetrofitError error;

    public ApiErrorEvent(RetrofitError error) {
        this.error = error;
    }

}

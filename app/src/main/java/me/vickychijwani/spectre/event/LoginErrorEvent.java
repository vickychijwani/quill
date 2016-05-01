package me.vickychijwani.spectre.event;

import android.support.annotation.Nullable;

import retrofit.RetrofitError;

public class LoginErrorEvent {

    public final RetrofitError error;
    public final String blogUrl;

    public LoginErrorEvent(RetrofitError error, @Nullable String blogUrl) {
        this.error = error;
        this.blogUrl = blogUrl;
    }

}

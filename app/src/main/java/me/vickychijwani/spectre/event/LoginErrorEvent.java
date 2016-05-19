package me.vickychijwani.spectre.event;

import android.support.annotation.Nullable;

import retrofit.RetrofitError;

public class LoginErrorEvent {

    public final RetrofitError error;
    public final String blogUrl;
    public final boolean wasInitiatedByUser;

    public LoginErrorEvent(RetrofitError error, @Nullable String blogUrl,
                           boolean wasInitiatedByUser) {
        this.error = error;
        this.blogUrl = blogUrl;
        this.wasInitiatedByUser = wasInitiatedByUser;
    }

}

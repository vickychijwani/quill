package me.vickychijwani.spectre.event;

import android.support.annotation.Nullable;

import me.vickychijwani.spectre.network.ApiFailure;
import me.vickychijwani.spectre.network.entity.ApiErrorList;

public class LoginErrorEvent<T> {

    public final ApiFailure<T> apiFailure;
    @Nullable public final ApiErrorList apiErrors;
    public final String blogUrl;
    public final boolean wasInitiatedByUser;

    public LoginErrorEvent(ApiFailure<T> error, @Nullable ApiErrorList apiErrors,
                           @Nullable String blogUrl, boolean wasInitiatedByUser) {
        this.apiFailure = error;
        this.apiErrors = apiErrors;
        this.blogUrl = blogUrl;
        this.wasInitiatedByUser = wasInitiatedByUser;
    }

}

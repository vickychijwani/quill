package me.vickychijwani.spectre.network;

import android.support.annotation.Nullable;

import retrofit2.Response;

public class ApiFailure<T> {

    @Nullable public final Response<T> response;
    @Nullable public final Throwable error;

    @SuppressWarnings("NullableProblems")
    public ApiFailure(Response<T> response, Throwable error) {
        this.response = response;
        this.error = error;
    }

    @SuppressWarnings("NullableProblems")
    public ApiFailure(Throwable error) {
        this.response = null;
        this.error = error;
    }

    @SuppressWarnings("NullableProblems")
    public ApiFailure(Response<T> response) {
        this.response = response;
        this.error = null;
    }

}

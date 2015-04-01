package me.vickychijwani.spectre.event;

import retrofit.RetrofitError;

public class LoginErrorEvent {

    public final RetrofitError error;

    public LoginErrorEvent(RetrofitError error) {
        this.error = error;
    }

}

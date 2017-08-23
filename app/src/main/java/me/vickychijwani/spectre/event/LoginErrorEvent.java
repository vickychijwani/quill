package me.vickychijwani.spectre.event;

import android.support.annotation.Nullable;

public class LoginErrorEvent {

    public final String blogUrl;

    public LoginErrorEvent(@Nullable String blogUrl) {
        this.blogUrl = blogUrl;
    }

}

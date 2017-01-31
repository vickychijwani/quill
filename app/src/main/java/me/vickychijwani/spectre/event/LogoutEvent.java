package me.vickychijwani.spectre.event;

import android.support.annotation.NonNull;

public class LogoutEvent {

    @NonNull public final String blogUrl;
    public final boolean forceLogout;

    public LogoutEvent(@NonNull String blogUrl, boolean forceLogout) {
        this.blogUrl = blogUrl;
        this.forceLogout = forceLogout;
    }

}

package me.vickychijwani.spectre.event;

public class LoginDoneEvent {

    public final String blogUrl;
    public final boolean wasInitiatedByUser;

    public LoginDoneEvent(String blogUrl,
                          boolean wasInitiatedByUser) {
        this.blogUrl = blogUrl;
        this.wasInitiatedByUser = wasInitiatedByUser;
    }

}

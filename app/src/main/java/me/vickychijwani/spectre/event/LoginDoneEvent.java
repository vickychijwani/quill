package me.vickychijwani.spectre.event;

public class LoginDoneEvent {

    public final String blogUrl;
    public final String username;
    public final String password;
    public final boolean wasInitiatedByUser;

    public LoginDoneEvent(String blogUrl, String username, String password,
                          boolean wasInitiatedByUser) {
        this.blogUrl = blogUrl;
        this.username = username;
        this.password = password;
        this.wasInitiatedByUser = wasInitiatedByUser;
    }

}

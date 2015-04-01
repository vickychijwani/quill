package me.vickychijwani.spectre.event;

public class LoginDoneEvent {

    public final String blogUrl;
    public final String username;
    public final String password;

    public LoginDoneEvent(String blogUrl, String username, String password) {
        this.blogUrl = blogUrl;
        this.username = username;
        this.password = password;
    }

}

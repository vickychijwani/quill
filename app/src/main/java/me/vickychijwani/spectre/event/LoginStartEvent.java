package me.vickychijwani.spectre.event;

public class LoginStartEvent {

    public final String blogUrl;
    public final String username;
    public final String password;

    public LoginStartEvent(String blogUrl, String username, String password) {
        this.blogUrl = blogUrl;
        this.username = username;
        this.password = password;
    }

}

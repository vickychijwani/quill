package me.vickychijwani.spectre.event;

public class LoginStartEvent {

    public final String blogUrl;
    public final String username;
    public final String password;
    public final boolean initiatedByUser;

    public LoginStartEvent(String blogUrl, String username, String password, boolean initiatedByUser) {
        this.blogUrl = blogUrl;
        this.username = username;
        this.password = password;
        this.initiatedByUser = initiatedByUser;
    }

}

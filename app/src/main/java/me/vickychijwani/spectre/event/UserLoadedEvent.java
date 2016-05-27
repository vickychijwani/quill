package me.vickychijwani.spectre.event;

import me.vickychijwani.spectre.model.entity.User;

public class UserLoadedEvent {

    public final User user;

    public UserLoadedEvent(User user) {
        this.user = user;
    }

}

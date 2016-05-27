package me.vickychijwani.spectre.event;

import me.vickychijwani.spectre.model.entity.Post;

public class PostSavedEvent {

    public final Post post;

    public PostSavedEvent(Post post) {
        this.post = post;
    }

}

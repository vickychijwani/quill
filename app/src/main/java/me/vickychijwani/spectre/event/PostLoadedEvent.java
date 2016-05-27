package me.vickychijwani.spectre.event;

import me.vickychijwani.spectre.model.entity.Post;

public class PostLoadedEvent {

    public final Post post;

    public PostLoadedEvent(Post post) {
        this.post = post;
    }

}

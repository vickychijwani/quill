package me.vickychijwani.spectre.event;

import me.vickychijwani.spectre.model.entity.Post;

public class PostCreatedEvent {

    public final Post newPost;

    public PostCreatedEvent(Post newPost) {
        this.newPost = newPost;
    }

}

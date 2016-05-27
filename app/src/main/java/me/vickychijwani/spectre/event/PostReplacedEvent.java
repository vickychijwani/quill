package me.vickychijwani.spectre.event;

import me.vickychijwani.spectre.model.entity.Post;

public class PostReplacedEvent {

    public final Post newPost;

    public PostReplacedEvent(Post newPost) {
        this.newPost = newPost;
    }

}

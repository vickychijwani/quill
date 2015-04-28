package me.vickychijwani.spectre.event;

import me.vickychijwani.spectre.model.Post;

public class CreatePostEvent {

    public final Post post;

    public CreatePostEvent(Post post) {
        this.post = post;
    }

}

package me.vickychijwani.spectre.event;

import me.vickychijwani.spectre.model.entity.Post;

public class DeletePostEvent {

    public final Post post;

    public DeletePostEvent(Post post) {
        this.post = post;
    }

}

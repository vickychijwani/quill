package me.vickychijwani.spectre.event;

import me.vickychijwani.spectre.model.Post;

public class SavePostEvent {

    public final Post post;

    public SavePostEvent(Post post) {
        this.post = post;
    }

}

package me.vickychijwani.spectre.event;

import me.vickychijwani.spectre.model.Post;

public class PostSavedEvent {

    public final Post post;
    public final boolean wasAutoSaved;

    public PostSavedEvent(Post post, boolean wasAutoSaved) {
        this.post = post;
        this.wasAutoSaved = wasAutoSaved;
    }

}

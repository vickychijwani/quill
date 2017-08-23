package me.vickychijwani.spectre.event;

public class PostDeletedEvent {

    public final String postId;

    public PostDeletedEvent(String postId) {
        this.postId = postId;
    }
}

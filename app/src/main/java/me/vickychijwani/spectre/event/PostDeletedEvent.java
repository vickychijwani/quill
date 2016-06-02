package me.vickychijwani.spectre.event;

public class PostDeletedEvent {

    public final int postId;

    public PostDeletedEvent(int postId) {
        this.postId = postId;
    }
}

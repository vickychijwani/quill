package me.vickychijwani.spectre.event;

public class SyncPostsEvent {

    public final boolean refreshPosts;

    public SyncPostsEvent(boolean refreshPosts) {
        this.refreshPosts = refreshPosts;
    }

}

package me.vickychijwani.spectre.event;

import java.util.List;

import me.vickychijwani.spectre.model.entity.Post;

public class PostsLoadedEvent {

    public final List<Post> posts;
    public final int postsFetchLimit;

    public PostsLoadedEvent(List<Post> posts, int postsFetchLimit) {
        this.posts = posts;
        this.postsFetchLimit = postsFetchLimit;
    }

}

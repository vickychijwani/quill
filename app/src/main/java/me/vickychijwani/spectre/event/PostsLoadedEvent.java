package me.vickychijwani.spectre.event;

import java.util.List;

import me.vickychijwani.spectre.model.Post;

public class PostsLoadedEvent {

    public final List<Post> posts;

    public PostsLoadedEvent(List<Post> posts) {
        this.posts = posts;
    }

}

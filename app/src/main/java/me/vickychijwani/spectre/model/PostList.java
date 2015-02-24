package me.vickychijwani.spectre.model;

import java.util.Arrays;
import java.util.List;

// dummy wrapper class needed for Retrofit
public class PostList {

    public List<Post> posts;

    public static PostList from(Post... posts) {
        PostList postList = new PostList();
        postList.posts = Arrays.asList(posts);
        return postList;
    }

}

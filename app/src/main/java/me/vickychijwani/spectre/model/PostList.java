package me.vickychijwani.spectre.model;

import android.support.annotation.NonNull;

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

    public boolean contains(@NonNull String uuid) {
        for (Post post : posts) {
            if (post.getUuid().equals(uuid)) {
                return true;
            }
        }
        return false;
    }

}

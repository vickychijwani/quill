package me.vickychijwani.spectre.network.entity;

import android.support.annotation.NonNull;

import java.util.Arrays;
import java.util.List;

import me.vickychijwani.spectre.model.entity.Post;

// dummy wrapper class needed for Retrofit
@SuppressWarnings("unused")
public class PostList {

    public List<Post> posts;

    public static PostList from(Post... posts) {
        PostList postList = new PostList();
        postList.posts = Arrays.asList(posts);
        return postList;
    }

    public boolean contains(@NonNull String id) {
        for (Post post : posts) {
            if (id.equals(post.getId())) {
                return true;
            }
        }
        return false;
    }

    public Post remove(int idx) {
        return posts.remove(idx);
    }

}

package me.vickychijwani.spectre.model;

import android.support.annotation.NonNull;

import java.util.Arrays;
import java.util.List;

// dummy wrapper class needed for Retrofit
@SuppressWarnings("unused")
public class PostList {

    public List<Post> posts;

    public static PostList from(Post... posts) {
        PostList postList = new PostList();
        postList.posts = Arrays.asList(posts);
        return postList;
    }

    public int indexOf(@NonNull String uuid) {
        for (int i = 0, numPosts = posts.size(); i < numPosts; ++i) {
            Post post = posts.get(i);
            if (uuid.equals(post.getUuid())) {
                return i;
            }
        }
        return -1;
    }

    public Post remove(int idx) {
        return posts.remove(idx);
    }

}

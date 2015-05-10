package me.vickychijwani.spectre.model;

import java.util.Arrays;
import java.util.List;

// dummy wrapper class needed for Retrofit
public class PostStubList {

    public List<PostStub> posts;

    public static PostStubList from(PostStub... stubs) {
        PostStubList stubList = new PostStubList();
        stubList.posts = Arrays.asList(stubs);
        return stubList;
    }

    public static PostStubList from(Post post, @Post.Status String status) {
        PostStubList stubList = new PostStubList();
        stubList.posts = Arrays.asList(new PostStub(post, status));
        return stubList;
    }

}

package me.vickychijwani.spectre.network.entity;

import java.util.Arrays;
import java.util.List;

import me.vickychijwani.spectre.model.entity.Post;

// dummy wrapper class needed for Retrofit
@SuppressWarnings({"WeakerAccess", "unused"})
public class PostStubList {

    public List<PostStub> posts;

    public static PostStubList from(PostStub... stubs) {
        PostStubList stubList = new PostStubList();
        stubList.posts = Arrays.asList(stubs);
        return stubList;
    }

    public static PostStubList from(Post post) {
        PostStubList stubList = new PostStubList();
        stubList.posts = Arrays.asList(new PostStub(post));
        return stubList;
    }

}

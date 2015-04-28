package me.vickychijwani.spectre.model;

import java.util.Arrays;
import java.util.List;

// dummy wrapper class needed for Retrofit
// exists only to help with the createPost API call
// TODO get rid of this as soon as Realm supports boxed primitives (Integer)
// https://github.com/realm/realm-java/issues/465
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

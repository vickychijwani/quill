package me.vickychijwani.spectre.model;

import android.support.annotation.NonNull;

// exists only to help with the createPost API call
// TODO get rid of this as soon as Realm supports boxed primitives (Integer)
// https://github.com/realm/realm-java/issues/465
public final class PostStub {

    public String title;
    public final String status = "draft";
    public String markdown;

    public PostStub(@NonNull Post post) {
        this.title = post.getTitle();
        this.markdown = post.getMarkdown();
    }

}

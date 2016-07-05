package me.vickychijwani.spectre.network.entity;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import me.vickychijwani.spectre.model.entity.Post;
import me.vickychijwani.spectre.model.entity.Tag;

@SuppressWarnings({"WeakerAccess", "unused"})
public final class PostStub {

    // field names should EXACTLY match those in Post class
    public final String title;
    public final String slug;
    public final String status;
    public final String markdown;
    public final List<TagStub> tags;
    public final String image;
    public final boolean featured;

    public PostStub(@NonNull Post post) {
        this.title = post.getTitle();
        this.slug = post.getSlug();
        this.status = post.getStatus();
        this.markdown = post.getMarkdown();
        this.tags = new ArrayList<>(post.getTags().size());
        for (Tag tag : post.getTags()) {
            this.tags.add(new TagStub(tag));
        }
        this.image = post.getImage();
        this.featured = post.isFeatured();
    }

}

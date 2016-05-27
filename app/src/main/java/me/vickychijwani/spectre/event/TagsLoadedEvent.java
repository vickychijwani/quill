package me.vickychijwani.spectre.event;

import java.util.List;

import me.vickychijwani.spectre.model.entity.Tag;

public class TagsLoadedEvent {

    public final List<Tag> tags;

    public TagsLoadedEvent(List<Tag> tags) {
        this.tags = tags;
    }

}

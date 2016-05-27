package me.vickychijwani.spectre.network.entity;

import android.support.annotation.NonNull;

import me.vickychijwani.spectre.model.entity.Tag;

@SuppressWarnings({"WeakerAccess", "unused"})
public class TagStub {

    public final String name;

    public TagStub(@NonNull Tag tag) {
        this.name = tag.getName();
    }

}

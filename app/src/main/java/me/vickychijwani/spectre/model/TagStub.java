package me.vickychijwani.spectre.model;

import android.support.annotation.NonNull;

public class TagStub {

    public final String name;

    public TagStub(@NonNull Tag tag) {
        this.name = tag.getName();
    }

}

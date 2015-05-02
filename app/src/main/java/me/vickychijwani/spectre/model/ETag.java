package me.vickychijwani.spectre.model;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.RealmClass;

@RealmClass
public class ETag extends RealmObject {

    @PrimaryKey
    private String type = "all_posts";
    private String tag;

    @SuppressWarnings("unused")
    public ETag() {}

    public ETag(String tag) {
        this.tag = tag;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

}

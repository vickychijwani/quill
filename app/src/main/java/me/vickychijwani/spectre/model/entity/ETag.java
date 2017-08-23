package me.vickychijwani.spectre.model.entity;

import android.support.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.RealmClass;
import io.realm.annotations.Required;

@RealmClass
public class ETag extends RealmObject {

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({ TYPE_ALL_POSTS, TYPE_CURRENT_USER, TYPE_BLOG_SETTINGS, TYPE_CONFIGURATION })
    public @interface Type {}

    public static final String TYPE_ALL_POSTS = "all_posts";
    public static final String TYPE_CURRENT_USER = "current_user";
    public static final String TYPE_BLOG_SETTINGS = "blog_settings";
    public static final String TYPE_CONFIGURATION = "configuration";

    @PrimaryKey
    private String type;

    @Required
    private String tag;

    @SuppressWarnings("unused")
    public ETag() {}

    public ETag(@Type String type, String tag) {
        this.type = type;
        this.tag = tag;
    }

    // NOTE: DO NOT ADD / MODIFY METHODS, SEE https://realm.io/docs/java/#faq
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

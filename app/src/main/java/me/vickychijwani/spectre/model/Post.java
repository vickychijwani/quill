package me.vickychijwani.spectre.model;

import android.support.annotation.NonNull;

import org.parceler.Parcel;

import java.util.Date;

import me.vickychijwani.spectre.util.AppUtils;

@Parcel
public class Post {

    public int id;
    public String title;
    public String slug;

    public String markdown;
    public String html;

    public Date created_at;
    public Date updated_at;
    public Date published_at;

    public String getAbsoluteUrl(@NonNull String baseUrl) {
        return AppUtils.pathJoin(baseUrl, slug);
    }

}

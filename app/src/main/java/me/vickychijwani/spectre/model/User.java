package me.vickychijwani.spectre.model;

import android.support.annotation.NonNull;

import java.util.Date;

import me.vickychijwani.spectre.util.AppUtils;

public class User {

    public int id;
    public String uuid;
    public String name;
    public String slug;

    public String email;
    public String image;
    public String bio;

    public Date created_at;
    public Date updated_at;

    public String getAbsoluteUrl(@NonNull String baseUrl) {
        return AppUtils.pathJoin(baseUrl, slug);
    }

}

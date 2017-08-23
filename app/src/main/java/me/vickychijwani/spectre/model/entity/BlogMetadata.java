package me.vickychijwani.spectre.model.entity;

import android.util.Base64;

import io.realm.RealmConfiguration;
import io.realm.RealmModel;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.RealmClass;

@RealmClass
public class BlogMetadata implements RealmModel {

    @PrimaryKey
    private String blogUrl;

    // may be false if, e.g., the OAuth code is known to be expired, in which case the user would
    // need to log in again
    private boolean loggedIn = true;

    // email and password are not @Required because they are absent in case of Ghost Auth
    private String email = null;

    private String password = null;

    private String authCode = null;

    private String permalinkFormat = "/:slug/";

    public BlogMetadata() {}

    public String getBlogUrl() {
        return blogUrl;
    }

    public void setBlogUrl(String blogUrl) {
        this.blogUrl = blogUrl;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public void setLoggedIn(boolean loggedIn) {
        this.loggedIn = loggedIn;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getAuthCode() {
        return authCode;
    }

    public void setAuthCode(String authCode) {
        this.authCode = authCode;
    }

    public String getPermalinkFormat() {
        return permalinkFormat;
    }

    public void setPermalinkFormat(String permalinkFormat) {
        this.permalinkFormat = permalinkFormat;
    }

    public RealmConfiguration getDataRealmConfig() {
        String encodedBlogUrl = Base64.encodeToString(blogUrl.getBytes(), Base64.URL_SAFE | Base64.NO_WRAP);
        return new RealmConfiguration.Builder()
                .name(encodedBlogUrl + ".realm")
                .build();
    }

}

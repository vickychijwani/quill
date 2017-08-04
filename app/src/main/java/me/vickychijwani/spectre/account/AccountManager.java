package me.vickychijwani.spectre.account;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.util.List;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmObject;
import io.realm.RealmResults;
import me.vickychijwani.spectre.SpectreApplication;
import me.vickychijwani.spectre.model.RealmUtils;
import me.vickychijwani.spectre.model.entity.BlogMetadata;
import me.vickychijwani.spectre.pref.UserPrefs;

public final class AccountManager {

    // methods for handling the "active blog"
    // the active blog is the one that the user has currently selected in the blog switcher UI
    public static boolean hasActiveBlog() {
        return !TextUtils.isEmpty(getActiveBlogUrl());
    }

    public static String getActiveBlogUrl() {
        return UserPrefs.getInstance(SpectreApplication.getInstance())
                .getString(UserPrefs.Key.ACTIVE_BLOG_URL);
    }

    @NonNull
    public static BlogMetadata getActiveBlog() throws IllegalStateException {
        String activeBlogUrl = getActiveBlogUrl();
        if (!hasActiveBlog()) {
            throw new IllegalStateException("There is no active blog");
        }
        final RealmResults<BlogMetadata> matchingBlogs = findAllBlogsMatchingUrl(activeBlogUrl);
        return Realm.getDefaultInstance().copyFromRealm(matchingBlogs.first());
    }

    public static void setActiveBlog(@NonNull String activeBlogUrl) {
        UserPrefs.getInstance(SpectreApplication.getInstance())
                .setString(UserPrefs.Key.ACTIVE_BLOG_URL, activeBlogUrl);
    }


    // methods for handling all blogs
    public static boolean hasBlog(@NonNull String blogUrl) {
        return !findAllBlogsMatchingUrl(blogUrl).isEmpty();
    }

    public static void addOrUpdateBlog(@NonNull BlogMetadata blog) {
        // add / update metadata
        RealmUtils.executeTransaction(Realm.getDefaultInstance(), realm -> {
            realm.copyToRealmOrUpdate(blog);
        });
        // we don't need to create a new data Realm right now, it'll be auto-created on first use
    }

    public static BlogMetadata getBlog(@NonNull String blogUrl) {
        final RealmResults<BlogMetadata> matchingBlogs = findAllBlogsMatchingUrl(blogUrl);
        return Realm.getDefaultInstance().copyFromRealm(matchingBlogs.first());
    }

    public static List<BlogMetadata> getAllBlogs() {
        final Realm realm = Realm.getDefaultInstance();
        return realm.copyFromRealm(realm
                .where(BlogMetadata.class)
                .findAll());
    }

    public static void deleteBlog(@NonNull String blogUrl) {
        RealmResults<BlogMetadata> matchingBlogs = findAllBlogsMatchingUrl(blogUrl);
        if (matchingBlogs.isEmpty()) {
            throw new IllegalStateException("No blog found matching the URL: " + blogUrl);
        }
        // we don't allow adding more than 1 blog with the same URL, so this should never happen
        if (matchingBlogs.size() > 1) {
            throw new IllegalStateException("More than 1 blog found matching the URL: " + blogUrl);
        }

        // delete blog metadata before data because data without metadata is harmless, but vice-versa is not
        // keep a copy of the metadata around so we can delete the data Realm after this
        final Realm realm = Realm.getDefaultInstance();
        BlogMetadata blogToDelete = matchingBlogs.get(0);
        RealmConfiguration dataRealmToDelete = realm.copyFromRealm(blogToDelete).getDataRealmConfig();
        RealmUtils.executeTransaction(realm, r -> {
            RealmObject.deleteFromRealm(blogToDelete);
        });

        // delete blog data
        Realm.deleteRealm(dataRealmToDelete);

        // if the active blog was deleted, set the active blog to a different one
        if (blogUrl.equals(getActiveBlogUrl())) {
            List<BlogMetadata> allBlogs = getAllBlogs();
            if (!allBlogs.isEmpty()) {
                setActiveBlog(allBlogs.get(0).getBlogUrl());
            } else {
                setActiveBlog("");
            }
        }
    }


    // private methods
    private static RealmResults<BlogMetadata> findAllBlogsMatchingUrl(String blogUrl) {
        return Realm.getDefaultInstance()
                .where(BlogMetadata.class)
                .equalTo("blogUrl", blogUrl)
                .findAll();
    }

}

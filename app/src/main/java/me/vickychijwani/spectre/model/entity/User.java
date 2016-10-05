package me.vickychijwani.spectre.model.entity;

import io.realm.RealmList;
import io.realm.RealmModel;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.RealmClass;
import io.realm.annotations.Required;

@SuppressWarnings("unused")
@RealmClass
public class User implements RealmModel {

    @PrimaryKey @Index
    private int id;

    @Required
    private String uuid;

    @Required
    private String name;

    @Required
    private String slug;

    @Required
    private String email;

    private String image;
    private String bio;

    private RealmList<Role> roles;

    // accessors
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public RealmList<Role> getRoles() {
        return roles;
    }

    public void setRoles(RealmList<Role> roles) {
        this.roles = roles;
    }

    // the way Ghost permissions work is, authors can only see and edit their own posts,
    // while all other roles can see and edits everybody's posts
    public boolean hasOnlyAuthorRole() {
        // a user can have multiple roles, so they should be restricted to author permissions
        // only if ALL their roles are "Author"
        boolean onlyAuthor = true;
        for (Role role : roles) {
            if (!"author".equalsIgnoreCase(role.getName())) {
                onlyAuthor = false;
            }
        }
        return onlyAuthor;
    }

}

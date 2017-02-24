package me.vickychijwani.spectre.model.entity;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import java.util.Date;

import io.realm.RealmModel;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.RealmClass;
import io.realm.annotations.Required;

@SuppressWarnings("unused")
@RealmClass
public class Tag implements RealmModel, Parcelable {

    @PrimaryKey
    private String id = null;

    @Required
    private String name;

    private String slug = null;
    private String description = null;
    private String image = null;
    private boolean hidden = false;

    private String metaTitle = null;
    private String metaDescription = null;

    private Date createdAt = null;
    private Date updatedAt = null;

    @SuppressWarnings("unused")
    public Tag() {}

    public Tag(String name) {
        this.name = name;
    }

    // TODO remember to update this, equals, Parcelable methods, and DB migration whenever fields are changed!
    public Tag(@NonNull Tag other) {
        this.setId(other.getId());
        this.setName(other.getName());
        this.setSlug(other.getSlug());
        this.setDescription(other.getDescription());
        this.setImage(other.getImage());
        this.setHidden(other.isHidden());
        this.setMetaTitle(other.getMetaTitle());
        this.setMetaDescription(other.getMetaDescription());
        this.setCreatedAt(other.getCreatedAt());
        this.setUpdatedAt(other.getUpdatedAt());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Tag tag = (Tag) o;
        if (isHidden() != tag.isHidden()) return false;
        if (getId() != null ? !getId().equals(tag.getId()) : tag.getId() != null)
            return false;
        if (getName() != null ? !getName().equals(tag.getName()) : tag.getName() != null)
            return false;
        if (getSlug() != null ? !getSlug().equals(tag.getSlug()) : tag.getSlug() != null)
            return false;
        if (getDescription() != null ? !getDescription().equals(tag.getDescription()) : tag.getDescription() != null)
            return false;
        if (getImage() != null ? !getImage().equals(tag.getImage()) : tag.getImage() != null)
            return false;
        if (getMetaTitle() != null ? !getMetaTitle().equals(tag.getMetaTitle()) : tag.getMetaTitle() != null)
            return false;
        if (getMetaDescription() != null ? !getMetaDescription().equals(tag.getMetaDescription()) : tag.getMetaDescription() != null)
            return false;
        if (getCreatedAt() != null ? !getCreatedAt().equals(tag.getCreatedAt()) : tag.getCreatedAt() != null)
            return false;
        return getUpdatedAt() != null ? getUpdatedAt().equals(tag.getUpdatedAt()) : tag.getUpdatedAt() == null;
    }

    // parcelable methods
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.id);
        dest.writeString(this.name);
        dest.writeString(this.slug);
        dest.writeString(this.description);
        dest.writeString(this.image);
        dest.writeByte(this.hidden ? (byte) 1 : (byte) 0);
        dest.writeString(this.metaTitle);
        dest.writeString(this.metaDescription);
        dest.writeLong(this.createdAt != null ? this.createdAt.getTime() : -1);
        dest.writeLong(this.updatedAt != null ? this.updatedAt.getTime() : -1);
    }

    protected Tag(Parcel in) {
        this.id = in.readString();
        this.name = in.readString();
        this.slug = in.readString();
        this.description = in.readString();
        this.image = in.readString();
        this.hidden = in.readByte() != 0;
        this.metaTitle = in.readString();
        this.metaDescription = in.readString();
        long tmpCreatedAt = in.readLong();
        this.createdAt = tmpCreatedAt == -1 ? null : new Date(tmpCreatedAt);
        long tmpUpdatedAt = in.readLong();
        this.updatedAt = tmpUpdatedAt == -1 ? null : new Date(tmpUpdatedAt);
    }

    public static final Parcelable.Creator<Tag> CREATOR = new Parcelable.Creator<Tag>() {
        @Override
        public Tag createFromParcel(Parcel source) {
            return new Tag(source);
        }

        @Override
        public Tag[] newArray(int size) {
            return new Tag[size];
        }
    };


    // accessors
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public String getMetaTitle() {
        return metaTitle;
    }

    public void setMetaTitle(String metaTitle) {
        this.metaTitle = metaTitle;
    }

    public String getMetaDescription() {
        return metaDescription;
    }

    public void setMetaDescription(String metaDescription) {
        this.metaDescription = metaDescription;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

}

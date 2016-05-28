package me.vickychijwani.spectre.model.entity;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;

import io.realm.RealmModel;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.RealmClass;
import io.realm.annotations.Required;

@SuppressWarnings("unused")
@RealmClass
public class Tag implements RealmModel, Parcelable {

    @PrimaryKey @Required
    private String uuid = null;

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


    // parcelable methods
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.uuid);
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
        this.uuid = in.readString();
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

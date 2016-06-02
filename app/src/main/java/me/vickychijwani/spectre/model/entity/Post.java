package me.vickychijwani.spectre.model.entity;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Date;

import io.realm.RealmList;
import io.realm.RealmModel;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.RealmClass;
import io.realm.annotations.Required;
import me.vickychijwani.spectre.model.GsonExclude;
import me.vickychijwani.spectre.util.DateTimeUtils;

@RealmClass
public class Post implements RealmModel, Parcelable {

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({ DRAFT, PUBLISHED })
    public @interface Status {}

    public static final String DRAFT = "draft";
    public static final String PUBLISHED = "published";

    private static final String DEFAULT_TITLE = "(Untitled)";
    public static final String DEFAULT_SLUG_PREFIX = "untitled";

    @PrimaryKey @Required
    private String uuid = null;
    private int id;

    @Required
    private String title = DEFAULT_TITLE;

    @Required
    private String slug = "";

    @Required @Status
    private String status = DRAFT;

    @Required
    private String markdown = "";

    private String html = "";

    private RealmList<Tag> tags;

    private String image = null;
    private boolean featured = false;
    private boolean page = false;

    @Required
    private String language = "en_US";

    private int author;
    private int createdBy;
    private int updatedBy;
    private int publishedBy;

    private Date createdAt = null;
    private Date publishedAt = DateTimeUtils.FAR_FUTURE;  // so that locally-created posts will be sorted to the top

    @Required
    private Date updatedAt = DateTimeUtils.FAR_FUTURE;  // so that locally-created posts will be sorted to the top

    private String metaTitle = "";
    private String metaDescription = "";

    // exclude from serialization / deserialization
    // NOTE: default values for these fields will be assigned to all serialized Posts (because they
    // are not touched by Retrofit), so don't assign any defaults specific to new posts here!
    @GsonExclude
    private RealmList<PendingAction> pendingActions = new RealmList<>();

    public Post() {}

    // TODO remember to update this, Parcelable methods, and DB migration whenever fields are changed!
    public Post(@NonNull Post post) {
        this.setUuid(post.getUuid());
        this.setId(post.getId());
        this.setTitle(post.getTitle());
        this.setSlug(post.getSlug());
        this.setStatus(post.getStatus());
        this.setMarkdown(post.getMarkdown());
        this.setHtml(post.getHtml());

        Tag[] tags = new Tag[post.getTags().size()];
        post.getTags().toArray(tags);
        this.setTags(new RealmList<>(tags));

        this.setImage(post.getImage());
        this.setFeatured(post.isFeatured());
        this.setPage(post.isPage());
        this.setLanguage(post.getLanguage());

        this.setAuthor(post.getAuthor());
        this.setCreatedBy(post.getCreatedBy());
        this.setUpdatedBy(post.getUpdatedBy());
        this.setPublishedBy(post.getPublishedBy());

        this.setCreatedAt(post.getCreatedAt());
        this.setUpdatedAt(post.getUpdatedAt());
        this.setPublishedAt(post.getPublishedAt());

        this.setMetaTitle(post.getMetaTitle());
        this.setMetaDescription(post.getMetaDescription());

        for (PendingAction action : post.getPendingActions()) {
            this.addPendingAction(action.getType());
        }
    }

    // Parcelable methods
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.uuid);
        dest.writeInt(this.id);
        dest.writeString(this.title);
        dest.writeString(this.slug);
        dest.writeString(this.status);
        dest.writeString(this.markdown);
        dest.writeString(this.html);
        dest.writeList(this.tags);
        dest.writeString(this.image);
        dest.writeByte(this.featured ? (byte) 1 : (byte) 0);
        dest.writeByte(this.page ? (byte) 1 : (byte) 0);
        dest.writeString(this.language);
        dest.writeInt(this.author);
        dest.writeInt(this.createdBy);
        dest.writeInt(this.updatedBy);
        dest.writeInt(this.publishedBy);
        dest.writeLong(this.createdAt != null ? this.createdAt.getTime() : -1);
        dest.writeLong(this.updatedAt != null ? this.updatedAt.getTime() : -1);
        dest.writeLong(this.publishedAt != null ? this.publishedAt.getTime() : -1);
        dest.writeString(this.metaTitle);
        dest.writeString(this.metaDescription);
        dest.writeList(this.pendingActions);
    }

    protected Post(Parcel in) {
        this.uuid = in.readString();
        this.id = in.readInt();
        this.title = in.readString();
        this.slug = in.readString();
        //noinspection WrongConstant
        this.status = in.readString();
        this.markdown = in.readString();
        this.html = in.readString();
        this.tags = new RealmList<>();
        in.readList(this.tags, Tag.class.getClassLoader());
        this.image = in.readString();
        this.featured = in.readByte() != 0;
        this.page = in.readByte() != 0;
        this.language = in.readString();
        this.author = in.readInt();
        this.createdBy = in.readInt();
        this.updatedBy = in.readInt();
        this.publishedBy = in.readInt();
        long tmpCreatedAt = in.readLong();
        this.createdAt = tmpCreatedAt == -1 ? null : new Date(tmpCreatedAt);
        long tmpUpdatedAt = in.readLong();
        this.updatedAt = tmpUpdatedAt == -1 ? null : new Date(tmpUpdatedAt);
        long tmpPublishedAt = in.readLong();
        this.publishedAt = tmpPublishedAt == -1 ? null : new Date(tmpPublishedAt);
        this.metaTitle = in.readString();
        this.metaDescription = in.readString();
        this.pendingActions = new RealmList<>();
        in.readList(this.pendingActions, PendingAction.class.getClassLoader());
    }

    public static final Parcelable.Creator<Post> CREATOR = new Parcelable.Creator<Post>() {
        @Override
        public Post createFromParcel(Parcel source) {
            return new Post(source);
        }

        @Override
        public Post[] newArray(int size) {
            return new Post[size];
        }
    };



    // accessors
    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public @Status String getStatus() {
        return status;
    }

    public void setStatus(@Status String status) {
        this.status = status;
    }

    public String getMarkdown() {
        return markdown;
    }

    public void setMarkdown(String markdown) {
        this.markdown = markdown;
    }

    public String getHtml() {
        return html;
    }

    public void setHtml(String html) {
        this.html = html;
    }

    public RealmList<Tag> getTags() {
     return tags;
    }

    public void setTags(RealmList<Tag> tags) {
        this.tags = tags;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public boolean isFeatured() {
        return featured;
    }

    public void setFeatured(boolean featured) {
        this.featured = featured;
    }

    public boolean isPage() {
        return page;
    }

    public void setPage(boolean page) {
        this.page = page;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public int getAuthor() {
        return author;
    }

    public void setAuthor(int author) {
        this.author = author;
    }

    public int getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(int createdBy) {
        this.createdBy = createdBy;
    }

    public int getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(int updatedBy) {
        this.updatedBy = updatedBy;
    }

    public int getPublishedBy() {
        return publishedBy;
    }

    public void setPublishedBy(int publishedBy) {
        this.publishedBy = publishedBy;
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

    public Date getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Date publishedAt) {
        this.publishedAt = publishedAt;
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

    public void setMetaDescription(String metaDesc) {
        this.metaDescription = metaDesc;
    }

    public RealmList<PendingAction> getPendingActions() {
        return pendingActions;
    }

    public void setPendingActions(RealmList<PendingAction> pendingActions) {
        this.pendingActions = pendingActions;
    }

    public boolean isPendingActionsEmpty() {
        return this.pendingActions.isEmpty();
    }

    public boolean hasPendingAction(@PendingAction.Type String type) {
        for (PendingAction action : getPendingActions()) {
            if (type.equals(action.getType())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Add a {@link PendingAction} to the given {@link Post}, if it doesn't already exist.
     * @param type the type of the pending action to add
     * @return true if the action was added now, false if it already existed
     */
    public boolean addPendingAction(@NonNull @PendingAction.Type String type) {
        if (hasPendingAction(type)) return false;
        getPendingActions().add(new PendingAction(type));
        return true;
    }

    public boolean isMarkedForDeletion() {
        return hasPendingAction(PendingAction.DELETE);
    }

}

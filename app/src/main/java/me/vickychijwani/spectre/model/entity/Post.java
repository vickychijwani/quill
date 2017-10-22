package me.vickychijwani.spectre.model.entity;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Date;
import java.util.List;

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
    @StringDef({ DRAFT, SCHEDULED, PUBLISHED })
    public @interface Status {}

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({ CONFLICT_NONE, CONFLICT_UNRESOLVED })
    public @interface ConflictState {}

    public static final String DRAFT = "draft";
    public static final String SCHEDULED = "scheduled";
    public static final String PUBLISHED = "published";

    public static final String CONFLICT_NONE = "conflict:none";
    public static final String CONFLICT_UNRESOLVED = "conflict:unresolved";

    private static final String DEFAULT_TITLE = "(Untitled)";
    public static final String DEFAULT_SLUG_PREFIX = "untitled";

    @PrimaryKey
    private String id;

    private String uuid = null;

    @Required
    private String title = DEFAULT_TITLE;

    @Required
    private String slug = "";

    @Required @Status
    private String status = DRAFT;

    @Required
    private String markdown = "";

    private String mobiledoc = "";

    private String html = "";

    private RealmList<Tag> tags;

    private String featureImage = null;
    private boolean featured = false;
    private boolean page = false;

    @Required
    private String language = "en_US";

    private String author;
    private String createdBy;
    private String updatedBy;
    private String publishedBy;

    private Date createdAt = null;
    private Date publishedAt = DateTimeUtils.FAR_FUTURE;  // so that locally-created posts will be sorted to the top

    @Required
    private Date updatedAt = DateTimeUtils.FAR_FUTURE;  // so that locally-created posts will be sorted to the top

    private String metaTitle = "";
    private String metaDescription = "";

    private String customExcerpt = null;

    // exclude from serialization / deserialization
    // NOTE: default values for these fields will be assigned to all serialized Posts (because they
    // are not touched by Retrofit), so don't assign any defaults specific to new posts here!
    @GsonExclude
    private RealmList<PendingAction> pendingActions = new RealmList<>();

    @Required @GsonExclude @ConflictState
    private String conflictState = CONFLICT_NONE;

    public Post() {}

    // TODO remember to update this, equals, Parcelable methods, PostUtils.isDirty() and DB migration whenever fields are changed!
    public Post(@NonNull Post post) {
        this.setId(post.getId());
        this.setUuid(post.getUuid());
        this.setTitle(post.getTitle());
        this.setSlug(post.getSlug());
        this.setStatus(post.getStatus());
        this.setMarkdown(post.getMarkdown());
        this.setMobiledoc(post.getMobiledoc());
        this.setHtml(post.getHtml());

        List<Tag> realmTags = post.getTags();
        RealmList<Tag> unmanagedTags = new RealmList<>();
        for (Tag realmTag : realmTags) {
            unmanagedTags.add(new Tag(realmTag));
        }
        this.setTags(unmanagedTags);

        this.setFeatureImage(post.getFeatureImage());
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

        this.setCustomExcerpt(post.getCustomExcerpt());

        for (PendingAction action : post.getPendingActions()) {
            this.addPendingAction(action.getType());
        }
        this.setConflictState(post.getConflictState());
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Post post = (Post) o;
        if (isFeatured() != post.isFeatured()) return false;
        if (isPage() != post.isPage()) return false;
        if (getId() != null ? !getId().equals(post.getId()) : post.getId() != null)
            return false;
        if (getUuid() != null ? !getUuid().equals(post.getUuid()) : post.getUuid() != null)
            return false;
        if (getTitle() != null ? !getTitle().equals(post.getTitle()) : post.getTitle() != null)
            return false;
        if (getSlug() != null ? !getSlug().equals(post.getSlug()) : post.getSlug() != null)
            return false;
        if (getStatus() != null ? !getStatus().equals(post.getStatus()) : post.getStatus() != null)
            return false;
        if (getMarkdown() != null ? !getMarkdown().equals(post.getMarkdown()) : post.getMarkdown() != null)
            return false;
        if (getMobiledoc() != null ? !getMobiledoc().equals(post.getMobiledoc()) : post.getMobiledoc() != null)
            return false;
        if (getHtml() != null ? !getHtml().equals(post.getHtml()) : post.getHtml() != null)
            return false;
        if (getTags() != null ? !getTags().equals(post.getTags()) : post.getTags() != null)
            return false;
        if (getFeatureImage() != null ? !getFeatureImage().equals(post.getFeatureImage()) : post.getFeatureImage() != null)
            return false;
        if (getLanguage() != null ? !getLanguage().equals(post.getLanguage()) : post.getLanguage() != null)
            return false;
        if (getAuthor() != null ? !getAuthor().equals(post.getAuthor()) : post.getAuthor() != null)
            return false;
        if (getCreatedBy() != null ? !getCreatedBy().equals(post.getCreatedBy()) : post.getCreatedBy() != null)
            return false;
        if (getUpdatedBy() != null ? !getUpdatedBy().equals(post.getUpdatedBy()) : post.getUpdatedBy() != null)
            return false;
        if (getPublishedBy() != null ? !getPublishedBy().equals(post.getPublishedBy()) : post.getPublishedBy() != null)
            return false;
        if (getCreatedAt() != null ? !getCreatedAt().equals(post.getCreatedAt()) : post.getCreatedAt() != null)
            return false;
        if (getPublishedAt() != null ? !getPublishedAt().equals(post.getPublishedAt()) : post.getPublishedAt() != null)
            return false;
        if (getUpdatedAt() != null ? !getUpdatedAt().equals(post.getUpdatedAt()) : post.getUpdatedAt() != null)
            return false;
        if (getMetaTitle() != null ? !getMetaTitle().equals(post.getMetaTitle()) : post.getMetaTitle() != null)
            return false;
        if (getMetaDescription() != null ? !getMetaDescription().equals(post.getMetaDescription()) : post.getMetaDescription() != null)
            return false;
        if (getCustomExcerpt() != null ? !getCustomExcerpt().equals(post.getCustomExcerpt()) : post.getCustomExcerpt() != null)
            return false;
        if (getPendingActions() != null ? !getPendingActions().equals(post.getPendingActions()) : post.getPendingActions() != null)
            return false;
        if (getConflictState() != null ? !getConflictState().equals(post.getConflictState()) : post.getConflictState() != null)
            return false;
        return true;
    }

    // Parcelable methods
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.id);
        dest.writeString(this.uuid);
        dest.writeString(this.title);
        dest.writeString(this.slug);
        dest.writeString(this.status);
        dest.writeString(this.markdown);
        dest.writeString(this.mobiledoc);
        dest.writeString(this.html);
        dest.writeList(this.tags);
        dest.writeString(this.featureImage);
        dest.writeByte(this.featured ? (byte) 1 : (byte) 0);
        dest.writeByte(this.page ? (byte) 1 : (byte) 0);
        dest.writeString(this.language);
        dest.writeString(this.author);
        dest.writeString(this.createdBy);
        dest.writeString(this.updatedBy);
        dest.writeString(this.publishedBy);
        dest.writeLong(this.createdAt != null ? this.createdAt.getTime() : -1);
        dest.writeLong(this.updatedAt != null ? this.updatedAt.getTime() : -1);
        dest.writeLong(this.publishedAt != null ? this.publishedAt.getTime() : -1);
        dest.writeString(this.metaTitle);
        dest.writeString(this.metaDescription);
        dest.writeString(this.customExcerpt);
        dest.writeList(this.pendingActions);
        dest.writeString(this.conflictState);
    }

    protected Post(Parcel in) {
        this.id = in.readString();
        this.uuid = in.readString();
        this.title = in.readString();
        this.slug = in.readString();
        //noinspection WrongConstant
        this.status = in.readString();
        this.markdown = in.readString();
        this.mobiledoc = in.readString();
        this.html = in.readString();
        this.tags = new RealmList<>();
        in.readList(this.tags, Tag.class.getClassLoader());
        this.featureImage = in.readString();
        this.featured = in.readByte() != 0;
        this.page = in.readByte() != 0;
        this.language = in.readString();
        this.author = in.readString();
        this.createdBy = in.readString();
        this.updatedBy = in.readString();
        this.publishedBy = in.readString();
        long tmpCreatedAt = in.readLong();
        this.createdAt = tmpCreatedAt == -1 ? null : new Date(tmpCreatedAt);
        long tmpUpdatedAt = in.readLong();
        this.updatedAt = tmpUpdatedAt == -1 ? null : new Date(tmpUpdatedAt);
        long tmpPublishedAt = in.readLong();
        this.publishedAt = tmpPublishedAt == -1 ? null : new Date(tmpPublishedAt);
        this.metaTitle = in.readString();
        this.metaDescription = in.readString();
        this.customExcerpt = in.readString();
        this.pendingActions = new RealmList<>();
        in.readList(this.pendingActions, PendingAction.class.getClassLoader());
        //noinspection WrongConstant
        this.conflictState = in.readString();
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
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
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

    public String getMobiledoc() {
        return mobiledoc;
    }

    public void setMobiledoc(String mobiledoc) {
        this.mobiledoc = mobiledoc;
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

    public String getFeatureImage() {
        return featureImage;
    }

    public void setFeatureImage(String featureImage) {
        this.featureImage = featureImage;
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

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public String getPublishedBy() {
        return publishedBy;
    }

    public void setPublishedBy(String publishedBy) {
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

    public String getCustomExcerpt() {
        return customExcerpt;
    }

    public void setCustomExcerpt(String customExcerpt) {
        this.customExcerpt = customExcerpt;
    }

    public RealmList<PendingAction> getPendingActions() {
        return pendingActions;
    }

    public void setPendingActions(RealmList<PendingAction> pendingActions) {
        this.pendingActions = pendingActions;
    }

    public @ConflictState String getConflictState() {
        return conflictState;
    }

    public void setConflictState(@ConflictState String conflictState) {
        this.conflictState = conflictState;
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

    public boolean isDraft() {
        return getStatus().equals(DRAFT);
    }

    public boolean isScheduled() {
        return getStatus().equals(SCHEDULED);
    }

    public boolean isPublished() {
        return getStatus().equals(PUBLISHED);
    }

}

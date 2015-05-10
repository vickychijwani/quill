package me.vickychijwani.spectre.model;

import android.support.annotation.NonNull;
import android.support.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Date;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.RealmClass;
import me.vickychijwani.spectre.util.DateTimeUtils;

@RealmClass
public class Post extends RealmObject {

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({ LOCAL_NEW, DRAFT, PUBLISHED })
    public @interface Status {}

    public static final String LOCAL_NEW = "local_new";
    public static final String DRAFT = "draft";
    public static final String PUBLISHED = "published";

    @PrimaryKey
    private String uuid = null;
    private int id;
    private String title = "(Untitled)";
    private String slug = null;
    @Status private String status = LOCAL_NEW;

    private String markdown = "";
    private String html = "";

    private RealmList<Tag> tags;

    private Date createdAt = null;
    private Date updatedAt = DateTimeUtils.FAR_FUTURE;  // so that locally-created posts will be sorted to the top
    private Date publishedAt = DateTimeUtils.FAR_FUTURE;  // so that locally-created posts will be sorted to the top

    // these fields only exist in the db, never in API calls
    private transient boolean isUploaded = false;   // temporary flag
    private transient RealmList<PendingAction> pendingActions = new RealmList<>();

    public Post() {}

    // TODO remember to update this whenever a new field is added!
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

        this.setCreatedAt(post.getCreatedAt());
        this.setUpdatedAt(post.getUpdatedAt());
        this.setPublishedAt(post.getPublishedAt());

        PendingAction[] pendingActions = new PendingAction[post.getPendingActions().size()];
        post.getPendingActions().toArray(pendingActions);
        this.setPendingActions(new RealmList<>(pendingActions));
    }

    // NOTE: DO NOT ADD / MODIFY METHODS, SEE https://realm.io/docs/java/#faq
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

    public boolean isUploaded() {
        return isUploaded;
    }

    public void setUploaded(boolean isUploaded) {
        this.isUploaded = isUploaded;
    }

    public RealmList<PendingAction> getPendingActions() {
        return pendingActions;
    }

    public void setPendingActions(RealmList<PendingAction> pendingActions) {
        this.pendingActions = pendingActions;
    }

}

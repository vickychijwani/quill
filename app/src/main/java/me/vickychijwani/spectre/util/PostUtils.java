package me.vickychijwani.spectre.util;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;

import java.util.Comparator;
import java.util.List;

import me.vickychijwani.spectre.R;
import me.vickychijwani.spectre.SpectreApplication;
import me.vickychijwani.spectre.model.entity.PendingAction;
import me.vickychijwani.spectre.model.entity.Post;
import me.vickychijwani.spectre.model.entity.Tag;
import me.vickychijwani.spectre.pref.UserPrefs;

public class PostUtils {

    /**
     * Sort order:
     * 1. New posts that are yet to be created on the server, sorted by updatedAt
     * 2. Scheduled posts, sorted by publishedAt
     * 3. Drafts, sorted by updatedAt
     * 4. Published posts, sorted by publishedAt
     *
     * See Ghost Admin's sort order in the file core/server/models/post.js, search for 'orderDefaultOptions'
     */
    @SuppressWarnings("unused")
    public static Comparator<Post> COMPARATOR_MAIN_LIST = (lhs, rhs) -> {
        boolean isLhsNew = lhs.hasPendingAction(PendingAction.CREATE);
        boolean isRhsNew = rhs.hasPendingAction(PendingAction.CREATE);
        boolean isLhsScheduled = lhs.isScheduled();
        boolean isRhsScheduled = rhs.isScheduled();
        boolean isLhsDraft = lhs.isDraft();
        boolean isRhsDraft = rhs.isDraft();
        boolean isLhsPublished = lhs.isPublished();
        boolean isRhsPublished = rhs.isPublished();

        if (isLhsNew && !isRhsNew) return -1;
        if (isRhsNew && !isLhsNew) return 1;

        if (isLhsScheduled && !isRhsScheduled) return -1;
        if (isRhsScheduled && !isLhsScheduled) return 1;

        if (isLhsDraft && !isRhsDraft) return -1;
        if (isRhsDraft && !isLhsDraft) return 1;

        if (isLhsPublished && !isRhsPublished) return -1;
        if (isRhsPublished && !isLhsPublished) return 1;

        // at this point, we know both lhs and rhs belong to the same group (new, scheduled, drafts,
        // published). NOTE: (-) sign because we want to sort in reverse chronological order.
        if (isLhsPublished || isLhsScheduled) {
            // use date published for sorting scheduled or published posts ...
            return -lhs.getPublishedAt().compareTo(rhs.getPublishedAt());
        }
        // ... else use date modified (for drafts and new posts)
        return -lhs.getUpdatedAt().compareTo(rhs.getUpdatedAt());
    };

    @SuppressWarnings({"RedundantIfStatement", "OverlyComplexMethod"})
    public static boolean isDirty(@NonNull Post original, @NonNull Post current) {
        boolean bothImagesNull = (original.getImage() == null && current.getImage() == null);
        boolean oneImageNull = (original.getImage() != null && current.getImage() == null)
                || (original.getImage() == null && current.getImage() != null);

        if (! original.getTitle().equals(current.getTitle()))
            return true;
        if (! original.getStatus().equals(current.getStatus()))
            return true;
        if (! original.getSlug().equals(current.getSlug()))
            return true;
        if (! original.getMarkdown().equals(current.getMarkdown()))
            return true;
        if (!bothImagesNull && (oneImageNull || !original.getImage().equals(current.getImage())))
            return true;
        if (original.getTags().size() != current.getTags().size())
            return true;
        if (! tagListsMatch(original.getTags(), current.getTags()))
            return true;
        return false;
    }

    public static String getPostUrl(@Nullable Post post) {
        if (post == null) throw new IllegalArgumentException("post cannot be null!");
        UserPrefs prefs = UserPrefs.getInstance(SpectreApplication.getInstance());
        String blogUrl = prefs.getString(UserPrefs.Key.BLOG_URL);
        if (post.isPublished()) {
            return NetworkUtils.makeAbsoluteUrl(blogUrl, post.getSlug());
        } else if (post.isDraft() || post.isScheduled()) {
            return NetworkUtils.makeAbsoluteUrl(blogUrl, "p/" + post.getUuid());
        } else {
            throw new IllegalArgumentException("URL not available for this post!");
        }
    }

    public static String getStatusString(@Nullable Post post, @NonNull Context context) {
        if (post == null) throw new IllegalArgumentException("post cannot be null!");
        String status;
        if (post.isMarkedForDeletion()) {
            status = context.getString(R.string.status_marked_for_deletion);
        } else if (post.hasPendingAction(PendingAction.EDIT_LOCAL)) {
            status = context.getString(R.string.status_published_auto_saved);
        } else if (! post.isPendingActionsEmpty()) {
            status = context.getString(R.string.status_offline_changes);
        } else if (post.isPublished()) {
            String dateStr = DateTimeUtils.formatRelative(post.getPublishedAt());
            status = context.getString(R.string.status_published, dateStr);
        } else if (post.isDraft()) {
            status = context.getString(R.string.status_draft);
        } else if (post.isScheduled()) {
            String dateStr = DateTimeUtils.formatRelative(post.getPublishedAt());
            status = context.getString(R.string.status_scheduled, dateStr);
        } else {
            throw new IllegalArgumentException("unknown post status!");
        }
        return status;
    }

    public static int getStatusColor(@Nullable Post post, @NonNull Context context) {
        if (post == null) throw new IllegalArgumentException("post cannot be null!");
        int colorId;
        if (post.hasPendingAction(PendingAction.DELETE)) {
            colorId = R.color.status_deleted;
        } else if (post.hasPendingAction(PendingAction.EDIT_LOCAL)) {
            colorId = R.color.status_published_auto_saved;
        } else if (! post.isPendingActionsEmpty()) {
            colorId = R.color.status_offline_changes;
        } else if (post.isPublished()) {
            colorId = R.color.status_published;
        } else if (post.isDraft()) {
            colorId = R.color.status_draft;
        } else if (post.isScheduled()) {
            colorId = R.color.status_scheduled;
        } else {
            throw new IllegalArgumentException("unknown post status!");
        }
        return ContextCompat.getColor(context, colorId);
    }


    // private functions
    private static boolean tagListsMatch(List<Tag> tags1, List<Tag> tags2) {
        for (Tag tag1 : tags1)
            if (! tagListContains(tags2, tag1))
                return false;
        for (Tag tag2 : tags2)
            if (! tagListContains(tags1, tag2))
                return false;
        return true;
    }

    private static boolean tagListContains(List<Tag> haystack, Tag needle) {
        for (Tag hay : haystack)
            if (needle.getName().equals(hay.getName()))
                return true;
        return false;
    }

}

package me.vickychijwani.spectre.util;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Comparator;
import java.util.List;

import me.vickychijwani.spectre.R;
import me.vickychijwani.spectre.SpectreApplication;
import me.vickychijwani.spectre.model.PendingAction;
import me.vickychijwani.spectre.model.Post;
import me.vickychijwani.spectre.model.Tag;
import me.vickychijwani.spectre.pref.UserPrefs;

// TODO some of these methods belong in the Post class but Realm doesn't allow arbitrary methods on
// RealmObjects at the moment
public class PostUtils {

    /**
     * Sort order:
     * 1. New posts that are yet to be created on the server, sorted by updatedAt
     * 2. Drafts, sorted by updatedAt
     * 3. Published posts, sorted by publishedAt
     */
    @SuppressWarnings("unused")
    public static Comparator<Post> COMPARATOR_MAIN_LIST = (lhs, rhs) -> {
        boolean isLhsNew = hasPendingAction(lhs, PendingAction.CREATE);
        boolean isRhsNew = hasPendingAction(rhs, PendingAction.CREATE);
        boolean isLhsDraft = Post.DRAFT.equals(lhs.getStatus());
        boolean isRhsDraft = Post.DRAFT.equals(rhs.getStatus());
        boolean isLhsPublished = Post.PUBLISHED.equals(lhs.getStatus());
        boolean isRhsPublished = Post.PUBLISHED.equals(rhs.getStatus());
        if (isLhsNew && !isRhsNew) return -1;
        if (isRhsNew && !isLhsNew) return 1;
        if (isLhsDraft && !isRhsDraft) return -1;
        if (isRhsDraft && !isLhsDraft) return 1;
        // at this point, we know both lhs and rhs belong to the same group (new, drafts, published)
        // NOTE: (-) sign because we want to sort in reverse chronological order
        if (isLhsPublished && isRhsPublished) {
            // use date published for sorting published posts ...
            return -lhs.getPublishedAt().compareTo(rhs.getPublishedAt());
        }
        // ... else use date modified (for drafts and new posts)
        return -lhs.getUpdatedAt().compareTo(rhs.getUpdatedAt());
    };

    @SuppressWarnings("RedundantIfStatement")
    public static boolean isDirty(@NonNull Post original, @NonNull Post current) {
        if (! original.getTitle().equals(current.getTitle()))
            return true;
        if (! original.getStatus().equals(current.getStatus()))
            return true;
        if (! original.getSlug().equals(current.getSlug()))
            return true;
        if (! original.getMarkdown().equals(current.getMarkdown()))
            return true;
        if (! original.getImage().equals(current.getImage()))
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
        if (Post.PUBLISHED.equals(post.getStatus())) {
            return AppUtils.pathJoin(blogUrl, post.getSlug());
        } else if (Post.DRAFT.equals(post.getStatus())) {
            return AppUtils.pathJoin(blogUrl, "p/" + post.getUuid());
        } else {
            throw new IllegalArgumentException("URLs only available for drafts and published posts!");
        }
    }

    /**
     * Add a {@link PendingAction} to the given {@link Post}, if it doesn't already exist.
     * @param post the post to which to add the action
     * @param type the type of the pending action to add
     * @return true if the action was added now, false if it already existed
     */
    public static boolean addPendingAction(@Nullable Post post, @Nullable @PendingAction.Type String type) {
        if (post == null) throw new IllegalArgumentException("post cannot be null!");
        if (type == null) throw new IllegalArgumentException("pending action type cannot be null!");
        if (hasPendingAction(post, type)) return false;
        post.getPendingActions().add(new PendingAction(type));
        return true;
    }

    public static String getStatusString(@Nullable Post post, @NonNull Context context) {
        if (post == null) throw new IllegalArgumentException("post cannot be null!");
        String status;
        if (hasPendingAction(post, PendingAction.EDIT_LOCAL)) {
            status = context.getString(R.string.published_auto_saved);
        } else if (! post.getPendingActions().isEmpty()) {
            status = context.getString(R.string.offline_changes);
        } else if (Post.PUBLISHED.equals(post.getStatus())) {
            String dateStr = DateTimeUtils.formatRelative(post.getPublishedAt());
            status = String.format(context.getString(R.string.published), dateStr);
        } else if (Post.DRAFT.equals(post.getStatus())) {
            status = context.getString(R.string.draft);
        } else {
            throw new IllegalArgumentException("unknown post status!");
        }
        return status;
    }

    public static int getStatusColor(@Nullable Post post, @NonNull Context context) {
        if (post == null) throw new IllegalArgumentException("post cannot be null!");
        int colorId;
        if (hasPendingAction(post, PendingAction.EDIT_LOCAL)) {
            colorId = R.color.published_auto_saved;
        } else if (! post.getPendingActions().isEmpty()) {
            colorId = R.color.offline_changes;
        } else if (Post.PUBLISHED.equals(post.getStatus())) {
            colorId = R.color.published;
        } else if (Post.DRAFT.equals(post.getStatus())) {
            colorId = R.color.draft;
        } else {
            throw new IllegalArgumentException("unknown post status!");
        }
        return context.getResources().getColor(colorId);
    }

    public static boolean hasPendingAction(Post post, @PendingAction.Type String type) {
        for (PendingAction action : post.getPendingActions()) {
            if (type.equals(action.getType())) {
                return true;
            }
        }
        return false;
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

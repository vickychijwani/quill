package me.vickychijwani.spectre.util;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;

import me.vickychijwani.spectre.R;
import me.vickychijwani.spectre.model.PendingAction;
import me.vickychijwani.spectre.model.Post;
import me.vickychijwani.spectre.model.Tag;

// TODO this class exists only because Realm doesn't allow arbitrary methods on Post at the moment
public class PostUtils {

    @SuppressWarnings("RedundantIfStatement")
    public static boolean isDirty(@NonNull Post original, @NonNull Post current) {
        if (! original.getTitle().equals(current.getTitle()))
            return true;
        if (! original.getSlug().equals(current.getSlug()))
            return true;
        if (! original.getMarkdown().equals(current.getMarkdown()))
            return true;
        if (original.getTags().size() != current.getTags().size())
            return true;
        if (! tagListsMatch(original.getTags(), current.getTags()))
            return true;
        return false;
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

        List<PendingAction> pendingActions = post.getPendingActions();
        for (PendingAction action : pendingActions) {
            if (type.equals(action.getType())) {
                return false;
            }
        }
        pendingActions.add(new PendingAction(type));
        return true;
    }

    public static String getStatusString(@Nullable Post post, @NonNull Context context) {
        if (post == null) throw new IllegalArgumentException("post cannot be null!");
        String status;
        if (! post.getPendingActions().isEmpty()) {
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
        if (! post.getPendingActions().isEmpty()) {
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

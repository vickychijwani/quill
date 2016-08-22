package me.vickychijwani.spectre.view.fragments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.github.slugify.Slugify;
import com.squareup.otto.Subscribe;

import java.io.IOException;

import butterknife.Bind;
import butterknife.ButterKnife;
import io.realm.RealmList;
import me.vickychijwani.spectre.R;
import me.vickychijwani.spectre.analytics.AnalyticsService;
import me.vickychijwani.spectre.event.FileUploadErrorEvent;
import me.vickychijwani.spectre.event.FileUploadEvent;
import me.vickychijwani.spectre.event.FileUploadedEvent;
import me.vickychijwani.spectre.event.PostSavedEvent;
import me.vickychijwani.spectre.event.PostSyncedEvent;
import me.vickychijwani.spectre.event.SavePostEvent;
import me.vickychijwani.spectre.model.entity.PendingAction;
import me.vickychijwani.spectre.model.entity.Post;
import me.vickychijwani.spectre.model.entity.Tag;
import me.vickychijwani.spectre.util.EditTextSelectionState;
import me.vickychijwani.spectre.util.EditTextUtils;
import me.vickychijwani.spectre.util.KeyboardUtils;
import me.vickychijwani.spectre.util.PostUtils;
import me.vickychijwani.spectre.view.BundleKeys;
import me.vickychijwani.spectre.view.FormatOptionClickListener;
import me.vickychijwani.spectre.view.Observables;
import me.vickychijwani.spectre.view.PostViewActivity;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Actions;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;

public class PostEditFragment extends BaseFragment implements
        FormatOptionClickListener {

    private static final String TAG = "PostEditFragment";

    private enum SaveScenario {
        UNKNOWN,
        PUBLISH_DRAFT,
        UNPUBLISH_PUBLISHED_POST,
        AUTO_SAVE_PUBLISHED_POST,
        EXPLICITLY_UPDATE_PUBLISHED_POST,
        AUTO_SAVE_DRAFT,
        EXPLICITLY_SAVE_DRAFT,                      // unused; feature was removed from UI as it seemed unnecessary
        AUTO_SAVE_SCHEDULED_POST,
        EXPLICITLY_UPDATE_SCHEDULED_POST,
    }

    @Bind(R.id.post_title_edit)             EditText mPostTitleEditView;
    @Bind(R.id.post_markdown)               EditText mPostEditView;

    private PostViewActivity mActivity;
    private PostSettingsManager mPostSettingsManager;

    private Post mOriginalPost;     // copy of post since the time it was opened for editing
    private Post mLastSavedPost;    // copy of post since it was last saved
    private Post mPost;             // current copy of post in memory

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private boolean mbDiscardChanges = false;

    private SaveScenario mSaveScenario = SaveScenario.UNKNOWN;
    private Runnable mSaveTimeoutRunnable;
    private static final int SAVE_TIMEOUT = 5 * 1000;       // milliseconds

    private boolean mPostChangedInMemory = false;
    private PostTextWatcher mPostTextWatcher = null;

    // image insert / upload
    private static final int REQUEST_CODE_IMAGE_PICK = 1;
    private Subscription mUploadSubscription = null;
    private ProgressDialog mUploadProgress = null;
    private EditTextSelectionState mMarkdownEditSelectionState;
    private boolean mbFileStorageEnabled = true;
    private Action1<String> mImageUploadDoneAction = null;


    @SuppressWarnings("unused")
    public static PostEditFragment newInstance(@NonNull Post post, boolean fileStorageEnabled) {
        PostEditFragment fragment = new PostEditFragment();
        Bundle args = new Bundle();
        args.putParcelable(BundleKeys.POST, post);
        args.putBoolean(BundleKeys.FILE_STORAGE_ENABLED, fileStorageEnabled);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_post_edit, container, false);
        ButterKnife.bind(this, view);

        mActivity = ((PostViewActivity) getActivity());
        mPostSettingsManager = mActivity;

        Bundle args = new Bundle(getArguments());   // defaults, given during original Fragment construction
        if (savedInstanceState != null) {
            args.putAll(savedInstanceState);        // overrides, for things that could've changed, and for new things like custom UI state
        }
        mbFileStorageEnabled = args.getBoolean(BundleKeys.FILE_STORAGE_ENABLED,
                mbFileStorageEnabled);
        if (args.containsKey(BundleKeys.POST_EDITED)) {
            mPostChangedInMemory = args.getBoolean(BundleKeys.POST_EDITED);
        }

        //noinspection ConstantConditions
        setPost(args.getParcelable(BundleKeys.POST), true);

        // must occur after setPost() to prevent being triggered when setting the post initially
        startMonitoringPostSettings();

        mSaveTimeoutRunnable = () -> {
            View parent = PostEditFragment.this.getView();
            if (parent != null) {
                Snackbar.make(parent, R.string.save_post_timeout, Snackbar.LENGTH_SHORT).show();
                mSaveScenario = SaveScenario.UNKNOWN;
            }
        };

        // title
        mActivity.setTitle(null);
        // hack for word wrap with "Done" IME action! see http://stackoverflow.com/a/13563946/504611
        mPostTitleEditView.setHorizontallyScrolling(false);
        mPostTitleEditView.setMaxLines(Integer.MAX_VALUE);

        setHasOptionsMenu(true);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        setPost(mPost, false);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(BundleKeys.POST, mPost);
        outState.putBoolean(BundleKeys.POST_EDITED, mPostChangedInMemory);
    }

    @Override
    public void onPause() {
        // remove pending callbacks
        mHandler.removeCallbacks(mSaveTimeoutRunnable);
        // persist changes to disk, unless the user opted to discard those changes
        saveAutomatically();
        // must call super method AFTER saving, else we won't get the PostSavedEvent reply!
        super.onPause();
        // unsubscribe from observable
        if (mUploadSubscription != null) {
            mUploadSubscription.unsubscribe();
            mUploadSubscription = null;
            Toast.makeText(mActivity, R.string.image_upload_failed, Toast.LENGTH_SHORT).show();
        }
        if (mUploadProgress != null) {
            mUploadProgress.dismiss();
            mUploadProgress = null;
        }
    }

    @Override
    public void onDestroyView() {
        // avoid leaking the activity (listeners hold a strong reference to it)
        stopMonitoringPostSettings();
        super.onDestroyView();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (mbFileStorageEnabled) {
            inflater.inflate(R.menu.post_edit_file_storage_enabled, menu);
        } else {
            inflater.inflate(R.menu.post_edit_file_storage_disabled, menu);
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
//         saveToMemory();   // make sure user changes are stored in mPost before computing diff
//         boolean isPostDirty = PostUtils.isDirty(mOriginalPost, mPost);
//         menu.findItem(R.id.action_discard).setVisible(isPostDirty);
    }

    // TODO consider moving this and other logic out into a View-Model
    public boolean shouldShowPublishAction() {
        // show the publish action for drafts and for locally-edited published posts
        if (mPost.isDraft()
                || mPost.hasPendingAction(PendingAction.EDIT_LOCAL)
                || mPostChangedInMemory) {
            stopMonitoringPostSettings();
            return true;
        } else {
            // published post with no auto-saved edits (may have unsynced published edits though)
            startMonitoringPostSettings();
            return false;
        }
    }

    public boolean shouldShowUnpublishAction() {
        return mPost.isPublished();
    }

    private void startMonitoringPostSettings() {
        if (mPostTextWatcher == null) {
            mPostTextWatcher = new PostTextWatcher();
            mPostTitleEditView.addTextChangedListener(mPostTextWatcher);
            mPostEditView.addTextChangedListener(mPostTextWatcher);
        }
        // this is safe to do multiple times as it is idempotent (even though a new instance is created)
        mPostSettingsManager.setOnPostSettingsChangedListener(new PostSettingsChangedListener());
    }

    private void stopMonitoringPostSettings() {
        if (mPostTextWatcher != null) {
            mPostTitleEditView.removeTextChangedListener(mPostTextWatcher);
            mPostEditView.removeTextChangedListener(mPostTextWatcher);
            mPostTextWatcher = null;
        }
        // this is safe to do multiple times as it is idempotent
        mPostSettingsManager.removeOnPostSettingsChangedListener();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
//            case R.id.action_discard:
//                onDiscardChangesClicked();
//                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onFormatBoldClicked(View v) {
        EditTextUtils.insertMarkdownBoldMarkers(new EditTextSelectionState(mPostEditView));
        KeyboardUtils.focusAndShowKeyboard(mActivity, mPostEditView);
    }

    @Override
    public void onFormatItalicClicked(View v) {
        EditTextUtils.insertMarkdownItalicMarkers(new EditTextSelectionState(mPostEditView));
        KeyboardUtils.focusAndShowKeyboard(mActivity, mPostEditView);
    }

    @Override
    public void onFormatLinkClicked(View v) {
        EditTextUtils.insertMarkdownLinkMarkers(new EditTextSelectionState(mPostEditView));
        KeyboardUtils.focusAndShowKeyboard(mActivity, mPostEditView);
    }

    @Override
    public void onFormatImageClicked(View v) {
        PopupMenu popupMenu = new PopupMenu(mActivity, v);
        if (mbFileStorageEnabled) {
            popupMenu.inflate(R.menu.insert_image_file_storage_enabled);
        } else {
            popupMenu.inflate(R.menu.insert_image_file_storage_disabled);
        }
        // hide the "Remove This Image" option
        MenuItem removeImageItem = popupMenu.getMenu().findItem(R.id.action_image_remove);
        if (removeImageItem != null) {
            removeImageItem.setVisible(false);
        }

        popupMenu.setOnMenuItemClickListener(item -> {
            mMarkdownEditSelectionState = new EditTextSelectionState(mPostEditView);
            Action1<String> insertMarkdownAction = (imageUrl) -> {
                EditTextUtils.insertMarkdownImageMarkers(imageUrl, mMarkdownEditSelectionState);
            };
            if (item.getItemId() == R.id.action_insert_image_url) {
                onInsertImageUrlClicked(insertMarkdownAction);
            } else if (item.getItemId() == R.id.action_insert_image_upload) {
                onInsertImageUploadClicked(insertMarkdownAction);
            }
            return true;
        });
        popupMenu.show();
    }

    public void onInsertImageUrlClicked(Action1<String> resultAction) {
        Observables.getImageUrlDialog(mActivity).subscribe((imageUrl) -> {
            resultAction.call(imageUrl);
            mMarkdownEditSelectionState = null;
            KeyboardUtils.focusAndShowKeyboard(mActivity, mPostEditView);
        });
    }

    public void onInsertImageUploadClicked(Action1<String> uploadDoneAction) {
        mImageUploadDoneAction = uploadDoneAction;
        Intent imagePickIntent = new Intent(Intent.ACTION_GET_CONTENT);
        imagePickIntent.addCategory(Intent.CATEGORY_OPENABLE);
        imagePickIntent.setType("image/*");
        if (imagePickIntent.resolveActivity(mActivity.getPackageManager()) != null) {
            startActivityForResult(imagePickIntent, REQUEST_CODE_IMAGE_PICK);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent result) {
        if (result == null || result.getData() == null || resultCode != Activity.RESULT_OK) {
            return;
        }

        if (mUploadSubscription != null) {
            mUploadSubscription.unsubscribe();
            mUploadSubscription = null;
        }

        mUploadProgress = ProgressDialog.show(mActivity, null,
                mActivity.getString(R.string.uploading), true, false);

        mUploadSubscription = Observables
                .getBitmapFromUri(mActivity.getContentResolver(), result.getData())
                .map(Observables.Funcs.copyBitmapToJpegFile())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((imagePath) -> {
                    getBus().post(new FileUploadEvent(imagePath, "image/jpeg"));
                }, (error) -> {
                    Toast.makeText(mActivity, R.string.image_upload_failed, Toast.LENGTH_SHORT).show();
                    mUploadProgress.dismiss();
                    mUploadProgress = null;
                }, () -> {      // onComplete
                    //noinspection Convert2MethodRef
                    mUploadSubscription = null;
                });
    }

    @Subscribe
    public void onFileUploadedEvent(FileUploadedEvent event) {
        mUploadProgress.dismiss();
        mUploadProgress = null;
        if (mImageUploadDoneAction == null) {
            Crashlytics.log(Log.ERROR, TAG, "No 'image upload done action' found!");
            return;
        }
        mImageUploadDoneAction.call(event.relativeUrl);
        mImageUploadDoneAction = null;
        mMarkdownEditSelectionState = null;
        KeyboardUtils.focusAndShowKeyboard(mActivity, mPostEditView);
    }

    @Subscribe
    public void onFileUploadErrorEvent(FileUploadErrorEvent event) {
        Toast.makeText(mActivity, R.string.image_upload_failed, Toast.LENGTH_SHORT).show();
        mUploadProgress.dismiss();
        mUploadProgress = null;
    }

    private boolean saveToServerExplicitly() {
        return saveToServerExplicitly(null);
    }

    private boolean saveToServerExplicitly(@Nullable @Post.Status String newStatus) {
        if (newStatus == null) {
            if (mPost.isDraft()) {
                mSaveScenario = SaveScenario.EXPLICITLY_SAVE_DRAFT;
            } else if (mPost.isScheduled()) {
                mSaveScenario = SaveScenario.EXPLICITLY_UPDATE_SCHEDULED_POST;
            } else if (mPost.isPublished()) {
                mSaveScenario = SaveScenario.EXPLICITLY_UPDATE_PUBLISHED_POST;
            } else {
                throw new IllegalArgumentException("unknown post status!");
            }
        } else if (Post.DRAFT.equals(newStatus)) {
            mSaveScenario = SaveScenario.UNPUBLISH_PUBLISHED_POST;
        } else if (Post.PUBLISHED.equals(newStatus)) {
            mSaveScenario = SaveScenario.PUBLISH_DRAFT;
        } else {
            throw new IllegalArgumentException("unknown post status!");
        }
        return savePost(true, false, newStatus);
    }

    public boolean saveToMemory() {
        return savePost(false, true, null);
    }

    private boolean saveAutomatically() {
        if (mPost.isMarkedForDeletion()) {
            return false;
        } else if (mPost.isDraft()) {
            mSaveScenario = SaveScenario.AUTO_SAVE_DRAFT;
        } else if (mPost.isScheduled()) {
            mSaveScenario = SaveScenario.AUTO_SAVE_SCHEDULED_POST;
        } else if (mPost.isPublished()) {
            mSaveScenario = SaveScenario.AUTO_SAVE_PUBLISHED_POST;
        } else {
            throw new IllegalArgumentException("unknown post status!");
        }
        return savePost(true, true, null);
    }

    public boolean saveAutomaticallyWithImage(@NonNull String imageUrl) {
        mPost.setImage(imageUrl);
        return saveAutomatically();
    }

    // returns true if a network call is pending, false otherwise
    private boolean savePost(boolean persistChanges, boolean isAutoSave,
                             @Nullable @Post.Status String newStatus) {
        mPost.setTitle(mPostTitleEditView.getText().toString());
        mPost.setMarkdown(mPostEditView.getText().toString());
        mPost.setHtml(null);   // omit stale HTML from request body
        mPost.setTags(mPostSettingsManager.getTags());
        mPost.setFeatured(mPostSettingsManager.isFeatured());
        if (newStatus != null) {
            mPost.setStatus(newStatus);
        }

        // this handles cases like edit => onPause saves changes => discard, which should discard
        // ALL changes made since the editor was opened, hence save mOriginalPost (can't use
        // savePost(!mbDiscardChanges, ...) in onPause for this reason)
        if (mbDiscardChanges) {
            // avoid network call if no changes have been made SINCE THE POST WAS OPENED FOR EDITING
            if (! PostUtils.isDirty(mOriginalPost, mPost)) return false;
            getBus().post(new SavePostEvent(mOriginalPost, false));
            mbDiscardChanges = false;
            return true;
        } else if (persistChanges) {
            // avoid network call if AUTO-SAVING and no changes have been made SINCE LAST SAVE
            if (isAutoSave && ! PostUtils.isDirty(mLastSavedPost, mPost)) return false;
            getBus().post(new SavePostEvent(mPost, isAutoSave));
            return true;
        }
        return false;
    }

    public void onPublishClicked() {
        if (mPost.isDraft()) {
            // case (1): publishing a draft
            onPublishUnpublishClicked();
        } else {
            // case (2): saving edits to a scheduled or published post
            onSaveClicked().subscribe(Actions.empty());
        }
    }

    public Observable<Boolean> onSaveClicked() {
        // can't use cleverness like !PostUtils.isDirty(mLastSavedPost, mPost) here
        // consider: edit published post => hit back to auto-save => open again and hit "Save"
        // in this case we will end up not asking for confirmation! here again, we're conflating 2
        // kinds of "dirtiness": (1) dirty relative to auto-saved post, and, (2) dirty relative to
        // post on server TODO fix this behaviour!
        if (mPost.isDraft()) {
            return Observable.just(saveToServerExplicitly());
        }
        return Observable.create((subscriber) -> {
            // confirm save for scheduled and published posts
            final AlertDialog alertDialog = new AlertDialog.Builder(mActivity)
                    .setMessage(getString(R.string.alert_save_msg))
                    .setPositiveButton(R.string.alert_save_yes, (dialog, which) -> {
                        subscriber.onNext(saveToServerExplicitly());
                        subscriber.onCompleted();
                    })
                    .setNegativeButton(R.string.alert_save_no, (dialog, which) -> {
                        dialog.dismiss();
                        subscriber.onNext(false);
                        subscriber.onCompleted();
                    })
                    .create();
            // dismiss the dialog automatically if this subscriber unsubscribes
            subscriber.add(Subscriptions.create(alertDialog::dismiss));
            alertDialog.show();
        });
    }

    public void onPublishUnpublishClicked() {
        int msg = R.string.alert_publish;
        String targetStatus = Post.PUBLISHED;
        if (mPost.isPublished()) {
            msg = R.string.alert_unpublish;
            targetStatus = Post.DRAFT;
        }
        @Post.Status final String finalTargetStatus = targetStatus;
        new AlertDialog.Builder(mActivity)
                .setMessage(msg)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    if (finalTargetStatus.equals(mPost.getStatus())) {
                        Crashlytics.logException(new IllegalStateException("UI is messed up, " +
                                "desired post status is same as current status!"));
                    }
                    // This will not be triggered when updating a published post, that goes through
                    // onSaveClicked(). It is assumed the user will ALWAYS want to synchronize the
                    // slug with the title as long as it's being published now (even if it was
                    // published and then unpublished earlier).
                    if (Post.PUBLISHED.equals(finalTargetStatus)) {
                        try {
                            // update the title in memory first, from the latest value in UI
                            mPost.setTitle(mPostTitleEditView.getText().toString());
                            mPost.setSlug(new Slugify().slugify(mPost.getTitle()));
                        } catch (IOException e) {
                            Crashlytics.logException(e);
                        }
                    }
                    saveToServerExplicitly(finalTargetStatus);
                })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
                .create().show();
    }

    private void onDiscardChangesClicked() {
        new AlertDialog.Builder(mActivity)
                .setTitle(getString(R.string.alert_discard_changes_title))
                .setMessage(getString(R.string.alert_discard_changes_msg))
                .setPositiveButton(R.string.discard, (dialog, which) -> {
                    mbDiscardChanges = true;
                    getActivity().finish();
                })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
                .create().show();
    }

    @Subscribe
    public void onPostSavedEvent(PostSavedEvent event) {
        // FIXME the assumption is that SavePostEvent and PostSavedEvent are synchronously sent one
        // FIXME after the other, which breaks the EventBus' "don't assume synchronous" contract
        if (mPost.getUuid().equals(event.post.getUuid())) {
            mLastSavedPost = new Post(mPost);
        }

        // hide the Publish / Unpublish actions if appropriate
        mPostChangedInMemory = false;
        mActivity.supportInvalidateOptionsMenu();

        if (getView() == null) {
            return;
        }
        if (mSaveScenario == SaveScenario.UNKNOWN) {
            AnalyticsService.logPostSavedInUnknownScenario();
            Snackbar.make(getView(), R.string.save_scenario_unknown, Snackbar.LENGTH_SHORT).show();
        } else if (mSaveScenario == SaveScenario.AUTO_SAVE_PUBLISHED_POST) {
            AnalyticsService.logPublishedPostAutoSavedLocally();
            Snackbar.make(getView(), R.string.save_scenario_auto_save_scheduled_or_published_post, Snackbar.LENGTH_SHORT).show();
            mSaveScenario = SaveScenario.UNKNOWN;
        } else if (mSaveScenario == SaveScenario.AUTO_SAVE_SCHEDULED_POST) {
            AnalyticsService.logScheduledPostAutoSavedLocally();
            Snackbar.make(getView(), R.string.save_scenario_auto_save_scheduled_or_published_post, Snackbar.LENGTH_SHORT).show();
            mSaveScenario = SaveScenario.UNKNOWN;
        } else {
            mHandler.postDelayed(mSaveTimeoutRunnable, SAVE_TIMEOUT);
        }
    }

    @Subscribe
    public void onPostSyncedEvent(PostSyncedEvent event) {
        SaveScenario saveScenario = mSaveScenario;
        mSaveScenario = SaveScenario.UNKNOWN;
        mHandler.removeCallbacks(mSaveTimeoutRunnable);
        View parent = getView();
        if (parent == null) {
            return;
        }
        switch (saveScenario) {
            case PUBLISH_DRAFT:
            case EXPLICITLY_UPDATE_PUBLISHED_POST:
                @StringRes int messageId;
                String postUrl = PostUtils.getPostUrl(mPost);
                if (saveScenario == SaveScenario.PUBLISH_DRAFT) {
                    messageId = R.string.save_scenario_publish_draft;
                    // FIXME not a good idea to put this in UI code - what if this happens in the background?!
                    // FIXME or, what if the user publishes offline and syncs later - save scenario would be unknown then!
                    AnalyticsService.logDraftPublished(postUrl);
                } else {
                    messageId = R.string.save_scenario_explicitly_update_scheduled_or_published_post;
                    AnalyticsService.logPublishedPostUpdated(postUrl);
                }
                Snackbar sn = Snackbar.make(parent, messageId, Snackbar.LENGTH_LONG);
                sn.setAction(R.string.save_post_view, v -> mActivity.viewPostInBrowser(false));
                sn.show();
                break;
            case UNPUBLISH_PUBLISHED_POST:
                AnalyticsService.logPostUnpublished();
                Snackbar.make(parent, R.string.save_scenario_unpublish_published_post, Snackbar.LENGTH_SHORT).show();
                break;
            case AUTO_SAVE_DRAFT:
                AnalyticsService.logDraftAutoSaved();
                Snackbar.make(parent, R.string.save_scenario_auto_save_draft, Snackbar.LENGTH_SHORT).show();
                break;
            case EXPLICITLY_SAVE_DRAFT:
                AnalyticsService.logDraftSavedExplicitly();
                Snackbar.make(parent, R.string.save_scenario_explicitly_save_draft, Snackbar.LENGTH_SHORT).show();
                break;
            case EXPLICITLY_UPDATE_SCHEDULED_POST:
                AnalyticsService.logScheduledPostUpdated(PostUtils.getPostUrl(mPost));
                Snackbar.make(parent, R.string.save_scenario_explicitly_update_scheduled_or_published_post, Snackbar.LENGTH_SHORT).show();
                break;
            case AUTO_SAVE_SCHEDULED_POST:
            case AUTO_SAVE_PUBLISHED_POST:
            case UNKNOWN:
                break;              // already handled
        }
    }

    public void setPost(@NonNull Post post, boolean isOriginal) {
        mPost = post;
        if (isOriginal) {
            mOriginalPost = new Post(post);             // store a copy for calculating diff later
            mLastSavedPost = new Post(mOriginalPost);   // the original is obviously already "saved"
        }
        mPostTitleEditView.setText(post.getTitle());
        mPostEditView.setText(post.getMarkdown());
    }

    private class PostTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // count == after indicates high probability of no change
            if (count != after) {
                mPostChangedInMemory = true;
                mActivity.supportInvalidateOptionsMenu();
                // the TextWatcher is removed later, can't remove it here because it crashes
                // https://code.google.com/p/android/issues/detail?id=190399
            }
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {}
    }

    class PostSettingsChangedListener implements PostViewActivity.PostSettingsChangedListener {
        @Override
        public void onPostSettingsChanged() {
            mPostChangedInMemory = true;
            mActivity.supportInvalidateOptionsMenu();
        }
    }

    public interface PostSettingsManager {
        void setOnPostSettingsChangedListener(PostViewActivity.PostSettingsChangedListener listener);
        void removeOnPostSettingsChangedListener();
        RealmList<Tag> getTags();
        boolean isFeatured();
    }

}

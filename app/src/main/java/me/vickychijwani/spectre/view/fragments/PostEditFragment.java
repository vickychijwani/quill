package me.vickychijwani.spectre.view.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
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
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.github.slugify.Slugify;
import com.squareup.otto.Subscribe;

import java.io.IOException;

import butterknife.BindView;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.realm.RealmList;
import me.vickychijwani.spectre.R;
import me.vickychijwani.spectre.analytics.AnalyticsService;
import me.vickychijwani.spectre.error.FileUploadFailedException;
import me.vickychijwani.spectre.event.FileUploadErrorEvent;
import me.vickychijwani.spectre.event.FileUploadEvent;
import me.vickychijwani.spectre.event.FileUploadedEvent;
import me.vickychijwani.spectre.event.PostSavedEvent;
import me.vickychijwani.spectre.event.PostSyncedEvent;
import me.vickychijwani.spectre.event.SavePostEvent;
import me.vickychijwani.spectre.model.entity.PendingAction;
import me.vickychijwani.spectre.model.entity.Post;
import me.vickychijwani.spectre.model.entity.Tag;
import me.vickychijwani.spectre.network.ApiFailure;
import me.vickychijwani.spectre.util.functions.Action1;
import me.vickychijwani.spectre.util.AppUtils;
import me.vickychijwani.spectre.util.EditTextSelectionState;
import me.vickychijwani.spectre.util.EditTextUtils;
import me.vickychijwani.spectre.util.KeyboardUtils;
import me.vickychijwani.spectre.util.PostUtils;
import me.vickychijwani.spectre.view.BundleKeys;
import me.vickychijwani.spectre.view.FormatOptionClickListener;
import me.vickychijwani.spectre.view.Observables;
import me.vickychijwani.spectre.view.PostViewActivity;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.RuntimePermissions;

@RuntimePermissions
public class PostEditFragment extends BaseFragment implements
        FormatOptionClickListener {

    private static final String TAG = "PostEditFragment";
    private static final String EDITOR_CURSOR_POS = "key:private:editor_cursor_pos";

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

    @BindView(R.id.post_title_edit)             EditText mPostTitleEditView;
    @BindView(R.id.post_markdown)               EditText mPostEditView;

    private PostViewActivity mActivity;
    private PostSettingsManager mPostSettingsManager;

    private Post mOriginalPost;     // copy of post since the time it was opened for editing
    private Post mLastSavedPost;    // copy of post since it was last saved
    private Post mPost;             // current copy of post in memory

    // used in onPause/onResume
    private int mPostEditViewCursorPos = -1;
    // used only when changing tabs (no fragment lifecycle methods are triggered)
    private EditTextSelectionState mFocusedEditTextSelectionState = null;

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private boolean mbDiscardChanges = false;

    private SaveScenario mSaveScenario = SaveScenario.UNKNOWN;
    private Runnable mSaveTimeoutRunnable;
    private static final int SAVE_TIMEOUT = 5 * 1000;       // milliseconds

    private boolean mPostChangedInMemory = false;
    private PostTextWatcher mPostTextWatcher = null;

    // image insert / upload
    private static final int REQUEST_CODE_IMAGE_PICK = 1;
    private Disposable mUploadDisposable = null;
    private ProgressDialog mUploadProgress = null;
    private EditTextSelectionState mMarkdownEditSelectionState;
    private Action1<String> mImageUploadDoneAction = null;


    @SuppressWarnings("unused")
    public static PostEditFragment newInstance(@NonNull Post post) {
        PostEditFragment fragment = new PostEditFragment();
        Bundle args = new Bundle();
        args.putParcelable(BundleKeys.POST, post);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_post_edit, container, false);
        bindView(view);

        mActivity = ((PostViewActivity) getActivity());
        mPostSettingsManager = mActivity;

        Bundle args = new Bundle(getArguments());   // defaults, given during original Fragment construction
        if (savedInstanceState != null) {
            args.putAll(savedInstanceState);        // overrides, for things that could've changed, and for new things like custom UI state
        }
        mPostEditViewCursorPos = args.getInt(EDITOR_CURSOR_POS, -1);
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
        // save the editor cursor pos because setPost is called in onCreate/onResume
        outState.putInt(EDITOR_CURSOR_POS, mPostEditView.getSelectionEnd());
    }

    public void saveSelectionState() {
        final View focusedView = mActivity.getCurrentFocus();
        if (focusedView != null && focusedView instanceof EditText) {
            mFocusedEditTextSelectionState = new EditTextSelectionState((EditText) focusedView);
        }
    }

    public void restoreSelectionState() {
        if (mFocusedEditTextSelectionState != null) {
            mFocusedEditTextSelectionState.focusAndRestoreSelectionState();
            mFocusedEditTextSelectionState = null;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // remove pending callbacks
        mHandler.removeCallbacks(mSaveTimeoutRunnable);
        // persist changes to disk, unless the user opted to discard those changes
        // workaround: do this ONLY if an image upload is NOT in progress - this is to avoid saving
        // the post prematurely and generating a spurious conflict that cannot be dealt with cleanly
        // because the post gets uploaded after this Activity goes away, putting it out-of-sync with
        // the model
        if (mImageUploadDoneAction == null) {
            saveAutomatically();
        } else {
            // we still need to save to memory, else the post will revert to its original state!
            saveToMemory();
        }
        // save misc editor state because setPost is called in onResume
        mPostEditViewCursorPos = mPostEditView.getSelectionEnd();

        // unsubscribe from observable and hide progress bar
        if (mUploadDisposable != null && !mUploadDisposable.isDisposed()) {
            mUploadDisposable.dispose();
            mUploadDisposable = null;
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
        inflater.inflate(R.menu.post_edit, menu);
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
        popupMenu.inflate(R.menu.insert_image);
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
                // the *WithCheck() method checks for runtime permissions and
                // is generated by the PermissionsDispatcher library
                PostEditFragmentPermissionsDispatcher.onInsertImageUploadClickedWithCheck(this,
                        insertMarkdownAction);
            }
            return true;
        });
        popupMenu.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        PostEditFragmentPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    public void onInsertImageUrlClicked(Action1<String> resultAction) {
        // ok to pass null here: https://possiblemobile.com/2013/05/layout-inflation-as-intended/
        @SuppressLint("InflateParams")
        final View dialogView = mActivity.getLayoutInflater().inflate(R.layout.dialog_image_insert,
                null, false);
        final TextView imageUrlView = (TextView) dialogView.findViewById(R.id.image_url);

        // hack for word wrap with "Done" IME action! see http://stackoverflow.com/a/13563946/504611
        imageUrlView.setHorizontallyScrolling(false);
        imageUrlView.setMaxLines(20);

        Observable<String> imageUrlObservable = Observables.getDialog(emitter -> {
            AlertDialog dialog = new AlertDialog.Builder(mActivity)
                    .setTitle(mActivity.getString(R.string.insert_image))
                    .setView(dialogView)
                    .setCancelable(true)
                    .setPositiveButton(R.string.insert, (d, which) -> {
                        emitter.onNext(imageUrlView.getText().toString());
                        emitter.onComplete();
                    })
                    .setNegativeButton(android.R.string.cancel, (d, which) -> {
                        d.dismiss();
                        emitter.onComplete();
                    })
                    .create();
            imageUrlView.setOnEditorActionListener((view, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
                    return true;
                }
                return false;
            });
            return dialog;
        });

        imageUrlObservable.subscribe((imageUrl) -> {
            resultAction.call(imageUrl);
            mMarkdownEditSelectionState = null;
            KeyboardUtils.focusAndShowKeyboard(mActivity, mPostEditView);
        });
    }

    @SuppressLint("InlinedApi") // suppressed because PermissionsDispatcher handles API levels for us
    @NeedsPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
    public void onInsertImageUploadClicked(Action1<String> uploadDoneAction) {
        mImageUploadDoneAction = uploadDoneAction;
        Intent imagePickIntent = new Intent(Intent.ACTION_GET_CONTENT);
        imagePickIntent.addCategory(Intent.CATEGORY_OPENABLE);
        imagePickIntent.setType("image/*");
        if (imagePickIntent.resolveActivity(mActivity.getPackageManager()) != null) {
            startActivityForResult(imagePickIntent, REQUEST_CODE_IMAGE_PICK);
        } else {
            Toast.makeText(mActivity, R.string.intent_no_apps, Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("InlinedApi") // suppressed because PermissionsDispatcher handles API levels for us
    @OnPermissionDenied(Manifest.permission.READ_EXTERNAL_STORAGE)
    void onStoragePermissionDenied() {
        Toast.makeText(mActivity, R.string.image_upload_failed, Toast.LENGTH_SHORT).show();
    }

    @SuppressLint("InlinedApi") // suppressed because PermissionsDispatcher handles API levels for us
    @OnNeverAskAgain(Manifest.permission.READ_EXTERNAL_STORAGE)
    void onStoragePermissionPermanentlyDenied() {
        Toast.makeText(mActivity, R.string.enable_permission_tip, Toast.LENGTH_LONG).show();
        AppUtils.showSystemAppSettingsActivity(mActivity);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent result) {
        if (result == null || result.getData() == null || resultCode != Activity.RESULT_OK) {
            return;
        }
        if (requestCode == REQUEST_CODE_IMAGE_PICK) {
            uploadImage(result.getData());
        }
    }

    private void uploadImage(@NonNull Uri uri) {
        if (mUploadDisposable != null && !mUploadDisposable.isDisposed()) {
            mUploadDisposable.dispose();
            mUploadDisposable = null;
        }

        mUploadProgress = ProgressDialog.show(mActivity, null,
                mActivity.getString(R.string.uploading), true, false);

        mUploadDisposable = Observables
                .getFileUploadMetadataFromUri(mActivity.getContentResolver(), uri)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((pair) -> {
                    getBus().post(new FileUploadEvent(pair.first, pair.second));
                }, (error) -> {
                    onFileUploadErrorEvent(new FileUploadErrorEvent(new ApiFailure(error)));
                }, () -> {
                    mUploadDisposable = null;
                });
    }

    @Subscribe
    public void onFileUploadedEvent(FileUploadedEvent event) {
        // the activity could have been destroyed and re-created
        if (mUploadProgress != null) {
            mUploadProgress.dismiss();
            mUploadProgress = null;
        }
        // the activity could have been destroyed and re-created
        if (mImageUploadDoneAction != null) {
            mImageUploadDoneAction.call(event.relativeUrl);
            mImageUploadDoneAction = null;
        }
        mMarkdownEditSelectionState = null;
        KeyboardUtils.focusAndShowKeyboard(mActivity, mPostEditView);
    }

    @Subscribe
    public void onFileUploadErrorEvent(FileUploadErrorEvent event) {
        if (event.apiFailure.error != null) {
            Crashlytics.logException(new FileUploadFailedException(event.apiFailure.error));
        } else if (event.apiFailure.response != null) {
            try {
                String responseStr = event.apiFailure.response.errorBody().string();
                Crashlytics.logException(new FileUploadFailedException(responseStr));
            } catch (IOException e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
        }
        Toast.makeText(mActivity, R.string.image_upload_failed, Toast.LENGTH_SHORT).show();
        // the activity could have been destroyed and re-created
        if (mUploadProgress != null) {
            mUploadProgress.dismiss();
            mUploadProgress = null;
        }
        mImageUploadDoneAction = null;
        mMarkdownEditSelectionState = null;
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
        mPost.setFeatureImage(imageUrl);
        return saveAutomatically();
    }

    // returns true if a network call is pending, false otherwise
    private boolean savePost(boolean persistChanges, boolean isAutoSave,
                             @Nullable @Post.Status String newStatus) {
        mPost.setTitle(mPostTitleEditView.getText().toString());
        mPost.setMarkdown(mPostEditView.getText().toString());
        mPost.setHtml(null);   // omit stale HTML from request body
        mPost.setTags(mPostSettingsManager.getTags());
        mPost.setCustomExcerpt(mPostSettingsManager.getCustomExcerpt());
        mPost.setFeatured(mPostSettingsManager.isFeatured());
        mPost.setPage(mPostSettingsManager.isPage());
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
            // empty subscribe() because we don't care about the Observable in this case
            onSaveClicked().subscribe();
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
        return Observables.getDialog(emitter -> {
            // confirm save for scheduled and published posts
            return new AlertDialog.Builder(mActivity)
                    .setMessage(getString(R.string.alert_save_msg))
                    .setPositiveButton(R.string.alert_save_yes, (dialog, which) -> {
                        emitter.onNext(saveToServerExplicitly());
                        emitter.onComplete();
                    })
                    .setNegativeButton(R.string.alert_save_no, (dialog, which) -> {
                        dialog.dismiss();
                        emitter.onNext(false);
                        emitter.onComplete();
                    })
                    .create();
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
                        // update the title in memory first, from the latest value in UI
                        mPost.setTitle(mPostTitleEditView.getText().toString());
                        mPost.setSlug(new Slugify().slugify(mPost.getTitle()));
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
        if (mPost.getId().equals(event.post.getId())) {
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
        if (mPostEditViewCursorPos >= 0
                // cursor pos is == length, when it's at the very end
                && mPostEditViewCursorPos <= mPostEditView.getText().length()) {
            mPostEditView.setSelection(mPostEditViewCursorPos);
        }
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

    private class PostSettingsChangedListener implements PostViewActivity.PostSettingsChangedListener {
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
        String getCustomExcerpt();
        boolean isFeatured();
        boolean isPage();
    }

}

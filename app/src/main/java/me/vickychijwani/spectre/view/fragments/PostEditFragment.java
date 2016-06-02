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
import android.text.Editable;
import android.text.TextUtils;
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
import me.vickychijwani.spectre.util.PostUtils;
import me.vickychijwani.spectre.view.BundleKeys;
import me.vickychijwani.spectre.view.Observables;
import me.vickychijwani.spectre.view.PostViewActivity;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Actions;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;

public class PostEditFragment extends BaseFragment {

    private static final String TAG = "PostEditFragment";

    private enum SaveType {
        NONE,
        PUBLISH,
        UNPUBLISH,
        PUBLISHED_AUTO,
        PUBLISHED_EXPLICIT,
        DRAFT_AUTO,
        DRAFT_EXPLICIT
    }

    @Bind(R.id.post_title_edit)             EditText mPostTitleEditView;
    @Bind(R.id.post_markdown)               EditText mPostEditView;

    private PostViewActivity mActivity;
    private PostTagsManager mPostTagsManager;

    private Post mOriginalPost;     // copy of post since the time it was opened for editing
    private Post mLastSavedPost;    // copy of post since it was last saved
    private Post mPost;             // current copy of post in memory

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private boolean mbDiscardChanges = false;

    private SaveType mSaveType = SaveType.NONE;
    private Runnable mSaveTimeoutRunnable;
    private static final int SAVE_TIMEOUT = 5 * 1000;       // milliseconds

    private boolean mPostTitleOrBodyTextChanged = false;
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
        mPostTagsManager = mActivity;
        mbFileStorageEnabled = getArguments().getBoolean(BundleKeys.FILE_STORAGE_ENABLED,
                mbFileStorageEnabled);

        //noinspection ConstantConditions
        setPost(getArguments().getParcelable(BundleKeys.POST), true);

        mSaveTimeoutRunnable = () -> {
            View parent = PostEditFragment.this.getView();
            if (parent != null) {
                Snackbar.make(parent, R.string.save_post_timeout, Snackbar.LENGTH_SHORT).show();
                mSaveType = SaveType.NONE;
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
        if (Post.DRAFT.equals(mPost.getStatus())
                || mPost.hasPendingAction(PendingAction.EDIT_LOCAL)
                || mPostTitleOrBodyTextChanged) {
            if (mPostTextWatcher != null) {
                mPostTitleEditView.removeTextChangedListener(mPostTextWatcher);
                mPostEditView.removeTextChangedListener(mPostTextWatcher);
                mPostTextWatcher = null;
            }
            return true;
        } else {
            // published post with no auto-saved edits (may have unsynced published edits though)
            if (mPostTextWatcher == null) {
                mPostTextWatcher = new PostTextWatcher();
                mPostTitleEditView.addTextChangedListener(mPostTextWatcher);
                mPostEditView.addTextChangedListener(mPostTextWatcher);
            }
            return false;
        }
    }

    public boolean shouldShowUnpublishAction() {
        return Post.PUBLISHED.equals(mPost.getStatus());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_insert_image_url:
            case R.id.action_insert_image_upload:
                mMarkdownEditSelectionState = new EditTextSelectionState(mPostEditView);
                Action1<String> insertMarkdownAction = Observables.Actions
                        .insertImageMarkdown(mActivity, mMarkdownEditSelectionState);
                if (item.getItemId() == R.id.action_insert_image_url) {
                    onInsertImageUrlClicked(insertMarkdownAction);
                } else {
                    onInsertImageUploadClicked(insertMarkdownAction);
                }
                return true;
//            case R.id.action_discard:
//                onDiscardChangesClicked();
//                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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
        mMarkdownEditSelectionState = null;
        if (mImageUploadDoneAction == null) {
            Crashlytics.log(Log.ERROR, TAG, "No 'image upload done action' found!");
            return;
        }
        mImageUploadDoneAction.call(event.relativeUrl);
        mImageUploadDoneAction = null;
    }

    @Subscribe
    public void onFileUploadErrorEvent(FileUploadErrorEvent event) {
        Toast.makeText(mActivity, R.string.image_upload_failed, Toast.LENGTH_SHORT).show();
        mUploadProgress.dismiss();
        mUploadProgress = null;
    }

    public void onInsertImageUrlClicked(Action1<String> resultAction) {
        Observables.getImageUrlDialog(mActivity).subscribe((url) -> {
            resultAction.call(url);
            mMarkdownEditSelectionState = null;
        });
    }

    private boolean saveToServerExplicitly() {
        return saveToServerExplicitly(null);
    }

    private boolean saveToServerExplicitly(@Nullable @Post.Status String newStatus) {
        if (newStatus == null) {
            if (Post.DRAFT.equals(mPost.getStatus())) {
                mSaveType = SaveType.DRAFT_EXPLICIT;
            } else if (Post.PUBLISHED.equals(mPost.getStatus())) {
                mSaveType = SaveType.PUBLISHED_EXPLICIT;
            }
        } else if (Post.DRAFT.equals(newStatus)) {
            mSaveType = SaveType.UNPUBLISH;
        } else if (Post.PUBLISHED.equals(newStatus)) {
            mSaveType = SaveType.PUBLISH;
        }
        return savePost(true, false, newStatus);
    }

    public boolean saveToMemory() {
        return savePost(false, true, null);
    }

    private boolean saveAutomatically() {
        if (mPost.isMarkedForDeletion()) {
            return false;
        } else if (Post.DRAFT.equals(mPost.getStatus())) {
            mSaveType = SaveType.DRAFT_AUTO;
        } else if (Post.PUBLISHED.equals(mPost.getStatus())) {
            mSaveType = SaveType.PUBLISHED_AUTO;
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
        mPost.setTags(mPostTagsManager.getTags());
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
        if (Post.DRAFT.equals(mPost.getStatus())) {
            // case (1): publishing a draft
            onPublishUnpublishClicked();
        } else {
            // case (2): saving edits to a published post
            onSaveClicked().subscribe(Actions.empty());
        }
    }

    public Observable<Boolean> onSaveClicked() {
        // can't use cleverness like !PostUtils.isDirty(mLastSavedPost, mPost) here
        // consider: edit published post => hit back to auto-save => open again and hit "Save"
        // in this case we will end up not asking for confirmation! here again, we're conflating 2
        // kinds of "dirtiness": (1) dirty relative to auto-saved post, and, (2) dirty relative to
        // post on server TODO fix this behaviour!
        if (Post.DRAFT.equals(mPost.getStatus())) {
            return Observable.just(saveToServerExplicitly());
        }
        return Observable.create((subscriber) -> {
            // confirm save for published posts
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
        if (Post.PUBLISHED.equals(mPost.getStatus())) {
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
                    if (TextUtils.isEmpty(mPost.getSlug())
                            || mPost.getSlug().startsWith(Post.DEFAULT_SLUG_PREFIX)) {
                        try {
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
        mPostTitleOrBodyTextChanged = false;
        mActivity.supportInvalidateOptionsMenu();

        if (getView() == null) {
            return;
        }
        if (mSaveType == SaveType.NONE) {
            Snackbar.make(getView(), R.string.save_post_generic, Snackbar.LENGTH_SHORT).show();
        } else if (mSaveType == SaveType.PUBLISHED_AUTO) {
            Snackbar.make(getView(), R.string.save_published_auto, Snackbar.LENGTH_SHORT).show();
            mSaveType = SaveType.NONE;
        } else {
            mHandler.postDelayed(mSaveTimeoutRunnable, SAVE_TIMEOUT);
        }
    }

    @Subscribe
    public void onPostSyncedEvent(PostSyncedEvent event) {
        SaveType saveType = mSaveType;
        mSaveType = SaveType.NONE;
        mHandler.removeCallbacks(mSaveTimeoutRunnable);
        View parent = getView();
        if (parent == null) {
            return;
        }
        switch (saveType) {
            case PUBLISH:
            case PUBLISHED_EXPLICIT:
                @StringRes int messageId = (saveType == SaveType.PUBLISH)
                        ? R.string.save_publish
                        : R.string.save_published_explicit;
                Snackbar sn = Snackbar.make(parent, messageId, Snackbar.LENGTH_SHORT);
                sn.setAction(R.string.save_post_view, v -> mActivity.viewPostInBrowser(false));
                sn.show();
                break;
            case UNPUBLISH:
                Snackbar.make(parent, R.string.save_unpublish, Snackbar.LENGTH_SHORT).show();
                break;
            case DRAFT_AUTO:
                Snackbar.make(parent, R.string.save_draft_auto, Snackbar.LENGTH_SHORT).show();
                break;
            case DRAFT_EXPLICIT:
                Snackbar.make(parent, R.string.save_draft_explicit, Snackbar.LENGTH_SHORT).show();
                break;
            case PUBLISHED_AUTO:
            case NONE:
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
                mPostTitleOrBodyTextChanged = true;
                mActivity.supportInvalidateOptionsMenu();
                // the TextWatcher is removed later, can't remove it here because it crashes
            }
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {}
    }

    public interface PostTagsManager {
        RealmList<Tag> getTags();
    }

}

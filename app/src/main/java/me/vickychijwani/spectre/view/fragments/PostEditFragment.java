package me.vickychijwani.spectre.view.fragments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.github.ksoichiro.android.observablescrollview.ObservableScrollView;
import com.github.ksoichiro.android.observablescrollview.ObservableScrollViewCallbacks;
import com.github.ksoichiro.android.observablescrollview.ScrollState;
import com.github.slugify.Slugify;
import com.squareup.otto.Subscribe;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.List;

import butterknife.Bind;
import butterknife.BindDimen;
import butterknife.ButterKnife;
import io.realm.RealmList;
import me.vickychijwani.spectre.R;
import me.vickychijwani.spectre.event.FileUploadErrorEvent;
import me.vickychijwani.spectre.event.FileUploadEvent;
import me.vickychijwani.spectre.event.FileUploadedEvent;
import me.vickychijwani.spectre.event.PostSavedEvent;
import me.vickychijwani.spectre.event.PostSyncedEvent;
import me.vickychijwani.spectre.event.SavePostEvent;
import me.vickychijwani.spectre.model.Post;
import me.vickychijwani.spectre.model.Tag;
import me.vickychijwani.spectre.pref.UserPrefs;
import me.vickychijwani.spectre.util.AppUtils;
import me.vickychijwani.spectre.util.EditTextSelectionState;
import me.vickychijwani.spectre.util.PostUtils;
import me.vickychijwani.spectre.view.EditTextActionModeManager;
import me.vickychijwani.spectre.view.Observables;
import me.vickychijwani.spectre.view.PostViewActivity;
import me.vickychijwani.spectre.view.TagsEditText;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Actions;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;

public class PostEditFragment extends BaseFragment implements ObservableScrollViewCallbacks,
        EditTextActionModeManager.Callbacks {

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

    @Bind(R.id.post_header_container)       ViewGroup mPostHeaderContainer;
    @Bind(R.id.post_header)                 View mPostHeader;
    @Bind(R.id.post_image)                  ImageView mPostImageView;
    @Bind(R.id.post_image_overlay)          View mPostImageOverlay;
    @Bind(R.id.post_title_edit)             EditText mPostTitleEditView;
    @Bind(R.id.post_tags_edit)              TagsEditText mPostTagsEditView;
    @Bind(R.id.post_markdown)               EditText mPostEditView;
    @Bind(R.id.preview_btn)                 FloatingActionButton mPreviewBtn;
    @Bind(R.id.observable_scroll_view)      ObservableScrollView mScrollView;

    private Post mOriginalPost;     // copy of post since the time it was opened for editing
    private Post mLastSavedPost;    // copy of post since it was last saved
    private Post mPost;             // current copy of post in memory

    private Handler mHandler = new Handler(Looper.getMainLooper());
    private String mBlogUrl;
    private Picasso mPicasso;

    private OnPreviewClickListener mPreviewClickListener;
    private boolean mbDiscardChanges = false;

    private SaveType mSaveType = SaveType.NONE;
    private Runnable mSaveTimeoutRunnable;
    private static final int SAVE_TIMEOUT = 5 * 1000;       // milliseconds

    // image insert / upload
    private static final int REQUEST_CODE_IMAGE_PICK = 1;
    private Subscription mUploadSubscription = null;
    private ProgressDialog mUploadProgress = null;
    private EditTextSelectionState mMarkdownEditSelectionState;

    // action mode
    private View.OnClickListener mActionModeCloseClickListener;
    private EditTextActionModeManager mEditTextActionModeManager;
    private PostViewActivity mActivity;

    // scroll behaviour
    private boolean mHasImage = false;
    private int mActionBarSize;
    private int mHeightCollapseDistance;
    private int mTotalCollapseDistance;
    private boolean mPostHeaderCollapsed = false;
    private boolean mPostTitleCollapsed = false;
    private float mImageOverlayBaseAlpha;
    private Drawable mHeaderBottomScrimDrawable;
    @BindDimen(R.dimen.post_header_image_height) float mImageMinHeight;


    public interface OnPreviewClickListener {
        void onPreviewClicked();
    }


    @SuppressWarnings("unused")
    public static PostEditFragment newInstance() {
        return new PostEditFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_post_edit, container, false);
        ButterKnife.bind(this, view);

        mActivity = ((PostViewActivity) getActivity());
        mBlogUrl = UserPrefs.getInstance(mActivity).getString(UserPrefs.Key.BLOG_URL);
        mPicasso = getPicasso();
        mHeaderBottomScrimDrawable = AppUtils.makeCubicGradientScrimDrawable(0xaa000000, 8,
                Gravity.BOTTOM);

        setPost(mActivity.getPost(), true);

        mSaveTimeoutRunnable = () -> {
            View parent = PostEditFragment.this.getView();
            if (parent != null) {
                Snackbar.make(parent, R.string.save_post_timeout, Snackbar.LENGTH_SHORT).show();
                mSaveType = SaveType.NONE;
            }
        };

        // action mode manager
        mEditTextActionModeManager = new EditTextActionModeManager(mActivity, this);
        mActionModeCloseClickListener = v -> mEditTextActionModeManager.stopActionMode(true);
        mEditTextActionModeManager.register(mPostTitleEditView);
        mEditTextActionModeManager.register(mPostTagsEditView);

        // title
        mActivity.setTitle(null);
        // hack for word wrap with "Done" IME action! see http://stackoverflow.com/a/13563946/504611
        mPostTitleEditView.setHorizontallyScrolling(false);
        mPostTitleEditView.setMaxLines(Integer.MAX_VALUE);

        // tags
        mPostTagsEditView.setAdapter(new ArrayAdapter<>(
                getActivity(), android.R.layout.simple_list_item_1, new Tag[]{}
        ));

        // preview button
        mPreviewBtn.setOnClickListener(v -> mPreviewClickListener.onPreviewClicked());

        // scroll behaviour
        mActionBarSize = getActionBarSize();
        TypedValue alphaValue = new TypedValue();
        getResources().getValue(R.dimen.editor_overlay_base_alpha, alphaValue, true);
        mImageOverlayBaseAlpha = alphaValue.getFloat();
        mScrollView.setScrollViewCallbacks(this);
        mPostHeaderContainer.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            int containerHeight = mPostHeaderContainer.getHeight();
            mTotalCollapseDistance = containerHeight - mActionBarSize;
            mHeightCollapseDistance = mTotalCollapseDistance - mPostHeader.getHeight()
                    + mActionBarSize;   // header includes paddingTop = mActionBarSize
            if (containerHeight > mImageMinHeight) {
                ViewGroup.LayoutParams lp = mPostImageView.getLayoutParams();
                lp.height = containerHeight;
                mPostImageView.setLayoutParams(lp);
                ViewGroup.LayoutParams lp2 = mPostImageOverlay.getLayoutParams();
                lp2.height = containerHeight;
                mPostImageOverlay.setLayoutParams(lp2);
            }
        });

        setHasOptionsMenu(true);

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        show();
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
        // stop editing title / tags and discard changes
        mEditTextActionModeManager.stopActionMode(true);
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
    public void onShow() {
        onScrollChanged(mScrollView.getCurrentScrollY(), false, false);
        if (mActivity != null && !mPostTitleCollapsed) {
            mActivity.setTitle(null);
        }
    }

    @Override
    public void onHide() {
        mPost.setMarkdown(mPostEditView.getText().toString());
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mPreviewClickListener = (OnPreviewClickListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                + "must implement OnPreviewClickListener");
        }
    }


    // action mode
    @Override
    public void onActionModeStarted(EditText editText) {
        if (editText == mPostTitleEditView) {
            mActivity.setTitle(getString(R.string.edit_title));
        } else if (editText == mPostTagsEditView) {
            mActivity.setTitle(getString(R.string.edit_tags));
        }
        mActivity.supportInvalidateOptionsMenu();
        mActivity.setNavigationItem(R.drawable.close, mActionModeCloseClickListener);
        mPreviewBtn.setVisibility(View.GONE);
    }

    @Override
    public void onActionModeStopped(boolean discardChanges) {
        if (discardChanges) {
            setPost(mPost, false);
        } else {
            saveToMemory();
        }
        AppUtils.hideKeyboard(mActivity);
        mActivity.setTitle(null);
        mActivity.supportInvalidateOptionsMenu();
        mActivity.resetNavigationItem();
        mPreviewBtn.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onBackPressed() {
        //noinspection SimplifiableIfStatement
        if (mEditTextActionModeManager.stopActionMode(true)) {
            return true;
        }
        return super.onBackPressed();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.post_edit, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        boolean isFragmentShown = isShown();
        boolean actionModeActive = mEditTextActionModeManager.isActionModeActive();
        menu.findItem(R.id.action_done).setVisible(actionModeActive);
        menu.findItem(R.id.action_insert_image).setVisible(isFragmentShown && !actionModeActive);
        menu.findItem(R.id.action_save).setVisible(!actionModeActive);
        menu.findItem(R.id.action_publish).setVisible(!actionModeActive);
        if (Post.PUBLISHED.equals(mPost.getStatus())) {
            menu.findItem(R.id.action_publish).setTitle(R.string.unpublish);
        } else {
            menu.findItem(R.id.action_publish).setTitle(R.string.publish);
        }
//         saveToMemory();   // make sure user changes are stored in mPost before computing diff
//         boolean isPostDirty = PostUtils.isDirty(mOriginalPost, mPost);
//         menu.findItem(R.id.action_discard).setVisible(isPostDirty && !actionModeActive);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_done:
                mEditTextActionModeManager.stopActionMode(false);
                return true;
            case R.id.action_insert_image_url:
                onInsertImageUrlClicked();
                return true;
            case R.id.action_insert_image_upload:
                onInsertImageUploadClicked();
                return true;
            case R.id.action_save:
                // TODO hate having to do an empty subscribe here
                onSaveClicked().subscribe(Actions.empty());
                return true;
            case R.id.action_publish:
                onPublishUnpublishClicked();
                return true;
//            case R.id.action_discard:
//                onDiscardChangesClicked();
//                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void onInsertImageUploadClicked() {
        mMarkdownEditSelectionState = new EditTextSelectionState(mPostEditView);
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
        Action1<String> insertMarkdownAction = Observables.Actions
                .insertImageMarkdown(mActivity, mMarkdownEditSelectionState);
        Observable.just(event.relativeUrl).subscribe((url) -> {
            mUploadProgress.dismiss();
            mUploadProgress = null;
            insertMarkdownAction.call(url);
            mMarkdownEditSelectionState = null;
        });
    }

    @Subscribe
    public void onFileUploadErrorEvent(FileUploadErrorEvent event) {
        Toast.makeText(mActivity, R.string.image_upload_failed, Toast.LENGTH_SHORT).show();
        mUploadProgress.dismiss();
        mUploadProgress = null;
    }

    private void onInsertImageUrlClicked() {
        mMarkdownEditSelectionState = new EditTextSelectionState(mPostEditView);
        Action1<String> insertMarkdownAction = Observables.Actions
                .insertImageMarkdown(mActivity, mMarkdownEditSelectionState);
        Observables.getImageUrlDialog(mActivity).subscribe((url) -> {
            insertMarkdownAction.call(url);
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

    private boolean saveToMemory() {
        return savePost(false, true, null);
    }

    private boolean saveAutomatically() {
        if (Post.DRAFT.equals(mPost.getStatus())) {
            mSaveType = SaveType.DRAFT_AUTO;
        } else if (Post.PUBLISHED.equals(mPost.getStatus())) {
            mSaveType = SaveType.PUBLISHED_AUTO;
        }
        return savePost(true, true, null);
    }

    // returns true if a network call is pending, false otherwise
    private boolean savePost(boolean persistChanges, boolean isAutoSave,
                             @Nullable @Post.Status String newStatus) {
        mPost.setTitle(mPostTitleEditView.getText().toString());
        mPost.setMarkdown(mPostEditView.getText().toString());
        mPost.setHtml(null);   // omit stale HTML from request body
        RealmList<Tag> tags = new RealmList<>();
        List<Object> tagObjects = mPostTagsEditView.getObjects();
        for (Object obj : tagObjects) {
            tags.add((Tag) obj);
        }
        mPost.setTags(tags);
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

    private void onPublishUnpublishClicked() {
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
        if (! TextUtils.isEmpty(post.getImage())) {
            String imageUrl = AppUtils.pathJoin(mBlogUrl, post.getImage());
            mPicasso.load(imageUrl).into(mPostImageView);
            mPostImageView.setVisibility(View.VISIBLE);
            mPostImageOverlay.setVisibility(View.VISIBLE);
            mActivity.setToolbarScrimAlpha(1f);
            mPostHeader.setBackground(mHeaderBottomScrimDrawable);
            mHasImage = true;
        } else {
            mPostImageView.setVisibility(View.GONE);
            mPostImageOverlay.setVisibility(View.GONE);
            mActivity.setToolbarScrimAlpha(0f);
            mPostHeader.setBackground(null);
            mHasImage = false;
        }
        mPostEditView.setText(post.getMarkdown());
        // FIXME clear doesn't work when fragment is pausing, because it posts to the UI thread
        mPostTagsEditView.clear();
        for (Tag tag : post.getTags()) {
            mPostTagsEditView.addObject(tag, tag.getName());
        }
    }


    // scroll behaviour
    @Override
    public void onScrollChanged(int scrollY, boolean ignored1, boolean ignored2) {
        if (mTotalCollapseDistance == 0 ||     // erroneous call
                (scrollY > mTotalCollapseDistance && mPostHeaderCollapsed)) {
            return;  // we definitely don't need to change anything in this case
        }

        if (scrollY > mHeightCollapseDistance) {
            collapsePostHeader();
        } else {
            expandPostHeader();
        }

        if (scrollY > mTotalCollapseDistance) {
            mActivity.setToolbarBackgroundOpaque(true);
            mPostHeaderCollapsed = true;
        } else {
            mActivity.setToolbarBackgroundOpaque(false);
            mPostHeaderCollapsed = false;
        }

        if (mHasImage) {
            // image parallax
            mPostImageView.setTranslationY(scrollY / 2f);

            // interpolate transition
            float fractionCollapsed;
            if (mTotalCollapseDistance - mHeightCollapseDistance == 0) {
                // edge case, no space available for smooth transition, so jump from 0 to 1 suddenly
                fractionCollapsed = (scrollY == 0) ? 0f : 1f;
            } else {
                fractionCollapsed = (scrollY - mHeightCollapseDistance) /
                        (float) (mTotalCollapseDistance - mHeightCollapseDistance);
                fractionCollapsed = Math.max(0f, Math.min(1f, fractionCollapsed));
            }

            mPostImageOverlay.setAlpha(mImageOverlayBaseAlpha + (1 - mImageOverlayBaseAlpha) * fractionCollapsed);
            mActivity.setToolbarScrimAlpha(1f - fractionCollapsed);
        }
    }

    @Override
    public void onDownMotionEvent() {

    }

    @Override
    public void onUpOrCancelMotionEvent(ScrollState scrollState) {

    }

    private void collapsePostHeader() {
        if (mPostTitleCollapsed) {
            return;
        }
        mPostHeader.animate().alpha(0.0f).setDuration(200);
        getActivity().setTitle(mPost.getTitle());
        mPostTitleCollapsed = true;
    }

    private void expandPostHeader() {
        if (! mPostTitleCollapsed) {
            return;
        }
        mPostHeader.animate().alpha(1.0f).setDuration(200);
        getActivity().setTitle(null);
        mPostTitleCollapsed = false;
    }

    private int getActionBarSize() {
        TypedValue typedValue = new TypedValue();
        int[] textSizeAttr = new int[] { R.attr.actionBarSize };
        TypedArray a = getActivity().obtainStyledAttributes(typedValue.data, textSizeAttr);
        int actionBarSize = a.getDimensionPixelSize(0, -1);
        a.recycle();
        return actionBarSize;
    }

}

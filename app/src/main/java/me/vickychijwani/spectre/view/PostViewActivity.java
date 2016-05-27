package me.vickychijwani.spectre.view;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.otto.Subscribe;
import com.squareup.picasso.Callback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import butterknife.Bind;
import io.realm.RealmList;
import me.vickychijwani.spectre.R;
import me.vickychijwani.spectre.event.LoadTagsEvent;
import me.vickychijwani.spectre.event.PostReplacedEvent;
import me.vickychijwani.spectre.event.PostSavedEvent;
import me.vickychijwani.spectre.event.PostSyncedEvent;
import me.vickychijwani.spectre.event.TagsLoadedEvent;
import me.vickychijwani.spectre.model.Post;
import me.vickychijwani.spectre.model.Tag;
import me.vickychijwani.spectre.pref.UserPrefs;
import me.vickychijwani.spectre.util.AppUtils;
import me.vickychijwani.spectre.util.PostUtils;
import me.vickychijwani.spectre.view.fragments.PostEditFragment;
import me.vickychijwani.spectre.view.fragments.PostViewFragment;
import me.vickychijwani.spectre.view.widget.ChipsEditText;
import rx.Observable;
import rx.functions.Action1;

public class PostViewActivity extends BaseActivity implements
        ViewPager.OnPageChangeListener,
        PostViewFragmentPagerAdapter.OnFragmentsInitializedListener,
        View.OnClickListener,
        PostEditFragment.PostTagsManager
{

    private static final String TAG = "PostViewActivity";
    private static boolean sFinishOnStart = false;

    @Bind(R.id.toolbar)                         Toolbar mToolbar;
    @Bind(R.id.toolbar_title)                   TextView mToolbarTitle;
    @Bind(R.id.tabbar)                          TabLayout mTabLayout;
    @Bind(R.id.view_pager)                      ViewPager mViewPager;
    @Bind(R.id.drawer_layout)                   DrawerLayout mDrawerLayout;
    @Bind(R.id.nav_view)                        NavigationView mNavView;

    private PostImageLayoutManager mPostImageLayoutManager = null;
    private ChipsEditText mPostTagsEditText;

    private Post mPost;
    private PostViewFragment mPostViewFragment;
    private PostEditFragment mPostEditFragment;

    private boolean mbPreviewPost = false;
    private ProgressDialog mProgressDialog;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private Runnable mSaveTimeoutRunnable;
    private String mBlogUrl;
    private boolean mbFileStorageEnabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setLayout(R.layout.activity_post_view);

        // ButterKnife doesn't work with the NavigationView's header because it isn't
        // exposed via findViewById: https://code.google.com/p/android/issues/detail?id=190226
        ViewGroup headerView = (ViewGroup) mNavView.getHeaderView(0);
        ViewGroup postImageLayout = (ViewGroup) headerView.findViewById(R.id.post_image_edit_layout);
        mPostImageLayoutManager = new PostImageLayoutManager(postImageLayout);
        mPostTagsEditText = (ChipsEditText) headerView.findViewById(R.id.post_tags_edit);

        setSupportActionBar(mToolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        mBlogUrl = UserPrefs.getInstance(this).getString(UserPrefs.Key.BLOG_URL);

        ArrayAdapter<String> tagSuggestionsAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, Collections.emptyList());
        mPostTagsEditText.setAdapter(tagSuggestionsAdapter);
        mPostTagsEditText.setTokenizer(new ChipsEditText.SpaceTokenizer());
        mPostTagsEditText.setChipBackgroundColor(getResources().getColor(R.color.primary));
        mPostTagsEditText.setChipTextColor(getResources().getColor(R.color.text_primary_inverted));

        // make the field single line, but wrapped instead of scrolling horizontally
        // the Done IME action makes the keyboard close on tapping
        // NOTE: setting these in XML doesn't seem to work
        mPostTagsEditText.setSingleLine(true);
        mPostTagsEditText.setMaxLines(4);
        mPostTagsEditText.setHorizontallyScrolling(false);
        mPostTagsEditText.setImeOptions(EditorInfo.IME_ACTION_DONE);

        mSaveTimeoutRunnable = () -> {
            if (mbPreviewPost) {
                mProgressDialog.dismiss();
                mbPreviewPost = false;
                Toast.makeText(this, R.string.save_post_failed, Toast.LENGTH_LONG).show();
            }
        };

        mPost = getIntent().getExtras().getParcelable(BundleKeys.POST);
        mbFileStorageEnabled = getIntent().getExtras().getBoolean(BundleKeys.FILE_STORAGE_ENABLED);
        mViewPager.setAdapter(new PostViewFragmentPagerAdapter(getSupportFragmentManager(),
                mPost, mbFileStorageEnabled, this));
        mViewPager.removeOnPageChangeListener(this);
        mViewPager.addOnPageChangeListener(this);
        mTabLayout.setupWithViewPager(mViewPager);
        updatePostSettings();
        mPostImageLayoutManager.setOnClickListener(this);

        getBus().post(new LoadTagsEvent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        updatePost(intent.getExtras().getParcelable(BundleKeys.POST));
    }

    @Override
    public void onPostViewFragmentInitialized(PostViewFragment postViewFragment) {
        mPostViewFragment = postViewFragment;
    }

    @Override
    public void onPostEditFragmentInitialized(PostEditFragment postEditFragment) {
        mPostEditFragment = postEditFragment;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (sFinishOnStart) {
            sFinishOnStart = false;
            finish();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Always cancel the request here, this is safe to call even if the image has been loaded.
        // This ensures that the anonymous callback we have does not prevent the activity from
        // being garbage collected. It also prevents our callback from getting invoked after the
        // activity is destroyed.
        getPicasso().cancelRequest(mPostImageLayoutManager.getImageView());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(mSaveTimeoutRunnable);
        mSaveTimeoutRunnable = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.post_view, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_publish).setVisible(mPostEditFragment.shouldShowPublishAction());
        menu.findItem(R.id.action_unpublish).setVisible(mPostEditFragment.shouldShowUnpublishAction());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_view_post:
                viewPostInBrowser(true);
                return true;
            case R.id.action_publish:
                mPostEditFragment.onPublishClicked();
                return true;
            case R.id.action_post_settings:
                mDrawerLayout.openDrawer(mNavView);
                return true;
            case R.id.action_unpublish:
                mPostEditFragment.onPublishUnpublishClicked();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        // try to close the drawer first
        if (mDrawerLayout.isDrawerOpen(mNavView)) {
            mDrawerLayout.closeDrawer(mNavView);
            return;
        }
        // send this Activity to the background instead of destroying, so we can reuse it
        Intent backIntent = getSupportParentActivityIntent();
        if (backIntent != null) {
            backIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(backIntent);
        } else {
            super.onBackPressed();
        }
    }

    public static void setFinishOnStart(boolean finishOnStart) {
        sFinishOnStart = finishOnStart;
    }

    public void viewPostInBrowser(boolean saveBeforeViewing) {
        mbPreviewPost = true;
        Observable<Boolean> waitForNetworkObservable;
        if (saveBeforeViewing) {
            waitForNetworkObservable = mPostEditFragment.onSaveClicked();
        } else {
            waitForNetworkObservable = Observable.just(false);
        }
        Action1<Boolean> waitForNetworkAction = isNetworkCallPending -> {
            if (isNetworkCallPending) {
                mProgressDialog = new ProgressDialog(this);
                mProgressDialog.setIndeterminate(true);
                mProgressDialog.setMessage(getString(R.string.save_post_progress));
                mProgressDialog.setCancelable(false);
                mProgressDialog.show();
                mHandler.postDelayed(mSaveTimeoutRunnable, 10000);
            } else {
                mbPreviewPost = false;
                startBrowserActivity(PostUtils.getPostUrl(mPost));
            }
        };
        waitForNetworkObservable
                .compose(bindToLifecycle())         // ensure unsubscription on activity pause
                .subscribe(waitForNetworkAction);
    }

    @Subscribe
    public void onPostSyncedEvent(PostSyncedEvent event) {
        if (event.uuid.equals(mPost.getUuid()) && mbPreviewPost) {
            mHandler.removeCallbacks(mSaveTimeoutRunnable);
            startBrowserActivity(PostUtils.getPostUrl(mPost));
            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
            }
            mbPreviewPost = false;
        }
    }

    @Subscribe
    public void onTagsLoadedEvent(TagsLoadedEvent event) {
        Set<String> allTags = new HashSet<>(event.tags.size());
        for (Tag tag : event.tags) {
            String tagName = tag.getName();
            if (! allTags.contains(tagName)) {
                allTags.add(tagName);
            }
        }
        // notifyDataSetChanged doesn't work for some reason
        String[] allTagsArray = new String[allTags.size()];
        ArrayAdapter<String> tagSuggestionsAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, allTags.toArray(allTagsArray));
        mPostTagsEditText.setAdapter(tagSuggestionsAdapter);
    }

    @Subscribe
    public void onPostReplacedEvent(PostReplacedEvent event) {
        // FIXME check which post changed before blindly assigning to mPost!
        updatePost(event.newPost);
    }

    @Subscribe
    public void onPostSavedEvent(PostSavedEvent event) {
        if (! mPost.getUuid().equals(event.post.getUuid())) {
            return;
        }
        updatePost(event.post);
    }

    private void updatePost(@NonNull Post newPost) {
        mPost = newPost;
        ((PostViewFragmentPagerAdapter) mViewPager.getAdapter()).setPost(mPost);
        mPostViewFragment.setPost(mPost);
        mPostEditFragment.setPost(mPost, true);
        updatePostSettings();
    }

    private void updatePostSettings() {
        String imageUrl = mPost.getImage();
        if (!TextUtils.isEmpty(imageUrl)) {
            mPostImageLayoutManager.setViewState(PostImageLayoutManager.ViewState.PROGRESS_BAR);
            imageUrl = AppUtils.pathJoin(mBlogUrl, imageUrl);
            getPicasso()
                    .load(imageUrl)
                    .fit().centerCrop()
                    .into(mPostImageLayoutManager.getImageView(), new Callback() {
                        @Override
                        public void onSuccess() {
                            mPostImageLayoutManager.setViewState(PostImageLayoutManager.ViewState.IMAGE);
                        }

                        @Override
                        public void onError() {
                            Toast.makeText(PostViewActivity.this, R.string.post_image_load_error,
                                    Toast.LENGTH_SHORT).show();
                            mPostImageLayoutManager.setViewState(PostImageLayoutManager.ViewState.PLACEHOLDER);
                        }
                    });
        } else {
            mPostImageLayoutManager.setViewState(PostImageLayoutManager.ViewState.PLACEHOLDER);
        }
        List<String> tagStrs = new ArrayList<>();
        for (Tag tag : mPost.getTags()) {
            tagStrs.add(tag.getName());
        }
        mPostTagsEditText.setTokens(tagStrs);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.post_image_edit_layout) {
            PopupMenu popupMenu = new PopupMenu(this, mPostImageLayoutManager.getRootLayout());
            if (mbFileStorageEnabled) {
                popupMenu.inflate(R.menu.insert_image_file_storage_enabled);
            } else {
                popupMenu.inflate(R.menu.insert_image_file_storage_disabled);
            }
            if (TextUtils.isEmpty(mPost.getImage())) {
                MenuItem removeImageItem = popupMenu.getMenu().findItem(R.id.action_image_remove);
                removeImageItem.setVisible(false);
            }
            popupMenu.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.action_insert_image_url) {
                    mPostEditFragment.onInsertImageUrlClicked(getInsertImageDoneAction());
                } else if (item.getItemId() == R.id.action_insert_image_upload) {
                    mPostEditFragment.onInsertImageUploadClicked(getInsertImageDoneAction());
                } else if (item.getItemId() == R.id.action_image_remove) {
                    getInsertImageDoneAction().call("");
                }
                return true;
            });
            popupMenu.show();
        }
    }

    private Action1<String> getInsertImageDoneAction() {
        return (url) -> {
            mPostEditFragment.saveAutomaticallyWithImage(url);
        };
    }

    @Override
    public RealmList<Tag> getTags() {
        RealmList<Tag> tags = new RealmList<>();
        List<String> tagStrs = mPostTagsEditText.getTokens();
        for (String tagStr : tagStrs) {
            tags.add(new Tag(tagStr));
        }
        return tags;
    }

    @Override
    public void onPageSelected(int position) {
        PostViewFragmentPagerAdapter pagerAdapter = (PostViewFragmentPagerAdapter)
                mViewPager.getAdapter();
        Class fragmentType = pagerAdapter.getFragmentType(position);
        if (fragmentType == PostViewFragment.class) {
            onShowPreview();
        } else if (fragmentType == PostEditFragment.class) {
            onShowEditor();
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        // no-op
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        // no-op
    }

    private void onShowPreview() {
        mPostEditFragment.saveToMemory();
        mPostViewFragment.updatePreview();
        supportInvalidateOptionsMenu();
    }

    private void onShowEditor() {
        supportInvalidateOptionsMenu();
    }

    @Override
    public void setTitle(CharSequence title) {
        mToolbarTitle.setText(title);
    }

    @Override
    public void setTitle(int titleId) {
        mToolbarTitle.setText(titleId);
    }



    private final static class PostImageLayoutManager {
        private final ImageView mPostImageView;
        private final ImageView mPostImagePlaceholderView;
        private final TextView mPostImageHintTextView;
        private final ProgressBar mPostImageProgressBar;
        private final ViewGroup mRootLayout;

        public enum ViewState {
            PLACEHOLDER,
            PROGRESS_BAR,
            IMAGE
        }

        public PostImageLayoutManager(ViewGroup rootLayout) {
            mRootLayout = rootLayout;
            mPostImageView = (ImageView) rootLayout.findViewById(R.id.post_image);
            mPostImagePlaceholderView = (ImageView) rootLayout.findViewById(R.id.post_image_placeholder);
            mPostImageHintTextView = (TextView) rootLayout.findViewById(R.id.post_image_hint);
            mPostImageProgressBar = (ProgressBar) rootLayout.findViewById(R.id.post_image_loading);
        }

        public ViewGroup getRootLayout() {
            return mRootLayout;
        }

        public ImageView getImageView() {
            return mPostImageView;
        }

        public void setOnClickListener(View.OnClickListener clickListener) {
            mRootLayout.setOnClickListener(clickListener);
        }

        public void setViewState(ViewState state) {
            switch (state) {
                case PLACEHOLDER:
                    mPostImageView.setVisibility(View.INVISIBLE);
                    mPostImagePlaceholderView.setVisibility(View.VISIBLE);
                    mPostImageHintTextView.setVisibility(View.VISIBLE);
                    mPostImageProgressBar.setVisibility(View.INVISIBLE);
                    break;
                case PROGRESS_BAR:
                    mPostImageView.setVisibility(View.INVISIBLE);
                    mPostImagePlaceholderView.setVisibility(View.INVISIBLE);
                    mPostImageHintTextView.setVisibility(View.INVISIBLE);
                    mPostImageProgressBar.setVisibility(View.VISIBLE);
                    break;
                case IMAGE:
                    mPostImageView.setVisibility(View.VISIBLE);
                    mPostImagePlaceholderView.setVisibility(View.INVISIBLE);
                    mPostImageHintTextView.setVisibility(View.INVISIBLE);
                    mPostImageProgressBar.setVisibility(View.INVISIBLE);
                    break;
            }
        }

    }

}

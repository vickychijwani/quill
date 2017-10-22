package me.vickychijwani.spectre.view;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.squareup.otto.Subscribe;
import com.squareup.picasso.Callback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import butterknife.BindDimen;
import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.functions.Consumer;
import io.realm.RealmList;
import me.vickychijwani.spectre.R;
import me.vickychijwani.spectre.account.AccountManager;
import me.vickychijwani.spectre.event.DeletePostEvent;
import me.vickychijwani.spectre.event.LoadTagsEvent;
import me.vickychijwani.spectre.event.PostDeletedEvent;
import me.vickychijwani.spectre.event.PostReplacedEvent;
import me.vickychijwani.spectre.event.PostSavedEvent;
import me.vickychijwani.spectre.event.PostSyncedEvent;
import me.vickychijwani.spectre.event.TagsLoadedEvent;
import me.vickychijwani.spectre.model.entity.Post;
import me.vickychijwani.spectre.model.entity.Tag;
import me.vickychijwani.spectre.util.NetworkUtils;
import me.vickychijwani.spectre.util.PostUtils;
import me.vickychijwani.spectre.util.functions.Action0;
import me.vickychijwani.spectre.util.functions.Action1;
import me.vickychijwani.spectre.view.fragments.PostEditFragment;
import me.vickychijwani.spectre.view.fragments.PostViewFragment;
import me.vickychijwani.spectre.view.widget.ChipsEditText;

public class PostViewActivity extends BaseActivity implements
        ViewPager.OnPageChangeListener,
        PostViewFragmentPagerAdapter.OnFragmentsInitializedListener,
        View.OnClickListener,
        PostEditFragment.PostSettingsManager,
        TabLayout.OnTabSelectedListener
{

    private static final String TAG = PostViewActivity.class.getSimpleName();
    public static final int RESULT_CODE_DELETED = 1;

    @BindView(R.id.toolbar)                         Toolbar mToolbar;
    @BindView(R.id.toolbar_title)                   TextView mToolbarTitle;
    @BindView(R.id.tabbar)                          TabLayout mTabLayout;
    @BindView(R.id.view_pager)                      ViewPager mViewPager;
    @BindView(R.id.drawer_layout)                   DrawerLayout mDrawerLayout;
    @BindView(R.id.nav_view)                        NavigationView mNavView;

    private FormattingToolbarManager mFormattingToolbarManager = null;
    private PostImageLayoutManager mPostImageLayoutManager = null;
    private ChipsEditText mPostTagsEditText;
    private EditText mPostExcerptEditText;
    private CheckBox mPostFeatureCheckBox;
    private CheckBox mPostPageCheckBox;

    private Post mPost;
    private PostViewFragment mPostViewFragment;
    private PostEditFragment mPostEditFragment;

    private boolean mbPreviewPost = false;
    private ProgressDialog mProgressDialog;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private Runnable mSaveTimeoutRunnable;
    private PostSettingsChangedListener mPostSettingsChangedListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setLayout(R.layout.activity_post_view);

        TypedValue typedColorValue = new TypedValue();
        getTheme().resolveAttribute(R.attr.colorPrimary, typedColorValue, true);
        @ColorInt int colorPrimary = typedColorValue.data;

        // ButterKnife doesn't work with the NavigationView's header because it isn't
        // exposed via findViewById: https://code.google.com/p/android/issues/detail?id=190226
        ViewGroup headerView = (ViewGroup) mNavView.getHeaderView(0);
        ViewGroup postImageLayout = (ViewGroup) headerView.findViewById(R.id.post_image_edit_layout);
        mPostImageLayoutManager = new PostImageLayoutManager(postImageLayout);
        mPostTagsEditText = (ChipsEditText) headerView.findViewById(R.id.post_tags_edit);
        mPostExcerptEditText = (EditText) headerView.findViewById(R.id.post_excerpt);
        mPostFeatureCheckBox = (CheckBox) headerView.findViewById(R.id.post_feature);
        mPostPageCheckBox = (CheckBox) headerView.findViewById(R.id.post_page);

        setSupportActionBar(mToolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        mFormattingToolbarManager = new FormattingToolbarManager((ViewGroup) findViewById(R.id.format_toolbar));

        ArrayAdapter<String> tagSuggestionsAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, Collections.emptyList());
        mPostTagsEditText.setAdapter(tagSuggestionsAdapter);
        mPostTagsEditText.setTokenizer(new ChipsEditText.CommaTokenizer());
        mPostTagsEditText.setChipBackgroundColor(colorPrimary);
        mPostTagsEditText.setChipTextColor(ContextCompat.getColor(this, R.color.text_primary_inverted));

        // make the field single line, but wrapped instead of scrolling horizontally
        // the Done IME action makes the keyboard close on tapping
        // NOTE: setting these in XML doesn't seem to work
        mPostTagsEditText.setSingleLine(true);
        mPostTagsEditText.setMaxLines(4);
        mPostTagsEditText.setHorizontallyScrolling(false);
        mPostTagsEditText.setImeOptions(EditorInfo.IME_ACTION_DONE);

        final Action0 postSettingsChangedHandler = () -> {
            if (mPostSettingsChangedListener != null) {
                mPostSettingsChangedListener.onPostSettingsChanged();
            }
        };
        final TextWatcher settingsTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable e) {
                postSettingsChangedHandler.call();
            }
        };
        mPostTagsEditText.addTextChangedListener(settingsTextWatcher);
        mPostExcerptEditText.addTextChangedListener(settingsTextWatcher);
        mPostFeatureCheckBox.setOnCheckedChangeListener((btn, checked) -> {
            postSettingsChangedHandler.call();
        });
        mPostPageCheckBox.setOnCheckedChangeListener((btn, checked) -> {
            postSettingsChangedHandler.call();
        });

        mSaveTimeoutRunnable = () -> {
            if (mbPreviewPost) {
                mProgressDialog.dismiss();
                mbPreviewPost = false;
                Toast.makeText(this, R.string.save_post_failed, Toast.LENGTH_LONG).show();
            }
        };

        Bundle bundle;
        if (savedInstanceState != null) {
            bundle = savedInstanceState;
        } else {
            bundle = getIntent().getExtras();
        }
        mPost = bundle.getParcelable(BundleKeys.POST);
        //noinspection ConstantConditions
        Crashlytics.log(Log.DEBUG, TAG, "[onCreate] post id = " + mPost.getId());

        @PostViewFragmentPagerAdapter.TabPosition int startingTabPosition =
                PostViewFragmentPagerAdapter.TAB_POSITION_PREVIEW;
        if (bundle.getBoolean(BundleKeys.START_EDITING)) {
            startingTabPosition = PostViewFragmentPagerAdapter.TAB_POSITION_EDIT;
        } else {
            // hide the formatting toolbar in the preview
            mFormattingToolbarManager.hide();
        }
        mViewPager.setAdapter(new PostViewFragmentPagerAdapter(this, getSupportFragmentManager(),
                mPost, this));
        mViewPager.removeOnPageChangeListener(this);
        mViewPager.addOnPageChangeListener(this);
        mViewPager.setCurrentItem(startingTabPosition);
        mTabLayout.setupWithViewPager(mViewPager);
        mTabLayout.addOnTabSelectedListener(this);
        updatePostSettings();
        mPostImageLayoutManager.setOnClickListener(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // if the post is replaced (e.g., right after new post creation) followed by an
        // orientation change, make sure we have the updated post after being re-created
        outState.putParcelable(BundleKeys.POST, mPost);
        outState.putBoolean(BundleKeys.START_EDITING,
                mViewPager.getCurrentItem() == PostViewFragmentPagerAdapter.TAB_POSITION_EDIT);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mPostTagsEditText.getAdapter() == null || mPostTagsEditText.getAdapter().isEmpty()) {
            getBus().post(new LoadTagsEvent());
        }
    }

    @Override
    public void onPostViewFragmentInitialized(PostViewFragment postViewFragment) {
        mPostViewFragment = postViewFragment;
    }

    @Override
    public void onPostEditFragmentInitialized(PostEditFragment postEditFragment) {
        mPostEditFragment = postEditFragment;
        mFormattingToolbarManager.setFormatOptionClickListener(mPostEditFragment);
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
        MenuItem publishItem = menu.findItem(R.id.action_publish);
        MenuItem unpublishItem = menu.findItem(R.id.action_unpublish);
        publishItem.setTitle(mPost.isDraft() ? R.string.publish : R.string.update_post);
        if (mPostEditFragment != null) {
            publishItem.setVisible(mPostEditFragment.shouldShowPublishAction());
            unpublishItem.setVisible(mPostEditFragment.shouldShowUnpublishAction());
        }
        // only drafts can be deleted (scheduled or published posts cannot be deleted at all,
        // to avoid all risk of accidental deletion)
        boolean shouldShowDeleteAction = mPost.isDraft();
        menu.findItem(R.id.action_delete).setVisible(shouldShowDeleteAction);
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
            case R.id.action_delete:
                onDeleteClicked();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(mNavView)) {
            mDrawerLayout.closeDrawer(mNavView);
            return;
        }
        super.onBackPressed();
    }

    public void viewPostInBrowser(boolean saveBeforeViewing) {
        mbPreviewPost = true;
        Observable<Boolean> waitForNetworkObservable;
        if (saveBeforeViewing) {
            waitForNetworkObservable = mPostEditFragment.onSaveClicked();
        } else {
            waitForNetworkObservable = Observable.just(false);
        }
        Consumer<Boolean> waitForNetworkAction = isNetworkCallPending -> {
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
        disposeOnPause(
                waitForNetworkObservable.subscribe(waitForNetworkAction));
    }

    private void onDeleteClicked() {
        // confirm deletion
        final AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.alert_delete_draft_title)
                .setMessage(R.string.alert_delete_draft_msg)
                .setPositiveButton(R.string.alert_delete_yes, (dialog, which) -> {
                    getBus().post(new DeletePostEvent(mPost));
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.alert_delete_no, (dialog, which) -> {
                    dialog.dismiss();
                })
                .create();
        alertDialog.show();
    }

    @Subscribe
    public void onPostSyncedEvent(PostSyncedEvent event) {
        if (event.id.equals(mPost.getId()) && mbPreviewPost) {
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
        if (! mPost.getId().equals(event.post.getId())) {
            return;
        }
        updatePost(event.post);
    }

    @Subscribe
    public void onPostDeletedEvent(PostDeletedEvent event) {
        setResult(RESULT_CODE_DELETED);
        finish();
    }

    private void updatePost(@NonNull Post newPost) {
        mPost = newPost;
        ((PostViewFragmentPagerAdapter) mViewPager.getAdapter()).setPost(mPost);
        // Crashlytics issue 104: fragments can be null when a draft gets uploaded right
        // after this screen is opened, but *before* the fragments could be initialized.
        // No need to handle this separately, as the ViewPager's adapter will correctly pass
        // the new post to these fragments when creating them later.
        if (mPostViewFragment != null) {
            mPostViewFragment.setPost(mPost);
        }
        if (mPostEditFragment != null) {
            mPostEditFragment.setPost(mPost, true);
        }
        updatePostSettings();
    }

    private void updatePostSettings() {
        String imageUrl = mPost.getFeatureImage();
        if (!TextUtils.isEmpty(imageUrl)) {
            mPostImageLayoutManager.setViewState(PostImageLayoutManager.ViewState.PROGRESS_BAR);
            String blogUrl = AccountManager.getActiveBlogUrl();
            imageUrl = NetworkUtils.makeAbsoluteUrl(blogUrl, imageUrl);
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
        mPostExcerptEditText.setText(mPost.getCustomExcerpt());
        mPostFeatureCheckBox.setChecked(mPost.isFeatured());
        mPostPageCheckBox.setChecked(mPost.isPage());
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.post_image_edit_layout) {
            PopupMenu popupMenu = new PopupMenu(this, mPostImageLayoutManager.getRootLayout());
            popupMenu.inflate(R.menu.insert_image);
            if (TextUtils.isEmpty(mPost.getFeatureImage())) {
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
    public void setOnPostSettingsChangedListener(@NonNull PostSettingsChangedListener listener) {
        mPostSettingsChangedListener = listener;
    }

    @Override
    public void removeOnPostSettingsChangedListener() {
        mPostSettingsChangedListener = null;
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
    public String getCustomExcerpt() {
        return mPostExcerptEditText.getText().toString();
    }

    @Override
    public boolean isFeatured() {
        return mPostFeatureCheckBox.isChecked();
    }

    public boolean isPage() {
        return mPostPageCheckBox.isChecked();
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
        if (position == PostViewFragmentPagerAdapter.TAB_POSITION_EDIT) {
            mFormattingToolbarManager.translateToolbar(positionOffset);
        } else if (position == PostViewFragmentPagerAdapter.TAB_POSITION_PREVIEW) {
            mFormattingToolbarManager.translateToolbar(1-positionOffset);
        }
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
        if (mPostEditFragment != null) {
            mPostEditFragment.restoreSelectionState();
        }
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

    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        // no-op
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {
        if (tab.getPosition() == PostViewFragmentPagerAdapter.TAB_POSITION_EDIT) {
            // can't do this in onPageSelected because that is called *after* the focus changes
            mPostEditFragment.saveSelectionState();
        }
    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {
        // no-op
    }


    public final static class FormattingToolbarManager implements View.OnClickListener {
        @BindDimen(R.dimen.format_toolbar_height)   int mFormattingToolbarHeight;

        final ViewGroup mFormattingToolbar;
        FormatOptionClickListener mFormatOptionClickListener = null;

        public FormattingToolbarManager(ViewGroup formattingToolbar) {
            ButterKnife.bind(this, formattingToolbar);
            mFormattingToolbar = formattingToolbar;
            ViewGroup buttonContainer = mFormattingToolbar;
            while (buttonContainer.getChildAt(0) instanceof ViewGroup) {
                buttonContainer = (ViewGroup) mFormattingToolbar.getChildAt(0);
            }
            for (int i = 0, childCount = buttonContainer.getChildCount(); i < childCount; ++i) {
                buttonContainer.getChildAt(i).setOnClickListener(this);
            }
        }

        public void translateToolbar(float offset) {
            mFormattingToolbar.setTranslationY(mFormattingToolbarHeight * offset);
        }

        public void hide() {
            translateToolbar(1f);
        }

        public void setFormatOptionClickListener(@NonNull FormatOptionClickListener listener) {
            mFormatOptionClickListener = listener;
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.format_bold:
                    mFormatOptionClickListener.onFormatBoldClicked(v);
                    break;
                case R.id.format_italic:
                    mFormatOptionClickListener.onFormatItalicClicked(v);
                    break;
                case R.id.format_link:
                    mFormatOptionClickListener.onFormatLinkClicked(v);
                    break;
                case R.id.format_image:
                    mFormatOptionClickListener.onFormatImageClicked(v);
                    break;
                default:
                    throw new IllegalArgumentException("No listener method assigned to this view!");
            }
        }
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


    public interface PostSettingsChangedListener {
        void onPostSettingsChanged();
    }

}

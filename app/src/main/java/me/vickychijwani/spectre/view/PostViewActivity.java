package me.vickychijwani.spectre.view;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.NavUtils;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.squareup.otto.Subscribe;

import butterknife.Bind;
import butterknife.BindColor;
import me.vickychijwani.spectre.R;
import me.vickychijwani.spectre.event.LoadPostEvent;
import me.vickychijwani.spectre.event.PostLoadedEvent;
import me.vickychijwani.spectre.event.PostReplacedEvent;
import me.vickychijwani.spectre.event.PostSyncedEvent;
import me.vickychijwani.spectre.model.Post;
import me.vickychijwani.spectre.util.AppUtils;
import me.vickychijwani.spectre.util.KeyboardUtils;
import me.vickychijwani.spectre.util.PostUtils;
import me.vickychijwani.spectre.view.fragments.PostEditFragment;
import me.vickychijwani.spectre.view.fragments.PostViewFragment;
import rx.Observable;
import rx.functions.Action1;

public class PostViewActivity extends BaseActivity implements
        PostViewFragment.OnEditClickListener,
        PostEditFragment.OnPreviewClickListener {

    private static final String TAG = "PostViewActivity";

    private static final String TAG_VIEW_FRAGMENT = "tag:view_fragment";
    private static final String TAG_EDIT_FRAGMENT = "tag:edit_fragment";
    private static final String KEY_IS_PREVIEW_VISIBLE = "key:is_preview_visible";

    @BindColor(R.color.primary)                 int mColorPrimary;
    @BindColor(android.R.color.transparent)     int mColorTransparent;

    @Bind(R.id.toolbar)                         Toolbar mToolbar;
    @Bind(R.id.toolbar_scrim)                   View mToolbarScrimView;

    private Post mPost;
    private PostViewFragment mPostViewFragment;
    private PostEditFragment mPostEditFragment;
    private View.OnClickListener mUpClickListener;

    private boolean mbIsPreviewVisible = false;
    private boolean mbIsKeyboardVisible = false;
    private boolean mbWasKeyboardVisibleInEditor = false;

    private boolean mbPreviewPost = false;
    private ProgressDialog mProgressDialog;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private Runnable mSaveTimeoutRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setLayout(R.layout.activity_post_edit);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        getBus().post(new LoadPostEvent(getIntent().getExtras().getString(BundleKeys.POST_UUID)));

        mUpClickListener = v -> NavUtils.navigateUpFromSameTask(PostViewActivity.this);
        mSaveTimeoutRunnable = () -> {
            if (mbPreviewPost) {
                mProgressDialog.dismiss();
                mbPreviewPost = false;
                Toast.makeText(this, R.string.save_post_failed, Toast.LENGTH_LONG).show();
            }
        };

        mToolbarScrimView.setBackground(AppUtils.makeCubicGradientScrimDrawable(0xaa000000, 2,
                Gravity.TOP));

        KeyboardUtils.addKeyboardVisibilityChangedListener(visible -> {
            mbIsKeyboardVisible = visible;
        }, this);

        mPostViewFragment = (PostViewFragment) getSupportFragmentManager()
                .findFragmentByTag(TAG_VIEW_FRAGMENT);
        if (mPostViewFragment == null) {
            mPostViewFragment = PostViewFragment.newInstance();
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.fragment_container, mPostViewFragment, TAG_VIEW_FRAGMENT)
                    .commit();
        }

        mPostEditFragment = (PostEditFragment) getSupportFragmentManager()
                .findFragmentByTag(TAG_EDIT_FRAGMENT);
        if (mPostEditFragment == null) {
            boolean fileStorageEnabled = getIntent().getExtras().getBoolean(BundleKeys.FILE_STORAGE_ENABLED);
            mPostEditFragment = PostEditFragment.newInstance(fileStorageEnabled);
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.fragment_container, mPostEditFragment, TAG_EDIT_FRAGMENT)
                    .commit();
        }
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState.getBoolean(KEY_IS_PREVIEW_VISIBLE, false)) {
            onPreviewClicked();
        }
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
        boolean isDraftOrPublished = Post.PUBLISHED.equals(mPost.getStatus())
                || Post.DRAFT.equals(mPost.getStatus());
        menu.findItem(R.id.action_view_post).setVisible(isDraftOrPublished);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_view_post:
                viewPostInBrowser(true);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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
            mProgressDialog.dismiss();
            mbPreviewPost = false;
        }
    }

    @Subscribe
    public void onPostLoadedEvent(PostLoadedEvent event) {
        mPost = event.post;
    }

    @Subscribe
    public void onPostChangedEvent(PostReplacedEvent event) {
        // FIXME check which post changed before blindly assigning to mPost!
        mPost = event.newPost;
        mPostViewFragment.setPost(mPost);
        mPostEditFragment.setPost(mPost, true);
    }

    @Override
    public void onPreviewClicked() {
        if (mbIsKeyboardVisible) {
            mbWasKeyboardVisibleInEditor = mbIsKeyboardVisible;
            KeyboardUtils.hideKeyboard(this);
        }
        setToolbarBackgroundOpaque(true);
        mPostEditFragment.hide();
        mPostViewFragment.show();
        supportInvalidateOptionsMenu();
        mbIsPreviewVisible = true;
    }

    @Override
    public void onEditClicked() {
        mPostViewFragment.hide();
        mPostEditFragment.show();
        if (mbWasKeyboardVisibleInEditor) {
            KeyboardUtils.showKeyboard(this);
        }
        supportInvalidateOptionsMenu();
        mbIsPreviewVisible = false;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_IS_PREVIEW_VISIBLE, mbIsPreviewVisible);
    }

    public void setToolbarBackgroundOpaque(boolean opaque) {
        mToolbar.setBackgroundColor(opaque ? mColorPrimary : mColorTransparent);
    }

    public void setToolbarScrimAlpha(float alpha) {
        mToolbarScrimView.setAlpha(alpha);
    }

    public void setNavigationItem(int iconResId, View.OnClickListener clickListener) {
        mToolbar.setNavigationIcon(iconResId);
        mToolbar.setNavigationOnClickListener(clickListener);
    }

    public void resetNavigationItem() {
        mToolbar.setNavigationIcon(R.drawable.arrow_left);
        mToolbar.setNavigationOnClickListener(mUpClickListener);
    }

    public Post getPost() {
        return mPost;
    }

}

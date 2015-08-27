package me.vickychijwani.spectre.view;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.otto.Subscribe;

import butterknife.Bind;
import me.vickychijwani.spectre.R;
import me.vickychijwani.spectre.event.LoadPostEvent;
import me.vickychijwani.spectre.event.PostLoadedEvent;
import me.vickychijwani.spectre.event.PostReplacedEvent;
import me.vickychijwani.spectre.event.PostSyncedEvent;
import me.vickychijwani.spectre.model.Post;
import me.vickychijwani.spectre.util.PostUtils;
import me.vickychijwani.spectre.view.fragments.PostEditFragment;
import me.vickychijwani.spectre.view.fragments.PostViewFragment;
import rx.Observable;
import rx.functions.Action1;

public class PostViewActivity extends BaseActivity implements ViewPager.OnPageChangeListener,
        PostViewFragmentPagerAdapter.OnFragmentsInitializedListener {

    private static final String TAG = "PostViewActivity";

    @Bind(R.id.toolbar)                         Toolbar mToolbar;
    @Bind(R.id.toolbar_title)                   TextView mToolbarTitle;
    @Bind(R.id.tabbar)                          TabLayout mTabLayout;
    @Bind(R.id.view_pager)                      ViewPager mViewPager;

    private Post mPost;
    private PostViewFragment mPostViewFragment;
    private PostEditFragment mPostEditFragment;

    private boolean mbPreviewPost = false;
    private ProgressDialog mProgressDialog;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private Runnable mSaveTimeoutRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setLayout(R.layout.activity_post_view);
        setSupportActionBar(mToolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        getBus().post(new LoadPostEvent(getIntent().getExtras().getString(BundleKeys.POST_UUID)));

        mSaveTimeoutRunnable = () -> {
            if (mbPreviewPost) {
                mProgressDialog.dismiss();
                mbPreviewPost = false;
                Toast.makeText(this, R.string.save_post_failed, Toast.LENGTH_LONG).show();
            }
        };

        // wait for the post to load, then initialize fragments, etc.
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
        boolean fileStorageEnabled = getIntent().getExtras()
                .getBoolean(BundleKeys.FILE_STORAGE_ENABLED);
        mViewPager.setAdapter(new PostViewFragmentPagerAdapter(getSupportFragmentManager(),
                fileStorageEnabled, this));
        mViewPager.addOnPageChangeListener(this);
        mTabLayout.setupWithViewPager(mViewPager);
    }

    @Subscribe
    public void onPostChangedEvent(PostReplacedEvent event) {
        // FIXME check which post changed before blindly assigning to mPost!
        mPost = event.newPost;
        mPostViewFragment.setPost(mPost);
        mPostEditFragment.setPost(mPost, true);
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

    public Post getPost() {
        return mPost;
    }

}

package me.vickychijwani.spectre.view;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.squareup.otto.Subscribe;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.OnClick;
import me.vickychijwani.spectre.BuildConfig;
import me.vickychijwani.spectre.R;
import me.vickychijwani.spectre.SpectreApplication;
import me.vickychijwani.spectre.event.BlogSettingsLoadedEvent;
import me.vickychijwani.spectre.event.ConfigurationLoadedEvent;
import me.vickychijwani.spectre.event.CreatePostEvent;
import me.vickychijwani.spectre.event.DataRefreshedEvent;
import me.vickychijwani.spectre.event.ForceCancelRefreshEvent;
import me.vickychijwani.spectre.event.LogoutEvent;
import me.vickychijwani.spectre.event.LogoutStatusEvent;
import me.vickychijwani.spectre.event.PostCreatedEvent;
import me.vickychijwani.spectre.event.PostsLoadedEvent;
import me.vickychijwani.spectre.event.RefreshDataEvent;
import me.vickychijwani.spectre.event.UserLoadedEvent;
import me.vickychijwani.spectre.model.ConfigurationParam;
import me.vickychijwani.spectre.model.Post;
import me.vickychijwani.spectre.model.Setting;
import me.vickychijwani.spectre.pref.AppState;
import me.vickychijwani.spectre.pref.UserPrefs;
import me.vickychijwani.spectre.util.AppUtils;
import me.vickychijwani.spectre.util.NetworkUtils;
import me.vickychijwani.spectre.view.image.BorderedCircleTransformation;
import me.vickychijwani.spectre.view.widget.SpaceItemDecoration;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.mime.TypedByteArray;

public class PostListActivity extends BaseActivity {

    private static final String TAG = "PostListActivity";

    private final List<Post> mPosts = new ArrayList<>();
    private PostAdapter mPostAdapter;

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private Runnable mRefreshDataRunnable;
    private Runnable mRefreshTimeoutRunnable;

    private boolean mFileStorageEnabled = true;

    private static final int REFRESH_FREQUENCY = 10 * 60 * 1000;    // in milliseconds

    // NOTE: very large timeout is needed for cases like initial sync on a blog with 100s of posts
    private static final int REFRESH_TIMEOUT = 5 * 60 * 1000;       // in milliseconds

    @Bind(R.id.toolbar)                     Toolbar mToolbar;
    @Bind(R.id.app_bar_bg)                  View mAppBarBg;
    @Bind(R.id.user_image)                  ImageView mUserImageView;
    @Bind(R.id.user_blog_title)             TextView mBlogTitleView;
    @Bind(R.id.swipe_refresh_layout)        SwipeRefreshLayout mSwipeRefreshLayout;
    @Bind(R.id.post_list_container)         FrameLayout mPostListContainer;
    @Bind(R.id.post_list)                   RecyclerView mPostList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (! AppState.getInstance(this).getBoolean(AppState.Key.LOGGED_IN)) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        setLayout(R.layout.activity_post_list);
        setSupportActionBar(mToolbar);
        if (BuildConfig.DEBUG) {
            SpectreApplication.getInstance().addDebugDrawer(this);
        }

        // get rid of the default action bar confetti
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayOptions(0);

        // initialize post list UI
        UserPrefs prefs = UserPrefs.getInstance(this);
        String blogUrl = prefs.getString(UserPrefs.Key.BLOG_URL);
        mPostAdapter = new PostAdapter(this, mPosts, blogUrl, getPicasso(), v -> {
            int pos = mPostList.getChildLayoutPosition(v);
            if (pos == RecyclerView.NO_POSITION) return;
            Post post = (Post) mPostAdapter.getItem(pos);
            Intent intent = new Intent(PostListActivity.this, PostViewActivity.class);
            intent.putExtra(BundleKeys.POST_UUID, post.getUuid());
            intent.putExtra(BundleKeys.FILE_STORAGE_ENABLED, mFileStorageEnabled);
            startActivity(intent);
        });
        mPostList.setAdapter(mPostAdapter);
        mPostList.setLayoutManager(new LinearLayoutManager(this));
        mPostList.setItemAnimator(new DefaultItemAnimator());
        int hSpace = getResources().getDimensionPixelOffset(R.dimen.padding_default_card_h);
        int vSpace = getResources().getDimensionPixelOffset(R.dimen.padding_default_card_v);
        mPostList.addItemDecoration(new SpaceItemDecoration(hSpace, vSpace));

        final Drawable appbarShadowDrawable;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            appbarShadowDrawable = getResources().getDrawable(R.drawable.appbar_shadow, getTheme());
        } else {
            appbarShadowDrawable = getResources().getDrawable(R.drawable.appbar_shadow);
        }
        mPostListContainer.setForeground(null);     // hide the shadow initially
        mPostList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int scrollY = mPostList.computeVerticalScrollOffset();
                mAppBarBg.setTranslationY(-scrollY);
                mPostListContainer.setForeground(scrollY <= 0 ? null : appbarShadowDrawable);
            }
        });

        mRefreshDataRunnable = () -> refreshData(false);
        mRefreshTimeoutRunnable = this::refreshTimedOut;
        mSwipeRefreshLayout.setColorSchemeResources(R.color.accent, R.color.primary);
        mSwipeRefreshLayout.setOnRefreshListener(() -> refreshData(false));

        // load cached data immediately
        refreshData(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshData(false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        cancelDataRefresh();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRefreshDataRunnable = null;    // the runnable holds an implicit reference to the activity!
                                        // allow it to get GC'ed to avoid a memory leak
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.post_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_view_homepage:
                UserPrefs prefs = UserPrefs.getInstance(SpectreApplication.getInstance());
                startBrowserActivity(prefs.getString(UserPrefs.Key.BLOG_URL));
                return true;
            case R.id.action_refresh:
                refreshData(false);
                return true;
            case R.id.action_feedback:
                AppUtils.emailDeveloper(this);
                return true;
            case R.id.action_about:
                Intent aboutIntent = new Intent(this, AboutActivity.class);
                startActivity(aboutIntent);
                return true;
            case R.id.action_logout:
                getBus().post(new LogoutEvent(false));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Subscribe
    public void onDataRefreshedEvent(DataRefreshedEvent event) {
        mSwipeRefreshLayout.setRefreshing(false);
        cancelRefreshTimeout();
        scheduleDataRefresh();

        RetrofitError error = event.error;
        if (error == null || ! NetworkUtils.isRealError(error)) {
            return;
        }

        Response response = error.getResponse();
        if (error.getKind() == RetrofitError.Kind.NETWORK
                && (error.getCause() instanceof ConnectException
                || error.getCause() instanceof SocketTimeoutException)) {
            Toast.makeText(this, R.string.network_timeout, Toast.LENGTH_LONG).show();
        } else {
            Crashlytics.log(Log.ERROR, TAG, "generic error message triggered during refresh");
            if (response != null) {
                Crashlytics.log(Log.ERROR, TAG, "response: " +
                        new String(((TypedByteArray) response.getBody()).getBytes()));
            }
            Crashlytics.logException(error);
            Toast.makeText(this, R.string.refresh_failed, Toast.LENGTH_LONG).show();
        }
    }

    @Subscribe
    public void onUserLoadedEvent(UserLoadedEvent event) {
        if (event.user.getImage() != null) {
            if (event.user.getImage().isEmpty()) {
                return;
            }
            String blogUrl = UserPrefs.getInstance(this).getString(UserPrefs.Key.BLOG_URL);
            String imageUrl = AppUtils.pathJoin(blogUrl, event.user.getImage());
            getPicasso()
                    .load(imageUrl)
                    .transform(new BorderedCircleTransformation())
                    .fit()
                    .into(mUserImageView);
        } else {
            // Crashlytics issue #77
            Crashlytics.logException(new Exception("user image is null!"));
        }
    }

    @Subscribe
    public void onBlogSettingsLoadedEvent(BlogSettingsLoadedEvent event) {
        String blogTitle = getString(R.string.app_name);
        for (Setting setting : event.settings) {
            if (setting.getKey().equals("title")) {
                blogTitle = setting.getValue();
            }
        }
        mBlogTitleView.setText(blogTitle);
    }

    @Subscribe
    public void onConfigurationLoadedEvent(ConfigurationLoadedEvent event) {
        for (ConfigurationParam param : event.params) {
            if (param.getKey().equals("fileStorage")) {
                mFileStorageEnabled = Boolean.valueOf(param.getValue());
            }
        }
    }

    @Subscribe
    public void onPostsLoadedEvent(PostsLoadedEvent event) {
        mPosts.clear();
        mPosts.addAll(event.posts);
        if (mPosts.size() >= event.postsFetchLimit) {
            CharSequence message = Html.fromHtml(String.format(
                    getString(R.string.post_limit_exceeded),
                    getString(R.string.app_name), event.postsFetchLimit,
                    "https://github.com/vickychijwani/quill/issues/81"));
            mPostAdapter.showFooter(message);
        } else {
            mPostAdapter.hideFooter();
        }
        mPostAdapter.notifyDataSetChanged();
    }

    @OnClick(R.id.new_post_btn)
    public void onNewPostBtnClicked() {
        getBus().post(new CreatePostEvent());
    }

    @Subscribe
    public void onPostCreatedEvent(PostCreatedEvent event) {
        Intent intent = new Intent(PostListActivity.this, PostViewActivity.class);
        intent.putExtra(BundleKeys.POST_UUID, event.newPost.getUuid());
        intent.putExtra(BundleKeys.FILE_STORAGE_ENABLED, mFileStorageEnabled);
        startActivity(intent);
    }

    @Subscribe
    public void onLogoutStatusEvent(LogoutStatusEvent event) {
        if (!event.succeeded && event.hasPendingActions) {
            final AlertDialog alertDialog = new AlertDialog.Builder(this)
                    .setMessage(getString(R.string.unsynced_changes_msg))
                    .setPositiveButton(R.string.dont_logout, (dialog, which) -> {
                        dialog.dismiss();
                    })
                    .setNegativeButton(R.string.logout, (dialog, which) -> {
                        dialog.dismiss();
                        getBus().post(new LogoutEvent(true));
                    })
                    .create();
            alertDialog.show();
        } else {
            finish();
            Intent logoutIntent = new Intent(this, LoginActivity.class);
            startActivity(logoutIntent);
        }
    }


    // private methods
    private void scheduleDataRefresh() {
        // cancel already-scheduled refresh event
        cancelDataRefresh();
        // NOTE do not pass this::refreshData directly, because that creates a new Runnable and
        // hence cannot be removed using Handler.removeCallbacks later, indirectly causing the
        // entire Activity to leak!
        mHandler.postDelayed(mRefreshDataRunnable, REFRESH_FREQUENCY);
    }

    private void cancelDataRefresh() {
        mHandler.removeCallbacks(mRefreshDataRunnable);
        cancelRefreshTimeout();
    }

    private void refreshData(boolean loadCachedData) {
        getBus().post(new RefreshDataEvent(loadCachedData));
        mHandler.postDelayed(mRefreshTimeoutRunnable, REFRESH_TIMEOUT);
    }

    private void cancelRefreshTimeout() {
        mHandler.removeCallbacks(mRefreshTimeoutRunnable);
    }

    private void refreshTimedOut() {
        getBus().post(new ForceCancelRefreshEvent());
        mSwipeRefreshLayout.setRefreshing(false);
        Toast.makeText(this, R.string.refresh_failed, Toast.LENGTH_LONG).show();
        scheduleDataRefresh();
    }

}

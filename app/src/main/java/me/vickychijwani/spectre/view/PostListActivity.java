package me.vickychijwani.spectre.view;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.CardView;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.otto.Subscribe;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.vickychijwani.spectre.R;
import me.vickychijwani.spectre.SpectreApplication;
import me.vickychijwani.spectre.event.BlogSettingsLoadedEvent;
import me.vickychijwani.spectre.event.CreatePostEvent;
import me.vickychijwani.spectre.event.DataRefreshedEvent;
import me.vickychijwani.spectre.event.LogoutEvent;
import me.vickychijwani.spectre.event.PostCreatedEvent;
import me.vickychijwani.spectre.event.PostsLoadedEvent;
import me.vickychijwani.spectre.event.RefreshDataEvent;
import me.vickychijwani.spectre.event.UserLoadedEvent;
import me.vickychijwani.spectre.model.Post;
import me.vickychijwani.spectre.model.Setting;
import me.vickychijwani.spectre.model.Tag;
import me.vickychijwani.spectre.network.BorderedCircleTransformation;
import me.vickychijwani.spectre.pref.AppState;
import me.vickychijwani.spectre.pref.UserPrefs;
import me.vickychijwani.spectre.util.AppUtils;
import me.vickychijwani.spectre.util.PostUtils;
import me.vickychijwani.spectre.view.widget.SpaceItemDecoration;

public class PostListActivity extends BaseActivity {

    private static final String TAG = "PostListActivity";

    private List<Post> mPosts = new ArrayList<>();
    private PostAdapter mPostAdapter;

    private Handler mHandler = new Handler(Looper.getMainLooper());
    private Runnable mRefreshDataRunnable;
    private static final int REFRESH_FREQUENCY = 10 * 60 * 1000;  // milliseconds

    @Bind(R.id.toolbar)
    Toolbar mToolbar;

    @Bind(R.id.toolbar_card)
    CardView mToolbarCard;

    @Bind(R.id.app_bar_bg)
    View mAppBarBg;

    @Bind(R.id.user_image)
    ImageView mUserImageView;

    @Bind(R.id.user_blog_title)
    TextView mBlogTitleView;

    @Bind(R.id.swipe_refresh_layout)
    SwipeRefreshLayout mSwipeRefreshLayout;

    @Bind(R.id.post_list)
    RecyclerView mPostList;

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

        // get rid of the default action bar confetti
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayOptions(0);

        // initialize post list UI
        UserPrefs prefs = UserPrefs.getInstance(this);
        String blogUrl = prefs.getString(UserPrefs.Key.BLOG_URL);
        mPostAdapter = new PostAdapter(this, mPosts, blogUrl, getPicasso(), v -> {
            int pos = mPostList.getChildLayoutPosition(v);
            if (pos == RecyclerView.NO_POSITION) return;
            Post post = mPostAdapter.getItem(pos);
            Intent intent = new Intent(PostListActivity.this, PostViewActivity.class);
            intent.putExtra(BundleKeys.POST_UUID, post.getUuid());
            startActivity(intent);
        });
        mPostList.setAdapter(mPostAdapter);
        mPostList.setLayoutManager(new LinearLayoutManager(this));
        mPostList.setItemAnimator(new DefaultItemAnimator());
        int hSpace = getResources().getDimensionPixelOffset(R.dimen.padding_default_card_h);
        int vSpace = getResources().getDimensionPixelOffset(R.dimen.padding_default_card_v);
        mPostList.addItemDecoration(new SpaceItemDecoration(hSpace, vSpace));

        final int toolbarElevation = getResources()
                .getDimensionPixelOffset(R.dimen.toolbar_elevation);
        mPostList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int scrollY = mPostList.computeVerticalScrollOffset();
                mAppBarBg.setTranslationY(-scrollY);
                mToolbarCard.setCardElevation(scrollY <= 0 ? 0 : toolbarElevation);
            }
        });

        mRefreshDataRunnable = this::refreshData;
        mSwipeRefreshLayout.setColorSchemeResources(R.color.accent, R.color.primary);
        mSwipeRefreshLayout.setOnRefreshListener(this::refreshData);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshData();
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
                refreshData();
                return true;
            case R.id.action_about:
                Intent aboutIntent = new Intent(this, AboutActivity.class);
                startActivity(aboutIntent);
                return true;
            case R.id.action_logout:
                getBus().post(new LogoutEvent());
                finish();
                Intent logoutIntent = new Intent(this, LoginActivity.class);
                startActivity(logoutIntent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Subscribe
    public void onDataRefreshedEvent(DataRefreshedEvent event) {
        mSwipeRefreshLayout.setRefreshing(false);
        scheduleDataRefresh();
    }

    @Subscribe
    public void onUserLoadedEvent(UserLoadedEvent event) {
        UserPrefs prefs = UserPrefs.getInstance(this);
        String imageUrl = AppUtils.pathJoin(prefs.getString(UserPrefs.Key.BLOG_URL),
                event.user.getImage());
        getPicasso()
                .load(imageUrl)
                .transform(new BorderedCircleTransformation())
                .fit()
                .into(mUserImageView);
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
    public void onPostsLoadedEvent(PostsLoadedEvent event) {
        mPosts.clear();
        mPosts.addAll(event.posts);
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
        startActivity(intent);
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
    }

    private void refreshData() {
        getBus().post(new RefreshDataEvent());
    }


    static class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

        private final LayoutInflater mLayoutInflater;
        private final List<Post> mPosts;
        private final Context mContext;
        private final String mBlogUrl;
        private final Picasso mPicasso;
        private final View.OnClickListener mItemClickListener;

        public PostAdapter(Context context, List<Post> posts, String blogUrl, Picasso picasso,
                           View.OnClickListener itemClickListener) {
            mContext = context;
            mBlogUrl = blogUrl;
            mPicasso = picasso;
            mLayoutInflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
            mPosts = posts;
            mItemClickListener = itemClickListener;
            setHasStableIds(true);
        }

        @Override
        public int getItemCount() {
            return mPosts.size();
        }

        public Post getItem(int position) {
            return mPosts.get(position);
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).getUuid().hashCode();
        }

        @Override
        public PostViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = mLayoutInflater.inflate(R.layout.post_list_item, parent, false);
            return new PostViewHolder(view, mItemClickListener);
        }

        @Override
        public void onBindViewHolder(PostViewHolder viewHolder, int position) {
            Post post = getItem(position);
            viewHolder.title.setText(post.getTitle());
            if (! TextUtils.isEmpty(post.getImage())) {
                String imageUrl = AppUtils.pathJoin(mBlogUrl, post.getImage());
                viewHolder.image.setVisibility(View.VISIBLE);
                mPicasso.load(imageUrl).into(viewHolder.image);
            } else {
                viewHolder.image.setVisibility(View.GONE);
            }
            viewHolder.published.setText(PostUtils.getStatusString(post, mContext));
            viewHolder.published.setTextColor(PostUtils.getStatusColor(post, mContext));
            List<Tag> tags = post.getTags();
            if (tags.size() > 0) {
                String tagsStr = "#" + tags.get(0).getName();
                if (tags.size() > 1) {
                    tagsStr += " +" + (tags.size()-1);
                }
                viewHolder.tags.setText(tagsStr);
                viewHolder.tags.setVisibility(View.VISIBLE);
            } else {
                viewHolder.tags.setVisibility(View.GONE);
            }
        }

        static class PostViewHolder extends RecyclerView.ViewHolder {
            @Bind(R.id.post_title)        TextView title;
            @Bind(R.id.post_published)    TextView published;
            @Bind(R.id.post_image)        ImageView image;
            @Bind(R.id.post_tags)         TextView tags;

            public PostViewHolder(@NonNull View view, View.OnClickListener clickListener) {
                super(view);
                ButterKnife.bind(this, view);
                view.setOnClickListener(clickListener);
            }
        }

    }

}

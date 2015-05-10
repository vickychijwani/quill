package me.vickychijwani.spectre.view;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.squareup.otto.Subscribe;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import me.vickychijwani.spectre.R;
import me.vickychijwani.spectre.event.BlogSettingsLoadedEvent;
import me.vickychijwani.spectre.event.CreatePostEvent;
import me.vickychijwani.spectre.event.DataRefreshedEvent;
import me.vickychijwani.spectre.event.LoadBlogSettingsEvent;
import me.vickychijwani.spectre.event.LoadPostsEvent;
import me.vickychijwani.spectre.event.LoadUserEvent;
import me.vickychijwani.spectre.event.LogoutEvent;
import me.vickychijwani.spectre.event.PostCreatedEvent;
import me.vickychijwani.spectre.event.PostsLoadedEvent;
import me.vickychijwani.spectre.event.RefreshDataEvent;
import me.vickychijwani.spectre.event.UserLoadedEvent;
import me.vickychijwani.spectre.model.Post;
import me.vickychijwani.spectre.model.Setting;
import me.vickychijwani.spectre.network.BorderedCircleTransformation;
import me.vickychijwani.spectre.pref.AppState;
import me.vickychijwani.spectre.pref.UserPrefs;
import me.vickychijwani.spectre.util.AppUtils;
import me.vickychijwani.spectre.util.DateTimeUtils;

public class PostListActivity extends BaseActivity {

    private static final String TAG = "PostListActivity";

    private List<Post> mPosts;
    private PostAdapter mPostAdapter;

    private Handler mHandler;
    private static final int REFRESH_FREQUENCY = 10 * 60 * 1000;  // milliseconds

    @InjectView(R.id.toolbar)
    Toolbar mToolbar;

    @InjectView(R.id.user_image)
    ImageView mUserImageView;

    @InjectView(R.id.user_blog_title)
    TextView mBlogTitleView;

    @InjectView(R.id.swipe_refresh_layout)
    SwipeRefreshLayout mSwipeRefreshLayout;

    @InjectView(R.id.post_list)
    ListView mPostList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (! AppState.getInstance(this).getBoolean(AppState.Key.LOGGED_IN)) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_post_list);
        ButterKnife.inject(this);
        setSupportActionBar(mToolbar);

        // get rid of the default action bar confetti
        getSupportActionBar().setDisplayOptions(0);

        // initialize post list UI
        mPosts = new ArrayList<>();
        mPostAdapter = new PostAdapter(this, mPosts, v -> {
            int pos = mPostList.getPositionForView(v);
            Post post = (Post) mPostAdapter.getItem(pos);
            Intent intent = new Intent(PostListActivity.this, PostViewActivity.class);
            intent.putExtra(BundleKeys.POST_UUID, post.getUuid());
            startActivity(intent);
        });
        mPostList.setAdapter(mPostAdapter);

        mHandler = new Handler(Looper.getMainLooper());

        getBus().post(new LoadUserEvent(false));
        getBus().post(new LoadBlogSettingsEvent(false));
        getBus().post(new LoadPostsEvent(false));

        mSwipeRefreshLayout.setColorSchemeResources(
                R.color.accent,
                R.color.primary_dark,
                R.color.accent_light,
                R.color.primary
        );
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
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.post_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                refreshData();
                return true;
            case R.id.action_logout:
                getBus().post(new LogoutEvent());
                finish();
                Intent intent = new Intent(this, LoginActivity.class);
                startActivity(intent);
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
        Picasso.with(this)
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
        mHandler.postDelayed(this::refreshData, REFRESH_FREQUENCY);
    }

    private void cancelDataRefresh() {
        mHandler.removeCallbacks(this::refreshData);
    }

    private void refreshData() {
        getBus().post(new RefreshDataEvent());
    }


    static class PostAdapter extends BaseAdapter {

        private final LayoutInflater mLayoutInflater;
        private final List<Post> mPosts;
        private final Context mContext;
        private final View.OnClickListener mItemClickListener;

        public PostAdapter(Context context, List<Post> posts, View.OnClickListener itemClickListener) {
            mContext = context;
            mLayoutInflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
            mPosts = posts;
            mItemClickListener = itemClickListener;
        }

        @Override
        public int getCount() {
            return mPosts.size();
        }

        @Override
        public Object getItem(int position) {
            return mPosts.get(position);
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public long getItemId(int position) {
            return ((Post) getItem(position)).getId();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            PostViewHolder holder;
            final Post item = (Post) getItem(position);

            if (convertView == null) {
                convertView = mLayoutInflater.inflate(R.layout.post_list_item, parent, false);
                holder = new PostViewHolder(convertView);
                convertView.setOnClickListener(mItemClickListener);
                convertView.setTag(holder);
            } else {
                holder = (PostViewHolder) convertView.getTag();
            }

            holder.title.setText(item.getTitle());
            String statusStr = "";
            int statusColor = R.color.published;
            switch (item.getStatus()) {
                case Post.PUBLISHED:
                    statusStr = String.format(
                            mContext.getString(R.string.published),
                            DateTimeUtils.dateToIsoDateString(item.getPublishedAt()));
                    statusColor = R.color.published;
                    break;
                case Post.DRAFT:
                    statusStr = mContext.getString(R.string.draft);
                    statusColor = R.color.draft;
                    break;
                case Post.LOCAL_NEW:
                    statusStr = mContext.getString(R.string.local_new);
                    statusColor = R.color.local_new;
                    break;
                default:
                    Log.wtf(TAG, "Invalid post status = " + item.getStatus() + "!");
            }
            holder.published.setText(statusStr);
            holder.published.setTextColor(mContext.getResources().getColor(statusColor));

            return convertView;
        }

        static class PostViewHolder {
            @InjectView(R.id.post_title)        TextView title;
            @InjectView(R.id.post_published)    TextView published;

            public PostViewHolder(View view) {
                ButterKnife.inject(this, view);
            }
        }

    }

}

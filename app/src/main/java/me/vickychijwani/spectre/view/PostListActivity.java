package me.vickychijwani.spectre.view;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import me.vickychijwani.spectre.Globals;
import me.vickychijwani.spectre.R;
import me.vickychijwani.spectre.model.Post;
import me.vickychijwani.spectre.model.PostList;
import me.vickychijwani.spectre.util.DateTimeUtils;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class PostListActivity extends BaseActivity {

    private static final String TAG = "PostListActivity";

    private List<Post> mPosts;
    private PostAdapter mPostAdapter;

    @InjectView(R.id.toolbar)
    Toolbar mToolbar;

    @InjectView(R.id.post_list)
    ListView mPostList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_list);
        ButterKnife.inject(this);
        setSupportActionBar(mToolbar);

        mPosts = new ArrayList<>();
        mPostAdapter = new PostAdapter(this, mPosts);
        mPostList.setAdapter(mPostAdapter);

        Globals.getInstance().api.getPosts("Bearer " + sAuthToken.access_token, new Callback<PostList>() {
            @Override
            public void success(PostList postList, Response response) {
                mPosts.clear();
                mPosts.addAll(postList.posts);
                mPostAdapter.notifyDataSetChanged();
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e(TAG, Log.getStackTraceString(error));
            }
        });
    }

    static class PostAdapter extends BaseAdapter {

        private final LayoutInflater mLayoutInflater;
        private final List<Post> mPosts;
        private final Context mContext;

        public PostAdapter(Context context, List<Post> posts) {
            mContext = context;
            mLayoutInflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
            mPosts = posts;
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
            return ((Post) getItem(position)).id;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            PostViewHolder holder;
            final Post item = (Post) getItem(position);

            if (convertView == null) {
                convertView = mLayoutInflater.inflate(R.layout.post_list_item, parent, false);
                holder = new PostViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (PostViewHolder) convertView.getTag();
            }

            holder.title.setText(item.title);
            String publishedStatus;
            if (item.published_at != null) {
                publishedStatus = String.format(
                        mContext.getString(R.string.published),
                        DateTimeUtils.dateToIsoDateString(item.published_at));
            } else {
                publishedStatus = mContext.getString(R.string.draft);
            }
            holder.published.setText(publishedStatus);

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

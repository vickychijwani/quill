package me.vickychijwani.spectre.view;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import me.vickychijwani.spectre.R;
import me.vickychijwani.spectre.model.Post;
import me.vickychijwani.spectre.model.Tag;
import me.vickychijwani.spectre.util.AppUtils;
import me.vickychijwani.spectre.util.PostUtils;

class PostAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_POST = 1;
    private static final int TYPE_FOOTER = 2;

    private final LayoutInflater mLayoutInflater;
    private final List<Post> mPosts;
    private final Context mContext;
    private final String mBlogUrl;
    private final Picasso mPicasso;
    private final View.OnClickListener mItemClickListener;
    private CharSequence mFooterText;

    public PostAdapter(Context context, List<Post> posts, String blogUrl, Picasso picasso,
                       View.OnClickListener itemClickListener) {
        mContext = context;
        mBlogUrl = blogUrl;
        mPicasso = picasso;
        mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mPosts = posts;
        mItemClickListener = itemClickListener;
        setHasStableIds(true);
    }

    @Override
    public int getItemCount() {
        int count = mPosts.size();
        if (mFooterText != null) {
            ++count; // +1 for footer
        }
        return count;
    }

    public Object getItem(int position) {
        if (position < mPosts.size()) {
            return mPosts.get(position);
        } else {
            return mFooterText;
        }
    }

    @Override
    public long getItemId(int position) {
        if (getItemViewType(position) == TYPE_POST) {
            return ((Post) getItem(position)).getUuid().hashCode();
        } else {
            return -9999;   // footer
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position < mPosts.size()) {
            return TYPE_POST;
        } else {
            return TYPE_FOOTER;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_POST) {
            View view = mLayoutInflater.inflate(R.layout.post_list_item, parent, false);
            return new PostViewHolder(view, mItemClickListener);
        } else if (viewType == TYPE_FOOTER) {
            View view = mLayoutInflater.inflate(R.layout.post_list_footer, parent, false);

            // make sure the footer spans the entire grid
            StaggeredGridLayoutManager.LayoutParams lp = (StaggeredGridLayoutManager.LayoutParams) view.getLayoutParams();
            if (lp == null) {
                lp = new StaggeredGridLayoutManager.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            }
            lp.setFullSpan(true);
            view.setLayoutParams(lp);

            return new FooterViewHolder(view);
        }
        throw new IllegalArgumentException("Invalid view type: " + viewType);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        if (viewHolder instanceof PostViewHolder) {
            PostViewHolder postVH = (PostViewHolder) viewHolder;
            Post post = (Post) getItem(position);
            bindPost(postVH, post);
        } else if (viewHolder instanceof FooterViewHolder) {
            FooterViewHolder footerVH = (FooterViewHolder) viewHolder;
            CharSequence footerText = (CharSequence) getItem(position);
            bindFooter(footerVH, footerText);
        } else {
            throw new IllegalArgumentException("Invalid ViewHolder type: " +
                    viewHolder.getClass().getSimpleName());
        }
    }

    private void bindPost(PostViewHolder viewHolder, Post post) {
        viewHolder.title.setText(post.getTitle());
        if (! TextUtils.isEmpty(post.getImage())) {
            String imageUrl = AppUtils.pathJoin(mBlogUrl, post.getImage());
            viewHolder.image.setVisibility(View.VISIBLE);
            mPicasso.load(imageUrl)
                    .fit().centerCrop()
                    .into(viewHolder.image);
        } else {
            viewHolder.image.setVisibility(View.GONE);
            viewHolder.image.setImageResource(android.R.color.transparent);
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

    private void bindFooter(FooterViewHolder viewHolder, CharSequence footerText) {
        viewHolder.textView.setText(footerText);
        viewHolder.textView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    public void showFooter(CharSequence footerText) {
        mFooterText = footerText;
    }

    public void hideFooter() {
        mFooterText = null;
    }


    static class PostViewHolder extends RecyclerView.ViewHolder {
        @Bind(R.id.post_title)          TextView title;
        @Bind(R.id.post_published)      TextView published;
        @Bind(R.id.post_image)          ImageView image;
        @Bind(R.id.post_tags)           TextView tags;

        public PostViewHolder(@NonNull View view, View.OnClickListener clickListener) {
            super(view);
            ButterKnife.bind(this, view);
            view.setOnClickListener(clickListener);
        }
    }


    static class FooterViewHolder extends RecyclerView.ViewHolder {
        @Bind(R.id.post_limit_exceeded) TextView textView;

        public FooterViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }

}

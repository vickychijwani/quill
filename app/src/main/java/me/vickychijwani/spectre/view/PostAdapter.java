package me.vickychijwani.spectre.view;

import android.content.Context;
import android.content.Intent;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorListener;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.vickychijwani.spectre.R;
import me.vickychijwani.spectre.model.entity.Post;
import me.vickychijwani.spectre.model.entity.Tag;
import me.vickychijwani.spectre.util.DeviceUtils;
import me.vickychijwani.spectre.util.NetworkUtils;
import me.vickychijwani.spectre.util.PostUtils;
import me.vickychijwani.spectre.util.Tip;

class PostAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_POST = 1;
    private static final int TYPE_FOOTER = 2;
    private static final int TYPE_TIP = 3;

    private final LayoutInflater mLayoutInflater;
    private final List<Post> mPosts;
    private final Context mContext;
    private final String mBlogUrl;
    private final Picasso mPicasso;
    private final View.OnClickListener mItemClickListener;
    private final Paint mLowAlphaPaint;
    private CharSequence mFooterText;
    private final Tip mGhostAndroidTip;

    // animation stuff
    private static final DecelerateInterpolator ANIM_INTERPOLATOR = new DecelerateInterpolator();
    private boolean mAnimateOnAttach = true;
    // for disabling mAnimateOnAttach after some time
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private int mAnimationDelay = 0; // for staggering

    public PostAdapter(Context context, List<Post> posts, String blogUrl, Picasso picasso,
                       View.OnClickListener itemClickListener) {
        mContext = context;
        mBlogUrl = blogUrl;
        mPicasso = picasso;
        mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mPosts = posts;
        mItemClickListener = itemClickListener;

        mLowAlphaPaint = new Paint();
        mLowAlphaPaint.setColorFilter(new ColorMatrixColorFilter(new float[] {
                1,     0,     0,     0,     0,    // red
                0,     1,     0,     0,     0,    // green
                0,     0,     1,     0,     0,    // blue
                0,     0,     0,     0.5f,  0,    // alpha
        }));

        mGhostAndroidTip = new Tip(R.drawable.quill_to_ghost_small,
                context.getString(R.string.quill_is_now_ghost_android),
                Arrays.asList(
                        new Tip.Action(R.drawable.play_store, context.getString(R.string.ghost_android_download), v -> {
                            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=org.ghost.android")));
                        }),
                        new Tip.Action(R.drawable.help_circle, context.getString(R.string.ghost_android_announcement), v -> {
                            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://blog.ghost.org/android")));
                        })));

        setHasStableIds(true);
    }

    @Override
    public int getItemCount() {
        int count = 0;
        ++count;    // +1 for tip
        count += mPosts.size();
        if (mFooterText != null) {
            ++count; // +1 for footer
        }
        return count;
    }

    public Object getItem(int position) {
        final int viewType = getItemViewType(position);
        switch (viewType) {
            case TYPE_POST:     return mPosts.get(position-1);
            case TYPE_TIP:      return mGhostAndroidTip;
            case TYPE_FOOTER:   return mFooterText;
            default:            throw new RuntimeException("Unhandled view type " + viewType);
        }
    }

    @Override
    public long getItemId(int position) {
        final int viewType = getItemViewType(position);
        switch (viewType) {
            case TYPE_POST:     return ((Post) getItem(position)).getId().hashCode();
            case TYPE_TIP:      return -9998;
            case TYPE_FOOTER:   return -9999;
            default:            throw new RuntimeException("Unhandled view type " + viewType);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0)                                      return TYPE_TIP;
        else if (position >= 1 && position <= mPosts.size())    return TYPE_POST;
        else if (position == mPosts.size()+1)                   return TYPE_FOOTER;
        else    throw new RuntimeException("Unknown view type at position " + position);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_POST) {
            View view = mLayoutInflater.inflate(R.layout.post_list_item, parent, false);
            return new PostViewHolder(view, mItemClickListener);
        } else if (viewType == TYPE_TIP) {
            View view = mLayoutInflater.inflate(R.layout.post_list_tip, parent, false);
            return new TipViewHolder(view);
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
        throw new IllegalArgumentException("Unhandled view type " + viewType);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        if (viewHolder instanceof PostViewHolder) {
            PostViewHolder postVH = (PostViewHolder) viewHolder;
            Post post = (Post) getItem(position);
            bindPost(postVH, post);
        } else if (viewHolder instanceof TipViewHolder) {
            TipViewHolder tipVH = (TipViewHolder) viewHolder;
            Tip tip = (Tip) getItem(position);
            bindTip(tipVH, tip);
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
        if (! TextUtils.isEmpty(post.getFeatureImage())) {
            String imageUrl = NetworkUtils.makeAbsoluteUrl(mBlogUrl, post.getFeatureImage());
            viewHolder.image.setVisibility(View.VISIBLE);
            mPicasso.load(imageUrl)
                    .fit().centerCrop()
                    .into(viewHolder.image);
        } else {
            viewHolder.image.setVisibility(View.GONE);
            viewHolder.image.setImageResource(android.R.color.transparent);
        }
        @ColorInt int postStatusColor = PostUtils.getStatusColor(post, mContext);
        viewHolder.statusIcon.setImageResource(PostUtils.getStatusIconResId(post));
        viewHolder.statusIcon.setColorFilter(postStatusColor, PorterDuff.Mode.SRC_IN);
        viewHolder.statusText.setText(PostUtils.getStatusString(post, mContext));
        viewHolder.statusText.setTextColor(postStatusColor);
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

        // grey out to-be-deleted posts, by making all the child Views of the item translucent
        ViewGroup viewGroup = (ViewGroup) viewHolder.itemView;
        boolean isMarkedForDeletion = post.isMarkedForDeletion();
        for (int i = 0, len = viewGroup.getChildCount(); i < len; ++i) {
            View childView = viewGroup.getChildAt(i);
            if (isMarkedForDeletion) {
                childView.setLayerType(View.LAYER_TYPE_HARDWARE, mLowAlphaPaint);
            } else {
                childView.setLayerType(View.LAYER_TYPE_NONE, null);
            }
        }
    }

    private void bindTip(TipViewHolder viewHolder, Tip tip) {
        if (tip.image > 0) {
            viewHolder.image.setImageResource(tip.image);
            viewHolder.image.setVisibility(View.VISIBLE);
        } else {
            viewHolder.image.setImageDrawable(null);
            viewHolder.image.setVisibility(View.GONE);
        }
        viewHolder.content.setText(tip.content);
        for (int i = 0; i < viewHolder.actions.getChildCount(); ++i) {
            TextView action = (TextView) viewHolder.actions.getChildAt(i);
            final Drawable icon = ContextCompat.getDrawable(mContext, tip.actions.get(i).icon);
            action.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
            action.setCompoundDrawablePadding(mContext.getResources().getDimensionPixelOffset(R.dimen.padding_default));
            action.setText(tip.actions.get(i).text);
            action.setOnClickListener(tip.actions.get(i).clickListener);
        }
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        // only show card animations when the adapter is initially created
        mHandler.postDelayed(() -> mAnimateOnAttach = false, 1000);
    }

    @Override
    public void onViewAttachedToWindow(RecyclerView.ViewHolder viewHolder) {
        if (! (viewHolder instanceof PostViewHolder)) {
            return;
        }
        // play a little cards animation similar to Google Now and Google+
        PostViewHolder postVH = (PostViewHolder) viewHolder;
        // check delay >= 0 to avoid Crashlytics issue #124, don't know how it becomes negative
        if (mAnimateOnAttach && mAnimationDelay >= 0) {
            View itemView = postVH.itemView;
            itemView.setTranslationY(DeviceUtils.dpToPx(300));
            itemView.setRotation(10);
            itemView.setAlpha(0f);
            ViewCompat.animate(itemView)
                    .withLayer()
                    .translationY(0f)
                    .rotation(0f)
                    .alpha(1f)
                    .setDuration(500)
                    .setInterpolator(ANIM_INTERPOLATOR)
                    .setStartDelay(mAnimationDelay)   // stagger the animation
                    .setListener(new ViewPropertyAnimatorListener() {
                        @Override
                        public void onAnimationStart(View view) {
                            mAnimationDelay += 100;
                        }

                        @Override
                        public void onAnimationEnd(View view) {
                            mAnimationDelay -= 100;
                        }

                        @Override
                        public void onAnimationCancel(View view) {
                            mAnimationDelay -= 100;
                            itemView.setTranslationY(0f);
                            itemView.setRotation(0f);
                            itemView.setAlpha(1f);
                        }
                    })
                    .start();
        }
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {
        if (holder instanceof PostViewHolder) {
            ((PostViewHolder) holder).cleanup();
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
        @BindView(R.id.post_title)          TextView title;
        @BindView(R.id.post_status_text)    TextView statusText;
        @BindView(R.id.post_status_icon)    ImageView statusIcon;
        @BindView(R.id.post_image)          ImageView image;
        @BindView(R.id.post_tags)           TextView tags;

        public PostViewHolder(@NonNull View view, View.OnClickListener clickListener) {
            super(view);
            ButterKnife.bind(this, view);
            view.setOnClickListener(clickListener);
        }

        // courtesy http://stackoverflow.com/a/33961706/504611
        public void cleanup() {
            Picasso.with(image.getContext())
                    .cancelRequest(image);
        }
    }


    static class TipViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.tip_image)   ImageView image;
        @BindView(R.id.tip_content) TextView content;
        @BindView(R.id.tip_actions) ViewGroup actions;

        TipViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }


    static class FooterViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.post_limit_exceeded) TextView textView;

        public FooterViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }

}

package me.vickychijwani.spectre.view;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.NavUtils;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.squareup.otto.Subscribe;

import org.parceler.Parcels;

import butterknife.ButterKnife;
import butterknife.InjectView;
import me.vickychijwani.spectre.R;
import me.vickychijwani.spectre.event.PostCreatedEvent;
import me.vickychijwani.spectre.model.Post;
import me.vickychijwani.spectre.view.fragments.PostEditFragment;
import me.vickychijwani.spectre.view.fragments.PostViewFragment;

public class PostViewActivity extends BaseActivity implements
        PostViewFragment.OnEditClickListener,
        PostEditFragment.OnPreviewClickListener {

    private static final String TAG = "PostViewActivity";

    private static final String TAG_VIEW_FRAGMENT = "tag:view_fragment";
    private static final String TAG_EDIT_FRAGMENT = "tag:edit_fragment";
    private static final String KEY_IS_PREVIEW_VISIBLE = "key:is_preview_visible";

    @InjectView(R.id.toolbar)
    Toolbar mToolbar;

    private Post mPost;
    private PostViewFragment mPostViewFragment;
    private PostEditFragment mPostEditFragment;
    private boolean mIsPreviewVisible = false;
    private View.OnClickListener mUpClickListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_edit);
        ButterKnife.inject(this);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mPost = Parcels.unwrap(getIntent().getExtras().getParcelable(BundleKeys.POST));

        mUpClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavUtils.navigateUpFromSameTask(PostViewActivity.this);
            }
        };

        mPostViewFragment = addFragment(PostViewFragment.class, R.id.fragment_container, TAG_VIEW_FRAGMENT);
        mPostEditFragment = addFragment(PostEditFragment.class, R.id.fragment_container, TAG_EDIT_FRAGMENT);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState.getBoolean(KEY_IS_PREVIEW_VISIBLE, false)) {
            onPreviewClicked();
        }
    }

    @Subscribe
    public void onPostCreatedEvent(PostCreatedEvent event) {
        mPost = event.newPost;
        mPostViewFragment.setPost(mPost);
        mPostEditFragment.setPost(mPost);
    }

    @Override
    public void onPreviewClicked() {
        mPostEditFragment.hide();
        mPostViewFragment.show();
        mIsPreviewVisible = true;
    }

    @Override
    public void onEditClicked() {
        mPostViewFragment.hide();
        mPostEditFragment.show();
        mIsPreviewVisible = false;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_IS_PREVIEW_VISIBLE, mIsPreviewVisible);
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

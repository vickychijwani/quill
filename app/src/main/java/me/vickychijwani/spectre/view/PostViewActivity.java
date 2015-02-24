package me.vickychijwani.spectre.view;

import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.widget.Toolbar;
import android.view.View;

import org.parceler.Parcels;

import butterknife.ButterKnife;
import butterknife.InjectView;
import me.vickychijwani.spectre.R;
import me.vickychijwani.spectre.model.Post;
import me.vickychijwani.spectre.view.fragments.PostEditFragment;
import me.vickychijwani.spectre.view.fragments.PostViewFragment;

public class PostViewActivity extends BaseActivity implements
        PostViewFragment.OnEditClickListener,
        PostEditFragment.OnPreviewClickListener {

    @InjectView(R.id.toolbar)
    Toolbar mToolbar;

    private Post mPost;
    private PostViewFragment mPostViewFragment;
    private PostEditFragment mPostEditFragment;
    private View.OnClickListener mUpClickListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_edit);
        ButterKnife.inject(this);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mPost = Parcels.unwrap(getIntent().getExtras().getParcelable(BundleKeys.POST));

        mPostViewFragment = PostViewFragment.newInstance(mPost);
        mPostEditFragment = PostEditFragment.newInstance(mPost);

        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.fragment_container, mPostViewFragment)
                .add(R.id.fragment_container, mPostEditFragment)
                .commit();

        // begin in edit mode
        onEditClicked();

        mUpClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavUtils.navigateUpFromSameTask(PostViewActivity.this);
            }
        };
    }

    @Override
    public void onPreviewClicked() {
        mPostViewFragment.onShow();
        getSupportFragmentManager()
                .beginTransaction()
                .show(mPostViewFragment)
                .hide(mPostEditFragment)
                .commit();
        mPostEditFragment.onHide();
    }

    @Override
    public void onEditClicked() {
        mPostEditFragment.onShow();
        getSupportFragmentManager()
                .beginTransaction()
                .show(mPostEditFragment)
                .hide(mPostViewFragment)
                .commit();
        mPostViewFragment.onHide();
    }

    public void setNavigationItem(int iconResId, View.OnClickListener clickListener) {
        mToolbar.setNavigationIcon(iconResId);
        mToolbar.setNavigationOnClickListener(clickListener);
    }

    public void resetNavigationItem() {
        mToolbar.setNavigationIcon(R.drawable.arrow_left);
        mToolbar.setNavigationOnClickListener(mUpClickListener);
    }

}

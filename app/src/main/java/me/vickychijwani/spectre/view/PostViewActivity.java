package me.vickychijwani.spectre.view;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;

import org.parceler.Parcels;

import butterknife.ButterKnife;
import butterknife.InjectView;
import me.vickychijwani.spectre.R;
import me.vickychijwani.spectre.model.Post;
import me.vickychijwani.spectre.view.fragments.PostEditFragment;
import me.vickychijwani.spectre.view.fragments.PostViewFragment;

public class PostViewActivity extends BaseActivity
        implements PostViewFragment.OnEditClickListener, PostEditFragment.OnPreviewClickListener {

    @InjectView(R.id.toolbar)
    Toolbar mToolbar;

    private Post mPost;
    private PostViewFragment mPostViewFragment;
    private PostEditFragment mPostEditFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_view);
        ButterKnife.inject(this);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mPost = Parcels.unwrap(getIntent().getExtras().getParcelable(BundleKeys.POST));
        getSupportActionBar().setTitle(mPost.title);

        mPostViewFragment = PostViewFragment.newInstance(mPost);
        mPostEditFragment = PostEditFragment.newInstance(mPost);

        // begin in preview mode, initially
        onPreviewClicked();
    }

    @Override
    public void onPreviewClicked() {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, mPostViewFragment)
                .commit();
    }

    @Override
    public void onEditClicked() {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, mPostEditFragment)
                .commit();
    }

}

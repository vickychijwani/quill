package me.vickychijwani.spectre.view;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;

import org.parceler.Parcels;

import butterknife.ButterKnife;
import butterknife.InjectView;
import me.vickychijwani.spectre.R;
import me.vickychijwani.spectre.model.Post;
import me.vickychijwani.spectre.view.fragments.PostViewFragment;

public class PostViewActivity extends BaseActivity {

    @InjectView(R.id.toolbar)
    Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_view);
        ButterKnife.inject(this);
        setSupportActionBar(mToolbar);

        Post post = Parcels.unwrap(getIntent().getExtras().getParcelable(BundleKeys.POST));
        getSupportActionBar().setTitle(post.title);

        PostViewFragment postViewFragment = PostViewFragment.newInstance(post);
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.fragment_container, postViewFragment)
                .commit();
    }

}

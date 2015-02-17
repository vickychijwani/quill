package me.vickychijwani.spectre.view.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import org.parceler.Parcels;

import butterknife.ButterKnife;
import butterknife.InjectView;
import me.vickychijwani.spectre.R;
import me.vickychijwani.spectre.model.Post;
import me.vickychijwani.spectre.pref.UserPrefs;
import me.vickychijwani.spectre.view.BundleKeys;
import me.vickychijwani.spectre.view.PostEditActivity;

public class PostViewFragment extends Fragment {

    @InjectView(R.id.post_html)
    WebView mPostHtmlView;

    @InjectView(R.id.edit_post_btn)
    View mEditBtn;

    public static PostViewFragment newInstance(@NonNull Post post) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(BundleKeys.POST, Parcels.wrap(post));
        PostViewFragment fragment = new PostViewFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_post_view, container, false);
        ButterKnife.inject(this, view);

        // show post html
        UserPrefs prefs = UserPrefs.getInstance(getActivity());
        String blogUrl = prefs.getString(UserPrefs.Key.BLOG_URL);
        final Post post = Parcels.unwrap(getArguments().getParcelable(BundleKeys.POST));
        String postUrl = post.getAbsoluteUrl(blogUrl);
        mPostHtmlView.loadDataWithBaseURL(blogUrl, post.html, "text/html", "UTF-8", postUrl);

        // set up edit button
        mEditBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), PostEditActivity.class);
                intent.putExtra(BundleKeys.POST, Parcels.wrap(post));
                startActivity(intent);
            }
        });

        return view;
    }

}

package me.vickychijwani.spectre.view.fragments;

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

public class PostViewFragment extends Fragment {

    public static final String ARG_POST = "arg:post";

    @InjectView(R.id.post_html)
    WebView mPostHtmlView;

    public static PostViewFragment newInstance(@NonNull Post post) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(ARG_POST, Parcels.wrap(post));
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

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        UserPrefs prefs = UserPrefs.getInstance(getActivity());
        String blogUrl = prefs.getString(UserPrefs.Key.BLOG_URL);
        Post post = Parcels.unwrap(getArguments().getParcelable(ARG_POST));
        String postUrl = post.getAbsoluteUrl(blogUrl);

        mPostHtmlView.loadDataWithBaseURL(blogUrl, post.html, "text/html", "UTF-8", postUrl);
    }

}

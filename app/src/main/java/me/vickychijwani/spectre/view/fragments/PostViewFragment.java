package me.vickychijwani.spectre.view.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import butterknife.ButterKnife;
import butterknife.InjectView;
import me.vickychijwani.spectre.R;
import me.vickychijwani.spectre.model.Post;
import me.vickychijwani.spectre.pref.UserPrefs;
import me.vickychijwani.spectre.view.PostViewActivity;

public class PostViewFragment extends BaseFragment {

    @InjectView(R.id.post_html)
    WebView mPostHtmlView;

    @InjectView(R.id.edit_post_btn)
    View mEditBtn;

    private OnEditClickListener mEditClickListener;
    private Post mPost;
    private String mBlogUrl;
    private int mHtmlHashCode;

    public interface OnEditClickListener {
        public void onEditClicked();
    }


    public static PostViewFragment newInstance() {
        return new PostViewFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_post_view, container, false);
        ButterKnife.inject(this, view);

        mPost = ((PostViewActivity) getActivity()).getPost();

        UserPrefs prefs = UserPrefs.getInstance(getActivity());
        mBlogUrl = prefs.getString(UserPrefs.Key.BLOG_URL);

        // set up edit button
        mEditBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditClickListener.onEditClicked();
            }
        });

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        hide();
    }

    @Override
    public void onShow() {
        if (getActivity() != null) {
            getActivity().setTitle(mPost.title);
        }
        int htmlHashCode = mPost.html.hashCode();
        if (htmlHashCode != mHtmlHashCode) {
            String postUrl = mPost.getAbsoluteUrl(mBlogUrl);
            mPostHtmlView.loadDataWithBaseURL(mBlogUrl, mPost.html, "text/html", "UTF-8", postUrl);
            mHtmlHashCode = htmlHashCode;
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mEditClickListener = (OnEditClickListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + "must implement OnEditClickListener");
        }
    }

}

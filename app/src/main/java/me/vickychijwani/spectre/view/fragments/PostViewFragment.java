package me.vickychijwani.spectre.view.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;

import butterknife.ButterKnife;
import butterknife.InjectView;
import me.vickychijwani.spectre.R;
import me.vickychijwani.spectre.model.Post;
import me.vickychijwani.spectre.pref.UserPrefs;
import me.vickychijwani.spectre.util.AppUtils;
import me.vickychijwani.spectre.view.PostViewActivity;

public class PostViewFragment extends BaseFragment {

    @InjectView(R.id.edit_post_btn)
    View mEditBtn;

    private OnEditClickListener mEditClickListener;
    private Post mPost;
    private int mMarkdownHashCode;
    private WebViewFragment mWebViewFragment;

    public interface OnEditClickListener {
        void onEditClicked();
    }


    @SuppressWarnings("unused")
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
        String blogUrl = prefs.getString(UserPrefs.Key.BLOG_URL);

        mWebViewFragment = WebViewFragment.newInstance("file:///android_asset/post-preview.html");
        mWebViewFragment.setJSInterface(new Object() {
            @JavascriptInterface
            public String getMarkdown() {
                // FIXME dirty string-replacement hack!
                return mPost.getMarkdown().replaceAll("/content/images",
                        AppUtils.pathJoin(blogUrl, "/content/images"));
            }
        }, "POST");
        getChildFragmentManager()
                .beginTransaction()
                .add(R.id.web_view_container, mWebViewFragment)
                .commit();

        // set up edit button
        mEditBtn.setOnClickListener(v -> mEditClickListener.onEditClicked());

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        hide();
    }

    @Override
    public void onShow() {
        if (getActivity() != null) {
            getActivity().setTitle(mPost.getTitle());
        }
        int markdownHashCode = mPost.getMarkdown().hashCode();
        if (markdownHashCode != mMarkdownHashCode) {
            mWebViewFragment.evaluateJavascript("preview()");
            mMarkdownHashCode = markdownHashCode;
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

    public void setPost(@NonNull Post post) {
        mPost = post;
        onShow();
    }

}

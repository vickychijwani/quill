package me.vickychijwani.spectre.view.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import butterknife.ButterKnife;
import butterknife.InjectView;
import in.uncod.android.bypass.Bypass;
import me.vickychijwani.spectre.R;
import me.vickychijwani.spectre.model.Post;
import me.vickychijwani.spectre.network.PicassoImageGetter;
import me.vickychijwani.spectre.pref.UserPrefs;
import me.vickychijwani.spectre.view.PostViewActivity;

public class PostViewFragment extends BaseFragment {

    @InjectView(R.id.post_html)
    TextView mPostHtmlView;

    @InjectView(R.id.edit_post_btn)
    View mEditBtn;

    private OnEditClickListener mEditClickListener;
    private Post mPost;
    private int mMarkdownHashCode;
    private Bypass mBypass;
    private PicassoImageGetter mImageGetter;

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

        // set up edit button
        mEditBtn.setOnClickListener(v -> mEditClickListener.onEditClicked());

        mBypass = new Bypass(getActivity());
        mImageGetter = new PicassoImageGetter(blogUrl, mPostHtmlView, getResources(),
                Picasso.with(getActivity()));

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
            mPostHtmlView.setText(mBypass.markdownToSpannable(mPost.getMarkdown(), mImageGetter));
            mPostHtmlView.setMovementMethod(LinkMovementMethod.getInstance());
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

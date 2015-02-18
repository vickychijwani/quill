package me.vickychijwani.spectre.view.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.commonsware.cwac.anddown.AndDown;

import org.parceler.Parcels;

import butterknife.ButterKnife;
import butterknife.InjectView;
import me.vickychijwani.spectre.R;
import me.vickychijwani.spectre.model.Post;
import me.vickychijwani.spectre.view.BundleKeys;

public class PostEditFragment extends Fragment {

    @InjectView(R.id.post_markdown)
    EditText mPostEditView;

    @InjectView(R.id.preview_btn)
    View mPreviewBtn;

    private OnPreviewClickListener mCallback;
    private Post mPost;
    private AndDown mAndDown;   // Markdown parser

    public interface OnPreviewClickListener {
        public void onPreviewClicked();
    }

    public static PostEditFragment newInstance(@NonNull Post post) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(BundleKeys.POST, Parcels.wrap(post));
        PostEditFragment fragment = new PostEditFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAndDown = new AndDown();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_post_edit, container, false);
        ButterKnife.inject(this, view);

        mPost = Parcels.unwrap(getArguments().getParcelable(BundleKeys.POST));

        // set up preview button
        mPreviewBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallback.onPreviewClicked();
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        mPostEditView.setText(mPost.markdown);
    }

    @Override
    public void onPause() {
        super.onPause();
        mPost.markdown = mPostEditView.getText().toString();
        mPost.html = mAndDown.markdownToHtml(mPost.markdown);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mCallback = (OnPreviewClickListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                + "must implement OnPreviewClickListener");
        }
    }

}

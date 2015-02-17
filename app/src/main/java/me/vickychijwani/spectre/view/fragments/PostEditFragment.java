package me.vickychijwani.spectre.view.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import org.parceler.Parcels;

import butterknife.ButterKnife;
import butterknife.InjectView;
import me.vickychijwani.spectre.R;
import me.vickychijwani.spectre.model.Post;
import me.vickychijwani.spectre.view.BundleKeys;

public class PostEditFragment extends Fragment {

    @InjectView(R.id.post_markdown)
    EditText mPostEditView;

    public static PostEditFragment newInstance(@NonNull Post post) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(BundleKeys.POST, Parcels.wrap(post));
        PostEditFragment fragment = new PostEditFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_post_edit, container, false);
        ButterKnife.inject(this, view);

        Post post = Parcels.unwrap(getArguments().getParcelable(BundleKeys.POST));
        mPostEditView.setText(post.markdown);

        return view;
    }

}

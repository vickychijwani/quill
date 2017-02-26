package me.vickychijwani.spectre.view.fragments;

import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class GenericFragment extends BaseFragment {

    private static final String KEY_LAYOUT_ID = "key:layout_id";

    public static GenericFragment newInstance(@LayoutRes int layoutId) {
        GenericFragment fragment = new GenericFragment();
        Bundle args = new Bundle();
        args.putInt(KEY_LAYOUT_ID, layoutId);
        fragment.setArguments(args);
        return fragment;
    }

    public GenericFragment() {}

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        @LayoutRes int layoutId = getArguments().getInt(KEY_LAYOUT_ID);
        return inflater.inflate(layoutId, container, false);
    }

}

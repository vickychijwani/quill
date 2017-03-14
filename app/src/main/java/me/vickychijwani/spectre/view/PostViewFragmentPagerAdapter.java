package me.vickychijwani.spectre.view;

import android.content.Context;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.ViewGroup;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import me.vickychijwani.spectre.R;
import me.vickychijwani.spectre.model.entity.Post;
import me.vickychijwani.spectre.view.fragments.BaseFragment;
import me.vickychijwani.spectre.view.fragments.PostEditFragment;
import me.vickychijwani.spectre.view.fragments.PostViewFragment;

class PostViewFragmentPagerAdapter extends FragmentPagerAdapter {

    public interface OnFragmentsInitializedListener {
        void onPostViewFragmentInitialized(PostViewFragment postViewFragment);
        void onPostEditFragmentInitialized(PostEditFragment postEditFragment);
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ TAB_POSITION_PREVIEW, TAB_POSITION_EDIT })
    public @interface TabPosition {}

    public static final int         TAB_POSITION_PREVIEW = 0;
    public static final int         TAB_POSITION_EDIT = 1;

    private static final int        TAB_COUNT = 2;
    private static final String[]   TAB_TITLES = new String[TAB_COUNT];

    private Post mPost;
    private final OnFragmentsInitializedListener mFragmentsInitializedListener;

    public PostViewFragmentPagerAdapter(Context context, FragmentManager fm,
                                        Post post, OnFragmentsInitializedListener listener) {
        super(fm);
        TAB_TITLES[TAB_POSITION_PREVIEW] = context.getString(R.string.preview);
        TAB_TITLES[TAB_POSITION_EDIT] = context.getString(R.string.edit);
        mPost = post;
        mFragmentsInitializedListener = listener;
    }

    public void setPost(@NonNull Post post) {
        mPost = post;
    }

    // called when a Fragment is to be attached -- this may either call getItem() to create a new
    // fragment instance, or reuse the existing one using FragmentManager#findFragmentByTag()
    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Object object = super.instantiateItem(container, position);
        if (object instanceof PostViewFragment) {
            mFragmentsInitializedListener.onPostViewFragmentInitialized((PostViewFragment) object);
        } else if (object instanceof PostEditFragment) {
            mFragmentsInitializedListener.onPostEditFragmentInitialized((PostEditFragment) object);
        }
        return object;
    }

    public Class<? extends BaseFragment> getFragmentType(@TabPosition int position) {
        switch (position) {
            case TAB_POSITION_PREVIEW:
                return PostViewFragment.class;
            case TAB_POSITION_EDIT:
                return PostEditFragment.class;
            default:
                throw new IllegalArgumentException("No fragment exists at position " + position);
        }
    }

    // FragmentPagerAdapter#instantiateItem() calls getItem() to create a new instance
    @Override
    public Fragment getItem(@TabPosition int position) {
        switch (position) {
            case TAB_POSITION_PREVIEW:
                return PostViewFragment.newInstance(mPost);
            case TAB_POSITION_EDIT:
                return PostEditFragment.newInstance(mPost);
            default:
                throw new IllegalArgumentException("No fragment exists at position " + position);
        }
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return TAB_TITLES[position];
    }

    @Override
    public int getCount() {
        return TAB_COUNT;
    }

}

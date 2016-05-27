package me.vickychijwani.spectre.view;

import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.ViewGroup;

import me.vickychijwani.spectre.model.entity.Post;
import me.vickychijwani.spectre.view.fragments.BaseFragment;
import me.vickychijwani.spectre.view.fragments.PostEditFragment;
import me.vickychijwani.spectre.view.fragments.PostViewFragment;

class PostViewFragmentPagerAdapter extends FragmentPagerAdapter {

    public interface OnFragmentsInitializedListener {
        void onPostViewFragmentInitialized(PostViewFragment postViewFragment);
        void onPostEditFragmentInitialized(PostEditFragment postEditFragment);
    }

    private static final String[] TAB_TITLES = new String[] { "Preview", "Edit" };
    private static final int TAB_COUNT = TAB_TITLES.length;

    private Post mPost;
    private boolean mFileStorageEnabled;
    private final OnFragmentsInitializedListener mFragmentsInitializedListener;

    public PostViewFragmentPagerAdapter(FragmentManager fm, Post post, boolean fileStorageEnabled,
                                        OnFragmentsInitializedListener listener) {
        super(fm);
        mPost = post;
        mFileStorageEnabled = fileStorageEnabled;
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

    public Class<? extends BaseFragment> getFragmentType(int position) {
        switch (position) {
            case 0:
                return PostViewFragment.class;
            case 1:
                return PostEditFragment.class;
            default:
                throw new IllegalArgumentException("No fragment exists at position " + position);
        }
    }

    // FragmentPagerAdapter#instantiateItem() calls getItem() to create a new instance
    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return PostViewFragment.newInstance(mPost);
            case 1:
                return PostEditFragment.newInstance(mPost, mFileStorageEnabled);
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

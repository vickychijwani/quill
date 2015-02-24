package me.vickychijwani.spectre.view.fragments;

import android.support.v4.app.Fragment;

public class BaseFragment extends Fragment {

    /**
     * Called by the hosting {@link me.vickychijwani.spectre.view.BaseActivity} when the fragment is
     * being hidden from view.
     */
    public void onHide() { }

    /**
     * Called by the hosting {@link me.vickychijwani.spectre.view.BaseActivity} when the fragment is
     * being brought into view.
     */
    public void onShow() { }

    /**
     * Called by the hosting {@link me.vickychijwani.spectre.view.BaseActivity} to give the Fragment
     * a chance to handle the back press event. The Fragment must return true in order to prevent
     * the default action: {@link android.app.Activity#finish}.
     *
     * @return true if this Fragment has handled the event, false otherwise
     */
    public boolean onBackPressed() {
        return false;
    }

}

package me.vickychijwani.spectre.view.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.crashlytics.android.Crashlytics;
import com.squareup.otto.Bus;
import com.squareup.picasso.Picasso;

import butterknife.ButterKnife;
import me.vickychijwani.spectre.SpectreApplication;
import me.vickychijwani.spectre.event.BusProvider;

public abstract class BaseFragment extends Fragment {

    private static final String TAG = "BaseFragment";

    protected Bus getBus() {
        return BusProvider.getBus();
    }

    protected Picasso getPicasso() {
        return SpectreApplication.getInstance().getPicasso();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        Crashlytics.log(Log.DEBUG, TAG, this.getClass().getName() + "#onCreateView()");
        return null;
    }

    @Override
    public void onStart() {
        super.onStart();
        Crashlytics.log(Log.DEBUG, TAG, this.getClass().getName() + "#onStart()");
    }

    @Override
    public void onResume() {
        super.onResume();
        Crashlytics.log(Log.DEBUG, TAG, this.getClass().getName() + "#onResume()");
        getBus().register(this);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Crashlytics.log(Log.DEBUG, TAG, this.getClass().getName() + "#onAttach()");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Crashlytics.log(Log.DEBUG, TAG, this.getClass().getName() + "#onDetach()");
    }

    @Override
    public void onPause() {
        super.onPause();
        Crashlytics.log(Log.DEBUG, TAG, this.getClass().getName() + "#onPause()");
        getBus().unregister(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        Crashlytics.log(Log.DEBUG, TAG, this.getClass().getName() + "#onStop()");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Crashlytics.log(Log.DEBUG, TAG, this.getClass().getName() + "#onDestroyView()");
        ButterKnife.reset(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Crashlytics.log(Log.DEBUG, TAG, this.getClass().getName() + "#onDestroy()");
    }

    /**
     * Hide the root {@link android.view.View} of this Fragment.
     */
    public void hide() {
        if (getView() != null) {
            getView().setVisibility(View.GONE);
            onHide();
        }
    }

    /**
     * Show the root {@link android.view.View} of this Fragment.
     */
    public void show() {
        if (getView() != null) {
            onShow();
            getView().setVisibility(View.VISIBLE);
        }
    }

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

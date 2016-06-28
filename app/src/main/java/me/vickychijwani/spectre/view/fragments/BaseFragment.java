package me.vickychijwani.spectre.view.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.crashlytics.android.Crashlytics;
import com.squareup.otto.Bus;
import com.squareup.picasso.Picasso;
import com.trello.rxlifecycle.components.support.RxFragment;

import butterknife.ButterKnife;
import me.vickychijwani.spectre.SpectreApplication;
import me.vickychijwani.spectre.event.BusProvider;

public abstract class BaseFragment extends RxFragment {

    private static final String TAG = "BaseFragment";

    protected Bus getBus() {
        return BusProvider.getBus();
    }

    @SuppressWarnings("unused")
    protected Picasso getPicasso() {
        return SpectreApplication.getInstance().getPicasso();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        Crashlytics.log(Log.DEBUG, TAG, this.getClass().getSimpleName() + "#onCreateView()");
        return null;
    }

    @Override
    public void onStart() {
        super.onStart();
        Crashlytics.log(Log.DEBUG, TAG, this.getClass().getSimpleName() + "#onStart()");
    }

    @Override
    public void onResume() {
        super.onResume();
        Crashlytics.log(Log.DEBUG, TAG, this.getClass().getSimpleName() + "#onResume()");
        getBus().register(this);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Crashlytics.log(Log.DEBUG, TAG, this.getClass().getSimpleName() + "#onAttach()");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Crashlytics.log(Log.DEBUG, TAG, this.getClass().getSimpleName() + "#onDetach()");
    }

    @Override
    public void onPause() {
        super.onPause();
        Crashlytics.log(Log.DEBUG, TAG, this.getClass().getSimpleName() + "#onPause()");
        getBus().unregister(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        Crashlytics.log(Log.DEBUG, TAG, this.getClass().getSimpleName() + "#onStop()");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Crashlytics.log(Log.DEBUG, TAG, this.getClass().getSimpleName() + "#onDestroyView()");
        ButterKnife.unbind(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Crashlytics.log(Log.DEBUG, TAG, this.getClass().getSimpleName() + "#onDestroy()");
    }

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

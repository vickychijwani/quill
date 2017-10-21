package me.vickychijwani.spectre.view.fragments;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
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
import butterknife.Unbinder;
import me.vickychijwani.spectre.SpectreApplication;
import me.vickychijwani.spectre.event.BusProvider;

@SuppressWarnings("WeakerAccess")
public abstract class BaseFragment extends Fragment {

    private static final String TAG = "BaseFragment";

    private final String mClassName;
    private Unbinder mUnbinder = null;

    protected Bus getBus() {
        return BusProvider.getBus();
    }

    public BaseFragment() {
        super();
        mClassName = this.getClass().getSimpleName();
    }

    @SuppressWarnings("unused")
    protected Picasso getPicasso() {
        return SpectreApplication.getInstance().getPicasso();
    }

    protected void bindView(@NonNull View view) {
        // Unbinding is needed for derived classes 2 or more levels down the inheritance chain,
        // because each class in the chain may call bindView() independently. Moreover, unbinding
        // and re-binding does not pose a problem because ButterKnife also binds base class fields.
        unbindView();
        mUnbinder = ButterKnife.bind(this, view);
    }

    private void unbindView() {
        if (mUnbinder != null) {
            mUnbinder.unbind();
            mUnbinder = null;
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        Crashlytics.log(Log.DEBUG, TAG, mClassName + "#onCreateView()");
        return null;
    }

    @Override
    public void onStart() {
        super.onStart();
        Crashlytics.log(Log.DEBUG, TAG, mClassName + "#onStart()");
        getBus().register(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mUnbinder == null) {
            throw new IllegalStateException("You forgot to call bindView() in " + mClassName +
                    "#onCreateView(). This is required in order to unbind Fragment views. See " +
                    "http://jakewharton.github.io/butterknife/#reset");
        }
        Crashlytics.log(Log.DEBUG, TAG, mClassName + "#onResume()");
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Crashlytics.log(Log.DEBUG, TAG, mClassName + "#onAttach()");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Crashlytics.log(Log.DEBUG, TAG, mClassName + "#onDetach()");
    }

    @Override
    public void onPause() {
        super.onPause();
        Crashlytics.log(Log.DEBUG, TAG, mClassName + "#onPause()");
    }

    @Override
    public void onStop() {
        super.onStop();
        Crashlytics.log(Log.DEBUG, TAG, mClassName + "#onStop()");
        getBus().unregister(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Crashlytics.log(Log.DEBUG, TAG, mClassName + "#onDestroyView()");
        unbindView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Crashlytics.log(Log.DEBUG, TAG, mClassName + "#onDestroy()");
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

    protected void openUrl(String url) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }

}

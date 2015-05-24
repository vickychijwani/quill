package me.vickychijwani.spectre.view;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;

import com.crashlytics.android.Crashlytics;
import com.squareup.otto.Bus;
import com.squareup.picasso.Picasso;

import java.util.List;

import me.vickychijwani.spectre.SpectreApplication;
import me.vickychijwani.spectre.event.BusProvider;
import me.vickychijwani.spectre.view.fragments.BaseFragment;

public abstract class BaseActivity extends AppCompatActivity {

    public static final String TAG = "BaseActivity";

    public Bus getBus() {
        return BusProvider.getBus();
    }

    public Picasso getPicasso() {
        return SpectreApplication.getInstance().getPicasso();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Crashlytics.log(Log.DEBUG, TAG, this.getClass().getName() + "#onCreate()");
        getBus().register(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Crashlytics.log(Log.DEBUG, TAG, this.getClass().getName() + "#onStart()");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Crashlytics.log(Log.DEBUG, TAG, this.getClass().getName() + "#onResume()");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Crashlytics.log(Log.DEBUG, TAG, this.getClass().getName() + "#onPause()");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Crashlytics.log(Log.DEBUG, TAG, this.getClass().getName() + "#onStop()");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Crashlytics.log(Log.DEBUG, TAG, this.getClass().getName() + "#onDestroy()");
        getBus().unregister(this);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Crashlytics.log(Log.DEBUG, TAG, this.getClass().getName() + "#onLowMemory()");
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        Crashlytics.log(Log.DEBUG, TAG, this.getClass().getName() + "#onTrimMemory()");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        Crashlytics.log(Log.DEBUG, TAG, this.getClass().getName() + "#onBackPressed()");
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        if (fragments != null) {
            for (Fragment f : fragments) {
                if (!(f instanceof BaseFragment)) {
                    continue;  // vanilla fragments don't have onBackPressed
                }

                BaseFragment bf = (BaseFragment) f;
                if (bf.onBackPressed()) {
                    return;
                }
            }
        }
        super.onBackPressed();
    }

    protected void startBrowserActivity(String url) {
        Intent browserIntent = new Intent(this, BrowserActivity.class);
        browserIntent.putExtra(BundleKeys.URL, url);
        startActivity(browserIntent);
    }

    /**
     * Add a {@link android.support.v4.app.Fragment} of the given type {@link T}. NOTE: this method
     * is only designed to work with Fragments that have a zero-argument static factory method named
     * newInstance().
     *
     * @param type      the class of the Fragment to add
     * @param container the ID of the view container in which to add this Fragment's view root
     * @param tag       the tag to assign to this Fragment, used for retrieval later
     * @param <T>       the type of the Fragment
     * @return a non-null instance of type {@link T} (could be a new instance or one already
     *                  attached to the activity)
     */
    @SuppressWarnings("unchecked")
    @NonNull
    public <T extends Fragment> T addFragment(Class<T> type, @IdRes int container, String tag) {
        T fragment = (T) getSupportFragmentManager().findFragmentByTag(tag);
        if (fragment == null) {
            //noinspection TryWithIdenticalCatches
            try {
                fragment = type.newInstance();
            } catch (InstantiationException e) {
                throw new IllegalArgumentException("Given Fragment class does not have a zero-" +
                        "argument static factory method named newInstance(), as required by this" +
                        "method");
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException("Given Fragment class does not have a zero-" +
                        "argument static factory method named newInstance(), as required by this" +
                        "method");
            }
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(container, fragment, tag)
                    .commit();
        }
        return fragment;
    }

}

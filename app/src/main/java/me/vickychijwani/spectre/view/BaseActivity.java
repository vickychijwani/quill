package me.vickychijwani.spectre.view;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.customtabs.CustomTabsIntent;
import android.support.v4.app.Fragment;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import com.squareup.picasso.Picasso;
import com.trello.rxlifecycle.components.support.RxAppCompatActivity;
import com.tsengvn.typekit.TypekitContextWrapper;

import java.util.List;

import butterknife.ButterKnife;
import me.vickychijwani.spectre.R;
import me.vickychijwani.spectre.SpectreApplication;
import me.vickychijwani.spectre.event.BusProvider;
import me.vickychijwani.spectre.event.PasswordChangedEvent;
import me.vickychijwani.spectre.view.fragments.BaseFragment;

public abstract class BaseActivity extends RxAppCompatActivity {

    private static final String TAG = "BaseActivity";
    private PasswordChangedEventHandler mPasswordChangedEventHandler = null;

    protected Bus getBus() {
        return BusProvider.getBus();
    }

    protected Picasso getPicasso() {
        return SpectreApplication.getInstance().getPicasso();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Crashlytics.log(Log.DEBUG, TAG, this.getClass().getSimpleName() + "#onCreate()");
        getBus().register(this);
    }

    protected void setLayout(int layoutResID) {
        super.setContentView(layoutResID);
        ButterKnife.bind(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Crashlytics.log(Log.DEBUG, TAG, this.getClass().getSimpleName() + "#onStart()");
        if (! (this instanceof LoginActivity)) {
            mPasswordChangedEventHandler = new PasswordChangedEventHandler(this);
            getBus().register(mPasswordChangedEventHandler);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Crashlytics.log(Log.DEBUG, TAG, this.getClass().getSimpleName() + "#onResume()");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Crashlytics.log(Log.DEBUG, TAG, this.getClass().getSimpleName() + "#onPause()");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Crashlytics.log(Log.DEBUG, TAG, this.getClass().getSimpleName() + "#onStop()");
        if (mPasswordChangedEventHandler != null) {
            getBus().unregister(mPasswordChangedEventHandler);
            mPasswordChangedEventHandler = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Crashlytics.log(Log.DEBUG, TAG, this.getClass().getSimpleName() + "#onDestroy()");
        getBus().unregister(this);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Crashlytics.log(Log.DEBUG, TAG, this.getClass().getSimpleName() + "#onLowMemory()");
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        Crashlytics.log(Log.DEBUG, TAG, this.getClass().getSimpleName() + "#onTrimMemory()");
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
        Crashlytics.log(Log.DEBUG, TAG, this.getClass().getSimpleName() + "#onBackPressed()");
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

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(TypekitContextWrapper.wrap(newBase));
    }

    protected void startBrowserActivity(String url) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
            builder.setToolbarColor(ContextCompat.getColor(this, R.color.primary));
            builder.addDefaultShareMenuItem();
            CustomTabsIntent customTabsIntent = builder.build();
            customTabsIntent.launchUrl(this, Uri.parse(url));
        } else {
            Intent browserIntent = new Intent(this, BrowserActivity.class);
            browserIntent.putExtra(BundleKeys.URL, url);
            startActivity(browserIntent);
        }
    }

    protected void openUrl(String url) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }


    // the event handler cannot be added to BaseActivity directly because Otto doesn't look at base
    // classes when looking for subscribers, hence this little helper class
    private static class PasswordChangedEventHandler {
        private final Activity mActivity;

        public PasswordChangedEventHandler(Activity activity) {
            mActivity = activity;
        }

        @Subscribe
        public void onPasswordChangedEvent(PasswordChangedEvent event) {
            Intent intent = new Intent(mActivity, LoginActivity.class);
            // destroy all activities in this task stack
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            mActivity.startActivity(intent);
            Toast.makeText(mActivity, mActivity.getString(R.string.password_changed), Toast.LENGTH_LONG).show();
            mActivity.finish();
        }
    }

}

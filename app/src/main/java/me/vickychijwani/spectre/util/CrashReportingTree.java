package me.vickychijwani.spectre.util;

import android.util.Log;

import com.crashlytics.android.Crashlytics;

import timber.log.Timber;

public class CrashReportingTree extends Timber.Tree {

    @Override
    protected void log(int priority, String tag, String message, Throwable t) {
        // log only INFO, WARN, ERROR and ASSERT levels
        if (priority == Log.VERBOSE || priority == Log.DEBUG) {
            return;
        }
        if (message != null) {
            Crashlytics.log(priority, tag, message);
        }
        if (t != null) {
            Crashlytics.logException(t);
        }
    }

}

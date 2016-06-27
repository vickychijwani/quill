package me.vickychijwani.spectre.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;

import java.util.Locale;

import me.vickychijwani.spectre.R;
import me.vickychijwani.spectre.view.BaseActivity;

public class AppUtils {

    private static final String TAG = AppUtils.class.getSimpleName();

    public static void emailDeveloper(@NonNull BaseActivity activity) {
        String emailSubject = String.format(activity.getString(R.string.email_subject),
                activity.getString(R.string.app_name));
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:")); // only email apps should handle this
        intent.putExtra(Intent.EXTRA_EMAIL, new String[] { "vickychijwani@gmail.com" });
        intent.putExtra(Intent.EXTRA_SUBJECT, emailSubject);
        if (intent.resolveActivity(activity.getPackageManager()) != null) {
            activity.startActivity(intent);
        } else {
            Toast.makeText(activity, R.string.intent_no_apps, Toast.LENGTH_LONG)
                    .show();
        }
    }

    /**
     * Return this app's PackageInfo containing info about version code, version name, etc.
     */
    @Nullable
    public static PackageInfo getPackageInfo(@NonNull Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            Crashlytics.logException(new RuntimeException("Failed to get package info, " +
                    "see previous exception for details", e));
            return null;
        }
    }

    /**
     * Set the app to use the given locale. Useful for testing translations. This is normally
     * not needed because the device locale is applied automatically.
     * @param context - context from which to get resources
     * @param locale - the locale to use
     */
    public static void setLocale(@NonNull Context context, @NonNull Locale locale) {
        Locale.setDefault(locale);
        Resources res = context.getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        android.content.res.Configuration conf = res.getConfiguration();
        conf.locale = Locale.getDefault();
        res.updateConfiguration(conf, dm);
    }

}

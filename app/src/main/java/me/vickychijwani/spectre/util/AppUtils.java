package me.vickychijwani.spectre.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
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
        String emailSubject = activity.getString(R.string.email_subject,
                activity.getString(R.string.app_name));
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:")); // only email apps should handle this
        intent.putExtra(Intent.EXTRA_EMAIL, new String[] { "vickychijwani@gmail.com" });
        intent.putExtra(Intent.EXTRA_SUBJECT, emailSubject);

        String body = "App version: " + getAppVersion(activity) + "\n";
        body += "Android API version: " + Build.VERSION.SDK_INT + "\n";
        body += "Ghost version: <include if relevant>" + "\n";
        body += "\n";
        intent.putExtra(Intent.EXTRA_TEXT, body);

        if (intent.resolveActivity(activity.getPackageManager()) != null) {
            activity.startActivity(intent);
        } else {
            Toast.makeText(activity, R.string.intent_no_apps, Toast.LENGTH_LONG)
                    .show();
        }
    }

    @NonNull
    public static String getAppVersion(@NonNull Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            if (packageInfo != null) {
                return packageInfo.versionName;
            } else {
                return context.getString(R.string.version_unknown);
            }
        } catch (PackageManager.NameNotFoundException e) {
            Crashlytics.logException(new RuntimeException("Failed to get package info, " +
                    "see previous exception for details", e));
            return context.getString(R.string.version_unknown);
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

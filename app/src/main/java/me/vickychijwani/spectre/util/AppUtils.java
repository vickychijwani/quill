package me.vickychijwani.spectre.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;

import java.util.Locale;

import me.vickychijwani.spectre.R;
import me.vickychijwani.spectre.util.functions.Action1;
import me.vickychijwani.spectre.view.BaseActivity;

public class AppUtils {

    private static final String TAG = AppUtils.class.getSimpleName();

    public static void emailFeedbackToDeveloper(@NonNull BaseActivity activity) {
        String subject = activity.getString(R.string.feedback_email_subject,
                activity.getString(R.string.app_name));
        String body = "App version: " + getAppVersion(activity) + "\n" +
                "Android API version: " + Build.VERSION.SDK_INT + "\n" +
                "Ghost version: <include if relevant>" + "\n" +
                "\n";
        emailDeveloper(activity, subject, body);
    }

    public static void emailLoginIssueToDeveloper(@NonNull BaseActivity activity) {
        String subject = activity.getString(R.string.login_help_email_subject,
                activity.getString(R.string.app_name));
        String body = "App version: " + getAppVersion(activity) + "\n" +
                "Android API version: " + Build.VERSION.SDK_INT + "\n" +
                "Ghost version: <include if relevant>" + "\n" +
                "\n" +
                "<Describe your issue here>\n";
        emailDeveloper(activity, subject, body);
    }

    private static void emailDeveloper(@NonNull BaseActivity activity, @NonNull String subject,
                                       @NonNull String body) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:")); // only email apps should handle this
        intent.putExtra(Intent.EXTRA_EMAIL, new String[] { "vickychijwani@gmail.com" });
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
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

    public static void showSystemAppSettingsActivity(@NonNull final Activity context) {
        final Intent i = new Intent();
        i.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        i.addCategory(Intent.CATEGORY_DEFAULT);
        i.setData(Uri.parse("package:" + context.getPackageName()));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        context.startActivity(i);
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

    // Add a custom event handler for link clicks in TextView HTML
    // credits: https://stackoverflow.com/a/19989677/504611
    public static void setHtmlWithLinkClickHandler(TextView tv, String html,
                                            Action1<String> linkClickHandler) {
        CharSequence sequence = Html.fromHtml(html);
        SpannableStringBuilder strBuilder = new SpannableStringBuilder(sequence);
        URLSpan[] urls = strBuilder.getSpans(0, sequence.length(), URLSpan.class);
        for (URLSpan span : urls) {
            int start = strBuilder.getSpanStart(span);
            int end = strBuilder.getSpanEnd(span);
            int flags = strBuilder.getSpanFlags(span);
            ClickableSpan clickable = new ClickableSpan() {
                public void onClick(View view) {
                    linkClickHandler.call(span.getURL());
                }
            };
            strBuilder.setSpan(clickable, start, end, flags);
            strBuilder.removeSpan(span);
        }
        tv.setText(strBuilder);
        tv.setMovementMethod(LinkMovementMethod.getInstance());
    }

}

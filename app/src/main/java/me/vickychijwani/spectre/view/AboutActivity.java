package me.vickychijwani.spectre.view;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.Bind;
import butterknife.OnClick;
import me.vickychijwani.spectre.R;
import me.vickychijwani.spectre.util.AppUtils;

public class AboutActivity extends BaseActivity {

    public static final String URL_GITHUB_CONTRIBUTING = "https://github.com/vickychijwani/quill/blob/master/CONTRIBUTING.md#reporting-bugs";
    public static final String URL_MY_WEBSITE = "http://vickychijwani.me";
    public static final String URL_TWITTER_PROFILE = "https://twitter.com/vickychijwani";
    public static final String URL_GITHUB_REPO = "https://github.com/vickychijwani/quill";
    public static final String URL_GITHUB_PROFILE = "https://github.com/vickychijwani";

    @Bind(R.id.toolbar) Toolbar mToolbar;
    @Bind(R.id.about_version) TextView mVersionView;
    @Bind(R.id.about_icon_credits) TextView mIconCreditsView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setLayout(R.layout.activity_about);

        setSupportActionBar(mToolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        PackageInfo packageInfo = AppUtils.getPackageInfo(this);
        String version = getString(R.string.version_unknown);
        if (packageInfo != null) {
            version = packageInfo.versionName;
        }
        mVersionView.setText(String.format(getString(R.string.version_placeholder), version));

        mIconCreditsView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    @OnClick(R.id.about_open_source_libs)
    public void onOpenSourceLibsClicked(View v) {
        Intent intent = new Intent(this, OpenSourceLibsActivity.class);
        startActivity(intent);
    }

    @OnClick(R.id.about_me)
    public void onAboutMeClicked(View v) {
        openUrl(URL_GITHUB_PROFILE);
    }

    @OnClick(R.id.about_github)
    public void onGithubClicked(View v) {
        openUrl(URL_GITHUB_REPO);
    }

    @OnClick(R.id.about_twitter)
    public void onTwitterClicked(View v) {
        openUrl(URL_TWITTER_PROFILE);
    }

    @OnClick(R.id.about_website)
    public void onWebsiteClicked(View v) {
        openUrl(URL_MY_WEBSITE);
    }

    @OnClick(R.id.about_report_bugs)
    public void onReportBugsClicked(View v) {
        openUrl(URL_GITHUB_CONTRIBUTING);
    }

    @OnClick(R.id.about_play_store)
    public void onRateOnPlayStoreClicked(View v) {
        final String appPackageName = getPackageName();
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
        } catch (android.content.ActivityNotFoundException anfe) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
        }
    }

    @OnClick(R.id.about_email_developer)
    public void onEmailDeveloperClicked(View v) {
        String emailSubject = String.format(getString(R.string.email_subject),
                getString(R.string.app_name));
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:")); // only email apps should handle this
        intent.putExtra(Intent.EXTRA_EMAIL, new String[] { "vickychijwani@gmail.com" });
        intent.putExtra(Intent.EXTRA_SUBJECT, emailSubject);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Toast.makeText(this, R.string.intent_no_apps, Toast.LENGTH_LONG)
                    .show();
        }
    }

}

package me.vickychijwani.spectre.view;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.OnClick;
import me.vickychijwani.spectre.R;
import me.vickychijwani.spectre.util.AppUtils;

public class AboutActivity extends BaseActivity {

    public static final String URL_GITHUB_CONTRIBUTING = "https://github.com/vickychijwani/quill/blob/master/CONTRIBUTING.md#reporting-bugs";
    public static final String URL_TRANSLATE = "https://hosted.weblate.org/engage/ghost/en/";
    public static final String URL_MY_WEBSITE = "http://vickychijwani.me";
    public static final String URL_TWITTER_PROFILE = "https://twitter.com/vickychijwani";
    public static final String URL_GITHUB_REPO = "https://github.com/vickychijwani/quill";
    public static final String URL_GITHUB_PROFILE = "https://github.com/vickychijwani";

    @BindView(R.id.toolbar) Toolbar mToolbar;
    @BindView(R.id.about_version) TextView mVersionView;
    @BindView(R.id.about_icon_credits) TextView mIconCreditsView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setLayout(R.layout.activity_about);

        setSupportActionBar(mToolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        String version = AppUtils.getAppVersion(this);
        mVersionView.setText(version);

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

    @OnClick(R.id.about_translate)
    public void onTranslateClicked(View v) {
        openUrl(URL_TRANSLATE);
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
        AppUtils.emailFeedbackToDeveloper(this);
    }

}

package me.vickychijwani.spectre.view;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;

import butterknife.Bind;
import butterknife.OnClick;
import me.vickychijwani.spectre.R;
import me.vickychijwani.spectre.util.AppUtils;

public class AboutActivity extends BaseActivity {

    @Bind(R.id.toolbar) Toolbar mToolbar;
    @Bind(R.id.about_version) TextView mVersionView;
    @Bind(R.id.about_author) TextView mAuthorView;
    @Bind(R.id.about_desc) TextView mDescView;
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
        String version = "unknown";
        if (packageInfo != null) {
            version = packageInfo.versionName;
        }
        mVersionView.setText(String.format(getString(R.string.version), version));

        mAuthorView.setMovementMethod(LinkMovementMethod.getInstance());
        mDescView.setMovementMethod(LinkMovementMethod.getInstance());
        mIconCreditsView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    @OnClick(R.id.about_open_source_libs_btn)
    public void onOpenSourceLibsBtnClicked(View v) {
        Intent intent = new Intent(this, OpenSourceLibsActivity.class);
        startActivity(intent);
    }

}

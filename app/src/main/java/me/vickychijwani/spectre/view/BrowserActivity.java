package me.vickychijwani.spectre.view;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;

import butterknife.Bind;
import me.vickychijwani.spectre.R;
import me.vickychijwani.spectre.view.fragments.WebViewFragment;

public class BrowserActivity extends BaseActivity implements WebViewFragment.OnWebViewCreatedListener {

    @Bind(R.id.toolbar) Toolbar mToolbar;
    private WebViewFragment mWebViewFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setLayout(R.layout.activity_browser);
        setSupportActionBar(mToolbar);
        setTitle(R.string.loading);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        String url = getIntent().getStringExtra(BundleKeys.URL);
        mWebViewFragment = WebViewFragment.newInstance(url);
        mWebViewFragment.setOnWebViewCreatedListener(this);
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.web_view_container, mWebViewFragment)
                .commit();
    }

    @Override
    public void onWebViewCreated() {
        mWebViewFragment.setWebViewClient(new WebViewFragment.DefaultWebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                BrowserActivity.this.setTitle(view.getTitle());
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.browser, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();   // navigate up to source (post list, post editor, etc)
                return true;
            case R.id.action_share:
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    //noinspection deprecation
                    shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                } else {
                    shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
                }
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, mWebViewFragment.getCurrentUrl());
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, mWebViewFragment.getCurrentTitle());
                startActivity(Intent.createChooser(shareIntent, getString(R.string.share)));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}

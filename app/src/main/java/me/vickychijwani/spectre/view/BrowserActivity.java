package me.vickychijwani.spectre.view;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;

import java.util.HashMap;
import java.util.Map;

import butterknife.Bind;
import me.vickychijwani.spectre.R;
import me.vickychijwani.spectre.view.fragments.WebViewFragment;

// FIXME this is only used on Ice Cream Sandwich
public class BrowserActivity extends BaseActivity implements WebViewFragment.OnWebViewCreatedListener {

    // generic infrastructure for opening links in a WebView - this may be useful later
    private static final Map<String, String> URL_MAP = new HashMap<>();
    private static final Map<String, Boolean> UP_ACTION_MAP = new HashMap<>();

//    private static final boolean SHOW_UP_ACTION = true, HIDE_UP_ACTION = false;

//    public static final String POST_FETCH_LIMIT_FEEDBACK = "me.vickychijwani.spectre://feedback/post-fetch-limit";

//    static {
//        URL_MAP.put(POST_FETCH_LIMIT_FEEDBACK, "https://github.com/vickychijwani/quill/issues/81");
//        UP_ACTION_MAP.put(POST_FETCH_LIMIT_FEEDBACK, SHOW_UP_ACTION);
//    }

    @Bind(R.id.toolbar) Toolbar mToolbar;
    private WebViewFragment mWebViewFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setLayout(R.layout.activity_browser);
        setSupportActionBar(mToolbar);
        setTitle(R.string.loading);

        String url;
        if (getIntent().getData() != null) {
            url = getIntent().getDataString();
        } else if (getIntent().hasExtra(BundleKeys.URL)) {
            url = getIntent().getStringExtra(BundleKeys.URL);
        } else {
            throw new IllegalArgumentException("No URL given");
        }
        if (URL_MAP.containsKey(url)) {
            String key = url;
            url = URL_MAP.get(key);
            // maybe hide the Toolbar "Up" action to avoid having too many "Back"-like actions
            //noinspection ConstantConditions
            getSupportActionBar().setDisplayHomeAsUpEnabled(UP_ACTION_MAP.get(key));
        }

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

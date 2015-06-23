package me.vickychijwani.spectre.view;

import android.os.Bundle;
import android.view.MenuItem;

import butterknife.ButterKnife;
import me.vickychijwani.spectre.R;
import me.vickychijwani.spectre.view.fragments.WebViewFragment;

public class BrowserActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browser);

        String url = getIntent().getStringExtra(BundleKeys.URL);
        WebViewFragment webViewFragment = WebViewFragment.newInstance(url);
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.web_view_container, webViewFragment)
                .commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();   // navigate up to source (post list, post editor, etc)
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}

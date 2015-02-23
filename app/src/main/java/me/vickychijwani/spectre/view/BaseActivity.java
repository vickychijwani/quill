package me.vickychijwani.spectre.view;

import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;

import me.vickychijwani.spectre.model.AuthToken;

public abstract class BaseActivity extends ActionBarActivity {

    // TODO get rid of this shit
    protected static AuthToken sAuthToken = null;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}

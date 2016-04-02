package me.vickychijwani.spectre.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.squareup.otto.Subscribe;

import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import butterknife.Bind;
import me.vickychijwani.spectre.R;
import me.vickychijwani.spectre.event.LoginDoneEvent;
import me.vickychijwani.spectre.event.LoginErrorEvent;
import me.vickychijwani.spectre.event.LoginStartEvent;
import me.vickychijwani.spectre.model.ApiError;
import me.vickychijwani.spectre.model.ApiErrorList;
import me.vickychijwani.spectre.pref.UserPrefs;
import me.vickychijwani.spectre.util.KeyboardUtils;
import me.vickychijwani.spectre.util.NetworkUtils;
import retrofit.RetrofitError;


/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends BaseActivity implements OnClickListener, TextView.OnEditorActionListener {

    private final String TAG = "LoginActivity";

    // UI references
    @Bind(R.id.toolbar)         Toolbar mToolbar;
    @Bind(R.id.blog_url_layout) TextInputLayout mBlogUrlLayout;
    @Bind(R.id.blog_url)        EditText mBlogUrlView;
    @Bind(R.id.email_layout)    TextInputLayout mEmailLayout;
    @Bind(R.id.email)           EditText mEmailView;
    @Bind(R.id.password_layout) TextInputLayout mPasswordLayout;
    @Bind(R.id.password)        EditText mPasswordView;
    @Bind(R.id.login_progress)  View mProgressView;
    @Bind(R.id.login_form)      View mLoginFormView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setLayout(R.layout.activity_login);
        setSupportActionBar(mToolbar);

        mPasswordView.setOnEditorActionListener(this);

        UserPrefs prefs = UserPrefs.getInstance(this);
        mBlogUrlView.setText(prefs.getString(UserPrefs.Key.BLOG_URL));
        mEmailView.setText(prefs.getString(UserPrefs.Key.USERNAME));
        mPasswordView.setText(prefs.getString(UserPrefs.Key.PASSWORD));

        findViewById(R.id.email_sign_in_button).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        attemptLogin();
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == getResources().getInteger(R.integer.ime_action_id_signin)
                || actionId == EditorInfo.IME_NULL) {
            attemptLogin();
            return true;
        }
        return false;
    }

    /**
     * Attempts to sign in the account specified by the login form. If there are form errors
     * (invalid email, missing fields, etc.), the errors are presented and no actual login attempt
     * is made.
     */
    @SuppressWarnings("OverlyLongMethod")
    public void attemptLogin() {
        if (! NetworkUtils.isConnected(this)) {
            Toast.makeText(this, R.string.no_internet_connection, Toast.LENGTH_SHORT).show();
            return;
        }

        // Reset errors.
        mEmailLayout.setError(null);
        mPasswordLayout.setError(null);

        // Store values at the time of the login attempt.
        String blogUrl = mBlogUrlView.getText().toString().trim();
        String email = mEmailView.getText().toString().trim();
        String password = mPasswordView.getText().toString().trim();

        boolean cancel = false;
        View focusView = null;

        // check for a valid url
        if (TextUtils.isEmpty(blogUrl)) {
            mBlogUrlLayout.setError(getString(R.string.error_field_required));
            focusView = mBlogUrlView;
            cancel = true;
        }

        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(password)) {
            mPasswordLayout.setError(getString(R.string.error_field_required));
            focusView = mPasswordView;
            cancel = true;
        } else if (! isPasswordValid(password)) {
            mPasswordLayout.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailLayout.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (! isEmailValid(email)) {
            mEmailLayout.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            KeyboardUtils.defocusAndHideKeyboard(this);
            sendLoginRequest(blogUrl, email, password);
        }
    }

    private void sendLoginRequest(String blogUrl, String username, String password) {
        getBus().post(new LoginStartEvent(blogUrl, username, password, true));
    }

    @Subscribe
    public void onLoginDoneEvent(LoginDoneEvent event) {
        UserPrefs prefs = UserPrefs.getInstance(this);
        prefs.setString(UserPrefs.Key.BLOG_URL, event.blogUrl);
        prefs.setString(UserPrefs.Key.USERNAME, event.username);
        prefs.setString(UserPrefs.Key.PASSWORD, event.password);
        finish();

        Intent intent = new Intent(this, PostListActivity.class);
        startActivity(intent);
    }

    @Subscribe
    public void onLoginErrorEvent(LoginErrorEvent event) {
        RetrofitError error = event.error;
        Crashlytics.logException(error);        // report login failures to Crashlytics
        Log.e(TAG, Log.getStackTraceString(error));
        showProgress(false);
        try {
            ApiErrorList errorList = (ApiErrorList) error.getBodyAs(ApiErrorList.class);
            ApiError apiError = errorList.errors.get(0);
            EditText errorView = mPasswordView;
            TextInputLayout errorLayout = mPasswordLayout;
            if ("NotFoundError".equals(apiError.errorType)) {
                errorView = mEmailView;
                errorLayout = mEmailLayout;
            }
            errorView.requestFocus();
            errorLayout.setError(Html.fromHtml(apiError.message));
        } catch (Exception ignored) {
            // errors in url: invalid / unknown hostname or ip
            boolean userNetworkError = error.getKind() == RetrofitError.Kind.NETWORK
                    && (error.getCause() instanceof UnknownHostException
                    || error.getCause() instanceof MalformedURLException);
            // connection error: timeout, etc
            boolean connectionError = error.getKind() == RetrofitError.Kind.NETWORK
                    && (error.getCause() instanceof ConnectException
                    || error.getCause() instanceof SocketTimeoutException);
            // don't remember when this happens, but it does happen consistently in one error scenario
            boolean conversionError = error.getKind() == RetrofitError.Kind.CONVERSION;
            String blogUrl = mBlogUrlView.getText().toString();
            if (userNetworkError || conversionError) {
                mBlogUrlLayout.setError(String.format(getString(R.string.no_such_blog), blogUrl));
                mBlogUrlView.requestFocus();
            } else if (connectionError) {
                Toast.makeText(this, String.format(getString(R.string.login_connection_error), blogUrl),
                        Toast.LENGTH_SHORT).show();
            } else {
                Crashlytics.log(Log.ERROR, TAG, "generic error message triggered during login!");
                Crashlytics.logException(error);
                Toast.makeText(this, getString(R.string.login_unexpected_error),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean isEmailValid(String email) {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean isPasswordValid(String password) {
        return password.length() >= 8;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    private void showProgress(final boolean show) {
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });

        mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        mProgressView.animate().setDuration(shortAnimTime).alpha(
                show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }

}

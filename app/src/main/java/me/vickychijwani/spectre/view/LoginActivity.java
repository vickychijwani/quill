package me.vickychijwani.spectre.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.os.Bundle;
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

import com.squareup.otto.Subscribe;

import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.UnknownHostException;

import butterknife.ButterKnife;
import butterknife.InjectView;
import me.vickychijwani.spectre.R;
import me.vickychijwani.spectre.event.LoginDoneEvent;
import me.vickychijwani.spectre.event.LoginErrorEvent;
import me.vickychijwani.spectre.event.LoginStartEvent;
import me.vickychijwani.spectre.model.ApiError;
import me.vickychijwani.spectre.model.ApiErrorList;
import me.vickychijwani.spectre.pref.UserPrefs;
import retrofit.RetrofitError;


/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends BaseActivity implements OnClickListener, TextView.OnEditorActionListener {

    private final String TAG = "LoginActivity";

    // UI references
    @InjectView(R.id.toolbar)
    Toolbar mToolbar;

    @InjectView(R.id.blog_url)
    EditText mBlogUrlView;

    @InjectView(R.id.email)
    EditText mEmailView;

    @InjectView(R.id.password)
    EditText mPasswordView;

    @InjectView(R.id.login_progress)
    View mProgressView;

    @InjectView(R.id.login_form)
    View mLoginFormView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.inject(this);
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
    public void attemptLogin() {
        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String blogUrl = mBlogUrlView.getText().toString();
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // check for a valid url
        if (TextUtils.isEmpty(blogUrl)) {
            mBlogUrlView.setError(getString(R.string.error_field_required));
            focusView = mBlogUrlView;
            cancel = true;
        }

        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(password)) {
            mPasswordView.setError(getString(R.string.error_field_required));
            focusView = mPasswordView;
            cancel = true;
        } else if (! isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (! isEmailValid(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
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
            sendLoginRequest();
        }
    }

    public void sendLoginRequest() {
        String blogUrl = mBlogUrlView.getText().toString();
        String username = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();
        getBus().post(new LoginStartEvent(blogUrl, username, password));
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
        Log.e(TAG, Log.getStackTraceString(error));
        showProgress(false);
        try {
            ApiErrorList errorList = (ApiErrorList) error.getBodyAs(ApiErrorList.class);
            ApiError apiError = errorList.errors.get(0);
            EditText errorView = mPasswordView;
            if ("NotFoundError".equals(apiError.errorType)) {
                errorView = mEmailView;
            }
            errorView.setError(Html.fromHtml(apiError.message));
            errorView.requestFocus();
        } catch (Exception ignored) {
            boolean userNetworkError = error.getKind() == RetrofitError.Kind.NETWORK
                    && (error.getCause() instanceof UnknownHostException
                    || error.getCause() instanceof MalformedURLException);
            boolean connectionError = error.getKind() == RetrofitError.Kind.NETWORK
                    && error.getCause() instanceof ConnectException;
            boolean conversionError = error.getKind() == RetrofitError.Kind.CONVERSION;
            String blogUrl = mBlogUrlView.getText().toString();
            if (userNetworkError || conversionError) {
                mBlogUrlView.setError(String.format(getString(R.string.no_such_blog), blogUrl));
                mBlogUrlView.requestFocus();
            } else if (connectionError) {
                Toast.makeText(this, String.format(getString(R.string.login_connection_error), blogUrl),
                        Toast.LENGTH_SHORT).show();
            } else {
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
    public void showProgress(final boolean show) {
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

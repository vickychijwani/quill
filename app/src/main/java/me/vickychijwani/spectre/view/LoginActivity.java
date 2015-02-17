package me.vickychijwani.spectre.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import butterknife.ButterKnife;
import butterknife.InjectView;
import me.vickychijwani.spectre.Globals;
import me.vickychijwani.spectre.R;
import me.vickychijwani.spectre.model.AuthReqBody;
import me.vickychijwani.spectre.model.AuthToken;
import me.vickychijwani.spectre.network.GhostApiService;
import me.vickychijwani.spectre.pref.UserPrefs;
import me.vickychijwani.spectre.util.AppUtils;
import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.converter.GsonConverter;


/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends BaseActivity {

    /**
     * A dummy authentication store containing known user names and passwords. TODO: remove after
     * connecting to a real authentication system.
     */
    private static final String[] DUMMY_CREDENTIALS = new String[]{
            "foo@example.com:hello", "bar@example.com:world"
    };

    private final String TAG = "LoginActivity";

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;

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

        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        UserPrefs prefs = UserPrefs.getInstance(this);
        mBlogUrlView.setText(prefs.getString(UserPrefs.Key.BLOG_URL));
        mEmailView.setText(prefs.getString(UserPrefs.Key.USERNAME));
        mPasswordView.setText(prefs.getString(UserPrefs.Key.PASSWORD));

        Button mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                final String blogUrl = mBlogUrlView.getText().toString();

                Gson gson = new GsonBuilder()
                        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                        .create();

                RestAdapter restAdapter = new RestAdapter.Builder()
                        .setEndpoint(AppUtils.pathJoin(blogUrl, "ghost/api/v0.1"))
                        .setConverter(new GsonConverter(gson))
                        .setLogLevel(RestAdapter.LogLevel.HEADERS)
                        .build();

                Globals.getInstance().api = restAdapter.create(GhostApiService.class);

                final AuthReqBody credentials = new AuthReqBody();
                credentials.username = mEmailView.getText().toString();
                credentials.password = mPasswordView.getText().toString();

                Globals.getInstance().api.getAuthToken(credentials, new Callback<AuthToken>() {
                    @Override
                    public void success(AuthToken authToken, Response response) {
                        sAuthToken = authToken;
                        Log.e(TAG, "Got new access token = " + sAuthToken.access_token);

                        UserPrefs prefs = UserPrefs.getInstance(LoginActivity.this);
                        prefs.setString(UserPrefs.Key.BLOG_URL, blogUrl);
                        prefs.setString(UserPrefs.Key.USERNAME, credentials.username);
                        prefs.setString(UserPrefs.Key.PASSWORD, credentials.password);

                        Intent intent = new Intent(LoginActivity.this, PostListActivity.class);
                        startActivity(intent);
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        Log.e(TAG, Log.getStackTraceString(error));
                    }
                });
            }
        });
    }

    /**
     * Attempts to sign in or register the account specified by the login form. If there are form
     * errors (invalid email, missing fields, etc.), the errors are presented and no actual login
     * attempt is made.
     */
    public void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;


        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!isEmailValid(email)) {
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
            mAuthTask = new UserLoginTask(email, password);
            mAuthTask.execute((Void) null);
        }
    }

    private boolean isEmailValid(String email) {
        //TODO: Replace this with your own logic
        return email.contains("@");
    }

    private boolean isPasswordValid(String password) {
        //TODO: Replace this with your own logic
        return password.length() > 4;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
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
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String mEmail;
        private final String mPassword;

        UserLoginTask(String email, String password) {
            mEmail = email;
            mPassword = password;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // TODO: attempt authentication against a network service.

            try {
                // Simulate network access.
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                return false;
            }

            for (String credential : DUMMY_CREDENTIALS) {
                String[] pieces = credential.split(":");
                if (pieces[0].equals(mEmail)) {
                    // Account exists, return true if the password matches.
                    return pieces[1].equals(mPassword);
                }
            }

            // TODO: register the new account here.
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            showProgress(false);

            if (success) {
                finish();
            } else {
                mPasswordView.setError(getString(R.string.error_incorrect_password));
                mPasswordView.requestFocus();
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }
}

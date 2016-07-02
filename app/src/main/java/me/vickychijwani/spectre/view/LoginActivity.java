package me.vickychijwani.spectre.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.squareup.otto.Subscribe;

import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLHandshakeException;

import butterknife.Bind;
import me.vickychijwani.spectre.R;
import me.vickychijwani.spectre.error.LoginFailedException;
import me.vickychijwani.spectre.event.LoginDoneEvent;
import me.vickychijwani.spectre.event.LoginErrorEvent;
import me.vickychijwani.spectre.event.LoginStartEvent;
import me.vickychijwani.spectre.network.entity.ApiError;
import me.vickychijwani.spectre.network.entity.ApiErrorList;
import me.vickychijwani.spectre.pref.UserPrefs;
import me.vickychijwani.spectre.util.KeyboardUtils;
import me.vickychijwani.spectre.util.NetworkUtils;
import retrofit.RetrofitError;
import rx.Subscription;
import rx.functions.Action1;


/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends BaseActivity implements
        OnClickListener,
        TextView.OnEditorActionListener,
        View.OnFocusChangeListener {

    private final String TAG = "LoginActivity";

    // UI references
    @Bind(R.id.blog_url_layout)         TextInputLayout mBlogUrlLayout;
    @Bind(R.id.blog_url)                EditText mBlogUrlView;
    @Bind(R.id.blog_url_progress_bar)   ProgressBar mBlogUrlProgressView;
    @Bind(R.id.blog_url_valid)          ImageView mBlogUrlValidView;
    @Bind(R.id.email_layout)            TextInputLayout mEmailLayout;
    @Bind(R.id.email)                   EditText mEmailView;
    @Bind(R.id.password_layout)         TextInputLayout mPasswordLayout;
    @Bind(R.id.password)                EditText mPasswordView;
    @Bind(R.id.sign_in_btn)             Button mSignInBtn;
    @Bind(R.id.login_progress)          View mProgressView;
    @Bind(R.id.login_form)              View mLoginFormView;

    // we proactively check the URL and store it if it is valid
    private String mValidGhostBlogUrl = null;
    // all listeners that monitor the state of the "check blog url" request
    private List<CheckBlogUrlListener> mCheckBlogUrlListeners = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setLayout(R.layout.activity_login);

        mPasswordView.setOnEditorActionListener(this);

        UserPrefs prefs = UserPrefs.getInstance(this);
        String blogUrl = prefs.getString(UserPrefs.Key.BLOG_URL);
        mBlogUrlView.setText(blogUrl);
        mEmailView.setText(prefs.getString(UserPrefs.Key.USERNAME));
        mPasswordView.setText(prefs.getString(UserPrefs.Key.PASSWORD));

        mSignInBtn.setOnClickListener(this);
        mBlogUrlView.setOnFocusChangeListener(this);

        if (!TextUtils.isEmpty(blogUrl)) {
            checkBlogUrl(new TextChangeCheckBlogUrlListener());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopCheckingBlogUrl();
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (v == mBlogUrlView) {
            if (!hasFocus) {
                stopCheckingBlogUrl();
                checkBlogUrl(new TextChangeCheckBlogUrlListener());
            } else {
                stopCheckingBlogUrl();
            }
        }
    }

    @Override
    public void onClick(View v) {
        attemptLogin();
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == getResources().getInteger(R.integer.ime_action_id_signin)
                || actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_NULL) {
            attemptLogin();
            // don't consume the event, so the keyboard can also be hidden
            // http://stackoverflow.com/questions/2342620/how-to-hide-keyboard-after-typing-in-edittext-in-android#comment20849208_10184099
            return false;
        }
        return false;
    }

    // check if the url points to a valid Ghost blog, and store it if yes
    private void checkBlogUrl(@NonNull CheckBlogUrlListener listener) {
        if (! NetworkUtils.isConnected(this)) {
            Toast.makeText(this, R.string.no_internet_connection, Toast.LENGTH_SHORT).show();
            return;
        }

        String blogUrl = mBlogUrlView.getText().toString().trim().replaceFirst("^(.*)/ghost$", "$1");
        if (TextUtils.isEmpty(blogUrl)) {
            return;
        }
        if (mValidGhostBlogUrl != null && blogUrl.equals(mValidGhostBlogUrl)) {
            // we already have a valid url, skip checking
            listener.setCheckBlogUrlSubscription(null);
            listener.onSuccess();
            return;
        }
        mValidGhostBlogUrl = null;
        mCheckBlogUrlListeners.add(listener);
        listener.onCheckStarted();
        Action1<Throwable> invalidGhostUrlHandler = (e) -> {
            mValidGhostBlogUrl = null;
            Log.e(TAG, Log.getStackTraceString(e));
            listener.setCheckBlogUrlSubscription(null);
            listener.onError(blogUrl, e);
            mCheckBlogUrlListeners.remove(listener);
        };

        // try HTTPS and HTTP, in that order, if none is given
        if (hasScheme(blogUrl)) {
            tryUrl(null, blogUrl, listener, invalidGhostUrlHandler);
        } else {
            tryUrl(NetworkUtils.SCHEME_HTTPS, blogUrl, listener, (e) -> {
                tryUrl(NetworkUtils.SCHEME_HTTP, blogUrl, listener, invalidGhostUrlHandler);
            });
        }
    }

    private abstract class CheckBlogUrlListener {
        private Subscription mCheckBlogUrlSubscription = null;
        public Subscription getCheckBlogUrlSubscription() {
            return mCheckBlogUrlSubscription;
        }
        public void setCheckBlogUrlSubscription(@Nullable Subscription s) {
            mCheckBlogUrlSubscription = s;
        }
        abstract void onReset();
        abstract void onCheckStarted();
        abstract void onSuccess();
        abstract void onError(@NonNull String blogUrl, Throwable e);
    }

    private void tryUrl(@Nullable @NetworkUtils.Scheme String scheme, @NonNull String blogUrl,
                        @NonNull CheckBlogUrlListener listener, @NonNull Action1<Throwable> errorHandler) {
        if (scheme != null) {
            blogUrl = scheme + blogUrl;
        }
        Subscription subscription = NetworkUtils.checkGhostBlog(blogUrl)
                .subscribe((validGhostBlogUrl) -> {
                    mValidGhostBlogUrl = validGhostBlogUrl;
                    listener.setCheckBlogUrlSubscription(null);
                    listener.onSuccess();
                    mCheckBlogUrlListeners.remove(listener);
                }, errorHandler);
        listener.setCheckBlogUrlSubscription(subscription);
    }

    private void stopCheckingBlogUrl() {
        List<CheckBlogUrlListener> listenersToRemove = new ArrayList<>();
        for (CheckBlogUrlListener listener : mCheckBlogUrlListeners) {
            Subscription subscription = listener.getCheckBlogUrlSubscription();
            if (subscription != null && !subscription.isUnsubscribed()) {
                mValidGhostBlogUrl = null;
                // cancel any ongoing network requests
                subscription.unsubscribe();
                listener.setCheckBlogUrlSubscription(null);
                listener.onReset();
                listenersToRemove.add(listener); // can't remove directly, produces ConcurrentModificationException on next iteration
            }
        }
        for (CheckBlogUrlListener listener : listenersToRemove) {
            mCheckBlogUrlListeners.remove(listener);
        }
    }

    // intentionally not static
    // used for checking the blog url on focus out
    private class TextChangeCheckBlogUrlListener extends CheckBlogUrlListener {
        @Override
        public void onReset() {
            mBlogUrlLayout.setErrorEnabled(false);
            mBlogUrlLayout.setError(null);
            mBlogUrlProgressView.setVisibility(View.INVISIBLE);
            mBlogUrlValidView.setVisibility(View.INVISIBLE);
        }

        @Override
        public void onCheckStarted() {
            mBlogUrlLayout.setErrorEnabled(false);
            mBlogUrlLayout.setError(null);
            mBlogUrlProgressView.setVisibility(View.VISIBLE);
            mBlogUrlValidView.setVisibility(View.INVISIBLE);
        }

        @Override
        public void onSuccess() {
            mBlogUrlLayout.setErrorEnabled(false);
            mBlogUrlLayout.setError(null);
            mBlogUrlProgressView.setVisibility(View.INVISIBLE);
            mBlogUrlValidView.setVisibility(View.VISIBLE);
        }

        @Override
        public void onError(@NonNull String blogUrl, Throwable e) {
            displayErrorResponse(blogUrl, e);
            mBlogUrlProgressView.setVisibility(View.INVISIBLE);
            mBlogUrlValidView.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Attempts to sign in the account specified by the login form. If there are form errors
     * (invalid email, missing fields, etc.), the errors are presented and no actual login attempt
     * is made.
     */
    @SuppressWarnings("OverlyLongMethod")
    private void attemptLogin() {
        checkBlogUrl(new CheckBlogUrlListener() {
            @Override
            public void onReset() {
                showProgress(false);
            }

            @Override
            public void onCheckStarted() {
                showProgress(true);
                KeyboardUtils.defocusAndHideKeyboard(LoginActivity.this);
            }

            @Override
            public void onSuccess() {
                attemptLoginWithValidUrl();
            }

            @Override
            public void onError(@NonNull String blogUrl, Throwable e) {
                showProgress(false);
                displayErrorResponse(blogUrl, e);
            }
        });
    }

    private void displayErrorResponse(@NonNull String blogUrl, Throwable e) {
        mBlogUrlLayout.setErrorEnabled(true);
        String errorStr;
        if (isUserNetworkError(e)) {
            errorStr = getString(R.string.no_such_blog, blogUrl);
        } else if (isConnectionError(e)) {
            errorStr = getString(R.string.login_connection_error, blogUrl);
        } else if (e instanceof SSLHandshakeException) {
            errorStr = getString(R.string.login_ssl_unsupported);
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        } else {
            errorStr = getString(R.string.login_unexpected_error);
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
        mBlogUrlLayout.setError(errorStr);
    }

    private void attemptLoginWithValidUrl() {
        // by the time this is called, mValidGhostBlogUrl MUST be valid
        if (TextUtils.isEmpty(mValidGhostBlogUrl) || !hasScheme(mValidGhostBlogUrl)) {
            throw new IllegalStateException("the \"valid ghost blog url\" is not actually valid, " +
                    "value = " + String.valueOf(mValidGhostBlogUrl));
        }

        // reset errors
        mEmailLayout.setError(null);
        mPasswordLayout.setError(null);

        // store values at the time of the login attempt
        String blogUrl = mValidGhostBlogUrl;
        String email = mEmailView.getText().toString().trim();
        String password = mPasswordView.getText().toString().trim();

        boolean hasError = false;
        View focusView = null;

        // check for a valid password
        if (TextUtils.isEmpty(password)) {
            mPasswordLayout.setError(getString(R.string.error_field_required));
            focusView = mPasswordView;
            hasError = true;
        } else if (! isPasswordValid(password)) {
            mPasswordLayout.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            hasError = true;
        }

        // check for a valid email address
        if (TextUtils.isEmpty(email)) {
            mEmailLayout.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            hasError = true;
        } else if (! isEmailValid(email)) {
            mEmailLayout.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            hasError = true;
        }

        if (hasError) {
            // there was an error; focus the first form field with an error
            focusView.requestFocus();
        } else {
            // actual login attempt
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
                    && isUserNetworkError(error.getCause());
            // connection error: timeout, etc
            boolean connectionError = error.getKind() == RetrofitError.Kind.NETWORK
                    && isConnectionError(error.getCause());
            // don't remember when this happens, but it does happen consistently in one error scenario
            boolean conversionError = error.getKind() == RetrofitError.Kind.CONVERSION;
            String blogUrl = event.blogUrl;
            if (userNetworkError || conversionError) {
                mBlogUrlLayout.setErrorEnabled(true);
                mBlogUrlLayout.setError(getString(R.string.no_such_blog, blogUrl));
                mBlogUrlView.requestFocus();
            } else if (connectionError) {
                mBlogUrlLayout.setErrorEnabled(false);
                mBlogUrlLayout.setError(getString(R.string.login_connection_error, blogUrl));
            } else {
                Crashlytics.log(Log.ERROR, TAG, "generic error message triggered during login!");
                Toast.makeText(this, getString(R.string.login_unexpected_error),
                        Toast.LENGTH_SHORT).show();
                mBlogUrlLayout.setErrorEnabled(false);
                mBlogUrlLayout.setError(null);
            }
        } finally {
            Crashlytics.log(Log.ERROR, TAG, "Blog URL: " + mValidGhostBlogUrl);
            Crashlytics.logException(new LoginFailedException(error));        // report login failures to Crashlytics
            Log.e(TAG, Log.getStackTraceString(error));
        }
    }


    public static boolean isUserNetworkError(Throwable error) {
        // user provided a malformed / non-existent URL
        return error instanceof UnknownHostException || error instanceof MalformedURLException;
    }

    public static boolean isConnectionError(Throwable error) {
        return error instanceof ConnectException || error instanceof SocketTimeoutException;
    }

    private static boolean isEmailValid(String email) {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private static boolean isPasswordValid(String password) {
        return password.length() >= 8;
    }

    private static boolean hasScheme(String url) {
        return url.startsWith(NetworkUtils.SCHEME_HTTP) || url.startsWith(NetworkUtils.SCHEME_HTTPS);
    }

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

package me.vickychijwani.spectre.view.fragments;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Html;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.vickychijwani.spectre.R;
import me.vickychijwani.spectre.auth.LoginOrchestrator;
import me.vickychijwani.spectre.util.KeyboardUtils;
import me.vickychijwani.spectre.util.Listenable;
import me.vickychijwani.spectre.util.NetworkUtils;
import me.vickychijwani.spectre.view.LoginActivity;

public class PasswordAuthFragment extends BaseFragment implements
        TextView.OnEditorActionListener,
        LoginOrchestrator.Listener
{

    @Bind(R.id.email)                   EditText mEmailView;
    @Bind(R.id.email_error)             TextView mEmailErrorView;
    @Bind(R.id.password)                EditText mPasswordView;
    @Bind(R.id.password_error)          TextView mPasswordErrorView;
    @Bind(R.id.sign_in_btn)             View mSignInBtn;
    @Bind(R.id.progress)                ProgressBar mProgress;

    private Listenable<LoginOrchestrator.Listener> mLoginOrchestrator = null;

    public static PasswordAuthFragment newInstance() {
        PasswordAuthFragment fragment = new PasswordAuthFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_password_auth, container, false);
        ButterKnife.bind(this, view);
        mPasswordView.setOnEditorActionListener(this);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        KeyboardUtils.focusAndShowKeyboard(getActivity(), mEmailView);
        mLoginOrchestrator = ((LoginActivity) getActivity()).getLoginOrchestratorListenable();
    }

    @Override
    public void onStart() {
        super.onStart();
        mLoginOrchestrator.listen(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        stopWaiting();
        mLoginOrchestrator.unlisten(this);
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == getResources().getInteger(R.integer.ime_action_id_signin)
                || actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_NULL) {
            onSignInClicked();
            // don't consume the event, so the keyboard can also be hidden
            // http://stackoverflow.com/questions/2342620/how-to-hide-keyboard-after-typing-in-edittext-in-android#comment20849208_10184099
            return false;
        }
        return false;
    }

    @OnClick(R.id.email_layout)
    public void onEmailLayoutClicked() {
        KeyboardUtils.focusAndShowKeyboard(getActivity(), mEmailView);
    }

    @OnClick(R.id.password_layout)
    public void onPasswordLayoutClicked() {
        KeyboardUtils.focusAndShowKeyboard(getActivity(), mPasswordView);
    }

    @OnClick(R.id.sign_in_btn)
    public void onSignInClicked() {
        if (! NetworkUtils.isConnected(getActivity())) {
            Toast.makeText(getActivity(), R.string.no_internet_connection, Toast.LENGTH_SHORT).show();
            return;
        }

        String email = mEmailView.getText().toString().trim();
        String password = mPasswordView.getText().toString().trim();

        boolean hasError = false;
        View focusView = null;

        // check for a valid email address
        if (TextUtils.isEmpty(email)) {
            showEmailError(getString(R.string.error_field_required));
            focusView = mEmailView;
            hasError = true;
        } else if (! isEmailValid(email)) {
            showEmailError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            hasError = true;
        } else {
            showEmailError(null);
        }

        // check for a valid password
        if (TextUtils.isEmpty(password)) {
            showPasswordError(getString(R.string.error_field_required));
            focusView = mPasswordView;
            hasError = true;
        } else if (! isPasswordValid(password)) {
            showPasswordError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            hasError = true;
        } else {
            showPasswordError(null);
        }

        if (hasError) {
            // there was an error; focus the first form field with an error
            focusView.requestFocus();
        } else {
            // actual login attempt
            startWaiting();
            ((LoginActivity) getActivity()).onEmailAndPassword(email, password);
        }
    }

    @Override
    public void onStartWaiting() {
        startWaiting();
    }

    @Override
    public void onBlogUrlError(LoginOrchestrator.UrlErrorType errorType, @NonNull Throwable error,
                               @NonNull String blogUrl) {
        // no-op
    }

    @Override
    public void onApiError(String error, boolean isEmailError) {
        stopWaiting();
        EditText errorView = mPasswordView;
        TextView errorMsgView = mPasswordErrorView;
        if (isEmailError) {
            errorView = mEmailView;
            errorMsgView = mEmailErrorView;
        }
        errorView.setSelection(errorView.getText().length());
        KeyboardUtils.focusAndShowKeyboard(getActivity(), errorView);
        errorMsgView.setVisibility(View.VISIBLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            errorMsgView.setText(Html.fromHtml(error, Html.FROM_HTML_MODE_LEGACY));
        } else {
            errorMsgView.setText(Html.fromHtml(error));
        }
    }

    @Override
    public void onGhostV0Error() {
        // no-op
    }

    @Override
    public void onLoginDone(String blogUrl) {
        // no-op
    }

    private void startWaiting() {
        allowInput(false);
        showEmailError(null);
        showPasswordError(null);
        mProgress.setVisibility(View.VISIBLE);
        mSignInBtn.setVisibility(View.INVISIBLE);
    }

    private void stopWaiting() {
        allowInput(true);
        mProgress.setVisibility(View.INVISIBLE);
        mSignInBtn.setVisibility(View.VISIBLE);
    }

    private void allowInput(boolean allow) {
        mEmailView.setEnabled(allow);
        mPasswordView.setEnabled(allow);
        mSignInBtn.setEnabled(allow);
    }

    private void showEmailError(@Nullable String error) {
        if (error == null || error.isEmpty()) {
            mEmailErrorView.setText("");
            mEmailErrorView.setVisibility(View.GONE);
        } else {
            mEmailErrorView.setText(error);
            mEmailErrorView.setVisibility(View.VISIBLE);
        }
    }

    private void showPasswordError(@Nullable String error) {
        if (error == null || error.isEmpty()) {
            mPasswordErrorView.setText("");
            mPasswordErrorView.setVisibility(View.INVISIBLE);
        } else {
            mPasswordErrorView.setText(error);
            mPasswordErrorView.setVisibility(View.VISIBLE);
        }
    }

    private static boolean isEmailValid(String email) {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private static boolean isPasswordValid(String password) {
        return password.length() >= 8;
    }

}

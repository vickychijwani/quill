package me.vickychijwani.spectre.view.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
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
import me.vickychijwani.spectre.pref.UserPrefs;
import me.vickychijwani.spectre.util.KeyboardUtils;
import me.vickychijwani.spectre.util.Listenable;
import me.vickychijwani.spectre.util.NetworkUtils;
import me.vickychijwani.spectre.view.LoginActivity;

public class LoginUrlFragment extends BaseFragment implements
        TextView.OnEditorActionListener,
        LoginOrchestrator.Listener
{

    @Bind(R.id.blog_url)                EditText mBlogUrlView;
    @Bind(R.id.next_btn)                View mNextBtn;
    @Bind(R.id.blog_url_error)          TextView mBlogUrlErrorView;
    @Bind(R.id.progress)                ProgressBar mProgress;

    private Listenable<LoginOrchestrator.Listener> mLoginOrchestrator = null;

    public static LoginUrlFragment newInstance() {
        LoginUrlFragment fragment = new LoginUrlFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_login_url, container, false);
        ButterKnife.bind(this, view);

        String blogUrl = UserPrefs.getInstance(getActivity()).getString(UserPrefs.Key.BLOG_URL);
        mBlogUrlView.setText(blogUrl.replaceFirst("^https?://", ""));
        mBlogUrlView.setSelection(mBlogUrlView.getText().length());
        mBlogUrlView.setOnEditorActionListener(this);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
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
        if (actionId == getResources().getInteger(R.integer.ime_action_id_next)
                || actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_NULL) {
            onNextClicked();
            // don't consume the event, so the keyboard can also be hidden
            // http://stackoverflow.com/questions/2342620/how-to-hide-keyboard-after-typing-in-edittext-in-android#comment20849208_10184099
            return false;
        }
        return false;
    }

    @OnClick(R.id.blog_url_layout)
    public void onBlogUrlLayoutClicked() {
        KeyboardUtils.focusAndShowKeyboard(getActivity(), mBlogUrlView);
    }

    @OnClick(R.id.next_btn)
    public void onNextClicked() {
        if (! NetworkUtils.isConnected(getActivity())) {
            Toast.makeText(getActivity(), R.string.no_internet_connection, Toast.LENGTH_SHORT).show();
            return;
        }

        String blogUrl = mBlogUrlView.getText().toString().trim().replaceFirst("^(.*)/ghost/?$", "$1");
        if (TextUtils.isEmpty(blogUrl)) {
            mBlogUrlErrorView.setText(R.string.error_field_required);
            return;
        }

        ((LoginActivity) getActivity()).onBlogUrl(blogUrl);
    }

    @Override
    public void onStartWaiting() {
        startWaiting();
    }

    @Override
    public void onBlogUrlError(LoginOrchestrator.UrlErrorType errorType, @NonNull Throwable error,
                               @NonNull String blogUrl) {
        stopWaiting();
        String errorStr;
        switch (errorType) {
            case ERR_CONNECTION:
                errorStr = getString(R.string.login_connection_error, blogUrl);
                break;
            case ERR_USER_NETWORK:
                errorStr = getString(R.string.no_such_blog, blogUrl);
                break;
            case ERR_SSL:
                errorStr = getString(R.string.login_ssl_unsupported);
                Toast.makeText(getActivity(), error.getMessage(), Toast.LENGTH_LONG).show();
                break;
            default:
                errorStr = getString(R.string.login_unexpected_error);
                Toast.makeText(getActivity(), error.getMessage(), Toast.LENGTH_LONG).show();
                break;
        }
        mBlogUrlErrorView.setText(errorStr);
        mBlogUrlView.setSelection(mBlogUrlView.getText().length());
        KeyboardUtils.focusAndShowKeyboard(getActivity(), mBlogUrlView);
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
        mBlogUrlErrorView.setText("");
        mProgress.setVisibility(View.VISIBLE);
        mNextBtn.setVisibility(View.INVISIBLE);
    }

    private void stopWaiting() {
        allowInput(true);
        mProgress.setVisibility(View.INVISIBLE);
        mNextBtn.setVisibility(View.VISIBLE);
    }

    private void allowInput(boolean allow) {
        mBlogUrlView.setEnabled(allow);
        mNextBtn.setEnabled(allow);
    }

}

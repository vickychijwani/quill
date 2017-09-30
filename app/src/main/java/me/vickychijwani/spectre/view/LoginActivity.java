package me.vickychijwani.spectre.view;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import me.vickychijwani.spectre.R;
import me.vickychijwani.spectre.SpectreApplication;
import me.vickychijwani.spectre.auth.CredentialSource;
import me.vickychijwani.spectre.auth.GhostAuth;
import me.vickychijwani.spectre.auth.LoginOrchestrator;
import me.vickychijwani.spectre.auth.PasswordAuth;
import me.vickychijwani.spectre.util.Listenable;
import me.vickychijwani.spectre.util.Pair;
import me.vickychijwani.spectre.view.fragments.GhostAuthFragment;
import me.vickychijwani.spectre.view.fragments.GhostV0ErrorFragment;
import me.vickychijwani.spectre.view.fragments.LoginUrlFragment;
import me.vickychijwani.spectre.view.fragments.PasswordAuthFragment;

public class LoginActivity extends BaseActivity implements
        CredentialSource,
        LoginOrchestrator.Listener
{

    private final String TAG = "LoginActivity";

    private LoginOrchestrator mLoginOrchestrator = null;
    // Rx subject for Ghost auth
    private final PublishSubject<String> mAuthCodeSubject = PublishSubject.create();
    // Rx subject for password auth
    private final PublishSubject<Pair<String, String>> mCredentialsSubject = PublishSubject.create();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setLayout(R.layout.activity_login);

        LoginOrchestrator.HACKListener hackListener = SpectreApplication.getInstance().getHACKListener();
        if (mLoginOrchestrator == null) {
            mLoginOrchestrator = LoginOrchestrator.create(this, hackListener);
        }

        // TODO MEMLEAK the fragment might already exist
        LoginUrlFragment urlFragment = LoginUrlFragment.newInstance();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, urlFragment)
                .commit();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mLoginOrchestrator.registerOnEventBus();
        mLoginOrchestrator.listen(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // pop the GhostAuthFragment etc if any
        getSupportFragmentManager().popBackStack();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mLoginOrchestrator.unlisten(this);
        mLoginOrchestrator.unregisterFromEventBus();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLoginOrchestrator.reset();
    }

    public Listenable<LoginOrchestrator.Listener> getLoginOrchestratorListenable() {
        return mLoginOrchestrator;
    }

    public void onBlogUrl(@NonNull String blogUrl) {
        mLoginOrchestrator.start(blogUrl);
    }

    @Override
    public Observable<String> getGhostAuthCode(GhostAuth.Params params) {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (!(currentFragment instanceof GhostAuthFragment)) {
            GhostAuthFragment fragment = GhostAuthFragment.newInstance(params);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        }
        return mAuthCodeSubject;
    }

    public void onGhostAuthCode(@NonNull String authCode) {
        mAuthCodeSubject.onNext(authCode);
    }

    @Override
    public Observable<Pair<String, String>> getEmailAndPassword(PasswordAuth.Params params) {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (!(currentFragment instanceof PasswordAuthFragment)) {
            PasswordAuthFragment newFragment = PasswordAuthFragment.newInstance();
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, newFragment)
                    .addToBackStack(null)
                    .commit();
        }
        return mCredentialsSubject;
    }

    public void onEmailAndPassword(@NonNull String email, @NonNull String password) {
        mCredentialsSubject.onNext(new Pair<>(email, password));
    }

    @Override
    public void onStartWaiting() {}

    @Override
    public void onBlogUrlError(LoginOrchestrator.ErrorType errorType, @NonNull Throwable error, @NonNull String blogUrl) {}

    @Override
    public void onApiError(String error, boolean isEmailError) {
        // no-op
    }

    @Override
    public void onGhostV0Error() {
        Fragment fragment = GhostV0ErrorFragment.newInstance();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onNetworkError(LoginOrchestrator.ErrorType errorType, @NonNull Throwable error) {
        // no-op
    }

    @Override
    public void onLoginDone() {
        finish();
        Intent intent = new Intent(this, PostListActivity.class);
        startActivity(intent);
    }

}

package me.vickychijwani.spectre.view;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Pair;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import me.vickychijwani.spectre.R;
import me.vickychijwani.spectre.SpectreApplication;
import me.vickychijwani.spectre.auth.GhostAuth;
import me.vickychijwani.spectre.auth.LoginOrchestrator;
import me.vickychijwani.spectre.pref.UserPrefs;
import me.vickychijwani.spectre.util.Listenable;
import me.vickychijwani.spectre.view.fragments.GenericFragment;
import me.vickychijwani.spectre.view.fragments.GhostAuthFragment;
import me.vickychijwani.spectre.view.fragments.LoginUrlFragment;
import me.vickychijwani.spectre.view.fragments.PasswordAuthFragment;
import okhttp3.OkHttpClient;

public class LoginActivity extends BaseActivity implements
        LoginOrchestrator.CredentialSource,
        LoginOrchestrator.Listener
{

    private final String TAG = "LoginActivity";

    private LoginOrchestrator mLoginOrchestrator = null;
    // Rx subject for Ghost auth
    private PublishSubject<String> mAuthCodeSubject = PublishSubject.create();
    // Rx subject for password auth
    private PublishSubject<Pair<String, String>> mCredentialsSubject = PublishSubject.create();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setLayout(R.layout.activity_login);

        OkHttpClient httpClient = SpectreApplication.getInstance().getOkHttpClient();
        LoginOrchestrator.HACKListener hackListener = SpectreApplication.getInstance().getHACKListener();
        if (mLoginOrchestrator == null) {
            mLoginOrchestrator = new LoginOrchestrator(httpClient, this, hackListener);
        }

        // TODO MEMLEAK the fragment might already exist
        LoginUrlFragment urlFragment = LoginUrlFragment.newInstance();
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.fragment_container, urlFragment)
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
    public Observable<Pair<String, String>> getEmailAndPassword(LoginOrchestrator.PasswordAuthParams __) {
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
    public void onBlogUrlError(LoginOrchestrator.UrlErrorType errorType, @NonNull Throwable error, @NonNull String blogUrl) {}

    @Override
    public void onApiError(String error, boolean isEmailError) {
        // no-op
    }

    @Override
    public void onGhostV0Error() {
        GenericFragment fragment = GenericFragment.newInstance(R.layout.fragment_ghost_v0_error);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onLoginDone(String blogUrl) {
        UserPrefs prefs = UserPrefs.getInstance(this);
        prefs.setString(UserPrefs.Key.BLOG_URL, blogUrl);
        finish();

        Intent intent = new Intent(this, PostListActivity.class);
        startActivity(intent);
    }

}

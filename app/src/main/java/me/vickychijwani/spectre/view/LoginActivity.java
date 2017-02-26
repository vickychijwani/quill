package me.vickychijwani.spectre.view;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Pair;

import io.reactivex.Single;
import io.reactivex.subjects.SingleSubject;
import me.vickychijwani.spectre.R;
import me.vickychijwani.spectre.SpectreApplication;
import me.vickychijwani.spectre.auth.GhostAuth;
import me.vickychijwani.spectre.auth.LoginOrchestrator;
import me.vickychijwani.spectre.pref.UserPrefs;
import me.vickychijwani.spectre.util.Listenable;
import me.vickychijwani.spectre.view.fragments.GenericFragment;
import me.vickychijwani.spectre.view.fragments.GhostAuthFragment;
import me.vickychijwani.spectre.view.fragments.LoginUrlFragment;
import okhttp3.OkHttpClient;

public class LoginActivity extends BaseActivity implements
        LoginOrchestrator.CredentialSource,
        LoginOrchestrator.Listener
{

    private final String TAG = "LoginActivity";

    private LoginOrchestrator mLoginOrchestrator = null;
    private SingleSubject<String> mAuthCodeSubject = SingleSubject.create();
    private SingleSubject<Pair<String, String>> mPasswordAuthCredentialsSubject = SingleSubject.create();

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
    public Single<String> getGhostAuthCode(GhostAuth.Params params) {
        GhostAuthFragment fragment = GhostAuthFragment.newInstance(params);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
        return mAuthCodeSubject;
    }

    public void onGhostAuthCode(@NonNull String authCode) {
        mAuthCodeSubject.onSuccess(authCode);
        mAuthCodeSubject = SingleSubject.create();
    }

    @Override
    public Single<Pair<String, String>> getUsernameAndPassword(LoginOrchestrator.PasswordAuthParams params) {
        // TODO
        return mPasswordAuthCredentialsSubject;
    }

    @Override
    public void onStartWaiting() {}

    @Override
    public void onBlogUrlError(LoginOrchestrator.UrlErrorType errorType, @NonNull Throwable error, @NonNull String blogUrl) {}

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

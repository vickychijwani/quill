package me.vickychijwani.spectre.auth;

import io.reactivex.Observable;
import me.vickychijwani.spectre.SpectreApplication;
import me.vickychijwani.spectre.network.entity.AuthReqBody;
import me.vickychijwani.spectre.pref.AppState;
import me.vickychijwani.spectre.pref.UserPrefs;
import me.vickychijwani.spectre.util.Pair;

public class AuthStore implements CredentialSource {

    public void saveCredentials(AuthReqBody authReqBody) {
        UserPrefs prefs = UserPrefs.getInstance(SpectreApplication.getInstance());
        if (authReqBody.isGrantTypePassword()) {
            prefs.setString(UserPrefs.Key.EMAIL, authReqBody.email);
            prefs.setString(UserPrefs.Key.PASSWORD, authReqBody.password);
        } else {
            prefs.setString(UserPrefs.Key.AUTH_CODE, authReqBody.authorizationCode);
        }
    }

    public void deleteCredentials() {
        setLoggedIn(false);
        UserPrefs prefs = UserPrefs.getInstance(SpectreApplication.getInstance());
        prefs.clear(UserPrefs.Key.EMAIL);
        prefs.clear(UserPrefs.Key.PASSWORD);
        prefs.clear(UserPrefs.Key.AUTH_CODE);
    }

    @Override
    public Observable<String> getGhostAuthCode(GhostAuth.Params params) {
        UserPrefs prefs = UserPrefs.getInstance(SpectreApplication.getInstance());
        String authCode = prefs.getString(UserPrefs.Key.AUTH_CODE);
        return Observable.just(authCode);
    }

    @Override
    public Observable<Pair<String, String>> getEmailAndPassword() {
        UserPrefs prefs = UserPrefs.getInstance(SpectreApplication.getInstance());
        String email = prefs.getString(UserPrefs.Key.EMAIL);
        String password = prefs.getString(UserPrefs.Key.PASSWORD);
        return Observable.just(new Pair<>(email, password));
    }

    public boolean isLoggedIn() {
        AppState appState = AppState.getInstance(SpectreApplication.getInstance());
        return appState.getBoolean(AppState.Key.LOGGED_IN);
    }

    public void setLoggedIn(boolean isLoggedIn) {
        AppState appState = AppState.getInstance(SpectreApplication.getInstance());
        appState.setBoolean(AppState.Key.LOGGED_IN, isLoggedIn);
    }

}

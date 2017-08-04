package me.vickychijwani.spectre.auth;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import me.vickychijwani.spectre.error.TokenRevocationFailedException;
import me.vickychijwani.spectre.event.CredentialsExpiredEvent;
import me.vickychijwani.spectre.model.entity.AuthToken;
import me.vickychijwani.spectre.network.GhostApiService;
import me.vickychijwani.spectre.network.entity.AuthReqBody;
import me.vickychijwani.spectre.network.entity.ConfigurationList;
import me.vickychijwani.spectre.network.entity.RefreshReqBody;
import me.vickychijwani.spectre.network.entity.RevokeReqBody;
import me.vickychijwani.spectre.util.Listenable;
import me.vickychijwani.spectre.util.NetworkUtils;
import timber.log.Timber;

import static me.vickychijwani.spectre.event.BusProvider.getBus;

public class AuthService implements Listenable<AuthService.Listener> {

    private final String mBlogUrl;
    private final GhostApiService mApi;
    private final CredentialSource mCredentialSource;
    private final CredentialSink mCredentialSink;

    // state
    private Listener mListener = null;
    private boolean mbRequestOngoing = false;

    public static AuthService createWithStoredCredentials(String blogUrl, GhostApiService api) {
        AuthStore storedCredSourceAndSink = new AuthStore();
        return new AuthService(blogUrl, api, storedCredSourceAndSink, storedCredSourceAndSink);
    }

    public static AuthService createWithGivenCredentials(String blogUrl, GhostApiService api,
                                                         CredentialSource credSource) {
        CredentialSink credSink = new AuthStore();
        return new AuthService(blogUrl, api, credSource, credSink);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    AuthService(String blogUrl, GhostApiService api,
                CredentialSource credSource, CredentialSink credSink) {
        mBlogUrl = blogUrl;
        mApi = api;
        mCredentialSource = credSource;
        mCredentialSink = credSink;
    }

    @Override
    public void listen(@NonNull Listener listener) {
        mListener = listener;
    }

    @Override
    public void unlisten(@NonNull Listener listener) {
        mListener = null;
    }

    public void refreshToken(AuthToken token) {
        if (mbRequestOngoing) {
            return;
        }
        mbRequestOngoing = true;
        mApi.getConfiguration()
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                .map(ConfigurationList::getClientSecret)
                .map(clientSecret -> new RefreshReqBody(token.getRefreshToken(), clientSecret))
                .flatMap(mApi::refreshAuthToken)
                // since this token was just refreshed, it doesn't have a refresh token, so add that
                .doOnNext(authToken -> authToken.setRefreshToken(token.getRefreshToken()))
                    .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleAuthToken, this::handleRefreshError);
    }

    public void revokeToken(AuthToken token) {
        mApi.getConfiguration()
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                .map(ConfigurationList::getClientSecret)
                .flatMap(clientSecret -> revokeToken(token, clientSecret))
                .doOnError(Timber::e)
                .subscribe();
    }


    private void loginAgain() {
        if (mCredentialSource != mCredentialSink) {
            throw new UnsupportedOperationException("This method can only handle the case where " +
                    "the credential source is the same as the sink, because it does not attempt " +
                    "to save the credentials in case of a successful login.");
        }
        if (mbRequestOngoing) {
            return;
        }
        mbRequestOngoing = true;
        mApi.getConfiguration()
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                .flatMap(this::getAuthReqBody)
                // no need to call mCredentialSink::saveCredentials here since the credentials came
                // from the same object anyway (source == sink as per check above)
                .flatMap(mApi::getAuthToken)
                    .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleAuthToken, this::handleLoginError);
    }

    private void handleAuthToken(AuthToken token) {
        mbRequestOngoing = false;
        mCredentialSink.setLoggedIn(mBlogUrl, true);
        // deliberately missing mListener != null check, to avoid suppressing errors
        mListener.onNewAuthToken(token);
    }

    private void handleRefreshError(Throwable e) {
        mbRequestOngoing = false;
        if (NetworkUtils.isUnauthorized(e)) {
            // recover by generating a new auth token with known credentials
            loginAgain();
        } else {
            // deliberately missing mListener != null check, to avoid suppressing errors
            mListener.onUnrecoverableFailure();
        }
    }

    private void handleLoginError(Throwable e) {
        mbRequestOngoing = false;
        // TODO Ghost returns 422 for an incorrect password, but I haven't tested what it returns for an
        // TODO expired / incorrect Ghost Auth code. Checking isUnauthorized just to be safe.
        if (NetworkUtils.isUnprocessableEntity(e) || NetworkUtils.isUnauthorized(e)) {
            // password changed / auth code expired
            mCredentialSink.deleteCredentials(mBlogUrl);
            getBus().post(new CredentialsExpiredEvent());
        } else {
            // deliberately missing mListener != null check, to avoid suppressing errors
            mListener.onUnrecoverableFailure();
        }
    }

    private Observable<JsonElement> revokeToken(AuthToken token, String clientSecret) {
        // this complexity exists because the access token must be revoked AFTER the refresh token
        // why? because the access token is needed for both revocations!
        Subject<JsonElement> responses = PublishSubject.create();
        RevokeReqBody refreshReqBody = RevokeReqBody.fromRefreshToken(
                token.getRefreshToken(), clientSecret);
        revokeSingleToken(token.getAuthHeader(), refreshReqBody, responses)
                .doOnComplete(() -> {
                    RevokeReqBody accessReqBody = RevokeReqBody.fromAccessToken(
                            token.getAccessToken(), clientSecret);
                    revokeSingleToken(token.getAuthHeader(), accessReqBody, responses)
                            .subscribe();
                })
                .subscribe();
        return responses;
    }

    private Observable<JsonElement> revokeSingleToken(String authHeader, RevokeReqBody reqBody,
                                                      Observer<JsonElement> observer) {
        return mApi.revokeAuthToken(authHeader, reqBody)
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                .map(res -> checkRevocationResponse(res, reqBody.tokenTypeHint))
                .doOnNext(observer::onNext)
                .doOnError(e -> wrapInTokenRevocationFailedException(e, reqBody.tokenTypeHint));
    }


    // helpers
    Observable<AuthReqBody> getAuthReqBody(ConfigurationList config) {
        String clientSecret = config.getClientSecret();
        if (authTypeIsGhostAuth(config)) {
            Timber.i("Using Ghost auth strategy for login");
            final GhostAuth.Params params = extractGhostAuthParams(mBlogUrl, config);
            return getGhostAuthReqBody(params, clientSecret, params.redirectUri);
        } else {
            Timber.i("Using password auth strategy for login");
            final PasswordAuth.Params params = new PasswordAuth.Params(mBlogUrl);
            return getPasswordAuthReqBody(params, clientSecret);
        }
    }

    private Observable<AuthReqBody> getGhostAuthReqBody(GhostAuth.Params params,
                                                        String clientSecret, String redirectUri) {
        return mCredentialSource.getGhostAuthCode(params)
                .map(authCode -> AuthReqBody.fromAuthCode(clientSecret, authCode, redirectUri));
    }

    private Observable<AuthReqBody> getPasswordAuthReqBody(PasswordAuth.Params params,
                                                           String clientSecret) {
        return mCredentialSource.getEmailAndPassword(params)
                .map(cred -> AuthReqBody.fromPassword(clientSecret, cred.first, cred.second));
    }

    private static GhostAuth.Params extractGhostAuthParams(String blogUrl, ConfigurationList config) {
        String authUrl = config.get("ghostAuthUrl");
        String ghostAuthId = config.get("ghostAuthId");
        String redirectUri = extractRedirectUri(config);
        if (authUrl == null || ghostAuthId == null || redirectUri == null) {
            throw new NullPointerException("A required parameter is null! values = "
                    + authUrl + ", " + ghostAuthId + ", " + redirectUri);
        }
        return new GhostAuth.Params(blogUrl, authUrl, ghostAuthId, redirectUri);
    }

    @Nullable
    private static String extractRedirectUri(ConfigurationList config) {
        String blogUrl = config.get("blogUrl");
        if (blogUrl == null) {
            return null;
        }
        return NetworkUtils.makeAbsoluteUrl(blogUrl, "ghost/");
    }

    private static boolean authTypeIsGhostAuth(ConfigurationList config) {
        return config.has("ghostAuthUrl");
    }

    private static JsonElement checkRevocationResponse(JsonElement jsonResponse, String tokenType) {
        JsonObject jsonObj = jsonResponse.getAsJsonObject();
        if (jsonObj.has("error")) {
            final String message = jsonObj.get("error").getAsString();
            throw new TokenRevocationFailedException(tokenType, message);
        }
        return jsonResponse;
    }

    private static Throwable wrapInTokenRevocationFailedException(Throwable e, String tokenType) {
        if (! (e instanceof TokenRevocationFailedException)) {
            return new TokenRevocationFailedException(tokenType, e);
        }
        return e;
    }


    public interface Listener {

        /**
         * A new auth token has been generated.
         * @param authToken - the new auth token
         */
        void onNewAuthToken(AuthToken authToken);

        /**
         * The process failed with an unrecoverable error.
         */
        void onUnrecoverableFailure();

    }

}

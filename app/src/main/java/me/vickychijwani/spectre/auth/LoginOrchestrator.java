package me.vickychijwani.spectre.auth;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.Pair;

import com.pacoworks.rxtuples2.RxTuples;

import java.util.HashSet;
import java.util.Set;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import io.reactivex.schedulers.Schedulers;
import me.vickychijwani.spectre.event.LoginDoneEvent;
import me.vickychijwani.spectre.model.entity.AuthToken;
import me.vickychijwani.spectre.model.entity.ConfigurationParam;
import me.vickychijwani.spectre.network.GhostApiService;
import me.vickychijwani.spectre.network.GhostApiUtils;
import me.vickychijwani.spectre.network.entity.AuthReqBody;
import me.vickychijwani.spectre.network.entity.ConfigurationList;
import me.vickychijwani.spectre.util.Listenable;
import me.vickychijwani.spectre.util.NetworkUtils;
import me.vickychijwani.spectre.util.functions.Action1;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import retrofit2.HttpException;
import retrofit2.Response;
import retrofit2.Retrofit;

import static me.vickychijwani.spectre.event.BusProvider.getBus;

/**
 * Orchestrates the entire login process from start to finish. Follows the Facade design pattern.
 */
public class LoginOrchestrator implements
        Listenable<LoginOrchestrator.Listener>
{

    private static final String TAG = LoginOrchestrator.class.getSimpleName();

    public enum UrlErrorType {
        ERR_CONNECTION,
        ERR_USER_NETWORK,
        ERR_SSL,
        ERR_UNKNOWN
    }

    private final OkHttpClient mHttpClient;
    private final CredentialSource mCredentialSource;
    private final HACKListener mHACKListener;

    // object state
    private final Set<Listener> mListeners;
    private Disposable mLoginFlowDisposable = Disposables.empty();

    public LoginOrchestrator(@NonNull OkHttpClient httpClient,
                             @NonNull CredentialSource credentialSource,
                             @NonNull HACKListener hackListener) {
        mHttpClient = httpClient;
        mCredentialSource = credentialSource;
        mHACKListener = hackListener;
        mListeners = new HashSet<>();
        reset();
    }

    public void reset() {
        mLoginFlowDisposable.dispose();
    }

    public void registerOnEventBus() {
        getBus().register(this);
    }

    public void unregisterFromEventBus() {
        getBus().unregister(this);
    }

    @Override
    public void listen(@NonNull Listener listener) {
        mListeners.add(listener);
    }

    @Override
    public void unlisten(@NonNull Listener listener) {
        mListeners.remove(listener);
    }

    public void start(@NonNull String inputBlogUrl) {
        reset();
        forEachListener(Listener::onStartWaiting);
        performLoginFlow(inputBlogUrl);
    }

    // the entire login flow is specified here, from start to finish
    private void performLoginFlow(String inputBlogUrl) {
        // READ THIS: https://upday.github.io/blog/subscribe_on/
        mLoginFlowDisposable = BlogUrlValidator
                .validate(inputBlogUrl, mHttpClient)
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                .flatMap(validBlogUrl -> {
                    Retrofit retrofit = GhostApiUtils.getRetrofit(validBlogUrl, mHttpClient);
                    GhostApiService api = retrofit.create(GhostApiService.class);
                    // FIXME FIXME FIXME
                    mHACKListener.setApiService(api, retrofit);

                    return api.getConfiguration()
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                            .flatMap(this::getAuthReqBody)
                                .observeOn(Schedulers.io())
                            .flatMap(api::getAuthToken)
                            .zipWith(Single.just(validBlogUrl), RxTuples.toPair());
                })
                    .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleAuthToken, this::handleAuthError);
    }

    private Single<AuthReqBody> getAuthReqBody(Response<ConfigurationList> response) throws HttpException {
        if (response.isSuccessful()) {
            ConfigurationList config = response.body();
            String clientSecret = config.get("clientSecret");
            if (authTypeIsGhostAuth(config)) {
                Log.d(TAG, "Using Ghost auth strategy for login");
                final GhostAuth.Params params = extractGhostAuthParams(config);
                return getGhostAuthReqBody(params, clientSecret, params.redirectUri);
            } else {
                Log.d(TAG, "Using password auth strategy for login");
                return getPasswordAuthReqBody(extractPasswordAuthParams(config), clientSecret);
            }
        } else {
            throw new HttpException(response);
        }
    }

    private Single<AuthReqBody> getGhostAuthReqBody(GhostAuth.Params params, String clientSecret,
                                                    String redirectUri) {
        return mCredentialSource.getGhostAuthCode(params)
                .map(authCode -> AuthReqBody.fromAuthCode(clientSecret, authCode, redirectUri));
    }

    private Single<AuthReqBody> getPasswordAuthReqBody(PasswordAuthParams params, String clientSecret) {
        return mCredentialSource.getUsernameAndPassword(params)
                .map(cred -> AuthReqBody.fromPassword(clientSecret, cred.first, cred.second));
    }

    private void handleAuthToken(org.javatuples.Pair<AuthToken, String> pair) {
        AuthToken authToken = pair.getValue0();
        String blogUrl = pair.getValue1();
        // FIXME FIXME FIXME FIXME
        mHACKListener.onNewAuthToken(authToken);
        forEachListener(l -> l.onLoginDone(blogUrl));
        getBus().post(new LoginDoneEvent(blogUrl,
                // FIXME FIXME FIXME FIXME!!!!
                true));
    }

    private void handleAuthError(Throwable e) {
        Log.e("LoginOrchestrator", Log.getStackTraceString(e));
        if (e instanceof BlogUrlValidator.UrlValidationException) {
            BlogUrlValidator.UrlValidationException urlEx = ((BlogUrlValidator.UrlValidationException) e);
            handleUrlValidationError(urlEx.getCause(), urlEx.getUrl());
        } else if (e instanceof HttpException && NetworkUtils.isUnauthorized(e)) {
            HttpUrl url = ((HttpException) e).response().raw().request().url();
            if (url.pathSegments().contains("configuration")) {
                // config was not publicly accessible before Ghost 1.x
                forEachListener(Listener::onGhostV0Error);
            } else if (url.pathSegments().contains("authentication")) {
                // TODO incorrect password / auth code ??? (VERIFY THIS!!)
            } else {
                // TODO no idea what to do here
            }
        } else {
            // TODO 1.x, but error
        }
    }

    public void handleUrlValidationError(Throwable error, String blogUrl) {
        UrlErrorType errorType = UrlErrorType.ERR_UNKNOWN;
        if (NetworkUtils.isUserNetworkError(error)) {
            errorType = UrlErrorType.ERR_USER_NETWORK;
        } else if (NetworkUtils.isConnectionError(error)) {
            errorType = UrlErrorType.ERR_CONNECTION;
        } else if (NetworkUtils.isSslError(error)) {
            errorType = UrlErrorType.ERR_SSL;
        }
        UrlErrorType finalErrorType = errorType;
        forEachListener(l -> l.onBlogUrlError(finalErrorType, error, blogUrl));
    }



    // helper methods
    private static GhostAuth.Params extractGhostAuthParams(ConfigurationList config) {
        String authUrl = config.get("ghostAuthUrl");
        String ghostAuthId = config.get("ghostAuthId");
        String redirectUri = extractRedirectUri(config);
        if (authUrl == null || ghostAuthId == null || redirectUri == null) {
            throw new NullPointerException("A required parameter is null! values = "
                    + authUrl + ", " + ghostAuthId + ", " + redirectUri);
        }
        return new GhostAuth.Params(authUrl, ghostAuthId, redirectUri);
    }

    private static PasswordAuthParams extractPasswordAuthParams(ConfigurationList config) {
        String clientSecret = config.get("clientSecret");
        if (clientSecret == null) {
            throw new NullPointerException("A required parameter is null! values = "
                    + clientSecret);
        }
        return new PasswordAuthParams(clientSecret);
    }

    @Nullable
    private static String extractRedirectUri(ConfigurationList config) {
        for (ConfigurationParam param : config.configuration) {
            if ("blogUrl".equals(param.getKey()) && param.getValue() != null) {
                String blogUrl = param.getValue();
                return NetworkUtils.makeAbsoluteUrl(blogUrl, "ghost/");
            }
        }
        return null;
    }

    private static boolean authTypeIsGhostAuth(ConfigurationList config) {
        for (ConfigurationParam param : config.configuration) {
            if ("ghostAuthUrl".equals(param.getKey())) {
                return true;
            }
        }
        return false;
    }

    private void forEachListener(Action1<Listener> action) {
        for (Listener listener : mListeners) {
            action.call(listener);
        }
    }



    public static class PasswordAuthParams {
        public final String clientSecret;

        public PasswordAuthParams(String clientSecret) {
            this.clientSecret = clientSecret;
        }
    }

    // FIXME this is basically a giant hack needed while I refactor NetworkService.java
    public interface HACKListener {
        void setApiService(GhostApiService api, Retrofit retrofit);
        void onNewAuthToken(AuthToken authToken);
    }

    public interface CredentialSource {
        Single<String> getGhostAuthCode(GhostAuth.Params params);
        Single<Pair<String, String>> getUsernameAndPassword(PasswordAuthParams params);
    }

    public interface Listener {
        /**
         * Start waiting for the next event.
         */
        void onStartWaiting();

        /**
         * An error occurred while accessing the blog URL.
         * @param errorType - the type of error that occurred
         * @param error - the exact error that occurred
         * @param blogUrl - the URL that was tried
         */
        void onBlogUrlError(UrlErrorType errorType, @NonNull Throwable error,
                            @NonNull String blogUrl);

        /**
         * Triggered if the user is using a version of Ghost older than 1.0.
         */
        void onGhostV0Error();

        /**
         * Login completed successfully.
         * @param blogUrl - the URL of the blog successfully logged in to
         */
        void onLoginDone(String blogUrl);
    }

}

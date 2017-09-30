package me.vickychijwani.spectre.auth;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import java.util.HashSet;
import java.util.Set;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import io.reactivex.schedulers.Schedulers;
import me.vickychijwani.spectre.SpectreApplication;
import me.vickychijwani.spectre.analytics.AnalyticsService;
import me.vickychijwani.spectre.error.LoginFailedException;
import me.vickychijwani.spectre.event.LoginDoneEvent;
import me.vickychijwani.spectre.event.LoginErrorEvent;
import me.vickychijwani.spectre.model.entity.AuthToken;
import me.vickychijwani.spectre.network.ApiProvider;
import me.vickychijwani.spectre.network.ApiProviderFactory;
import me.vickychijwani.spectre.network.GhostApiService;
import me.vickychijwani.spectre.network.GhostApiUtils;
import me.vickychijwani.spectre.network.entity.ApiError;
import me.vickychijwani.spectre.network.entity.ApiErrorList;
import me.vickychijwani.spectre.network.entity.AuthReqBody;
import me.vickychijwani.spectre.network.entity.ConfigurationList;
import me.vickychijwani.spectre.util.Listenable;
import me.vickychijwani.spectre.util.NetworkUtils;
import me.vickychijwani.spectre.util.functions.Action1;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import retrofit2.HttpException;
import timber.log.Timber;

import static me.vickychijwani.spectre.event.BusProvider.getBus;

/**
 * Orchestrates the entire login process from start to finish.
 */
public class LoginOrchestrator implements
        Listenable<LoginOrchestrator.Listener>
{

    public enum ErrorType {
        ERR_CONNECTION,
        ERR_USER_NETWORK,
        ERR_SSL,
        ERR_UNKNOWN
    }

    private final BlogUrlValidator mBlogUrlValidator;
    private final ApiProviderFactory mApiProviderFactory;
    private final CredentialSource mCredentialSource;
    private final CredentialSink mCredentialSink;
    private final HACKListener mHACKListener;

    // object state
    private final Set<Listener> mListeners;
    private Disposable mLoginFlowDisposable = Disposables.empty();
    private String mValidBlogUrl = null;
    private ApiProvider mApiProvider = null;
    private AuthService mAuthService = null;
    private AuthReqBody mCandidateCredentials = null;

    public static LoginOrchestrator create(@NonNull CredentialSource credentialSource,
                                           @NonNull HACKListener hackListener) {
        OkHttpClient httpClient = SpectreApplication.getInstance().getOkHttpClient();
        final CredentialSink credentialSink = new AuthStore();
        return new LoginOrchestrator(new NetworkBlogUrlValidator(httpClient),
                new ProductionApiProviderFactory(httpClient), credentialSource, credentialSink,
                hackListener);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    LoginOrchestrator(BlogUrlValidator blogUrlValidator, ApiProviderFactory apiProviderFactory,
                      CredentialSource credentialSource, CredentialSink credentialSink,
                      HACKListener hackListener) {
        mBlogUrlValidator = blogUrlValidator;
        mApiProviderFactory = apiProviderFactory;
        mCredentialSource = credentialSource;
        mCredentialSink = credentialSink;
        mHACKListener = hackListener;
        mListeners = new HashSet<>();
        reset();
    }

    public void reset() {
        mLoginFlowDisposable.dispose();
        setState(null);
    }

    private void setState(@Nullable String blogUrl) {
        if (blogUrl != null) {
            Timber.i("VALID BLOG URL: " + blogUrl);
            mValidBlogUrl = blogUrl;
            mApiProvider = mApiProviderFactory.create(blogUrl);
            mAuthService = AuthService.createWithGivenCredentials(blogUrl,
                    mApiProvider.getGhostApi(), mCredentialSource);
            // FIXME FIXME FIXME
            mHACKListener.setApiService(blogUrl, mApiProvider.getGhostApi());
        } else {
            mValidBlogUrl = null;
            mApiProvider = null;
            mAuthService = null;
        }
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
        String normalizedBlogUrl = normalizeBlogUrl(inputBlogUrl);
        mLoginFlowDisposable = kickOffLoginFlow(normalizedBlogUrl);
    }

    // the entire login flow is specified here, from start to finish
    private Disposable kickOffLoginFlow(String blogUrl) {
        // READ THIS: https://upday.github.io/blog/subscribe_on/
        return mBlogUrlValidator
                .validate(blogUrl)
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                .doOnNext(this::setState)
                .flatMap(url -> mApiProvider.getGhostApi().getConfiguration())
                .flatMap(config -> this.getAuthToken(mApiProvider.getGhostApi(), config))
                    .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleAuthToken, this::handleError);
    }

    private Observable<AuthToken> getAuthToken(GhostApiService api, ConfigurationList config)
            throws HttpException {
        // NOTE: this could have been part of the main login flow but I wanted the retry() to only
        // apply to this part of the stream, hence a separate observable
        return mAuthService
                .getAuthReqBody(config)
                    .subscribeOn(AndroidSchedulers.mainThread())
                    .observeOn(Schedulers.io())
                .doOnNext(this::setCandidateCredentials)
                .flatMap(api::getAuthToken)
                    .observeOn(AndroidSchedulers.mainThread())
                .doOnError(this::handleError)
                .doOnError(e -> this.unsetCandidateCredentials())
                // if auth fails, ask for credentials again and retry
                // don't retry infinitely, as a safety mechanism for tests
                .retry(20);
    }

    private void handleAuthToken(AuthToken authToken) {
        mCredentialSink.saveCredentials(mValidBlogUrl, mCandidateCredentials);
        // FIXME FIXME FIXME FIXME
        mHACKListener.onNewLogin(mValidBlogUrl, authToken);
        forEachListener(Listener::onLoginDone);
        getBus().post(new LoginDoneEvent(mValidBlogUrl));
    }

    private void handleError(Throwable e) {
        // NOTE this could be null if the error occurred *during* URL validation
        String blogUrl = mValidBlogUrl;

        if (e instanceof NetworkBlogUrlValidator.UrlValidationException) {
            NetworkBlogUrlValidator.UrlValidationException urlEx = ((NetworkBlogUrlValidator.UrlValidationException) e);
            blogUrl = urlEx.getUrl();
            handleUrlValidationError(urlEx.getCause(), blogUrl);
        }

        else if (e instanceof HttpException) {
            final HttpException httpEx = (HttpException) e;
            HttpUrl url = httpEx.response().raw().request().url();
            if (NetworkUtils.isUnauthorized(e)
                    && url.pathSegments().contains("configuration")) {
                // config was not publicly accessible before Ghost 1.x
                AnalyticsService.logGhostV0Error();
                forEachListener(Listener::onGhostV0Error);
            } else if (url.pathSegments().contains("authentication")) {
                ApiErrorList apiErrors = GhostApiUtils.parseApiErrors(
                        mApiProvider.getRetrofit(), httpEx);
                if (apiErrors != null && !apiErrors.errors.isEmpty()) {
                    ApiError apiError = apiErrors.errors.get(0);
                    boolean isEmailError = "NotFoundError".equals(apiError.errorType)
                            || "TooManyRequestsError".equals(apiError.errorType);
                    forEachListener(l -> l.onApiError(apiError.message, isEmailError));
                }
            } else {
                Timber.e(new LoginFailedException(e));
            }
        }

        else {
            Timber.e(new LoginFailedException(e));
            forEachListener(l -> l.onNetworkError(getErrorType(e), e));
        }

        getBus().post(new LoginErrorEvent(blogUrl));
    }

    private void handleUrlValidationError(Throwable error, String blogUrl) {
        ErrorType errorType = getErrorType(error);
        forEachListener(l -> l.onBlogUrlError(errorType, error, blogUrl));
    }



    // helper methods
    private static ErrorType getErrorType(Throwable error) {
        ErrorType errorType = ErrorType.ERR_UNKNOWN;
        if (NetworkUtils.isUserNetworkError(error)) {
            errorType = ErrorType.ERR_USER_NETWORK;
        } else if (NetworkUtils.isConnectionError(error)) {
            errorType = ErrorType.ERR_CONNECTION;
        } else if (NetworkUtils.isSslError(error)) {
            errorType = ErrorType.ERR_SSL;
        }
        return errorType;
    }

    private void forEachListener(Action1<Listener> action) {
        for (Listener listener : mListeners) {
            action.call(listener);
        }
    }

    // this function is in LoginOrchestrator because it is only useful when taking in user input
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    static String normalizeBlogUrl(String inputBlogUrl) {
        return inputBlogUrl.trim().replaceFirst("^(.*)/ghost/?$", "$1");
    }

    private void setCandidateCredentials(AuthReqBody authReqBody) {
        mCandidateCredentials = authReqBody;
    }

    private void unsetCandidateCredentials() {
        mCandidateCredentials = null;
    }


    // FIXME this is basically a giant hack needed while I refactor NetworkService.java
    public interface HACKListener {
        void setApiService(String blogUrl, GhostApiService api);
        void onNewLogin(String blogUrl, AuthToken authToken);
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
        void onBlogUrlError(ErrorType errorType, @NonNull Throwable error,
                            @NonNull String blogUrl);

        /**
         * An error was returned by the Ghost API.
         * @param error - the error message direct from the API
         * @param isEmailError - true iff the error is for the email field
         */
        void onApiError(String error, boolean isEmailError);

        /**
         * The user is using a version of Ghost older than 1.0.
         */
        void onGhostV0Error();

        /**
         * A network-level error occurred.
         * @param errorType - the type of error that occurred
         * @param error - the exact error that occurred
         */
        void onNetworkError(ErrorType errorType, @NonNull Throwable error);

        /**
         * Login completed successfully.
         */
        void onLoginDone();
    }

}

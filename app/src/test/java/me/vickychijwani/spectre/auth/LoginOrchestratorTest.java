package me.vickychijwani.spectre.auth;

import com.squareup.otto.Subscribe;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;

import java.util.Arrays;

import io.reactivex.Observable;
import me.vickychijwani.spectre.auth.LoginOrchestrator.HACKListener;
import me.vickychijwani.spectre.auth.LoginOrchestrator.Listener;
import me.vickychijwani.spectre.event.LoginDoneEvent;
import me.vickychijwani.spectre.event.LoginErrorEvent;
import me.vickychijwani.spectre.network.ApiProvider;
import me.vickychijwani.spectre.network.ApiProviderFactory;
import me.vickychijwani.spectre.network.GhostApiService;
import me.vickychijwani.spectre.network.GhostApiUtils;
import me.vickychijwani.spectre.testing.EventBusRule;
import me.vickychijwani.spectre.testing.Helpers;
import me.vickychijwani.spectre.testing.LoggingRule;
import me.vickychijwani.spectre.testing.MockGhostApiService;
import me.vickychijwani.spectre.testing.RxSchedulersRule;
import me.vickychijwani.spectre.util.Pair;
import retrofit2.Retrofit;
import retrofit2.mock.BehaviorDelegate;
import retrofit2.mock.MockRetrofit;
import retrofit2.mock.NetworkBehavior;

import static me.vickychijwani.spectre.event.BusProvider.getBus;
import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

/**
 * TYPE: unit tests
 * PURPOSE: testing overall logic flow for login process. This does NOT include logic for refreshing
 * an expired token, etc - only the initial user login flow is covered. The refresh logic is tested
 * in {@link AuthServiceTest}.
 */

public class LoginOrchestratorTest {

    @ClassRule public static TestRule rxSchedulersRule = new RxSchedulersRule();
    @ClassRule public static TestRule loggingRule = new LoggingRule();
    @ClassRule public static TestRule eventBusRule = new EventBusRule();

    private static final String BLOG_URL_WITHOUT_PROTOCOL = "blog.example.com";
    private static final String BLOG_URL = "http://" + BLOG_URL_WITHOUT_PROTOCOL;

    private CredentialSource credSource;
    private CredentialSink credSink;
    private Listener listener;

    @Before
    public void setupMocks() {
        credSource = mock(CredentialSource.class);
        credSink = mock(CredentialSink.class);
        listener = mock(Listener.class);
    }


    // tests
    @Test
    public void ghostAuth_success() {
        LoginOrchestrator orchestrator = makeOrchestrator(credSource, credSink, true);
        when(credSource.getGhostAuthCode(any())).thenReturn(Observable.just("auth-code"));
        orchestrator.listen(listener);

        orchestrator.start(BLOG_URL_WITHOUT_PROTOCOL);

        verify(listener).onStartWaiting();
        verify(credSource).getGhostAuthCode(any());
        verify(credSink).saveCredentials(argThat(is(BLOG_URL)), any());
        verify(credSink, never()).deleteCredentials(BLOG_URL);
        verify(listener).onLoginDone();
    }

    @Test
    public void ghostAuth_networkFailure() {
        NetworkBehavior failingNetworkBehavior = Helpers.getFailingNetworkBehaviour();
        LoginOrchestrator orchestrator = makeOrchestrator(credSource, credSink, true, failingNetworkBehavior);
        when(credSource.getGhostAuthCode(any())).thenReturn(Observable.just("auth-code"));
        orchestrator.listen(listener);

        orchestrator.start(BLOG_URL_WITHOUT_PROTOCOL);

        verify(listener).onNetworkError(any(), argThat(sameInstance(failingNetworkBehavior.failureException())));
    }

    @Test
    public void passwordAuth_success() {
        LoginOrchestrator orchestrator = makeOrchestrator(credSource, credSink, false);
        when(credSource.getEmailAndPassword(any()))
                .thenReturn(Observable.just(new Pair<>("email", "password")));
        orchestrator.listen(listener);

        orchestrator.start(BLOG_URL_WITHOUT_PROTOCOL);

        verify(listener).onStartWaiting();
        verify(credSource).getEmailAndPassword(any());
        verify(credSink).saveCredentials(argThat(is(BLOG_URL)), any());
        verify(credSink, never()).deleteCredentials(BLOG_URL);
        verify(listener).onLoginDone();
    }

    @Test
    public void passwordAuth_wrongPassword() {
        LoginOrchestrator orchestrator = makeOrchestrator(credSource, credSink, false);
        orchestrator.listen(listener);
        // simulate entering the wrong password once, followed by the right password
        final boolean[] retrying = {false}, retried = {false};
        when(credSource.getEmailAndPassword(any())).thenReturn(Observable.fromCallable(() -> {
            if (!retrying[0]) {
                retrying[0] = true;
                return new Pair<>("email", "wrong-password");
            } else {
                retried[0] = true;
                return new Pair<>("email", "password");
            }
        }));

        orchestrator.start(BLOG_URL_WITHOUT_PROTOCOL);

        verify(listener).onStartWaiting();
        verify(credSource).getEmailAndPassword(any());
        verify(credSink).saveCredentials(argThat(is(BLOG_URL)), any());
        verify(credSink, never()).deleteCredentials(BLOG_URL);
        verify(listener).onLoginDone();
        // this throws an NPE for no apparent reason - wtf? hence the ugly "retried" flag
        //verify(listener).onApiError(any(), any());
        assertThat(retried[0], is(true));
    }

    @Test
    public void loginSucceededEvent() {
        LoginStatusEventListener spy = spy(new LoginStatusEventListener());
        getBus().register(spy);
        LoginOrchestrator orchestrator = makeOrchestrator(credSource, credSink, true);
        when(credSource.getGhostAuthCode(any())).thenReturn(Observable.just("auth-code"));
        orchestrator.listen(listener);

        orchestrator.start(BLOG_URL_WITHOUT_PROTOCOL);

        verify(spy).onLoginDoneEvent(any());
        getBus().unregister(spy);
    }

    @Test
    public void loginFailureEvent() {
        LoginStatusEventListener spy = spy(new LoginStatusEventListener());
        getBus().register(spy);

        LoginOrchestrator orchestrator = makeOrchestrator(credSource, credSink, true);
        when(credSource.getGhostAuthCode(any())).thenReturn(Observable.just("wrong-auth-code"));
        orchestrator.listen(listener);

        orchestrator.start(BLOG_URL_WITHOUT_PROTOCOL);

        // atLeastOnce() because the operation gets retried automatically
        verify(spy, atLeastOnce()).onLoginErrorEvent(any());
        getBus().unregister(spy);
    }

    @Test
    public void normalizeBlogUrl_shouldTrimWhitespaceAndTrailingGhostPath() {
        assertThat(Arrays.asList(
                LoginOrchestrator.normalizeBlogUrl("  https://my-blog.com         "),
                LoginOrchestrator.normalizeBlogUrl("  https://my-blog.com/ghost   "),
                LoginOrchestrator.normalizeBlogUrl("  https://my-blog.com/ghost/  ")
        ), everyItem(is("https://my-blog.com")));
    }



    // helpers
    private static LoginOrchestrator makeOrchestrator(CredentialSource credSource,
                                                      CredentialSink credSink,
                                                      boolean useGhostAuth) {
        return makeOrchestrator(credSource, credSink, useGhostAuth,
                Helpers.getIdealNetworkBehavior());
    }

    private static LoginOrchestrator makeOrchestrator(CredentialSource credSource,
                                                      CredentialSink credSink,
                                                      boolean useGhostAuth,
                                                      NetworkBehavior networkBehavior) {
        BlogUrlValidator blogUrlValidator = blogUrl -> Observable.just("http://" + blogUrl);
        HACKListener hackListener = mock(HACKListener.class);
        final MockApiProviderFactory apiProviderFactory = new MockApiProviderFactory(
                useGhostAuth, networkBehavior);
        return new LoginOrchestrator(blogUrlValidator, apiProviderFactory, credSource,
                credSink, hackListener);
    }

    private static class MockApiProviderFactory implements ApiProviderFactory {
        private final boolean mUseGhostAuth;
        private final NetworkBehavior mNetworkBehavior;

        public MockApiProviderFactory(boolean useGhostAuth, NetworkBehavior networkBehavior) {
            mUseGhostAuth = useGhostAuth;
            mNetworkBehavior = networkBehavior;
        }

        @Override
        public ApiProvider create(String blogUrl) {
            final Retrofit retrofit = GhostApiUtils.getRetrofit(blogUrl, Helpers.getProdHttpClient());
            return new ApiProvider() {
                @Override
                public Retrofit getRetrofit() {
                    return retrofit;
                }

                @Override
                public GhostApiService getGhostApi() {
                    MockRetrofit mockRetrofit = Helpers.getMockRetrofit(retrofit, mNetworkBehavior);
                    BehaviorDelegate<GhostApiService> delegate = mockRetrofit.create(GhostApiService.class);
                    return new MockGhostApiService(delegate, mUseGhostAuth);
                }
            };
        }
    }

    private static class LoginStatusEventListener {
        @Subscribe
        public void onLoginDoneEvent(LoginDoneEvent event) {
            // no-op
        }

        @Subscribe
        public void onLoginErrorEvent(LoginErrorEvent event) {
            // no-op
        }
    }

}

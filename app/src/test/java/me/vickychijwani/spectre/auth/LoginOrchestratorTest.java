package me.vickychijwani.spectre.auth;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.android.plugins.RxAndroidPlugins;
import io.reactivex.internal.schedulers.TrampolineScheduler;
import io.reactivex.plugins.RxJavaPlugins;
import me.vickychijwani.spectre.auth.LoginOrchestrator.CredentialSource;
import me.vickychijwani.spectre.auth.LoginOrchestrator.HACKListener;
import me.vickychijwani.spectre.auth.LoginOrchestrator.Listener;
import me.vickychijwani.spectre.event.BusProvider;
import me.vickychijwani.spectre.network.ApiProvider;
import me.vickychijwani.spectre.network.ApiProviderFactory;
import me.vickychijwani.spectre.network.GhostApiService;
import me.vickychijwani.spectre.network.GhostApiUtils;
import me.vickychijwani.spectre.testing.Helpers;
import me.vickychijwani.spectre.testing.MockGhostApiService;
import me.vickychijwani.spectre.util.Pair;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.mock.BehaviorDelegate;
import retrofit2.mock.MockRetrofit;
import retrofit2.mock.NetworkBehavior;
import timber.log.Timber;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

/**
 * TYPE: unit tests
 * PURPOSE: testing overall logic flow for login process
 */

public class LoginOrchestratorTest {

    // setup / teardown
    @BeforeClass
    public static void setupBus() {
        BusProvider.setupForTesting();
    }

    @BeforeClass
    public static void setupLogger() {
        Timber.plant(new Timber.Tree() {
            @Override
            protected void log(int priority, String tag, String message, Throwable t) {
                if (message != null) {
                    System.out.println(message);
                }
                if (t != null) {
                    t.printStackTrace();
                }
            }
        });
    }

    @Before
    public void setupRxSchedulers() {
        Scheduler scheduler = TrampolineScheduler.instance();
        RxJavaPlugins.reset();
        RxJavaPlugins.setInitIoSchedulerHandler(__ -> scheduler);
        RxJavaPlugins.setInitNewThreadSchedulerHandler(__ -> scheduler);
        RxAndroidPlugins.reset();
        RxAndroidPlugins.setInitMainThreadSchedulerHandler(__ -> scheduler);
    }

    @After
    public void resetRxSchedulers() {
        RxJavaPlugins.reset();
        RxAndroidPlugins.reset();
    }


    // actual tests
    @Test
    public void ghostAuth() {
        CredentialSource credSource = mock(CredentialSource.class);
        Listener listener = mock(Listener.class);
        LoginOrchestrator orchestrator = makeOrchestrator(credSource, true);
        when(credSource.getGhostAuthCode(any())).thenReturn(Observable.just("auth-code"));
        orchestrator.listen(listener);

        orchestrator.start("blog.com");

        verify(listener).onStartWaiting();
        verify(credSource).getGhostAuthCode(any());
        verify(listener).onLoginDone(argThat(is("http://blog.com")));
    }

    @Test
    public void ghostAuth_networkFailure() {
        CredentialSource credSource = mock(CredentialSource.class);
        NetworkBehavior failingNetworkBehavior = Helpers.getFailingNetworkBehaviour();
        Listener listener = mock(Listener.class);
        LoginOrchestrator orchestrator = makeOrchestrator(credSource, true, failingNetworkBehavior);
        when(credSource.getGhostAuthCode(any())).thenReturn(Observable.just("auth-code"));
        orchestrator.listen(listener);

        orchestrator.start("blog.com");

        verify(listener).onNetworkError(any(), argThat(sameInstance(failingNetworkBehavior.failureException())));
    }

    @Test
    public void passwordAuth() {
        CredentialSource credSource = mock(CredentialSource.class);
        Listener listener = mock(Listener.class);
        LoginOrchestrator orchestrator = makeOrchestrator(credSource, false);
        when(credSource.getEmailAndPassword(any()))
                .thenReturn(Observable.just(new Pair<>("email", "password")));
        orchestrator.listen(listener);

        orchestrator.start("blog.com");

        verify(listener).onStartWaiting();
        verify(credSource).getEmailAndPassword(any());
        verify(listener).onLoginDone(argThat(is("http://blog.com")));
    }

    @Test
    public void passwordAuth_wrongPassword() {
        CredentialSource credSource = mock(CredentialSource.class);
        Listener listener = mock(Listener.class);
        LoginOrchestrator orchestrator = makeOrchestrator(credSource, false);
        orchestrator.listen(listener);
        // simulate entering the wrong password once, followed by the right password
        final boolean[] retrying = {false}, retried = {false};
        when(credSource.getEmailAndPassword(any())).thenReturn(Observable.create(e -> {
            if (!retrying[0]) {
                retrying[0] = true;
                e.onNext(new Pair<>("email", "wrong-password"));
            } else {
                retried[0] = true;
                e.onNext(new Pair<>("email", "password"));
            }
            e.onComplete();
        }));

        orchestrator.start("blog.com");

        verify(listener).onStartWaiting();
        verify(credSource).getEmailAndPassword(any());
        verify(listener).onLoginDone(argThat(is("http://blog.com")));
        // this throws an NPE for no apparent reason - wtf? hence the ugly "retried" flag
        //verify(listener).onApiError(any(), any());
        assertThat(retried[0], is(true));
    }



    // helpers
    private static LoginOrchestrator makeOrchestrator(CredentialSource credSource,
                                                      boolean useGhostAuth) {
        return makeOrchestrator(credSource, useGhostAuth, Helpers.getIdealNetworkBehavior());
    }

    private static LoginOrchestrator makeOrchestrator(CredentialSource credSource,
                                                      boolean useGhostAuth,
                                                      NetworkBehavior networkBehavior) {
        BlogUrlValidator blogUrlValidator = blogUrl -> Observable.just("http://" + blogUrl);
        HACKListener hackListener = mock(HACKListener.class);
        final MockApiProviderFactory apiProviderFactory = new MockApiProviderFactory(
                useGhostAuth, Helpers.getProdHttpClient(), networkBehavior);
        return new LoginOrchestrator(blogUrlValidator, apiProviderFactory, credSource, hackListener);
    }

    private static class MockApiProviderFactory implements ApiProviderFactory {
        private final boolean mUseGhostAuth;
        private final OkHttpClient mHttpClient;
        private final NetworkBehavior mNetworkBehavior;

        public MockApiProviderFactory(boolean useGhostAuth, OkHttpClient httpClient,
                                      NetworkBehavior networkBehavior) {
            mUseGhostAuth = useGhostAuth;
            mHttpClient = httpClient;
            mNetworkBehavior = networkBehavior;
        }

        @Override
        public ApiProvider create(String blogUrl) {
            final Retrofit retrofit = GhostApiUtils.getRetrofit(blogUrl, mHttpClient);
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

}

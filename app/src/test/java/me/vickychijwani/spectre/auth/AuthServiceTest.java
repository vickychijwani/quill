package me.vickychijwani.spectre.auth;

import com.squareup.otto.Subscribe;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;

import io.reactivex.Observable;
import me.vickychijwani.spectre.auth.AuthService.Listener;
import me.vickychijwani.spectre.event.CredentialsExpiredEvent;
import me.vickychijwani.spectre.model.entity.AuthToken;
import me.vickychijwani.spectre.network.GhostApiService;
import me.vickychijwani.spectre.network.GhostApiUtils;
import me.vickychijwani.spectre.testing.EventBusRule;
import me.vickychijwani.spectre.testing.Helpers;
import me.vickychijwani.spectre.testing.LoggingRule;
import me.vickychijwani.spectre.testing.MockGhostApiService;
import me.vickychijwani.spectre.testing.RxSchedulersRule;
import retrofit2.Retrofit;
import retrofit2.mock.BehaviorDelegate;
import retrofit2.mock.MockRetrofit;

import static me.vickychijwani.spectre.event.BusProvider.getBus;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasProperty;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

/**
 * TYPE: unit tests
 * PURPOSE: testing logic for token / credential expiry scenarios
 */

public class AuthServiceTest {

    @ClassRule public static TestRule rxSchedulersRule = new RxSchedulersRule();
    @ClassRule public static TestRule loggingRule = new LoggingRule();
    @ClassRule public static TestRule eventBusRule = new EventBusRule();

    private static final String BLOG_URL = "https://blog.example.com";

    private CredentialSource credSource;
    private CredentialSink credSink;
    private Listener listener;

    @Before
    public void setupMocks() {
        // source must be == sink because of the limitation in AuthService#loginAgain
        AuthStore credSourceAndSink = mock(AuthStore.class);
        credSource = credSourceAndSink;
        credSink = credSourceAndSink;
        listener = mock(Listener.class);
    }


    // tests
    @Test
    public void refreshToken_expiredAccessToken() {
        refreshToken("expired-access-token", "refresh-token", "auth-code");

        verify(credSink).setLoggedIn(BLOG_URL, true);
        verify(listener).onNewAuthToken(argThat(hasProperty("accessToken", is("refreshed-access-token"))));
        verify(listener).onNewAuthToken(argThat(hasProperty("refreshToken", is("refresh-token"))));
    }

    @Test
    public void refreshToken_expiredAccessAndRefreshToken() {
        refreshToken("expired-access-token", "expired-refresh-token", "auth-code");

        verify(credSink).setLoggedIn(BLOG_URL, true);
        verify(listener).onNewAuthToken(argThat(hasProperty("accessToken", is("access-token"))));
        verify(listener).onNewAuthToken(argThat(hasProperty("refreshToken", is("refresh-token"))));
    }

    @Test
    public void refreshToken_expiredTokensAndAuthCode() {
        CredentialsExpiredEventListener spy = spy(new CredentialsExpiredEventListener());
        getBus().register(spy);

        refreshToken("expired-access-token", "expired-refresh-token", "expired-auth-code");

        verify(credSink).deleteCredentials(BLOG_URL);
        verify(spy).onCredentialsExpiredEvent(any());
        getBus().unregister(spy);
    }


    // helpers
    private void refreshToken(String accessToken, String refreshToken, String authCode) {
        Retrofit retrofit = GhostApiUtils.getRetrofit(BLOG_URL, Helpers.getProdHttpClient());
        MockRetrofit mockRetrofit = Helpers.getMockRetrofit(retrofit, Helpers.getIdealNetworkBehavior());
        BehaviorDelegate<GhostApiService> delegate = mockRetrofit.create(GhostApiService.class);
        GhostApiService api = new MockGhostApiService(delegate, true);

        when(credSource.getGhostAuthCode(any())).thenReturn(Observable.just(authCode));

        AuthToken token = new AuthToken();
        token.setAccessToken(accessToken);
        token.setRefreshToken(refreshToken);

        AuthService authService = new AuthService(BLOG_URL, api, credSource, credSink);
        authService.listen(listener);
        authService.refreshToken(token);
    }

    private static class CredentialsExpiredEventListener {
        @Subscribe
        public void onCredentialsExpiredEvent(CredentialsExpiredEvent event) {
            // no-op
        }
    }

}

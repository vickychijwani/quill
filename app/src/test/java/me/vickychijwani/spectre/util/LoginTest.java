package me.vickychijwani.spectre.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

import me.vickychijwani.spectre.error.UrlNotFoundException;
import me.vickychijwani.spectre.network.ProductionHttpClientFactory;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.tls.SslClient;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import rx.functions.Func1;

import static me.vickychijwani.spectre.hamcrest.UrlMatches.urlMatches;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isOneOf;
import static org.junit.Assert.assertThat;

/**
 * PURPOSE: Android-independent unit tests for login-related functionality
 */

public class LoginTest {

    private static final SSLSocketFactory LOCALHOST_SOCKET_FACTORY = SslClient.localhost().socketFactory;
    private static final X509TrustManager LOCALHOST_TRUST_MANAGER = SslClient.localhost().trustManager;

    private MockWebServer server;


    // setup / teardown
    @Before
    public void setupMockServer() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @After
    public void shutdownMockServer() throws IOException {
        server.shutdown();
    }


    // actual tests
    @Test
    public void checkGhostBlog_simpleHttpUrl() throws IOException {
        String blogUrl = "http://" + server.getHostName() + ":" + server.getPort();
        server.enqueue(new MockResponse());
        OkHttpClient httpClient = getProdHttpClient();

        rx.Observable<String> result = NetworkUtils.checkGhostBlog(blogUrl, httpClient);

        assertThat(result.toBlocking().first(), is(blogUrl));
    }

    @Test
    public void checkGhostBlog_404() throws IOException {
        String blogUrl = "http://" + server.getHostName() + ":" + server.getPort() + "/THIS_DOESNT_EXIST";
        server.enqueue(new MockResponse().setResponseCode(404));
        OkHttpClient httpClient = getProdHttpClient();

        rx.Observable<String> result = NetworkUtils.checkGhostBlog(blogUrl, httpClient);

        try {
            result.toBlocking().first();
            // fail the test if no exception is thrown
            assertThat("Test did not throw exception as expected!", false, is(true));
        } catch (Exception e) {
            assertThat(e, instanceOf(UrlNotFoundException.class));
        }
    }

    @Test
    public void checkGhostBlog_httpUrlWithTrailingSlash() throws IOException {
        String blogUrl = "http://" + server.getHostName() + ":" + server.getPort() + "/";
        server.enqueue(new MockResponse());
        OkHttpClient httpClient = getProdHttpClient();

        rx.Observable<String> result = NetworkUtils.checkGhostBlog(blogUrl, httpClient);

        assertThat(result.toBlocking().first(), isOneOf(blogUrl, blogUrl.replaceFirst("/$", "")));
    }

    @Test
    public void checkGhostBlog_simpleHttpsUrl() throws IOException, NoSuchAlgorithmException {
        String blogUrl = "https://" + server.getHostName() + ":" + server.getPort();
        server.useHttps(LOCALHOST_SOCKET_FACTORY, false);
        server.enqueue(new MockResponse());
        OkHttpClient httpClient = getProdHttpClient();

        rx.Observable<String> result = NetworkUtils.checkGhostBlog(blogUrl, httpClient);

        assertThat(result.toBlocking().first(), is(blogUrl));
    }


    @Test
    public void checkGhostBlog_httpToHttpsRedirect() throws IOException, NoSuchAlgorithmException {
        Func1<HttpUrl, HttpUrl> toHttps = (httpUrl) -> httpUrl.newBuilder().scheme("https").build();

        String httpUrl = "http://" + server.getHostName() + ":" + server.getPort();
        String httpsUrl = toHttps.call(HttpUrl.parse(httpUrl)).toString();
        server.useHttps(LOCALHOST_SOCKET_FACTORY, false);
        server.enqueue(new MockResponse());
        OkHttpClient httpClient = getProdHttpClient();
        httpClient = httpClient.newBuilder().addInterceptor(chain -> {
            if (chain.request().isHttps()) {
                throw new IllegalStateException("This test is supposed to make a vanilla HTTP request!");
            }
            // pretend as if the request was redirected and this response is from the redirected URL
            String httpsRequestUrl = toHttps.call(chain.request().url()).toString();
            return new Response.Builder()
                    .protocol(Protocol.HTTP_1_1)
                    .request(chain.request().newBuilder().url(httpsRequestUrl).build())
                    .code(200)
                    .body(ResponseBody.create(MediaType.parse("text/plain"), ""))
                    .build();
        }).build();

        rx.Observable<String> result = NetworkUtils.checkGhostBlog(httpUrl, httpClient);

        assertThat(result.toBlocking().first(), urlMatches(httpsUrl));
    }

    @Test
    public void checkGhostBlog_underSubFolder() throws IOException {
        String blogUrl = "http://" + server.getHostName() + ":" + server.getPort() + "/blog";
        server.enqueue(new MockResponse());
        OkHttpClient httpClient = getProdHttpClient();

        rx.Observable<String> result = NetworkUtils.checkGhostBlog(blogUrl, httpClient);

        assertThat(result.toBlocking().first(), is(blogUrl));
    }

    @Test
    public void checkGhostBlog_underSubDomain() throws IOException {
        String blogUrl = "http://blog." + server.getHostName() + ":" + server.getPort();
        server.enqueue(new MockResponse());
        OkHttpClient httpClient = getProdHttpClient();
        httpClient = httpClient.newBuilder().addInterceptor(chain -> {
            //noinspection CodeBlock2Expr
            return new Response.Builder()
                    .protocol(Protocol.HTTP_1_1)
                    .request(chain.request())
                    .code(200)
                    .body(ResponseBody.create(MediaType.parse("text/plain"), ""))
                    .build();
        }).build();

        rx.Observable<String> result = NetworkUtils.checkGhostBlog(blogUrl, httpClient);

        assertThat(result.toBlocking().first(), is(blogUrl));
    }


    // helpers
    private OkHttpClient getProdHttpClient() {
        OkHttpClient httpClient = new ProductionHttpClientFactory().create(null);

        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        return httpClient.newBuilder()
                .sslSocketFactory(LOCALHOST_SOCKET_FACTORY, LOCALHOST_TRUST_MANAGER)
                .addInterceptor(loggingInterceptor)
                .build();
    }

}

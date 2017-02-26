package me.vickychijwani.spectre.auth;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

import io.reactivex.Single;
import me.vickychijwani.spectre.error.UrlNotFoundException;
import me.vickychijwani.spectre.network.ProductionHttpClientFactory;
import me.vickychijwani.spectre.util.functions.Func1;
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

import static me.vickychijwani.spectre.hamcrest.UrlMatches.urlMatches;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isOneOf;
import static org.junit.Assert.assertThat;

/**
 * TYPE: unit tests (independent of server and android)
 * PURPOSE: testing blog URL validation
 */

public class BlogUrlValidatorTest {

    private static final SSLSocketFactory LOCALHOST_SOCKET_FACTORY = SslClient.localhost().socketFactory;
    private static final X509TrustManager LOCALHOST_TRUST_MANAGER = SslClient.localhost().trustManager;
    private static final String HTTP = "http://";
    private static final String HTTPS = "https://";

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
    public void checkGhostBlog_simpleHttps() throws IOException, NoSuchAlgorithmException {
        String blogUrl = HTTPS + server.getHostName() + ":" + server.getPort();
        server.useHttps(LOCALHOST_SOCKET_FACTORY, false);
        server.enqueue(new MockResponse());
        OkHttpClient httpClient = getProdHttpClient();

        Single<String> result = BlogUrlValidator.checkGhostBlog(blogUrl, httpClient);

        assertThat(result.blockingGet(), is(blogUrl));
    }

    @Test
    public void checkGhostBlog_simpleHttp() throws IOException, NoSuchAlgorithmException {
        String blogUrl = HTTP + server.getHostName() + ":" + server.getPort();
        server.enqueue(new MockResponse());
        OkHttpClient httpClient = getProdHttpClient();

        Single<String> result = BlogUrlValidator.checkGhostBlog(blogUrl, httpClient);

        assertThat(result.blockingGet(), is(blogUrl));
    }

    @Test
    public void checkGhostBlog_404() throws IOException {
        String blogUrl = HTTPS + server.getHostName() + ":" + server.getPort() + "/THIS_DOESNT_EXIST";
        server.useHttps(LOCALHOST_SOCKET_FACTORY, false);
        server.enqueue(new MockResponse().setResponseCode(404));
        OkHttpClient httpClient = getProdHttpClient();

        Single<String> result = BlogUrlValidator.checkGhostBlog(blogUrl, httpClient);

        try {
            result.blockingGet();
            // fail the test if no exception is thrown
            assertThat("Test did not throw exception as expected!", false, is(true));
        } catch (Exception e) {
            assertThat(e, instanceOf(UrlNotFoundException.class));
        }
    }

    @Test
    public void checkGhostBlog_trailingSlash() throws IOException {
        String blogUrl = HTTPS + server.getHostName() + ":" + server.getPort() + "/";
        server.useHttps(LOCALHOST_SOCKET_FACTORY, false);
        server.enqueue(new MockResponse());
        OkHttpClient httpClient = getProdHttpClient();

        Single<String> result = BlogUrlValidator.checkGhostBlog(blogUrl, httpClient);

        assertThat(result.blockingGet(), isOneOf(blogUrl, blogUrl.replaceFirst("/$", "")));
    }

    @Test
    public void checkGhostBlog_httpToHttpsRedirect() throws IOException, NoSuchAlgorithmException {
        Func1<HttpUrl, HttpUrl> toHttps = (httpUrl) -> httpUrl.newBuilder().scheme("https").build();

        String httpUrl = HTTP + server.getHostName() + ":" + server.getPort();
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

        Single<String> result = BlogUrlValidator.checkGhostBlog(httpUrl, httpClient);

        assertThat(result.blockingGet(), urlMatches(httpsUrl));
    }

    @Test
    public void checkGhostBlog_underSubFolder() throws IOException {
        String blogUrl = HTTPS + server.getHostName() + ":" + server.getPort() + "/blog";
        server.useHttps(LOCALHOST_SOCKET_FACTORY, false);
        server.enqueue(new MockResponse());
        OkHttpClient httpClient = getProdHttpClient();

        Single<String> result = BlogUrlValidator.checkGhostBlog(blogUrl, httpClient);

        assertThat(result.blockingGet(), is(blogUrl));
    }

    @Test
    public void checkGhostBlog_underSubDomain() throws IOException {
        String blogUrl = HTTPS + "blog." + server.getHostName() + ":" + server.getPort();
        server.useHttps(LOCALHOST_SOCKET_FACTORY, false);
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

        Single<String> result = BlogUrlValidator.checkGhostBlog(blogUrl, httpClient);

        assertThat(result.blockingGet(), is(blogUrl));
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

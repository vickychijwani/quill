package me.vickychijwani.spectre.auth;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import me.vickychijwani.spectre.error.UrlNotFoundException;
import me.vickychijwani.spectre.testing.Helpers;
import me.vickychijwani.spectre.util.functions.Func1;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static me.vickychijwani.spectre.testing.Helpers.execute;
import static me.vickychijwani.spectre.testing.UrlMatches.urlMatches;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isOneOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * TYPE: unit tests (independent of server and android)
 * PURPOSE: testing blog URL validation
 */

public class NetworkBlogUrlValidatorTest {

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
        server.useHttps(Helpers.LOCALHOST_SOCKET_FACTORY, false);
        server.enqueue(new MockResponse());
        OkHttpClient httpClient = Helpers.getProdHttpClient();

        assertThat(checkGhostBlog(blogUrl, httpClient), is(blogUrl));
    }

    @Test
    public void checkGhostBlog_simpleHttp() throws IOException, NoSuchAlgorithmException {
        String blogUrl = HTTP + server.getHostName() + ":" + server.getPort();
        server.enqueue(new MockResponse());
        OkHttpClient httpClient = Helpers.getProdHttpClient();

        assertThat(checkGhostBlog(blogUrl, httpClient), is(blogUrl));
    }

    @Test
    public void checkGhostBlog_404() throws IOException {
        String blogUrl = HTTPS + server.getHostName() + ":" + server.getPort() + "/THIS_DOESNT_EXIST";
        server.useHttps(Helpers.LOCALHOST_SOCKET_FACTORY, false);
        server.enqueue(new MockResponse().setResponseCode(404));
        OkHttpClient httpClient = Helpers.getProdHttpClient();

        try {
            checkGhostBlog(blogUrl, httpClient);
            fail("Test did not throw exception as expected!");
        } catch (Exception e) {
            assertThat(e, instanceOf(UrlNotFoundException.class));
        }
    }

    @Test
    public void checkGhostBlog_trailingSlash() throws IOException {
        String blogUrl = HTTPS + server.getHostName() + ":" + server.getPort() + "/";
        server.useHttps(Helpers.LOCALHOST_SOCKET_FACTORY, false);
        server.enqueue(new MockResponse());
        OkHttpClient httpClient = Helpers.getProdHttpClient();

        assertThat(checkGhostBlog(blogUrl, httpClient),
                isOneOf(blogUrl, blogUrl.replaceFirst("/$", "")));
    }

    @Test
    public void checkGhostBlog_httpToHttpsRedirect() throws IOException, NoSuchAlgorithmException {
        Func1<HttpUrl, HttpUrl> toHttps = (httpUrl) -> httpUrl.newBuilder().scheme("https").build();

        String httpUrl = HTTP + server.getHostName() + ":" + server.getPort();
        String httpsUrl = toHttps.call(HttpUrl.parse(httpUrl)).toString();
        server.useHttps(Helpers.LOCALHOST_SOCKET_FACTORY, false);
        server.enqueue(new MockResponse());
        OkHttpClient httpClient = Helpers.getProdHttpClient();
        httpClient = httpClient.newBuilder().addInterceptor(chain -> {
            if (chain.request().isHttps()) {
                throw new IllegalStateException("This test is supposed to make a vanilla HTTP request!");
            }
            // pretend as if the request was redirected and this response is from the redirected URL
            String httpsRequestUrl = toHttps.call(chain.request().url()).toString();
            return new Response.Builder()
                    .protocol(Protocol.HTTP_1_1)
                    .request(chain.request().newBuilder().url(httpsRequestUrl).build())
                    .code(200).message("OK")
                    .body(ResponseBody.create(MediaType.parse("text/plain"), ""))
                    .build();
        }).build();

        assertThat(checkGhostBlog(httpUrl, httpClient), urlMatches(httpsUrl));
    }

    @Test
    public void checkGhostBlog_underSubFolder() throws IOException {
        String blogUrl = HTTPS + server.getHostName() + ":" + server.getPort() + "/blog";
        server.useHttps(Helpers.LOCALHOST_SOCKET_FACTORY, false);
        server.enqueue(new MockResponse());
        OkHttpClient httpClient = Helpers.getProdHttpClient();

        assertThat(checkGhostBlog(blogUrl, httpClient), is(blogUrl));
    }

    @Test
    public void checkGhostBlog_underSubDomain() throws IOException {
        String blogUrl = HTTPS + "blog." + server.getHostName() + ":" + server.getPort();
        server.useHttps(Helpers.LOCALHOST_SOCKET_FACTORY, false);
        server.enqueue(new MockResponse());
        OkHttpClient httpClient = Helpers.getProdHttpClient();
        httpClient = httpClient.newBuilder().addInterceptor(chain -> {
            //noinspection CodeBlock2Expr
            return new Response.Builder()
                    .protocol(Protocol.HTTP_1_1)
                    .request(chain.request())
                    .code(200).message("OK")
                    .body(ResponseBody.create(MediaType.parse("text/plain"), ""))
                    .build();
        }).build();

        assertThat(checkGhostBlog(blogUrl, httpClient), is(blogUrl));
    }


    // helper methods
    private static String checkGhostBlog(String blogUrl, OkHttpClient httpClient) {
        return execute(NetworkBlogUrlValidator.checkGhostBlog(blogUrl, httpClient));
    }

}

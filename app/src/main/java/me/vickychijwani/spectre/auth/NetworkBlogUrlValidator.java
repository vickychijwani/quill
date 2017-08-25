package me.vickychijwani.spectre.auth;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;

import io.reactivex.Observable;
import io.reactivex.Single;
import me.vickychijwani.spectre.error.UrlNotFoundException;
import me.vickychijwani.spectre.util.NetworkUtils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static me.vickychijwani.spectre.util.NetworkUtils.networkCall;

class NetworkBlogUrlValidator implements BlogUrlValidator {

    private static final String TAG = "BlogUrlValidator";

    private final OkHttpClient mHttpClient;

    NetworkBlogUrlValidator(@NonNull OkHttpClient httpClient) {
        mHttpClient = httpClient;
    }

    /**
     * @param blogUrl - URL to validate, without http:// or https://
     */
    @Override
    public Observable<String> validate(@NonNull String blogUrl) {
        // try HTTPS and HTTP, in that order
        return Observable.create(source -> {
            checkGhostBlog("https://" + blogUrl, mHttpClient)
                    .onErrorResumeNext(checkGhostBlog("http://" + blogUrl, mHttpClient))
                    .subscribe(source::onNext, e -> {
                        source.onError(new UrlValidationException(e, "https://" + blogUrl));
                    });
        });
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    static Single<String> checkGhostBlog(@NonNull String blogUrl,
                                         @NonNull OkHttpClient client) {
        final String adminPagePath = "/ghost/";
        String adminPageUrl = NetworkUtils.makeAbsoluteUrl(blogUrl, adminPagePath);
        return checkUrl(adminPageUrl, client).flatMap(response -> {
            if (response.isSuccessful()) {
                // the request may have been redirected, most commonly from HTTP => HTTPS
                // so pick up the eventual URL of the blog and use that
                // (even if the user manually entered HTTP - it's certainly a mistake)
                // to get that, chop off the admin page path from the end
                String potentiallyRedirectedUrl = response.request().url().toString();
                String finalBlogUrl = potentiallyRedirectedUrl.replaceFirst(adminPagePath + "?$", "");
                return Single.just(finalBlogUrl);
            } else if (response.code() == HttpURLConnection.HTTP_NOT_FOUND) {
                return Single.error(new UrlNotFoundException(blogUrl));
            } else {
                return Single.error(new RuntimeException("Response code " + response.code()
                        + " on requesting admin page at " + adminPageUrl));
            }
       });
    }

    private static Single<Response> checkUrl(@NonNull String url,
                                             @NonNull OkHttpClient client) {
        try {
            Request request = new Request.Builder()
                    .url(url)
                    .head()     // make a HEAD request because we only want the response code
                    .build();
            return networkCall(client.newCall(request));
        } catch (IllegalArgumentException e) {
            // invalid url (whitespace chars etc)
            return Single.error(new MalformedURLException("Invalid Ghost admin address: " + url));
        }
    }

    public static class UrlValidationException extends RuntimeException {
        private final String url;
        public UrlValidationException(Throwable cause, String url) {
            super("Couldn't validate the url " + url, cause);
            this.url = url;
        }
        public String getUrl() {
            return url;
        }
    }

}

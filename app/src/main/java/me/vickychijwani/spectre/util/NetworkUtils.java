package me.vickychijwani.spectre.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.support.annotation.StringDef;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit.RetrofitError;
import retrofit.client.Response;
import rx.Observable;
import rx.subscriptions.Subscriptions;

public class NetworkUtils {


    @Retention(RetentionPolicy.SOURCE)
    @StringDef({SCHEME_HTTP, SCHEME_HTTPS})
    public @interface Scheme {}

    public static final String SCHEME_HTTP = "http://";
    public static final String SCHEME_HTTPS = "https://";

    /**
     * Check whether there is any network with a usable connection.
     */
    public static boolean isConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public static boolean isRealError(@NonNull RetrofitError retrofitError) {
        Response response = retrofitError.getResponse();
        if (response == null) {
            // consider this an error, to be safer
            return true;
        } else if (response.getStatus() == HttpURLConnection.HTTP_NOT_MODIFIED) {
            // HTTP 304 is not exactly an error
            return false;
        }
        return true;
    }

    public static boolean isUnauthorized(@NonNull RetrofitError retrofitError) {
        Response response = retrofitError.getResponse();
        // Ghost returns 403 Forbidden in some cases, inappropriately
        // see this for what 401 vs 403 should mean: http://stackoverflow.com/a/3297081/504611
        return response != null && (response.getStatus() == HttpURLConnection.HTTP_UNAUTHORIZED
                        || response.getStatus() == HttpURLConnection.HTTP_FORBIDDEN);
    }

    public static String makeAbsoluteUrl(@NonNull String baseUrl, @NonNull String relativePath) {
        // handling for protocol-relative URLs
        // can't remember which scenario actually produces these URLs except maybe the Markdown preview
        if (relativePath.startsWith("//")) {
            relativePath = "http:" + relativePath;
        }

        // maybe relativePath is already absolute
        if (relativePath.startsWith(SCHEME_HTTP) || relativePath.startsWith(SCHEME_HTTPS)) {
            return relativePath;
        }

        boolean baseHasSlash = baseUrl.endsWith("/");
        boolean relHasSlash = relativePath.startsWith("/");
        if (baseHasSlash && relHasSlash) {
            return baseUrl + relativePath.substring(1);
        } else if ((!baseHasSlash && relHasSlash) || (baseHasSlash && !relHasSlash)) {
            return baseUrl + relativePath;
        } else {
            return baseUrl + "/" + relativePath;
        }
    }

    public static Observable<String> checkGhostBlog(@NonNull String blogUrl,
                                                    @NonNull OkHttpClient client) {
        final String adminPagePath = "/ghost/";
        String adminPageUrl = makeAbsoluteUrl(blogUrl, adminPagePath);
        return checkUrl(adminPageUrl, client)
                .flatMap(response -> {
                    // the request may have been redirected, most commonly from HTTP => HTTPS
                    // so pick up the eventual URL of the blog and use that
                    // (even if the user manually entered HTTP - it's certainly a mistake)
                    // to get that, chop off the admin page path from the end
                    String potentiallyRedirectedUrl = response.request().url().toString();
                    String finalBlogUrl = potentiallyRedirectedUrl.replaceFirst(adminPagePath + "?$", "");
                    return Observable.just(finalBlogUrl);
                });
    }

    public static Observable<okhttp3.Response> checkUrl(@NonNull String url,
                                                        @NonNull OkHttpClient client) {
        try {
            Request request = new Request.Builder()
                    .url(url)
                    .head()     // make a HEAD request because we only want the response code
                    .build();
            return networkCall(client.newCall(request));
        } catch (IllegalArgumentException e) {
            // invalid url (whitespace chars etc)
            return Observable.error(new MalformedURLException("Invalid Ghost admin address: " + url));
        }
    }

    public static Observable<okhttp3.Response> networkCall(@NonNull Call call) {
        return Observable.create(subscriber -> {
            // cancel the request when there are no subscribers
            subscriber.add(Subscriptions.create(call::cancel));
            try {
                subscriber.onNext(call.execute());
                subscriber.onCompleted();
            } catch (IOException e) {
                subscriber.onError(e);
            }
        });
    }

}

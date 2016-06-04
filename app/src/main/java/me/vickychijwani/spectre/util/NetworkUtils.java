package me.vickychijwani.spectre.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.support.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.HttpURLConnection;

import retrofit.RetrofitError;
import retrofit.client.Response;

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

}

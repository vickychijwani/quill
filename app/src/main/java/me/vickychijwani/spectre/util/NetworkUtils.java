package me.vickychijwani.spectre.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;

import java.net.HttpURLConnection;

import retrofit.RetrofitError;
import retrofit.client.Response;

public class NetworkUtils {

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

}

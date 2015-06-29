package me.vickychijwani.spectre.util;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

public class AppUtils {

    public static String pathJoin(String basePath, String relativePath) {
        if (relativePath.startsWith("http")) return relativePath;
        return Uri.withAppendedPath(Uri.parse(basePath), relativePath).toString();
    }

    /**
     * Show the soft keyboard.
     * @param activity the current activity
     */
    public static void showKeyboard(@Nullable Activity activity) {
        if (activity == null) return;
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_IMPLICIT_ONLY);
    }

    /**
     * Hide the soft keyboard.
     * @param activity the current activity
     */
    public static void hideKeyboard(@Nullable Activity activity) {
        if (activity == null) return;
        View view = activity.getCurrentFocus();
        if (view != null) {
            hideKeyboard(activity, view.getWindowToken());
        }
    }

    /**
     * Focus the given view and show the soft keyboard.
     * @param activity the current activity
     * @param view the view to focus
     */
    public static void focusAndShowKeyboard(@Nullable Activity activity, @NonNull View view) {
        if (activity == null) return;
        if (view.isFocusable()) {
            view.requestFocus();
        }
        if (view instanceof EditText) {
            showKeyboard(activity);
        }
    }

    /**
     * Clear focus from the current view and hide the soft keyboard.
     * @param activity the current activity
     */
    public static void defocusAndHideKeyboard(@Nullable Activity activity) {
        if (activity == null) return;
        View view = activity.getCurrentFocus();
        if (view != null) {
            view.clearFocus();
            hideKeyboard(activity, view.getWindowToken());
        }
    }

    /**
     * Check whether there is any network with a usable connection.
     */
    public static boolean isNetworkConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }


    // private methods
    private static void hideKeyboard(@Nullable Activity activity, @Nullable IBinder windowToken) {
        if (activity == null) return;
        InputMethodManager inputManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(windowToken, InputMethodManager.HIDE_NOT_ALWAYS);
    }

}

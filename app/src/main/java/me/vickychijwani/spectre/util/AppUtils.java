package me.vickychijwani.spectre.util;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.IBinder;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.crashlytics.android.Crashlytics;

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

    public static int insertTextAtCursorOrEnd(@NonNull EditText editText, @NonNull String textToInsert) {
        Editable editable = editText.getText();
        int editableLen = editable.length();
        int selStart = editText.getSelectionStart(), selEnd = editText.getSelectionEnd();
        int start = (selStart >= 0) ? selStart : editableLen-1;
        int end = (selEnd >= 0) ? selEnd : editableLen-1;
        editable.replace(Math.min(start, end), Math.max(start, end),
                textToInsert, 0, textToInsert.length());
        return Math.min(start, end);
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

    /**
     * Creates an approximated cubic gradient using a multi-stop linear gradient. See
     * https://plus.google.com/+RomanNurik/posts/2QvHVFWrHZf for more details.
     */
    public static Drawable makeCubicGradientScrimDrawable(@ColorInt int baseColor, int numStops,
                                                          int gravity) {
        numStops = Math.max(numStops, 2);

        PaintDrawable paintDrawable = new PaintDrawable();
        paintDrawable.setShape(new RectShape());

        final int[] stopColors = new int[numStops];

        int red = Color.red(baseColor);
        int green = Color.green(baseColor);
        int blue = Color.blue(baseColor);
        int alpha = Color.alpha(baseColor);

        for (int i = 0; i < numStops; i++) {
            float x = i * 1f / (numStops - 1);
            float opacity = Math.max(0, Math.min(1, x * x * x));
            stopColors[i] = Color.argb((int) (alpha * opacity), red, green, blue);
        }

        final float x0, x1, y0, y1;
        switch (gravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
            case Gravity.LEFT:
                x0 = 1; x1 = 0;
                break;
            case Gravity.RIGHT:
                x0 = 0; x1 = 1;
                break;
            default:
                x0 = 0; x1 = 0;
                break;
        }
        switch (gravity & Gravity.VERTICAL_GRAVITY_MASK) {
            case Gravity.TOP:
                y0 = 1; y1 = 0;
                break;
            case Gravity.BOTTOM:
                y0 = 0; y1 = 1;
                break;
            default:
                y0 = 0; y1 = 0;
                break;
        }

        paintDrawable.setShaderFactory(new ShapeDrawable.ShaderFactory() {
            @Override
            public Shader resize(int width, int height) {
                LinearGradient linearGradient = new LinearGradient(
                        width * x0,
                        height * y0,
                        width * x1,
                        height * y1,
                        stopColors, null,
                        Shader.TileMode.CLAMP);
                return linearGradient;
            }
        });

        return paintDrawable;
    }

    /**
     * Return this app's PackageInfo containing info about version code, version name, etc.
     */
    @Nullable
    public static PackageInfo getPackageInfo(@NonNull Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            Crashlytics.logException(e);
            return null;
        }
    }


    // private methods
    private static void hideKeyboard(@Nullable Activity activity, @Nullable IBinder windowToken) {
        if (activity == null) return;
        InputMethodManager inputManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(windowToken, InputMethodManager.HIDE_NOT_ALWAYS);
    }

}

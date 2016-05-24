package me.vickychijwani.spectre.util;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

public class KeyboardUtils {

    public interface OnKeyboardVisibilityChangedListener {
        void onVisibilityChanged(boolean visible);
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

    // courtesy http://stackoverflow.com/a/18992807/504611
    public static void addKeyboardVisibilityChangedListener(
            @NonNull OnKeyboardVisibilityChangedListener listener, @NonNull Activity activity) {
        final View activityRootView = ((ViewGroup) activity.findViewById(android.R.id.content))
                .getChildAt(0);
        ViewTreeObserver viewTreeObserver = activityRootView.getViewTreeObserver();
        viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            private final int ESTIMATED_KEYBOARD_SIZE_DP = 100;
            private final Rect r = new Rect();

            @Override
            public void onGlobalLayout() {
                float estimatedKeyboardHeight = DeviceUtils.dpToPx(ESTIMATED_KEYBOARD_SIZE_DP);
                activityRootView.getWindowVisibleDisplayFrame(r);
                int heightDiff = activityRootView.getRootView().getHeight() - (r.bottom - r.top);
                boolean isVisible = (heightDiff >= estimatedKeyboardHeight);
                listener.onVisibilityChanged(isVisible);
            }
        });
    }

    // private methods
    private static void hideKeyboard(@Nullable Activity activity, @Nullable IBinder windowToken) {
        if (activity == null) return;
        InputMethodManager inputManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(windowToken, InputMethodManager.HIDE_NOT_ALWAYS);
    }

}

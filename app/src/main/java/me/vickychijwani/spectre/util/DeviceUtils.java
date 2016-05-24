package me.vickychijwani.spectre.util;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;

public class DeviceUtils {

    public static int getScreenWidth(@NonNull Context context) {
        return context.getResources().getDisplayMetrics().widthPixels;
    }

    public static float dpToPx(final float dp) {
        return dp * Resources.getSystem().getDisplayMetrics().density;
    }

    public static float pxToDp(final float px) {
        return px / Resources.getSystem().getDisplayMetrics().density;
    }

}

package me.vickychijwani.spectre.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.net.Uri;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;

import me.vickychijwani.spectre.R;
import me.vickychijwani.spectre.view.BaseActivity;

public class AppUtils {

    public static String pathJoin(@NonNull String basePath, @NonNull String relativePath) {
        // handling for protocol-relative URLs
        if (relativePath.startsWith("//")) relativePath = "http:" + relativePath;
        if (relativePath.startsWith("http")) return relativePath;
        return Uri.withAppendedPath(Uri.parse(basePath), relativePath).toString();
    }

    public static float dpToPx(final float dp) {
        return dp * Resources.getSystem().getDisplayMetrics().density;
    }

    public static float pxToDp(final float px) {
        return px / Resources.getSystem().getDisplayMetrics().density;
    }

    public static void emailDeveloper(@NonNull BaseActivity activity) {
        String emailSubject = String.format(activity.getString(R.string.email_subject),
                activity.getString(R.string.app_name));
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:")); // only email apps should handle this
        intent.putExtra(Intent.EXTRA_EMAIL, new String[] { "vickychijwani@gmail.com" });
        intent.putExtra(Intent.EXTRA_SUBJECT, emailSubject);
        if (intent.resolveActivity(activity.getPackageManager()) != null) {
            activity.startActivity(intent);
        } else {
            Toast.makeText(activity, R.string.intent_no_apps, Toast.LENGTH_LONG)
                    .show();
        }
    }

    public static int insertTextAtCursorOrEnd(@NonNull EditTextSelectionState selectionState,
                                              @NonNull String textToInsert) {
        EditText editText = selectionState.getEditText();
        Editable editable = editText.getText();
        int editableLen = editable.length();
        int selStart = selectionState.getSelectionStart();
        int selEnd = selectionState.getSelectionEnd();
        int start = (selStart >= 0) ? selStart : editableLen-1;
        int end = (selEnd >= 0) ? selEnd : editableLen-1;
        editable.replace(Math.min(start, end), Math.max(start, end),
                textToInsert, 0, textToInsert.length());
        return Math.min(start, end);
    }

    /**
     * Creates an approximated cubic gradient using a multi-stop linear gradient. See
     * https://plus.google.com/+RomanNurik/posts/2QvHVFWrHZf for more details.
     */
    @SuppressWarnings("OverlyLongMethod")
    @SuppressLint("RtlHardcoded")
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
                return new LinearGradient(
                        width * x0,
                        height * y0,
                        width * x1,
                        height * y1,
                        stopColors, null,
                        Shader.TileMode.CLAMP);
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

}

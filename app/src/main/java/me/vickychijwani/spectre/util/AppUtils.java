package me.vickychijwani.spectre.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Editable;
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

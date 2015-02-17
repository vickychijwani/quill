package me.vickychijwani.spectre.util;

import android.net.Uri;

public class AppUtils {

    public static String pathJoin(String basePath, String relativePath) {
        return Uri.withAppendedPath(Uri.parse(basePath), relativePath).toString();
    }

}

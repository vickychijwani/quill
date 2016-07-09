package me.vickychijwani.spectre.pref;

import android.content.Context;
import android.support.annotation.NonNull;

/**
 * Utility class to persist user preferences. Do NOT use this for persisting application state, that
 * is managed separately by {@link AppState}.
 */
public class UserPrefs extends Prefs<UserPrefs.Key> {

    private static final String PREFS_FILE_NAME = "user_prefs";
    private static UserPrefs sInstance = null;

    // keys
    public static class Key extends BaseKey {

        public static final Key BLOG_URL = new Key("blog_url", String.class, "");
        public static final Key USERNAME = new Key("username", String.class, "");
        public static final Key PASSWORD = new Key("password", String.class, "");
        public static final Key PERMALINK_FORMAT = new Key("permalink_format", String.class, "/:slug/");

        /* package */ <T> Key(String str, Class<T> type, T defaultValue) {
            super(str, type, defaultValue);
        }

    }

    private UserPrefs(@NonNull Context context) {
        super(context.getApplicationContext(), PREFS_FILE_NAME);
    }

    public static UserPrefs getInstance(@NonNull Context context) {
        if (sInstance == null) {
            sInstance = new UserPrefs(context);
        }

        return sInstance;
    }

}

package me.vickychijwani.spectre.pref;

import android.content.Context;
import android.support.annotation.NonNull;

/**
 * Utility class to persist global app state (like first-run status, etc). Do NOT use this for
 * persisting user preferences, those are managed separately by {@link UserPrefs}.
 */
public class AppState extends Prefs<AppState.Key> {

    private static final String PREFS_FILE_NAME = "app_state";

    // keys
    public static class Key extends BaseKey {

        protected Key(String str, Class type, Object defaultValue) {
            super(str, type, defaultValue);
        }

    }

    AppState(@NonNull Context context) {
        super(context.getApplicationContext(), PREFS_FILE_NAME);
    }

}

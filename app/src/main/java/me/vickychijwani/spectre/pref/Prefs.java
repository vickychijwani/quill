package me.vickychijwani.spectre.pref;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

/**
 * Base class for managing a specific {@link SharedPreferences} file.
 *
 * @param <K> an {@link BaseKey} type representing the set of valid keys for this preference file
 */
abstract class Prefs<K extends BaseKey> {

    private SharedPreferences mPrefs;

    protected Prefs(@NonNull Context context, @NonNull String prefsFileName)
            throws IllegalArgumentException {
        Context appContext = context.getApplicationContext();
        if (appContext != null) {
            mPrefs = appContext.getSharedPreferences(appContext.getPackageName() + prefsFileName,
                    Context.MODE_PRIVATE);
        } else {
            throw new IllegalArgumentException("context.getApplicationContext() returned null");
        }
    }

    protected void checkKey(K key, Class clazz)
            throws IllegalArgumentException {
        if (key.getType() != clazz) {
            throw new IllegalArgumentException("invalid key");
        }
    }

    public final boolean getBoolean(K key) {
        checkKey(key, Boolean.class);
        return mPrefs.getBoolean(key.toString(), (Boolean) key.getDefaultValue());
    }

    public final void setBoolean(K key, boolean value) {
        checkKey(key, Boolean.class);
        mPrefs.edit().putBoolean(key.toString(), value).apply();
    }

    public final int getInteger(K key) {
        checkKey(key, Integer.class);
        return mPrefs.getInt(key.toString(), (Integer) key.getDefaultValue());
    }

    public final void setInteger(K key, int value) {
        checkKey(key, Integer.class);
        mPrefs.edit().putInt(key.toString(), value).apply();
    }

    public final String getString(K key) {
        checkKey(key, String.class);
        return mPrefs.getString(key.toString(), (String) key.getDefaultValue());
    }

    public final void setString(K key, String value) {
        checkKey(key, String.class);
        mPrefs.edit().putString(key.toString(), value).apply();
    }

}

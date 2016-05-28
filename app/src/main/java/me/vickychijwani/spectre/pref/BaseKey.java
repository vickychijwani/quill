package me.vickychijwani.spectre.pref;

abstract class BaseKey {

    private final String mStr;
    private final Class mType;
    private final Object mDefaultValue;

    /* package */ <T> BaseKey(String str, Class<T> type, T defaultValue) {
        mStr = str;
        mType = type;
        mDefaultValue = defaultValue;
    }

    public Object getDefaultValue() { return mDefaultValue; }
    public Class getType() { return mType; }
    public String toString() { return mStr; }

}


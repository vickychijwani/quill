package me.vickychijwani.spectre.pref;

abstract class BaseKey {

    private String mStr;
    private Class mType;
    private Object mDefaultValue;

    protected BaseKey(String str, Class type, Object defaultValue) {
        mStr = str;
        mType = type;
        mDefaultValue = defaultValue;
    }

    public Object getDefaultValue() { return mDefaultValue; }
    public Class getType() { return mType; }
    public String toString() { return mStr; }

}


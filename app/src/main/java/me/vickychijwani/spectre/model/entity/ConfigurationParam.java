package me.vickychijwani.spectre.model.entity;

import io.realm.RealmModel;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.RealmClass;
import io.realm.annotations.Required;

@RealmClass
public class ConfigurationParam implements RealmModel {

    @PrimaryKey
    private String key;

    @Required
    private String value;

    public ConfigurationParam() {}

    public ConfigurationParam(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "Config[" + this.key + " = " + this.value + "]";
    }

}

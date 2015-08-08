package me.vickychijwani.spectre.model;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.RealmClass;

@RealmClass
public class ConfigurationParam extends RealmObject {

    @PrimaryKey
    private String key;
    private String value;

    // NOTE: DO NOT ADD / MODIFY METHODS, SEE https://realm.io/docs/java/#faq
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

}

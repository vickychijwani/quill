package me.vickychijwani.spectre.model.entity;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.RealmClass;
import io.realm.annotations.Required;

@RealmClass
public class ConfigurationParam extends RealmObject {

    @PrimaryKey @Required
    private String key;

    @Required
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

package me.vickychijwani.spectre.model.entity;

import io.realm.RealmModel;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.RealmClass;
import io.realm.annotations.Required;

@RealmClass
public class Setting implements RealmModel {

    @PrimaryKey @Index
    private int id;

    @Required
    private String uuid;

    @Required
    private String key;

    @Required
    private String value;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
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
        return "Setting[" + this.key + " = " + this.value + "]";
    }

}

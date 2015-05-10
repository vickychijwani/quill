package me.vickychijwani.spectre.model;

import android.support.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import io.realm.RealmObject;
import io.realm.annotations.RealmClass;

@RealmClass
public class PendingAction extends RealmObject {

    public static final String EDIT = "pendingaction:edit";

    @StringDef({EDIT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Type {}

    @Type
    private String type;

    @SuppressWarnings("unused")
    public PendingAction() {}

    public PendingAction(@Type String type) {
        this.type = type;
    }

    @Type
    public String getType() {
        return type;
    }

    public void setType(@Type String type) {
        this.type = type;
    }

}

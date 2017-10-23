package me.vickychijwani.spectre.model;

import android.util.Log;

import com.crashlytics.android.Crashlytics;

import io.realm.DynamicRealm;
import io.realm.RealmMigration;
import io.realm.RealmSchema;

public class BlogDBMigration implements RealmMigration {

    private static final String TAG = BlogMetadataDBMigration.class.getSimpleName();

    @Override
    public void migrate(DynamicRealm realm, long oldVersion, long newVersion) {
        RealmSchema schema = realm.getSchema();
        Crashlytics.log(Log.INFO, TAG, "MIGRATING DATABASE from v" + oldVersion + " to v" + newVersion);

        // why < 2? because I forgot to assign a schema version until I wrote this migration, so the
        // oldVersion here will be whatever default is assigned by Realm
        if (oldVersion < 2) {
            if (!schema.get("Post").hasField("customExcerpt")) {
                Crashlytics.log(Log.DEBUG, TAG, "ADDING CUSTOM EXCERPT FIELD TO POST TABLE");
                schema.get("Post").addField("customExcerpt", String.class);
            }
            oldVersion = 2;
        }
    }

}

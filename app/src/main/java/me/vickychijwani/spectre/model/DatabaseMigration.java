package me.vickychijwani.spectre.model;

import android.util.Log;

import com.crashlytics.android.Crashlytics;

import io.realm.DynamicRealm;
import io.realm.RealmMigration;
import io.realm.RealmSchema;

public class DatabaseMigration implements RealmMigration {

    private static final String TAG = DatabaseMigration.class.getSimpleName();

    @Override
    public void migrate(DynamicRealm realm, long oldVersion, long newVersion) {
        RealmSchema schema = realm.getSchema();
        Crashlytics.log(Log.INFO, TAG, "MIGRATING DATABASE from v" + oldVersion + " to v" + newVersion);

        if (oldVersion == 0) {
            schema.get("Post")
                    .setNullable("slug", true)
                    .setNullable("html", true)
                    .setNullable("image", true)
                    .setNullable("createdAt", true)
                    .setNullable("publishedAt", true)
                    .setNullable("metaTitle", true)
                    .setNullable("metaDescription", true);
            schema.get("User")
                    .addIndex("id")
                    .setNullable("image", true)
                    .setNullable("bio", true);
            schema.get("Tag")
                    .setNullable("slug", true)
                    .setNullable("description", true)
                    .setNullable("image", true)
                    .setNullable("metaTitle", true)
                    .setNullable("metaDescription", true)
                    .setNullable("createdAt", true)
                    .setNullable("updatedAt", true);
            schema.get("Setting")
                    .addIndex("id");
            ++oldVersion;
        }
    }

}

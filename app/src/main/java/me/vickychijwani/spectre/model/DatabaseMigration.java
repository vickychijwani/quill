package me.vickychijwani.spectre.model;

import android.util.Log;

import com.crashlytics.android.Crashlytics;

import io.realm.DynamicRealm;
import io.realm.DynamicRealmObject;
import io.realm.RealmMigration;
import io.realm.RealmResults;
import io.realm.RealmSchema;
import me.vickychijwani.spectre.model.entity.Post;

public class DatabaseMigration implements RealmMigration {

    private static final String TAG = DatabaseMigration.class.getSimpleName();

    @Override
    public void migrate(DynamicRealm realm, long oldVersion, long newVersion) {
        RealmSchema schema = realm.getSchema();
        Crashlytics.log(Log.INFO, TAG, "MIGRATING DATABASE from v" + oldVersion + " to v" + newVersion);

        if (oldVersion == 0) {
            if (schema.get("Post").isNullable("slug")) {
                // get rid of null-valued slugs, if any exist
                RealmResults<DynamicRealmObject> postsWithNullSlug = realm
                        .where(Post.class.getSimpleName())
                        .isNull("slug")
                        .findAll();
                Crashlytics.log(Log.DEBUG, TAG, "CONVERTING " + postsWithNullSlug.size() + " SLUGS FROM NULL TO \"\"");
                for (DynamicRealmObject obj : postsWithNullSlug) {
                    obj.setString("slug", "");
                }
                // finally, make the field required
                schema.get("Post").setNullable("slug", false);
            }

            schema.get("Post")
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

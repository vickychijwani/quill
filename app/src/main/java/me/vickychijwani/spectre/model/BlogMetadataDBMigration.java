package me.vickychijwani.spectre.model;

import android.util.Log;

import com.crashlytics.android.Crashlytics;

import io.realm.DynamicRealm;
import io.realm.DynamicRealmObject;
import io.realm.FieldAttribute;
import io.realm.RealmMigration;
import io.realm.RealmResults;
import io.realm.RealmSchema;
import me.vickychijwani.spectre.SpectreApplication;
import me.vickychijwani.spectre.model.entity.ETag;
import me.vickychijwani.spectre.model.entity.Post;
import me.vickychijwani.spectre.pref.AppState;
import me.vickychijwani.spectre.pref.UserPrefs;

public class BlogMetadataDBMigration implements RealmMigration {

    private static final String TAG = BlogMetadataDBMigration.class.getSimpleName();

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

        if (oldVersion == 1) {
            // delete all etags, so the info can be fetched and stored
            // again, with role-based permissions enforced
            RealmResults<DynamicRealmObject> allEtags = realm
                    .where(ETag.class.getSimpleName())
                    .equalTo("type", ETag.TYPE_CURRENT_USER)
                    .or()
                    .equalTo("type", ETag.TYPE_ALL_POSTS)
                    .findAll();
            Crashlytics.log(Log.DEBUG, TAG, "DELETING ALL ETAGS TO REFRESH DATA COMPLETELY");
            allEtags.deleteAllFromRealm();

            if (!schema.contains("Role")) {
                // create the Role table
                Crashlytics.log(Log.DEBUG, TAG, "CREATING ROLE TABLE");
                schema.create("Role")
                        .addField("id", Integer.class, FieldAttribute.PRIMARY_KEY)
                        .addField("uuid", String.class, FieldAttribute.REQUIRED)
                        .addField("name", String.class, FieldAttribute.REQUIRED)
                        .addField("description", String.class, FieldAttribute.REQUIRED);
            }

            if (!schema.get("User").hasField("roles")) {
                Crashlytics.log(Log.DEBUG, TAG, "ADDING ROLES FIELD TO USER TABLE");
                schema.get("User").addRealmListField("roles", schema.get("Role"));
            }
            ++oldVersion;
        }

        if (oldVersion == 2) {
            if (!schema.get("Post").hasField("conflictState")) {
                Crashlytics.log(Log.DEBUG, TAG, "ADDING CONFLICT STATE FIELD TO POST TABLE");
                schema.get("Post").addField("conflictState", String.class, FieldAttribute.REQUIRED);
            }
            ++oldVersion;
        }

        if (oldVersion == 3) {
            // Ghost 1.0 upgrade, drop all data
            Crashlytics.log(Log.WARN, TAG, "DROPPING ALL DATA");
            final SpectreApplication app = SpectreApplication.getInstance();
            app.setOldRealmSchemaVersion(3);

            // clear logged in state
            AppState.getInstance(app).setBoolean(AppState.Key.LOGGED_IN, false);
            UserPrefs.getInstance(app).clear(UserPrefs.Key.EMAIL);
            UserPrefs.getInstance(app).clear(UserPrefs.Key.PASSWORD);
            UserPrefs.getInstance(app).clear(UserPrefs.Key.PERMALINK_FORMAT);

            ++oldVersion;
        }

        // STARTING FROM V4, THE REALM THAT USED TO STORE THE BLOG DATA NOW
        // ONLY STORES THE *METADATA* FOR ALL CONNECTED BLOGS
    }

}

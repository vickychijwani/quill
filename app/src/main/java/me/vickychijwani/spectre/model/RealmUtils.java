package me.vickychijwani.spectre.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import io.realm.DynamicRealmObject;
import io.realm.FieldAttribute;
import io.realm.Realm;
import io.realm.RealmObjectSchema;
import me.vickychijwani.spectre.util.functions.Action3;

public final class RealmUtils {

    private static final String TAG = "RealmUtils";

    public static void executeTransaction(@NonNull Realm realm,
                                               @NonNull RealmTransaction transaction) {
        executeTransaction(realm, r -> {
            transaction.execute(r);
            return null;
        });
    }

    public static <T> T executeTransaction(@NonNull Realm realm,
                                           @NonNull RealmTransactionWithReturn<T> transaction) {
        T retValue;
        realm.beginTransaction();
        try {
            retValue = transaction.execute(realm);
            realm.commitTransaction();
        } catch (Throwable e) {
            if (realm.isInTransaction()) {
                realm.cancelTransaction();
            } else {
                Log.w(TAG, "Could not cancel transaction, not currently in a transaction.");
            }
            throw e;
        }
        return retValue;
    }

    public static final class Migration {

        private static void changeFieldType(RealmObjectSchema objectSchema, String fieldName,
                                     Class newType, @Nullable FieldAttribute attribute,
                                     Action3<DynamicRealmObject, String, String> transformation) {
            String tempFieldName = fieldName + "_temp";
            if (attribute != null) {
                if (attribute == FieldAttribute.PRIMARY_KEY && objectSchema.hasPrimaryKey()) {
                    // remove existing primary key
                    objectSchema.removePrimaryKey();
                }
                objectSchema.addField(tempFieldName, newType, attribute);
            } else {
                objectSchema.addField(tempFieldName, newType);
            }
            objectSchema
                    .transform(obj -> {
                        transformation.call(obj, fieldName, tempFieldName);
                    })
                    .removeField(fieldName)
                    .renameField(tempFieldName, fieldName);
        }

    }



    public interface RealmTransaction {
        void execute(@NonNull Realm realm);
    }

    public interface RealmTransactionWithReturn<T> {
        T execute(@NonNull Realm realm);
    }

}

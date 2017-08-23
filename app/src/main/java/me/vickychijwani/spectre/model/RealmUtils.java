package me.vickychijwani.spectre.model;

import android.support.annotation.NonNull;
import android.util.Log;

import io.realm.Realm;

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



    public interface RealmTransaction {
        void execute(@NonNull Realm realm);
    }

    public interface RealmTransactionWithReturn<T> {
        T execute(@NonNull Realm realm);
    }

}

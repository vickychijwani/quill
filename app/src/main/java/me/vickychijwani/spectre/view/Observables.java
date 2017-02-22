package me.vickychijwani.spectre.view;

import android.content.ContentResolver;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.util.Pair;

import com.crashlytics.android.Crashlytics;

import java.io.IOException;
import java.io.InputStream;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;

public class Observables {

    private static final String TAG = "Observables";

    public static <T> Observable<T> getDialog(ObservableDialogMaker<T> dialogMaker) {
        return Observable.create(emitter -> {
            // confirm save for scheduled and published posts
            final AlertDialog alertDialog = dialogMaker.makeDialog(emitter);

            // dismiss the dialog automatically if this subscriber unsubscribes
            emitter.setCancellable(alertDialog::dismiss);
            alertDialog.show();
        });
    }

    public interface ObservableDialogMaker<T> {
        AlertDialog makeDialog(ObservableEmitter<T> emitter);
    }

    public static Observable<Pair<InputStream, String>> getFileUploadMetadataFromUri(
            @NonNull ContentResolver contentResolver,
            @NonNull Uri fileUri) {
        return Observable.create(emitter -> {
            try {
                Log.d(TAG, "Attempting to read uri: " + fileUri);
                InputStream inputStream = contentResolver.openInputStream(fileUri);
                String mimeType = contentResolver.getType(fileUri);
                emitter.onNext(new Pair<>(inputStream, mimeType));
            } catch (IOException e) {
                emitter.onError(e);
                Crashlytics.logException(new Exception("Failed to open input stream for uri, " +
                        "see previous exception", e));
                Log.e(TAG, Log.getStackTraceString(e));
            } finally {
                emitter.onComplete();
            }
        });
    }

}

package me.vickychijwani.spectre.view;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import me.vickychijwani.spectre.R;
import me.vickychijwani.spectre.util.AppUtils;
import me.vickychijwani.spectre.util.EditTextSelectionState;
import me.vickychijwani.spectre.util.KeyboardUtils;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subscriptions.Subscriptions;

public class Observables {

    private static final String TAG = "Observables";

    public static Observable<String> getImageUrlDialog(@NonNull Activity activity) {
        // ok to pass null here: https://possiblemobile.com/2013/05/layout-inflation-as-intended/
        @SuppressLint("InflateParams")
        final View dialogView = activity.getLayoutInflater().inflate(R.layout.dialog_image_insert,
                null, false);
        final TextView imageUrlView = (TextView) dialogView.findViewById(R.id.image_url);

        // hack for word wrap with "Done" IME action! see http://stackoverflow.com/a/13563946/504611
        imageUrlView.setHorizontallyScrolling(false);
        imageUrlView.setMaxLines(20);

        return Observable.create((subscriber) -> {
            final AlertDialog insertImageDialog = new AlertDialog.Builder(activity)
                    .setTitle(activity.getString(R.string.insert_image))
                    .setView(dialogView)
                    .setCancelable(true)
                    .setPositiveButton(R.string.insert, (dialog, which) -> {
                        subscriber.onNext(imageUrlView.getText().toString());
                        subscriber.onCompleted();
                    })
                    .setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                        dialog.dismiss();
                        subscriber.onCompleted();
                    })
                    .create();
            imageUrlView.setOnEditorActionListener((view, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    insertImageDialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
                    return true;
                }
                return false;
            });
            // dismiss the dialog automatically if this subscriber unsubscribes
            subscriber.add(Subscriptions.create(insertImageDialog::dismiss));
            insertImageDialog.show();
        });
    }

    public static Observable<Bitmap> getBitmapFromUri(@NonNull ContentResolver contentResolver,
                                                      @NonNull Uri imageUri) {
        Observable.OnSubscribe<Bitmap> onSubscribe = (subscriber) -> {
            try {
                Log.d(TAG, "Attempting to decode: " + imageUri.getPath());
                ParcelFileDescriptor parcelFileDescriptor =
                        contentResolver.openFileDescriptor(imageUri, "r");
                Bitmap bitmap = BitmapFactory.decodeFileDescriptor(parcelFileDescriptor.getFileDescriptor());
                parcelFileDescriptor.close();
                subscriber.onNext(bitmap);
            } catch (IOException e) {
                subscriber.onError(e);
                Crashlytics.logException(e);
                Log.e(TAG, Log.getStackTraceString(e));
            } finally {
                subscriber.onCompleted();
            }
        };
        return Observable.create(onSubscribe);
    }

    public static class Actions {

        public static Action1<String> insertImageMarkdown(@NonNull Activity activity,
                                                          EditTextSelectionState selectionState) {
            return (url) -> {
                String lhs = "\n\n![", rhs = "](" + url + ")\n\n";
                EditText editText = selectionState.getEditText();
                if (selectionState.isFocused()) {
                    int start = AppUtils.insertTextAtCursorOrEnd(selectionState, lhs + rhs);
                    // position the cursor between the square brackets: ![|](http://...)
                    editText.setSelection(start + lhs.length());
                } else {
                    editText.getText().append(lhs).append(rhs);
                    // position the cursor between the square brackets: ![|](http://...)
                    editText.setSelection(editText.getText().length() - rhs.length());
                }
                KeyboardUtils.focusAndShowKeyboard(activity, editText);
            };
        }

    }

    public static class Funcs {

        public static Func1<Bitmap, String> copyBitmapToJpegFile() {
            return (bitmap) -> {
                try {
                    File uploadFile = File.createTempFile("upload", ".jpeg");
                    OutputStream os = new BufferedOutputStream(new FileOutputStream(uploadFile));
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 75, os);
                    os.close();
                    return uploadFile.getAbsolutePath();
                } catch (IOException e) {
                    throw new RuntimeException(e);  // propagate checked exception to onError
                }
            };
        }

    }

}

package me.vickychijwani.spectre.view;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import me.vickychijwani.spectre.R;
import rx.Observable;
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

}

package me.vickychijwani.spectre.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

public class EditTextActionModeManager implements View.OnFocusChangeListener,
        TextView.OnEditorActionListener {

    private enum ActionMode { STARTING, STARTED, STOPPING, STOPPED }

    private ActionMode mActionMode;
    private EditText mActiveEditText = null;
    private final Callbacks mCallbacks;

    private final int mTransparentColor;

    public interface Callbacks {
        void onActionModeStarted(EditText editText);
        void onActionModeStopped(boolean saveChanges);
    }

    public EditTextActionModeManager(@NonNull Context context, @NonNull Callbacks callbacks) {
        mCallbacks = callbacks;
        mTransparentColor = context.getResources().getColor(android.R.color.transparent);
    }

    public void register(@NonNull EditText editText) {
        editText.setBackgroundColor(mTransparentColor);
        editText.setOnFocusChangeListener(this);
        editText.setOnEditorActionListener(this);
    }

    public void unregister(@NonNull EditText editText) {
        editText.setOnFocusChangeListener(null);
        editText.setOnEditorActionListener(null);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void startActionMode(@NonNull EditText editText) {
        if (mActiveEditText != null) {
            stopActionMode(false);
        }
        mActionMode = ActionMode.STARTING;
        mActiveEditText = editText;
        mActionMode = ActionMode.STARTED;
        mCallbacks.onActionModeStarted(mActiveEditText);
    }

    public boolean stopActionMode(boolean discardChanges) {
        if (mActionMode != ActionMode.STARTED) {
            return false;
        }
        mActionMode = ActionMode.STOPPING;
        mActiveEditText.clearFocus();
        mActionMode = ActionMode.STOPPED;
        mCallbacks.onActionModeStopped(discardChanges);
        return true;
    }


    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            stopActionMode(false);
            return true;
        }
        return false;
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus) {
            startActionMode((EditText) v);
        } else {
            stopActionMode(false);
        }
    }

    public boolean isActionModeActive() {
        return mActionMode == ActionMode.STARTING ||
                mActionMode == ActionMode.STARTED;
    }

}

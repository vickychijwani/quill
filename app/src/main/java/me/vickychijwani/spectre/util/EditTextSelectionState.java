package me.vickychijwani.spectre.util;

import android.support.annotation.NonNull;
import android.widget.EditText;

/**
 * Captures the focus / selection state of an {@link android.widget.EditText}.
 */
public class EditTextSelectionState {

    private final EditText mEditText;
    private final boolean mFocused;
    private final int mSelectionStart;
    private final int mSelectionEnd;

    public EditTextSelectionState(@NonNull EditText editText) {
        mEditText = editText;
        mFocused = editText.hasFocus();
        mSelectionStart = editText.getSelectionStart();
        mSelectionEnd = editText.getSelectionEnd();
    }

    public EditText getEditText() {
        return mEditText;
    }

    public boolean isFocused() {
        return mFocused;
    }

    public int getSelectionStart() {
        return mSelectionStart;
    }

    public int getSelectionEnd() {
        return mSelectionEnd;
    }

}

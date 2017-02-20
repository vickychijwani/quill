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
        int selectionStart = editText.getSelectionStart();
        int selectionEnd = editText.getSelectionEnd();
        if (selectionStart > selectionEnd && selectionStart != -1 && selectionEnd != -1) {
            mSelectionStart = selectionEnd;
            mSelectionEnd = selectionStart;
        } else {
            mSelectionStart = selectionStart;
            mSelectionEnd = selectionEnd;
        }
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

    /**
     * Focus the associated EditText and restore its selection state.
     */
    public void focusAndRestoreSelectionState() {
        EditText focusedEditView = this.getEditText();
        int start = this.getSelectionStart();
        int end = this.getSelectionEnd();
        final int len = focusedEditView.getText().length();
        focusedEditView.requestFocus();
        // cursor pos is == length, when it's at the very end
        if (start <= end && start >= 0 && start <= len && end >= 0 && end <= len) {
            focusedEditView.setSelection(start, end);
        } else if (end >= 0 && end <= len) {
            focusedEditView.setSelection(end);
        }
    }

}

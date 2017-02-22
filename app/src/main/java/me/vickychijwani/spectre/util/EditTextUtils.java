package me.vickychijwani.spectre.util;

import android.support.annotation.NonNull;
import android.text.Editable;
import android.util.Patterns;
import android.widget.EditText;

import me.vickychijwani.spectre.util.functions.Action1;

public final class EditTextUtils {

//    public static void insertMarkdownHeadingMarkers(@NonNull EditTextSelectionState selectionState) {
//        String marker = "#";
//        Editable editableText = selectionState.getEditText().getEditableText();
//        if (hasSelection(selectionState)) {
//            // insert at beginning of current line
//            int currentPos = editableText.length() - 1;
//            // let currentPos go to zero (inclusive) because we're going to insert the marker *after* it
//            while (editableText.charAt(currentPos) != '\n' && currentPos >= 0) {
//                --currentPos;
//            }
//            editableText.insert(currentPos+1, marker);
//        } else if (selectionState.isFocused()) {
//            // insert after cursor, after some newlines
//            int desiredNewlinesAroundMarker = 2;
//            int newlinesBeforeMarker = 0;
//            int newlinesAfterMarker = 0;
//            int cursorPos = selectionState.getEditText().getSelectionStart();
//            int currentPos = cursorPos - 1;
//            while (currentPos >= 0 && editableText.charAt(currentPos) == '\n'
//                    && newlinesBeforeMarker < desiredNewlinesAroundMarker) {
//                --currentPos;
//                ++newlinesBeforeMarker;
//            }
//            currentPos = cursorPos;
//            while (currentPos < editableText.length() && editableText.charAt(currentPos) == '\n'
//                    && newlinesAfterMarker < desiredNewlinesAroundMarker) {
//                ++currentPos;
//                ++newlinesAfterMarker;
//            }
//            String newlineStrBefore = makeMultiNewlineString(desiredNewlinesAroundMarker-newlinesBeforeMarker);
//            String newlineStrAfter = makeMultiNewlineString(desiredNewlinesAroundMarker-newlinesAfterMarker);
//            insertAtCursor(selectionState, newlineStrBefore + marker + newlineStrAfter);
//        } else {
//            // insert at end, after some newlines
//            int desiredNewlinesBeforeMarker = 2;
//            int existingNewlines = 0;
//            int currentPos = editableText.length() - 1;
//            while (currentPos >= 0 && editableText.charAt(currentPos) == '\n'
//                    && existingNewlines < desiredNewlinesBeforeMarker) {
//                --currentPos;
//                ++existingNewlines;
//            }
//            String newlineStrBefore = makeMultiNewlineString(desiredNewlinesBeforeMarker-existingNewlines);
//            insertAtEnd(selectionState, newlineStrBefore + marker);
//        }
//    }

    public static void insertMarkdownBoldMarkers(@NonNull EditTextSelectionState selectionState) {
        insertFormattingMarkers("**", "**", selectionState);
    }

    public static void insertMarkdownItalicMarkers(@NonNull EditTextSelectionState selectionState) {
        insertFormattingMarkers("*", "*", selectionState);
    }

    public static void insertMarkdownLinkMarkers(@NonNull EditTextSelectionState selectionState) {
        String left = "[", middle = "](", right = ")";

        if (hasSelection(selectionState)) {
            transformSelection(selectionState, (selectedText) -> {
                if (Patterns.WEB_URL.matcher(selectedText).matches()) {
                    // we have the URL part, so put the cursor in the text part: [|](URL)
                    int insertPos = surroundSelection(selectionState, left + middle, right);
                    moveCursorTo(selectionState, insertPos + left.length());
                } else {
                    // we have the link text part, so put the cursor in the URL part: [TEXT](|)
                    int insertPos = surroundSelection(selectionState, left, middle + right);
                    moveCursorTo(selectionState, insertPos + left.length() + selectedText.length() + middle.length());
                }
            });
        } else {
            int insertPos = insertAtCursorOrEnd(selectionState, left + middle + right);
            // position cursor after left: LEFT|MIDDLE RIGHT
            moveCursorTo(selectionState, insertPos + left.length());
        }
    }

    public static void insertMarkdownImageMarkers(@NonNull String imageUrl,
                                                  @NonNull EditTextSelectionState selectionState) {
        String left = "\n\n![", right = "](" + imageUrl + ")\n\n";
        int insertPos = insertAtCursorOrEnd(selectionState, left + right);
        // position cursor after left: LEFT|RIGHT
        moveCursorTo(selectionState, insertPos + left.length());
    }



    // private methods
    private static void insertFormattingMarkers(String left, String right,
                                                EditTextSelectionState selectionState) {
        if (hasSelection(selectionState)) {
            surroundSelection(selectionState, left, right);
        } else {
            int insertPos = insertAtCursorOrEnd(selectionState, left + right);
            // position cursor after left: LEFT|RIGHT
            moveCursorTo(selectionState, insertPos + left.length());
        }
    }

    private static void moveCursorTo(EditTextSelectionState selectionState, int newCursorPos) {
        selectionState.getEditText().setSelection(newCursorPos);
    }

    private static boolean hasSelection(EditTextSelectionState selectionState) {
        int selStart = selectionState.getSelectionStart();
        int selEnd = selectionState.getSelectionEnd();
        return (selStart < selEnd && selStart != -1 && selEnd != -1);
    }

    private static int surroundSelection(EditTextSelectionState selectionState,
                                         CharSequence prefix, CharSequence suffix) {
        return transformSelection(selectionState, (selectedText) -> {
            insertAtCursor(selectionState, prefix.toString() + selectedText + suffix);
        });
    }

    private static int transformSelection(EditTextSelectionState selectionState,
                                          Action1<CharSequence> transformation) {
        int selStart = selectionState.getSelectionStart();
        int selEnd = selectionState.getSelectionEnd();
        CharSequence selectedText = selectionState.getEditText().getText().subSequence(selStart, selEnd);
        transformation.call(selectedText);
        return selStart;
    }

    private static int insertAtCursorOrEnd(EditTextSelectionState selectionState,
                                           CharSequence textToInsert) {
        if (selectionState.isFocused()) {
            // insert at cursor
            return insertAtCursor(selectionState, textToInsert);
        } else {
            // insert at end
            return insertAtEnd(selectionState, textToInsert);
        }
    }

    private static int insertAtEnd(EditTextSelectionState selectionState,
                                   CharSequence textToInsert) {
        Editable editableText = selectionState.getEditText().getEditableText();
        int insertPos = editableText.length();
        editableText.append(textToInsert);
        return insertPos;
    }

    private static int insertAtCursor(EditTextSelectionState selectionState,
                                      CharSequence textToInsert) {
        EditText editText = selectionState.getEditText();
        Editable editable = editText.getText();
        int editableLen = editable.length();
        int selStart = selectionState.getSelectionStart();
        int selEnd = selectionState.getSelectionEnd();
        int start = (selStart >= 0) ? selStart : editableLen-1;
        int end = (selEnd >= 0) ? selEnd : editableLen-1;
        editable.replace(Math.min(start, end), Math.max(start, end),
                textToInsert, 0, textToInsert.length());
        return Math.min(start, end);
    }

    private static String makeMultiNewlineString(int newlineCount) {
        StringBuilder sb = new StringBuilder(newlineCount);
        for (int i = 0; i < newlineCount; ++i) {
            sb.append('\n');
        }
        return sb.toString();
    }

}

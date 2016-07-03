package me.vickychijwani.spectre.view.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.ColorInt;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ReplacementSpan;
import android.util.AttributeSet;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.MultiAutoCompleteTextView;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.vickychijwani.spectre.util.DeviceUtils;

/**
 * Adapted from https://github.com/kpbird/chipsedittext/blob/master/src/com/kpbird/chipsedittext/ChipsMultiAutoCompleteTextview.java
 */
public class ChipsEditText extends MultiAutoCompleteTextView implements AdapterView.OnItemClickListener {

    private Pattern mTokenPattern;
    @ColorInt private int mChipBgColor;
    @ColorInt private int mChipTextColor;

    public ChipsEditText(Context context) {
        super(context);
        init();
    }

    public ChipsEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ChipsEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        mTokenPattern = Pattern.compile("[^,]+");
        setOnItemClickListener(this);
        addTextChangedListener(mTextWatcher);

        // regenerate chips when user taps "Done" action on the keyboard
        setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                updateChips();
                // don't consume the event, so the keyboard can also be hidden
                // http://stackoverflow.com/questions/2342620/how-to-hide-keyboard-after-typing-in-edittext-in-android#comment20849208_10184099
                return false;
            }
            return false;
        });
    }

    public void setChipBackgroundColor(@ColorInt int bgColor) {
        mChipBgColor = bgColor;
    }

    public void setChipTextColor(@ColorInt int textColor) {
        mChipTextColor = textColor;
    }

    // regenerate chips if the user types any string followed by a space
    private final TextWatcher mTextWatcher = new TextWatcher() {
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (count >= 1 && s.charAt(start) == ',') {
                updateChips();
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void afterTextChanged(Editable s) {}
    };

    private void updateChips() {
        List<String> tokens = getTokens();
        String sanitizedText = joinStrings(tokens, ",");
        SpannableStringBuilder ssb = new SpannableStringBuilder(sanitizedText);
        Matcher matcher = mTokenPattern.matcher(sanitizedText);
        while (matcher.find()) {
            // SPAN_EXCLUSIVE_EXCLUSIVE spans cannot have a zero length
            if (matcher.end() > matcher.start()) {
                ssb.setSpan(new RoundedBackgroundSpan(mChipBgColor, mChipTextColor), matcher.start(),
                        matcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        setText(ssb);
        setSelection(getText().length());   // move cursor to end
    }

    public List<String> getTokens() {
        List<String> tokens = new ArrayList<>();
        String fullText = getText().toString().trim();
        Matcher matcher = mTokenPattern.matcher(fullText);
        while (matcher.find()) {
            String token = matcher.group().trim();
            if (token.length() > 0) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    public void setTokens(List<String> tokens) {
        setText(joinStrings(tokens, ","));
        updateChips();
    }

    private static String joinStrings(List<String> strs, String delimiter) {
        StringBuilder joined = new StringBuilder();
        int numStrs = strs.size();
        for (int i = 0; i < numStrs; ++i) {
            String str = strs.get(i);
            // intentionally appending to end of string so the user can type after it
            joined.append(str).append(delimiter);
        }
        return joined.toString();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // regenerate chips when user selects any item from auto complete dropdown
        updateChips();
    }



    // courtesy http://stackoverflow.com/a/29442039/504611
    public static class RoundedBackgroundSpan extends ReplacementSpan {
//        private static final String TALLEST_STRING = "Pyfgl";

        private final float mCornerRadius = DeviceUtils.dpToPx(7);
//        private final int mTopPadding = (int) AppUtils.dpToPx(0);
//        private final int mBottomPadding = (int) AppUtils.dpToPx(0);
        private final int mLeftPadding = (int) DeviceUtils.dpToPx(5);
        private final int mRightPadding = (int) DeviceUtils.dpToPx(5);

//        private final int mTopMargin = (int) AppUtils.dpToPx(0);
//        private final int mLeftMargin = (int) AppUtils.dpToPx(0);

        private final int mBackgroundColor;
        private final int mTextColor;

        public RoundedBackgroundSpan(int backgroundColor, int textColor) {
            super();
            mBackgroundColor = backgroundColor;
            mTextColor = textColor;
        }

        @Override
        public int getSize(Paint paint, CharSequence text, int start, int end,
                           Paint.FontMetricsInt fm) {
            return (int) (mLeftPadding + paint.measureText(text, start, end) + mRightPadding);
            // this is supposed to work but isn't
            // the span keeps growing in height as each character is typed / deleted (!)
//            if (fm != null) {
//                // add extra vertical space for the padding
//                // descent is +ve, represents depth below baseline
//                // ascent is -ve, represents height above baseline
//                Rect textBounds = new Rect();
//                paint.getTextBounds(TALLEST_STRING, 0, TALLEST_STRING.length(), textBounds);
//                int totalHeight = mTopPadding + textBounds.height() + mBottomPadding;
//                int need = totalHeight - (fm.descent - fm.ascent);
//                Log.e(TAG, "[before] totalHeight: " + totalHeight + ", need: " + need);
//                Log.e(TAG, "[before] fm: " + fm.toString());
//                if (need + mTopMargin > 0) {
//                    int ascent = need / 2;
//                    fm.descent += need - ascent;
//                    fm.ascent -= ascent;
//                    fm.bottom += need - ascent;
//                    fm.top -= mTopMargin + ascent;
//                }
//                Log.e(TAG, "[after ] fm: " + fm.toString());
//            }
//            return (int) (mLeftMargin + mLeftPadding
//                    + paint.measureText(text.subSequence(start, end).toString()) + mRightPadding);
        }

        @Override
        public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top,
                         int y, int bottom, Paint paint) {
//            canvas.translate(mLeftMargin, mTopMargin);
            float width = paint.measureText(text.subSequence(start, end).toString());
            float newBottom = bottom - 2;   // magic number to make sure consecutive lines don't touch
            RectF rect = new RectF(x, top, x + mLeftPadding + width + mRightPadding, newBottom);
            paint.setColor(mBackgroundColor);
            canvas.drawRoundRect(rect, mCornerRadius, mCornerRadius, paint);
            paint.setColor(mTextColor);
            canvas.drawText(text, start, end, x + mLeftPadding, y, paint);
        }
    }



    public static class CommaTokenizer implements Tokenizer {
        public int findTokenStart(CharSequence text, int cursor) {
            int i = cursor;
            while (i > 0 && text.charAt(i-1) != ',') {
                i--;
            }
            // ignore spaces after comma, or at the beginning of text
            int len = text.length();
            while (i < len && text.charAt(i) == ' ') {
                i++;
            }
            return i;
        }

        public int findTokenEnd(CharSequence text, int cursor) {
            int i = cursor;
            int len = text.length();
            while (i < len) {
                if (text.charAt(i) == ',') {
                    // ignore spaces before comma and after cursor
                    while (i > cursor && text.charAt(i-1) == ' ') {
                        --i;
                    }
                    return i;
                } else {
                    i++;
                }
            }
            return len;
        }

        public CharSequence terminateToken(CharSequence text) {
            int i = text.length();
            int lastNonSpaceIdx = i-1;
            while (lastNonSpaceIdx >= 0 && text.charAt(lastNonSpaceIdx) == ' ') {
                --lastNonSpaceIdx;
            }
            if (lastNonSpaceIdx >= 0 && text.charAt(lastNonSpaceIdx-1) == ',') {
                return text;
            } else if (text instanceof Spanned) {
                SpannableString sp = new SpannableString(text + ",");
                TextUtils.copySpansFrom((Spanned) text, 0, text.length(), Object.class, sp, 0);
                return sp;
            } else {
                return text + ",";
            }
        }
    }

}

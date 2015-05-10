package me.vickychijwani.spectre.view;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.tokenautocomplete.TokenCompleteTextView;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import me.vickychijwani.spectre.R;
import me.vickychijwani.spectre.model.Tag;

public class TagsEditText extends TokenCompleteTextView {

    /**
     * Start showing suggestions when these many characters have been entered
     */
    private static final int SUGGESTION_THRESHOLD = 1;

    /**
     * Allow a maximum of these many tags in the text field
     */
    private static final int MAX_TAGS = 10;

    /**
     * Treat these characters as tag delimiters
     */
    private static final char[] TAG_DELIMS = new char[]{ ',', ' ', ';' };

    private LayoutInflater mLayoutInflater;

    public TagsEditText(Context context) {
        super(context);
        init(context);
    }

    public TagsEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public TagsEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(@NonNull Context context) {
        mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        performBestGuess(false);   // allow free text entry
        setSplitChar(TAG_DELIMS);
        setThreshold(SUGGESTION_THRESHOLD);
        setTokenLimit(MAX_TAGS);

        setTokenClickStyle(TokenClickStyle.Select);
        setDeletionStyle(TokenDeleteStyle.Clear);
    }

    /**
     * A token view for the object.
     *
     * @param obj the object selected by the user from the list
     * @return a view to display a token in the text field for the object
     */
    @Override
    protected View getViewForObject(Object obj) {
        Tag tag = (Tag) obj;
        View view = mLayoutInflater.inflate(R.layout.tag_item, (ViewGroup) getParent(), false);
        ((TextView) view.findViewById(R.id.tag_name)).setText(tag.getName());
        return view;
    }

    /**
     * Provides a default completion when the user hits "," and there is no item in the completion
     * list.
     *
     * @param completionText the current text we are completing against
     * @return a best guess for what the user meant to complete
     */
    @Override
    protected Object defaultObject(String completionText) {
        return new Tag(completionText);
    }

    @Override
    protected ArrayList<Serializable> getSerializableObjects() {
        List<Object> objs = getObjects();
        ArrayList<Serializable> serialized = new ArrayList<>(objs.size());
        for (Object obj : objs) {
            serialized.add(((Tag) obj).getName());
        }
        return serialized;
    }

    @Override
    protected ArrayList<Object> convertSerializableArrayToObjectArray(ArrayList<Serializable> sers) {
        ArrayList<Object> objs = new ArrayList<>(sers.size());
        for (Serializable s : sers) {
            objs.add(new Tag((String) s));
        }
        return objs;
    }

}

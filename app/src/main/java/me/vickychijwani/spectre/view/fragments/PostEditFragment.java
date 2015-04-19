package me.vickychijwani.spectre.view.fragments;

import android.app.Activity;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.github.ksoichiro.android.observablescrollview.ObservableScrollView;
import com.github.ksoichiro.android.observablescrollview.ObservableScrollViewCallbacks;
import com.github.ksoichiro.android.observablescrollview.ScrollState;
import com.melnykov.fab.FloatingActionButton;
import com.squareup.otto.Subscribe;

import butterknife.ButterKnife;
import butterknife.InjectView;
import me.vickychijwani.spectre.R;
import me.vickychijwani.spectre.event.PostSavedEvent;
import me.vickychijwani.spectre.event.SavePostEvent;
import me.vickychijwani.spectre.model.Post;
import me.vickychijwani.spectre.util.AppUtils;
import me.vickychijwani.spectre.view.PostViewActivity;

public class PostEditFragment extends BaseFragment implements
        ObservableScrollViewCallbacks {

    private static final String TAG = "PostEditFragment";

    @InjectView(R.id.post_header)
    View mPostHeader;

    @InjectView(R.id.post_title_edit)
    EditText mPostTitleEditView;

    @InjectView(R.id.post_markdown)
    EditText mPostEditView;

    @InjectView(R.id.preview_btn)
    FloatingActionButton mPreviewBtn;

    @InjectView(R.id.post_markdown_scroll_view)
    ObservableScrollView mScrollView;

    private OnPreviewClickListener mPreviewClickListener;
    private Post mPost;

    // action mode
    private enum ActionModeState {
        STARTING, STARTED, STOPPING, STOPPED
    }
    private ActionModeState mActionModeState = ActionModeState.STOPPED;
    private Drawable mEditTextDefaultBackground;
    private int mTransparentColor;
    private View.OnClickListener mActionModeCloseClickListener;
    private PostViewActivity mActivity;

    // scroll behaviour
    private int mActionBarSize;
    private boolean mPostHeaderCollapsed = false;


    public interface OnPreviewClickListener {
        public void onPreviewClicked();
    }


    public static PostEditFragment newInstance() {
        return new PostEditFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_post_edit, container, false);
        ButterKnife.inject(this, view);

        mActivity = ((PostViewActivity) getActivity());
        mPost = mActivity.getPost();

        mActivity.setTitle(null);
        mPostTitleEditView.setText(mPost.getTitle());

        mActionModeCloseClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPostTitleEditView.setText(mPost.getTitle());
                stopActionMode(true);
            }
        };

        mEditTextDefaultBackground = mPostTitleEditView.getBackground();
        mTransparentColor = mActivity.getResources().getColor(android.R.color.transparent);
        mPostTitleEditView.setBackgroundColor(mTransparentColor);
        mPostTitleEditView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (mActionModeState == ActionModeState.STOPPED) {
                    startActionMode();
                } else if (mActionModeState == ActionModeState.STARTED) {
                    stopActionMode(true);
                }
            }
        });

        // hack for word wrap with "Done" IME action! see http://stackoverflow.com/a/13563946/504611
        mPostTitleEditView.setHorizontallyScrolling(false);
        mPostTitleEditView.setMaxLines(Integer.MAX_VALUE);
        mPostTitleEditView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    mPost.setTitle(mPostTitleEditView.getText().toString());
                    stopActionMode(false);
                    return true;
                }
                return false;
            }
        });

        // set up preview button
        mPreviewBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPreviewClickListener.onPreviewClicked();
            }
        });

        mActionBarSize = getActionBarSize();
        mScrollView.setScrollViewCallbacks(this);

        setHasOptionsMenu(true);

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        show();
    }

    @Override
    public void onResume() {
        super.onResume();
        mPostEditView.setText(mPost.getMarkdown());
    }

    @Override
    public void onShow() {
        if (mActivity != null && ! mPostHeaderCollapsed) {
            mActivity.setTitle(null);
        }
    }

    @Override
    public void onHide() {
        mPost.setMarkdown(mPostEditView.getText().toString());
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mPreviewClickListener = (OnPreviewClickListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                + "must implement OnPreviewClickListener");
        }
    }


    // action mode
    private void startActionMode() {
        mActionModeState = ActionModeState.STARTING;
        mPostTitleEditView.setBackgroundDrawable(mEditTextDefaultBackground);
        mActivity.setTitle(getString(R.string.edit_title));
        mActivity.supportInvalidateOptionsMenu();
        mActivity.setNavigationItem(R.drawable.close, mActionModeCloseClickListener);
        mPreviewBtn.hide(false);
        mActionModeState = ActionModeState.STARTED;
    }

    private void stopActionMode(boolean discardChanges) {
        if (discardChanges) {
            mPostTitleEditView.setText(mPost.getTitle());
        } else {
            mPost.setTitle(mPostTitleEditView.getText().toString());
        }
        mActionModeState = ActionModeState.STOPPING;
        mActivity.setTitle(null);
        mPostTitleEditView.setBackgroundColor(mTransparentColor);
        mPostTitleEditView.clearFocus();
        AppUtils.hideKeyboard(mActivity);
        mActivity.supportInvalidateOptionsMenu();
        mActivity.resetNavigationItem();
        mPreviewBtn.show(false);
        mActionModeState = ActionModeState.STOPPED;
    }

    @Override
    public boolean onBackPressed() {
        if (mActionModeState == ActionModeState.STARTED) {
            stopActionMode(true);
            return true;
        }
        return super.onBackPressed();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.post_edit, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        boolean actionModeActive =
                mActionModeState == ActionModeState.STARTING ||
                mActionModeState == ActionModeState.STARTED;
        menu.findItem(R.id.action_done).setVisible(actionModeActive);
        menu.findItem(R.id.action_save).setVisible(! actionModeActive);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_done:
                stopActionMode(false);
                return true;
            case R.id.action_save:
                onSaveClicked();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void onSaveClicked() {
        mPost.setMarkdown(mPostEditView.getText().toString());
        mPost.setHtml(null);   // omit stale HTML from request body
        getBus().post(new SavePostEvent(mPost));
    }

    @Subscribe
    public void onPostSavedEvent(PostSavedEvent event) {
        Toast.makeText(mActivity, "Post saved!", Toast.LENGTH_SHORT).show();
    }


    // scroll behaviour
    @Override
    public void onScrollChanged(int scrollY, boolean firstScroll, boolean dragging) {
        if (scrollY >= mActionBarSize) {
            collapsePostHeader();
        } else {
            expandPostHeader();
        }
    }

    @Override
    public void onDownMotionEvent() {

    }

    @Override
    public void onUpOrCancelMotionEvent(ScrollState scrollState) {

    }

    private void collapsePostHeader() {
        if (mPostHeaderCollapsed) {
            return;
        }
        mPostHeader.animate().alpha(0.0f).setDuration(200);
        getActivity().setTitle(mPost.getTitle());
        mPostHeaderCollapsed = true;
    }

    private void expandPostHeader() {
        if (! mPostHeaderCollapsed) {
            return;
        }
        mPostHeader.animate().alpha(1.0f).setDuration(200);
        getActivity().setTitle(null);
        mPostHeaderCollapsed = false;
    }

    private int getActionBarSize() {
        TypedValue typedValue = new TypedValue();
        int[] textSizeAttr = new int[] { R.attr.actionBarSize };
        int indexOfAttrTextSize = 0;
        TypedArray a = getActivity().obtainStyledAttributes(typedValue.data, textSizeAttr);
        int actionBarSize = a.getDimensionPixelSize(indexOfAttrTextSize, -1);
        a.recycle();
        return actionBarSize;
    }

}

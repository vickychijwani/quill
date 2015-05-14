package me.vickychijwani.spectre.view.widget;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

public class SpaceItemDecoration extends RecyclerView.ItemDecoration {

    private int horizontal;
    private int vertical;

    public SpaceItemDecoration(int horizontal, int vertical) {
        this.horizontal = horizontal;
        this.vertical = vertical;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        outRect.top = vertical;
        outRect.left = horizontal;
        outRect.right = horizontal;

        // Add bottom margin only for the last item to avoid double space between items
        if (parent.getChildLayoutPosition(view) == state.getItemCount()) {
            outRect.bottom = vertical;
        }
    }

}

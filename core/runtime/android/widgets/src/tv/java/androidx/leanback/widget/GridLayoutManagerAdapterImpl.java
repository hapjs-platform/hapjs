/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package androidx.leanback.widget;

import android.view.View;


import androidx.recyclerview.widget.OrientationHelper;
import androidx.recyclerview.widget.RecyclerView;

import org.hapjs.widgets.view.list.FlexLayoutManager;
import org.hapjs.widgets.view.list.RecyclerViewAdapter;

public class GridLayoutManagerAdapterImpl implements FlexLayoutManager {

    private static final String TAG = "GridLayoutManager";

    private RecyclerViewAdapter mRecyclerView;
    private RecyclerView mBaseGridView;
    private final GridLayoutManager mGridLayoutManagerDelegate;
    private int mSpanCount;

    public GridLayoutManagerAdapterImpl(GridLayoutManager layoutManager) {
        mGridLayoutManagerDelegate = layoutManager;
        mGridLayoutManagerDelegate.setOrientation(RecyclerView.VERTICAL);
    }


    @Override
    public void setFlexRecyclerView(RecyclerViewAdapter flexRecyclerView) {
        mRecyclerView = flexRecyclerView;
        mBaseGridView = flexRecyclerView.getActualRecyclerView();
    }

    @Override
    public RecyclerViewAdapter getFlexRecyclerView() {
        return mRecyclerView;
    }

    @Override
    public RecyclerView.LayoutManager getRealLayoutManager() {
        return mGridLayoutManagerDelegate;
    }

    @Override
    public void setScrollPage(boolean scrollPage) {
        // do not support.
    }

    @Override
    public int findFlexFirstVisibleItemPosition() {
        final View child = findOneVisibleChild(0, mGridLayoutManagerDelegate.getChildCount(), true, false);
        return child == null ? RecyclerView.NO_POSITION : mBaseGridView.getChildAdapterPosition(child);
    }

    @Override
    public int findFlexLastVisibleItemPosition() {
        final View child = findOneVisibleChild(mGridLayoutManagerDelegate.getChildCount() - 1, -1, false, true);
        return child == null ? RecyclerView.NO_POSITION : mBaseGridView.getChildAdapterPosition(child);
    }

    @Override
    public int findFlexFirstCompletelyVisibleItemPosition() {
        return findFlexFirstVisibleItemPosition();
    }

    @Override
    public int findFlexLastCompletelyVisibleItemPosition() {
        return findFlexLastVisibleItemPosition();
    }

    @Override
    public int getFlexItemCount() {
        return mGridLayoutManagerDelegate.getItemCount();
    }

    @Override
    public int getStateItemCount() {
        if (mRecyclerView != null) {
            return mRecyclerView.getState().getItemCount();
        }
        return 0;
    }

    @Override
    public int getOverScrolledY() {
        return 0;
    }

    @Override
    public int getFlexOrientation() {
        return mGridLayoutManagerDelegate.mOrientation;
    }

    @Override
    public void setFlexOrientation(int orientation) {
        mGridLayoutManagerDelegate.setOrientation(orientation);
    }

    @Override
    public void setFlexReverseLayout(boolean reverseLayout) {
        //do not support current.
        //TODO...
    }

    @Override
    public boolean canFlexScrollHorizontally() {
        return mGridLayoutManagerDelegate.canScrollHorizontally();
    }

    @Override
    public boolean canFlexScrollVertically() {
        return mGridLayoutManagerDelegate.canScrollVertically();
    }

    @Override
    public void setFlexSpanCount(int spanCount) {
        mSpanCount = spanCount;
        mGridLayoutManagerDelegate.setNumRows(spanCount);
    }

    @Override
    public int getFlexSpanCount() {
        return mSpanCount;
    }

    @Override
    public void scrollToFlexPositionWithOffset(int position, int offset) {
        mGridLayoutManagerDelegate.scrollToPosition(position);
    }

    @Override
    public View getFlexChildAt(int position) {
        return null;
    }

    @Override
    public int getFlexChildPosition(View view) {
        return 0;
    }

    @Override
    public void setSpanSizeLookup(androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup spanSizeLookup) {
        //do not support current.
        //TODO...
    }

    View findOneVisibleChild(int fromIndex, int toIndex, boolean completelyVisible,
                             boolean acceptPartiallyVisible) {
        OrientationHelper helper;
        if (mGridLayoutManagerDelegate.canScrollVertically()) {
            helper = OrientationHelper.createVerticalHelper(mGridLayoutManagerDelegate);
        } else {
            helper = OrientationHelper.createHorizontalHelper(mGridLayoutManagerDelegate);
        }

        final int start = helper.getStartAfterPadding();
        final int end = helper.getEndAfterPadding();
        final int next = toIndex > fromIndex ? 1 : -1;
        View partiallyVisible = null;
        for (int i = fromIndex; i != toIndex; i += next) {
            final View child = mGridLayoutManagerDelegate.getChildAt(i);
            final int childStart = helper.getDecoratedStart(child);
            final int childEnd = helper.getDecoratedEnd(child);
            if (childStart < end && childEnd > start) {
                if (completelyVisible) {
                    if (childStart >= start && childEnd <= end) {
                        return child;
                    } else if (acceptPartiallyVisible && partiallyVisible == null) {
                        partiallyVisible = child;
                    }
                } else {
                    return child;
                }
            }
        }
        return partiallyVisible;
    }
}
/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.view.list;

import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.view.ViewParent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.HapStaggeredGridLayoutManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.yoga.YogaConstants;
import com.facebook.yoga.YogaNode;
import org.hapjs.component.utils.YogaUtil;
import org.hapjs.component.view.YogaLayout;

public class FlexStaggeredGridLayoutManager extends HapStaggeredGridLayoutManager
        implements FlexLayoutManager {

    private boolean mScrollPage;
    private int mMaxHeight;
    private int mItemCount;
    private int mIndex = Integer.MAX_VALUE;
    private int mHeight;

    private RecyclerViewAdapter mFlexRecyclerView;

    private RecyclerView.Recycler mRecycler;
    private ViewGroup mParent;

    private int mOverScrolledY;

    public FlexStaggeredGridLayoutManager(int orientation) {
        super(DEFAULT_COLUMN_COUNT, orientation);
    }

    @Override
    public boolean isAutoMeasureEnabled() {
        return false;
    }

    @Override
    public void onMeasure(
            RecyclerView.Recycler recycler, RecyclerView.State state, int widthSpec,
            int heightSpec) {
        mRecycler = recycler;
        YogaNode node = YogaUtil.getYogaNode(mFlexRecyclerView.getActualRecyclerView());

        int maxHeight = 0;
        if (mScrollPage) {
            View moveableView = mFlexRecyclerView.getMoveableView();
            maxHeight =
                    (moveableView == null)
                            ? 0
                            : (moveableView.getMeasuredHeight()
                            - moveableView.getPaddingTop()
                            - moveableView.getPaddingBottom());
        } else {
            if (widthSpec == 0) {
                if (node != null && node.getParent() != null
                        && node.getParent().getChildCount() == 1) {
                    float parentWidth = node.getParent().getLayoutWidth();
                    if (parentWidth > 0) {
                        widthSpec = MeasureSpec
                                .makeMeasureSpec(Math.round(parentWidth), MeasureSpec.EXACTLY);
                    }
                }
            } else {
                int widthMode = MeasureSpec.getMode(widthSpec);
                int widthSize = MeasureSpec.getSize(widthSpec);
                if (widthSize > 0
                        && (widthMode == MeasureSpec.UNSPECIFIED
                        || widthMode == MeasureSpec.AT_MOST)) {
                    widthSpec = MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY);
                }
            }

            if (heightSpec == 0) {
                if (node != null && node.getParent() != null
                        && node.getParent().getChildCount() == 1) {
                    float parentHeight = node.getParent().getLayoutHeight();
                    if (parentHeight > 0) {
                        heightSpec = MeasureSpec
                                .makeMeasureSpec(Math.round(parentHeight), MeasureSpec.EXACTLY);
                    }
                }
            } else {
                int heightMode = MeasureSpec.getMode(heightSpec);
                int heightSize = MeasureSpec.getSize(heightSpec);
                if (heightSize > 0
                        && (heightMode == MeasureSpec.UNSPECIFIED
                        || heightMode == MeasureSpec.AT_MOST)) {
                    heightSpec = MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY);
                }
            }
        }

        if (maxHeight != mMaxHeight) {
            mIndex = Integer.MAX_VALUE;
            mItemCount = 0;
            mMaxHeight = maxHeight;
        }

        if (!mScrollPage || getOrientation() == HORIZONTAL || maxHeight == 0) {
            super.onMeasure(recycler, state, widthSpec, heightSpec);

            if (node != null && mItemCount != state.getItemCount()) {
                node.dirty();
            }
            mItemCount = state.getItemCount();
            return;
        }

        final int widthSize = View.MeasureSpec.getSize(widthSpec);
        if ((mHeight == maxHeight && state.getItemCount() >= mIndex)
                || state.getItemCount() == mItemCount) {
            setMeasuredDimension(widthSize, mHeight);
            setYogaHeight(mHeight);
            return;
        }

        int height =
                measureScrollPageHeight(recycler, state.getItemCount(), widthSpec, heightSpec,
                        maxHeight);
        setMeasuredDimension(widthSize, height);
        setYogaHeight(height);
        if (node != null) {
            node.dirty();
            mFlexRecyclerView.setDirty(true);
        }

        mHeight = height;
        mItemCount = state.getItemCount();
    }

    private int measureScrollPageHeight(
            RecyclerView.Recycler recycler, int itemCount, int widthSpec, int heightSpec,
            int maxHeight) {
        int height = 0;
        if (itemCount <= 0) {
            return height;
        }
        int spanCount =
                getSpanCount() >= DEFAULT_COLUMN_COUNT ? getSpanCount() : DEFAULT_COLUMN_COUNT;
        int endCol = spanCount - 1;
        int[] totalHeights = new int[spanCount];
        int previousCol = -1;
        int currentCol;
        boolean isHigherThanMoveableView = false;
        int curChildHeight = 0;
        int childWidthSpec = 0;
        int childHeightSpec = 0;
        for (int i = 0; i < itemCount; i++) {
            View view = recycler.getViewForPosition(i);
            if (view != null) {
                LayoutParams p = (LayoutParams) view.getLayoutParams();
                boolean fullSpan = p.isFullSpan();
                if (previousCol == endCol) {
                    currentCol = 0;
                    previousCol = 0;
                } else if (p.isFullSpan()) {
                    currentCol = 0;
                    previousCol = endCol;
                } else {
                    currentCol = previousCol + 1;
                    previousCol = currentCol;
                }

                childWidthSpec =
                        ViewGroup.getChildMeasureSpec(widthSpec,
                                getPaddingLeft() + getPaddingRight(), p.width);
                childHeightSpec =
                        ViewGroup.getChildMeasureSpec(
                                heightSpec, getPaddingTop() + getPaddingBottom(), p.height);
                view.measure(childWidthSpec, childHeightSpec);
                curChildHeight = view.getMeasuredHeight() + p.bottomMargin + p.topMargin;
                if (fullSpan) {
                    for (int index = 0; index < totalHeights.length; index++) {
                        totalHeights[index] += curChildHeight;
                    }
                } else {
                    totalHeights[currentCol] += curChildHeight;
                }
                for (int index = 0; index < totalHeights.length; index++) {
                    if (totalHeights[index] > maxHeight) {
                        height = maxHeight;
                        mIndex = i;
                        isHigherThanMoveableView = true;
                        break;
                    }
                }
                if (isHigherThanMoveableView) {
                    break;
                }
                mIndex = i;
            }
        }
        return height;
    }

    private void preMeasureHeight() {
        if (getOrientation() == HORIZONTAL
                || mRecycler == null
                || mFlexRecyclerView == null
                || getStateItemCount() == 0) {
            return;
        }
        if (mParent == null) {
            mParent = (ViewGroup) mFlexRecyclerView.getActualRecyclerView().getParent();
        }
        if (mParent == null) {
            return;
        }
        if (mScrollPage) {
            final View moveableView = mFlexRecyclerView.getMoveableView();
            int maxHeight =
                    moveableView.getMeasuredHeight()
                            - moveableView.getPaddingTop()
                            - moveableView.getPaddingBottom();
            if (maxHeight != mMaxHeight) {
                mIndex = Integer.MAX_VALUE;
                mItemCount = 0;
                mMaxHeight = maxHeight;
            }
            mItemCount = getStateItemCount();
            int tempSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            mHeight = measureScrollPageHeight(mRecycler, mItemCount, tempSpec, tempSpec, maxHeight);
            setYogaHeight(mHeight);
        } else {
            if (mParent instanceof YogaLayout) {
                YogaLayout yogaParent = (YogaLayout) mParent;
                YogaNode recyclerNode = yogaParent.getYogaNodeForView(mFlexRecyclerView.getActualRecyclerView());
                recyclerNode.setWidth(YogaConstants.UNDEFINED);
                recyclerNode.setHeight(YogaConstants.UNDEFINED);
            }
        }
    }

    private void setYogaHeight(int height) {
        mParent = (ViewGroup) mFlexRecyclerView.getActualRecyclerView().getParent();
        if (mParent instanceof YogaLayout) {
            YogaLayout parentView = (YogaLayout) mParent;
            YogaNode recyclerNode = parentView.getYogaNodeForView(mFlexRecyclerView.getActualRecyclerView());
            recyclerNode.setHeight(height);
        }
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler,
                                  RecyclerView.State state) {
        int scrolled = super.scrollVerticallyBy(dy, recycler, state);
        int overScrolledY = dy - scrolled;
        if (overScrolledY < 0) {
            mOverScrolledY = overScrolledY;
        } else {
            mOverScrolledY = 0;
        }

        return scrolled;
    }

    @Override
    public void smoothScrollToPosition(
            RecyclerView recyclerView, RecyclerView.State state, int position) {
        final TopSmoothScroller scroller = new TopSmoothScroller(recyclerView.getContext());
        scroller.setTargetPosition(position);
        startSmoothScroll(scroller);
    }

    @Override
    public RecyclerViewAdapter getFlexRecyclerView() {
        return mFlexRecyclerView;
    }

    @Override
    public void setFlexRecyclerView(RecyclerViewAdapter flexRecyclerView) {
        mFlexRecyclerView = flexRecyclerView;
    }

    @Override
    public RecyclerView.LayoutManager getRealLayoutManager() {
        return this;
    }

    @Override
    public void setScrollPage(boolean scrollPage) {
        if (scrollPage != mScrollPage) {
            mScrollPage = scrollPage;

            mMaxHeight = 0;
            mItemCount = 0;
            mIndex = Integer.MAX_VALUE;
            mHeight = 0;

            preMeasureHeight();

            // remeasure when scrollpage attribute changed.
            mFlexRecyclerView.resumeRequestLayout();
            mFlexRecyclerView.requestLayout();
        }
    }

    @Override
    public int getOverScrolledY() {
        return mOverScrolledY;
    }

    @Override
    public int getFlexOrientation() {
        return getOrientation();
    }

    @Override
    public void setFlexOrientation(int orientation) {
        setOrientation(orientation);
    }

    @Override
    public void setFlexReverseLayout(boolean reverseLayout) {
        setReverseLayout(reverseLayout);
    }

    @Override
    public boolean canFlexScrollHorizontally() {
        return canScrollHorizontally();
    }

    @Override
    public boolean canFlexScrollVertically() {
        return canScrollVertically();
    }

    @Override
    public int getFlexSpanCount() {
        return getSpanCount();
    }

    @Override
    public void setFlexSpanCount(int spanCount) {
        setSpanCount(spanCount);
    }

    @Override
    public int findFlexFirstVisibleItemPosition() {
        int[] column = findFirstVisibleItemPositions(null);
        if (column != null && column.length > 0) {
            return column[0];
        }
        return 0;
    }

    @Override
    public int findFlexLastVisibleItemPosition() {
        int lastPosition = 0;
        int[] column = findLastVisibleItemPositions(null);
        if (column != null && column.length > 0) {
            lastPosition = column[0];
            for (int value : column) {
                if (value > lastPosition) {
                    lastPosition = value;
                }
            }
        }
        return lastPosition;
    }

    @Override
    public int findFlexFirstCompletelyVisibleItemPosition() {
        int[] column = findFirstCompletelyVisibleItemPositions(null);
        if (column != null && column.length > 0) {
            return column[0];
        }
        return 0;
    }

    @Override
    public int findFlexLastCompletelyVisibleItemPosition() {
        int lastPosition = 0;
        int[] column = findLastCompletelyVisibleItemPositions(null);
        if (column != null && column.length > 0) {
            lastPosition = column[0];
            for (int value : column) {
                if (value > lastPosition) {
                    lastPosition = value;
                }
            }
        }
        return lastPosition;
    }

    @Override
    public int getFlexItemCount() {
        return getItemCount();
    }

    @Override
    public int getStateItemCount() {
        if (mFlexRecyclerView != null) {
            return mFlexRecyclerView.getState().getItemCount();
        }
        return 0;
    }

    @Override
    public void scrollToFlexPositionWithOffset(int position, int offset) {
        scrollToPositionWithOffset(position, offset);
    }

    @Override
    public View getFlexChildAt(int position) {
        return getChildAt(position);
    }

    @Override
    public int getFlexChildPosition(View view) {
        return getPosition(view);
    }

    @Override
    public void setSpanSizeLookup(GridLayoutManager.SpanSizeLookup spanSizeLookup) {
        // nothing to do.
    }

    @Override
    public boolean onRequestChildFocus(
            @NonNull RecyclerView parent,
            @NonNull RecyclerView.State state,
            @NonNull View child,
            @Nullable View focused) {
        // if focused view is removed from ViewTree,and the result of {super.onResultChildFocus} is
        // false,
        // it will crash in {@linkRecyclerView#requestChildOnScreen}.
        // parameter must be a descendant of this view
        boolean ret = super.onRequestChildFocus(parent, state, child, focused);
        if (!ret && focused != null && !isViewAttached(focused)) {
            ret = true;
        }
        return ret;
    }

    private boolean isViewAttached(View view) {
        if (view == null) {
            return false;
        }

        if (!view.isAttachedToWindow()) {
            return false;
        }

        // if the view was removed from its parent,it's mParent will be null.
        // But if the view is performing animation,when it is removed from it's parent,
        // the mAttachInfo of the view will not be set to null util the animation is complete
        ViewParent parent = view.getParent();
        while (parent instanceof View) {
            parent = parent.getParent();
        }
        // parent == null,the view is dettach from window
        // parent == ViewRootImpl, the top parent is ViewRootImpl
        return parent != null;
    }
}

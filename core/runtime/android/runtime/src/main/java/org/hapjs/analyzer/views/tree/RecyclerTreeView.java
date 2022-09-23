/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.analyzer.views.tree;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.hapjs.analyzer.views.SlideMonitoredRecyclerView;

import java.util.List;

public class RecyclerTreeView extends SlideMonitoredRecyclerView {

    private float mLastX;
    private float mLastY;

    private TreeViewAdapter mAdapter;

    public RecyclerTreeView(Context context) {
        this(context, null);
    }

    public RecyclerTreeView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RecyclerTreeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LinearLayoutManager linearLayoutManager = new ReSizeLinearLayoutManater(context, RecyclerView.VERTICAL, false);
        setLayoutManager(linearLayoutManager);
        mAdapter = new TreeViewAdapter();
        mAdapter.bindRecyclerView(this);
    }

    public void setData(List<TreeNode<NodeItemInfo>> nodes) {
        mAdapter.setNodes(nodes);
    }

    public void setOnNodeItemClickListener(TreeViewAdapter.OnNodeItemClickListener listener) {
        mAdapter.setOnNodeItemClickListener(listener);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        boolean intercept = super.onInterceptTouchEvent(e);

        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLastX = e.getX();
                mLastY = e.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                float deltaX = Math.abs(e.getX() - mLastX);
                float deltaY = Math.abs(e.getY() - mLastY);
                if (Math.abs(deltaY) > Math.abs(deltaX)) {
                    requestDisallowInterceptTouchEvent(true);
                    intercept = true;
                }
                break;
            case MotionEvent.ACTION_UP:
                intercept = false;
                break;
            default:
        }
        return intercept;
    }

    private static class ReSizeLinearLayoutManater extends LinearLayoutManager {
        private int[] mMeasuredDimension = new int[2];
        private int mMaxWidth;

        public ReSizeLinearLayoutManater(Context context) {
            super(context);
        }

        public ReSizeLinearLayoutManater(Context context, int orientation, boolean reverseLayout) {
            super(context, orientation, reverseLayout);
        }

        public ReSizeLinearLayoutManater(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
            super(context, attrs, defStyleAttr, defStyleRes);
        }

        @Override
        public void onMeasure(@NonNull Recycler recycler, @NonNull State state, int widthSpec, int heightSpec) {
            if (getItemCount() != state.getItemCount()) {
                super.onMeasure(recycler, state, widthSpec, heightSpec);
                return;
            }
            int widthMode = View.MeasureSpec.getMode(widthSpec);
            int heightMode = View.MeasureSpec.getMode(heightSpec);
            int widthSize = View.MeasureSpec.getSize(widthSpec);
            int heightSize = View.MeasureSpec.getSize(heightSpec);
            int width = 0;
            int height = 0;
            boolean isHorizontal = getOrientation() == HORIZONTAL;
            for (int i = 0; i < getItemCount(); i++) {
                if (isHorizontal) {
                    measureScrapChild(recycler, i,
                            View.MeasureSpec.makeMeasureSpec(i, View.MeasureSpec.UNSPECIFIED),
                            heightSpec,
                            mMeasuredDimension);
                    width = width + mMeasuredDimension[0];
                    if (i == 0) {
                        height = mMeasuredDimension[1];
                    } else {
                        height = Math.max(height, mMeasuredDimension[1]);
                    }
                } else {
                    measureScrapChild(recycler, i,
                            widthSpec,
                            View.MeasureSpec.makeMeasureSpec(i, View.MeasureSpec.UNSPECIFIED),
                            mMeasuredDimension);
                    height = height + mMeasuredDimension[1];
                    if (i == 0) {
                        width = mMeasuredDimension[0];
                    } else {
                        width = Math.max(width, mMeasuredDimension[0]);
                    }
                }
            }
            mMaxWidth = width;
            switch (widthMode) {
                case View.MeasureSpec.EXACTLY:
                    width = widthSize;
                case View.MeasureSpec.AT_MOST:
                case View.MeasureSpec.UNSPECIFIED:
            }

            switch (heightMode) {
                case View.MeasureSpec.EXACTLY:
                    height = heightSize;
                case View.MeasureSpec.AT_MOST:
                case View.MeasureSpec.UNSPECIFIED:
            }
            setMeasuredDimension(width, height);
        }

        private void measureScrapChild(RecyclerView.Recycler recycler, int position, int widthSpec,
                                       int heightSpec, int[] measuredDimension) {
            View view = recycler.getViewForPosition(position);
            recycler.bindViewToPosition(view, position);
            RecyclerView.LayoutParams p = (RecyclerView.LayoutParams) view.getLayoutParams();
            int childWidthSpec = ViewGroup.getChildMeasureSpec(widthSpec,
                    getPaddingLeft() + getPaddingRight(), p.width);
            int childHeightSpec = ViewGroup.getChildMeasureSpec(heightSpec,
                    getPaddingTop() + getPaddingBottom(), p.height);
            view.measure(childWidthSpec, childHeightSpec);
            measuredDimension[0] = view.getMeasuredWidth() + p.leftMargin + p.rightMargin;
            measuredDimension[1] = view.getMeasuredHeight() + p.bottomMargin + p.topMargin;
            recycler.recycleView(view);
        }

        @Override
        public void setMeasuredDimension(Rect childrenBounds, int wSpec, int hSpec) {
            // In the autoMeasured mode of RecyclerView, the final width and height will be re-adjusted according to the loaded item,
            // causing the content to be unable to be displayed.
            int usedWidth = childrenBounds.width() + getPaddingLeft() + getPaddingRight();
            int usedHeight = childrenBounds.height() + getPaddingTop() + getPaddingBottom();
            int width = chooseSize(wSpec, usedWidth, getMinimumWidth());
            int height = chooseSize(hSpec, usedHeight, getMinimumHeight());
            setMeasuredDimension(Math.max(mMaxWidth, width), height);
        }
    }

}

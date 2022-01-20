/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.view.list.section;

import android.content.Context;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.facebook.yoga.YogaNode;
import org.hapjs.common.utils.DisplayUtil;
import org.hapjs.component.Component;
import org.hapjs.component.utils.YogaUtil;

public class SectionListLayoutManager extends LinearLayoutManager {

    private SectionRecyclerView mRecyclerView;
    private int[] mMeasuredDimension = new int[2];

    public SectionListLayoutManager(
            @NonNull Context context, @NonNull SectionRecyclerView recyclerView) {
        super(context);
        mRecyclerView = recyclerView;
    }

    @Override
    public boolean isAutoMeasureEnabled() {
        Component component = mRecyclerView.getComponent();
        return component.isWidthDefined() && component.isHeightDefined();
    }

    @Override
    public void onMeasure(
            @NonNull RecyclerView.Recycler recycler,
            @NonNull RecyclerView.State state,
            int widthSpec,
            int heightSpec) {
        if (isAutoMeasureEnabled()) {
            super.onMeasure(recycler, state, widthSpec, heightSpec);
            return;
        }

        YogaNode node = YogaUtil.getYogaNode(mRecyclerView);

        if (widthSpec == 0) {
            if (node != null && node.getParent() != null && node.getParent().getChildCount() == 1) {
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
                    && (widthMode == MeasureSpec.UNSPECIFIED || widthMode == MeasureSpec.AT_MOST)) {
                widthSpec = MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY);
            }
        }

        if (heightSpec == 0) {
            if (node != null && node.getParent() != null && node.getParent().getChildCount() == 1) {
                float parentHeight = node.getParent().getLayoutHeight();
                if (parentHeight > 0) {
                    heightSpec = MeasureSpec
                            .makeMeasureSpec(Math.round(parentHeight), MeasureSpec.EXACTLY);
                }
            } else {
                int height = 0;
                int screenHeight = DisplayUtil.getScreenHeight(mRecyclerView.getContext());
                int itemCount = state.getItemCount();
                for (int i = 0; i < itemCount; i++) {
                    measureScrapChild(
                            recycler,
                            i,
                            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                            mMeasuredDimension);
                    height += mMeasuredDimension[1];
                    if (height > screenHeight) {
                        height = screenHeight;
                        break;
                    }
                }
                heightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
            }
        } else {
            int heightMode = MeasureSpec.getMode(heightSpec);
            int heightSize = MeasureSpec.getSize(heightSpec);
            if (heightSize > 0
                    &&
                    (heightMode == MeasureSpec.UNSPECIFIED || heightMode == MeasureSpec.AT_MOST)) {
                heightSpec = MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY);
            }
        }

        super.onMeasure(recycler, state, widthSpec, heightSpec);
    }

    private void measureScrapChild(
            RecyclerView.Recycler recycler,
            int position,
            int widthSpec,
            int heightSpec,
            int[] measuredDimension) {
        View view = recycler.getViewForPosition(position);
        RecyclerView.LayoutParams p = (RecyclerView.LayoutParams) view.getLayoutParams();
        int childWidthSpec =
                ViewGroup.getChildMeasureSpec(widthSpec, getPaddingLeft() + getPaddingRight(),
                        p.width);
        int childHeightSpec =
                ViewGroup.getChildMeasureSpec(heightSpec, getPaddingTop() + getPaddingBottom(),
                        p.height);
        view.measure(childWidthSpec, childHeightSpec);
        measuredDimension[0] = view.getMeasuredWidth() + p.leftMargin + p.rightMargin;
        measuredDimension[1] = view.getMeasuredHeight() + p.bottomMargin + p.topMargin;
    }
}

/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.view.refresh;

import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import org.hapjs.common.utils.FloatUtil;
import org.hapjs.component.Component;
import org.hapjs.component.view.ComponentHost;

public abstract class RefreshMovement {

    private View mView;
    private int mMoveY;

    private OnMoveListener mMoveListener;

    public RefreshMovement(@NonNull View view) {
        mView = view;
    }

    @NonNull
    public View getView() {
        return mView;
    }

    public void measure(
            int parentPaddingH, int parentPaddingV, int widthMeasureSpec, int heightMeasureSpec) {
        View view = getView();

        int parentWidth = View.MeasureSpec.getSize(widthMeasureSpec);
        int parentHeight = View.MeasureSpec.getSize(heightMeasureSpec);
        int maxWidth = parentWidth - parentPaddingH;
        int maxHeight = parentHeight - parentPaddingV;

        // 1.计算view设置的宽度和高度
        RefreshLayout.LayoutParams params = (RefreshLayout.LayoutParams) view.getLayoutParams();
        int expectWidth = params.width;
        int expectHeight = params.height;
        if (view instanceof ComponentHost) {
            Component component = ((ComponentHost) view).getComponent();
            if (component.isWidthDefined() && !FloatUtil.isUndefined(component.getPercentWidth())) {
                float percentWidth = component.getPercentWidth();
                if (percentWidth >= 0) {
                    expectWidth = (int) (parentWidth * percentWidth);
                }
            }

            if (component.isHeightDefined()
                    && !FloatUtil.isUndefined(component.getPercentHeight())) {
                float percentHeight = component.getPercentHeight();
                if (percentHeight > 0) {
                    expectHeight = (int) (parentHeight * percentHeight);
                }
            }
        }

        int childWidthSpec =
                ViewGroup.getChildMeasureSpec(widthMeasureSpec, parentPaddingH, expectWidth);
        int childHeightSpec =
                ViewGroup.getChildMeasureSpec(heightMeasureSpec, parentPaddingV, expectHeight);
        view.measure(childWidthSpec, childHeightSpec);
    }

    public void layout(int left, int top, int right, int bottom) {
        View view = getView();
        RefreshLayout.LayoutParams params = (RefreshLayout.LayoutParams) view.getLayoutParams();
        int l = left + params.leftMargin;
        int t = top + params.topMargin;
        int r = l + view.getMeasuredWidth();
        int b = t + view.getMeasuredHeight();
        view.layout(l, t, r, b);
    }

    public int getMeasureWidth() {
        return getView().getMeasuredWidth();
    }

    public int getMeasureHeight() {
        return getView().getMeasuredHeight();
    }

    /**
     * 移动content、header或footer。
     *
     * @param moveY   相对于初始位置的绝对移动距离。moveY > 0：向下移动。moveY < 0：向上移动
     * @param percent 当前移动的距离/触发刷新距离。如果moveY > 0，触发刷新的距离由header决定。如果moveY小于0，触发刷新的距离由footer决定。
     * @param isDrag  是否在拖拽状态下移动
     */
    public final void move(float moveY, float percent, boolean isDrag, boolean isRefreshing) {
        mMoveY = (int) moveY;
        if (mMoveListener != null) {
            mMoveListener.onMove(this, moveY, percent, isDrag, isRefreshing);
        }
        onMove(moveY, percent, isDrag, isRefreshing);
    }

    public int getMoveY() {
        return mMoveY;
    }

    protected abstract void onMove(float moveY, float percent, boolean isDrag, boolean refresh);

    public void setMoveListener(OnMoveListener moveListener) {
        mMoveListener = moveListener;
    }

    public interface OnMoveListener {
        void onMove(
                RefreshMovement movement,
                float moveDistance,
                float percent,
                boolean isDrag,
                boolean isRefreshing);
    }
}

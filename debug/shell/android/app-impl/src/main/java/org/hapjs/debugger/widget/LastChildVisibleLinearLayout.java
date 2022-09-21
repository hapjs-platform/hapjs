/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.debugger.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

public class LastChildVisibleLinearLayout extends LinearLayout {

    public LastChildVisibleLinearLayout(Context context) {
        super(context);
    }

    public LastChildVisibleLinearLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public LastChildVisibleLinearLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int count = getChildCount();
        if (count <= 1) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int totalLength = 0;
        for (int i = 0; i < getChildCount(); ++i) {
            totalLength += getChildAt(i).getMeasuredWidth();
        }
        if (totalLength > getMeasuredWidth()) {
            setChildrenLayoutWeightExceptLast(1);
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    private void setChildrenLayoutWeightExceptLast(int weight) {
        for (int i = 0; i < getChildCount() - 1; ++i) {
            View child = getChildAt(i);
            LayoutParams params = (LayoutParams) child.getLayoutParams();
            params.weight = weight;
            child.setLayoutParams(params);
        }
    }
}

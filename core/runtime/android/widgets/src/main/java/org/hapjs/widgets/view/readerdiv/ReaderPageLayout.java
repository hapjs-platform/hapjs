/*
 * Copyright (c) 2023, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hapjs.widgets.view.readerdiv;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewParent;
import android.widget.LinearLayout;

import com.facebook.yoga.YogaConstants;
import com.facebook.yoga.YogaNode;

import org.hapjs.component.view.YogaLayout;

public class ReaderPageLayout extends LinearLayout {
    private static final String TAG = "ReaderPageLayout";

    public ReaderPageLayout(Context context) {
        super(context);
    }

    public ReaderPageLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ReaderPageLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ReaderPageLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        float realWidth = MeasureSpec.getSize(widthMeasureSpec);
        float realHeight = MeasureSpec.getSize(heightMeasureSpec);
        ViewParent viewParent = getParent();
        if (viewParent instanceof YogaLayout) {
            YogaNode yogaNode = ((YogaLayout) viewParent).getYogaNodeForView(this);
            if (null != yogaNode) {
                yogaNode.setWidth(YogaConstants.UNDEFINED);
                if (realHeight == 0) {
                    yogaNode.setHeight(YogaConstants.UNDEFINED);
                }
            } else {
                Log.w(TAG, ReaderLayoutView.READER_LOG_TAG + "onMeasure yogaNode null ,realWidth : " + realWidth
                        + " realHeight : " + realHeight);
            }
        } else {
            Log.w(TAG, ReaderLayoutView.READER_LOG_TAG + "onMeasure viewParent not YogaLayout , realWidth : " + realWidth
                    + " realHeight : " + realHeight);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (viewParent instanceof ReaderPageView) {
            int width = ((ReaderPageView) viewParent).getWidth();
            int height = ((ReaderPageView) viewParent).getHeight();
            if (width > 0 && getMeasuredWidth() < width && height > 0 && getMeasuredHeight() <= height) {
                Log.w(TAG, ReaderLayoutView.READER_LOG_TAG + "onMeasure width error ,parent width : " + width
                        + " parent height : " + height
                        + " getMeasuredWidth() : " + getMeasuredWidth()
                        + " getMeasuredHeight() : " + getMeasuredHeight());
                setMeasuredDimension(width, height);
            }
        }
    }
}

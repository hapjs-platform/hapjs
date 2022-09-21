/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hapjs.analyzer.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ScrollView;

public class SlideMonitoredScrollView extends ScrollView implements SlideDetectable{
    private float mLastY;
    private OnSlideToBottomListener mOnSlideToBottomListener;

    public SlideMonitoredScrollView(Context context) {
        super(context);
    }

    public SlideMonitoredScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SlideMonitoredScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setOnSlideToBottomListener(OnSlideToBottomListener onCloseListener) {
        this.mOnSlideToBottomListener = onCloseListener;
    }

    @Override
    public void onSlideToBottom() {
        if (mOnSlideToBottomListener != null) {
            mOnSlideToBottomListener.onSlideToBottom();
        }
    }

    @Override
    public boolean isSlideToBottom() {
        View contentView = getChildAt(0);
        return contentView.getMeasuredHeight() <= getScrollY() + getHeight();
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLastY = ev.getY();
                break;
            case MotionEvent.ACTION_UP:
                float y = ev.getY();
                if (mLastY - y > 50 && isSlideToBottom()) {
                    onSlideToBottom();
                    break;
                }
        }
        return super.onTouchEvent(ev);
    }
}

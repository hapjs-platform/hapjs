/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.analyzer.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

public class SlideMonitoredRecyclerView extends RecyclerView implements SlideDetectable{
    private float mLastY;
    private OnSlideToBottomListener mOnSlideToBottomListener;

    public SlideMonitoredRecyclerView(@NonNull Context context) {
        super(context);
    }

    public SlideMonitoredRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public SlideMonitoredRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
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
            default:
        }
        return super.onTouchEvent(ev);
    }

    /**
     * Determine whether mRecyclerTreeView has been slid to the bottom
     */
    @Override
    public boolean isSlideToBottom() {
        return computeVerticalScrollExtent() + computeVerticalScrollOffset()
                >= computeVerticalScrollRange();
    }

    @Override
    public void onSlideToBottom() {
        if (mOnSlideToBottomListener != null) {
            mOnSlideToBottomListener.onSlideToBottom();
        }
    }

    public void setOnSlideToBottomListener(OnSlideToBottomListener onSlideToBottomListener) {
        this.mOnSlideToBottomListener = onSlideToBottomListener;
    }
}

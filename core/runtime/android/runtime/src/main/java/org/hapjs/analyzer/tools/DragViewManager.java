/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hapjs.analyzer.tools;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.annotation.IntDef;

import java.util.List;

public class DragViewManager implements View.OnTouchListener {
    private static final String TAG = "DragViewManager";
    public static final int POSITION_UNKNOWN = -1;
    public static final int POSITION_LEFT = 0;
    public static final int POSITION_RIGHT = 1;
    private static final int TOUCH_SLOP = 15;
    private View mTarget;
    private List<View> mControllers;
    private float mStartX, mLastX;
    private float mStartY, mLastY;
    private boolean mDraggable = false;
    private OnDragListener mListener;
    private @HorizontalPosition int mPosition = POSITION_UNKNOWN;

    public DragViewManager(View targetView, List<View> controllerViews) {
        if (targetView == null || controllerViews == null || controllerViews.isEmpty()) {
            throw new IllegalArgumentException("the view of target or controller must not be null!");
        }
        this.mTarget = targetView;
        this.mControllers = controllerViews;
        for (View controllerView : controllerViews) {
            if (controllerView == null) {
                throw new IllegalArgumentException("the view of target or controller must not be null!");
            }
            controllerView.setOnTouchListener(this);
        }
        targetView.post(() -> {
            if (mPosition == POSITION_UNKNOWN) {
                mPosition = getEdgePosition();
                Log.i(TAG, "AnalyzerPanel_LOG init mPosition: " + mPosition);
            }
        });
    }

    public void setOnDragListener(OnDragListener onDragListener) {
        this.mListener = onDragListener;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (!mControllers.contains(v)) {
            return false;
        }
        float rawX = event.getRawX();
        float rawY = event.getRawY();
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                mStartX = mLastX = event.getRawX();
                mStartY = mLastY = event.getRawY();
                v.setPressed(true);
                break;
            case MotionEvent.ACTION_UP:
                v.setPressed(false);
                if (!mDraggable) {
                    v.performClick();
                } else {
                    moveToEdge();
                }
                mDraggable = false;
                break;
            case MotionEvent.ACTION_MOVE:
                if (!mDraggable && (Math.abs(rawY - mStartY) > TOUCH_SLOP || (Math.abs(rawX - mStartX) > TOUCH_SLOP))) {
                    mDraggable = true;
                    if (mListener != null) {
                        mListener.onDragging(mTarget);
                    }
                }
                if (mDraggable && mTarget != null) {
                    float dx = rawX - mLastX;
                    float dy = rawY - mLastY;
                    mTarget.setTranslationX(mTarget.getTranslationX() + dx);
                    mTarget.setTranslationY(mTarget.getTranslationY() + dy);
                }
                break;
            default:
                break;
        }
        mLastX = rawX;
        mLastY = rawY;
        return true;
    }

    /**
     * Move nearby to the left or right of the parent layout
     */
    private void moveToEdge() {
        ViewGroup parent = (ViewGroup) (mTarget.getParent());
        float translationX = mTarget.getTranslationX();
        float x = mTarget.getX();
        int lastTranslationX;
        int newPosition;
        if (x <= ((float) parent.getWidth() - mTarget.getWidth()) / 2) {
            lastTranslationX = 0 - mTarget.getLeft();
            newPosition = POSITION_LEFT;
        } else {
            lastTranslationX = parent.getWidth() - mTarget.getWidth() - mTarget.getLeft();
            newPosition = POSITION_RIGHT;
        }
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(translationX, lastTranslationX);
        valueAnimator.setDuration(300);
        valueAnimator.setRepeatCount(0);
        valueAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        valueAnimator.addUpdateListener(animation -> {
            float animatedValue = (float) animation.getAnimatedValue();
            mTarget.setTranslationX(animatedValue);
        });
        int finalNewPosition = newPosition;
        valueAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (mListener != null) {
                    mListener.onMoveToEdge(mTarget, mPosition, finalNewPosition);
                }
                mPosition = finalNewPosition;
            }
        });
        valueAnimator.start();
    }

    private int getEdgePosition(){
        ViewGroup parent = (ViewGroup) (mTarget.getParent());
        float x = mTarget.getX();
        if (x <= ((float) parent.getWidth() - mTarget.getWidth()) / 2) {
            return POSITION_LEFT;
        } else {
            return POSITION_RIGHT;
        }
    }

    public interface OnDragListener {
        void onMoveToEdge(View target, @HorizontalPosition int lastPosition, @HorizontalPosition int newPosition);

        void onDragging(View target);
    }

    @IntDef({POSITION_UNKNOWN, POSITION_LEFT, POSITION_RIGHT})
    public @interface HorizontalPosition {
    }
}

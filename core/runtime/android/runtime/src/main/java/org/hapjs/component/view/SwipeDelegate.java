/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.view;

import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;
import java.util.HashMap;
import java.util.Map;
import org.hapjs.component.Component;
import org.hapjs.component.SwipeObserver;
import org.hapjs.component.constants.Attributes;
import org.hapjs.runtime.HapEngine;

public abstract class SwipeDelegate implements OnTouchListener {

    private static final String TAG = "SwipeDelegate";

    private static final String LEFT = "left";
    private static final String RIGHT = "right";
    private static final String UP = "up";
    private static final String DOWN = "down";
    private static final String DIRECTION = "direction";
    private final int mSwipeDistance;

    private VelocityTracker mVelocityTracker;
    private int mMaximumFlingVelocity;
    private int mMinmumFlingVelocity;
    private float mLastDownX;
    private float mLastDownY;

    public SwipeDelegate(HapEngine hapEngine) {
        mMaximumFlingVelocity = ViewConfiguration.getMaximumFlingVelocity();
        mMinmumFlingVelocity = ViewConfiguration.getMinimumFlingVelocity();
        mSwipeDistance = Attributes.getInt(hapEngine, "30px");
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        onTouchEvent(event);
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (v instanceof ComponentHost) {
                Component component = ((ComponentHost) v).getComponent();
                if (component instanceof SwipeObserver) {
                    return false;
                }
            }
            if (v.getParent() instanceof ScrollView) {
                return false;
            }
            return true;
        }
        return false;
    }

    public void onTouchEvent(MotionEvent ev) {
        final int action = ev.getAction();
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: {
                if (mVelocityTracker == null) {
                    mVelocityTracker = VelocityTracker.obtain();
                }
                mLastDownX = ev.getX();
                mLastDownY = ev.getY();
                break;
            }
            case MotionEvent.ACTION_UP: {
                final VelocityTracker velocityTracker = mVelocityTracker;
                float distanceX = ev.getX() - mLastDownX;
                float distanceY = ev.getY() - mLastDownY;
                float absDistanceX = Math.abs(distanceX);
                float absDistanceY = Math.abs(distanceY);
                if (velocityTracker != null
                        && (absDistanceX > mSwipeDistance || absDistanceY > mSwipeDistance)) {
                    final int pointerId = ev.getPointerId(0);
                    mVelocityTracker.computeCurrentVelocity(1000, mMaximumFlingVelocity);
                    final float xvel = mVelocityTracker.getXVelocity(pointerId);
                    final float yvel = mVelocityTracker.getYVelocity(pointerId);
                    Map<String, Object> param = new HashMap<>();
                    if (absDistanceX >= absDistanceY && Math.abs(xvel) > mMinmumFlingVelocity) {
                        param.put(DIRECTION, distanceX < 0 ? LEFT : RIGHT);
                    } else if (absDistanceX < absDistanceY
                            && Math.abs(yvel) > mMinmumFlingVelocity) {
                        param.put(DIRECTION, distanceY < 0 ? UP : DOWN);
                    }
                    if (!param.isEmpty()) {
                        onSwipe(param);
                    }
                }
                releaseVelocityTracker();
                break;
            }
            case MotionEvent.ACTION_CANCEL: {
                releaseVelocityTracker();
                break;
            }
            default:
                break;
        }
        final VelocityTracker velocityTracker = mVelocityTracker;
        if (velocityTracker != null) {
            velocityTracker.addMovement(ev);
        }
    }

    private void releaseVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.clear();
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    public abstract void onSwipe(Map<String, Object> direction);
}

/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.debugger.widget;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.math.MathUtils;
import androidx.core.view.MotionEventCompat;
import androidx.core.view.ViewCompat;
import androidx.customview.widget.ViewDragHelper;

import org.hapjs.debugger.app.impl.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class SettingViewBehavior<V extends View> extends CoordinatorLayout.Behavior<V> {
    public static final int STATE_DRAGGING = 1;
    public static final int STATE_SETTLING = 2;
    public static final int STATE_EXPANDED = 3;
    public static final int STATE_HIDDEN = 4;

    @IntDef({STATE_EXPANDED, STATE_HIDDEN, STATE_DRAGGING, STATE_SETTLING})
    @Retention(RetentionPolicy.SOURCE)
    public @interface State {
    }

    private int mPeekHeight;
    private int mMinOffset;
    private int mMaxOffset;
    @State
    private int mState = STATE_EXPANDED;
    private ViewDragHelper mViewDragHelper;
    private boolean mIgnoreEvents;
    private int mParentHeight;
    private WeakReference<V> mViewRef;
    private final ArrayList<Callback> callbacks = new ArrayList<>();

    public SettingViewBehavior() {
    }

    public SettingViewBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public Parcelable onSaveInstanceState(CoordinatorLayout parent, V child) {
        return new SavedState(super.onSaveInstanceState(parent, child), mState);
    }

    @Override
    public void onRestoreInstanceState(CoordinatorLayout parent, V child, Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(parent, child, ss.getSuperState());
        mState = ss.state;
        if (mState == STATE_DRAGGING || mState == STATE_SETTLING) {
            mState = STATE_HIDDEN;
        }
    }

    @Override
    public boolean onLayoutChild(CoordinatorLayout parent, V child, int layoutDirection) {
        parent.onLayoutChild(child, layoutDirection);
        mParentHeight = parent.getHeight();
        mMinOffset = Math.max(0, mParentHeight - child.getHeight());
        mMaxOffset = mParentHeight;
        mPeekHeight = mMinOffset + child.getHeight() / 6;
        if (mViewDragHelper == null) {
            mViewDragHelper = ViewDragHelper.create(parent, mDragCallback);
        }
        mViewRef = new WeakReference<>(child);
//        if (mState == STATE_EXPANDED) {
//            ViewCompat.offsetTopAndBottom(child, 0);
//        } else if (mState == STATE_COLLAPSED) {
//            ViewCompat.offsetTopAndBottom(child, mMaxOffset);
//        }
        return true;
    }

    @Override
    public boolean onInterceptTouchEvent(CoordinatorLayout parent, V child, MotionEvent event) {
        int action = MotionEventCompat.getActionMasked(event);
        switch (action) {
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                // Reset the ignore flag
                if (mIgnoreEvents) {
                    mIgnoreEvents = false;
                    return false;
                }
                break;
            case MotionEvent.ACTION_DOWN:
                View toolbar = child.findViewById(R.id.toolbar);
                mIgnoreEvents = !parent.isPointInChildBounds(toolbar, (int) event.getX(), (int) event.getY());
                break;
        }
        return !mIgnoreEvents && mViewDragHelper.shouldInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(CoordinatorLayout parent, V child, MotionEvent event) {
        if (!child.isShown()) {
            return false;
        }
        int action = event.getActionMasked();
        if (mState == STATE_DRAGGING && action == MotionEvent.ACTION_DOWN) {
            return true;
        }
        if (mViewDragHelper != null && !mIgnoreEvents) {
            mViewDragHelper.processTouchEvent(event);
        }
        return !mIgnoreEvents;
    }

    public final void setPeekHeight(int peekHeight) {
        mPeekHeight = Math.max(0, peekHeight);
        mMaxOffset = mParentHeight;
    }

    public final int getPeekHeight() {
        return mPeekHeight;
    }

    public final void setState(@State int state) {
        if (mViewRef == null) {
            return;
        }
        V child = mViewRef.get();
        if (child == null) {
            return;
        }
        int top;
        if (state == STATE_HIDDEN) {
            top = mMaxOffset;
        } else if (state == STATE_EXPANDED) {
            top = mMinOffset;
        } else {
            throw new IllegalArgumentException("Illegal state argument: " + state);
        }
        setStateInternal(STATE_SETTLING);
        if (mViewDragHelper.smoothSlideViewTo(child, child.getLeft(), top)) {
            ViewCompat.postOnAnimation(child, new SettleRunnable(child, state));
        }
    }

    @State
    public final int getState() {
        return mState;
    }

    private void setStateInternal(@State int state) {
        if (mState == state) {
            return;
        }
        mState = state;
        for (int i = 0; i < callbacks.size(); i++) {
            callbacks.get(i).onStateChanged(mViewRef.get(), state);
        }
    }

    public void addCallback(@NonNull Callback callback) {
        if (!callbacks.contains(callback)) {
            callbacks.add(callback);
        }
    }

    public void removeCallback(@NonNull Callback callback) {
        callbacks.remove(callback);
    }

    private final ViewDragHelper.Callback mDragCallback = new ViewDragHelper.Callback() {
        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            return true;
        }

        @Override
        public void onViewDragStateChanged(int state) {
            if (state == ViewDragHelper.STATE_DRAGGING) {
                setStateInternal(STATE_DRAGGING);
            }
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            int top;
            @State int targetState;
            if (mPeekHeight > releasedChild.getTop()) {
                top = mMinOffset;
                targetState = STATE_EXPANDED;
            } else {
                top = mMaxOffset;
                targetState = STATE_HIDDEN;
            }
            setStateInternal(STATE_SETTLING);
            if (mViewDragHelper.settleCapturedViewAt(releasedChild.getLeft(), top)) {
                ViewCompat.postOnAnimation(releasedChild, new SettleRunnable(releasedChild, targetState));
            }
        }

        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            return MathUtils.clamp(top, mMinOffset, mMaxOffset);
        }

        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            return child.getLeft();
        }
    };

    private class SettleRunnable implements Runnable {
        private final View mView;
        @State
        private final int mTargetState;

        SettleRunnable(View view, @State int targetState) {
            mView = view;
            mTargetState = targetState;
        }

        @Override
        public void run() {
            if (mViewDragHelper != null && mViewDragHelper.continueSettling(true)) {
                ViewCompat.postOnAnimation(mView, this);
            } else {
                setStateInternal(mTargetState);
            }
        }
    }

    protected static class SavedState extends View.BaseSavedState {
        @State
        final int state;

        public SavedState(Parcel source) {
            super(source);
            state = source.readInt();
        }

        public SavedState(Parcelable superState, @State int state) {
            super(superState);
            this.state = state;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(state);
        }

        public static final Creator<SavedState> CREATOR =
                new Creator<SavedState>() {
                    @Override
                    public SavedState createFromParcel(Parcel source) {
                        return new SavedState(source);
                    }

                    @Override
                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }

    @SuppressWarnings("unchecked")
    public static <V extends View> SettingViewBehavior<V> from(V view) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (!(params instanceof CoordinatorLayout.LayoutParams)) {
            throw new IllegalArgumentException("The view is not a child of CoordinatorLayout");
        }
        CoordinatorLayout.Behavior behavior = ((CoordinatorLayout.LayoutParams) params).getBehavior();
        if (!(behavior instanceof SettingViewBehavior)) {
            throw new IllegalArgumentException(
                    "The view is not associated with SettingViewBehavior");
        }
        return (SettingViewBehavior<V>) behavior;
    }

    public abstract static class Callback {
        public abstract void onStateChanged(@NonNull View bottomSheet, @State int newState);

        public abstract void onSlide(@NonNull View bottomSheet, float slideOffset);
    }
}

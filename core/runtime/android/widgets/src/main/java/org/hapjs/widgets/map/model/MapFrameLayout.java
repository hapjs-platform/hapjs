/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.map.model;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.hapjs.component.Component;
import org.hapjs.component.view.ComponentHost;
import org.hapjs.component.view.gesture.GestureHost;
import org.hapjs.component.view.gesture.IGesture;
import org.hapjs.component.view.keyevent.KeyEventDelegate;
import org.hapjs.widgets.R;
import org.hapjs.widgets.map.Map;

public class MapFrameLayout extends FrameLayout implements ComponentHost, GestureHost {

    private Component mComponent;
    private KeyEventDelegate mKeyEventDelegate;
    private IGesture mGesture;
    private MapFrameLayoutStatusListener mMapFrameLayoutStatusListener;

    public MapFrameLayout(@NonNull Context context) {
        super(context);
    }

    public MapFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public MapFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs,
                          int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public static MapFrameLayout getNoMapView(Map map, Context context) {
        MapFrameLayout layout = new MapFrameLayout(context);
        layout.setLayoutParams(
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        layout.setComponent(map);

        TextView view = new TextView(context);
        view.setText(R.string.no_map);
        view.setGravity(Gravity.CENTER);
        layout.addView(view);

        return layout;
    }

    @Override
    public Component getComponent() {
        return mComponent;
    }

    @Override
    public void setComponent(Component component) {
        mComponent = component;
    }

    public void setMapFrameLayoutStatusListener(
            MapFrameLayoutStatusListener mapFrameLayoutStatusListener) {
        this.mMapFrameLayoutStatusListener = mapFrameLayoutStatusListener;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mMapFrameLayoutStatusListener != null) {
            mMapFrameLayoutStatusListener.onMapFrameLayoutDetachedFromWindow();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mMapFrameLayoutStatusListener != null) {
            mMapFrameLayoutStatusListener.onMapFrameLayoutAttachedToWindow();
        }
    }

    @Override
    public IGesture getGesture() {
        return mGesture;
    }

    @Override
    public void setGesture(IGesture gestureDelegate) {
        mGesture = gestureDelegate;
    }

    /**
     * onTouchEvent中无法收到事件，事件会被地图sdk中的组件给消费掉, 所以此处写在dispatchTouchEvent中处理
     *
     * @param ev
     * @return
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        boolean result = super.dispatchTouchEvent(ev);
        if (mGesture != null) {
            result |= mGesture.onTouch(ev);
        }
        return result;
    }

    /**
     * 地图组件的滑动事件，都由地图组件处理，阻止Scroller Component劫持
     *
     * @param ev
     * @return
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_UP
                || ev.getAction() == MotionEvent.ACTION_CANCEL) {
            requestDisallowInterceptTouchEvent(false);
        } else {
            requestDisallowInterceptTouchEvent(true);
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean result = super.onKeyDown(keyCode, event);
        return onKey(KeyEvent.ACTION_DOWN, keyCode, event, result);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        boolean result = super.onKeyUp(keyCode, event);
        return onKey(KeyEvent.ACTION_UP, keyCode, event, result);
    }

    private boolean onKey(int keyAction, int keyCode, KeyEvent event, boolean result) {
        if (mKeyEventDelegate == null) {
            mKeyEventDelegate = new KeyEventDelegate(mComponent);
        }
        result |= mKeyEventDelegate.onKey(keyAction, keyCode, event);
        return result;
    }

    public interface MapFrameLayoutStatusListener {
        void onMapFrameLayoutAttachedToWindow();

        void onMapFrameLayoutDetachedFromWindow();
    }
}

/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package androidx.leanback.widget;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import org.hapjs.component.Component;
import org.hapjs.component.view.ComponentHost;
import org.hapjs.component.view.NestedScrollingListener;
import org.hapjs.component.view.NestedScrollingView;
import org.hapjs.component.view.gesture.GestureHost;
import org.hapjs.component.view.gesture.IGesture;
import org.hapjs.widgets.view.list.FlexLayoutManager;
import org.hapjs.widgets.view.list.RecyclerViewAdapter;

public class GridViewAdapterImpl extends BaseGridView implements ComponentHost,
        NestedScrollingView, GestureHost, RecyclerViewAdapter {

    public final static int FOCUS_SCROLL_ALIGNED = 0;
    public final static int FOCUS_SCROLL_ITEM = 1;
    public final static int FOCUS_SCROLL_PAGE = 2;

    private Component mComponent;
    private IGesture mGesture;
    private GridLayoutManagerAdapterImpl mGridLayoutManagerAdapter;

    public GridViewAdapterImpl(Context context) {
        super(context, null, -1);
        mGridLayoutManagerAdapter = new GridLayoutManagerAdapterImpl(mLayoutManager);
        mLayoutManager.setFocusOutAllowed(true, true);
        mLayoutManager.setFocusOutSideAllowed(true, true);
    }

    @Override
    public RecyclerView getActualRecyclerView() {
        return this;
    }

    @Override
    public void setScrollPage(boolean scrollPage) {
        //do nothing
    }

    @Override
    public void setComponent(Component component) {
        mComponent = component;
    }

    @Override
    public Component getComponent() {
        return mComponent;
    }

    @Override
    public void resumeRequestLayout() {
        // do not support
    }

    @Override
    public View getMoveableView() {
        return null;
    }

    @Override
    public void setDirty(boolean dirty) {
        //do not support.
    }

    @Override
    public State getState() {
        return mLayoutManager.mState;
    }

    @Override
    public boolean shouldScrollFirst(int dy, int velocityY) {
        return false;
    }

    @Override
    public boolean nestedFling(int velocityX, int velocityY) {
        return fling(0, velocityY);
    }

    @Override
    public void setNestedScrollingListener(NestedScrollingListener listener) {
        //do not support.
    }

    @Override
    public NestedScrollingListener getNestedScrollingListener() {
        return null;
    }

    @Override
    public ViewGroup getChildNestedScrollingView() {
        return null;
    }

    @Override
    public void setGesture(IGesture gestureDelegate) {
        mGesture = gestureDelegate;
    }

    @Override
    public IGesture getGesture() {
        return mGesture;
    }

    @Override
    public void setFocusScrollStrategy(int scrollStrategy) {
        if (scrollStrategy != FOCUS_SCROLL_ALIGNED && scrollStrategy != FOCUS_SCROLL_ITEM
                && scrollStrategy != FOCUS_SCROLL_PAGE) {
            throw new IllegalArgumentException("Invalid scrollStrategy");
        }
        mLayoutManager.setFocusScrollStrategy(scrollStrategy);
        requestLayout();
    }

    public FlexLayoutManager getGridLayoutManagerAdapter() {
        return mGridLayoutManagerAdapter;
    }
}
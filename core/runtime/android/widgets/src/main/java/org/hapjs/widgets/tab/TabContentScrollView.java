/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.tab;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import androidx.viewpager.widget.ViewPager;
import org.hapjs.component.Component;
import org.hapjs.component.view.ComponentHost;
import org.hapjs.component.view.SwipeDelegate;
import org.hapjs.widgets.view.HapNestedScrollView;

public class TabContentScrollView extends HapNestedScrollView {
    private static final String TAG = "TabContentScrollView";
    SwipeDelegate mParentViewSwipeDelegate = null;

    public TabContentScrollView(Context context) {
        super(context);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                View parentView = null;
                if (getParent() instanceof ViewPager) {
                    parentView = (View) getParent();
                }
                if (parentView instanceof ComponentHost) {
                    Component component = ((ComponentHost) parentView).getComponent();
                    mParentViewSwipeDelegate = component.getSwipeDelegate();
                }
                break;
            }
            case MotionEvent.ACTION_CANCEL: {
                mParentViewSwipeDelegate = null;
                break;
            }
            default:
                break;
        }
        if (mParentViewSwipeDelegate != null) {
            mParentViewSwipeDelegate.onTouchEvent(ev);
        }
        return super.onTouchEvent(ev);
    }
}

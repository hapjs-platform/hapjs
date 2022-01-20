/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component;

import android.content.Context;
import android.graphics.Canvas;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import java.util.Map;
import org.hapjs.common.utils.DisplayUtil;
import org.hapjs.common.utils.ViewUtils;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.component.view.ScrollView;
import org.hapjs.render.vdom.DocComponent;
import org.hapjs.runtime.HapEngine;

public class Scroller extends AbstractScrollable<ScrollView>
        implements ScrollView.ScrollViewListener {
    private static final String TAG = "Scroller";
    private static final String KEY_SCROLL_Y = "scroll_y";

    private ScrollView mScrollView;

    public Scroller(
            HapEngine hapEngine,
            Context context,
            Container parent,
            int ref,
            RenderEventCallback callback,
            Map<String, Object> savedState) {
        super(hapEngine, context, parent, ref, callback, savedState);
    }

    @Override
    protected ScrollView createViewImpl() {
        mScrollView =
                new ScrollView(mContext) {
                    @Override
                    public void draw(Canvas canvas) {
                        super.draw(canvas);
                        mCallback.onPostRender();
                    }
                };
        ViewGroup.LayoutParams lp =
                new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.MATCH_PARENT,
                        RelativeLayout.LayoutParams.MATCH_PARENT);
        ViewUtils.fitParentLayoutParams(lp, mParent == null ? null : mParent.getInnerView());
        mScrollView.setLayoutParams(lp);

        mScrollView.addScrollViewListener(this);

        return mScrollView;
    }

    @Override
    public void addChild(Component child, int index) {
        if (getChildCount() > 0) {
            throw new IllegalStateException("ScrollView can host only one direct child");
        }
        super.addChild(child, index);
    }

    @Override
    public void addView(View childView, int index) {
        ViewGroup.LayoutParams lp = childView.getLayoutParams();
        if (lp == null) {
            lp =
                    new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT);
        } else {
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
            lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
        }
        childView.setLayoutParams(lp);
        DocComponent docComponent = getRootComponent();
        if (docComponent != null) {
            docComponent.setViewId(DocComponent.ROOT_VIEW_ID, childView.getId());
        } else {
            Log.e(TAG, "addView: docComponent is null");
        }

        super.addView(childView, index);
    }

    @Override
    protected void onSaveInstanceState(Map<String, Object> outState) {
        super.onSaveInstanceState(outState);
        if (mHost == null) {
            return;
        }
        outState.put(KEY_SCROLL_Y, mHost.getScrollY());
    }

    @Override
    protected void onRestoreInstanceState(Map<String, Object> savedState) {
        super.onRestoreInstanceState(savedState);
        if (savedState == null) {
            return;
        }

        if (savedState.get(KEY_SCROLL_Y) != null) {
            mHost.setScrollY((int) savedState.get(KEY_SCROLL_Y));
        }
    }

    @Override
    public void onScrollChanged(ScrollView scrollView, int x, int y, int oldx, int oldy) {
        processAppearanceEvent();
        int yRealPx = Math.round(DisplayUtil.getRealPxByWidth(y, mHapEngine.getDesignWidth()));
        mCallback.onPageScroll(yRealPx);
        if (scrollView.getChildCount() <= 0) {
            Log.d(TAG, "onScrollChanged child is null");
            return;
        }
        if (y == 0) {
            mCallback.onPageReachTop();
        }
        View layout = scrollView.getChildAt(0);
        if (layout.getHeight() == scrollView.getHeight() + y) {
            mCallback.onPageReachBottom();
        }
    }

    public void scrollTo(int y) {
        if (mHost != null) {
            mHost.scrollTo(0, y);
        }
    }

    public void smoothScrollTo(int y) {
        if (mHost != null) {
            mHost.smoothScrollTo(0, y);
        }
    }

    public void scrollBy(int y) {
        if (mHost != null) {
            mHost.scrollBy(0, y);
        }
    }

    public void smoothScrollBy(int y) {
        if (mHost != null) {
            mHost.smoothScrollBy(0, y);
        }
    }
}

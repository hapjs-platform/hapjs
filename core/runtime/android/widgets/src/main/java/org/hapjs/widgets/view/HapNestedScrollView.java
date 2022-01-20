/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.view;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import org.hapjs.component.view.NestedScrollingListener;
import org.hapjs.component.view.NestedScrollingView;
import org.hapjs.component.view.ScrollView;
import org.hapjs.widgets.view.utils.ScrollableViewUtil;

public class HapNestedScrollView extends ScrollView implements NestedScrollingView {

    private ViewGroup mNestedChildView;
    private NestedScrollingListener mNestedScrollingListener;

    public HapNestedScrollView(Context context) {
        super(context);
    }

    @Override
    public boolean onStartNestedScroll(
            @NonNull View child, @NonNull View target, int axes, int type) {
        if (target instanceof ViewGroup) {
            mNestedChildView = (ViewGroup) target;
        }
        return super.onStartNestedScroll(child, target, axes, type);
    }

    @Override
    public boolean shouldScrollFirst(int dy, int velocityY) {
        if (mNestedChildView instanceof NestedScrollingView) {
            if (((NestedScrollingView) mNestedChildView).shouldScrollFirst(dy, velocityY)) {
                return true;
            }
        }

        boolean toBottom = dy > 0 || velocityY > 0;
        boolean toTop = dy < 0 || velocityY < 0;
        if (toBottom && ScrollableViewUtil.isNestedScrollViewToBottom(this)) {
            return false;
        }

        if (toTop && ScrollableViewUtil.isNestedScrollViewToTop(this)) {
            return false;
        }
        return true;
    }

    @Override
    public boolean nestedFling(int velocityX, int velocityY) {
        if (mNestedChildView instanceof NestedScrollingView) {
            if (((NestedScrollingView) mNestedChildView).nestedFling(velocityX, velocityY)) {
                return true;
            }
        }
        boolean toBottom = velocityY > 0;
        boolean toTop = velocityY < 0;
        if (toBottom && ScrollableViewUtil.isNestedScrollViewToBottom(this)) {
            return false;
        }

        if (toTop && ScrollableViewUtil.isNestedScrollViewToTop(this)) {
            return false;
        }

        fling(velocityY);
        return true;
    }

    @Override
    public NestedScrollingListener getNestedScrollingListener() {
        return mNestedScrollingListener;
    }

    @Override
    public void setNestedScrollingListener(NestedScrollingListener listener) {
        mNestedScrollingListener = listener;
    }

    @Override
    public ViewGroup getChildNestedScrollingView() {
        return mNestedChildView;
    }
}

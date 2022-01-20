/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.view.refresh;

import android.view.View;

public class Header extends RefreshExtension {

    private IHeaderView mHeaderView;

    public Header(IHeaderView headerView) {
        super(headerView.get());
        mHeaderView = headerView;
        setTriggerRefreshRatio(DEFAULT_TRIGGER_REFRESH_RATIO);
        setMaxDragRatio(DEFAULT_MAX_DRAG_RATIO);
        setDisplayRatio(DEFAULT_DISPLAY_RATIO);
        setTranslationWithContentWhenRefreshing(true);
        headerView.bindExpand(this);
    }

    @Override
    public boolean canTranslationWithContentWhenRefreshing() {
        int style = getStyle();
        if (style == STYLE_FIXED_BEHIND) {
            return false;
        }
        return super.canTranslationWithContentWhenRefreshing();
    }

    @Override
    public void onMove(float moveY, float percent, boolean isDrag, boolean refresh) {
        View headerView = getView();
        headerView.setTranslationY(moveY);
        mHeaderView.onMove(this, moveY, percent, isDrag, refresh);
    }

    @Override
    public float calculateStickyMoving(float distance) {
        if (distance < 0) {
            distance = 0;
        }
        int measuredHeight = getMeasureHeight();
        if (measuredHeight <= 0) {
            return distance;
        }

        final float M = getMeasuredDragMaxSize();
        final float H = measuredHeight;
        final float x = Math.max(0, distance * getDragRate());
        // 公式 y = M(1-100^(-x/H))
        final float y = (float) Math.min(M * (1 - Math.pow(100, -x / (H == 0 ? 1 : H))), x);
        return y;
    }

    @Override
    public void onStateChanged(RefreshState state, int oldState, int currentState) {
        mHeaderView.onStateChanged(state, oldState, currentState);
    }
}

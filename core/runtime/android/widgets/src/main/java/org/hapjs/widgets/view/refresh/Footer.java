/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.view.refresh;

import android.view.View;

public class Footer extends RefreshExtension {

    private IFooterView mFooterView;

    public Footer(IFooterView footerView) {
        super(footerView.get());
        mFooterView = footerView;
        setTriggerRefreshRatio(DEFAULT_TRIGGER_REFRESH_RATIO);
        setMaxDragRatio(DEFAULT_MAX_DRAG_RATIO);
        setDisplayRatio(1f);
        setTranslationWithContentWhenRefreshing(true);
        footerView.bindExpand(this);
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
        View footerView = getView();
        footerView.setTranslationY(moveY);
        mFooterView.onMove(this, moveY, percent, isDrag, refresh);
    }

    @Override
    public float calculateStickyMoving(float distance) {
        if (distance > 0) {
            distance = 0;
        }
        int measuredHeight = getMeasureHeight();
        if (measuredHeight <= 0) {
            return distance;
        }
        final float M = getMeasuredDragMaxSize();
        final float H = measuredHeight;
        final float x = -Math.min(0, distance * getDragRate());
        // 公式 y = M(1-100^(-x/H))
        final float y = (float) -Math.min(M * (1 - Math.pow(100, -x / (H == 0 ? 1 : H))), x);
        return y;
    }

    @Override
    public void onStateChanged(RefreshState state, int oldState, int currentState) {
        mFooterView.onStateChanged(state, oldState, currentState);
    }
}

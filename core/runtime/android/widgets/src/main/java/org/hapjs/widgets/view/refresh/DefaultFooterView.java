/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.view.refresh;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import androidx.annotation.NonNull;

public class DefaultFooterView extends RelativeLayout implements IFooterView<DefaultFooterView> {

    private final BallLoadingView mLoadingView;
    private RefreshExtension mRefreshExtension;

    public DefaultFooterView(Context context) {
        super(context);
        mLoadingView = new BallLoadingView(context);

        RelativeLayout.LayoutParams params =
                new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        addView(mLoadingView, params);
    }

    @NonNull
    @Override
    public DefaultFooterView get() {
        return this;
    }

    @Override
    public void bindExpand(RefreshExtension extension) {
        mRefreshExtension = extension;
    }

    public void setLoadingColor(int color) {
        mLoadingView.setIndicatorColor(color);
    }

    @Override
    public void onMove(
            RefreshMovement movement, float moveY, float percent, boolean isDrag,
            boolean isRefreshing) {
    }

    @Override
    public void onStateChanged(RefreshState state, int oldState, int currentState) {
    }

    @Override
    public boolean apply(String name, Object attribute) {
        return false;
    }
}

/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.view.refresh;

import android.content.Context;
import android.graphics.Color;
import android.widget.RelativeLayout;
import androidx.annotation.NonNull;
import androidx.swiperefreshlayout.widget.CircularProgressDrawable;

public class DefaultHeaderView extends RelativeLayout implements IHeaderView<DefaultHeaderView> {

    // Default background for the progress spinner
    private static final int CIRCLE_BG_LIGHT = 0xFFFAFAFA;

    private static final int CIRCLE_DIAMETER = 40;

    private static final float MAX_PROGRESS_ANGLE = .8f;

    private final CircularProgressDrawable mProgress;
    private final CircleImageView mCircleImageView;
    private int mCircleDiameter;
    private RefreshExtension mRefreshExtension;

    public DefaultHeaderView(Context context) {
        super(context);

        setMinimumHeight(dp2px(CIRCLE_DIAMETER));

        mCircleDiameter =
                (int) (CIRCLE_DIAMETER * context.getResources().getDisplayMetrics().density);

        mCircleImageView = new CircleImageView(context, CIRCLE_BG_LIGHT);
        mProgress = new CircularProgressDrawable(context);
        mProgress.setColorSchemeColors(Color.BLACK);
        mProgress.setStyle(CircularProgressDrawable.DEFAULT);
        mCircleImageView.setImageDrawable(mProgress);

        RelativeLayout.LayoutParams params = new LayoutParams(mCircleDiameter, mCircleDiameter);
        params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        addView(mCircleImageView, params);
    }

    private int dp2px(float dpValue) {
        return (int) (0.5f + dpValue * getContext().getResources().getDisplayMetrics().density);
    }

    @NonNull
    @Override
    public DefaultHeaderView get() {
        return this;
    }

    @Override
    public void bindExpand(RefreshExtension extension) {
        mRefreshExtension = extension;
    }

    @Override
    public void onMove(
            RefreshMovement movement, float moveY, float percent, boolean isDrag,
            boolean isRefreshing) {
        if (!(movement instanceof RefreshExtension)) {
            return;
        }

        RefreshExtension expand = (RefreshExtension) movement;
        int mTotalDragDistance = expand.getMeasuredTriggerRefreshSize();
        int height = expand.getMeasureHeight();
        if (mTotalDragDistance == 0 || height == 0) {
            return;
        }
        mProgress.setArrowEnabled(true);

        float originalDragPercent = moveY / mTotalDragDistance;

        float dragPercent = Math.min(1f, Math.abs(originalDragPercent));
        float adjustedPercent = (float) Math.max(dragPercent - .4, 0) * 5 / 3;
        float extraOS = Math.abs(moveY) - mTotalDragDistance;
        float tensionSlingshotPercent = Math.max(0, Math.min(extraOS, height * 2) / height);
        float tensionPercent =
                (float) ((tensionSlingshotPercent / 4)
                        - Math.pow((tensionSlingshotPercent / 4), 2)) * 2f;

        float strokeStart = adjustedPercent * .8f;
        mProgress.setStartEndTrim(0f, Math.min(MAX_PROGRESS_ANGLE, strokeStart));
        mProgress.setArrowScale(Math.min(1f, adjustedPercent));

        float rotation = (-0.25f + .4f * adjustedPercent + tensionPercent * 2) * .5f;
        mProgress.setProgressRotation(rotation);
    }

    @Override
    public void onStateChanged(RefreshState state, int oldState, int currentState) {
        if (currentState == RefreshState.PULL_DOWN_REFRESHING) {
            mProgress.start();
        } else {
            mProgress.stop();
        }
    }

    public void setSpinnerColor(int color) {
        mCircleImageView.setBackgroundColor(color);
    }

    public void setProgressColor(int color) {
        mProgress.setColorSchemeColors(color);
    }

    @Override
    public boolean apply(String name, Object attribute) {
        return false;
    }
}

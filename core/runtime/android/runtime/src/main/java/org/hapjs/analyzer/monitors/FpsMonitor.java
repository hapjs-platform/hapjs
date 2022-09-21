/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.analyzer.monitors;

import android.content.Context;
import android.util.Log;
import android.view.Choreographer;
import android.view.WindowManager;

import org.hapjs.analyzer.monitors.abs.AbsTimerMonitor;

public class FpsMonitor extends AbsTimerMonitor<Integer> implements Choreographer.FrameCallback {
    private static final String TAG = "FpsMonitor";
    public static final String NAME = "fps";
    private static final float TIME_MILLIS_TO_NANO = 1000_000f;
    private long mLastFrameTimeNanos;
    private float mFrameIntervalMs;
    private int mFrameValid;
    private int mFrameTotal;
    private boolean mIsEnable;
    private float mRefreshRate;

    public FpsMonitor() {
        super(NAME, true);
        mRefreshRate = getRefreshRate();
        if (mRefreshRate > 0) {
            mFrameIntervalMs = 1000 / mRefreshRate;
            mIsEnable = true;
        } else {
            mIsEnable = false;
        }
    }

    private float getRefreshRate() {
        WindowManager windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        if (windowManager != null) {
            return windowManager.getDefaultDisplay().getRefreshRate();
        } else {
            Log.e(TAG, "AnalyzerPanel_LOG fail to get refresh rate");
            return 0;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mIsEnable) {
            Choreographer.getInstance().postFrameCallback(this);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Choreographer.getInstance().removeFrameCallback(this);
    }

    @Override
    protected void loop() {
        if (isAppVisible() && mIsEnable) {
            int fps = (int) Math.min(mRefreshRate, mRefreshRate * (float) mFrameValid / mFrameTotal);
            clean();
            Pipeline<Integer> pipeline = getPipeline();
            if (pipeline == null) {
                return;
            }
            pipeline.output(fps);
        }
    }

    private void clean() {
        mFrameValid = 0;
        mFrameTotal = 0;
        mLastFrameTimeNanos = 0;
    }

    @Override
    public void doFrame(long frameTimeNanos) {
        collect(frameTimeNanos);
        if (isRunning()) {
            Choreographer.getInstance().postFrameCallback(this);
        }
    }

    @Override
    public boolean isRunning() {
        return super.isRunning() && mIsEnable;
    }

    private void collect(long frameTimeNanos) {
        if (mLastFrameTimeNanos == 0) {
            mLastFrameTimeNanos = frameTimeNanos;
            return;
        }
        int dropFrame = (int) ((frameTimeNanos - mLastFrameTimeNanos) / TIME_MILLIS_TO_NANO / mFrameIntervalMs);
        mFrameTotal += (dropFrame + 1);
        mFrameValid++;
        mLastFrameTimeNanos = frameTimeNanos;
    }
}

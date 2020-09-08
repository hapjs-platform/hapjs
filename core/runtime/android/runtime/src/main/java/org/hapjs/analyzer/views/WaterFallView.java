/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hapjs.analyzer.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * Waterfall view of network request
 */
public class WaterFallView extends View {
    private static final int MIN_VISIBLE_PIXELS = 3;
    private long mStartTime = -1;
    private long mLatestTime = -1;
    private long mSentTime = -1; // connect start
    private long mResReceivedTime = -1; // download start
    private long mEndTime = -1; // download end
    private boolean mEnabled = false;
    private Paint mPaintAll = new Paint();
    private Paint mPaintDownload = new Paint();

    public WaterFallView(Context context) {
        super(context);
        init();
    }

    private void init() {
        mPaintAll.setColor(Color.WHITE);
        mPaintAll.setAntiAlias(true);
        mPaintAll.setStyle(Paint.Style.FILL);
        mPaintDownload.setColor(0xFF456FFF);
        mPaintDownload.setAntiAlias(true);
        mPaintDownload.setStyle(Paint.Style.FILL);
    }

    public WaterFallView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public WaterFallView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void updateTime(long startTime, long latestTime, long sentTime, long resReceivedTime, long endTime){
        if (startTime >= latestTime) {
            disabled();
            return;
        }
        this.mStartTime = startTime;
        this.mLatestTime = latestTime;
        this.mSentTime = sentTime;
        this.mResReceivedTime = resReceivedTime;
        this.mEndTime = endTime;
        mEnabled = true;
        postInvalidate();
    }

    public void disabled (){
        this.mStartTime = -1;
        this.mLatestTime = -1;
        this.mSentTime = -1;
        this.mResReceivedTime = -1;
        this.mEndTime = -1;
        mEnabled = false;
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mEnabled && mStartTime <= mSentTime && mSentTime <= mResReceivedTime && mResReceivedTime <= mEndTime && mEndTime <= mLatestTime && mStartTime < mLatestTime) {
            int width = getWidth();
            int height = getHeight();
            if (width > 0 && height > 0) {
                float left = width * 1f * (mSentTime - mStartTime) / (mLatestTime - mStartTime);
                float right = width * 1f * (mEndTime - mStartTime) / (mLatestTime - mStartTime);
                float download = width * 1f * (mResReceivedTime - mStartTime) / (mLatestTime - mStartTime);
                if (right - left < MIN_VISIBLE_PIXELS) {
                    float tmpLeft = left, tmpRight, tmpDownload;
                    // check minimum visible width
                    tmpRight = left + MIN_VISIBLE_PIXELS;
                    if (tmpRight > width) {
                        tmpRight = width;
                        tmpLeft = tmpRight - MIN_VISIBLE_PIXELS;
                    } else if (left < 0) {
                        tmpLeft = 0;
                        tmpRight = MIN_VISIBLE_PIXELS;
                    }
                    tmpDownload = tmpLeft + (download - left) / (right - left) * (tmpRight - tmpLeft);
                    left = tmpLeft;
                    right = tmpRight;
                    download = tmpDownload;
                }
                if (left <= download) {
                    float bottomAll = height * 3f / 5f;
                    float topAll = height * 2f / 5f;
                    canvas.drawRect(left, topAll, download, bottomAll, mPaintAll);
                }
                if (download <= right) {
                    float bottomDownload = height * 5f / 8f;
                    float topDownload = height * 3f / 8f;
                    canvas.drawRect(download, topDownload, right, bottomDownload, mPaintDownload);
                }
            }
        }
    }
}

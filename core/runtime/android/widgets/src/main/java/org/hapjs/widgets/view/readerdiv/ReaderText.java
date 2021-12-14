/*
 * Copyright (c) 2023, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hapjs.widgets.view.readerdiv;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.RectF;
import android.text.Layout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.appcompat.widget.AppCompatTextView;

import java.util.ArrayList;
import java.util.List;


public class ReaderText extends AppCompatTextView {


    private final static String TAG = "ReaderText";
    private List<String> mPageData = new ArrayList<>();
    private int mLineY;
    private String mText = null;
    private int mLineHeight = 0;
    private float mTextSize = 0;
    private int mTextColor = Color.BLACK;
    private int mMaxLines = 0;
    private ReaderPageView.PageCallback mPageCallback = null;

    public ReaderText(Context context, int bgColor) {
        super(context);
        mBgColor = bgColor;
        initDefaultReaderText();
    }

    public void initReaderText(int textColor, int bgColor) {
        mTextColor = textColor;
        mBgColor = bgColor;
        mIsLineBgInvalid = false;
        setTextColor(mTextColor);
        setBgColor(mBgColor);
    }

    public void initDefaultReaderText() {
        setTextColor(mTextColor);
        setBgColor(mBgColor);
    }

    public ReaderText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ReaderText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setPageCallback(ReaderPageView.PageCallback pageCallback) {
        this.mPageCallback = pageCallback;
    }

    public List<String> getPageData() {
        return mPageData;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mPageCallback = null;
        mLastDownMotionEvent = null;
    }

    public void setReaderPageData(List<String> datas) {
        mPageData.clear();
        mPageData.addAll(datas);
        mText = getDrawPageString(mPageData);
        setText(mText);
    }

    @Override
    public void setTextSize(float size) {
        mTextSize = size;
        super.setTextSize(size);

    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        float realWidth = MeasureSpec.getSize(widthMeasureSpec);
        float realHeight = MeasureSpec.getSize(heightMeasureSpec);
        if (realWidth == 0 || realHeight == 0) {
            Log.w(TAG, ReaderLayoutView.READER_LOG_TAG + "onMeasure realWidth or realHeight is not valid , realWidth : " + realWidth
                    + " realHeight : " + realHeight);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public void setTextSize(int unit, float size) {
        mTextSize = size;
        super.setTextSize(unit, size);
    }

    @Override
    public void setTextColor(int color) {
        super.setTextColor(color);
        mTextColor = color;
    }

    @Override
    public void setMaxLines(int maxLines) {
        mMaxLines = maxLines;
        super.setMaxLines(maxLines);
    }

    @Override
    public void setLineHeight(int lineHeight) {
        mLineHeight = lineHeight;
        super.setLineHeight(lineHeight);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        //设置点击导致无法透传
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                if (null != mPageCallback) {
                    mPageCallback.onClickCallback(mLastDownMotionEvent);
                } else {
                    Log.w(TAG, ReaderLayoutView.READER_LOG_TAG + "onAttachedToWindow onClick mPageCallback is null.");
                }

            }
        });
    }

    private MotionEvent mLastDownMotionEvent = null;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            mLastDownMotionEvent = event;
        }
        return super.onTouchEvent(event);
    }

    public String getDrawPageString(List<String> pagelineDatas) {
        if (null == pagelineDatas) {
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder();
        int size = pagelineDatas.size();
        for (int i = 0; i < size; i++) {
            stringBuilder.append(pagelineDatas.get(i));
        }
        String pageData = stringBuilder.toString();
        return pageData;
    }


    @Override
    protected void onLayout(boolean changed, int left, int top, int right,
                            int bottom) {
        super.onLayout(changed, left, top, right, bottom);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        TextPaint paint = getPaint();
        paint.setTextSize(mTextSize);
        paint.drawableState = getDrawableState();
        CharSequence tmpChars = getText();
        String text = tmpChars.toString();
        mLineY = 0;
        Layout layout = getLayout();
        if (null == layout) {
            Log.w(TAG, ReaderLayoutView.READER_LOG_TAG + "onDraw error layout is null.");
            return;
        }
        int hasDrawCount = 0;
        for (int i = 0; i < layout.getLineCount(); i++) {
            hasDrawCount++;
            int lineStart = layout.getLineStart(i);
            int lineEnd = layout.getLineEnd(i);
            String line = text.substring(lineStart, lineEnd);
            if (null != line && i < mPageData.size()) {
                String lineReal = line.trim();
                String pageLineStr = mPageData.get(i);
                if (null != lineReal
                        && null != pageLineStr
                        && !lineReal.equals(pageLineStr)) {
                    //可以限定是非段尾情况
                    canvas.save();
                    canvas.clipRect(new RectF(0, mLineY, getWidth(), mLineY + mLineHeight));
                    canvas.drawColor(mBgColor);
                    canvas.drawText(pageLineStr, 0, mLineY + mTextSize, paint);
                    canvas.restore();
                } else {
                    //canvas.drawColor(mBgColor);
                }
                //背景色
                if (!TextUtils.isEmpty(pageLineStr) && mIsLineBgInvalid && i >= mStartLine && i < mEndLine) {
                    canvas.save();
                    canvas.clipRect(new RectF(0, mLineY, getWidth(), mLineY + mLineHeight));
                    canvas.drawColor(mLineBgColor);
                    canvas.drawText(pageLineStr, 0, mLineY + mTextSize, paint);
                    //canvas.drawColor(Color.BLUE);
                    canvas.restore();
                    //仅生效当前一次
                }
            } else {
                if (null != line && i >= mPageData.size()) {
                    canvas.save();
                    canvas.clipRect(new RectF(0, mLineY, getWidth(), mLineY + mLineHeight));
                    canvas.drawColor(mBgColor);
                    canvas.restore();
                }
            }
            mLineY += getLineHeight();
        }
        if (hasDrawCount > 0 && mPageData.size() > 0
                && hasDrawCount < (mPageData.size() - 1)) {
            Log.w(TAG, ReaderLayoutView.READER_LOG_TAG + "onDraw error hasDrawCount  : " + hasDrawCount
                    + " mPageData.size() : " + mPageData.size()
                    + " Layout count : " + (null != layout ? layout.getLineCount() : "null layout"));
        }
    }


    private boolean mIsLineBgInvalid = false;
    private int mStartLine = -1;
    private int mEndLine = -1;
    private int mBgColor = Color.WHITE;
    private int mLineBgColor = Color.WHITE;

    public void setBgColor(int bgColor) {
        if (bgColor == mTextColor) {
            Log.w(TAG, ReaderLayoutView.READER_LOG_TAG + "setBgColor bgColor equals mTextColor, mTextColor : " + mTextColor);
        }
        mBgColor = bgColor;
        setBackgroundColor(mBgColor);
    }

    // 0 1可以，即可以大于最大行数
    public boolean setLineBgColor(int bgColor, int startLine, int endLine) {
        boolean isSuccess = false;
        if (bgColor == mTextColor) {
            Log.w(TAG, ReaderLayoutView.READER_LOG_TAG + "setLineBgColor bgColor equals mTextColor, mTextColor : " + mTextColor);
            return isSuccess;
        }
        if (startLine < 0 || startLine >= endLine || (startLine + 1) >= mPageData.size()) {
            Log.w(TAG, ReaderLayoutView.READER_LOG_TAG + "setLineBgColor startLine is invalid, startLine : " + startLine
                    + " mPageData size : " + mPageData.size());
            return isSuccess;
        }
        isSuccess = true;
        mIsLineBgInvalid = true;
        mStartLine = startLine;
        mEndLine = endLine;
        mLineBgColor = bgColor;
        invalidate();
        return isSuccess;
    }

}

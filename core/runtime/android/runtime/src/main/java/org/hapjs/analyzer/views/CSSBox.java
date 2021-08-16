/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.analyzer.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import org.hapjs.common.utils.DisplayUtil;

public class CSSBox extends View {

    private static final int DEFAULT_WIDTH = 200;
    private static final int DEFAULT_HEIGHT = 160;
    private static final int DEFAULT_SPACING_HORIZONAL = 20;
    private static final int DEFAULT_SPACING_VERTICAL = 20;
    private static final int MARGIN_COLOR = 0xFFF9CC9D;
    private static final int BORDER_COLOR = 0xFFFFEEBC;
    private static final int PADDING_COLOR = 0XFFC4DFB8;
    private static final int CONTENT_COLOR = 0xFFA0C6E8;
    private static final int TEXT_COLOR = 0xFF222222;
    private static final String TEXT_MARGIN = "margin";
    private static final String TEXT_BORDER = "border";
    private static final String TEXT_PADDING = "padding";
    private static final String TEXT_UNKNOWN = "?";
    private static final String TEXT_AUTO = "auto";
    private static final String TEXT_ZERO = "0";
    private static final String TEXT_NULL = "-";
    private Rect marginBound;
    private Rect borderBound;
    private Rect paddingBound;
    private Rect contentBound;
    private Paint mBackgroundPaint;
    private TextPaint mTextPaint;
    private DashPathEffect mPathEffect;
    private int mTextOffsetX;
    private int mTextOffsetY;
    private Rect mTempTextBound = new Rect();
    private boolean isNative;
    private String mMarginLeftText;
    private String mMarginTopText;
    private String mMarginRightText;
    private String mMarginBottomText;
    private String mBorderLeftText;
    private String mBorderTopText;
    private String mBorderRightText;
    private String mBorderBottomText;

    private String mPaddingLeftText;
    private String mPaddingTopText;
    private String mPaddingRightText;
    private String mPaddingBottomText;

    private String mWidthText;
    private String mHeightText;


    public CSSBox(Context context) {
        super(context);
        init(context);
    }

    public CSSBox(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CSSBox(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setMinimumWidth(DisplayUtil.dip2Pixel(context, DEFAULT_WIDTH));
        setMinimumHeight(DisplayUtil.dip2Pixel(context, DEFAULT_HEIGHT));
        mBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBackgroundPaint.setStrokeWidth(DisplayUtil.dip2Pixel(context, 1));
        mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setColor(TEXT_COLOR);
        mTextPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, context.getResources().getDisplayMetrics()));
        mPathEffect = new DashPathEffect(new float[]{10, 10, 10, 10}, 0);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(getResolvedSize(getSuggestedMinimumWidth(), widthMeasureSpec),
                getResolvedSize(getSuggestedMinimumHeight(), heightMeasureSpec));
    }

    private int getResolvedSize(int size, int measureSpec) {
        int result = size;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        switch (specMode) {
            case MeasureSpec.UNSPECIFIED:
                result = size;
                break;
            case MeasureSpec.AT_MOST:
                result = Math.min(size, specSize);
                break;
            case MeasureSpec.EXACTLY:
                result = specSize;
                break;
            default:
        }
        return result;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();

        float scaleX = w * 1f / getMinimumWidth();
        float scaleY = h * 1f / getMinimumHeight();
        int spaceH = DisplayUtil.dip2Pixel(getContext(), (int) (DEFAULT_SPACING_HORIZONAL * scaleX));
        int spaceV = DisplayUtil.dip2Pixel(getContext(), (int) (DEFAULT_SPACING_VERTICAL * scaleY));

        marginBound = new Rect(paddingLeft, paddingTop, w - paddingRight, h - paddingBottom);
        borderBound = new Rect(paddingLeft + spaceH, paddingTop + spaceV, w - paddingRight - spaceH, h - paddingBottom - spaceV);
        paddingBound = new Rect(paddingLeft + spaceH * 2, paddingTop + spaceV * 2, w - paddingRight - spaceH * 2, h - paddingBottom - spaceV * 2);
        contentBound = new Rect(paddingLeft + spaceH * 3, paddingTop + spaceV * 3, w - paddingRight - spaceH * 3, h - paddingBottom - spaceV * 3);

        mTextOffsetX = spaceH / 4;
        Paint.FontMetricsInt fontMetrics = mTextPaint.getFontMetricsInt();
        mTextOffsetY = -(fontMetrics.ascent + fontMetrics.descent) / 2;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mBackgroundPaint.setStyle(Paint.Style.FILL);
        // margin background
        mBackgroundPaint.setColor(MARGIN_COLOR);
        canvas.drawRect(marginBound, mBackgroundPaint);
        // border background
        mBackgroundPaint.setColor(BORDER_COLOR);
        canvas.drawRect(borderBound, mBackgroundPaint);
        // padding background
        mBackgroundPaint.setColor(PADDING_COLOR);
        canvas.drawRect(borderBound, mBackgroundPaint);
        // content background
        mBackgroundPaint.setColor(CONTENT_COLOR);
        canvas.drawRect(contentBound, mBackgroundPaint);
        mBackgroundPaint.setStyle(Paint.Style.STROKE);
        mBackgroundPaint.setPathEffect(mPathEffect);
        mBackgroundPaint.setColor(Color.BLACK);
        canvas.drawRect(marginBound, mBackgroundPaint);
        mBackgroundPaint.setPathEffect(null);
        canvas.drawRect(borderBound, mBackgroundPaint);
        mBackgroundPaint.setPathEffect(mPathEffect);
        mBackgroundPaint.setColor(0xFF808080);
        canvas.drawRect(paddingBound, mBackgroundPaint);
        mBackgroundPaint.setPathEffect(null);
        canvas.drawRect(contentBound, mBackgroundPaint);
        // margin
        canvas.drawText(TEXT_MARGIN, marginBound.left + mTextOffsetX, marginBound.top + (marginBound.height() - borderBound.height()) / 4f + mTextOffsetY, mTextPaint);
        // margin-left
        String marginLeftText = calcText(mMarginLeftText, TEXT_ZERO, mTempTextBound);
        canvas.drawText(marginLeftText, marginBound.left + (marginBound.width() - borderBound.width()) / 4 - mTempTextBound.width() / 2,
                marginBound.top + marginBound.height() / 2f + mTextOffsetY, mTextPaint);
        // margin-top
        String marginTopText = calcText(mMarginTopText, TEXT_ZERO, mTempTextBound);
        canvas.drawText(marginTopText, marginBound.left + (marginBound.width() - mTempTextBound.width()) / 2,
                marginBound.top + (marginBound.height() - borderBound.height()) / 4f + mTextOffsetY, mTextPaint);
        // margin-right
        String marginRightText = calcText(mMarginRightText, TEXT_ZERO, mTempTextBound);
        canvas.drawText(marginRightText, borderBound.right + (marginBound.width() - borderBound.width()) / 4 - mTempTextBound.width() / 2,
                marginBound.top + marginBound.height() / 2f + mTextOffsetY, mTextPaint);
        // margin-bottom
        String marginBottomText = calcText(mMarginBottomText, TEXT_ZERO, mTempTextBound);
        canvas.drawText(marginBottomText, marginBound.left + (marginBound.width() - mTempTextBound.width()) / 2,
                borderBound.bottom + (marginBound.height() - borderBound.height()) / 4f + mTextOffsetY, mTextPaint);

        // border
        canvas.drawText(TEXT_BORDER, borderBound.left + mTextOffsetX, borderBound.top + (borderBound.height() - paddingBound.height()) / 4f + mTextOffsetY, mTextPaint);
        // border-left
        String borderLeftText = calcText(mBorderLeftText, isNative ? TEXT_NULL : TEXT_ZERO, mTempTextBound);
        canvas.drawText(borderLeftText, borderBound.left + (borderBound.width() - paddingBound.width()) / 4 - mTempTextBound.width() / 2,
                borderBound.top + borderBound.height() / 2f + mTextOffsetY, mTextPaint);
        // border-top
        String borderTopText = calcText(mBorderTopText, isNative ? TEXT_NULL : TEXT_ZERO, mTempTextBound);
        canvas.drawText(borderTopText, borderBound.left + (borderBound.width() - mTempTextBound.width()) / 2,
                borderBound.top + (borderBound.height() - paddingBound.height()) / 4f + mTextOffsetY, mTextPaint);
        // border-right
        String borderRightText = calcText(mBorderRightText, isNative ? TEXT_NULL : TEXT_ZERO, mTempTextBound);
        canvas.drawText(borderRightText, paddingBound.right + (borderBound.width() - paddingBound.width()) / 4 - mTempTextBound.width() / 2,
                borderBound.top + borderBound.height() / 2f + mTextOffsetY, mTextPaint);
        // border-bottom
        String borderBottomText = calcText(mBorderBottomText, isNative ? TEXT_NULL : TEXT_ZERO, mTempTextBound);
        canvas.drawText(borderBottomText, borderBound.left + (borderBound.width() - mTempTextBound.width()) / 2,
                paddingBound.bottom + (borderBound.height() - paddingBound.height()) / 4f + mTextOffsetY, mTextPaint);

        // padding
        canvas.drawText(TEXT_PADDING, paddingBound.left + mTextOffsetX, paddingBound.top + (paddingBound.height() - contentBound.height()) / 4f + mTextOffsetY, mTextPaint);
        //padding-left
        String paddingLeftText = calcText(mPaddingLeftText, TEXT_ZERO, mTempTextBound);
        canvas.drawText(paddingLeftText, paddingBound.left + (paddingBound.width() - contentBound.width()) / 4 - mTempTextBound.width() / 2,
                paddingBound.top + paddingBound.height() / 2f + mTextOffsetY, mTextPaint);
        // padding-top
        String paddingTopText = calcText(mPaddingTopText, TEXT_ZERO, mTempTextBound);
        canvas.drawText(paddingTopText, paddingBound.left + (paddingBound.width() - mTempTextBound.width()) / 2,
                paddingBound.top + (paddingBound.height() - contentBound.height()) / 4f + mTextOffsetY, mTextPaint);
        // padding-right
        String paddingRightText = calcText(mPaddingRightText, TEXT_ZERO, mTempTextBound);
        canvas.drawText(paddingRightText, contentBound.right + (paddingBound.width() - contentBound.width()) / 4 - mTempTextBound.width() / 2,
                paddingBound.top + paddingBound.height() / 2f + mTextOffsetY, mTextPaint);
        // padding-bottom
        String paddingBottomText = calcText(mPaddingBottomText, TEXT_ZERO, mTempTextBound);
        canvas.drawText(paddingBottomText, paddingBound.left + (paddingBound.width() - mTempTextBound.width()) / 2,
                contentBound.bottom + (paddingBound.height() - contentBound.height()) / 4f + mTextOffsetY, mTextPaint);

        // content
        String content = preText(mWidthText, TEXT_AUTO) + " x " + preText(mHeightText, TEXT_AUTO);
        content = calcText(content, "", mTempTextBound);
        canvas.drawText(content, contentBound.left + (contentBound.width() - mTempTextBound.width()) / 2,
                contentBound.top + contentBound.height() / 2f + mTextOffsetY, mTextPaint);
    }

    private String preText(String expireText, String placeholderText) {
        if (TextUtils.isEmpty(expireText)) {
            return placeholderText;
        } else {
            return expireText;
        }
    }

    private String calcText(String expireText, String placeholderText, Rect rect) {
        String text = preText(expireText, placeholderText);
        mTextPaint.getTextBounds(text, 0, text.length(), rect);
        return text;
    }

    public boolean isNative() {
        return isNative;
    }

    public void setNative(boolean aNative) {
        isNative = aNative;
    }

    public String getMarginLeftText() {
        return mMarginLeftText;
    }

    public void setMarginLeftText(String marginLeftText) {
        mMarginLeftText = marginLeftText;
    }

    public String getMarginTopText() {
        return mMarginTopText;
    }

    public void setMarginTopText(String marginTopText) {
        mMarginTopText = marginTopText;
    }

    public String getMarginRightText() {
        return mMarginRightText;
    }

    public void setMarginRightText(String marginRightText) {
        mMarginRightText = marginRightText;
    }

    public String getMarginBottomText() {
        return mMarginBottomText;
    }

    public void setMarginBottomText(String marginBottomText) {
        mMarginBottomText = marginBottomText;
    }

    public String getBorderLeftText() {
        return mBorderLeftText;
    }

    public void setBorderLeftText(String borderLeftText) {
        mBorderLeftText = borderLeftText;
    }

    public String getBorderTopText() {
        return mBorderTopText;
    }

    public void setBorderTopText(String borderTopText) {
        mBorderTopText = borderTopText;
    }

    public String getBorderRightText() {
        return mBorderRightText;
    }

    public void setBorderRightText(String borderRightText) {
        mBorderRightText = borderRightText;
    }

    public String getBorderBottomText() {
        return mBorderBottomText;
    }

    public void setBorderBottomText(String borderBottomText) {
        mBorderBottomText = borderBottomText;
    }

    public String getPaddingLeftText() {
        return mPaddingLeftText;
    }

    public void setPaddingLeftText(String paddingLeftText) {
        mPaddingLeftText = paddingLeftText;
    }

    public String getPaddingTopText() {
        return mPaddingTopText;
    }

    public void setPaddingTopText(String paddingTopText) {
        mPaddingTopText = paddingTopText;
    }

    public String getPaddingRightText() {
        return mPaddingRightText;
    }

    public void setPaddingRightText(String paddingRightText) {
        mPaddingRightText = paddingRightText;
    }

    public String getPaddingBottomText() {
        return mPaddingBottomText;
    }

    public void setPaddingBottomText(String paddingBottomText) {
        mPaddingBottomText = paddingBottomText;
    }

    public String getWidthText() {
        return mWidthText;
    }

    public void setWidthText(String widthText) {
        mWidthText = widthText;
    }

    public String getHeightText() {
        return mHeightText;
    }

    public void setHeightText(String heightText) {
        mHeightText = heightText;
    }

    public void update() {
        invalidate();
    }
}

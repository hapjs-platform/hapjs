/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.canvas.canvas2d;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import androidx.annotation.FloatRange;
import androidx.annotation.IntRange;
import java.util.Locale;
import org.hapjs.common.utils.DisplayUtil;
import org.hapjs.widgets.canvas.annotation.CanvasField;

public class CanvasContextState {

    private static final String TAG = "CanvasContextState";

    public Paint mFillPaint;
    public Paint mStrokePaint;
    public CanvasPath mPath;
    public float[] mLineDashSegments;
    public String mFillPattern;
    public Bitmap mFillPatternOriginBitmap;
    public Bitmap mStrokePatternOriginBitmap;
    public String mStrokePattern;
    public Bitmap mFillPatternBitmap;
    public Bitmap mStrokePatternBitmap;
    @CanvasField
    public float mGlobalAlpha;
    @CanvasField
    public int mFillStyleColor;
    @CanvasField
    public int mStrokeStyleColor;
    @CanvasField
    public CSSFont mFont;
    @CanvasField
    public String mGlobalCompositeOperation;
    @CanvasField
    public String mLineCap;
    @CanvasField
    public float mLineDashOffset;
    @CanvasField
    public String mLineJoin;
    @CanvasField
    public float mLineWidth;
    @CanvasField
    public float mMiterLimit;
    @CanvasField
    public float mShadowBlur;
    @CanvasField
    public int mShadowColor;
    @CanvasField
    public float mShadowOffsetX;
    @CanvasField
    public float mShadowOffsetY;
    @CanvasField
    public String mTextAlign;
    @CanvasField
    public String mTextBaseline;
    private CSSFont mDefaultFont;
    private int mDesignWidth;

    public CanvasContextState(int designWidth) {
        mDesignWidth = designWidth;
        mFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mFillPaint.setStyle(Paint.Style.FILL);

        mStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mStrokePaint.setStyle(Paint.Style.STROKE);

        mPath = new CanvasPath();
        mLineDashSegments = null;

        mGlobalAlpha = 1.0f;
        mFillStyleColor = Color.BLACK;
        mStrokeStyleColor = Color.BLACK;

        mLineCap = "butt";
        mLineDashOffset = 0;
        mLineJoin = "miter";
        mLineWidth = 1;
        mMiterLimit = 10;
        mShadowBlur = 0;
        mShadowColor = Color.TRANSPARENT;
        mShadowOffsetX = 0;
        mShadowOffsetY = 0;
        mTextAlign = "start";
        mTextBaseline = "alphabetic";

        // 设置默认值
        mDefaultFont = CSSFont.parse("sans-serif 10px");
        if (mDefaultFont != null) {
            mFont = mDefaultFont;
            mFillPaint.setTypeface(mFont.getTypeface());
            mStrokePaint.setTypeface(mFont.getTypeface());
            mFillPaint.setTextSize(getRealSize(mFont.getFontSize()));
            mStrokePaint.setTextSize(getRealSize(mFont.getFontSize()));
        }
        mStrokePaint.setStrokeCap(Paint.Cap.BUTT);
        mStrokePaint.setStrokeJoin(Paint.Join.MITER);
        mStrokePaint.setStrokeWidth(getRealSize(mLineWidth));
        mStrokePaint.setStrokeMiter(getRealSize(mMiterLimit));
        if (isRTL()) {
            mFillPaint.setTextAlign(Paint.Align.RIGHT);
            mStrokePaint.setTextAlign(Paint.Align.RIGHT);
        } else {
            mFillPaint.setTextAlign(Paint.Align.LEFT);
            mStrokePaint.setTextAlign(Paint.Align.LEFT);
        }
    }

    public CanvasContextState(CanvasContextState state) {
        mDesignWidth = state.mDesignWidth;
        mFillPaint = new Paint(state.mFillPaint);
        mStrokePaint = new Paint(state.mStrokePaint);
        mPath = new CanvasPath(state.mPath);
        mFont = state.mFont;
        mFillPattern = state.mFillPattern;
        mStrokePattern = state.mStrokePattern;

        mGlobalAlpha = state.mGlobalAlpha;
        mFillStyleColor = state.mFillStyleColor;
        mStrokeStyleColor = state.mStrokeStyleColor;
        mLineCap = state.mLineCap;
        mLineDashOffset = state.mLineDashOffset;
        mLineJoin = state.mLineJoin;
        mLineWidth = state.mLineWidth;
        mMiterLimit = state.mMiterLimit;
        mShadowBlur = state.mShadowBlur;
        mShadowColor = state.mShadowColor;
        mShadowOffsetX = state.mShadowOffsetX;
        mShadowOffsetY = state.mShadowOffsetY;
        mTextAlign = state.mTextAlign;
        mTextBaseline = state.mTextBaseline;

        mFillPaint.set(state.mFillPaint);
        mStrokePaint.set(state.mStrokePaint);
    }

    public void setGlobalAlpha(@FloatRange(from = 0.0f, to = 1.0f) float globalAlpha) {
        if (globalAlpha < 0f || globalAlpha > 1f) {
            Log.e(TAG, "setting globalAlpha out of range!");
            return;
        }
        Log.i(TAG, "set globalAlpha:" + globalAlpha);
        mGlobalAlpha = globalAlpha;

        mFillPaint.setColor(blendColor(mFillStyleColor, (int) (globalAlpha * 255)));
        mStrokePaint.setColor(blendColor(mStrokeStyleColor, (int) (globalAlpha * 255)));
    }

    public void setFillStyleColor(int color) {
        mFillStyleColor = color;
        mFillPaint.setColor(blendColor(mFillStyleColor, (int) (mGlobalAlpha * 255)));
    }

    public void setStrokeStyleColor(int color) {
        mStrokeStyleColor = color;
        mStrokePaint.setColor(blendColor(mStrokeStyleColor, (int) (mGlobalAlpha * 255)));
    }

    public int blendColor(int color, @IntRange(from = 0, to = 255) int alpha) {
        if (alpha >= 255) {
            return color;
        }
        if (alpha < 0) {
            alpha = 0;
        }
        return ((((color >>> 24) * alpha) / 255) << 24) | (0x00ffffff & color);
    }

    public boolean isRTL() {
        return TextUtils.getLayoutDirectionFromLocale(Locale.getDefault())
                == View.LAYOUT_DIRECTION_RTL;
    }

    public void reset() {
        mGlobalAlpha = 1;
        mStrokePaint.setStrokeWidth(getRealSize(1));

        mFillPaint.setColor(Color.BLACK);
        mStrokePaint.setColor(Color.BLACK);

        if (mDefaultFont != null) {
            mFillPaint.setTypeface(mDefaultFont.getTypeface());
            mStrokePaint.setTypeface(mDefaultFont.getTypeface());
            mFillPaint.setTextSize(getRealSize(mDefaultFont.getFontSize()));
            mStrokePaint.setTextSize(getRealSize(mDefaultFont.getFontSize()));
        }
        mStrokePaint.setStrokeCap(Paint.Cap.BUTT);
        mStrokePaint.setStrokeJoin(Paint.Join.MITER);
        mStrokePaint.setStrokeMiter(10);
        if (isRTL()) {
            mFillPaint.setTextAlign(Paint.Align.RIGHT);
            mStrokePaint.setTextAlign(Paint.Align.RIGHT);
        } else {
            mFillPaint.setTextAlign(Paint.Align.LEFT);
            mStrokePaint.setTextAlign(Paint.Align.LEFT);
        }

        mFillPaint.setXfermode(null);
        mStrokePaint.setXfermode(null);

        mFillPaint.setShader(null);
        mStrokePaint.setShader(null);

        mFillPaint.setShadowLayer(0, 0, 0, Color.TRANSPARENT);
        mStrokePaint.setShadowLayer(0, 0, 0, Color.TRANSPARENT);

        mPath.reset();
    }

    private float getRealSize(float size) {
        return DisplayUtil.getRealPxByWidth(size, mDesignWidth);
    }

    public void recycleFillPattern() {
        if (mFillPatternBitmap != null) {
            mFillPatternBitmap.recycle();
            mFillPatternBitmap = null;
        }
        // origin bitmap不需要回收，在缓存中处理
        mFillPatternOriginBitmap = null;
    }

    public void recycleStrokePattern() {
        if (mStrokePatternBitmap != null) {
            mStrokePatternBitmap.recycle();
            mStrokePatternBitmap = null;
        }
        mStrokePatternOriginBitmap = null;
    }

    public void recyclePattern() {
        recycleFillPattern();
        recycleStrokePattern();
    }
}

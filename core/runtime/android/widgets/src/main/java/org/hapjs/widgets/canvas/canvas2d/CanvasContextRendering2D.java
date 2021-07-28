/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.canvas.canvas2d;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.Stack;
import org.hapjs.common.utils.DisplayUtil;
import org.hapjs.runtime.Runtime;
import org.hapjs.widgets.canvas.CanvasContext;
import org.hapjs.widgets.canvas.CanvasManager;
import org.hapjs.widgets.canvas.CanvasRenderAction;
import org.hapjs.widgets.canvas.annotation.CanvasMethod;
import org.hapjs.widgets.canvas.image.ImageData;
import org.hapjs.widgets.view.CanvasViewContainer;

public class CanvasContextRendering2D extends CanvasContext {
    private static final String TAG = "CanvasRendering2D";

    // 当actions过多时，将绘制到bitmap中，actions未变化的话，只需要绘制该bitmap即可，提高绘制速度
    private static final int HARDWARE_RENDER_ACTIONS_LIMIT = 2000;

    private CanvasContextState mState;

    private CanvasView2D mCanvasView;
    private Canvas mCurrentCanvas;
    private Stack<CanvasContextState> mStates = new Stack<>();
    private Paint mCompositePaint;
    private Canvas mSaveCountCanvas;
    private Matrix mIdentifyMatrix = new Matrix();
    private Bitmap mDstBitmap;
    private Canvas mDstCanvas;
    private Bitmap mSrcBitmap;
    private Canvas mSrcCanvas;

    private Canvas mCachedCanvas;
    private Bitmap mCachedBitmap;
    private boolean mDirty = false;
    private Rect mClipWhiteArea;

    public CanvasContextRendering2D(int pageId, int canvasId, int designWidth) {
        super(pageId, canvasId, designWidth);
        mState = new CanvasContextState(designWidth);
        mStates.push(mState);
    }

    private CanvasContextRendering2D(CanvasContextRendering2D context) {
        super(context.getPageId(), context.getCanvasElementId(), context.getDesignWidth());
        mState = new CanvasContextState(context.mState);
        mStates.push(mState);
    }

    @Override
    public String type() {
        return "2d";
    }

    @Override
    public boolean is2d() {
        return true;
    }

    public @NonNull CanvasContextState currentState() {
        return mState;
    }

    public void setDirty(boolean dirty) {
        mDirty = dirty;
    }

    public void render(
            CanvasView2D canvasView, Canvas canvas, ArrayList<CanvasRenderAction> actions) {
        if (actions == null || actions.isEmpty()) {
            return;
        }

        // 绘制前用单位矩阵重置变换矩阵
        canvas.setMatrix(mIdentifyMatrix);
        mCurrentCanvas = canvas;
        mCanvasView = canvasView;

        boolean dirty = mDirty;

        // actions未变化，actions超过了限制就使用cachedbitmap绘制，提高速度
        if (!dirty && actions.size() > HARDWARE_RENDER_ACTIONS_LIMIT && mCachedBitmap != null) {
            canvas.drawBitmap(mCachedBitmap, 0, 0, null);
            return;
        }

        // actions不超过限制，不再使用cached bitmap方式绘制
        if (actions.size() <= HARDWARE_RENDER_ACTIONS_LIMIT) {
            recycleCachedBitmap();
        }

        // dirty时，需要重新更新CachedBitmap上的内容
        if (dirty && actions.size() > HARDWARE_RENDER_ACTIONS_LIMIT) {
            createCachedBitmap();
            if (mCachedBitmap != null) {
                // 清除cachedBitmap上的内容
                mCachedBitmap.eraseColor(Color.TRANSPARENT);
            }
        }

        if (mCachedCanvas != null) {
            mCachedCanvas.setMatrix(mIdentifyMatrix);
            renderInternal(mCachedCanvas, actions);
            canvas.drawBitmap(mCachedBitmap, 0, 0, null);
        } else {
            renderInternal(canvas, actions);
        }

        reset();
        mCanvasView = null;
        mDirty = false;
    }

    private void renderInternal(Canvas canvas, ArrayList<CanvasRenderAction> actions) {
        // !!!globalCompositeOperation需要将dst和src分别绘制在两个bitmap中进行合成效果才正确
        mCurrentCanvas = canvas;
        boolean useComposite = false;
        for (CanvasRenderAction action : actions) {
            if (action.useCompositeCanvas()) {
                useComposite = true;
                break;
            }
        }

        if (useComposite) {
            createCompositeBitmap();
            if (mDstCanvas != null) {
                mCurrentCanvas = mDstCanvas;
            }
        } else {
            recycleCompositeBitmap();
        }

        mSaveCountCanvas = mCurrentCanvas;

        for (CanvasRenderAction action : actions) {
            try {
                action.render(this);
                if (useComposite && needUpdateComposite(action)) {
                    mDstCanvas.drawBitmap(mSrcBitmap, 0, 0, mCompositePaint);
                    mSrcBitmap.eraseColor(Color.TRANSPARENT);
                }
            } catch (Exception e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
        }

        mSaveCountCanvas = null;

        if (useComposite && mDstBitmap != null) {
            canvas.drawBitmap(mDstBitmap, 0, 0, null);
        }
        mCurrentCanvas = null;
    }

    private boolean needUpdateComposite(CanvasRenderAction action) {
        if (mCompositePaint == null
                || mCompositePaint.getXfermode() == null
                || mDstCanvas == null
                || mSrcBitmap == null) {
            return false;
        }

        return isDrawAction(action);
    }

    private boolean isDrawAction(CanvasRenderAction action) {
        String name = action.getAction();
        return TextUtils.equals(name, "fill") ||
                TextUtils.equals(name, "fillRect") ||
                TextUtils.equals(name, "stroke") ||
                TextUtils.equals(name, "strokeRect") ||
                TextUtils.equals(name, "clearRect") ||
                TextUtils.equals(name, "drawImage") ||
                TextUtils.equals(name, "putImageData") ||
                TextUtils.equals(name, "transform") ||
                TextUtils.equals(name, "setTransform") ||
                TextUtils.equals(name, "fillText");
    }

    public Rect getOrCreateClipWhiteArea() {
        if (mClipWhiteArea == null) {
            mClipWhiteArea = new Rect();
        }
        return mClipWhiteArea;
    }

    @CanvasMethod
    public void setFont(CSSFont font) {
        if (font == null) {
            return;
        }

        mState.mFont = font;
        mState.mFillPaint.setTypeface(font.getTypeface());
        mState.mFillPaint.setTextSize(getRealSize(font.getFontSize()));

        mState.mStrokePaint.setTypeface(font.getTypeface());
        mState.mStrokePaint.setTextSize(getRealSize(font.getFontSize()));
    }

    @CanvasMethod
    public void setFillStyle(int color) {
        mState.setFillStyleColor(color);
        mState.mFillPaint.setShader(null);
        mState.recycleFillPattern();
    }

    @CanvasMethod
    public void setFillStyle(CanvasGradient canvasGradient) {
        if (canvasGradient == null || !canvasGradient.isValid()) {
            return;
        }

        float width = DisplayUtil.getScreenWidth(Runtime.getInstance().getContext());
        canvasGradient.setDesignRatio(width * 1f / mDesignWidth);
        Shader shader = canvasGradient.createShader();
        mState.mFillPaint.setShader(shader);
        mState.recycleFillPattern();
    }

    @CanvasMethod
    public void setFillStyle(@NonNull Bitmap bitmap, String pattern) {
        if (bitmap.isRecycled()) {
            Log.e(TAG, "setFillStyle pattern,bitmap is recycle");
            return;
        }
        Shader.TileMode xRepeat;
        Shader.TileMode yRepeat;
        switch (pattern) {
            case "repeat":
                xRepeat = Shader.TileMode.REPEAT;
                yRepeat = Shader.TileMode.REPEAT;
                break;
            case "repeat-x":
                xRepeat = Shader.TileMode.REPEAT;
                yRepeat = Shader.TileMode.CLAMP;
                break;
            case "repeat-y":
                xRepeat = Shader.TileMode.CLAMP;
                yRepeat = Shader.TileMode.REPEAT;
                break;
            case "no-repeat":
            default:
                xRepeat = Shader.TileMode.CLAMP;
                yRepeat = Shader.TileMode.CLAMP;
                break;
        }

        if (bitmap != mState.mFillPatternOriginBitmap
                || !TextUtils.equals(mState.mFillPattern, pattern)
                || mState.mFillPatternBitmap == null) {
            mState.recycleFillPattern();
            mState.mFillPatternOriginBitmap = bitmap;
            mState.mFillPatternBitmap = resolvePatternBitmap(bitmap, xRepeat, yRepeat);
        }

        mState.mFillPaint.setShader(new BitmapShader(mState.mFillPatternBitmap, xRepeat, yRepeat));
        mState.mFillPattern = pattern;
    }

    @CanvasMethod
    public void setStrokeStyle(int color) {
        mState.setStrokeStyleColor(color);
        mState.mStrokePaint.setShader(null);
        mState.recycleStrokePattern();
    }

    @CanvasMethod
    public void setStrokeStyle(CanvasGradient canvasGradient) {
        if (canvasGradient == null || !canvasGradient.isValid()) {
            return;
        }
        float width = DisplayUtil.getScreenWidth(Runtime.getInstance().getContext());
        canvasGradient.setDesignRatio(width * 1f / mDesignWidth);
        Shader shader = canvasGradient.createShader();
        mState.mStrokePaint.setShader(shader);
        mState.recycleStrokePattern();
    }

    @CanvasMethod
    public void setStrokeStyle(@NonNull Bitmap bitmap, String pattern) {
        if (bitmap.isRecycled()) {
            Log.e(TAG, "setStrokeStyle pattern,bitmap is recycle");
            return;
        }
        Shader.TileMode xRepeat;
        Shader.TileMode yRepeat;
        switch (pattern) {
            case "repeat":
                xRepeat = Shader.TileMode.REPEAT;
                yRepeat = Shader.TileMode.REPEAT;
                break;
            case "repeat-x":
                xRepeat = Shader.TileMode.REPEAT;
                yRepeat = Shader.TileMode.CLAMP;
                break;
            case "repeat-y":
                xRepeat = Shader.TileMode.CLAMP;
                yRepeat = Shader.TileMode.REPEAT;
                break;
            case "no-repeat":
            default:
                xRepeat = Shader.TileMode.CLAMP;
                yRepeat = Shader.TileMode.CLAMP;
                break;
        }

        if (bitmap != mState.mStrokePatternOriginBitmap
                || !TextUtils.equals(mState.mStrokePattern, pattern)
                || mState.mStrokePatternBitmap == null) {
            mState.recycleStrokePattern();
            mState.mStrokePatternOriginBitmap = bitmap;
            mState.mStrokePatternBitmap = resolvePatternBitmap(bitmap, xRepeat, yRepeat);
        }

        mState.mStrokePaint
                .setShader(new BitmapShader(mState.mStrokePatternBitmap, xRepeat, yRepeat));
        mState.mStrokePattern = pattern;
    }

    @CanvasMethod
    public void setGlobalAlpha(float globalAlpha) {
        mState.setGlobalAlpha(globalAlpha);
    }

    @CanvasMethod
    public void setGlobalCompositeOperation(String operation) {
        mState.mGlobalCompositeOperation = operation;

        PorterDuff.Mode mode = null;
        switch (operation) {
            case "source-over":
                mode = PorterDuff.Mode.SRC_OVER;
                break;
            case "source-atop":
                mode = PorterDuff.Mode.SRC_ATOP;
                break;
            case "source-in":
                mode = PorterDuff.Mode.SRC_IN;
                break;
            case "source-out":
                mode = PorterDuff.Mode.SRC_OUT;
                break;
            case "destination-over":
                mode = PorterDuff.Mode.DST_OVER;
                break;
            case "destination-atop":
                mode = PorterDuff.Mode.DST_ATOP;
                break;
            case "destination-in":
                mode = PorterDuff.Mode.DST_IN;
                break;
            case "destination-out":
                mode = PorterDuff.Mode.DST_OUT;
                break;
            case "lighter":
                mode = PorterDuff.Mode.LIGHTEN;
                break;
            case "copy":
                mode = PorterDuff.Mode.SRC;
                break;
            case "xor":
                mode = PorterDuff.Mode.XOR;
                break;
            default:
                break;
        }

        if (mode != null) {
            mCurrentCanvas = mSrcCanvas;
            mCompositePaint.setXfermode(new PorterDuffXfermode(mode));
        }
    }

    private void createCachedBitmap() {
        if (mCurrentCanvas == null) {
            return;
        }

        if (mCachedBitmap == null
                || mCachedBitmap.getWidth() != mCurrentCanvas.getWidth()
                || mCachedBitmap.getHeight() != mCurrentCanvas.getHeight()) {
            recycleCachedBitmap();

            int width = mCurrentCanvas.getWidth();
            int height = mCurrentCanvas.getHeight();

            int screenWidth = getScreenWidth();
            int screenHeight = getScreenHeight();

            if (width <= 0 || height <= 0 || width > screenWidth || height > screenHeight) {
                Log.w(TAG, "create cache bitmap fail,the canvas is too large!");
                return;
            }

            try {
                mCachedBitmap = createBitmap(width, height, Bitmap.Config.ARGB_8888);
                mCachedCanvas = new Canvas(mCachedBitmap);
            } catch (OutOfMemoryError e) {
                Log.w(TAG, "create cache bitmap fail,occur OOM exception!");
                Log.e(TAG, Log.getStackTraceString(e));
            }
        }
    }

    private void recycleCachedBitmap() {
        if (mCachedBitmap != null) {
            mCachedBitmap.recycle();
            mCachedBitmap = null;
            mCachedCanvas = null;
        }
    }

    private void createCompositeBitmap() {
        if (mCurrentCanvas == null) {
            return;
        }

        if (mDstBitmap == null
                || mDstBitmap.getWidth() != mCurrentCanvas.getWidth()
                || mDstBitmap.getHeight() != mCurrentCanvas.getHeight()) {
            recycleDstBitmap();
            mDstBitmap =
                    createBitmap(
                            mCurrentCanvas.getWidth(), mCurrentCanvas.getHeight(),
                            Bitmap.Config.ARGB_8888);
            if (mDstBitmap != null) {
                mDstCanvas = new Canvas(mDstBitmap);
            }
        }

        if (mDstBitmap != null) {
            mDstCanvas.setMatrix(mIdentifyMatrix);
            mDstBitmap.eraseColor(Color.TRANSPARENT);
        }

        if (mSrcBitmap == null
                || mSrcBitmap.getWidth() != mCurrentCanvas.getWidth()
                || mSrcBitmap.getHeight() != mCurrentCanvas.getHeight()) {
            recycleSrcBitmap();
            mSrcBitmap =
                    createBitmap(
                            mCurrentCanvas.getWidth(), mCurrentCanvas.getHeight(),
                            Bitmap.Config.ARGB_8888);
            if (mSrcBitmap != null) {
                mSrcCanvas = new Canvas(mSrcBitmap);
            }
        }

        if (mSrcBitmap != null) {
            mSrcCanvas.setMatrix(mIdentifyMatrix);
            mSrcBitmap.eraseColor(Color.TRANSPARENT);
        }

        if (mCompositePaint == null) {
            mCompositePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        }
        mCompositePaint.setXfermode(null);
    }

    private void recycleCompositeBitmap() {
        recycleDstBitmap();
        recycleSrcBitmap();
    }

    private void recycleDstBitmap() {
        if (mDstBitmap != null) {
            mDstBitmap.recycle();
            mDstBitmap = null;
            mDstCanvas = null;
        }
    }

    private void recycleSrcBitmap() {
        if (mSrcBitmap != null) {
            mSrcBitmap.recycle();
            mSrcBitmap = null;
            mSrcCanvas = null;
        }
    }

    @CanvasMethod
    public void setLineCap(String lineCap) {
        mState.mLineCap = lineCap;
        switch (lineCap) {
            case "round":
                mState.mStrokePaint.setStrokeCap(Paint.Cap.ROUND);
                break;
            case "square":
                mState.mStrokePaint.setStrokeCap(Paint.Cap.SQUARE);
                break;
            case "butt":
            default:
                mState.mStrokePaint.setStrokeCap(Paint.Cap.BUTT);
                break;
        }
    }

    @CanvasMethod
    public void setLineDashOffset(float offset) {
        mState.mLineDashOffset = offset;
        if (mState.mLineDashSegments != null) {
            mState.mStrokePaint.setPathEffect(new DashPathEffect(mState.mLineDashSegments, offset));
        }
    }

    @CanvasMethod
    public void setLineJoin(String lineJoin) {
        mState.mLineJoin = lineJoin;
        switch (lineJoin) {
            case "bevel":
                mState.mStrokePaint.setStrokeJoin(Paint.Join.BEVEL);
                break;
            case "round":
                mState.mStrokePaint.setStrokeJoin(Paint.Join.ROUND);
                break;

            // "miter" or invalid lineJoin
            default:
                mState.mStrokePaint.setStrokeJoin(Paint.Join.MITER);
                break;
        }
    }

    @CanvasMethod
    public void setLineWidth(float width) {
        // 忽略0和负数
        if (width <= 0) {
            return;
        }

        mState.mLineWidth = width;
        width = getRealSize(width);
        mState.mStrokePaint.setStrokeWidth(width);
    }

    @CanvasMethod
    public void setMiterLimit(float miterLimit) {
        mState.mMiterLimit = miterLimit;
        mState.mStrokePaint.setStrokeMiter(getRealSize(miterLimit));
    }

    @CanvasMethod
    public void setShadowBlur(float blur) {
        // blur=0会关闭shadow，将blur设置为比较小的数值
        if (blur <= 0) {
            blur = 0.1f;
        }
        mState.mShadowBlur = blur;
        mState.mFillPaint.setShadowLayer(
                mState.mShadowBlur, mState.mShadowOffsetX, mState.mShadowOffsetY,
                mState.mShadowColor);
        mState.mStrokePaint.setShadowLayer(
                mState.mShadowBlur, mState.mShadowOffsetX, mState.mShadowOffsetY,
                mState.mShadowColor);
    }

    @CanvasMethod
    public void setShadowColor(int color) {
        mState.mShadowColor = color;
        mState.mFillPaint.setShadowLayer(
                mState.mShadowBlur, mState.mShadowOffsetX, mState.mShadowOffsetY,
                mState.mShadowColor);
        mState.mStrokePaint.setShadowLayer(
                mState.mShadowBlur, mState.mShadowOffsetX, mState.mShadowOffsetY,
                mState.mShadowColor);
    }

    @CanvasMethod
    public void setShadowOffsetX(float offsetX) {
        mState.mShadowOffsetX = offsetX;
        mState.mFillPaint.setShadowLayer(
                mState.mShadowBlur, mState.mShadowOffsetX, mState.mShadowOffsetY,
                mState.mShadowColor);
        mState.mStrokePaint.setShadowLayer(
                mState.mShadowBlur, mState.mShadowOffsetX, mState.mShadowOffsetY,
                mState.mShadowColor);
    }

    @CanvasMethod
    public void setShadowOffsetY(float offsetY) {
        mState.mShadowOffsetY = offsetY;
        mState.mFillPaint.setShadowLayer(
                mState.mShadowBlur, mState.mShadowOffsetX, mState.mShadowOffsetY,
                mState.mShadowColor);
        mState.mStrokePaint.setShadowLayer(
                mState.mShadowBlur, mState.mShadowOffsetX, mState.mShadowOffsetY,
                mState.mShadowColor);
    }

    @CanvasMethod
    public void setTextAlign(String textAlign) {
        mState.mTextAlign = textAlign;

        boolean rtl = mState.isRTL();
        switch (textAlign) {
            case "start":
                if (rtl) {
                    mState.mFillPaint.setTextAlign(Paint.Align.RIGHT);
                    mState.mStrokePaint.setTextAlign(Paint.Align.RIGHT);
                } else {
                    mState.mFillPaint.setTextAlign(Paint.Align.LEFT);
                    mState.mStrokePaint.setTextAlign(Paint.Align.LEFT);
                }

                break;
            case "end":
                if (rtl) {
                    mState.mFillPaint.setTextAlign(Paint.Align.LEFT);
                    mState.mStrokePaint.setTextAlign(Paint.Align.LEFT);
                } else {
                    mState.mFillPaint.setTextAlign(Paint.Align.RIGHT);
                    mState.mStrokePaint.setTextAlign(Paint.Align.RIGHT);
                }
                break;
            case "left":
                mState.mFillPaint.setTextAlign(Paint.Align.LEFT);
                mState.mStrokePaint.setTextAlign(Paint.Align.LEFT);
                break;
            case "center":
                mState.mFillPaint.setTextAlign(Paint.Align.CENTER);
                mState.mStrokePaint.setTextAlign(Paint.Align.CENTER);
                break;
            case "right":
                mState.mFillPaint.setTextAlign(Paint.Align.RIGHT);
                mState.mStrokePaint.setTextAlign(Paint.Align.RIGHT);
                break;
            default:
                break;
        }
    }

    @CanvasMethod
    public void setTextBaseline(String baseline) {
        mState.mTextBaseline = baseline;
    }

    @CanvasMethod
    public void arc(
            float x, float y, float radius, float startAngle, float endAngle,
            boolean anticlockwise) {
        x = getRealSize(x);
        y = getRealSize(y);
        radius = getRealSize(radius);
        mState.mPath.arc(x, y, radius, startAngle, endAngle, anticlockwise);
    }

    @CanvasMethod
    public void arcTo(float x1, float y1, float x2, float y2, float radius) {
        x1 = getRealSize(x1);
        y1 = getRealSize(y1);
        x2 = getRealSize(x2);
        y2 = getRealSize(y2);
        radius = getRealSize(radius);
        mState.mPath.arcTo(x1, y1, x2, y2, radius);
    }

    @CanvasMethod
    public void beginPath() {
        mState.mPath.beginPath();
    }

    @CanvasMethod
    public void bezierCurveTo(float cp1x, float cp1y, float cp2x, float cp2y, float x, float y) {
        cp1x = getRealSize(cp1x);
        cp1y = getRealSize(cp1y);
        cp2x = getRealSize(cp2x);
        cp2y = getRealSize(cp2y);
        x = getRealSize(x);
        y = getRealSize(y);
        mState.mPath.cubicTo(cp1x, cp1y, cp2x, cp2y, x, y);
    }

    @CanvasMethod
    public void clearRect(float x, float y, float width, float height) {
        if (mCurrentCanvas == null) {
            return;
        }

        x = getRealSize(x);
        y = getRealSize(y);
        width = getRealSize(width);
        height = getRealSize(height);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.TRANSPARENT);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        mCurrentCanvas.drawRect(x, y, x + width, y + height, paint);
        if (mDstCanvas != null) {
            mDstCanvas.drawRect(x, y, x + width, y + height, paint);
        }
        if (mCanvasView != null) {
            // clearRect后显示背景
            Drawable background = mCanvasView.getBackground();
            if (background != null) {
                background.draw(mCurrentCanvas);
            } else {
                // 没有背景显示白色
                paint.setXfermode(null);
                // clear后为黑色，需要重新绘制一片白色区域
                mCurrentCanvas.drawRect(x, y, x + width, y + height, paint);
            }
        }
    }

    @CanvasMethod
    public void clip() {
        if (mCurrentCanvas == null) {
            return;
        }

        mCurrentCanvas.clipPath(mState.mPath);
    }

    @CanvasMethod
    public void closePath() {
        mState.mPath.close();
    }

    @CanvasMethod
    public void drawImage(
            @NonNull Bitmap bitmap,
            float sx,
            float sy,
            float srcWidth,
            float srcHeight,
            float dx,
            float dy,
            float dstWidth,
            float dstHeight) {
        if (mCurrentCanvas == null) {
            return;
        }

        if (bitmap.isRecycled()) {
            Log.e(TAG, "drawImage,bitmap is recycle");
            return;
        }

        if (srcWidth <= 0) {
            srcWidth = bitmap.getWidth();
        }

        if (srcHeight <= 0) {
            srcHeight = bitmap.getHeight();
        }

        dx = getRealSize(dx);
        dy = getRealSize(dy);
        dstWidth = getRealSize(dstWidth);
        dstHeight = getRealSize(dstHeight);

        Rect srcRect;
        RectF dstRect;

        // androidP以下，超出bitmap范围的会被截取，在AndroidQ则是处理为空白
        // Chrome和firefox上，超出范围也是会处理为空白。
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            srcRect =
                    new Rect(
                            sx < 0 ? 0 : (int) sx,
                            sy < 0 ? 0 : (int) sy,
                            sx + srcWidth < bitmap.getWidth() ? (int) (sx + srcWidth) :
                                    bitmap.getWidth(),
                            sy + srcHeight < bitmap.getHeight() ? (int) (sy + srcHeight) :
                                    bitmap.getHeight());

            float left = dx;
            float top = dy;
            float right = dx + dstWidth;
            float bottom = dy + dstHeight;
            if (sx < 0) {
                left = left + Math.abs(getRealSize(sx));
            }
            if (sy < 0) {
                top = top + Math.abs(getRealSize(sy));
            }

            if (sx + srcWidth > bitmap.getWidth()) {
                right -= getRealSize((sx + srcWidth - bitmap.getWidth()));
            }

            if (sy + srcHeight > bitmap.getHeight()) {
                bottom -= getRealSize((sy + srcHeight - bitmap.getHeight()));
            }
            dstRect = new RectF(left, top, right, bottom);
        } else {
            srcRect = new Rect((int) sx, (int) sy, (int) (sx + srcWidth), (int) (sy + srcHeight));
            dstRect = new RectF(dx, dy, dx + dstWidth, dy + dstHeight);
        }

        Bitmap alphaBitmap = bitmap.extractAlpha();
        if (alphaBitmap != null) {
            mCurrentCanvas.drawBitmap(alphaBitmap, srcRect, dstRect, mState.mFillPaint);
            alphaBitmap.recycle();
        }
        mState.mFillPaint.setShadowLayer(0, 0, 0, Color.TRANSPARENT);
        mCurrentCanvas.drawBitmap(bitmap, srcRect, dstRect, mState.mFillPaint);
        mState.mFillPaint.setShadowLayer(
                mState.mShadowBlur, mState.mShadowOffsetX, mState.mShadowOffsetY,
                mState.mShadowColor);
    }

    @CanvasMethod
    public void fill() {
        if (mCurrentCanvas == null) {
            return;
        }

        mCurrentCanvas.drawPath(mState.mPath, mState.mFillPaint);
    }

    @CanvasMethod
    public void fillRect(float x, float y, float width, float height) {
        if (mCurrentCanvas == null) {
            return;
        }
        x = getRealSize(x);
        y = getRealSize(y);
        width = getRealSize(width);
        height = getRealSize(height);
        mCurrentCanvas.drawRect(x, y, x + width, y + height, mState.mFillPaint);
    }

    @CanvasMethod
    public void fillText(String text, float x, float y) {
        if (mCurrentCanvas == null) {
            return;
        }

        x = getRealSize(x);
        y = getRealSize(y);
        if (!TextUtils.isEmpty(mState.mTextBaseline)) {
            Paint.FontMetrics fontMetrics = mState.mFillPaint.getFontMetrics();
            switch (mState.mTextBaseline) {
                case "alphabetic":
                    // default,do nothing
                    break;
                case "middle":
                    y += (fontMetrics.bottom - fontMetrics.top) / 2.0f - fontMetrics.bottom;
                    break;
                case "top":
                    y -= fontMetrics.ascent;
                    break;
                case "hanging":
                    y -= fontMetrics.descent + fontMetrics.ascent;
                    break;
                case "bottom":
                    y -= fontMetrics.bottom;
                    break;
                case "ideographic":
                    y -= fontMetrics.descent;
                    break;
                default:
                    break;
            }
        }
        mCurrentCanvas.drawText(text, x, y, mState.mFillPaint);
    }

    @CanvasMethod
    public void fillText(String text, float x, float y, float maxWidth) {
        if (mCurrentCanvas == null) {
            return;
        }

        x = getRealSize(x);
        y = getRealSize(y);
        maxWidth = getRealSize(maxWidth);
        if (maxWidth <= 0) {
            mState.mFillPaint.setTextScaleX(1);
            return;
        }
        if (!TextUtils.isEmpty(mState.mTextBaseline)) {
            Paint.FontMetrics fontMetrics = mState.mFillPaint.getFontMetrics();
            switch (mState.mTextBaseline) {
                case "alphabetic":
                    // default,do nothing
                    break;
                case "middle":
                    y += (fontMetrics.bottom - fontMetrics.top) / 2.0f - fontMetrics.bottom;
                    break;
                case "top":
                    y -= fontMetrics.ascent;
                    break;
                case "hanging":
                    y -= fontMetrics.descent + fontMetrics.ascent;
                    break;
                case "bottom":
                    y -= fontMetrics.bottom;
                    break;
                case "ideographic":
                    y -= fontMetrics.descent;
                    break;
                default:
                    break;
            }
        }
        float width = mState.mFillPaint.measureText(text);
        if (maxWidth < width) {
            mState.mFillPaint.setTextScaleX(maxWidth / width);
        }
        mCurrentCanvas.drawText(text, x, y, mState.mFillPaint);
        mState.mFillPaint.setTextScaleX(1);
    }

    @CanvasMethod
    public ImageData getImageData(float x, float y, float sw, float sh) {
        Bitmap image = dumpBitmap();
        if (image == null) {
            return createEmptyImageData(sw, sh);
        }
        int width = (int) sw;
        int height = (int) sh;

        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();
        int[] datas = new int[imageWidth * imageHeight];
        image.getPixels(datas, 0, imageWidth, 0, 0, imageWidth, imageHeight);
        image.recycle();

        ImageData imageData = new ImageData();
        imageData.width = width;
        imageData.height = height;
        imageData.data = new byte[width * height * 4];

        int dx = (int) x;
        int dy = (int) y;
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                int index = row * width + col;
                if (x + col < 0 || x + col >= imageWidth || y + row < 0 || y + row >= imageHeight) {
                    imageData.data[index * 4] = 0;
                    imageData.data[index * 4 + 1] = 0;
                    imageData.data[index * 4 + 2] = 0;
                    imageData.data[index * 4 + 3] = 0;
                } else {
                    int colorIndex = (dy + row) * imageWidth + dx + col;
                    int color = datas[colorIndex];
                    imageData.data[index * 4] = (byte) Color.red(color);
                    imageData.data[index * 4 + 1] = (byte) Color.green(color);
                    imageData.data[index * 4 + 2] = (byte) Color.blue(color);
                    imageData.data[index * 4 + 3] = (byte) Color.alpha(color);
                }
            }
        }
        return imageData;
    }

    private ImageData createEmptyImageData(float sw, float sh) {
        ImageData imageData = new ImageData();
        imageData.width = (int) sw;
        imageData.height = (int) sh;

        int size = (int) sw * (int) sh;
        imageData.data = new byte[size * 4];
        for (int i = 0; i < size; i++) {
            imageData.data[i * 4] = 0;
            imageData.data[i * 4 + 1] = 0;
            imageData.data[i * 4 + 2] = 0;
            imageData.data[i * 4 + 3] = 0;
        }
        return imageData;
    }

    /**
     * 按照designWidth大小dump当前canvas的显示内容到bitmap
     *
     * @return
     */
    public Bitmap dumpBitmap() {
        org.hapjs.widgets.canvas.Canvas canvas = getCanvas();
        if (canvas == null) {
            return null;
        }

        Bitmap bitmap;
        Canvas c;
        CanvasViewContainer hostView = canvas.getHostView();
        if (hostView != null && hostView.getWidth() > 0 && hostView.getHeight() > 0) {
            bitmap = createBitmap(hostView.getWidth(), hostView.getHeight(),
                    Bitmap.Config.ARGB_8888);
            c = new Canvas(bitmap);
            hostView.layout(
                    hostView.getLeft(), hostView.getTop(), hostView.getRight(),
                    hostView.getBottom());
            Drawable background = hostView.getBackground();
            if (background != null) {
                background.draw(c);
            } else {
                c.drawColor(Color.WHITE);
            }
        } else {
            int canvasWidth = getCanvasWidth();
            int canvasHeight = getCanvasHeight();
            if (canvasWidth <= 0 || canvasHeight <= 0) {
                return null;
            }
            bitmap = createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888);
            c = new Canvas(bitmap);
        }
        CanvasContextRendering2D context = new CanvasContextRendering2D(this);
        ArrayList<CanvasRenderAction> renderActions =
                CanvasManager.getInstance().getRenderActions(getPageId(), getCanvasElementId());
        if (renderActions != null && renderActions.size() > 0) {
            context.render((CanvasView2D) canvas.getCanvasView(), c, renderActions);
        }
        context.destroy();

        float scale = getDesignWidth() * 1f / getRealSize(getDesignWidth());
        Matrix matrix = new Matrix();
        matrix.setScale(scale, scale);

        Bitmap image =
                createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        bitmap.recycle();
        return image;
    }

    @CanvasMethod
    public void lineTo(float x, float y) {
        x = getRealSize(x);
        y = getRealSize(y);
        mState.mPath.lineTo(x, y);
    }

    @CanvasMethod
    public float measureText(String text, @Nullable CSSFont font) {
        if (TextUtils.isEmpty(text)) {
            return 0;
        }
        Paint paint;
        if (font == null) {
            paint = mState.mFillPaint;
        } else {
            paint = new Paint();
            paint.set(mState.mFillPaint);
            paint.setTypeface(font.getTypeface());
            paint.setTextSize(getRealSize(font.getFontSize()));
        }
        float textSize = paint.measureText(text);
        return getDesignSize(textSize);
    }

    @CanvasMethod
    public void moveTo(float x, float y) {
        x = getRealSize(x);
        y = getRealSize(y);
        mState.mPath.moveTo(x, y);
    }

    @CanvasMethod
    public void putImageData(Bitmap bitmap, float dx, float dy) {
        if (mCurrentCanvas == null) {
            return;
        }
        dx = getRealSize(dx);
        dy = getRealSize(dy);
        int width = getRealSize(bitmap.getWidth());
        int height = getRealSize(bitmap.getHeight());
        Bitmap alphaBitmap = bitmap.extractAlpha();
        if (alphaBitmap != null) {
            mCurrentCanvas.drawBitmap(
                    alphaBitmap, null, new RectF(dx, dy, dx + width, dy + height),
                    mState.mFillPaint);
            alphaBitmap.recycle();
        }
        mState.mFillPaint.setShadowLayer(0, 0, 0, Color.TRANSPARENT);
        mCurrentCanvas.drawBitmap(
                bitmap, null, new RectF(dx, dy, dx + width, dy + height), mState.mFillPaint);
        mState.mFillPaint.setShadowLayer(
                mState.mShadowBlur, mState.mShadowOffsetX, mState.mShadowOffsetY,
                mState.mShadowColor);
    }

    @CanvasMethod
    public void quadraticCurveTo(float cpx, float cpy, float x, float y) {
        cpx = getRealSize(cpx);
        cpy = getRealSize(cpy);
        x = getRealSize(x);
        y = getRealSize(y);
        mState.mPath.quadTo(cpx, cpy, x, y);
    }

    @CanvasMethod
    public void rect(float x, float y, float width, float height) {
        x = getRealSize(x);
        y = getRealSize(y);
        width = getRealSize(width);
        height = getRealSize(height);
        mState.mPath.addRect(x, y, x + width, y + height, Path.Direction.CCW);
    }

    @CanvasMethod
    public void restore() {
        if (mSaveCountCanvas == null) {
            return;
        }

        if (mSaveCountCanvas.getSaveCount() > 1) {
            mSaveCountCanvas.restore();
        }

        if (mStates.size() > 1) {
            mStates.pop();
            mState = mStates.peek();
        }
    }

    /**
     * 顺时针旋转画布
     *
     * @param angle 弧度
     */
    @CanvasMethod
    public void rotate(float angle) {
        if (mCurrentCanvas == null) {
            return;
        }
        angle = (float) ((angle * 180.0f) / Math.PI);
        mCurrentCanvas.rotate(angle);
    }

    @CanvasMethod
    public void save() {
        if (mSaveCountCanvas == null) {
            return;
        }
        mSaveCountCanvas.save();
        mCurrentCanvas.save();
        CanvasContextState state = new CanvasContextState(mState);
        mStates.push(state);
        mState = state;
    }

    @CanvasMethod
    public void scale(float x, float y) {
        if (mCurrentCanvas == null) {
            return;
        }

        mCurrentCanvas.scale(x, y);
    }

    @CanvasMethod
    public void setLineDash(float[] segments) {
        mState.mLineDashSegments = segments;
        if (segments == null || segments.length <= 0) {
            mState.mStrokePaint.setPathEffect(null);
            return;
        }

        float[] tmp = new float[segments.length];
        System.arraycopy(segments, 0, tmp, 0, segments.length);
        for (int i = 0; i < tmp.length; i++) {
            tmp[i] = getRealSize(tmp[i]);
        }
        segments = tmp;

        if (segments.length % 2 != 0) {
            float[] s = new float[segments.length * 2];
            System.arraycopy(segments, 0, s, 0, segments.length);
            System.arraycopy(segments, 0, s, segments.length, segments.length);
            segments = s;
        }

        mState.mStrokePaint.setPathEffect(
                new DashPathEffect(segments, getRealSize(mState.mLineDashOffset)));
    }

    /**
     * [scaleX skewX translateX skewY scaleY translateY 0 0 1 ]
     *
     * @param scaleX
     * @param skewX
     * @param skewY
     * @param scaleY
     * @param translateX
     * @param translateY
     */
    @CanvasMethod
    public void setTransform(
            float scaleX, float skewY, float skewX, float scaleY, float translateX,
            float translateY) {
        if (mCurrentCanvas == null) {
            return;
        }

        Matrix matrix = new Matrix();
        matrix.setValues(
                new float[] {scaleX, skewX, translateX, skewY, scaleY, translateY, 0, 0, 1});
        mCurrentCanvas.setMatrix(matrix);
    }

    @CanvasMethod
    public void stroke() {
        if (mCurrentCanvas == null) {
            return;
        }

        mCurrentCanvas.drawPath(mState.mPath, mState.mStrokePaint);
    }

    @CanvasMethod
    public void strokeRect(float x, float y, float width, float height) {
        if (mCurrentCanvas == null) {
            return;
        }
        x = getRealSize(x);
        y = getRealSize(y);
        width = getRealSize(width);
        height = getRealSize(height);
        mCurrentCanvas.drawRect(x, y, x + width, y + height, mState.mStrokePaint);
    }

    @CanvasMethod
    public void strokeText(String text, float x, float y) {
        if (mCurrentCanvas == null) {
            return;
        }

        x = getRealSize(x);
        y = getRealSize(y);
        if (!TextUtils.isEmpty(mState.mTextBaseline)) {
            Paint.FontMetrics fontMetrics = mState.mStrokePaint.getFontMetrics();
            switch (mState.mTextBaseline) {
                case "alphabetic":
                    // default,do nothing
                    break;
                case "middle":
                    y += (fontMetrics.bottom - fontMetrics.top) / 2.0f - fontMetrics.bottom;
                    break;
                case "top":
                    y -= fontMetrics.ascent;
                    break;
                case "hanging":
                    y -= fontMetrics.descent + fontMetrics.ascent;
                    break;
                case "bottom":
                    y -= fontMetrics.bottom;
                    break;
                case "ideographic":
                    y -= fontMetrics.descent;
                    break;
                default:
                    break;
            }
        }
        mCurrentCanvas.drawText(text, x, y, mState.mStrokePaint);
    }

    @CanvasMethod
    public void strokeText(String text, float x, float y, float maxWidth) {
        if (mCurrentCanvas == null) {
            return;
        }

        x = getRealSize(x);
        y = getRealSize(y);
        maxWidth = getRealSize(maxWidth);
        if (maxWidth <= 0) {
            return;
        }
        if (!TextUtils.isEmpty(mState.mTextBaseline)) {
            Paint.FontMetrics fontMetrics = mState.mStrokePaint.getFontMetrics();
            switch (mState.mTextBaseline) {
                case "alphabetic":
                    // default,do nothing
                    break;
                case "middle":
                    y += (fontMetrics.bottom - fontMetrics.top) / 2.0f - fontMetrics.bottom;
                    break;
                case "top":
                    y -= fontMetrics.ascent;
                    break;
                case "hanging":
                    y -= fontMetrics.descent + fontMetrics.ascent;
                    break;
                case "bottom":
                    y -= fontMetrics.bottom;
                    break;
                case "ideographic":
                    y -= fontMetrics.descent;
                    break;
                default:
                    break;
            }
        }
        float width = mState.mStrokePaint.measureText(text);
        if (maxWidth < width) {
            mState.mStrokePaint.setTextScaleX(maxWidth / width);
        }
        mCurrentCanvas.drawText(text, x, y, mState.mStrokePaint);
        mState.mStrokePaint.setTextScaleX(1);
    }

    /**
     * [scaleX skewX translateX skewY scaleY translateY 0 0 1 ]
     *
     * @param scaleX
     * @param skewX
     * @param skewY
     * @param scaleY
     * @param translateX
     * @param translateY
     */
    @CanvasMethod
    public void transform(
            float scaleX, float skewY, float skewX, float scaleY, float translateX,
            float translateY) {
        if (mCurrentCanvas == null) {
            return;
        }
        Matrix matrix = new Matrix();
        matrix.setValues(
                new float[] {scaleX, skewX, translateX, skewY, scaleY, translateY, 0, 0, 1});
        mCurrentCanvas.concat(matrix);
    }

    @CanvasMethod
    public void translate(float x, float y) {
        if (mCurrentCanvas == null) {
            return;
        }
        x = getRealSize(x);
        y = getRealSize(y);
        mCurrentCanvas.translate(x, y);
    }

    /**
     * clamp会导致边界的颜色拉伸，如果x为clamp，将bitmap最右边一列的像素设置为透明。如果y为clamp，将bitmap最下面一行设置为透明
     *
     * @param bitmap
     * @param repeatX
     * @param repeatY
     * @return
     */
    private Bitmap resolvePatternBitmap(
            Bitmap bitmap, Shader.TileMode repeatX, Shader.TileMode repeatY) {
        Bitmap patternBitmap = createBitmap(bitmap);

        if (repeatX == Shader.TileMode.REPEAT && repeatY == Shader.TileMode.REPEAT) {
            return patternBitmap;
        }

        if (repeatX == Shader.TileMode.REPEAT) {
            int[] colors = new int[bitmap.getWidth()];
            // 最后一行设置为透明
            patternBitmap.setPixels(
                    colors, 0, bitmap.getWidth(), 0, bitmap.getHeight() - 1, bitmap.getWidth(), 1);
        } else if (repeatY == Shader.TileMode.REPEAT) {
            int[] colors = new int[bitmap.getHeight()];
            // 最后一列设置为透明
            patternBitmap.setPixels(colors, 0, 1, bitmap.getWidth() - 1, 0, 1, bitmap.getHeight());
        } else {
            int[] colors = new int[bitmap.getWidth()];
            // 最后一行设置为透明
            patternBitmap.setPixels(
                    colors, 0, bitmap.getWidth(), 0, bitmap.getHeight() - 1, bitmap.getWidth(), 1);

            colors = new int[bitmap.getHeight()];
            // 最后一列设置为透明
            patternBitmap.setPixels(colors, 0, 1, bitmap.getWidth() - 1, 0, 1, bitmap.getHeight());
        }
        return patternBitmap;
    }

    /**
     * 清除paint上设置的属性
     */
    private void reset() {
        mState.reset();
        if (mCompositePaint != null) {
            mCompositePaint.setXfermode(null);
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        recycleCompositeBitmap();
        recycleCachedBitmap();

        for (CanvasContextState state : mStates) {
            state.recyclePattern();
        }
    }
}

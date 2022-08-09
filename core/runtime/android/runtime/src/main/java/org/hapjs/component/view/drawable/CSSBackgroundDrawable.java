/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.view.drawable;

import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.DashPathEffect;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.shapes.PathShape;
import android.graphics.drawable.shapes.Shape;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import java.util.Arrays;
import org.hapjs.common.utils.ColorUtil;
import org.hapjs.common.utils.FloatUtil;
import org.hapjs.component.constants.Corner;
import org.hapjs.component.constants.Edge;
import org.hapjs.component.constants.Spacing;

public class CSSBackgroundDrawable extends Drawable {
    public static final int DEFAULT_BORDER_COLOR = Color.BLACK;
    private static final String TAG = "CSSBackgroundDrawable";
    private static final BorderStyle DEFAULT_BORDER_STYLE = BorderStyle.SOLID;
    /* Used by all types of background and for drawing borders */
    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    /* Value at Spacing.ALL index used for rounded borders, whole array used by rectangular borders */
    private Spacing mBorderWidth;
    private int[] mBorderColor;
    private BorderStyle mBorderStyle;

    /* Used for rounded border and rounded background */
    private PathEffect mPathEffectForBorderStyle;
    private Path mPathForBorderRadius;
    private Path mPathForBorderRadiusOutline;
    private Path mPathForBorder;
    private RectF mTempRectForBorderRadius;
    private RectF mTempRectForBorderRadiusOutline;
    private boolean mNeedUpdatePathForBorderRadius = false;
    private float mBorderRadius = FloatUtil.UNDEFINED;
    private float mBorderRadiusPercent = FloatUtil.UNDEFINED;
    private int mColor = Color.TRANSPARENT;
    private int mAlpha = 255;
    private boolean mNeedPaintBackgroundColor = true;
    private float[] mBorderCornerRadii;
    private float[] mBorderCornerRadiiPercent;
    private float mCurrentFullBorderWidth = 0f;
    private LayerDrawable mLayerDrawable;

    @Override
    public void draw(Canvas canvas) {
        if (getBounds().width() <= 0 || getBounds().height() <= 0) {
            return;
        }
        updatePathEffect();
        drawBackgroundWithBorders(canvas, isRoundedBorders());
    }

    private boolean isRoundedBorders() {
        if (mBorderCornerRadii != null && mBorderCornerRadii.length == 4) {
            if (!FloatUtil.isUndefined(mBorderCornerRadii[Corner.TOP_LEFT])
                    || !FloatUtil.isUndefined(mBorderCornerRadii[Corner.TOP_RIGHT])
                    || !FloatUtil.isUndefined(mBorderCornerRadii[Corner.BOTTOM_RIGHT])
                    || !FloatUtil.isUndefined(mBorderCornerRadii[Corner.BOTTOM_LEFT])) {
                return true;
            }
        }

        if ((!FloatUtil.isUndefined(mBorderRadius) && mBorderRadius > 0)) {
            return true;
        }

        if (mBorderCornerRadiiPercent != null && mBorderCornerRadiiPercent.length == 4) {
            if (!FloatUtil.isUndefined(mBorderCornerRadiiPercent[Corner.TOP_LEFT])
                    || !FloatUtil.isUndefined(mBorderCornerRadiiPercent[Corner.TOP_RIGHT])
                    || !FloatUtil.isUndefined(mBorderCornerRadiiPercent[Corner.BOTTOM_RIGHT])
                    || !FloatUtil.isUndefined(mBorderCornerRadiiPercent[Corner.BOTTOM_LEFT])) {
                return true;
            }
        }

        if ((!FloatUtil.isUndefined(mBorderRadiusPercent) && mBorderRadiusPercent > 0)) {
            return true;
        }

        return false;
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        if (mLayerDrawable != null) {
            mLayerDrawable.setBounds(bounds);
        }
        mNeedUpdatePathForBorderRadius = true;
    }

    @Override
    public int getAlpha() {
        return mAlpha;
    }

    @Override
    public void setAlpha(int alpha) {
        if (alpha != mAlpha) {
            mAlpha = alpha;
            invalidateSelf();
        }
    }

    @Override
    public void setColorFilter(ColorFilter cf) {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP && mLayerDrawable != null) {
            int number = mLayerDrawable.getNumberOfLayers();
            for (int i = 0; i < number; i++) {
                Drawable drawable = mLayerDrawable.getDrawable(i);
                if (drawable == null) {
                    continue;
                }
                drawable.setColorFilter(cf);
                drawable.invalidateSelf();
            }
        }
    }

    @Override
    public int getOpacity() {
        return ColorUtil.getOpacityFromColor(ColorUtil.multiplyColorAlpha(mColor, mAlpha));
    }

    /* Android's elevation implementation requires this to be implemented to know where to draw the shadow. */
    @Override
    public void getOutline(Outline outline) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            super.getOutline(outline);
            return;
        }
        if (isRoundedBorders()) {
            updatePath();

            outline.setConvexPath(mPathForBorderRadiusOutline);
        } else {
            outline.setRect(getBounds());
        }
    }

    public void setLayerDrawable(LayerDrawable drawable) {
        mLayerDrawable = drawable;
        invalidateSelf();
    }

    public LayerDrawable getLayerDrawable() {
        return mLayerDrawable;
    }

    public void setRadiusPercent(float radiusPercent) {
        if (!FloatUtil.floatsEqual(mBorderRadiusPercent, radiusPercent)) {
            mBorderRadiusPercent = radiusPercent;
            mNeedUpdatePathForBorderRadius = true;
            invalidateSelf();
        }
    }

    public void setRadiusPercent(float[] radiusPercent) {
        if (radiusPercent == null || radiusPercent.length != 4) {
            Log.e(TAG, "setRadiusPercent(),radiusPercent is null or invalid");
            return;
        }
        if (!FloatUtil.floatListsEqual(mBorderCornerRadiiPercent, radiusPercent)) {
            mNeedUpdatePathForBorderRadius = true;
            mBorderCornerRadiiPercent = radiusPercent.clone();
            invalidateSelf();
        }
    }

    public int getColor() {
        return mColor;
    }

    public void setColor(int color) {
        mColor = color;
        invalidateSelf();
    }

    public float getRadius() {
        if (!FloatUtil.isUndefined(mBorderRadius)) {
            return mBorderRadius;
        } else if (mBorderCornerRadii != null && mBorderCornerRadii.length == 4) {
            if (Math.round(mBorderCornerRadii[0]) == Math.round(mBorderCornerRadii[1])
                    && Math.round(mBorderCornerRadii[0]) == Math.round(mBorderCornerRadii[2])
                    && Math.round(mBorderCornerRadii[0]) == Math.round(mBorderCornerRadii[3])) {
                return mBorderCornerRadii[0];
            }
        }
        return mBorderRadius;
    }

    public void setRadius(float radius) {
        if (!FloatUtil.floatsEqual(mBorderRadius, radius)) {
            mBorderRadius = radius;
            mNeedUpdatePathForBorderRadius = true;
            invalidateSelf();
        }
    }

    public void setRadius(float[] radius) {
        if (radius == null || radius.length != 4) {
            Log.e(TAG, "setRadius(),radius is null or invalid");
            return;
        }
        if (!FloatUtil.floatListsEqual(mBorderCornerRadii, radius)) {
            mNeedUpdatePathForBorderRadius = true;
            mBorderCornerRadii = radius.clone();
            invalidateSelf();
        }
    }

    public SizeBackgroundDrawable.Position getPosition() {
        if (mLayerDrawable != null) {
            try {
                Drawable drawable =
                        mLayerDrawable.getNumberOfLayers() > 0 ? mLayerDrawable.getDrawable(0) :
                                null;
                if (drawable instanceof SizeBackgroundDrawable) {
                    return ((SizeBackgroundDrawable) drawable).getPosition();
                }
            } catch (IndexOutOfBoundsException e) {
                Log.e(TAG, "setPosition: " + e);
            }
        }
        return null;
    }

    public void setPosition(String positionStr) {
        if (mLayerDrawable != null) {
            try {
                for (int i = 0; i < mLayerDrawable.getNumberOfLayers(); i++) {
                    Drawable drawable = mLayerDrawable.getDrawable(i);
                    if (drawable instanceof SizeBackgroundDrawable) {
                        ((SizeBackgroundDrawable) drawable).setBackgroundPosition(positionStr);
                    }
                }
                invalidateSelf();
            } catch (IndexOutOfBoundsException e) {
                Log.e(TAG, "setPosition: " + e);
            }
        }
    }

    private void updatePath() {
        if (!mNeedUpdatePathForBorderRadius) {
            return;
        }
        mNeedUpdatePathForBorderRadius = false;
        if (mPathForBorderRadius == null) {
            mPathForBorderRadius = new Path();
            mTempRectForBorderRadius = new RectF();
            mPathForBorderRadiusOutline = new Path();
            mTempRectForBorderRadiusOutline = new RectF();
        }

        mPathForBorderRadius.reset();
        mPathForBorderRadiusOutline.reset();

        mTempRectForBorderRadius.set(getBounds());
        mTempRectForBorderRadiusOutline.set(getBounds());

        float width = mTempRectForBorderRadius.width();
        float height = mTempRectForBorderRadius.height();

        float[] radii = new float[8];

        radii[Corner.TOP_LEFT_X] = getBorderCornerRadius(Corner.TOP_LEFT, width);
        radii[Corner.TOP_LEFT_Y] = getBorderCornerRadius(Corner.TOP_LEFT, height);
        radii[Corner.TOP_RIGHT_X] = getBorderCornerRadius(Corner.TOP_RIGHT, width);
        radii[Corner.TOP_RIGHT_Y] = getBorderCornerRadius(Corner.TOP_RIGHT, height);
        radii[Corner.BOTTOM_RIGHT_X] = getBorderCornerRadius(Corner.BOTTOM_RIGHT, width);
        radii[Corner.BOTTOM_RIGHT_Y] = getBorderCornerRadius(Corner.BOTTOM_RIGHT, height);
        radii[Corner.BOTTOM_LEFT_X] = getBorderCornerRadius(Corner.BOTTOM_LEFT, width);
        radii[Corner.BOTTOM_LEFT_Y] = getBorderCornerRadius(Corner.BOTTOM_LEFT, height);

        float fullBorderWidth = getFullBorderWidth();
        if (fullBorderWidth > 0) {
            float offset = fullBorderWidth * 0.5f;
            mTempRectForBorderRadius.inset(offset, offset);
        }

        mPathForBorderRadius.addRoundRect(mTempRectForBorderRadius, radii, Path.Direction.CW);

        float extraRadiusForOutline = 0;

        if (mBorderWidth != null) {
            extraRadiusForOutline = fullBorderWidth / 2f;
        }

        mPathForBorderRadiusOutline.addRoundRect(
                mTempRectForBorderRadiusOutline,
                new float[] {
                        radii[Corner.TOP_LEFT_X] + extraRadiusForOutline,
                        radii[Corner.TOP_LEFT_Y] + extraRadiusForOutline,
                        radii[Corner.TOP_RIGHT_X] + extraRadiusForOutline,
                        radii[Corner.TOP_RIGHT_Y] + extraRadiusForOutline,
                        radii[Corner.BOTTOM_RIGHT_X] + extraRadiusForOutline,
                        radii[Corner.BOTTOM_RIGHT_Y] + extraRadiusForOutline,
                        radii[Corner.BOTTOM_LEFT_X] + extraRadiusForOutline,
                        radii[Corner.BOTTOM_LEFT_Y] + extraRadiusForOutline
                },
                Path.Direction.CW);
    }

    private float getBorderCornerRadius(int corner, float baseLength) {
        float borderCornerRadius;
        if (!FloatUtil.isUndefined(mBorderRadiusPercent)) {
            float defaultBorderRadiusPercent = mBorderRadiusPercent;
            if (mBorderCornerRadiiPercent != null
                    && !FloatUtil.isUndefined(mBorderCornerRadiiPercent[corner])) {
                borderCornerRadius = mBorderCornerRadiiPercent[corner] * baseLength;
            } else if (mBorderCornerRadii != null
                    && !FloatUtil.isUndefined(mBorderCornerRadii[corner])) {
                borderCornerRadius = mBorderCornerRadii[corner];
            } else {
                borderCornerRadius = defaultBorderRadiusPercent * baseLength;
            }
        } else {
            float defaultBorderRadius = !FloatUtil.isUndefined(mBorderRadius) ? mBorderRadius : 0;
            if (mBorderCornerRadiiPercent != null
                    && !FloatUtil.isUndefined(mBorderCornerRadiiPercent[corner])) {
                borderCornerRadius = mBorderCornerRadiiPercent[corner] * baseLength;
            } else if (mBorderCornerRadii != null
                    && !FloatUtil.isUndefined(mBorderCornerRadii[corner])) {
                borderCornerRadius = mBorderCornerRadii[corner];
            } else {
                borderCornerRadius = defaultBorderRadius;
            }
        }
        return borderCornerRadius;
    }

    /**
     * Set type of border
     */
    private void updatePathEffect() {
        updatePathEffect(getFullBorderWidth());
    }

    private void updatePathEffect(float borderWidth) {
        mPathEffectForBorderStyle =
                mBorderStyle != null ? mBorderStyle.getPathEffect(borderWidth) : null;

        mPaint.setPathEffect(mPathEffectForBorderStyle);
    }

    /**
     * For rounded borders we use default "borderWidth" property.
     */
    private float getFullBorderWidth() {
        if (mBorderWidth == null) {
            return 0f;
        }

        if (!FloatUtil.isUndefined(mBorderWidth.getRaw(Spacing.ALL))) {
            return mBorderWidth.getRaw(Spacing.ALL);
        }
        if (!FloatUtil.isUndefined(mBorderWidth.getRaw(Spacing.LEFT))
                && FloatUtil.floatsEqual(
                mBorderWidth.getRaw(Spacing.LEFT), mBorderWidth.getRaw(Spacing.TOP))
                && FloatUtil.floatsEqual(
                mBorderWidth.getRaw(Spacing.TOP), mBorderWidth.getRaw(Spacing.RIGHT))
                && FloatUtil.floatsEqual(
                mBorderWidth.getRaw(Spacing.RIGHT), mBorderWidth.getRaw(Spacing.BOTTOM))) {
            return mBorderWidth.getRaw(Spacing.LEFT);
        }

        return 0f;
    }

    /**
     * We use this method for getting color for rounded borders only similarly as for {@link
     * #getFullBorderWidth}.
     */
    private int getFullBorderColor() {
        if (mBorderColor == null) {
            return DEFAULT_BORDER_COLOR;
        }

        if (mBorderColor[Edge.ALL] != DEFAULT_BORDER_COLOR) {
            return mBorderColor[Edge.ALL];
        }
        if (mBorderColor[Edge.LEFT] != DEFAULT_BORDER_COLOR
                && mBorderColor[Edge.LEFT] == mBorderColor[Edge.TOP]
                && mBorderColor[Edge.TOP] == mBorderColor[Edge.RIGHT]
                && mBorderColor[Edge.RIGHT] == mBorderColor[Edge.BOTTOM]) {
            return mBorderColor[Edge.LEFT];
        }
        return DEFAULT_BORDER_COLOR;
    }

    private boolean isBorderOpacity() {
        return Color.alpha(getFullBorderColor()) < 255;
    }

    private void drawBackgroundWithBorders(Canvas canvas, boolean roundedRadius) {
        if (roundedRadius) {
            updatePath();
            drawColorBg(canvas, true);
            drawLayerBg(canvas, true);
            drawRoundedBorder(canvas);
        } else {
            drawColorBg(canvas, false);
            drawLayerBg(canvas, false);
            drawRectangularBorder(canvas);
        }
    }

    private void drawColorBg(Canvas canvas, boolean roundedRadius) {
        int useColor = ColorUtil.multiplyColorAlpha(mColor, mAlpha);
        if (mNeedPaintBackgroundColor) {
            if ((useColor >>> 24) != 0) { // color is not transparent
                mPaint.setColor(useColor);
                mPaint.setStyle(Paint.Style.FILL);
                if (roundedRadius) {
                    // 有边框时只绘制边框以内部分，避免周围出现细线
                    Path path =
                            getFullBorderWidth() > 0 && !isBorderOpacity()
                                    ? mPathForBorderRadius
                                    : mPathForBorderRadiusOutline;
                    canvas.drawPath(path, mPaint);
                } else {
                    canvas.drawRect(getBounds(), mPaint);
                }
            }
        }
    }

    private void drawLayerBg(Canvas canvas, boolean roundedRadius) {
        if (mLayerDrawable == null) {
            return;
        }
        if (roundedRadius) {
            // 有边框时只绘制边框以内部分，避免周围出现细线
            Path path =
                    getFullBorderWidth() > 0 && !isBorderOpacity()
                            ? mPathForBorderRadius
                            : mPathForBorderRadiusOutline;
            for (int i = 0; i < mLayerDrawable.getNumberOfLayers(); i++) {
                Drawable d = mLayerDrawable.getDrawable(i);
                if (d instanceof SizeBackgroundDrawable) {
                    SizeBackgroundDrawable sizeBackgroundDrawable = (SizeBackgroundDrawable) d;
                    sizeBackgroundDrawable.setOutlinePath(path);
                    sizeBackgroundDrawable.setAntiAlias(true);
                } else if (d instanceof LinearGradientDrawable) {
                    LinearGradientDrawable linearGradientDrawable = (LinearGradientDrawable) d;
                    Shape shape = new PathShape(path, getBounds().width(), getBounds().height());
                    linearGradientDrawable.setShape(shape);
                } else {
                    // 少数场景，例如没有设置背景，但设置了圆角
                    canvas.clipPath(mPathForBorderRadiusOutline);
                }
            }
        }
        mLayerDrawable.setBounds(getBounds());
        mLayerDrawable.draw(canvas);
    }

    private void drawRoundedBorder(Canvas canvas) {
        float fullBorderWidth = getFullBorderWidth();
        if (fullBorderWidth > 0) {
            int borderColor = getFullBorderColor();
            mPaint.setColor(ColorUtil.multiplyColorAlpha(borderColor, mAlpha));
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(fullBorderWidth);
            if (null != mPathForBorderRadius) {
                canvas.drawPath(mPathForBorderRadius, mPaint);
            }
        }
    }

    private void drawRectangularBorder(Canvas canvas) {
        Rect bounds = getBounds();

        float borderLeft = getDrawBorderWidth(Spacing.LEFT);
        float borderTop = getDrawBorderWidth(Spacing.TOP);
        float borderRight = getDrawBorderWidth(Spacing.RIGHT);
        float borderBottom = getDrawBorderWidth(Spacing.BOTTOM);
        int colorLeft = getDrawBorderColor(Spacing.LEFT);
        int colorTop = getDrawBorderColor(Spacing.TOP);
        int colorRight = getDrawBorderColor(Spacing.RIGHT);
        int colorBottom = getDrawBorderColor(Spacing.BOTTOM);

        int top = bounds.top;
        int left = bounds.left;
        int width = bounds.width();
        int height = bounds.height();

        if (mPathForBorder == null) {
            mPathForBorder = new Path();
        }

        // border没有相交的部分

        if (isBorderNeedDraw(Spacing.LEFT)) {
            mPaint.setColor(colorLeft);
            mPaint.setStrokeWidth(borderLeft);
            mPaint.setStyle(Paint.Style.STROKE);
            updatePathEffect(borderLeft);
            mPathForBorder.reset();
            float pathLeft = left + Math.max(borderLeft / 2, 1);
            mPathForBorder.moveTo(pathLeft, top + borderTop);
            mPathForBorder.lineTo(pathLeft, top + height - borderBottom);
            canvas.drawPath(mPathForBorder, mPaint);
        }

        if (isBorderNeedDraw(Spacing.TOP)) {
            mPaint.setColor(colorTop);
            mPaint.setStrokeWidth(borderTop);
            mPaint.setStyle(Paint.Style.STROKE);
            updatePathEffect(borderTop);
            mPathForBorder.reset();
            float pathTop = top + Math.max(borderTop / 2, 1);
            mPathForBorder.moveTo(left + borderLeft, pathTop);
            mPathForBorder.lineTo(left + width - borderRight, pathTop);
            canvas.drawPath(mPathForBorder, mPaint);
        }

        if (isBorderNeedDraw(Spacing.RIGHT)) {
            mPaint.setColor(colorRight);
            mPaint.setStrokeWidth(borderRight);
            mPaint.setStyle(Paint.Style.STROKE);
            updatePathEffect(borderRight);
            mPathForBorder.reset();
            float pathRight = left + width - Math.max(borderRight / 2, 1);
            mPathForBorder.moveTo(pathRight, top + borderTop);
            mPathForBorder.lineTo(pathRight, top + height - borderBottom);
            canvas.drawPath(mPathForBorder, mPaint);
        }

        if (isBorderNeedDraw(Spacing.BOTTOM)) {
            mPaint.setColor(colorBottom);
            mPaint.setStrokeWidth(borderBottom);
            mPaint.setStyle(Paint.Style.STROKE);
            updatePathEffect(borderBottom);
            mPathForBorder.reset();
            float pathBottom = top + height - Math.max(borderBottom / 2, 1);
            mPathForBorder.moveTo(left + borderLeft, pathBottom);
            mPathForBorder.lineTo(left + width - borderRight, pathBottom);
            canvas.drawPath(mPathForBorder, mPaint);
        }

        // border相交的部分，corner部分

        // 避免部分机型出现交界处白边现象
        int compatibleOffset = 1;

        if (isBorderNeedDraw(Spacing.LEFT) && isBorderNeedDraw(Spacing.TOP)) {
            mPaint.setColor(colorLeft);
            mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            mPaint.setStrokeWidth(0);
            mPathForBorder.reset();
            mPathForBorder.moveTo(left, top);
            mPathForBorder.lineTo(left, top + borderTop);
            mPathForBorder.lineTo(left, top + borderTop + compatibleOffset);
            mPathForBorder.lineTo(left + borderLeft, top + borderTop + compatibleOffset);
            mPathForBorder.lineTo(left + borderLeft, top + borderTop);
            mPathForBorder.close();
            canvas.drawPath(mPathForBorder, mPaint);

            mPaint.setColor(colorTop);
            mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            mPaint.setStrokeWidth(0);
            mPathForBorder.reset();
            mPathForBorder.moveTo(left, top);
            mPathForBorder.lineTo(left + borderLeft, top);
            mPathForBorder.lineTo(left + borderLeft + compatibleOffset, top);
            mPathForBorder.lineTo(left + borderLeft + compatibleOffset, top + borderTop);
            mPathForBorder.lineTo(left + borderLeft, top + borderTop);
            mPathForBorder.close();
            canvas.drawPath(mPathForBorder, mPaint);
        }

        if (isBorderNeedDraw(Spacing.TOP) && isBorderNeedDraw(Spacing.RIGHT)) {
            mPaint.setColor(colorTop);
            mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            mPaint.setStrokeWidth(0);
            mPathForBorder.reset();
            mPathForBorder.moveTo(left + width, top);
            mPathForBorder.lineTo(left + width - borderRight, top);
            mPathForBorder.lineTo(left + width - borderRight - compatibleOffset, top);
            mPathForBorder.lineTo(left + width - borderRight - compatibleOffset, top + borderTop);
            mPathForBorder.lineTo(left + width - borderRight, top + borderTop);
            mPathForBorder.close();
            canvas.drawPath(mPathForBorder, mPaint);

            mPaint.setColor(colorRight);
            mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            mPaint.setStrokeWidth(0);
            mPathForBorder.reset();
            mPathForBorder.moveTo(left + width, top);
            mPathForBorder.lineTo(left + width, top + borderTop);
            mPathForBorder.lineTo(left + width, top + borderTop + compatibleOffset);
            mPathForBorder.lineTo(left + width - borderRight, top + borderTop + compatibleOffset);
            mPathForBorder.lineTo(left + width - borderRight, top + borderTop);
            mPathForBorder.close();
            canvas.drawPath(mPathForBorder, mPaint);
        }

        if (isBorderNeedDraw(Spacing.RIGHT) && isBorderNeedDraw(Spacing.BOTTOM)) {
            mPaint.setColor(colorRight);
            mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            mPaint.setStrokeWidth(0);
            mPathForBorder.reset();
            mPathForBorder.moveTo(left + width, top + height);
            mPathForBorder.lineTo(left + width, top + height - borderBottom);
            mPathForBorder.lineTo(left + width, top + height - borderBottom - compatibleOffset);
            mPathForBorder.lineTo(
                    left + width - borderRight, top + height - borderBottom - compatibleOffset);
            mPathForBorder.lineTo(left + width - borderRight, top + height - borderBottom);
            mPathForBorder.close();
            canvas.drawPath(mPathForBorder, mPaint);

            mPaint.setColor(colorBottom);
            mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            mPaint.setStrokeWidth(0);
            mPathForBorder.reset();
            mPathForBorder.moveTo(left + width, top + height);
            mPathForBorder.lineTo(left + width - borderRight, top + height);
            mPathForBorder.lineTo(left + width - borderRight - compatibleOffset, top + height);
            mPathForBorder.lineTo(
                    left + width - borderRight - compatibleOffset, top + height - borderBottom);
            mPathForBorder.lineTo(left + width - borderRight, top + height - borderBottom);
            mPathForBorder.close();
            canvas.drawPath(mPathForBorder, mPaint);
        }

        if (isBorderNeedDraw(Spacing.BOTTOM) && isBorderNeedDraw(Spacing.LEFT)) {
            mPaint.setColor(colorBottom);
            mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            mPaint.setStrokeWidth(0);
            mPathForBorder.reset();
            mPathForBorder.moveTo(left, top + height);
            mPathForBorder.lineTo(left + borderLeft, top + height);
            mPathForBorder.lineTo(left + borderLeft + compatibleOffset, top + height);
            mPathForBorder
                    .lineTo(left + borderLeft + compatibleOffset, top + height - borderBottom);
            mPathForBorder.lineTo(left + borderLeft, top + height - borderBottom);
            mPathForBorder.close();
            canvas.drawPath(mPathForBorder, mPaint);

            mPaint.setColor(colorLeft);
            mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            mPaint.setStrokeWidth(0);
            mPathForBorder.reset();
            mPathForBorder.moveTo(left, top + height);
            mPathForBorder.lineTo(left, top + height - borderBottom);
            mPathForBorder.lineTo(left, top + height - borderBottom - compatibleOffset);
            mPathForBorder
                    .lineTo(left + borderLeft, top + height - borderBottom - compatibleOffset);
            mPathForBorder.lineTo(left + borderLeft, top + height - borderBottom);
            mPathForBorder.close();
            canvas.drawPath(mPathForBorder, mPaint);
        }
    }

    public float getDrawBorderWidth(int position) {
        float borderWidth = getBorderWidth(position);
        return !FloatUtil.floatsEqual(borderWidth, 0f) ? borderWidth : getFullBorderWidth();
    }

    public int getDrawBorderColor(int position) {
        int borderColor = getBorderColor(position);
        return borderColor != DEFAULT_BORDER_COLOR ? borderColor : getFullBorderColor();
    }

    private boolean isBorderNeedDraw(int position) {
        float drawBorderWidth = getDrawBorderWidth(position);
        return drawBorderWidth > 0;
    }

    public String getBorderStyle() {
        if (mBorderStyle == null) {
            return DEFAULT_BORDER_STYLE.toString().toLowerCase();
        }
        return mBorderStyle.toString();
    }

    public void setBorderStyle(String style) {
        BorderStyle borderStyle = BorderStyle.find(style);
        if (mBorderStyle != borderStyle) {
            mBorderStyle = borderStyle;
            mNeedUpdatePathForBorderRadius = true;
            invalidateSelf();
        }
    }

    public float getBorderWidth(int position) {
        return mBorderWidth != null ? mBorderWidth.get(position) : 0;
    }

    public Spacing getBorderWidth() {
        return mBorderWidth;
    }

    public void setBorderWidth(Spacing borderWidth) {
        if ((borderWidth == null && mBorderWidth != null)
                || (borderWidth != null && !borderWidth.equals(mBorderWidth))) {
            mNeedUpdatePathForBorderRadius = true;
            mBorderWidth = borderWidth;
            invalidateSelf();
        }
    }

    public int[] getBorderColor() {
        return mBorderColor;
    }

    public int getBorderColor(int position) {
        return mBorderColor != null ? mBorderColor[position] : DEFAULT_BORDER_COLOR;
    }

    public void setBorderColor(int[] borderColor) {
        if (!Arrays.equals(borderColor, mBorderColor)) {
            mBorderColor = borderColor;
            invalidateSelf();
        }
    }

    @Override
    public void setTintList(ColorStateList tint) {
        if (mLayerDrawable != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mLayerDrawable.setTintList(tint);
            }
        }
    }

    @Override
    public void setTintMode(PorterDuff.Mode tintMode) {
        if (mLayerDrawable != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mLayerDrawable.setTintMode(tintMode);
            }
        }
    }

    private enum BorderStyle {
        SOLID,
        DASHED,
        DOTTED;

        public static BorderStyle find(String value) {
            if (TextUtils.isEmpty(value)) {
                return null;
            }
            switch (value.toUpperCase()) {
                case "SOLID":
                    return SOLID;
                case "DASHED":
                    return DASHED;
                case "DOTTED":
                    return DOTTED;
                default:
                    break;
            }
            return null;
        }

        public PathEffect getPathEffect(float borderWidth) {
            switch (this) {
                case SOLID:
                    return null;

                case DASHED:
                    return new DashPathEffect(
                            new float[] {borderWidth * 3, borderWidth * 3, borderWidth * 3,
                                    borderWidth * 3}, 0);

                case DOTTED:
                    if (borderWidth < 2 && borderWidth > 0) {
                        borderWidth = 2;
                    }
                    return new DashPathEffect(
                            new float[] {borderWidth, borderWidth, borderWidth, borderWidth}, 0);

                default:
                    return null;
            }
        }
    }
}

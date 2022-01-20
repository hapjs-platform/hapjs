/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.skeleton;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.hapjs.cache.Cache;
import org.hapjs.common.utils.ColorUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SkeletonSvgView extends View {
    public static final int STATE_NOT_STARTED =
            0; // The animation has been reset or hasn't started yet.
    public static final int STATE_FILL_STARTED = 1;
    // The SVG has been traced and is now being filled
    public static final int STATE_FINISHED = 2; // The animation has finished
    public static final String DEFAULT_ELE_COLOR = "#EFEFEF";
    private static final String TAG = "Skeleton_SvgView";
    private static final int GLYPH_DATA_TYPE_PATH = 1;
    private static final int GLYPH_DATA_TYPE_IMG = 2;
    RectF mImgRect;
    private String mPackageName;
    private PointF mViewport;
    private float aspectRatioWidth = 1;
    private float aspectRatioHeight = 1;
    private List<RenderData> mRenderDataList;
    private int mWidth;
    private int mHeight;
    private int mState = STATE_NOT_STARTED;
    private boolean mAutoHide = true;
    private Paint mPathFillPaint; // 绘制三种基本形状
    // 绘制img所用
    private Paint mImgPaint;

    public SkeletonSvgView(Context context, String packageName) {
        super(context);
        this.mPackageName = packageName;
        init();
    }

    private void init() {
        setClickable(true);
    }

    private void initImgData() {
        if (mImgPaint == null) {
            mImgPaint = new Paint();
            mImgPaint.setAntiAlias(true);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mWidth = w;
        mHeight = h;
        rebuildRenderData();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        if (height <= 0
                && width <= 0
                && heightMode == MeasureSpec.UNSPECIFIED
                && widthMode == MeasureSpec.UNSPECIFIED) {
            width = 0;
            height = 0;
        } else if (height <= 0 && heightMode == MeasureSpec.UNSPECIFIED) {
            height = (int) (width * aspectRatioHeight / aspectRatioWidth);
        } else if (width <= 0 && widthMode == MeasureSpec.UNSPECIFIED) {
            width = (int) (height * aspectRatioWidth / aspectRatioHeight);
        } else if (width * aspectRatioHeight > aspectRatioWidth * height) {
            width = (int) (height * aspectRatioWidth / aspectRatioHeight);
        } else {
            height = (int) (width * aspectRatioHeight / aspectRatioWidth);
        }
        super.onMeasure(
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        long drawStart = System.currentTimeMillis();
        super.onDraw(canvas);
        if (mState == STATE_NOT_STARTED) {
            return;
        }
        if (mRenderDataList != null && mRenderDataList.size() > 0) {
            if (mState < STATE_FILL_STARTED) {
                changeState(STATE_FILL_STARTED);
            }
            // If after fill start, draw skeleton view
            for (RenderData renderData : mRenderDataList) {
                if (renderData != null) {
                    switch (renderData.getType()) {
                        case GLYPH_DATA_TYPE_PATH:
                            if (renderData instanceof PathData) {
                                PathData pathData = (PathData) renderData;
                                mPathFillPaint.setARGB(
                                        Color.alpha(pathData.color),
                                        Color.red(pathData.color),
                                        Color.green(pathData.color),
                                        Color.blue(pathData.color));
                                canvas.drawPath(pathData.path, mPathFillPaint);
                            }
                            break;
                        case GLYPH_DATA_TYPE_IMG:
                            if (renderData instanceof ImgData) {
                                ImgData imgData = (ImgData) renderData;
                                long imgStart = System.currentTimeMillis();
                                if (imgData.bitMap != null) {
                                    drawFullScreenImg(canvas, imgData.fitStyle, mWidth, mHeight,
                                            imgData.bitMap);
                                    Log.d(
                                            TAG,
                                            "LOG_SKELETON draw one img, time = "
                                                    + (System.currentTimeMillis() - imgStart));
                                }
                            }
                            break;
                        default:
                            break;
                    }
                }
            }
        } else {
            Log.w(TAG, "LOG_SKELETON skeleton onDraw, mRenderDataList is null or empty");
        }
        changeState(STATE_FINISHED);
        long drawEnd = System.currentTimeMillis();
        Log.d(TAG, "LOG_SKELETON Skeleton draw time= " + (drawEnd - drawStart));
    }

    private void drawFullScreenImg(
            Canvas canvas, String fitStyle, float viewWidth, float viewHeight, Bitmap bitmap) {
        if (canvas == null
                || bitmap == null
                || TextUtils.isEmpty(fitStyle)
                || viewWidth <= 0
                || viewHeight <= 0) {
            return;
        }
        if (mImgRect == null) {
            mImgRect = new RectF();
        }
        float bmWidth = bitmap.getWidth();
        float bmHeight = bitmap.getHeight();
        float viewRatio = viewHeight / viewWidth;
        float bmRatio = bmHeight / bmWidth;
        switch (fitStyle) {
            case "cover":
                // Keep the aspect ratio, shrink or enlarge, make both sides greater than or equal to the
                // display boundary, center display
                if (bmWidth >= viewWidth && bmHeight >= viewHeight) {
                    // shrink
                    if (viewRatio >= bmRatio) {
                        // view is higher
                        float newBmWidth = bmWidth * (viewHeight / bmHeight);
                        mImgRect.set((viewWidth - newBmWidth) / 2, 0, (viewWidth + newBmWidth) / 2,
                                viewHeight);
                        canvas.drawBitmap(bitmap, null, mImgRect, mImgPaint);
                    } else {
                        // view is shorter
                        float newBmHeight = bmHeight * (viewWidth / bmWidth);
                        mImgRect.set(
                                0, (viewHeight - newBmHeight) / 2, viewWidth,
                                (viewHeight + newBmHeight) / 2);
                        canvas.drawBitmap(bitmap, null, mImgRect, mImgPaint);
                    }
                } else if (bmWidth >= viewWidth) {
                    float newBmWidth = bmWidth * (viewHeight / bmHeight);
                    mImgRect.set((viewWidth - newBmWidth) / 2, 0, (viewWidth + newBmWidth) / 2,
                            viewHeight);
                    canvas.drawBitmap(bitmap, null, mImgRect, mImgPaint);
                } else if (bmHeight >= viewHeight) {
                    float newBmHeight = bmHeight * (viewWidth / bmWidth);
                    mImgRect.set(
                            0, (viewHeight - newBmHeight) / 2, viewWidth,
                            (viewHeight + newBmHeight) / 2);
                    canvas.drawBitmap(bitmap, null, mImgRect, mImgPaint);
                } else {
                    if (viewRatio >= bmRatio) {
                        // view is higher
                        float newBmWidth = bmWidth * (viewHeight / bmHeight);
                        mImgRect.set((viewWidth - newBmWidth) / 2, 0, (viewWidth + newBmWidth) / 2,
                                viewHeight);
                        canvas.drawBitmap(bitmap, null, mImgRect, mImgPaint);
                    } else {
                        // view is shorter
                        float newBmHeight = bmHeight * (viewWidth / bmWidth);
                        mImgRect.set(
                                0, (viewHeight - newBmHeight) / 2, viewWidth,
                                (viewHeight + newBmHeight) / 2);
                        canvas.drawBitmap(bitmap, null, mImgRect, mImgPaint);
                    }
                }
                break;
            case "contain":
                // Keep the aspect ratio, shrink or enlarge, so that the picture is displayed completely
                // within the display boundary, centered
                if (bmWidth <= viewWidth && bmHeight <= viewHeight) {
                    // 放大
                    if (viewRatio >= bmRatio) {
                        // view is higher
                        float newBmHeight = bmHeight * (viewWidth / bmWidth);
                        mImgRect.set(
                                0, (viewHeight - newBmHeight) / 2, viewWidth,
                                (viewHeight + newBmHeight) / 2);
                        canvas.drawBitmap(bitmap, null, mImgRect, mImgPaint);
                    } else {
                        // view is shorter
                        float newBmWidth = bmWidth * (viewHeight / bmHeight);
                        mImgRect.set((viewWidth - newBmWidth) / 2, 0, (viewWidth + newBmWidth) / 2,
                                viewHeight);
                        canvas.drawBitmap(bitmap, null, mImgRect, mImgPaint);
                    }
                } else if (bmWidth <= viewWidth) {
                    float newBmWidth = bmWidth * (viewHeight / bmHeight);
                    mImgRect.set((viewWidth - newBmWidth) / 2, 0, (viewWidth + newBmWidth) / 2,
                            viewHeight);
                    canvas.drawBitmap(bitmap, null, mImgRect, mImgPaint);
                } else if (bmHeight <= viewHeight) {
                    float newBmHeight = bmHeight * (viewWidth / bmWidth);
                    mImgRect.set(
                            0, (viewHeight - newBmHeight) / 2, viewWidth,
                            (viewHeight + newBmHeight) / 2);
                    canvas.drawBitmap(bitmap, null, mImgRect, mImgPaint);
                } else {
                    if (viewRatio >= bmRatio) {
                        // view is higher
                        float newBmHeight = bmHeight * (viewWidth / bmWidth);
                        mImgRect.set(
                                0, (viewHeight - newBmHeight) / 2, viewWidth,
                                (viewHeight + newBmHeight) / 2);
                        canvas.drawBitmap(bitmap, null, mImgRect, mImgPaint);
                    } else {
                        // view is shorter
                        float newBmWidth = bmWidth * (viewHeight / bmHeight);
                        mImgRect.set((viewWidth - newBmWidth) / 2, 0, (viewWidth + newBmWidth) / 2,
                                viewHeight);
                        canvas.drawBitmap(bitmap, null, mImgRect, mImgPaint);
                    }
                }
                break;
            case "scale-down":
                // Keep the aspect ratio, reduce or keep the same, take the smaller one of contain and none,
                // display in the center
                if (bmWidth <= viewWidth && bmHeight <= viewHeight) {
                    // none
                    RectF recSd =
                            new RectF(
                                    (mWidth - bmWidth) / 2,
                                    (mHeight - bmHeight) / 2,
                                    (mWidth + bmWidth) / 2,
                                    (mHeight + bmHeight) / 2);
                    canvas.drawBitmap(bitmap, null, recSd, mImgPaint);
                } else if (bmWidth <= viewWidth) {
                    float newBmWidth = bmWidth * (viewHeight / bmHeight);
                    mImgRect.set((viewWidth - newBmWidth) / 2, 0, (viewWidth + newBmWidth) / 2,
                            viewHeight);
                    canvas.drawBitmap(bitmap, null, mImgRect, mImgPaint);
                } else if (bmHeight <= viewHeight) {
                    float newBmHeight = bmHeight * (viewWidth / bmWidth);
                    mImgRect.set(
                            0, (viewHeight - newBmHeight) / 2, viewWidth,
                            (viewHeight + newBmHeight) / 2);
                    canvas.drawBitmap(bitmap, null, mImgRect, mImgPaint);
                } else {
                    if (viewRatio >= bmRatio) {
                        // view is higher
                        float newBmHeight = bmHeight * (viewWidth / bmWidth);
                        mImgRect.set(
                                0, (viewHeight - newBmHeight) / 2, viewWidth,
                                (viewHeight + newBmHeight) / 2);
                        canvas.drawBitmap(bitmap, null, mImgRect, mImgPaint);
                    } else {
                        // view is shorter
                        float newBmWidth = bmWidth * (viewHeight / bmHeight);
                        mImgRect.set((viewWidth - newBmWidth) / 2, 0, (viewWidth + newBmWidth) / 2,
                                viewHeight);
                        canvas.drawBitmap(bitmap, null, mImgRect, mImgPaint);
                    }
                }
                break;
            case "fill":
                // Do not maintain the aspect ratio, fill the display boundary
                mImgRect.set(0, 0, viewWidth, viewHeight);
                canvas.drawBitmap(bitmap, null, mImgRect, mImgPaint);
                break;
            case "none":
            default:
                // Centered, no shrink or enlarge
                mImgRect.set(
                        (mWidth - bmWidth) / 2,
                        (mHeight - bmHeight) / 2,
                        (mWidth + bmWidth) / 2,
                        (mHeight + bmHeight) / 2);
                canvas.drawBitmap(bitmap, null, mImgRect, mImgPaint);
                break;
        }
    }

    /**
     * rebuild: Generate Path objects for path type data
     */
    public void rebuildRenderData() {
        if (mRenderDataList != null && mViewport != null) {
            if (mViewport.x > 0 && mViewport.y > 0) {
                float x = mWidth / mViewport.x;
                float y = mHeight / mViewport.y;

                Matrix scaleMatrix = new Matrix();
                RectF outerRect = new RectF(x, x, y, y);
                scaleMatrix.setScale(x, y, outerRect.centerX(), outerRect.centerY());

                for (RenderData renderData : mRenderDataList) {
                    if (renderData != null) {
                        int type = renderData.getType();
                        if (type == GLYPH_DATA_TYPE_PATH) {
                            PathData pathData = (PathData) renderData;
                            try {
                                pathData.path =
                                        PathParser.createPathFromPathData(pathData.pathString);
                                if (pathData.path != null) {
                                    pathData.path.transform(scaleMatrix);
                                } else {
                                    pathData.path = new Path();
                                }
                            } catch (Exception e) {
                                pathData.path = new Path();
                            }
                        } else if (type == GLYPH_DATA_TYPE_IMG) {
                            initImgData();
                        }
                    }
                }
            }
        }
    }

    /**
     * Set the viewport width and height of the SVG. This can be found in the viewBox in the SVG. This
     * is not the size of the view.
     *
     * @param viewportWidth  the width
     * @param viewportHeight the height
     */
    public void setViewportSize(float viewportWidth, float viewportHeight) {
        if (viewportWidth > 0 && viewportHeight > 0) {
            aspectRatioWidth = viewportWidth;
            aspectRatioHeight = viewportHeight;
            mViewport = new PointF(viewportWidth, viewportHeight);
            if (mState > STATE_NOT_STARTED) {
                requestLayout();
            }
        }
    }

    private void setRenderData(List<RenderData> renderDataList) {
        mRenderDataList = renderDataList;
        if (mRenderDataList != null) {
            if (mPathFillPaint == null) {
                mPathFillPaint = new Paint();
                mPathFillPaint.setAntiAlias(true);
                mPathFillPaint.setStyle(Paint.Style.FILL);
            }
        }
    }

    /**
     * Start the svgView render
     */
    public void start() {
        changeState(STATE_FILL_STARTED);
        postInvalidateOnAnimation();
    }

    /**
     * Reset the animation render
     */
    public void reset() {
        changeState(STATE_NOT_STARTED);
        postInvalidateOnAnimation();
    }

    public int getState() {
        return mState;
    }

    private void changeState(int state) {
        if (mState == state) {
            return;
        }
        mState = state;
    }

    public boolean isAutoHide() {
        return mAutoHide;
    }

    public void setAutoHide(boolean autoHide) {
        this.mAutoHide = autoHide;
        Log.d(TAG, "LOG_SKELETON setAutoHide: " + this.mAutoHide);
    }

    public void setup(JSONObject json, int skeletonViewWidth, int skeletonViewHeight)
            throws Exception {
        long buildRenderStart = System.currentTimeMillis();
        List<RenderData> renderDataList =
                buildRenderData(json, skeletonViewWidth, skeletonViewHeight);
        Log.d(
                TAG,
                "LOG_SKELETON buildRenderData time = "
                        + (System.currentTimeMillis() - buildRenderStart));
        setRenderData(renderDataList);

        // Set the svg background color from DSL
        if (json.has("bgColor")) {
            int bgColor = ColorUtil.getColor(json.getString("bgColor"), Color.TRANSPARENT);
            if (bgColor != Color.TRANSPARENT) {
                setBackgroundColor(bgColor);
            }
        }
        rebuildRenderData();
    }

    /**
     * Generate rendering data from parsed json and set viewportSize and autoHide for svgSkeletonView
     */
    private List<RenderData> buildRenderData(
            JSONObject json, int skeletonViewWidth, int skeletonViewHeight) throws JSONException {
        int skeletonWidth = json.getInt("skeletonWidth");
        int skeletonHeight = 0;
        if (json.has("skeletonHeight")) {
            skeletonHeight = json.getInt("skeletonHeight");
        }
        int viewportWidth = skeletonWidth;
        int viewportHeight = skeletonWidth * skeletonViewHeight / skeletonViewWidth;
        setViewportSize(viewportWidth, viewportHeight);
        boolean autoHide = json.optBoolean("autoHide", true);
        setAutoHide(autoHide);
        Log.i(
                TAG,
                "LOG_SKELETON set skeleton, viewportWidth = "
                        + viewportWidth
                        + ", viewportHeight = "
                        + viewportHeight
                        + ", autoHide = "
                        + autoHide);
        List<RenderData> list = new ArrayList<>();
        File mAppResFile = Cache.getResourceDir(getContext(), mPackageName);
        if (json.has("clipPath")) {
            JSONArray clipPath = json.getJSONArray("clipPath");
            for (int i = 0; i < clipPath.length(); i++) {
                JSONObject element = clipPath.getJSONObject(i);
                RenderData renderData;
                if ("rect".equals(element.getString("tag"))) {
                    boolean footer = element.getBoolean("footer");
                    int y = element.getInt("y");
                    if (footer && skeletonHeight > 0) {
                        y = y - (skeletonHeight - viewportHeight);
                    }
                    if (y <= viewportHeight) {
                        renderData = new PathData();
                        ((PathData) renderData).pathString =
                                SvgViewUtility.rect2path(
                                        element.getInt("width"),
                                        element.getInt("height"),
                                        element.getInt("x"),
                                        y,
                                        element.getInt("rx"),
                                        element.getInt("ry"));
                        String color = element.optString("color", DEFAULT_ELE_COLOR);
                        ((PathData) renderData).color =
                                ColorUtil.getColor(color, ColorUtil.getColor(DEFAULT_ELE_COLOR));
                        list.add(renderData);
                    }
                } else if ("circle".equals(element.getString("tag"))) {
                    boolean footer = element.getBoolean("footer");
                    int cy = element.getInt("cy");
                    int r = element.getInt("r");
                    if (footer && skeletonHeight > 0) {
                        cy = cy - (skeletonHeight - viewportHeight);
                    }
                    if (cy <= viewportHeight - r) {
                        renderData = new PathData();
                        ((PathData) renderData).pathString =
                                SvgViewUtility.ellipse2path(element.getInt("cx"), cy, r, r);
                        String color = element.optString("color", DEFAULT_ELE_COLOR);
                        ((PathData) renderData).color =
                                ColorUtil.getColor(color, ColorUtil.getColor(DEFAULT_ELE_COLOR));
                        list.add(renderData);
                    }
                } else if ("ellipse".equals(element.getString("tag"))) {
                    boolean footer = element.getBoolean("footer");
                    int cy = element.getInt("cy");
                    int ry = element.getInt("ry");
                    if (footer && skeletonHeight > 0) {
                        cy = cy - (skeletonHeight - viewportHeight);
                    }
                    if (cy <= viewportHeight - ry) {
                        renderData = new PathData();
                        ((PathData) renderData).pathString =
                                SvgViewUtility.ellipse2path(element.getInt("cx"), cy,
                                        element.getInt("rx"), ry);
                        String color = element.optString("color", DEFAULT_ELE_COLOR);
                        ((PathData) renderData).color =
                                ColorUtil.getColor(color, ColorUtil.getColor(DEFAULT_ELE_COLOR));
                        list.add(renderData);
                    }
                } else if ("full-screen-img".equals(element.getString("tag"))) {
                    renderData = new ImgData();
                    ((ImgData) renderData).imgPath = element.getString("localSrc");
                    ((ImgData) renderData).fitStyle = element.getString("fitStyle");
                    File imgFile = new File(mAppResFile, ((ImgData) renderData).imgPath);
                    ((ImgData) renderData).bitMap = BitmapFactory.decodeFile(imgFile.getPath());
                    list.add(renderData);
                }
            }
        }
        return list;
    }

    private static class SvgViewUtility {
        private static String rect2path(int width, int height, int x, int y, int rx, int ry) {
            // 圆角最多一半
            if (rx > width / 2) {
                rx = width / 2;
            }
            if (ry > height / 2) {
                ry = height / 2;
            }

            // 如果其中一个设置为 0 则圆角不生效
            if (rx == 0 || ry == 0) {
                return "M" + x + " " + y + " h" + width + " v" + height + " h" + (-width) + "z";
            } else {
                return "M"
                        + x
                        + " "
                        + (y + ry)
                        + "a"
                        + rx
                        + " "
                        + ry
                        + " 0 0 1 "
                        + rx
                        + " "
                        + (-ry)
                        + "h"
                        + (width - rx - rx)
                        + "a"
                        + rx
                        + " "
                        + ry
                        + " 0 0 1 "
                        + rx
                        + " "
                        + ry
                        + "v"
                        + (height - ry - ry)
                        + "a"
                        + rx
                        + " "
                        + ry
                        + " 0 0 1 "
                        + (-rx)
                        + " "
                        + ry
                        + "h"
                        + (rx + rx - width)
                        + "a"
                        + rx
                        + " "
                        + ry
                        + " 0 0 1 "
                        + (-rx)
                        + " "
                        + (-ry)
                        + "z";
            }
        }

        private static String ellipse2path(int cx, int cy, int rx, int ry) {
            if (rx == 0 && ry > 0) {
                rx = ry;
            }
            if (ry == 0 && rx > 0) {
                ry = rx;
            }
            return "M" + (cx - rx) + " " + cy + "a" + rx + " " + ry + " 0 1 0 " + 2 * rx + " 0"
                    + "a" + rx
                    + " " + ry + " 0 1 0 " + (-2 * rx) + " 0" + "z";
        }
    }

    private abstract static class RenderData {
        abstract int getType();
    }

    private static class PathData extends RenderData {
        String pathString;
        int color;
        Path path; // rebuildRenderData()方法中赋值

        @Override
        int getType() {
            return GLYPH_DATA_TYPE_PATH;
        }

        @Override
        public String toString() {
            return "PathData{"
                    + "pathString='"
                    + pathString
                    + '\''
                    + ", color="
                    + color
                    + ", path="
                    + path
                    + '}';
        }
    }

    private static class ImgData extends RenderData {
        Bitmap bitMap;
        String imgPath;
        String fitStyle;

        @Override
        int getType() {
            return GLYPH_DATA_TYPE_IMG;
        }

        @Override
        public String toString() {
            return "ImgData{" + "imgPath='" + imgPath + '\'' + ", fitStyle='" + fitStyle + '\''
                    + '}';
        }
    }
}

/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.view.drawable;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import java.util.regex.Pattern;
import org.hapjs.common.utils.FloatUtil;
import org.hapjs.component.callback.VisibilityDrawableCallback;
import org.hapjs.component.constants.Attributes;
import org.hapjs.model.AppInfo;
import org.hapjs.runtime.HapEngine;

public class SizeBackgroundDrawable extends BitmapDrawable {

    private static final String TAG = "SizeBackgroundDrawable";
    private static final String DEFAULT_SIZE_UNDER_1080 = "100% 100%";
    private static final int DEFAULT_SIZE_AUTO_VERSION = 1080;

    private View mHostView;

    private Matrix mScaleMatrix = new Matrix();

    private BackgroundSize mBackgroundSize;
    private RepeatMode mBackgroundRepeatMode = RepeatMode.REPEAT;
    private Position mPosition;
    private String mBackgroundUrl;

    // prevent bitmap cache in BitmapUtils from being removed
    private ConstantState mConstantStateSource;
    private VisibilityDrawableCallback mVisibilityCallback;

    private HapEngine mHapEngine;
    private Path mOutlinePath;

    public SizeBackgroundDrawable(
            HapEngine hapEngine,
            Resources res,
            BitmapDrawable drawable,
            VisibilityDrawableCallback visibilityCallback) {
        super(res, drawable.getBitmap());
        mHapEngine = hapEngine;
        mConstantStateSource = drawable.getConstantState();
        mVisibilityCallback = visibilityCallback;
    }

    public void setHostView(View hostView) {
        mHostView = hostView;
    }

    public String getBackgroundUrl() {
        return mBackgroundUrl;
    }

    public void setBackgroundUrl(String backgroundUrl) {
        if (TextUtils.equals(backgroundUrl, mBackgroundUrl)) {
            return;
        }
        mBackgroundUrl = backgroundUrl;
    }

    public String getBackgroundSizeStr() {
        return mBackgroundSize != null ? mBackgroundSize.getDesc() : null;
    }

    public void setBackgroundSize(String backgroundSize) {
        if (mBackgroundSize != null
                && TextUtils.equals(backgroundSize, mBackgroundSize.getDesc())) {
            return;
        }
        mBackgroundSize = BackgroundSize.parse(mHapEngine, backgroundSize);
        invalidateSelf();
    }

    public String getBackgroundRepeatStr() {
        return mBackgroundRepeatMode.getDesc();
    }

    public void setBackgroundRepeat(String repeatMode) {
        if (TextUtils.equals(repeatMode, mBackgroundRepeatMode.getDesc())) {
            return;
        }
        mBackgroundRepeatMode = RepeatMode.parse(repeatMode);
        invalidateSelf();
    }

    public String getBackgroundPositionStr() {
        return mPosition != null ? mPosition.getParseStr() : null;
    }

    public Position getPosition() {
        return mPosition;
    }

    public void setBackgroundPosition(String positionStr) {
        if (mPosition != null && TextUtils.equals(positionStr, mPosition.getParseStr())) {
            return;
        }
        mPosition = Position.parse(positionStr);
        invalidateSelf();
    }

    @Override
    public boolean setVisible(boolean visible, boolean restart) {
        if (mVisibilityCallback != null) {
            mVisibilityCallback.onVisibilityChange(visible);
        }
        return super.setVisible(visible, restart);
    }

    @Override
    public void draw(Canvas canvas) {
        // for background drawable start
        if (!isVisible()) {
            return;
        }
        // check bitmap state
        if (getBitmap() == null || getBitmap().isRecycled()) {
            return;
        }
        if (mVisibilityCallback != null) {
            boolean isNeedReDraw = mVisibilityCallback.onDraw(mBackgroundUrl);
            if (!isNeedReDraw) {
                return;
            }
        }
        // for background drawable end
        if (mHostView == null) {
            if (getGravity() != Gravity.FILL) {
                setGravity(Gravity.FILL);
            }
            innerDraw(canvas);
            return;
        }

        final int containerWidth = mHostView.getWidth();
        final int containerHeight = mHostView.getHeight();
        final int bitmapWidth = getIntrinsicWidth();
        final int bitmapHeight = getIntrinsicHeight();
        if (containerWidth <= 0 || containerHeight <= 0 || bitmapWidth <= 0 || bitmapHeight <= 0) {
            if (getGravity() != Gravity.FILL) {
                setGravity(Gravity.FILL);
            }
            innerDraw(canvas);
            return;
        }
        if (mBackgroundSize == null) {
            if (getGravity() != Gravity.FILL) {
                setGravity(Gravity.FILL);
            }
            if (mHapEngine != null && mHapEngine.getApplicationContext().getAppInfo() != null) {
                AppInfo appInfo = mHapEngine.getApplicationContext().getAppInfo();
                int minPlatformCode = appInfo.getMinPlatformVersion();
                if (minPlatformCode >= DEFAULT_SIZE_AUTO_VERSION) {
                    // 1080+ 版本为对齐前端默认值 auto，也为满足 background-position 动画需求
                    // background-size 不能为 null
                    mBackgroundSize = new BackgroundSize();
                } else {
                    mBackgroundSize = BackgroundSize.parse(mHapEngine, DEFAULT_SIZE_UNDER_1080);
                }
            } else {
                // 兼容低版本默认值 100% 100%
                mBackgroundSize = BackgroundSize.parse(mHapEngine, DEFAULT_SIZE_UNDER_1080);
            }
        }

        float backgroundWidth = 0f;
        float backgroundHeight = 0f;
        if (Attributes.ImageMode.CONTAIN.equals(mBackgroundSize.mImageMode)) {
            float widthScale = (float) containerWidth / bitmapWidth;
            float heightScale = (float) containerHeight / bitmapHeight;
            float scale = Math.min(widthScale, heightScale);
            backgroundWidth = bitmapWidth * scale;
            backgroundHeight = bitmapHeight * scale;
        } else if (Attributes.ImageMode.COVER.equals(mBackgroundSize.mImageMode)) {
            float widthScale = (float) containerWidth / bitmapWidth;
            float heightScale = (float) containerHeight / bitmapHeight;
            float scale = Math.max(widthScale, heightScale);
            backgroundWidth = bitmapWidth * scale;
            backgroundHeight = bitmapHeight * scale;
        } else {
            if (mBackgroundSize.mWidthUnit == BackgroundSize.SIZE_UNIT_PERCENT) {
                backgroundWidth = mBackgroundSize.mWidth * containerWidth;
            } else if (mBackgroundSize.mWidthUnit == BackgroundSize.SIZE_UNIT_PX) {
                backgroundWidth = mBackgroundSize.mWidth;
            }
            if (mBackgroundSize.mHeightUnit == BackgroundSize.SIZE_UNIT_PERCENT) {
                backgroundHeight = mBackgroundSize.mHeight * containerHeight;
            } else if (mBackgroundSize.mHeightUnit == BackgroundSize.SIZE_UNIT_PX) {
                backgroundHeight = mBackgroundSize.mHeight;
            }
            // auto
            if (backgroundWidth <= 0) {
                if (backgroundHeight <= 0) {
                    // width:auto height:auto
                    backgroundWidth = bitmapWidth;
                    backgroundHeight = bitmapHeight;
                } else {
                    // width:auto height:fix
                    backgroundWidth = backgroundHeight / bitmapHeight * bitmapWidth;
                }
            } else {
                if (backgroundHeight <= 0) {
                    // width:fix height:auto
                    backgroundHeight = backgroundWidth / bitmapWidth * bitmapHeight;
                }
            }
        }

        if (backgroundWidth <= 0 || backgroundHeight <= 0) {
            // error, fix to fitxy（100% 100%）
            if (getGravity() != Gravity.FILL) {
                setGravity(Gravity.FILL);
            }
            innerDraw(canvas);
            return;
        }

        if (mPosition == null) {
            if (getGravity() != (Gravity.START | Gravity.TOP)) {
                setGravity(Gravity.START | Gravity.TOP);
            }
        } else {
            mPosition.setRelativeSize(
                    mHostView.getWidth(),
                    mHostView.getHeight(),
                    (int) backgroundWidth,
                    (int) backgroundHeight);
            mPosition.calculatePx(mHapEngine);
            mVisibilityCallback.onPositionCalculated(mPosition);
        }

        repeatDraw(canvas, backgroundWidth, backgroundHeight);
    }

    private void repeatDraw(Canvas canvas, float backgroundWidth, float backgroundHeight) {

        int width = getIntrinsicWidth();
        int height = getIntrinsicHeight();

        Paint paint = getPaint();
        if (paint == null) {
            paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        }

        if (mPosition != null) {
            mScaleMatrix.setTranslate(mPosition.mPositionX, mPosition.mPositionY);
        } else {
            mScaleMatrix.setTranslate(0, 0);
        }
        if (width > 0 && height > 0) {
            mScaleMatrix.preScale(backgroundWidth / width, backgroundHeight / height);
        }

        Bitmap bgBitmap = null;
        Bitmap imgBitmap = getBitmap();
        Rect bounds = getBounds();

        Shader.TileMode modeX = Shader.TileMode.REPEAT;
        Shader.TileMode modeY = Shader.TileMode.REPEAT;
        switch (mBackgroundRepeatMode) {
            case REPEAT_X:
                modeY = Shader.TileMode.CLAMP;
                if (backgroundHeight < bounds.height()) {
                    bgBitmap =
                            Bitmap.createBitmap((int) backgroundWidth, bounds.height(),
                                    Bitmap.Config.ARGB_8888);
                    Canvas bgCanvas = new Canvas(bgBitmap);
                    bgCanvas.drawBitmap(getBitmap(), mScaleMatrix, paint);
                }
                break;
            case REPEAT_Y:
                modeX = Shader.TileMode.CLAMP;
                if (backgroundWidth < bounds.width()) {
                    bgBitmap =
                            Bitmap.createBitmap(bounds.width(), (int) backgroundHeight,
                                    Bitmap.Config.ARGB_8888);
                    Canvas bgCanvas = new Canvas(bgBitmap);
                    bgCanvas.drawBitmap(getBitmap(), mScaleMatrix, paint);
                }
                break;
            case REPEAT_NONE:
                modeX = Shader.TileMode.CLAMP;
                modeY = Shader.TileMode.CLAMP;
                if (backgroundWidth < bounds.width() || backgroundHeight < bounds.height()) {
                    bgBitmap = Bitmap.createBitmap(bounds.width(), bounds.height(),
                            Bitmap.Config.ARGB_8888);
                    Canvas bgCanvas = new Canvas(bgBitmap);
                    bgCanvas.drawBitmap(getBitmap(), mScaleMatrix, paint);
                }
                break;
            default:
                break;
        }

        BitmapShader shader;
        if (bgBitmap != null) {
            shader = new BitmapShader(bgBitmap, modeX, modeY);
        } else {
            shader = new BitmapShader(imgBitmap, modeX, modeY);
            shader.setLocalMatrix(mScaleMatrix);
        }

        paint.setShader(shader);
        paint.setStyle(Paint.Style.FILL);
        if (mOutlinePath != null) {
            canvas.drawPath(mOutlinePath, paint);
        } else {
            canvas.drawRect(getBounds(), paint);
        }
    }

    public void setOutlinePath(Path path) {
        mOutlinePath = path;
    }

    private void innerDraw(Canvas canvas) {
        float backgroundWidth = getBounds().width();
        float backgroundHeight = getBounds().height();
        int width = getIntrinsicWidth();
        int height = getIntrinsicHeight();
        if (width > 0 && height > 0) {
            mScaleMatrix.setScale(backgroundWidth / width, backgroundHeight / height);
        }
        Paint paint = getPaint();
        if (paint == null) {
            paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        }
        paint.setShader(
                new BitmapShader(getBitmap(), Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
        paint.getShader().setLocalMatrix(mScaleMatrix);
        paint.setStyle(Paint.Style.FILL);
        if (mOutlinePath != null) {
            canvas.drawPath(mOutlinePath, paint);
        } else {
            canvas.drawRect(getBounds(), paint);
        }
    }

    public enum RepeatMode {
        REPEAT(Attributes.RepeatMode.REPEAT),
        REPEAT_X(Attributes.RepeatMode.REPEAT_X),
        REPEAT_Y(Attributes.RepeatMode.REPEAT_Y),
        REPEAT_NONE(Attributes.RepeatMode.REPEAT_NONE);

        private String mDesc;

        RepeatMode(String desc) {
            mDesc = desc;
        }

        public static RepeatMode parse(String repeatMode) {
            for (RepeatMode mode : values()) {
                if (TextUtils.equals(repeatMode, mode.getDesc())) {
                    return mode;
                }
            }
            return RepeatMode.REPEAT;
        }

        public String getDesc() {
            return mDesc;
        }
    }

    private static final class BackgroundSize {
        // auto
        private static final int SIZE_UNIT_AUTO = 0;
        // px
        private static final int SIZE_UNIT_PX = 1;
        // percent
        private static final int SIZE_UNIT_PERCENT = 2;

        private String mDesc;
        private String mImageMode = Attributes.ImageMode.NONE;
        private float mWidth = 0f;
        private float mHeight = 0f;
        private int mWidthUnit = SIZE_UNIT_AUTO;
        private int mHeightUnit = SIZE_UNIT_AUTO;

        private BackgroundSize(String imageMode, String desc) {
            mImageMode = imageMode;
            mDesc = desc;
        }

        private BackgroundSize() {
        }

        static BackgroundSize parse(HapEngine hapEngine, String backgroundSize) {
            if (backgroundSize == null
                    || TextUtils.equals(backgroundSize, Attributes.ImageMode.NONE)) {
                int minPlatformVersion;
                if (hapEngine != null) {
                    AppInfo appInfo = hapEngine.getApplicationContext().getAppInfo();
                    if (appInfo != null) {
                        minPlatformVersion = appInfo.getMinPlatformVersion();
                        // 1080+ 版本为对齐前端，满足 background-position 需求，background-size 不应返回 null
                        if (minPlatformVersion >= DEFAULT_SIZE_AUTO_VERSION) {
                            return new BackgroundSize();
                        }
                    }
                }
                // 兼容 1080 以下版本 默认值为 100% 100%
                return parse(hapEngine, DEFAULT_SIZE_UNDER_1080);
            }
            BackgroundSize size;
            if (Attributes.ImageMode.CONTAIN.equals(backgroundSize)) {
                size = new BackgroundSize(Attributes.ImageMode.CONTAIN, backgroundSize);
            } else if (Attributes.ImageMode.COVER.equals(backgroundSize)) {
                size = new BackgroundSize(Attributes.ImageMode.COVER, backgroundSize);
            } else {
                size = new BackgroundSize(Attributes.ImageMode.NONE, backgroundSize);
                String[] sizes = backgroundSize.split(" ");
                int len = sizes.length;
                String widthStr = null;
                if (len >= 1){
                    widthStr = sizes[0];
                }
                String heightStr = null;
                if (len >= 2) {
                    heightStr = sizes[1];
                }
                if (widthStr != null) {
                    if (widthStr.endsWith("%")) {
                        String temp = widthStr.substring(0, widthStr.indexOf("%"));
                        size.setWidth(Float.parseFloat(temp) / 100f, BackgroundSize.SIZE_UNIT_PERCENT);
                    } else if (widthStr.endsWith("px")) {
                        size.setWidth(Attributes.getFloat(hapEngine, widthStr), BackgroundSize.SIZE_UNIT_PX);
                    } else {
                        //fix bug,when the width is NaN,the background-image cannot display.
                        if (FloatUtil.isUndefined(Attributes.getFloat(hapEngine, widthStr))) {
                            size.setWidth(0, BackgroundSize.SIZE_UNIT_AUTO);
                        } else {
                            size.setWidth(Attributes.getFloat(hapEngine, widthStr), BackgroundSize.SIZE_UNIT_PX);
                        }
                    }
                } else {
                    //auto
                    size.setWidth(0, BackgroundSize.SIZE_UNIT_AUTO);
                }
                if (heightStr != null) {
                    if (heightStr.endsWith("%")) {
                        String temp = heightStr.substring(0, heightStr.indexOf("%"));
                        size.setHeight(Float.parseFloat(temp) / 100f,
                                BackgroundSize.SIZE_UNIT_PERCENT);
                    } else if (heightStr.endsWith("px")) {
                        size.setHeight(Attributes.getFloat(hapEngine, heightStr),
                                BackgroundSize.SIZE_UNIT_PX);
                    } else {
                        // fix bug,when the height is NaN,the background-image cannot display.
                        if (FloatUtil.isUndefined(Attributes.getFloat(hapEngine, heightStr))) {
                            size.setHeight(0, BackgroundSize.SIZE_UNIT_AUTO);
                        } else {
                            size.setHeight(
                                    Attributes.getFloat(hapEngine, heightStr),
                                    BackgroundSize.SIZE_UNIT_PX);
                        }
                    }
                } else {
                    // auto
                    size.setHeight(0, BackgroundSize.SIZE_UNIT_AUTO);
                }
            }
            return size;
        }

        public String getDesc() {
            return mDesc;
        }

        void setWidth(float width, int unit) {
            mWidth = width;
            mWidthUnit = unit;
        }

        void setHeight(float height, int unit) {
            mHeight = height;
            mHeightUnit = unit;
        }
    }

    public static final class Position {

        public static final int UNIT_PX = 1;
        public static final int UNIT_PERCENT = 2;
        public static final int UNIT_PERCENT_OFFSET = 3;
        private static final int UNIT_UNDEFINED = 0;
        private static final String PERCENT = "%";
        private static final String PX = "px";

        private static final int POSITION_ONE_PARAM = 1;
        private static final int POSITION_TWO_PARAM = 2;
        private static final int POSITION_THREE_PARAM = 3;
        private static final int POSITION_FOUR_PARAM = 4;

        private static final String TOP = "top";
        private static final String CENTER = "center";
        private static final String BOTTOM = "bottom";
        private static final String LEFT = "left";
        private static final String RIGHT = "right";

        private float mParseX = 0;
        private float mOffsetX = 0;
        private int mXUnit = UNIT_UNDEFINED;
        private float mParseY = 0;
        private float mOffsetY = 0;
        private int mYUnit = UNIT_UNDEFINED;

        private int mPositionX = 0;
        private int mPositionY = 0;

        private String mParseStr;

        private int mRelativeWidth;
        private int mRelativeHeight;

        Position() {
        }

        public static Position parse(String positionStr) {
            Position position = new Position();
            position.parsePosition(positionStr);
            return position;
        }

        public String getParseStr() {
            return mParseStr;
        }

        private void setRelativeSize(
                int containerWidth, int containerHeight, int backgroundWidth,
                int backgroundHeight) {

            mRelativeWidth = containerWidth - backgroundWidth;
            mRelativeHeight = containerHeight - backgroundHeight;
        }

        public void setRelativeSize(int relativeWidth, int relativeHeight) {
            mRelativeWidth = relativeWidth;
            mRelativeHeight = relativeHeight;
        }

        /**
         * 将字符串解析成px值/百分比/百分比加偏移量
         *
         * @param str
         */
        public void parsePosition(String str) {

            if (TextUtils.equals(str, mParseStr)) {
                return;
            }
            mParseStr = str;
            if (TextUtils.isEmpty(str)) {
                setDefault();
                return;
            }

            mXUnit = UNIT_UNDEFINED;
            mYUnit = UNIT_UNDEFINED;

            // split the parameters
            String regex = " +";
            Pattern pattern = Pattern.compile(regex);
            String[] raw = pattern.split(str.trim());

            if (raw.length > POSITION_FOUR_PARAM || raw.length < POSITION_ONE_PARAM) {
                setDefault();
                return;
            }

            if (raw.length == POSITION_ONE_PARAM) {
                parseOneParam(raw[0]);
                return;
            }

            if (raw.length == POSITION_TWO_PARAM) {
                parseTwoParams(raw[0], raw[1]);
                return;
            }

            if (raw.length == POSITION_THREE_PARAM) {
                parseThreeParams(raw[0], raw[1], raw[2]);
                return;
            }

            parseFourParams(raw[0], raw[1], raw[2], raw[3]);
        }

        private void parseOneParam(String str) {

            if (str.endsWith(PX) || str.endsWith(PERCENT)) {
                setTwoSpecifiedValue(LEFT, str);
                setTwoSpecifiedValue(TOP, "50%");
                return;
            }

            switch (str) {
                case CENTER:
                    setTwoSpecifiedValue(LEFT, "50%");
                    setTwoSpecifiedValue(TOP, "50%");
                    return;
                case TOP:
                case BOTTOM:
                    setOneSpecifiedValue(str);
                    setTwoSpecifiedValue(LEFT, "50%");
                    return;
                case LEFT:
                case RIGHT:
                    setOneSpecifiedValue(str);
                    setTwoSpecifiedValue(TOP, "50%");
                    return;
                default:
                    setDefault();
                    return;
            }

            // Never get here.

        }

        /**
         * 解析两个参数
         *
         * @param str1 待解析的第一个参数
         * @param str2 待解析的第二个参数
         */
        private void parseTwoParams(String str1, String str2) {

            if (str1.endsWith(PX) || str1.endsWith(PERCENT)) {
                setTwoSpecifiedValue(LEFT, str1);

                if (mYUnit != UNIT_UNDEFINED) {
                    return;
                }
            } else {
                switch (str1) {
                    case LEFT:
                    case RIGHT:
                    case TOP:
                    case BOTTOM:
                        setOneSpecifiedValue(str1);
                        break;
                    case CENTER:
                        break;
                    default:
                        setDefault();
                        return;
                }
            }

            if (str2.endsWith(PX) || str2.endsWith(PERCENT)) {
                setTwoSpecifiedValue(TOP, str2);

                if (CENTER.equals(str1)) {
                    setTwoSpecifiedValue(LEFT, "50%");
                }
            } else {
                switch (str2) {
                    case LEFT:
                    case RIGHT:
                        if (CENTER.equals(str1)) {
                            setTwoSpecifiedValue(TOP, "50%");
                        }
                        setOneSpecifiedValue(str2);
                        return;
                    case TOP:
                    case BOTTOM:
                        if (CENTER.equals(str1)) {
                            setTwoSpecifiedValue(LEFT, "50%");
                        }
                        setOneSpecifiedValue(str2);
                        return;
                    case CENTER:
                        switch (str1) {
                            case TOP:
                            case BOTTOM:
                                setTwoSpecifiedValue(LEFT, "50%");
                                return;
                            case CENTER:
                                setTwoSpecifiedValue(LEFT, "50%");
                                setTwoSpecifiedValue(TOP, "50%");
                                return;
                            case LEFT:
                            case RIGHT:
                            default:
                                setTwoSpecifiedValue(TOP, "50%");
                                return;
                        }
                    default:
                        setDefault();
                        return;
                }
            }
        }

        /**
         * 解析三个参数 参数有效的情况分为两类进行讨论： 第一类，第一个参数是位置标识符，即属于{left,right,top,bottom} 第二类，第二个参数是位置标识符
         *
         * @param str1 待解析的第一个参数
         * @param str2 待解析的第二个参数
         * @param str3 待解析的第三个参数
         */
        private void parseThreeParams(String str1, String str2, String str3) {

            switch (str1) {
                case LEFT:
                case RIGHT:
                case TOP:
                case BOTTOM:
                    setDataForTheFirstCaseInThreeParams(str1, str2, str3);
                    return;
                case CENTER:
                    setDataForTheSecondCaseInThreeParams(str2, str3);
                    return;
                default:
                    setDefault();
                    return;
            }

            // Never get here.
        }

        private void setDataForTheFirstCaseInThreeParams(String str1, String str2, String str3) {

            if (str2.endsWith(PX) || str2.endsWith(PERCENT)) {

                switch (str3) {
                    case LEFT:
                    case RIGHT:
                    case TOP:
                    case BOTTOM:
                        setOneSpecifiedValue(str3);
                        break;
                    case CENTER:
                        switch (str1) {
                            case LEFT:
                            case RIGHT:
                                setTwoSpecifiedValue(TOP, "50%");
                                break;
                            case TOP:
                            case BOTTOM:
                                setTwoSpecifiedValue(LEFT, "50%");
                                break;
                            default:
                                // Never get here.
                                Log.e(TAG,
                                        "setDataForTheFirstCaseInThreeParams: Never get here. value:"
                                                + str1);
                        }
                        break;
                    default:
                        setDefault();
                        return;
                }

                setTwoSpecifiedValue(str1, str2);

                return;
            }

            switch (str2) {
                case LEFT:
                case RIGHT:
                case TOP:
                case BOTTOM:
                    switch (str1) {
                        case LEFT:
                        case RIGHT:
                        case TOP:
                        case BOTTOM:
                            setOneSpecifiedValue(str1);
                            break;
                        default:
                            // Never get here.
                            Log.e(TAG,
                                    "setDataForTheFirstCaseInThreeParams: Never get here. value:"
                                            + str1);
                    }
                    setTwoSpecifiedValue(str2, str3);

                    break;
                default:
                    setDefault();
                    return;
            }
        }

        private void setDataForTheSecondCaseInThreeParams(String str2, String str3) {

            switch (str2) {
                case LEFT:
                case RIGHT:
                    setTwoSpecifiedValue(TOP, "50%");
                    break;
                case TOP:
                case BOTTOM:
                    setTwoSpecifiedValue(LEFT, "50%");
                    break;
                default:
                    setDefault();
                    return;
            }

            setTwoSpecifiedValue(str2, str3);
        }

        /**
         * 解析四个参数
         *
         * @param str1 待解析的第一个参数，只能为位置标识符，即属于{left,right,top,bottom}
         * @param str2 待解析的第二个参数，只能为位置偏移量，即属于{px,%}
         * @param str3 待解析的第三个参数，只能为位置标识符
         * @param str4 待解析的第四个参数，只能为位置偏移量
         */
        private void parseFourParams(String str1, String str2, String str3, String str4) {

            setTwoSpecifiedValue(str1, str2);
            setTwoSpecifiedValue(str3, str4);
        }

        private void setOneSpecifiedValue(String position) {

            switch (position) {
                case LEFT:
                    setTwoSpecifiedValue(LEFT, "0px");
                    break;
                case RIGHT:
                    setTwoSpecifiedValue(LEFT, "100%");
                    break;
                case TOP:
                    setTwoSpecifiedValue(TOP, "0px");
                    break;
                case BOTTOM:
                    setTwoSpecifiedValue(TOP, "100%");
                    break;
                default:
                    // Never get here.
                    Log.e(TAG, "setOneSpecifiedValue: Never get here. value:" + position);
            }
        }

        private void setTwoSpecifiedValue(String position, String value) {

            // 位置标签合法性及互斥性检查
            switch (position) {
                case LEFT:
                case RIGHT:
                    if (mXUnit != UNIT_UNDEFINED) {
                        setDefault();
                        return;
                    }
                    break;
                case TOP:
                case BOTTOM:
                    if (mYUnit != UNIT_UNDEFINED) {
                        setDefault();
                        return;
                    }
                    break;
                default:
                    setDefault();
                    return;
            }

            float num = 0;
            int unit = 0;

            try {
                if (value.endsWith(PX)) {
                    num = Float.parseFloat(value.substring(0, value.length() - PX.length()));
                    unit = UNIT_PX;
                } else if (value.endsWith(PERCENT)) {
                    num = Float.parseFloat(value.substring(0, value.length() - PERCENT.length()));
                    unit = UNIT_PERCENT;
                } else {
                    setDefault();
                    return;
                }
            } catch (NumberFormatException e) {
                setDefault();
                return;
            }

            switch (position) {
                case LEFT:
                    mParseX = num;
                    mXUnit = unit;
                    break;
                case RIGHT:
                    switch (unit) {
                        case UNIT_PERCENT:
                            mParseX = 100 - num;
                            mXUnit = unit;
                            break;
                        case UNIT_PX:
                            mParseX = 100;
                            mOffsetX = -num;
                            mXUnit = UNIT_PERCENT_OFFSET;
                            break;
                        default:
                            // Never get here.
                            Log.e(
                                    TAG,
                                    "setTwoSpecifiedValue: Never get here. position:" + position
                                            + " unit:" + unit);
                    }
                    break;
                case TOP:
                    mParseY = num;
                    mYUnit = unit;
                    break;
                case BOTTOM:
                    switch (unit) {
                        case UNIT_PERCENT:
                            mParseY = 100 - num;
                            mYUnit = unit;
                            break;
                        case UNIT_PX:
                            mParseY = 100;
                            mOffsetY = -num;
                            mYUnit = UNIT_PERCENT_OFFSET;
                            break;
                        default:
                            // Never get here.
                            Log.e(
                                    TAG,
                                    "setTwoSpecifiedValue: Never get here. position:" + position
                                            + " unit:" + unit);
                    }
                    break;
                default:
                    // Never get here.
                    Log.e(TAG, "setTwoSpecifiedValue: Never get here. value:" + position);
            }
        }

        public void setDefault() {

            mParseX = 0;
            mOffsetX = 0;
            mXUnit = UNIT_PX;
            mParseY = 0;
            mOffsetY = 0;
            mYUnit = UNIT_PX;
        }

        public void calculatePx(HapEngine hapEngine) {

            switch (mXUnit) {
                case UNIT_PX:
                    mPositionX = (int) Attributes.getFloat(hapEngine, mParseX + PX);
                    break;
                case UNIT_PERCENT:
                    mPositionX = (int) ((mParseX) / 100 * mRelativeWidth);
                    break;
                case UNIT_PERCENT_OFFSET:
                    mPositionX = (int) (mParseX / 100 * mRelativeWidth);
                    mPositionX += (int) Attributes.getFloat(hapEngine, mOffsetX + PX);
                    break;
                default:
                    Log.e(TAG, "calculatePx: Position x's unit is not defined.");
            }

            switch (mYUnit) {
                case UNIT_PX:
                    mPositionY = (int) Attributes.getFloat(hapEngine, mParseY + PX);
                    break;
                case UNIT_PERCENT:
                    mPositionY = (int) (mParseY / 100 * mRelativeHeight);
                    break;
                case UNIT_PERCENT_OFFSET:
                    mPositionY = (int) (mParseY / 100 * mRelativeHeight);
                    mPositionY += (int) Attributes.getFloat(hapEngine, mOffsetY + PX);
                    break;
                default:
                    Log.e(TAG, "calculatePx: Position y's unit is not defined.");
            }
        }

        public float getParseX() {

            return mParseX;
        }

        public float getOffsetX() {
            return mOffsetX;
        }

        public int getXUnit() {

            return mXUnit;
        }

        public float getParseY() {

            return mParseY;
        }

        public float getOffsetY() {
            return mOffsetY;
        }

        public int getYUnit() {

            return mYUnit;
        }

        public int getRelativeWidth() {
            return mRelativeWidth;
        }

        public int getRelativeHeight() {
            return mRelativeHeight;
        }

        public int getPositionX() {
            return mPositionX;
        }

        public int getPositionY() {
            return mPositionY;
        }
    }
}

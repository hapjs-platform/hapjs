/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.DynamicDrawableSpan;
import android.text.style.ImageSpan;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatDelegate;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.image.CloseableImage;

import org.hapjs.bridge.annotation.WidgetAnnotation;
import org.hapjs.common.utils.BitmapUtils;
import org.hapjs.common.utils.DisplayUtil;
import org.hapjs.common.utils.FloatUtil;
import org.hapjs.component.Component;
import org.hapjs.component.Container;
import org.hapjs.component.animation.Transform;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.component.constants.Attributes;
import org.hapjs.component.constants.Corner;
import org.hapjs.render.Autoplay;
import org.hapjs.runtime.ConfigurationManager;
import org.hapjs.runtime.DarkThemeUtil;
import org.hapjs.runtime.HapConfiguration;
import org.hapjs.runtime.HapEngine;
import org.hapjs.runtime.ProviderManager;
import org.hapjs.system.SysOpProvider;
import org.hapjs.widgets.text.Text;
import org.hapjs.widgets.view.image.FlexImageView;
import org.json.JSONObject;

@WidgetAnnotation(
        name = Image.WIDGET_NAME,
        methods = {
                Component.METHOD_ANIMATE,
                Component.METHOD_GET_BOUNDING_CLIENT_RECT,
                Component.METHOD_TO_TEMP_FILE_PATH,
                Component.METHOD_FOCUS,
                Image.METHOD_START_ANIMATION,
                Image.METHOD_STOP_ANIMAION
        }
)
public class Image extends Component<FlexImageView> implements Autoplay, InnerSpannable {

    protected static final String WIDGET_NAME = "image";
    protected static final String RESULT_WIDTH = "width";
    protected static final String RESULT_HEIGHT = "height";

    protected static final String EVENT_COMPLETE = "complete";
    protected static final String EVENT_ERROR = "error";

    protected static final String BLUR = "blur";

    protected static final String BLANK = "blank";

    // default true
    // true：#000，alpha 50%
    protected static final String ENABLE_NIGHT_MODE = "enablenightmode";

    protected static final String METHOD_START_ANIMATION = "startAnimation";
    protected static final String METHOD_STOP_ANIMAION = "stopAnimation";
    protected static final String AUTOPLAY = "autoplay";

    private boolean mHasCompleteListener = false;
    private boolean mHasErrorListener = false;
    private boolean mIsSrcInit = false;
    private boolean mEnableNightMode = true;
    private boolean mHasSetForceDark = false;
    private OnConfigurationListener mConfigurationListener;

    private static final String STYLE_ALIGN = "align";
    private static final String ALIGN_BOTTOM = "bottom";
    private static final String ALIGN_BASELINE = "baseline";

    private Text mParentText;
    private SpannableString mSpannable;
    private boolean mIsImageSpan = false;
    private boolean mSetImageBlur = false;

    private static final int DEFAULT_ADAPTIVE_BANNER_PADDING = 400;

    private String mSrcStr;
    private String mAltStr;
    private int mImgWidth;
    private int mImgHeight;
    private int mVerticalAlignment = DynamicDrawableSpan.ALIGN_BASELINE;

    public Image(
            HapEngine hapEngine,
            Context context,
            Container parent,
            int ref,
            RenderEventCallback callback,
            Map<String, Object> savedState) {
        super(hapEngine, context, parent, ref, callback, savedState);
        mParentText = getParentText();
        if (mParentText != null) {
            mIsImageSpan = true;
            return;
        }
        mConfigurationListener = new OnConfigurationListener(this);
        ConfigurationManager.getInstance().addListener(mConfigurationListener);
    }

    public OnConfigurationListener getConfigurationListener() {
        return mConfigurationListener;
    }

    @Override
    protected FlexImageView createViewImpl() {
        if (mIsImageSpan) {
            // just a spannable.
            return null;
        }
        FlexImageView imageView = new FlexImageView(mContext);
        imageView.setComponent(this);

        imageView.setOnLoadStatusListener(
                new FlexImageView.OnLoadStatusListener() {
                    @Override
                    public void onComplete(int width, int height) {
                        if (mHasCompleteListener) {
                            Map<String, Object> params = new HashMap<>();
                            params.put(
                                    RESULT_WIDTH, DisplayUtil.getDesignPxByWidth(width,
                                            mHapEngine.getDesignWidth()));
                            params.put(
                                    RESULT_HEIGHT,
                                    DisplayUtil.getDesignPxByWidth(height,
                                            mHapEngine.getDesignWidth()));
                            mCallback.onJsEventCallback(
                                    getPageId(), mRef, EVENT_COMPLETE, Image.this, params, null);
                        }
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        if (mHasErrorListener) {
                            mCallback.onJsEventCallback(getPageId(), mRef, EVENT_ERROR, Image.this,
                                    null, null);
                        }
                    }
                });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            imageView.setForceDarkAllowed(false);
        }
        setNightMode(imageView, DarkThemeUtil.isDarkMode(mContext));
        return imageView;
    }

    @Override
    protected boolean setAttribute(String key, Object attribute) {
        if (mIsImageSpan) {
            return setImageSpanAttribute(key, attribute);
        }
        switch (key) {
            case Attributes.Style.SRC:
                mIsSrcInit = true;
                String src = Attributes.getString(attribute);
                setSrc(src);
                setImageBlur();
                return true;
            case Attributes.Style.RESIZE_MODE:
            case Attributes.Style.OBJECT_FIT:
                String resizeMode = Attributes.getString(attribute, Attributes.ObjectFit.COVER);
                setObjectFit(resizeMode);
                return true;
            case Attributes.Style.ALT_OBJECT_FIT:
                String altObjectFit = Attributes.getString(attribute, Attributes.ObjectFit.COVER);
                setAltObjectFit(altObjectFit);
                return true;
            case Attributes.Style.FILTER:
                setFilter(attribute);
                return true;
            case Attributes.Style.ALT:
                String alt = Attributes.getString(attribute);
                setAlt(alt);
                return true;
            case Attributes.Style.WIDTH:
                String width = Attributes.getString(attribute, "");
                int lastWidth = getWidth();
                setWidth(width);
                if (lastWidth != getWidth()) {
                    retrySrc();
                }
                return true;
            case Attributes.Style.HEIGHT:
                String height = Attributes.getString(attribute, "");
                int lastHeight = getHeight();
                setHeight(height);
                if (lastHeight != getHeight()) {
                    retrySrc();
                }
                return true;
            case ENABLE_NIGHT_MODE:
                if (!mHasSetForceDark) {
                    mEnableNightMode = Attributes.getBoolean(attribute, true);
                    setNightMode(mHost, DarkThemeUtil.isDarkMode(mContext));
                }
                return true;
            case Attributes.Style.FORCE_DARK:
                mHasSetForceDark = true;
                mEnableNightMode = Attributes.getBoolean(attribute, true);
                setNightMode(mHost, DarkThemeUtil.isDarkMode(mContext));
                return true;
            case AUTOPLAY:
                boolean autoplay = Attributes.getBoolean(attribute, true);
                setAutoplay(autoplay);
                return true;
            default:
                break;
        }
        return super.setAttribute(key, attribute);
    }

    private boolean setImageSpanAttribute(String key, Object attribute) {
        switch (key) {
            case Attributes.Style.SRC:
                String srcStr = Attributes.getString(attribute);
                setImageSpanSrc(srcStr);
                return true;
            case Attributes.Style.ALT:
                String altStr = Attributes.getString(attribute, "");
                setImageSpanAlt(altStr);
                return true;
            case Attributes.Style.WIDTH:
                String widthStr = Attributes.getString(attribute);
                setImageSpanWidth(widthStr);
                return true;
            case Attributes.Style.HEIGHT:
                String heightStr = Attributes.getString(attribute);
                setImageSpanHeight(heightStr);
                return true;
            case STYLE_ALIGN:
                String alignStr = Attributes.getString(attribute);
                setImageSpanAlign(alignStr);
                return true;
        }
        return false;
    }

    private void setImageSpanSrc(String srcStr) {
        if (TextUtils.equals(mSrcStr, srcStr)) {
            return;
        }
        mSrcStr = srcStr;
        applySpannable();
    }

    private void setImageSpanAlt(String altStr) {
        if (TextUtils.equals(mAltStr, altStr)) {
            return;
        }
        mAltStr = altStr;
        applySpannable();
    }

    @Override
    public void setWidth(String widthStr) {
        super.setWidth(widthStr);
        //折叠屏自适应模式下banner图增加高斯模糊背景
        //父布局直接为swiper时需要通过设置padding来缩小图片尺寸
        if (isComponentAdaptiveEnable() && getParentSwiper() != null) {
            if (mAdaptiveBeforeWidth > 0 || getWidth() == ViewGroup.LayoutParams.MATCH_PARENT) {
                Swiper swiper = getParentSwiper();
                swiper.setPadding(Attributes.Style.PADDING_LEFT, DEFAULT_ADAPTIVE_BANNER_PADDING);
                swiper.setPadding(Attributes.Style.PADDING_RIGHT, DEFAULT_ADAPTIVE_BANNER_PADDING);
                swiper.setRealPadding();
                mSetImageBlur = true;
                setImageBlur();
            }
        }
    }

    private void setImageBlur() {
        if (isComponentAdaptiveEnable() && getParentSwiper() != null) {
            if (mHost.getSource() != null && mSetImageBlur) {
                getParentSwiper().setBackgroundImage(mHost.getSource(), true);
                getParentSwiper().applyBackground();
            }
        }
    }

    private Swiper getParentSwiper(){
        if (mParent instanceof Swiper) {
            return (Swiper) mParent;
        } else if (mParent != null) {
            Container parentParent = mParent.getParent();
            if (parentParent instanceof Swiper) {
                return (Swiper) parentParent;
            }
        }
        return null;
    }

    private void setImageSpanWidth(String widthStr) {
        int width = Attributes.getInt(mHapEngine, widthStr, 0);
        if (mImgWidth == width) {
            return;
        }
        mImgWidth = width;
        if (mImgWidth > 0 && mImgHeight > 0) {
            applySpannable();
        }
    }

    private void setImageSpanHeight(String heightStr) {
        int height = Attributes.getInt(mHapEngine, heightStr, 0);
        if (mImgHeight == height) {
            return;
        }
        mImgHeight = height;
        if (mImgWidth > 0 && mImgHeight > 0) {
            applySpannable();
        }
    }

    private void setImageSpanAlign(String alignStr) {
        int align = mVerticalAlignment;
        if (TextUtils.equals(alignStr, ALIGN_BOTTOM)) {
            align = DynamicDrawableSpan.ALIGN_BOTTOM;
        } else if (TextUtils.equals(alignStr, ALIGN_BASELINE)) {
            align = DynamicDrawableSpan.ALIGN_BASELINE;
        }
        if (align != mVerticalAlignment) {
            mVerticalAlignment = align;
            applySpannable();
        }
    }

    /**
     * @param imageView host
     * @param nightMode current mode
     */
    public void setNightMode(ImageView imageView, boolean nightMode) {
        SysOpProvider sysOpProvider = ProviderManager.getDefault().getProvider(SysOpProvider.NAME);
        if (sysOpProvider != null && sysOpProvider.handleImageForceDark(imageView, mEnableNightMode)) {
            return;
        }

        if (imageView == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return;
        }

        // clear color filter
        if (!mEnableNightMode || !nightMode || !mParent.getHostView().isForceDarkAllowed()) {
            imageView.clearColorFilter();
            return;
        }
        //close the default global color filter
        if (sysOpProvider != null && sysOpProvider.isCloseGlobalDefaultNightMode()) {
            imageView.clearColorFilter();
            return;
        }
        imageView.setColorFilter(Color.parseColor("#80000000"), PorterDuff.Mode.SRC_ATOP);
    }

    private void retrySrc() {
        if (isHeightDefined() && isWidthDefined() && mIsSrcInit) {
            mIsSrcInit = false;
            mHost.retrySource();
        }
    }

    @Override
    protected boolean addEvent(String event) {
        if (TextUtils.isEmpty(event) || mHost == null) {
            return true;
        }

        if (EVENT_COMPLETE.equals(event)) {
            mHasCompleteListener = true;
            return true;
        } else if (EVENT_ERROR.equals(event)) {
            mHasErrorListener = true;
            return true;
        }

        return super.addEvent(event);
    }

    @Override
    protected boolean removeEvent(String event) {
        if (TextUtils.isEmpty(event) || mHost == null) {
            return true;
        }

        switch (event) {
            case EVENT_COMPLETE:
                mHasCompleteListener = false;
                return true;
            case EVENT_ERROR:
                mHasErrorListener = false;
                return true;
            default:
                break;
        }

        return super.removeEvent(event);
    }

    @Override
    public void setBorderRadiusPercent(String position, float borderRadiusPercent) {
        if (FloatUtil.isUndefined(borderRadiusPercent) || borderRadiusPercent < 0
                || mHost == null) {
            return;
        }

        switch (position) {
            case Attributes.Style.BORDER_RADIUS:
                mHost.setBorderRadiusPercent(borderRadiusPercent);
                break;
            case Attributes.Style.BORDER_TOP_LEFT_RADIUS:
                mHost.setBorderRadiusPercent(Corner.TOP_LEFT, borderRadiusPercent);
                break;
            case Attributes.Style.BORDER_TOP_RIGHT_RADIUS:
                mHost.setBorderRadiusPercent(Corner.TOP_RIGHT, borderRadiusPercent);
                break;
            case Attributes.Style.BORDER_BOTTOM_LEFT_RADIUS:
                mHost.setBorderRadiusPercent(Corner.BOTTOM_LEFT, borderRadiusPercent);
                break;
            case Attributes.Style.BORDER_BOTTOM_RIGHT_RADIUS:
                mHost.setBorderRadiusPercent(Corner.BOTTOM_RIGHT, borderRadiusPercent);
                break;
            default:
                break;
        }

        super.setBorderRadiusPercent(position, borderRadiusPercent);
    }

    public void invokeMethod(String methodName, Map<String, Object> args) {
        if (mIsImageSpan) {
            return;
        }
        super.invokeMethod(methodName, args);
        if (METHOD_START_ANIMATION.equals(methodName)) {
            startAnimation();
        } else if (METHOD_STOP_ANIMAION.equals(methodName)) {
            stopAnimation();
        }
    }

    @Override
    public void setBorderRadius(String position, float borderRadius) {
        if (FloatUtil.isUndefined(borderRadius) || borderRadius < 0 || mHost == null) {
            return;
        }

        switch (position) {
            case Attributes.Style.BORDER_RADIUS:
                mHost.setBorderRadius(borderRadius);
                break;
            case Attributes.Style.BORDER_TOP_LEFT_RADIUS:
                mHost.setBorderRadius(Corner.TOP_LEFT, borderRadius);
                break;
            case Attributes.Style.BORDER_TOP_RIGHT_RADIUS:
                mHost.setBorderRadius(Corner.TOP_RIGHT, borderRadius);
                break;
            case Attributes.Style.BORDER_BOTTOM_LEFT_RADIUS:
                mHost.setBorderRadius(Corner.BOTTOM_LEFT, borderRadius);
                break;
            case Attributes.Style.BORDER_BOTTOM_RIGHT_RADIUS:
                mHost.setBorderRadius(Corner.BOTTOM_RIGHT, borderRadius);
                break;
            default:
                break;
        }

        super.setBorderRadius(position, borderRadius);
    }

    @Override
    public void applySpannable() {
        if (TextUtils.isEmpty(mAltStr) || TextUtils.isEmpty(mSrcStr)) {
            return;
        }
        mSpannable = new SpannableString(mAltStr);

        Uri srcUri = tryParseUri(mSrcStr);
        if (isImageSpanSrcSupported(srcUri)) {
            BitmapUtils.fetchBitmapForImageSpan(srcUri, new BitmapUtils.BitmapLoadCallback() {
                @Override
                public void onLoadSuccess(CloseableReference<CloseableImage> reference, Bitmap bitmap) {
                    Bitmap imageSpanBitmap = bitmap;
                    if (mImgWidth > 0 && mImgHeight > 0) {
                        // fresco不能将图片大小精确的调整到指定宽高，需要手动调整
                        imageSpanBitmap = BitmapUtils.resizeBitmap(bitmap, mImgWidth, mImgHeight);
                    }
                    ImageSpan imageSpan = new ImageSpan(mContext, imageSpanBitmap, mVerticalAlignment);
                    mSpannable.setSpan(imageSpan, 0, TextUtils.isEmpty(mAltStr) ? 0 : mAltStr.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    applyParentSpannable();
                }

                @Override
                public void onLoadFailure() {
                    Object[] spansToRemove = mSpannable.getSpans(0, mSpannable.length(), ImageSpan.class);
                    if (spansToRemove != null && spansToRemove.length > 0) {
                        for (Object span : spansToRemove) {
                            mSpannable.removeSpan(span);
                        }
                        applyParentSpannable();
                    }
                }
            }, mImgWidth, mImgHeight);
        }
    }

    private boolean isImageSpanSrcSupported(Uri uri) {
        if (uri == null) {
            return false;
        }
        boolean isSupported = false;
        String lastPathSegment = uri.getLastPathSegment();
        if (lastPathSegment != null) {
            lastPathSegment = lastPathSegment.toLowerCase();
            if (lastPathSegment.endsWith(".jpg") || lastPathSegment.endsWith(".jpeg")) {
                isSupported = true;
            } else if (lastPathSegment.endsWith(".png")) {
                isSupported = true;
            } else if (lastPathSegment.endsWith(".webp")) {
                isSupported = true;
            } else if (lastPathSegment.endsWith(".svg")) {
                isSupported = true;
            }
        }
        return isSupported;
    }

    @Override
    public Spannable getSpannable() {
        return mSpannable;
    }

    private Text getParentText() {
        Container parent = mParent;
        while (parent != null && !(parent instanceof Text)) {
            parent = parent.getParent();
        }
        return parent == null ? null : (Text) parent;
    }

    private void applyParentSpannable() {
        if (mParentText != null) {
            mParentText.setDirty(true);
            mParentText.updateSpannable();
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        if (mIsImageSpan) {
            return;
        }
        ConfigurationManager.getInstance().removeListener(mConfigurationListener);
    }

    public void setSrc(String src) {
        if (mHost == null) {
            return;
        }

        if (TextUtils.isEmpty(src)) {
            mHost.setSource(null);
            return;
        }
        Uri uri = tryParseUri(src);
        mHost.setSource(uri);

        if (uri == null && mHasErrorListener) {
            mCallback.onJsEventCallback(getPageId(), mRef, EVENT_ERROR, Image.this, null, null);
        }
    }

    // 源图片的缩放模式
    public void setObjectFit(String objectFit) {
        if (mHost == null) {
            return;
        }

        mHost.setObjectFit(objectFit);
    }

    // 占位图的缩放模式
    public void setAltObjectFit(String altObjectFit) {
        if (mHost == null) {
            return;
        }

        mHost.setAltObjectFit(altObjectFit);
    }

    // 设置CSS滤镜，示例：filter:blur(5px)，支持多个滤镜并排
    // 暂时只支持blur滤镜
    public void setFilter(Object filterObject) {
        if (mHost == null || filterObject == null) {
            return;
        }
        JSONObject jsonObj = Transform.toJsonObject(filterObject);
        if (jsonObj == null) {
            return;
        }
        // 处理blur逻辑
        String blurString = jsonObj.optString(BLUR);
        // blur的长度只支持px、dp，不支持百分比，为WEB CSS定义，将通过toolkit提醒开发者
        // blurRadius < 0 时，效果与blurRadius = 0 一样
        if (!TextUtils.isEmpty(blurString)) {
            int blurRadius = Attributes.getInt(mHapEngine, blurString, 0);
            mHost.setBlurRadius(Math.max(blurRadius, 0));
        }
    }

    public void setAlt(String alt) {
        if (mHost == null) {
            return;
        }
        if (TextUtils.isEmpty(alt) || BLANK.equals(alt)) {
            mHost.setPlaceholderDrawable(null);
            return;
        }

        Uri uri = mCallback.getCache(alt);
        if (uri != null) {
            mHost.setPlaceholderDrawable(uri);
        }
    }

    public void setAutoplay(boolean autoplay) {
        if (mHost == null) {
            return;
        }
        stopAnimation();
        mHost.setAutoplay(autoplay);
    }

    public void startAnimation() {
        if (mHost == null) {
            return;
        }
        mHost.startAnimation();
    }

    public void stopAnimation() {
        if (mHost == null) {
            return;
        }
        mHost.stopAnimation();
    }

    @Override
    public void start() {
        startAnimation();
    }

    @Override
    public void stop() {
        stopAnimation();
    }

    @Override
    public boolean isRunning() {
        if (mHost != null) {
            return mHost.isAnimationRunning();
        }
        return false;
    }

    private static class OnConfigurationListener
            implements ConfigurationManager.ConfigurationListener {
        private WeakReference<Image> imageWeakReference;

        public OnConfigurationListener(Image image) {
            imageWeakReference = new WeakReference<>(image);
        }

        @Override
        public void onConfigurationChanged(HapConfiguration newConfig) {
            Image image = imageWeakReference.get();
            if (null != image) {
                if (newConfig.getUiMode() != newConfig.getLastUiMode()) {
                    if (AppCompatDelegate.getDefaultNightMode() != AppCompatDelegate.MODE_NIGHT_NO
                            && AppCompatDelegate.getDefaultNightMode()
                            != AppCompatDelegate.MODE_NIGHT_YES) {
                        boolean darkMode = newConfig.getUiMode() == Configuration.UI_MODE_NIGHT_YES;
                        image.setNightMode(image.mHost, darkMode);
                    }
                }
            }
        }
    }
}

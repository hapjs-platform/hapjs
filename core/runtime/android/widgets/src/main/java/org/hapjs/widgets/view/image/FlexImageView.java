/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.view.image;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.common.executors.UiThreadImmediateExecutorService;
import com.facebook.common.util.UriUtil;
import com.facebook.datasource.BaseDataSubscriber;
import com.facebook.datasource.DataSource;
import com.facebook.datasource.DataSubscriber;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.controller.AbstractDraweeControllerBuilder;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.controller.ControllerListener;
import com.facebook.drawee.drawable.DrawableParent;
import com.facebook.drawee.drawable.FadeDrawable;
import com.facebook.drawee.drawable.ForwardingDrawable;
import com.facebook.drawee.drawable.ScaleTypeDrawable;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.drawee.generic.RootDrawable;
import com.facebook.drawee.generic.RoundingParams;
import com.facebook.drawee.view.AspectRatioMeasure;
import com.facebook.drawee.view.GenericDraweeView;
import com.facebook.fresco.animation.drawable.AnimatedDrawable2;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.common.RotationOptions;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.listener.BaseRequestListener;
import com.facebook.imagepipeline.listener.RequestListener;
import com.facebook.imagepipeline.postprocessors.IterativeBoxBlurPostProcessor;
import com.facebook.imagepipeline.request.BasePostprocessor;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.facebook.imagepipeline.request.Postprocessor;
import com.facebook.imageutils.BitmapUtil;
import com.facebook.yoga.YogaFlexDirection;
import com.facebook.yoga.YogaNode;

import org.hapjs.analyzer.model.NoticeMessage;
import org.hapjs.analyzer.tools.AnalyzerHelper;
import org.hapjs.common.executors.Executors;
import org.hapjs.common.utils.BitmapUtils;
import org.hapjs.common.utils.FloatUtil;
import org.hapjs.common.utils.SvgDecoderUtil;
import org.hapjs.common.utils.UriUtils;
import org.hapjs.component.Component;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.component.constants.Attributes;
import org.hapjs.component.constants.Corner;
import org.hapjs.component.constants.Spacing;
import org.hapjs.component.view.ComponentHost;
import org.hapjs.component.view.YogaLayout;
import org.hapjs.component.view.gesture.GestureHost;
import org.hapjs.component.view.gesture.IGesture;
import org.hapjs.component.view.helper.StateHelper;
import org.hapjs.component.view.keyevent.KeyEventDelegate;
import org.hapjs.render.AutoplayManager;
import org.hapjs.render.Page;
import org.hapjs.render.RootView;
import org.hapjs.runtime.ConfigurationManager;
import org.hapjs.runtime.DarkThemeUtil;
import org.hapjs.widgets.Image;
import org.hapjs.widgets.R;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class FlexImageView extends GenericDraweeView implements ComponentHost, GestureHost {

    private static final Matrix sMatrix = new Matrix();
    private static final Matrix sInverse = new Matrix();
    private static final int IMAGE_MAX_BITMAP_SIZE = 100 * 1024 * 1024; // 100 MB
    private static final String TAG = "FlexImageView";
    private static final String STRETCH = "stretch";
    private static final String CENTER = "center";
    private final FlexImageViewAttach mViewAttach;
    private final AbstractDraweeControllerBuilder mDraweeControllerBuilder;
    private final RoundedCornerPostprocessor mRoundedCornerPostprocessor;
    private Component mComponent;
    private KeyEventDelegate mKeyEventDelegate;
    private float[] mComputedCornerRadii = new float[8];
    private ImageResizeMethod mResizeMethod = ImageResizeMethod.RESIZE;
    private int mBorderColor;
    private int mOverlayColor;
    private int mImageHeight;
    private int mImageWidth;
    private float mBorderWidth;
    private float mBorderRadius = FloatUtil.UNDEFINED;
    private float mBorderRadiusPercent = FloatUtil.UNDEFINED;
    private float[] mBorderCornerRadii;
    private float[] mBorderCornerRadiiPercent;
    private ScalingUtils.ScaleType mScaleType = ScalingUtils.ScaleType.CENTER_CROP;
    private ScalingUtils.ScaleType mAltScaleType = ScalingUtils.ScaleType.CENTER_CROP;
    private volatile Uri mSource;
    private Drawable mPlaceholderDrawable;
    private Uri mPlaceholderUri;
    private boolean mDefaultPlaceholder = true;
    private int mFadeDurationMs = -1;
    private boolean mProgressiveRenderingEnabled;
    private OnLoadStatusListener mOnLoadStatusListener;
    private @Nullable IterativeBoxBlurPostProcessor mIterativeBoxBlurPostProcessor;
    private boolean mIsDirty;
    private boolean mSourceChanged;
    private boolean mIsRetrySrc = false;
    private boolean mIsSetBorderRadius = false;
    private int mBlurRadius = 0;
    private boolean mHasRadius = false;
    private AspectRatioMeasure.Spec mMeasureSpec = new AspectRatioMeasure.Spec();

    private String mObjectFit;
    private boolean mAltObjectFitHasApplied = false;
    private boolean mAutoplay = true;
    private Animatable mAnimatable = null;
    private boolean mIsStartAnimation = false;
    private AutoplayManager mAutoplayManager;
    private IGesture mGesture;

    private static final String DECODE_PRODUCER_NAME = "DecodeProducer";
    private static final String ENCODE_SIZE_KEY = "encodedImageSize";
    private static final String BITMAP_SIZE_KEY = "bitmapSize";

    public FlexImageView(Context context) {
        super(context, buildHierarchy(context));
        mRoundedCornerPostprocessor = new RoundedCornerPostprocessor();
        mDraweeControllerBuilder = Fresco.newDraweeControllerBuilder();
        mViewAttach = new FlexImageViewAttach(this);
        mViewAttach.setOnPreDrawListener(
                new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        if (mIsSetBorderRadius) {
                            mIsDirty = true;
                            maybeUpdateView(
                                    mScaleType != ScalingUtils.ScaleType.CENTER_CROP
                                            && mScaleType != ScalingUtils.ScaleType.FOCUS_CROP);

                            mIsSetBorderRadius = false;
                        }
                        return true;
                    }
                });
    }

    private static GenericDraweeHierarchy buildHierarchy(Context context) {
        return new GenericDraweeHierarchyBuilder(context.getResources()).build();
    }

    @Override
    public IGesture getGesture() {
        return mGesture;
    }

    @Override
    public void setGesture(IGesture gestureDelegate) {
        mGesture = gestureDelegate;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean result = super.onTouchEvent(event);
        if (mGesture != null) {
            result |= mGesture.onTouch(event);
        }
        return result;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean result = super.onKeyDown(keyCode, event);
        return onKey(KeyEvent.ACTION_DOWN, keyCode, event, result);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        boolean result = super.onKeyUp(keyCode, event);
        return onKey(KeyEvent.ACTION_UP, keyCode, event, result);
    }

    private boolean onKey(int keyAction, int keyCode, KeyEvent event, boolean result) {
        if (mKeyEventDelegate == null) {
            mKeyEventDelegate = new KeyEventDelegate(mComponent);
        }
        result |= mKeyEventDelegate.onKey(keyAction, keyCode, event);
        return result;
    }

    public void setOnLoadStatusListener(OnLoadStatusListener listener) {
        mOnLoadStatusListener = listener;
        mViewAttach.setOnLoadStatusListener(listener);
    }

    public void setBorderRadius(float borderRadius) {
        if (!FloatUtil.floatsEqual(mBorderRadius, borderRadius)) {
            mBorderRadius = borderRadius;
            mBorderRadiusPercent = FloatUtil.UNDEFINED;
            mIsSetBorderRadius = true;
        }
    }

    public void setBorderRadius(int position, float borderRadius) {
        if (mBorderCornerRadii == null) {
            mBorderCornerRadii = new float[4];
            Arrays.fill(mBorderCornerRadii, FloatUtil.UNDEFINED);
        }

        if (!FloatUtil.floatsEqual(mBorderCornerRadii[position], borderRadius)) {
            mBorderCornerRadii[position] = borderRadius;
            if (mBorderCornerRadiiPercent != null) {
                mBorderCornerRadiiPercent[position] = FloatUtil.UNDEFINED;
            }
            mIsSetBorderRadius = true;
        }
    }

    public void setBorderRadiusPercent(float borderRadiusPercent) {
        if (!FloatUtil.floatsEqual(mBorderRadiusPercent, borderRadiusPercent)) {
            mBorderRadiusPercent = borderRadiusPercent;
            mBorderRadius = FloatUtil.UNDEFINED;
            mIsSetBorderRadius = true;
        }
    }

    public void setBorderRadiusPercent(int position, float borderRadiusPercent) {
        if (mBorderCornerRadiiPercent == null) {
            mBorderCornerRadiiPercent = new float[4];
            Arrays.fill(mBorderCornerRadiiPercent, FloatUtil.UNDEFINED);
        }

        if (!FloatUtil.floatsEqual(mBorderCornerRadiiPercent[position], borderRadiusPercent)) {
            mBorderCornerRadiiPercent[position] = borderRadiusPercent;
            if (mBorderCornerRadii != null) {
                mBorderCornerRadii[position] = FloatUtil.UNDEFINED;
            }
            mIsSetBorderRadius = true;
        }
    }

    public void setBlurRadius(int blurRadius) {
        if (mBlurRadius == blurRadius) {
            return;
        }
        if (blurRadius == 0) {
            mIterativeBoxBlurPostProcessor = null;
        } else {
            mIterativeBoxBlurPostProcessor = new IterativeBoxBlurPostProcessor(2, blurRadius);
        }
        mIsDirty = true;
        mBlurRadius = blurRadius;
        maybeUpdateView(true);
    }

    public void setObjectFit(String objectFit) {
        if (!TextUtils.equals(mObjectFit, objectFit)) {
            mObjectFit = objectFit;
            mScaleType = parseObjectFit(objectFit);
            mIsDirty = true;
            maybeUpdateView(mScaleType == ScalingUtils.ScaleType.CENTER);
        }
    }

    // 占位图的缩放模式的设置只会响应一次，不响应前端变更而刷新
    public void setAltObjectFit(String altObjectFit) {
        if (!mAltObjectFitHasApplied) {
            mAltScaleType = parseObjectFit(altObjectFit);
            mAltObjectFitHasApplied = true;
        }
    }

    public void setSource(Uri uri) {
        if (uri == null) {
            setController(null);
            mSource = null;
            mViewAttach.releaseSource();
            return;
        }

        if (mSource != null) {
            if (mSource.equals(uri)) {
                return;
            }
            mViewAttach.releaseSource();
        }

        mSource = uri;
        mIsDirty = true;
        mSourceChanged = true;

        maybeUpdateView(true);
    }

    public String getSource() {
        if (mSource != null) {
            return mSource.toString();
        }
        return null;
    }

    /**
     * update placeholder image
     */
    private void maybeUpdatePlaceholderImage() {
        if (mComponent == null || mSource == null) {
            return;
        }

        GenericDraweeHierarchy hierarchy = getHierarchy();
        if (hierarchy != null) {
            boolean widthDefined = mComponent.isWidthDefined();
            boolean heightDefined = mComponent.isHeightDefined();
            if (!widthDefined || !heightDefined) { // width or height no defined
                removePlaceholderImage(hierarchy);
                return;
            }

            ImagePipeline imagePipeline = Fresco.getImagePipeline();
            // Is in memory cache?
            if (imagePipeline != null && imagePipeline.isInBitmapMemoryCache(mSource)) {
                removePlaceholderImage(hierarchy);
            } else {
                boolean isCheckDiskCache = false;
                if (mDefaultPlaceholder) { // Use default placeholder image
                    String scheme = mSource.getScheme();
                    if (scheme != null
                            && (scheme.startsWith("http")
                            || scheme.startsWith(
                            "https"))) { // only remote image set placeholder image
                        android.content.res.Resources resource = getResources();
                        if (resource != null) {
                            Drawable drawable = null;
                            try {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                                        && DarkThemeUtil.isDarkMode()) {
                                    drawable = resource.getDrawable(
                                            R.color.image_placeholder_color_dark, null);
                                } else {
                                    drawable =
                                            resource.getDrawable(R.color.image_placeholder_color);
                                }
                            } catch (Exception e) {
                                Log.w(TAG,
                                        "resource \"R.color.image_placeholder_color\" get drawable error : ",
                                        e);
                            }
                            if (drawable != null) {
                                setPlaceholderImage(hierarchy, drawable);
                                isCheckDiskCache = true;
                            }
                        }
                    }
                } else {
                    if (mPlaceholderDrawable != null) {
                        setPlaceholderImage(hierarchy, mPlaceholderDrawable);
                        isCheckDiskCache = true;
                    }
                }

                // Is in disk cache?
                if (isCheckDiskCache && imagePipeline != null) {
                    Executors.io()
                            .execute(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            ImageRequest imageRequest =
                                                    ImageRequest.fromUri(mSource);
                                            if (null == imageRequest) {
                                                Log.e(TAG, "imageRequest is null");
                                                return;
                                            }
                                            DataSource<Boolean> inDiskCacheSource =
                                                    imagePipeline.isInDiskCache(imageRequest);
                                            if (inDiskCacheSource != null) {
                                                DataSubscriber<Boolean> subscriber =
                                                        new BaseDataSubscriber<Boolean>() {
                                                            @Override
                                                            protected void onNewResultImpl(
                                                                    DataSource<Boolean> dataSource) {
                                                                try {
                                                                    if (dataSource == null
                                                                            ||
                                                                            !dataSource.isFinished()
                                                                            ||
                                                                            dataSource.isClosed()) {
                                                                        return;
                                                                    }
                                                                    boolean isInCache =
                                                                            dataSource.getResult();
                                                                    if (isInCache) {
                                                                        if (hierarchy != null) {
                                                                            removePlaceholderImage(
                                                                                    hierarchy);
                                                                        }
                                                                    }
                                                                } catch (Exception e) {
                                                                    Log.e(TAG,
                                                                            "maybe update place holder image",
                                                                            e);
                                                                } finally {
                                                                    if (dataSource != null
                                                                            && !dataSource
                                                                                    .isClosed()) {
                                                                        dataSource.close();
                                                                    }
                                                                }
                                                            }

                                                            @Override
                                                            protected void onFailureImpl(
                                                                    DataSource<Boolean> dataSource) {
                                                            }
                                                        };
                                                inDiskCacheSource.subscribe(
                                                        subscriber, UiThreadImmediateExecutorService
                                                                .getInstance());
                                            }
                                        }
                                    });
                }
            }
        }
    }

    public void setPlaceholderDrawable(Uri uri) {
        mDefaultPlaceholder = false;
        if (uri == null) {
            mPlaceholderUri = null;
            removePlaceholderImage(getHierarchy());
            return;
        }
        if (!UriUtils.equals(uri, mPlaceholderUri) || mPlaceholderDrawable == null) {
            mPlaceholderUri = uri;
            final Uri bitmapUri = uri;
            removePlaceholderImage(getHierarchy());

            BitmapUtils.fetchLocalDrawable(getContext(), bitmapUri,
                    new DecodedListener(this, bitmapUri));
        }
    }

    public void setFadeDuration(int durationMs) {
        mFadeDurationMs = durationMs;
    }

    private void cornerRadii(float[] computedCorners) {
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();

        computedCorners[Corner.TOP_LEFT_X] = getBorderCornerRadius(Corner.TOP_LEFT, width);
        computedCorners[Corner.TOP_LEFT_Y] = getBorderCornerRadius(Corner.TOP_LEFT, height);
        computedCorners[Corner.TOP_RIGHT_X] = getBorderCornerRadius(Corner.TOP_RIGHT, width);
        computedCorners[Corner.TOP_RIGHT_Y] = getBorderCornerRadius(Corner.TOP_RIGHT, height);
        computedCorners[Corner.BOTTOM_RIGHT_X] = getBorderCornerRadius(Corner.BOTTOM_RIGHT, width);
        computedCorners[Corner.BOTTOM_RIGHT_Y] = getBorderCornerRadius(Corner.BOTTOM_RIGHT, height);
        computedCorners[Corner.BOTTOM_LEFT_X] = getBorderCornerRadius(Corner.BOTTOM_LEFT, width);
        computedCorners[Corner.BOTTOM_LEFT_Y] = getBorderCornerRadius(Corner.BOTTOM_LEFT, height);
    }

    private float getBorderCornerRadius(int corner, int baseLength) {
        float borderCornerRadius;

        float biasRadius = getComponentBorderWidth() / 2f;

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
        return borderCornerRadius - biasRadius;
    }

    private float getComponentBorderWidth() {
        if (mComponent == null) {
            return 0f;
        }

        float borderWidth = mComponent.getBorderWidth(Attributes.Style.BORDER_WIDTH);
        if (!FloatUtil.isUndefined(borderWidth) && !FloatUtil.floatsEqual(borderWidth, 0f)) {
            return borderWidth;
        }

        float borderLeftWidth = mComponent.getBorderWidth(Attributes.Style.BORDER_LEFT_WIDTH);
        float borderTopWidth = mComponent.getBorderWidth(Attributes.Style.BORDER_TOP_WIDTH);
        float borderRightWidth = mComponent.getBorderWidth(Attributes.Style.BORDER_RIGHT_WIDTH);
        float borderBottomWidth = mComponent.getBorderWidth(Attributes.Style.BORDER_BOTTOM_WIDTH);
        if (!FloatUtil.isUndefined(borderLeftWidth)
                && !FloatUtil.floatsEqual(borderLeftWidth, 0f)
                && FloatUtil.floatsEqual(borderLeftWidth, borderTopWidth)
                && FloatUtil.floatsEqual(borderTopWidth, borderRightWidth)
                && FloatUtil.floatsEqual(borderRightWidth, borderBottomWidth)) {
            return borderLeftWidth;
        }

        return 0f;
    }

    public void retrySource() {
        if (mSource == null || TextUtils.isEmpty(mSource.toString())) {
            return;
        }

        mIsDirty = true;
        mIsRetrySrc = true;
        maybeUpdateView(true);
        mIsRetrySrc = false;
    }

    public void maybeUpdateView(boolean rebuild) {
        if (mSource == null) {
            return;
        }

        if (!mIsDirty) {
            return;
        }

        boolean doResize = shouldResize(mSource);
        int tmpWidth = -1;
        int tmpHeight = -1;
        if (mIsRetrySrc && null != mComponent) {
            String withStr = mComponent.getCurStateStyleString(Attributes.Style.WIDTH, "");
            String heightStr = mComponent.getCurStateStyleString(Attributes.Style.HEIGHT, "");
            int domWidth = -1;
            int domHeight = -1;
            if (Attributes.isSpecificAttributes(withStr)) {
                domWidth = mComponent.getCurStateStyleInt(Attributes.Style.WIDTH, -1);
            }
            if (Attributes.isSpecificAttributes(heightStr)) {
                domHeight = mComponent.getCurStateStyleInt(Attributes.Style.HEIGHT, -1);
            }
            if (domWidth > 0) {
                tmpWidth = domWidth;
            }
            if (domHeight > 0) {
                tmpHeight = domHeight;
            }
        }

        if ((doResize && (getWidth() <= 0 && getHeight() <= 0) && !mIsRetrySrc)
                || (doResize && mIsRetrySrc && (tmpWidth <= 0 || tmpHeight <= 0))) {
            // If need a resize and the size is not yet set, wait until the layout pass provides one
            return;
        }

        mImageWidth = 0;
        mImageHeight = 0;

        GenericDraweeHierarchy hierarchy = getHierarchy();
        hierarchy.setActualImageScaleType(mScaleType);

        setupRoundingParams(hierarchy);
        hierarchy.setFadeDuration(mFadeDurationMs >= 0 ? mFadeDurationMs : 0);
        if (!rebuild) {
            return;
        }

        // update placeholder image
        maybeUpdatePlaceholderImage();

        boolean usePostprocessorScaling =
                mScaleType != ScalingUtils.ScaleType.CENTER_CROP
                        && mScaleType != ScalingUtils.ScaleType.FOCUS_CROP;

        List<Postprocessor> postprocessors = new LinkedList<>();

        if (usePostprocessorScaling) {
            postprocessors.add(mRoundedCornerPostprocessor);
        }
        if (mIterativeBoxBlurPostProcessor != null) {
            postprocessors.add(mIterativeBoxBlurPostProcessor);
        }

        Postprocessor postprocessor = MultiPostprocessor.from(postprocessors);
        int width = getLayoutParams().width;
        int height = getLayoutParams().height;

        if (mIsRetrySrc && width <= 0 && tmpWidth > 0) {
            width = tmpWidth;
        }
        if (mIsRetrySrc && height <= 0 && tmpHeight > 0) {
            height = tmpHeight;
        }

        doResize = doResize && (width > 0 && height > 0)
                && mScaleType != ScalingUtils.ScaleType.CENTER;
        ResizeOptions resizeOptions = doResize ? new ResizeOptions(width, height) : null;
        RequestListener requestListener = new ImageSizeDetectRequestListener();
        ImageRequest imageRequest =
                ImageRequestBuilder.newBuilderWithSource(mSource)
                        .setPostprocessor(postprocessor)
                        .setResizeOptions(resizeOptions)
                        .setRotationOptions(RotationOptions.autoRotate())
                        .setProgressiveRenderingEnabled(mProgressiveRenderingEnabled)
                        .setRequestListener(requestListener)
                        .build();

        final boolean supportLargeImage = shouldSupportLargeImage();
        if (supportLargeImage) {
            mViewAttach.handleAttachedImage(imageRequest);
        }
        ControllerListener controllerListener =
                new BaseControllerListener<ImageInfo>() {
                    @Override
                    public void onFailure(String id, Throwable throwable) {
                        mSource = null;

                        if (mOnLoadStatusListener != null) {
                            mOnLoadStatusListener.onError(throwable);
                        }
                    }

                    @Override
                    public void onFinalImageSet(
                            String id, @Nullable final ImageInfo imageInfo,
                            @Nullable Animatable animatable) {
                        if (mComponent == null) {
                            return;
                        }

                        if (null != imageInfo
                                && imageInfo instanceof SvgDecoderUtil.CloseableSvgImage) {
                            /** 解决android 4.4 svg图片不显示和5.0以上版本svg图片显示模糊的问题 */
                            setLayerType(ImageView.LAYER_TYPE_SOFTWARE, null);
                        } else {
                            setLayerType(ImageView.LAYER_TYPE_NONE, null);
                        }
                        post(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        updateImageInfo(imageInfo);
                                    }
                                });
                        if (mImageWidth == 0 && mImageHeight == 0 && !supportLargeImage) {
                            if (imageInfo != null) {
                                mImageWidth = imageInfo.getWidth();
                                mImageHeight = imageInfo.getHeight();
                            } else {
                                mImageWidth = 0;
                                mImageHeight = 0;
                            }
                            if (mOnLoadStatusListener != null && mSourceChanged) {
                                mOnLoadStatusListener.onComplete(mImageWidth, mImageHeight);
                                mSourceChanged = false;
                            }
                        }
                        GenericDraweeHierarchy hierarchy = getHierarchy();
                        if (hierarchy != null) {
                            removePlaceholderImage(hierarchy);
                        }

                        if (mAnimatable != null && animatable == null) {
                            getAutoplayManager().removeAutoplay(mComponent.getRef());
                        }

                        mAnimatable = animatable;
                        if (mAnimatable != null) {
                            if (mAutoplay) {
                                getAutoplayManager()
                                        .addAutoplay(mComponent.getRef(), (Image) mComponent);
                            } else {
                                getAutoplayManager().removeAutoplay(mComponent.getRef());
                            }

                            if (mIsStartAnimation) {
                                mAnimatable.start();
                            } else {
                                mAnimatable.stop();
                            }
                        }
                    }
                };

        // This builder is reused
        mDraweeControllerBuilder.reset();

        mDraweeControllerBuilder
                .setAutoPlayAnimations(mAutoplay)
                .setControllerListener(controllerListener)
                .setOldController(getController())
                .setImageRequest(imageRequest);

        setController(mDraweeControllerBuilder.build());
        mIsDirty = false;
    }

    private DrawableParent findDrawableParentForLeaf(DrawableParent parent) {
        while (true) {
            Drawable child = parent.getDrawable();
            if (child == parent || !(child instanceof DrawableParent)) {
                break;
            }
            parent = (DrawableParent) child;
        }
        return parent;
    }

    private FadeDrawable getFadeDrawable() {
        GenericDraweeHierarchy hierarchy = getHierarchy();
        if (null == hierarchy) {
            return null;
        }
        Drawable drawable = hierarchy.getTopLevelDrawable();
        RootDrawable tmpDrawable = null;
        Drawable innerDrawable = null;
        FadeDrawable fadeDrawable = null;
        if (drawable instanceof RootDrawable) {
            tmpDrawable = (RootDrawable) drawable;
        }
        if (null != tmpDrawable) {
            innerDrawable = tmpDrawable.getDrawable();
        }
        if (innerDrawable instanceof FadeDrawable) {
            fadeDrawable = ((FadeDrawable) innerDrawable);
        }
        return fadeDrawable;
    }

    private void setupRoundingParams(GenericDraweeHierarchy hierarchy) {
        boolean hasBorderRadius = false;
        cornerRadii(mComputedCornerRadii);
        for (float radius : mComputedCornerRadii) {
            if (radius != 0) {
                hasBorderRadius = true;
                break;
            }
        }
        mHasRadius = hasBorderRadius;
        if (!hasBorderRadius) {
            try {
                hierarchy.setRoundingParams(null);
            } catch (Exception e) {
                Log.w(TAG, "hierarchy.setRoundingParams error", e);
            }
            return;
        }

        RoundingParams roundingParams = hierarchy.getRoundingParams();
        if (roundingParams == null) {
            roundingParams = RoundingParams.fromCornersRadius(0);
        }
        roundingParams.setPaintFilterBitmap(true);
        boolean usePostprocessorScaling =
                mScaleType != ScalingUtils.ScaleType.CENTER_CROP
                        && mScaleType != ScalingUtils.ScaleType.FOCUS_CROP;
        if (usePostprocessorScaling) {
            roundingParams.setCornersRadius(0);
        } else {
            roundingParams.setCornersRadii(mComputedCornerRadii);
        }

        roundingParams.setBorder(mBorderColor, mBorderWidth);
        if (mOverlayColor != Color.TRANSPARENT) {
            roundingParams.setOverlayColor(mOverlayColor);
        } else {
            // make sure the default rounding method is used.
            roundingParams.setRoundingMethod(RoundingParams.RoundingMethod.BITMAP_ONLY);
        }
        try {
            hierarchy.setRoundingParams(roundingParams);
        } catch (Exception e) {
            Log.w(TAG, "hierarchy.setRoundingParams error", e);
        }
    }

    private void updateImageInfo(ImageInfo imageInfo) {
        if (imageInfo == null || mComponent == null) {
            return;
        }

        ViewGroup.LayoutParams lp = getLayoutParams();
        boolean widthDefined = mComponent.isWidthDefined();
        boolean heightDefined = mComponent.isHeightDefined();
        float aspectRatio = (float) imageInfo.getWidth() / imageInfo.getHeight();
        float layoutWidth;
        float layoutHeight;

        if (mComponent.isParentYogaLayout()) {
            YogaNode yogaNode = ((YogaLayout) getParent()).getYogaNodeForView(this);
            YogaFlexDirection parentDirection =
                    yogaNode.getParent() == null ? null : yogaNode.getParent().getFlexDirection();
            layoutWidth = yogaNode.getLayoutWidth();
            layoutHeight = yogaNode.getLayoutHeight();
            float leftBorderWidth = mComponent.getBorder(Spacing.LEFT);
            float topBorderWidth = mComponent.getBorder(Spacing.TOP);
            float rightBorderWidth = mComponent.getBorder(Spacing.RIGHT);
            float bottomBorderWidth = mComponent.getBorder(Spacing.BOTTOM);
            float layoutWidthWithoutBorder =
                    layoutWidth - Math.round(leftBorderWidth) - Math.round(rightBorderWidth);
            float layoutHeightWithoutBorder =
                    layoutHeight - Math.round(topBorderWidth) - Math.round(bottomBorderWidth);

            if (!widthDefined && !heightDefined) {

                // default dimension [1, 1] or [2, 2]
                if (parentDirection == YogaFlexDirection.ROW) {
                    // consider main-axis first
                    if (layoutWidthWithoutBorder > 2) {
                        // main-axis grow
                        yogaNode.setWidth(layoutWidth);
                        layoutHeight =
                                layoutWidthWithoutBorder / aspectRatio + topBorderWidth
                                        + bottomBorderWidth;
                        yogaNode.setHeight(layoutHeight);
                    } else if (layoutHeightWithoutBorder > 2) {
                        // cross-axis stretched
                        yogaNode.setHeight(layoutHeight);
                        layoutWidth =
                                layoutHeightWithoutBorder * aspectRatio + leftBorderWidth
                                        + rightBorderWidth;
                        yogaNode.setWidth(layoutWidth);
                    } else {
                        // default to image dimension
                        layoutWidth = imageInfo.getWidth() + leftBorderWidth + rightBorderWidth;
                        layoutHeight = imageInfo.getHeight() + topBorderWidth + bottomBorderWidth;
                        yogaNode.setWidth(layoutWidth);
                        yogaNode.setHeight(layoutHeight);
                    }
                } else {
                    if (layoutHeightWithoutBorder > 2) {
                        // main-axis grow
                        yogaNode.setHeight(layoutHeight);
                        layoutWidth =
                                layoutHeightWithoutBorder * aspectRatio + leftBorderWidth
                                        + rightBorderWidth;
                        yogaNode.setWidth(layoutWidth);
                    } else if (layoutWidthWithoutBorder > 2) {
                        // cross-axis stretched
                        yogaNode.setWidth(layoutWidth);
                        layoutHeight =
                                layoutWidthWithoutBorder / aspectRatio + topBorderWidth
                                        + bottomBorderWidth;
                        yogaNode.setHeight(layoutHeight);
                    } else {
                        // default to image dimension
                        layoutWidth = imageInfo.getWidth() + leftBorderWidth + rightBorderWidth;
                        layoutHeight = imageInfo.getHeight() + topBorderWidth + bottomBorderWidth;
                        yogaNode.setWidth(layoutWidth);
                        yogaNode.setHeight(layoutHeight);
                    }
                }
            } else if (!widthDefined && heightDefined) {
                layoutWidth = layoutHeightWithoutBorder * aspectRatio + leftBorderWidth
                        + rightBorderWidth;
                yogaNode.setWidth(layoutWidth);
            } else if (widthDefined && !heightDefined) {
                layoutHeight =
                        layoutWidthWithoutBorder / aspectRatio + topBorderWidth + bottomBorderWidth;
                yogaNode.setHeight(layoutHeight);
            }
        } else {
            layoutWidth = lp.width;
            layoutHeight = lp.height;
            if (!widthDefined && !heightDefined) {
                layoutWidth = Math.max(getMeasuredWidth(), imageInfo.getWidth());
                lp.width = (int) layoutWidth;
            } else if (!widthDefined && heightDefined) {
                layoutHeight =
                        (getMeasuredHeight() > imageInfo.getHeight() || lp.height < 0)
                                ? getMeasuredHeight()
                                : lp.height;
                layoutWidth = Math.round(layoutHeight * aspectRatio);
                lp.width = (int) layoutWidth;
            } else if (widthDefined && !heightDefined) {
                layoutWidth =
                        (getMeasuredWidth() > imageInfo.getWidth() || lp.width < 0)
                                ? getMeasuredWidth()
                                : lp.width;
                layoutHeight = Math.round(layoutWidth / aspectRatio);
                lp.height = (int) layoutHeight;
            }
            setAspectRatio(aspectRatio);
        }
        if ("scale-down".equals(mObjectFit)
                && (imageInfo.getWidth() > layoutWidth || imageInfo.getHeight() > layoutHeight)) {
            mScaleType = ScalingUtils.ScaleType.FIT_CENTER;
            getHierarchy().setActualImageScaleType(mScaleType);
        }
        if (!widthDefined || !heightDefined) {
            requestLayout();
        }
    }

    @Override
    protected void onAttach() {
        super.onAttach();
        mViewAttach.onAttach();
        if (mAutoplay && mAnimatable != null && mComponent != null) {
            getAutoplayManager().addAutoplay(mComponent.getRef(), (Image) mComponent);
        }
        if (mComponent instanceof Image) {
            Image image = ((Image) mComponent);
            image.setNightMode(this, DarkThemeUtil.isDarkMode(getContext()));
            if (!ConfigurationManager.getInstance().contains(image.getConfigurationListener())) {
                ConfigurationManager.getInstance().addListener(image.getConfigurationListener());
            }
        }
    }

    @Override
    protected void onDetach() {
        super.onDetach();
        mViewAttach.onDetach();
        if (mAutoplay && mAnimatable != null && mComponent != null) {
            getAutoplayManager().removeAutoplay(mComponent.getRef());
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        try {
            super.onDraw(canvas);
            if (mViewAttach != null) {
                mViewAttach.onDraw(canvas);
            }
        } catch (RuntimeException e) {
            boolean isTooLarge = isTooLargeBitmap();
            if (isTooLarge) {
                if (null != mComponent) {
                    RenderEventCallback callback = mComponent.getCallback();
                    if (null != callback) {
                        callback.onJsException(e);
                    }
                }
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    private boolean isTooLargeBitmap() {
        boolean isTooLarge = false;
        Drawable usingDrawable = null;
        Bitmap bitmap = null;
        FadeDrawable fadeDrawable = getFadeDrawable();
        if (null == fadeDrawable) {
            return isTooLarge;
        }
        // ACTUAL_IMAGE_INDEX 2  GenericDraweeHierarchy define
        if (fadeDrawable.getNumberOfLayers() >= 3) {
            usingDrawable = fadeDrawable.getDrawable(2);
        }
        if (usingDrawable instanceof ScaleTypeDrawable) {
            usingDrawable = usingDrawable.getCurrent();
        }
        if (usingDrawable instanceof ForwardingDrawable) {
            usingDrawable = usingDrawable.getCurrent();
        }
        if (usingDrawable instanceof BitmapDrawable) {
            bitmap = ((BitmapDrawable) usingDrawable).getBitmap();
        }
        if (null != bitmap && !bitmap.isRecycled()) {
            int bitmapSize = bitmap.getByteCount();
            if (bitmapSize > IMAGE_MAX_BITMAP_SIZE) {
                Log.e(TAG, "Canvas: trying to draw too large(" + bitmapSize + "bytes) bitmap.");
                isTooLarge = true;
            }
        }
        return isTooLarge;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mMeasureSpec.width = widthMeasureSpec;
        mMeasureSpec.height = heightMeasureSpec;
        updateMeasureSpec(
                mMeasureSpec,
                getAspectRatio(),
                getLayoutParams(),
                getPaddingLeft() + getPaddingRight(),
                getPaddingTop() + getPaddingBottom());
        super.onMeasure(mMeasureSpec.width, mMeasureSpec.height);
    }

    private void updateMeasureSpec(
            AspectRatioMeasure.Spec spec,
            float aspectRatio,
            @Nullable ViewGroup.LayoutParams layoutParams,
            int widthPadding,
            int heightPadding) {
        if (aspectRatio <= 0 || layoutParams == null) {
            return;
        }

        if (shouldAdjust(layoutParams.height)) {
            int widthSpecSize = View.MeasureSpec.getSize(spec.width);
            int desiredHeight =
                    Math.round((widthSpecSize - widthPadding) / aspectRatio + heightPadding);
            int resolvedHeight = View.resolveSize(desiredHeight, spec.height);
            spec.height =
                    View.MeasureSpec.makeMeasureSpec(resolvedHeight, View.MeasureSpec.EXACTLY);
        } else if (shouldAdjust(layoutParams.width)) {
            int heightSpecSize = View.MeasureSpec.getSize(spec.height);
            int desiredWidth =
                    Math.round((heightSpecSize - heightPadding) * aspectRatio + widthPadding);
            int resolvedWidth = View.resolveSize(desiredWidth, spec.width);
            spec.width = View.MeasureSpec.makeMeasureSpec(resolvedWidth, View.MeasureSpec.EXACTLY);
        }
    }

    private boolean shouldAdjust(int layoutDimension) {
        return layoutDimension == ViewGroup.LayoutParams.MATCH_PARENT;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 || h > 0) {
            maybeUpdateView(true);
        }
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    private boolean isResource(Uri source) {
        return UriUtil.isLocalResourceUri(source);
    }

    private boolean shouldResize(Uri source) {
        // Resizing is inferior to scaling. See http://frescolib.org/docs/resizing-rotating.html#_
        // We resize here only for images likely to be from the device's camera, where the app developer
        // has no control over the original size
        if (mResizeMethod == ImageResizeMethod.AUTO) {
            return UriUtil.isLocalContentUri(source) || UriUtil.isLocalFileUri(source);
        } else if (mResizeMethod == ImageResizeMethod.RESIZE) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        StateHelper.onStateChanged(this, mComponent);
    }

    @Override
    public Component getComponent() {
        return mComponent;
    }

    @Override
    public void setComponent(Component component) {
        mComponent = component;
    }

    private boolean shouldSupportLargeImage() {
        if (mComponent == null) {
            return false;
        }
        boolean heightDefined = false;
        boolean widthDefined = false;
        String height = (String) mComponent.getCurStateStyle(Attributes.Style.HEIGHT, null);
        if (!TextUtils.isEmpty(height)) {
            heightDefined = true;
        }
        String width = (String) mComponent.getCurStateStyle(Attributes.Style.WIDTH, null);
        if (!TextUtils.isEmpty(width)) {
            widthDefined = true;
        }

        // width/height no defined or defined and large.
        boolean supportHeight = !heightDefined || getMeasuredHeight() > BitmapUtil.MAX_BITMAP_SIZE;
        boolean supportWidth = !widthDefined || getMeasuredWidth() > BitmapUtil.MAX_BITMAP_SIZE;
        return (supportHeight || supportWidth) && (mScaleType != ScalingUtils.ScaleType.FIT_XY);
    }

    private ScalingUtils.ScaleType parseObjectFit(String objectFit) {
        ScalingUtils.ScaleType scaleType = ScalingUtils.ScaleType.CENTER_CROP;

        if (TextUtils.isEmpty(objectFit)) {
            return scaleType;
        }

        if (Attributes.ObjectFit.CONTAIN.equals(objectFit)) {
            return ScalingUtils.ScaleType.FIT_CENTER;
        }
        if (Attributes.ObjectFit.COVER.equals(objectFit)) {
            return ScalingUtils.ScaleType.CENTER_CROP;
        }
        if (Attributes.ObjectFit.FILL.equals(objectFit)) {
            return ScalingUtils.ScaleType.FIT_XY;
        }
        if (Attributes.ObjectFit.NONE.equals(objectFit)) {
            return ScalingUtils.ScaleType.CENTER;
        }
        if (Attributes.ObjectFit.SCALE_DOWN.equals(objectFit)) {
            // if the image is over-size, we set it again
            return ScalingUtils.ScaleType.CENTER;
        }

        // compat resize-mode
        if (STRETCH.equals(objectFit)) {
            return ScalingUtils.ScaleType.FIT_XY;
        }
        if (CENTER.equals(objectFit)) {
            return ScalingUtils.ScaleType.CENTER;
        }

        return scaleType;
    }

    private void removePlaceholderImage(GenericDraweeHierarchy hierarchy) {
        stopPlaceholderAnimation();
        mPlaceholderDrawable = null;
        try {
            hierarchy.setPlaceholderImage(null);
        } catch (Throwable e) {
            Log.w(TAG, "set placeholder error : ", e);
        }
    }

    private void setPlaceholderImage(GenericDraweeHierarchy hierarchy, Drawable drawable) {
        stopPlaceholderAnimation();
        mPlaceholderDrawable = drawable;
        try {
            hierarchy.setPlaceholderImage(drawable, mAltScaleType);
        } catch (Throwable e) {
            Log.w(TAG, "set placeholder error : ", e);
        }
        startPlaceholderAnimation();
    }

    private void startPlaceholderAnimation() {
        if (mPlaceholderDrawable instanceof AnimatedDrawable2) {
            AnimatedDrawable2 animatedDrawable2 = (AnimatedDrawable2) mPlaceholderDrawable;
            animatedDrawable2.start();
        }
    }

    private void stopPlaceholderAnimation() {
        if (mPlaceholderDrawable instanceof AnimatedDrawable2) {
            AnimatedDrawable2 animatedDrawable2 = (AnimatedDrawable2) mPlaceholderDrawable;
            animatedDrawable2.stop();
        }
    }

    public void setAutoplay(boolean autoplay) {
        if (autoplay != mAutoplay) {
            mAutoplay = autoplay;
            mIsDirty = true;
            maybeUpdateView(true);
        }
    }

    public void startAnimation() {
        mIsStartAnimation = true;
        if (mAnimatable != null) {
            mAnimatable.start();
        }
    }

    public void stopAnimation() {
        mIsStartAnimation = false;
        if (mAnimatable != null) {
            mAnimatable.stop();
        }
    }

    public boolean isAnimationRunning() {
        if (mAnimatable != null) {
            return mAnimatable.isRunning();
        }
        return false;
    }

    private AutoplayManager getAutoplayManager() {
        if (mAutoplayManager == null && mComponent != null
                && mComponent.getRootComponent() != null) {
            mAutoplayManager =
                    ((RootView) mComponent.getRootComponent().getHostView()).getAutoplayManager();
        }
        return mAutoplayManager;
    }

    public interface OnLoadStatusListener {
        void onComplete(int width, int height);

        void onError(Throwable throwable);
    }

    private static class DecodedListener implements BitmapUtils.OnDrawableDecodedListener {
        WeakReference<FlexImageView> flexImageViewWeakReference;
        Uri mBitmapUri;

        public DecodedListener(FlexImageView flexImageView, Uri bitmapUri) {
            flexImageViewWeakReference = new WeakReference<>(flexImageView);
            mBitmapUri = bitmapUri;
        }

        @Override
        public void onDrawableDecoded(Drawable drawable, Uri uri) {
            FlexImageView view = flexImageViewWeakReference.get();
            if (view != null) {
                if (drawable != null && UriUtils.equals(mBitmapUri, uri)) {
                    view.setPlaceholderImage(view.getHierarchy(), drawable);
                }
            }
        }
    }

    private class RoundedCornerPostprocessor extends BasePostprocessor {

        void getRadii(Bitmap source, float[] computedCornerRadii, float[] mappedRadii) {
            mScaleType.getTransform(
                    sMatrix,
                    new Rect(0, 0, source.getWidth(), source.getHeight()),
                    source.getWidth(),
                    source.getHeight(),
                    0.0f,
                    0.0f);
            sMatrix.invert(sInverse);

            mappedRadii[Corner.TOP_LEFT_X] =
                    sInverse.mapRadius(computedCornerRadii[Corner.TOP_LEFT_X]);
            mappedRadii[Corner.TOP_LEFT_Y] =
                    sInverse.mapRadius(computedCornerRadii[Corner.TOP_LEFT_Y]);

            mappedRadii[Corner.TOP_RIGHT_X] =
                    sInverse.mapRadius(computedCornerRadii[Corner.TOP_RIGHT_X]);
            mappedRadii[Corner.TOP_RIGHT_Y] =
                    sInverse.mapRadius(computedCornerRadii[Corner.TOP_RIGHT_Y]);

            mappedRadii[Corner.BOTTOM_RIGHT_X] =
                    sInverse.mapRadius(computedCornerRadii[Corner.BOTTOM_RIGHT_X]);
            mappedRadii[Corner.BOTTOM_RIGHT_Y] =
                    sInverse.mapRadius(computedCornerRadii[Corner.BOTTOM_RIGHT_Y]);

            mappedRadii[Corner.BOTTOM_LEFT_X] =
                    sInverse.mapRadius(computedCornerRadii[Corner.BOTTOM_LEFT_X]);
            mappedRadii[Corner.BOTTOM_LEFT_Y] =
                    sInverse.mapRadius(computedCornerRadii[Corner.BOTTOM_LEFT_Y]);
        }

        @Override
        public void process(Bitmap output, Bitmap source) {
            cornerRadii(mComputedCornerRadii);

            output.setHasAlpha(true);
            if (FloatUtil.floatsEqual(mComputedCornerRadii[Corner.TOP_LEFT_X], 0f)
                    && FloatUtil.floatsEqual(mComputedCornerRadii[Corner.TOP_LEFT_Y], 0f)
                    && FloatUtil.floatsEqual(mComputedCornerRadii[Corner.TOP_RIGHT_X], 0f)
                    && FloatUtil.floatsEqual(mComputedCornerRadii[Corner.TOP_RIGHT_Y], 0f)
                    && FloatUtil.floatsEqual(mComputedCornerRadii[Corner.BOTTOM_RIGHT_X], 0f)
                    && FloatUtil.floatsEqual(mComputedCornerRadii[Corner.BOTTOM_RIGHT_Y], 0f)
                    && FloatUtil.floatsEqual(mComputedCornerRadii[Corner.BOTTOM_LEFT_X], 0f)
                    && FloatUtil.floatsEqual(mComputedCornerRadii[Corner.BOTTOM_LEFT_Y], 0f)) {
                super.process(output, source);
                return;
            }
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setShader(new BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
            Canvas canvas = new Canvas(output);

            float[] radii = new float[8];

            getRadii(source, mComputedCornerRadii, radii);

            Path pathForBorderRadius = new Path();

            pathForBorderRadius.addRoundRect(
                    new RectF(0, 0, source.getWidth(), source.getHeight()), radii,
                    Path.Direction.CW);

            canvas.drawPath(pathForBorderRadius, paint);
        }
    }

    private class ImageSizeDetectRequestListener extends BaseRequestListener {
        @Override
        public void onProducerFinishWithSuccess(String requestId, String producerName, @javax.annotation.Nullable Map<String, String> extraMap) {
            // Check if the picture is too large
            if (AnalyzerHelper.getInstance().isInAnalyzerMode() && mSourceChanged && mSource!= null && TextUtils.equals(DECODE_PRODUCER_NAME, producerName)) {
                if (extraMap != null && !extraMap.isEmpty()) {
                    String encodeString = extraMap.get(ENCODE_SIZE_KEY);
                    String bitmapSizeString = extraMap.get(BITMAP_SIZE_KEY);
                    int refs = mComponent.getRef();
                    String src;
                    if ("file".equals(mSource.getScheme())) {
                        src = mSource.getLastPathSegment();
                    } else {
                        src = mSource.toString();
                    }
                    if (!TextUtils.isEmpty(encodeString) && !TextUtils.isEmpty(bitmapSizeString) && !TextUtils.isEmpty(src)){
                        float encodeSize = AnalyzerHelper.getInstance().parsePixelsNumFromString(encodeString);
                        float bitmapSize = AnalyzerHelper.getInstance().parsePixelsNumFromString(bitmapSizeString);
                        Log.d(TAG, "AnalyzerPanel_LOG image_resize " + encodeString + " -> " + bitmapSizeString);
                        // Fresco's definition of "a lot bigger": the number of pixels in the picture> the magnitude of the view x 2
                        if (encodeSize > 0 && bitmapSize > 0 && encodeSize > bitmapSize * 2) {
                            Page currentPage = AnalyzerHelper.getInstance().getCurrentPage();
                            if (currentPage != null) {
                                String warnContent = getContext().getString(R.string.analyzer_image_check_warning, currentPage.getName(), encodeString, bitmapSizeString, src);
                                NoticeMessage warn = NoticeMessage.warn(currentPage.getName(), warnContent);
                                warn.setAction(new NoticeMessage.UIAction.Builder().pageId(currentPage.getPageId()).addComponentId(refs).build());
                                AnalyzerHelper.getInstance().notice(warn);
                            }
                        }
                    }
                }
            }
        }

        @Override
        public boolean requiresExtraMap(String requestId) {
            return true;
        }
    }
}

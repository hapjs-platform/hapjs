/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.NinePatchDrawable;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import com.facebook.common.references.CloseableReference;
import com.facebook.drawee.components.DeferredReleaser;
import com.facebook.imagepipeline.image.CloseableBitmap;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.hapjs.common.executors.Executors;
import org.hapjs.common.utils.BitmapUtils;
import org.hapjs.common.utils.ColorUtil;
import org.hapjs.common.utils.FloatUtil;
import org.hapjs.common.utils.UriUtils;
import org.hapjs.component.animation.CSSAnimatorSet;
import org.hapjs.component.callback.VisibilityDrawableCallback;
import org.hapjs.component.constants.Edge;
import org.hapjs.component.constants.Spacing;
import org.hapjs.component.view.CSSGradientParser;
import org.hapjs.component.view.drawable.CSSBackgroundDrawable;
import org.hapjs.component.view.drawable.SizeBackgroundDrawable;
import org.hapjs.component.view.drawable.SizeBackgroundDrawable.Position;

public class ComponentBackgroundComposer {

    private static final String TAG = "BackgroundComposer";
    private final Parameter mParameter;
    private Component mComponent;
    private boolean mDirty = true;
    private BackgroundHolder mBackgroundHolder;
    private CSSBackgroundDrawable mBackgroundDrawable;

    private Drawable mGradientDrawable;
    private Drawable mImageDrawable;

    public ComponentBackgroundComposer(@NonNull Component component) {
        mComponent = component;
        mParameter = new Parameter();
        mBackgroundHolder = new BackgroundHolder(component, this);
        mBackgroundDrawable = new CSSBackgroundDrawable();
    }

    public CSSBackgroundDrawable getBackgroundDrawable() {
        return mBackgroundDrawable;
    }

    public int getBackgroundColor() {
        return mParameter.mBackgroundColor;
    }

    public void setBackgroundColor(String color) {
        int c = ColorUtil.getColor(color, Color.TRANSPARENT);
        setBackgroundColor(c);
    }

    public void setBackgroundColor(int color) {
        if (mParameter.mBackgroundColor != color) {
            mParameter.mBackgroundColor = color;
            mDirty = true;
        }
    }

    public void setBackgroundImage(String img) {
        setBackgroundImage(img, false);
    }

    public void setBackgroundImage(String img, boolean setBlur) {
        if (!TextUtils.equals(mParameter.mBackgroundImg, img)) {
            mParameter.mBackgroundImg = img;
            mParameter.mSetBackgroundBlur = setBlur;
            mDirty = true;
        }
    }

    public void setBackgroundSize(String size) {
        if (!TextUtils.equals(mParameter.mBackgroundSize, size)) {
            mParameter.mBackgroundSize = size;
            mDirty = true;
        }
    }

    public void setBackgroundRepeat(String repeat) {
        repeat = SizeBackgroundDrawable.RepeatMode.parse(repeat).getDesc();
        if (!TextUtils.equals(mParameter.mBackgroundRepeatMode, repeat)) {
            mParameter.mBackgroundRepeatMode = repeat;
            mDirty = true;
        }
    }

    public void setBackgroundPosition(String position) {
        if (!TextUtils.equals(mParameter.mBackgroundPosition, position)) {
            mParameter.mBackgroundPosition = position;
            mDirty = true;
        }
    }

    public void setBackground(String background) {
        if (!TextUtils.equals(mParameter.mBackground, background)) {
            mParameter.mBackground = background;
            mDirty = true;
        }
    }

    public void setBorderColor(int position, int color) {
        if (mParameter.mBorderColor == null) {
            mParameter.mBorderColor = new int[5];
            Arrays.fill(mParameter.mBorderColor, CSSBackgroundDrawable.DEFAULT_BORDER_COLOR);
        }
        if (mParameter.mBorderColor[position] != color) {
            mParameter.mBorderColor[position] = color;
            mDirty = true;
        }
        // 确保数据的一致性
        if (position == Edge.ALL) {
            mParameter.mBorderColor[Edge.LEFT] =
                    mParameter.mBorderColor[Edge.TOP] =
                            mParameter.mBorderColor[Edge.RIGHT] =
                                    mParameter.mBorderColor[Edge.BOTTOM] = color;
        }
    }

    public int getBorderColor(int position) {
        return mBackgroundDrawable.getBorderColor(position);
    }

    public void setBorderWidth(int position, float width) {
        if (!FloatUtil.floatsEqual(mParameter.mBorderWidth.getRaw(position), width)) {
            mParameter.mBorderWidth.set(position, width);
            mDirty = true;
        }
    }

    public float getBorderWidth(int position) {
        return mBackgroundDrawable.getBorderWidth(position);
    }

    public String getBorderStyle() {
        return mBackgroundDrawable.getBorderStyle();
    }

    public void setBorderStyle(String style) {
        if (!TextUtils.equals(mParameter.mBorderStyle, style)) {
            mParameter.mBorderStyle = style;
            mDirty = true;
        }
    }

    public void setBorderCornerRadii(int position, float radius) {
        if (!FloatUtil.floatsEqual(mParameter.mBorderCornerRadii[position], radius)) {
            mParameter.mBorderCornerRadii[position] = radius;
            mParameter.mBorderCornerRadiiPercent[position] = FloatUtil.UNDEFINED;
            mDirty = true;
        }
    }

    public void setBorderRadius(float radius) {
        if (!FloatUtil.floatsEqual(mParameter.mBorderRadius, radius)) {
            mParameter.mBorderRadius = radius;
            mParameter.mBorderRadiusPercent = FloatUtil.UNDEFINED;
            mDirty = true;
        }
    }

    public void setBorderCornerRadiiPercent(int position, float radiusPercent) {
        if (!FloatUtil.floatsEqual(mParameter.mBorderCornerRadiiPercent[position], radiusPercent)) {
            mParameter.mBorderCornerRadiiPercent[position] = radiusPercent;
            mParameter.mBorderCornerRadii[position] = FloatUtil.UNDEFINED;
            mDirty = true;
        }
    }

    public void setBorderRadiusPercent(float radiusPercent) {
        if (!FloatUtil.floatsEqual(mParameter.mBorderRadiusPercent, radiusPercent)) {
            mParameter.mBorderRadiusPercent = radiusPercent;
            mParameter.mBorderRadius = FloatUtil.UNDEFINED;
            mDirty = true;
        }
    }

    public void apply() {
        if (!mDirty) {
            return;
        }

        final Uri imgUri = mComponent.tryParseUri(mParameter.mBackgroundImg);
        if (imgUri == null) {
            mImageDrawable = null;
            applyDrawable();
        } else {
            if (!TextUtils.isEmpty(imgUri.getLastPathSegment())
                    && imgUri.getLastPathSegment() != null
                    && imgUri.getLastPathSegment().endsWith(BitmapUtils.NINE_PATCH_SUFFIX)) {
                BitmapUtils.fetchLocalDrawable(
                        mComponent.getHostView().getContext(),
                        imgUri,
                        new BitmapUtils.OnDrawableDecodedListener() {
                            @Override
                            public void onDrawableDecoded(Drawable drawable, Uri fileUri) {
                                if (UriUtils.equals(imgUri, fileUri)) {
                                    mImageDrawable = drawable;
                                    applyDrawable();
                                }
                            }
                        });
            } else {
                if (TextUtils.equals(imgUri.getScheme(), "http")
                        || TextUtils.equals(imgUri.getScheme(), "https")) {
                    doFetchBitmap(imgUri, mParameter.mBackgroundImg, mParameter.mSetBackgroundBlur);
                } else {
                    Executors.io()
                            .execute(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            doFetchBitmap(imgUri, mParameter.mBackgroundImg, mParameter.mSetBackgroundBlur);
                                        }
                                    });
                }
            }
        }
    }

    public void invalid() {
        mDirty = true;
    }

    private void applyDrawable() {
        if (mComponent == null) {
            return;
        }
        View hostView = mComponent.getHostView();
        if (hostView == null) {
            return;
        }
        mBackgroundDrawable.setColor(mParameter.mBackgroundColor);
        mBackgroundDrawable.setBorderColor(mParameter.mBorderColor);
        mBackgroundDrawable.setBorderWidth(mParameter.mBorderWidth);
        mBackgroundDrawable.setBorderStyle(mParameter.mBorderStyle);
        mBackgroundDrawable.setRadius(mParameter.mBorderRadius);
        mBackgroundDrawable.setRadius(mParameter.mBorderCornerRadii);
        mBackgroundDrawable.setRadiusPercent(mParameter.mBorderRadiusPercent);
        mBackgroundDrawable.setRadiusPercent(mParameter.mBorderCornerRadiiPercent);
        mGradientDrawable =
                CSSGradientParser
                        .parseGradientDrawable(mComponent.mHapEngine, mParameter.mBackground);

        if (isLayerDrawableValid()) {
            List<Drawable> drawables = new ArrayList<>();

            // Avoid default background of some component not work, e.g button.
            if (getDefaultBgDrawable() != null) {
                drawables.add(getDefaultBgDrawable());
            }

            if (mImageDrawable != null) {
                tryDelayedBgTransition();
                drawables.add(transformDrawable(mImageDrawable));
            }

            if (mGradientDrawable != null) {
                tryDelayedBgTransition();
                drawables.add(transformDrawable(mGradientDrawable));
            }

            Drawable[] tmp = new Drawable[drawables.size()];
            LayerDrawable layerDrawable = new LayerDrawable(drawables.toArray(tmp));
            mBackgroundDrawable.setLayerDrawable(layerDrawable);
        } else {
            mBackgroundDrawable.setLayerDrawable(null);
        }

        hostView.setBackground(mBackgroundDrawable);
        mDirty = false;

        if (mImageDrawable != null && mImageDrawable instanceof NinePatchDrawable) {
            mComponent.refreshPaddingFromBackground((NinePatchDrawable) mImageDrawable);
        }
    }

    private void tryDelayedBgTransition() {
        if (mComponent == null) {
            return;
        }
        Component component = mComponent.getSceneRootComponent();
        if (component != null) {
            component
                    .getOrCreateTransitionSet()
                    .beginDelayedTransition(mComponent.getHostView(),
                            (ViewGroup) component.getHostView());
        }
    }

    private Drawable transformDrawable(Drawable drawable) {
        if (!(drawable instanceof BitmapDrawable) || mComponent.getHostView() == null) {
            return drawable;
        }

        SizeBackgroundDrawable sizeBackgroundDrawable =
                new SizeBackgroundDrawable(
                        mComponent.mHapEngine,
                        mComponent.getHostView().getResources(),
                        (BitmapDrawable) drawable,
                        mBackgroundHolder);

        sizeBackgroundDrawable.setHostView(mComponent.getHostView());
        sizeBackgroundDrawable.setBackgroundUrl(mParameter.mBackgroundImg);
        sizeBackgroundDrawable.setBackgroundSize(mParameter.mBackgroundSize);
        sizeBackgroundDrawable.setBackgroundPosition(mParameter.mBackgroundPosition);
        sizeBackgroundDrawable.setBackgroundRepeat(mParameter.mBackgroundRepeatMode);

        return sizeBackgroundDrawable;
    }

    private void doFetchBitmap(final Uri backgroundUri, final String originBgImgStr) {
        doFetchBitmap(backgroundUri, originBgImgStr, false);
    }

    private void doFetchBitmap(final Uri backgroundUri, final String originBgImgStr, boolean setBlur) {
        if (backgroundUri == null || originBgImgStr == null) {
            return;
        }
        mBackgroundHolder.setRequestSubmitted(true);
        // 按照图片原有尺寸获取
        BitmapUtils.fetchBitmap(backgroundUri,
                new BitmapLoadCallback(this, backgroundUri, originBgImgStr),
                0, 0, setBlur);
    }

    private static class BitmapLoadCallback implements BitmapUtils.BitmapLoadCallback {
        private WeakReference<ComponentBackgroundComposer> mBackgroundComposerRef;
        private Uri mBackgroundUri;
        private String mOriginBgImgStr;

        public BitmapLoadCallback(ComponentBackgroundComposer backgroundComposer,
                                  Uri backgroundUri, String originBgImgStr) {
            mBackgroundComposerRef = new WeakReference<>(backgroundComposer);
            mBackgroundUri = backgroundUri;
            mOriginBgImgStr = originBgImgStr;
        }

        @Override
        public void onLoadSuccess(CloseableReference reference, Bitmap bitmap) {
            ComponentBackgroundComposer backgroundComposer = mBackgroundComposerRef.get();
            if (backgroundComposer != null) {
                backgroundComposer.onLoadBitmapSuccess(reference, bitmap, mBackgroundUri, mOriginBgImgStr);
            }
        }

        @Override
        public void onLoadFailure() {
            ComponentBackgroundComposer backgroundComposer = mBackgroundComposerRef.get();
            if (backgroundComposer != null) {
                backgroundComposer.onLoadBitmapFailure(mBackgroundUri);
            }
        }
    }

    private void onLoadBitmapSuccess(CloseableReference reference, Bitmap bitmap, Uri backgroundUri, String originBgImgStr) {
        if (bitmap != null && mComponent != null
                && originBgImgStr.equals(mParameter.mBackgroundImg)) {
            Resources resources = null;
            Context context = mComponent.mContext;
            if (context != null) {
                resources = context.getResources();
            }
            if (resources != null) {
                mImageDrawable = new BitmapDrawable(resources, bitmap);
            } else {
                mImageDrawable = new BitmapDrawable(bitmap);
            }
            mBackgroundHolder.setCloseableReference(backgroundUri, reference);
            applyDrawable();
        }
    }

    private void onLoadBitmapFailure(Uri backgroundUri) {
        Log.e(TAG, "onLoadFailure backgroundUrl:" + backgroundUri.toString());
        mImageDrawable = null;
        applyDrawable();
    }

    public void releaseDrawable() {
        if (null != mBackgroundDrawable) {
            mBackgroundDrawable.setLayerDrawable(null);
        }
    }

    public int[] getBgRelativeWidthHeight() {
        return mBackgroundHolder.mBgRelativeWidthHeight;
    }

    public String getInitialPositionStr() {
        return mBackgroundHolder.mInitialPositionStr;
    }

    public void setListenToBgPosition(boolean listenToPosition) {
        mBackgroundHolder.mListenToDrawablePosition = listenToPosition;
    }

    public void destroy() {
        mBackgroundHolder.destroy();
    }

    private boolean isLayerDrawableValid() {
        return mGradientDrawable != null || mImageDrawable != null
                || getDefaultBgDrawable() != null;
    }

    private Drawable getDefaultBgDrawable() {
        if (mComponent.getHostView() == null
                || mComponent.getHostView().getBackground() instanceof CSSBackgroundDrawable) {
            return null;
        }
        return mComponent.getHostView().getBackground();
    }

    private static class BackgroundHolder
            implements DeferredReleaser.Releasable, VisibilityDrawableCallback {

        private Component mComponent;
        // for mBackground image release
        private DeferredReleaser mDeferredReleaser;
        private boolean mIsVisible = true;
        private boolean mIsViewAttached = false;
        private boolean mIsRequestSubmitted;
        private Uri mBackgroundUrl;
        private CloseableReference mCloseableReference;
        private SoftReference<ComponentBackgroundComposer> mBackgroundComposerReference;
        private int[] mBgRelativeWidthHeight = null;
        private boolean mListenToDrawablePosition = false;
        private String mInitialPositionStr = null;

        public BackgroundHolder(@NonNull Component component,
                                ComponentBackgroundComposer composer) {
            mComponent = component;
            initBackgroundSetting();
            mBackgroundComposerReference = new SoftReference<>(composer);
        }

        // for mBackground image release
        @Override
        public boolean onDraw(String currentDrawUrl) {
            boolean isNeedRedraw = true;
            mIsViewAttached = true;
            mIsVisible = true;
            if (currentDrawUrl == null
                    || mBackgroundComposerReference.get() == null
                    || !mBackgroundComposerReference.get().isLayerDrawableValid()) {
                isNeedRedraw = false;
            }
            return isNeedRedraw;
        }

        // for mBackground image release
        @Override
        public void onVisibilityChange(boolean visible) {
            if (mIsVisible == visible) {
                return;
            }
            mIsVisible = visible;
            attachOrDetachBgroundDrawable();
        }

        @Override
        public void onPositionCalculated(@NonNull Position position) {
            if (mBgRelativeWidthHeight == null) {
                mBgRelativeWidthHeight = new int[2];
                mInitialPositionStr = position.getParseStr();
            }
            mBgRelativeWidthHeight[0] = position.getRelativeWidth();
            mBgRelativeWidthHeight[1] = position.getRelativeHeight();
            if (mListenToDrawablePosition) {
                mListenToDrawablePosition = false;
                mComponent
                        .getHostView()
                        .post(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        CSSAnimatorSet animatorSet = mComponent.getAnimatorSet();
                                        if (animatorSet != null) {
                                            CSSAnimatorSet temp = animatorSet.parseAndStart();
                                            if (temp != null) {
                                                mComponent.setAnimatorSet(temp);
                                            }
                                        }
                                    }
                                });
            }
        }

        // for mBackground image release
        private void attachOrDetachBgroundDrawable() {
            if (mIsViewAttached && mIsVisible) {
                if (mDeferredReleaser != null) {
                    mDeferredReleaser.cancelDeferredRelease(this);
                    if (!mIsRequestSubmitted || getCloseableBitmap(mBackgroundUrl) == null) {
                        ComponentBackgroundComposer composer = mBackgroundComposerReference.get();
                        if (composer != null) {
                            composer.apply();
                        }
                    }
                }
            } else {
                if (mDeferredReleaser != null) {
                    mDeferredReleaser.scheduleDeferredRelease(this);
                    ComponentBackgroundComposer composer = mBackgroundComposerReference.get();
                    if (composer != null) {
                        composer.invalid();
                    }
                }
            }
        }

        private void initBackgroundSetting() {
            mDeferredReleaser = DeferredReleaser.getInstance();
            View hostView = mComponent.getHostView();
            if (hostView != null) {
                hostView.addOnAttachStateChangeListener(
                        new View.OnAttachStateChangeListener() {
                            @Override
                            public void onViewAttachedToWindow(View v) {
                                mIsViewAttached = true;
                                attachOrDetachBgroundDrawable();
                            }

                            @Override
                            public void onViewDetachedFromWindow(View v) {
                                mIsViewAttached = false;
                                attachOrDetachBgroundDrawable();
                            }
                        });
            }
        }

        @Override
        public void release() {
            ComponentBackgroundComposer composer = mBackgroundComposerReference.get();
            if (composer != null) {
                composer.releaseDrawable();
            }
            mIsRequestSubmitted = false;
            CloseableReference.closeSafely(mCloseableReference);
        }

        public void setRequestSubmitted(boolean requestSubmitted) {
            mIsRequestSubmitted = requestSubmitted;
        }

        public void destroy() {
            CloseableReference.closeSafely(mCloseableReference);
        }

        public Bitmap getCloseableBitmap(Uri backgroundUrl) {
            if (mCloseableReference != null
                    && mCloseableReference.isValid()
                    && mCloseableReference.get() instanceof CloseableBitmap
                    && backgroundUrl != null
                    && backgroundUrl.equals(mBackgroundUrl)) {
                return ((CloseableBitmap) mCloseableReference.get()).getUnderlyingBitmap();
            }
            return null;
        }

        public void setCloseableReference(Uri backgroundUrl, CloseableReference reference) {
            mBackgroundUrl = backgroundUrl;
            mCloseableReference = reference;
        }
    }

    private static class Parameter {
        int mBackgroundColor;
        String mBackgroundImg;
        boolean mSetBackgroundBlur;
        String mBackgroundSize;
        String mBackgroundRepeatMode;
        String mBackgroundPosition;
        String mBackground;
        int[] mBorderColor;
        Spacing mBorderWidth = new Spacing();
        float[] mBorderCornerRadii =
                new float[] {
                        FloatUtil.UNDEFINED, FloatUtil.UNDEFINED, FloatUtil.UNDEFINED,
                        FloatUtil.UNDEFINED
                };
        float mBorderRadius = FloatUtil.UNDEFINED;
        float[] mBorderCornerRadiiPercent =
                new float[] {
                        FloatUtil.UNDEFINED, FloatUtil.UNDEFINED, FloatUtil.UNDEFINED,
                        FloatUtil.UNDEFINED
                };
        float mBorderRadiusPercent = FloatUtil.UNDEFINED;
        String mBorderStyle;
    }
}

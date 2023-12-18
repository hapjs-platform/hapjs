/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.progress;

import android.content.Context;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import androidx.annotation.RequiresApi;

import java.util.Map;
import org.hapjs.bridge.annotation.TypeAnnotation;
import org.hapjs.bridge.annotation.WidgetAnnotation;
import org.hapjs.common.utils.ColorUtil;
import org.hapjs.component.Component;
import org.hapjs.component.Container;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.component.constants.Attributes;
import org.hapjs.component.view.GestureFrameLayout;
import org.hapjs.runtime.HapEngine;
import org.hapjs.widgets.R;

@WidgetAnnotation(
        name = Progress.WIDGET_NAME,
        methods = {
                Component.METHOD_ANIMATE,
                Component.METHOD_GET_BOUNDING_CLIENT_RECT,
                Component.METHOD_TO_TEMP_FILE_PATH,
                Component.METHOD_FOCUS,
                Component.METHOD_TALKBACK_FOCUS,
                Component.METHOD_TALKBACK_ANNOUNCE
        },
        types = {@TypeAnnotation(name = HorizontalProgress.TYPE_HORIZONTAL, isDefault = true)})
public class HorizontalProgress extends Progress<FrameLayout> {

    protected static final String TYPE_HORIZONTAL = "horizontal";

    private static final String LAYER_COLOR = "layerColor";

    private static final float CORNER_RADIUS = 15;

    private static final int DEFAULT_LAYER_COLOR = 0xfff0f0f0;

    private ProgressBar mProgressBar;
    private GradientDrawable mLayerDrawable;
    private GradientDrawable mProgressDrawable;

    private int mProgressColor = DEFAULT_COLOR;
    private int mLayerColor = DEFAULT_LAYER_COLOR;

    public HorizontalProgress(
            HapEngine hapEngine,
            Context context,
            Container parent,
            int ref,
            RenderEventCallback callback,
            Map<String, Object> savedState) {
        super(hapEngine, context, parent, ref, callback, savedState);
    }

    @Override
    protected FrameLayout createViewImpl() {
        mLayerDrawable = new GradientDrawable();
        mLayerDrawable.setColor(mLayerColor);
        mLayerDrawable.setCornerRadius(CORNER_RADIUS);

        mProgressDrawable = new GradientDrawable();
        mProgressDrawable.setColor(mProgressColor);
        mProgressDrawable.setCornerRadius(CORNER_RADIUS);

        ClipDrawable clip =
                new ClipDrawable(mProgressDrawable, Gravity.LEFT, ClipDrawable.HORIZONTAL);
        LayerDrawable layer = new LayerDrawable(new Drawable[] {mLayerDrawable, clip});
        layer.setId(0, android.R.id.background);
        layer.setId(1, android.R.id.progress);

        mProgressBar = new ProgressBar(mContext, null, android.R.attr.progressBarStyleHorizontal);
        initTalkBack();
        mProgressBar.setProgressDrawable(layer);
        FrameLayout.LayoutParams lp =
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT, mDefaultDimension,
                        Gravity.CENTER_VERTICAL);

        GestureFrameLayout frameLayout = new GestureFrameLayout(mContext);
        frameLayout.addView(mProgressBar, lp);
        frameLayout.setComponent(this);
        return frameLayout;
    }

    private void initTalkBack() {
        if (isEnableTalkBack() && null != mProgressBar) {
            mProgressBar.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(View v) {
                    mProgressBar.setAccessibilityDelegate(new View.AccessibilityDelegate() {
                        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                        @Override
                        public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
                            super.onInitializeAccessibilityNodeInfo(host, info);
                            info.setClassName("");
                            info.setClickable(false);
                            info.removeAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK);
                            if (null != mProgressBar && mProgressBar.getProgress() >= 0 && null != mContext) {
                                info.setText(mContext.getResources().getString(R.string.talkback_progress_percent)
                                        + mProgressBar.getProgress()
                                        + " "
                                        + mContext.getResources().getString(R.string.talkback_progress));
                            }
                        }
                    });
                }

                @Override
                public void onViewDetachedFromWindow(View v) {
                    if (null != mProgressBar) {
                        mProgressBar.setAccessibilityDelegate(null);
                        mProgressBar.removeOnAttachStateChangeListener(this);
                    }
                }
            });
        }
    }

    @Override
    protected boolean setAttribute(String key, Object attribute) {
        switch (key) {
            case Attributes.Style.PERCENT:
                int percent = Attributes.getInt(mHapEngine, attribute, 0);
                setPercent(percent);
                return true;
            case Attributes.Style.COLOR:
                String colorStr =
                        Attributes.getString(attribute, ColorUtil.getColorStr(mProgressColor));
                setColor(colorStr);
                return true;
            case Attributes.Style.STROKE_WIDTH:
                int strokeWidth = Attributes.getInt(mHapEngine, attribute, mDefaultDimension);
                setStrokeWidth(strokeWidth);
                return true;
            case LAYER_COLOR:
                String layerColorStr =
                        Attributes.getString(attribute, ColorUtil.getColorStr(mLayerColor));
                setLayerColor(layerColorStr);
                return true;
            default:
                break;
        }

        return super.setAttribute(key, attribute);
    }

    public void setPercent(int percent) {
        if (mProgressBar == null) {
            return;
        }
        if (percent < 0) {
            percent = 0;
        }
        if (percent > 100) {
            percent = 100;
        }
        mProgressBar.setProgress(percent);
    }

    public void setColor(String colorStr) {
        if (TextUtils.isEmpty(colorStr) || mProgressDrawable == null) {
            return;
        }

        mProgressColor = ColorUtil.getColor(colorStr, mProgressColor);
        mProgressDrawable.setColor(mProgressColor);
    }

    public void setStrokeWidth(int strokeWidth) {
        if (mProgressBar == null) {
            return;
        }
        if (strokeWidth <= 0) {
            strokeWidth = mDefaultDimension;
        }
        ViewGroup.LayoutParams lp = mProgressBar.getLayoutParams();
        lp.height = strokeWidth;
        mProgressBar.setLayoutParams(lp);
    }

    public void setLayerColor(String layerColorStr) {
        if (TextUtils.isEmpty(layerColorStr) || mLayerDrawable == null) {
            return;
        }

        mLayerColor = ColorUtil.getColor(layerColorStr, mLayerColor);
        mLayerDrawable.setColor(mLayerColor);
    }
}

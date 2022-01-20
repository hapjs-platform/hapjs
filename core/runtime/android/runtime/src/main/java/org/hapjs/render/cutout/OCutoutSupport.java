/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.cutout;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import java.util.List;
import org.hapjs.common.utils.DisplayUtil;

class OCutoutSupport implements ICutoutSupport {
    private CutoutProvider mCutoutProvider;

    OCutoutSupport(@Nullable CutoutProvider provider) {
        mCutoutProvider = provider;
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Override
    public boolean isCutoutScreen(@NonNull Context context, @NonNull Window window) {
        if (mCutoutProvider == null) {
            return false;
        }
        return mCutoutProvider.isCutoutScreen(context, window);
    }

    /**
     * 获取异形屏Cutout区域信息
     *
     * @param context
     * @param window
     * @return List<Rect> Rect left: cutout在竖屏下相对于屏幕左边的偏移距离 Rect top: cutout在竖屏下相对于屏幕顶部的偏移距离 Rect
     * right: cutout在竖屏下相对于屏幕右边的偏移距离 Rect bottom: cutout在竖屏下相对于屏幕底部的偏移距离
     */
    @Nullable
    @Override
    public List<Rect> getCutoutDisplay(@NonNull Context context, @NonNull Window window) {
        if (mCutoutProvider == null) {
            return null;
        }
        return mCutoutProvider.getCutoutDisplay(context, window);
    }

    @Override
    public int getCutoutHeight(@NonNull Context context, @NonNull Window window) {
        if (mCutoutProvider == null) {
            return 0;
        }
        return mCutoutProvider.getCutoutHeight(context, window);
    }

    @TargetApi(Build.VERSION_CODES.O)
    @Override
    public void fit(
            @NonNull Context context,
            @NonNull Window window,
            View cutoutView,
            boolean portrait,
            String fitCutout) {

        if (cutoutView == null) {
            return;
        }

        boolean isCutoutScreen = isCutoutScreen(context, window);
        if (!isCutoutScreen) {
            return;
        }

        List<Rect> cutoutDisplay = getCutoutDisplay(context, window);
        int cutoutHeight;

        if (cutoutDisplay == null || cutoutDisplay.isEmpty()) {
            cutoutHeight = getCutoutHeight(context, window);
        } else {
            // 暂定异形区域只有1个
            cutoutHeight = DisplayUtil.getScreenHeight(context) - cutoutDisplay.get(0).height();
        }

        if (cutoutHeight > 0
                && (!isValidFitcutout(fitCutout)
                || (portrait && !fitCutout.toLowerCase().contains(FIT_CUTOUT_PORTRAIT))
                || (!portrait && !fitCutout.toLowerCase().contains(FIT_CUTOUT_LANDSCAPE)))) {

            ViewGroup.LayoutParams layoutParams = cutoutView.getLayoutParams();
            if (layoutParams != null) {
                if (portrait) {
                    layoutParams.height = cutoutHeight;
                } else {
                    layoutParams.width = cutoutHeight;
                }
                cutoutView.setBackgroundColor(Color.BLACK);
                cutoutView.requestLayout();
            }
        } else {
            ViewGroup.LayoutParams layoutParams = cutoutView.getLayoutParams();
            if (layoutParams != null) {
                if (portrait) {
                    layoutParams.height = 0;
                } else {
                    layoutParams.width = 0;
                }
                cutoutView.requestLayout();
            }
        }
    }

    private boolean isValidFitcutout(String fitCutout) {
        return !TextUtils.isEmpty(fitCutout)
                && (fitCutout.toLowerCase().contains(FIT_CUTOUT_PORTRAIT)
                || fitCutout.toLowerCase().contains(FIT_CUTOUT_LANDSCAPE));
    }
}

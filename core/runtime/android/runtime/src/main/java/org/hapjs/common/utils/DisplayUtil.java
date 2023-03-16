/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.utils;

import android.app.UiModeManager;
import android.content.Context;
import android.content.res.Configuration;
import android.util.DisplayMetrics;
import android.view.WindowInsets;

import org.hapjs.model.AppInfo;
import org.hapjs.runtime.HapEngine;
import org.hapjs.runtime.ProviderManager;
import org.hapjs.runtime.Runtime;
import org.hapjs.system.SysOpProvider;

import java.util.HashMap;

public class DisplayUtil {
    private static final String TAG = "DisplayUtil";
    public static final String USE_FOLDSTATUS_WH = "use_foldstatus_wh";
    private static SysOpProvider sysOpProvider = ProviderManager.getDefault().getProvider(SysOpProvider.NAME);

    private static WindowInsets sWindowInsets;
    private static HapEngine sHapEngine;
    private static int sViewPortWidth;
    private static int sViewPortHeight;

    private DisplayUtil() {
    }

    public static void setViewPortWidth(int width) {
        sViewPortWidth = width;
    }

    public static void setViewPortHeight(int height) {
        sViewPortHeight = height;
    }

    public static HapEngine getHapEngine() {
        return sHapEngine;
    }

    public static void setHapEngine(HapEngine hapEngine) {
        sHapEngine = hapEngine;
    }

    public static float getRealPxByWidth(float designPx, int designWidth) {
        Context context = Runtime.getInstance().getContext();
        SysOpProvider provider = ProviderManager.getDefault().getProvider(SysOpProvider.NAME);
        float densityScaledRatio = provider != null ? provider.getDensityScaledRatio(context) : 1f;
        return designPx * getScreenWidth(context) / designWidth * densityScaledRatio;
    }

    public static float getDesignPxByWidth(float realPx, int designWidth) {
        Context context = Runtime.getInstance().getContext();
        SysOpProvider provider = ProviderManager.getDefault().getProvider(SysOpProvider.NAME);
        float densityScaledRatio = provider != null ? provider.getDensityScaledRatio(context) : 1f;
        return realPx / getScreenWidth(context) * designWidth / densityScaledRatio;
    }

    public static float getFoldRealPxByWidth(float designPx, int designWidth,boolean isFold) {
        Context context = Runtime.getInstance().getContext();
        SysOpProvider provider = ProviderManager.getDefault().getProvider(SysOpProvider.NAME);
        float densityScaledRatio = provider != null ? provider.getDensityScaledRatio(context) : 1f;
        HashMap<String,Object> params = new HashMap<>();
        params.put(USE_FOLDSTATUS_WH,isFold);
        return designPx * getScreenWidth(context,params) / designWidth * densityScaledRatio;
    }

    public static int getScreenWidth(Context context, HashMap<String,Object> datas) {
        if (context == null || sHapEngine == null) {
            return 0;
        }

        AppInfo appInfo = sHapEngine.getApplicationContext().getAppInfo();
        if (appInfo == null) {
            return 0;
        }
        return sysOpProvider.getScreenWidthPixels(context, appInfo.getMinPlatformVersion(),datas);
    }

    public static int getScreenHeight(Context context, HashMap<String,Object> datas) {
        if (context == null || sHapEngine == null) {
            return 0;
        }

        AppInfo appInfo = sHapEngine.getApplicationContext().getAppInfo();
        if (appInfo == null) {
            return 0;
        }
        return sysOpProvider.getScreenHeightPixels(context, appInfo.getMinPlatformVersion(),datas);
    }


    public static boolean isPortraitMode(Context context) {
        if (context != null
                && context.getResources() != null
                && context.getResources().getDisplayMetrics() != null) {
            DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
            return displayMetrics.heightPixels > displayMetrics.widthPixels;
        }
        return false;
    }

    public static boolean isLandscapeMode(Context context) {
        if (context != null
                && context.getResources() != null
                && context.getResources().getDisplayMetrics() != null) {
            DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
            return displayMetrics.widthPixels > displayMetrics.heightPixels;
        }
        return false;
    }

    public static int getScreenWidth(Context context) {
        if (context == null || sHapEngine == null) {
            return 0;
        }

        AppInfo appInfo = sHapEngine.getApplicationContext().getAppInfo();
        if (appInfo == null) {
            return 0;
        }
        return sysOpProvider.getScreenWidthPixels(context, appInfo.getMinPlatformVersion(),null);
    }

    public static int getScreenHeight(Context context) {
        if (context == null || sHapEngine == null) {
            return 0;
        }

        AppInfo appInfo = sHapEngine.getApplicationContext().getAppInfo();
        if (appInfo == null) {
            return 0;
        }
        return sysOpProvider.getScreenHeightPixels(context, appInfo.getMinPlatformVersion(),null);
    }

    public static int getStatusBarHeight(Context context) {
        StatusBarSizeProvider provider = ProviderManager.getDefault().getProvider(StatusBarSizeProvider.NAME);

        return provider.getStatusBarHeight(context);
    }

    public static int parseCmToPx(Context context, float cm) {
        float density = context.getResources().getDisplayMetrics().densityDpi;
        return Math.round(cm * density / 2.54F);
    }

    public static int dip2Pixel(Context context, int dip) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        float density = displayMetrics.density;
        return (int) (dip * density + 0.5f);
    }

    public static int getDestinyDpi() {
        DisplayMetrics dm = Runtime.getInstance().getContext().getResources().getDisplayMetrics();
        return dm.densityDpi;
    }

    public static int getScreenWidthByDP() {
        Context context = Runtime.getInstance().getContext();
        if (context == null) {
            return 0;
        }
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return (int) (displayMetrics.widthPixels / displayMetrics.density);
    }

    public static int getScreenHeightByDp() {
        Context context = Runtime.getInstance().getContext();
        if (context == null) {
            return 0;
        }
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return (int) (displayMetrics.heightPixels / displayMetrics.density);
    }

    public static int getViewPortWidthByDp() {
        Context context = Runtime.getInstance().getContext();
        if (context == null) {
            return 0;
        }
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return (int) (sViewPortWidth / displayMetrics.density);
    }

    public static int getViewPortHeightByDp() {
        Context context = Runtime.getInstance().getContext();
        if (context == null) {
            return 0;
        }
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return (int) (sViewPortHeight / displayMetrics.density);
    }

    /**
     * 判断是否为电视设备
     *
     * @param context
     * @return
     */
    public static boolean isTelevisionDevice(Context context) {
        UiModeManager uiModeManager =
                (UiModeManager) context.getSystemService(Context.UI_MODE_SERVICE);
        if (uiModeManager == null) {
            return false;
        }
        return uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION;
    }

    public static int getNavigationBarHeight(Context context) {
        int resourceId =
                context.getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return context.getResources().getDimensionPixelSize(resourceId);
        }

        return 0;
    }

    public static boolean hasNavigationBar(Context context) {
        if (sWindowInsets == null) {
            return false;
        }
        int navigationHeight = getNavigationBarHeight(context);
        return isLandscapeMode(context)
                ? sWindowInsets.getSystemWindowInsetRight() == navigationHeight
                : sWindowInsets.getSystemWindowInsetBottom() == navigationHeight;
    }

    public static WindowInsets getWindowInsets() {
        return sWindowInsets;
    }

    public static void setWindowInsets(WindowInsets windowInsets) {
        sWindowInsets = windowInsets;
    }
}

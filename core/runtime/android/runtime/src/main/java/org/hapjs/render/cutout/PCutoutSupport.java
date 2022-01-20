/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.cutout;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Rect;
import android.os.Build;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.DisplayCutout;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import java.lang.reflect.Field;
import java.util.List;
import org.hapjs.common.utils.DisplayUtil;
import org.hapjs.runtime.BuildConfig;

class PCutoutSupport implements ICutoutSupport {

    private static final String TAG = "Cutout";
    private static final boolean DEBUG = BuildConfig.DEBUG;

    @RequiresApi(Build.VERSION_CODES.P)
    @Override
    public boolean isCutoutScreen(@NonNull Context context, @NonNull Window window) {
        List<Rect> cutoutDisplay = getCutoutDisplay(context, window);
        return cutoutDisplay != null && cutoutDisplay.size() > 0;
    }

    @RequiresApi(Build.VERSION_CODES.P)
    @Nullable
    @Override
    public List<Rect> getCutoutDisplay(@NonNull Context context, @NonNull Window window) {
        List<Rect> cutoutDisplay = getCutoutDisplayInternal(context, window);

        if (cutoutDisplay != null && window.getContext() instanceof Activity) {
            adjustPortraitCoordDisplayCutout((Activity) window.getContext(), cutoutDisplay);
        }
        return cutoutDisplay;
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private List<Rect> getCutoutDisplayInternal(@NonNull Context context, @NonNull Window window) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return getCutoutDisplayQ(context);
        }

        try {
            // WindowInsets#getDisplayCutout maybe return null.
            Display display = getDisplay(context);
            if (display != null) {
                Field mDisplayInfoField = Display.class.getDeclaredField("mDisplayInfo");
                mDisplayInfoField.setAccessible(true);
                Object displayInfo = mDisplayInfoField.get(display);
                if (displayInfo != null) {
                    Field displayCutoutField =
                            displayInfo.getClass().getDeclaredField("displayCutout");
                    displayCutoutField.setAccessible(true);
                    DisplayCutout displayCutout =
                            (DisplayCutout) displayCutoutField.get(displayInfo);
                    if (displayCutout != null) {
                        return displayCutout.getBoundingRects();
                    }
                }
            }
        } catch (Exception e) {
            // ignore
        }

        return getCutoutDisplayDefault(context, window);
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private List<Rect> getCutoutDisplayQ(Context context) {
        WindowManager windowManager =
                (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (windowManager == null) {
            return null;
        }
        Display display = getDisplay(context);
        if (display == null) {
            return null;
        }

        DisplayCutout cutout = display.getCutout();
        if (cutout == null) {
            return null;
        }
        return cutout.getBoundingRects();
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private List<Rect> getCutoutDisplayDefault(Context context, Window window) {
        View decorView = window.getDecorView();
        WindowInsets rootWindowInsets = decorView.getRootWindowInsets();
        if (rootWindowInsets == null) {
            return null;
        }

        DisplayCutout displayCutout = rootWindowInsets.getDisplayCutout();
        if (displayCutout == null) {
            return null;
        }
        return displayCutout.getBoundingRects();
    }

    private Display getDisplay(Context context) {
        WindowManager windowManager =
                (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (windowManager == null) {
            return null;
        }
        return windowManager.getDefaultDisplay();
    }

    /**
     * 将cutout转换为竖屏坐标系。在横屏下获取的cutout默认以横屏为坐标参考系。 cutout的右边界和下边界的值为到屏幕左边和屏幕上边的距离，需要转换为到屏幕右边和屏幕下边的距离
     *
     * @param activity
     * @param displayCutout
     */
    private void adjustPortraitCoordDisplayCutout(
            @NonNull Activity activity, List<Rect> displayCutout) {
        if (displayCutout == null || displayCutout.size() == 0) {
            return;
        }

        DisplayMetrics displayMetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getRealMetrics(displayMetrics);
        int screenWidth;
        int screenHeight;
        if (DisplayUtil.isTelevisionDevice(activity.getApplicationContext())) {
            screenWidth = displayMetrics.widthPixels;
            screenHeight = displayMetrics.heightPixels;
        } else {
            screenWidth = Math.min(displayMetrics.widthPixels, displayMetrics.heightPixels);
            screenHeight = Math.max(displayMetrics.widthPixels, displayMetrics.heightPixels);
        }

        if (DEBUG) {
            Log.d(TAG, "screenWidth:" + screenWidth + "  screenHeight:" + screenHeight);
        }

        int orientation = activity.getRequestedOrientation();
        if (DEBUG) {
            Log.d(TAG, "activityOrientation:" + orientation);
        }
        // 横屏下cutout以横屏为坐标系，需要调整为以竖屏为坐标系
        if (orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            for (Rect rect : displayCutout) {
                int left = rect.left;
                int top = rect.top;
                int right = screenHeight - rect.right;
                int bottom = screenWidth - rect.bottom;
                rect.set(bottom, left, top, right);
                if (DEBUG) {
                    Log.d(TAG, "CutoutInfo:" + rect);
                }
            }
        } else {
            for (Rect rect : displayCutout) {
                rect.set(rect.left, rect.top, screenWidth - rect.right, screenHeight - rect.bottom);
                if (DEBUG) {
                    Log.d(TAG, "CutoutInfo:" + rect);
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    @Override
    public int getCutoutHeight(@NonNull Context context, @NonNull Window window) {
        View decorView = window.getDecorView();
        WindowInsets rootWindowInsets = decorView.getRootWindowInsets();
        if (rootWindowInsets == null) {
            return 0;
        }
        DisplayCutout displayCutout = rootWindowInsets.getDisplayCutout();
        if (displayCutout == null) {
            return 0;
        }
        return displayCutout.getSafeInsetTop();
    }

    @Override
    @RequiresApi(Build.VERSION_CODES.P)
    public void fit(
            Context context, Window window, View cutoutView, boolean portrait, String fitCutout) {
        if (!isCutoutScreen(context, window)) {
            return;
        }
        WindowManager.LayoutParams attributes = window.getAttributes();
        if (!isValidFitcutout(fitCutout)
                || (portrait && !fitCutout.toLowerCase().contains(FIT_CUTOUT_PORTRAIT))
                || (!portrait && !fitCutout.toLowerCase().contains(FIT_CUTOUT_LANDSCAPE))) {

            attributes.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER;
        } else {
            attributes.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        window.setAttributes(attributes);
    }

    private boolean isValidFitcutout(String fitCutout) {
        return !TextUtils.isEmpty(fitCutout)
                && (fitCutout.toLowerCase().contains(FIT_CUTOUT_PORTRAIT)
                || fitCutout.toLowerCase().contains(FIT_CUTOUT_LANDSCAPE));
    }
}

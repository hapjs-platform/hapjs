/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.utils;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.view.WindowManager;
import java.util.ArrayList;
import java.util.List;

public class BrightnessUtils {

    private static final String TAG = "BrightnessUtils";
    private static final Uri BRIGHTNESS_URI =
            Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS);
    private static final Uri BRIGHTNESS_ADJ_URI =
            Settings.System.getUriFor("screen_auto_brightness_adj");
    private static List<BrightnessObserver> mObervers;
    private static ContentObserver mContentObserver;

    public static void registerOberver(Context context, BrightnessObserver observer) {
        if (mObervers == null) {
            mObervers = new ArrayList<>();
        }
        if (mContentObserver == null) {
            mContentObserver =
                    new ContentObserver(new Handler()) {
                        @Override
                        public void onChange(boolean selfChange) {
                            super.onChange(selfChange);
                            for (int i = 0; i < mObervers.size(); i++) {
                                mObervers.get(i).onChange(selfChange);
                            }
                        }
                    };
        }
        if (mObervers.size() == 0) {
            final ContentResolver cr = context.getContentResolver();
            cr.unregisterContentObserver(mContentObserver);
            cr.registerContentObserver(BRIGHTNESS_URI, false, mContentObserver);
            cr.registerContentObserver(BRIGHTNESS_ADJ_URI, false, mContentObserver);
        }
        mObervers.add(observer);
    }

    public static void unregisterOberver(Context context, BrightnessObserver observer) {
        if (observer == null || mObervers == null) {
            return;
        }
        if (mObervers.remove(observer) && mObervers.size() == 0 && mContentObserver != null) {
            context.getContentResolver().unregisterContentObserver(mContentObserver);
        }
    }

    public static void unregisterAllObervers(Context context) {
        if (mObervers == null) {
            return;
        }
        mObervers.clear();
        if (mContentObserver != null) {
            context.getContentResolver().unregisterContentObserver(mContentObserver);
        }
    }

    public static void resetWindowBrightness(Activity activity) {
        setWindowBrightness(activity, WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE);
    }

    public static void setWindowBrightness(Activity activity, float value) {
        WindowManager.LayoutParams lp = activity.getWindow().getAttributes();
        if (!FloatUtil.floatsEqual(value, lp.screenBrightness)) {
            lp.screenBrightness = value;
            activity.getWindow().setAttributes(lp);
        }
    }

    public static float getWindowBrightness(Activity activity) {
        WindowManager.LayoutParams lp = activity.getWindow().getAttributes();
        return lp.screenBrightness;
    }

    public static void setKeepScreenOn(Activity activity, boolean keepScreenOn) {
        if (keepScreenOn) {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    public interface BrightnessObserver {
        void onChange(boolean selfChange);
    }
}

/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.runtime;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.view.View;
import androidx.appcompat.app.AppCompatDelegate;
import org.hapjs.render.DecorLayout;
import org.hapjs.render.vdom.VDocument;
import org.hapjs.system.SysOpProvider;

public class DarkThemeUtil {

    public static final int THEME_NIGHT_NO = 0;
    public static final int THEME_NIGHT_YES = 1;

    public static boolean isDarkMode() {
        return isDarkMode(Runtime.getInstance().getContext());
    }

    public static boolean isDarkMode(Context context) {
        SysOpProvider sysOpProvider = ProviderManager.getDefault().getProvider(SysOpProvider.NAME);
        if (sysOpProvider != null && !sysOpProvider.allowNightModeInAndroidVersion()) {
            return false;
        }
        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
            return true;
        }

        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_NO) {
            return false;
        }
        int uiMode = context.getResources().getConfiguration().uiMode;
        return (uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    // defined by manifest.json
    // dark-no--0,dark_yes--1,default--auto,
    public static int convertThemeMode(int themeMode) {
        if (themeMode == THEME_NIGHT_NO) {
            return AppCompatDelegate.MODE_NIGHT_NO;
        }

        if (themeMode == THEME_NIGHT_YES) {
            return AppCompatDelegate.MODE_NIGHT_YES;
        }
        return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
    }

    /**
     * mark dark/normal mode or not just set theme mode related with dark
     * mode(dark-night-yes/dark-night-no)
     *
     * @param themeMode defined by mainifest.json
     * @return
     */
    public static boolean isDarkModeMarked(int themeMode) {
        if (themeMode == THEME_NIGHT_NO) {
            return true;
        }

        if (themeMode == THEME_NIGHT_YES) {
            return true;
        }
        return false;
    }

    public static boolean needChangeDefaultNightMode(int themeMode) {
        if (AppCompatDelegate.getDefaultNightMode() != convertThemeMode(themeMode)
                && isDarkModeMarked(themeMode)) {
            return true;
        }
        return false;
    }

    // defined by manifest.json
    // dark-no--0,dark_yes--1,default--auto,
    public static boolean needRecreate(int themeMode) {
        Context context = Runtime.getInstance().getContext();
        int uiMode = context.getResources().getConfiguration().uiMode;
        // define night no and current night no ,no need to recreate
        if (themeMode == THEME_NIGHT_NO) {
            if ((uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_NO) {
                return false;
            }
        }

        // define night and current night ,no need to recreate
        if (themeMode == THEME_NIGHT_YES) {
            if ((uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES) {
                return false;
            }
        }
        return true;
    }

    /**
     * in some case,when page has webview component,dialog may show abnormally so invalide it
     */
    public static void enableForceDark(View view, boolean enable) {
        if (view == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return;
        }
        view.setForceDarkAllowed(enable);
    }

    /**
     * in some case,when page has webview component && Configuration.UI_MODE_NIGHT_NO dialog may show
     * abnormally so disable force dark in such case
     */
    public static void disableForceDark(Dialog dialog) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return;
        }
        // 避免Android 6.0及以前版本，AlertDialog在操作Window或setContentView出现requestFeature() must be called
        // before adding content崩溃问题
        View decorView = dialog.getWindow() == null ? null : dialog.getWindow().getDecorView();
        if (decorView != null
                && AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_NO) {
            decorView.setForceDarkAllowed(false);
        }
    }

    public static boolean isDarkModeChanged(Context context, VDocument vdocument) {
        if (vdocument == null
                || vdocument.getComponent() == null
                || vdocument.getComponent().getDecorLayout() == null) {
            return false;
        }
        DecorLayout decorLayout = vdocument.getComponent().getDecorLayout();
        return decorLayout.isDarkMode() != isDarkMode(context);
    }
}

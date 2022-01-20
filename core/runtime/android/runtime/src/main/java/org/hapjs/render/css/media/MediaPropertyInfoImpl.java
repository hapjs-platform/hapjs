/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.css.media;

import android.content.Context;
import android.content.res.Configuration;
import org.hapjs.card.api.CardConfig;
import org.hapjs.card.sdk.utils.CardConfigUtils;
import org.hapjs.common.compat.BuildPlatform;
import org.hapjs.common.utils.DisplayUtil;
import org.hapjs.model.AppInfo;
import org.hapjs.runtime.DarkThemeUtil;
import org.hapjs.runtime.HapEngine;

public class MediaPropertyInfoImpl implements MediaPropertyInfo {

    private static final String TAG = "MediaPropertyInfoImpl";
    private Context mContext;

    public MediaPropertyInfoImpl() {
        mContext = DisplayUtil.getHapEngine().getContext();
    }

    @Override
    public int getScreenHeight() {
        return DisplayUtil.getScreenHeightByDp();
    }

    @Override
    public int getScreenWidth() {
        return DisplayUtil.getScreenWidthByDP();
    }

    @Override
    public int getViewPortHeight() {
        return DisplayUtil.getViewPortHeightByDp();
    }

    @Override
    public int getViewPortWidth() {
        return DisplayUtil.getViewPortWidthByDp();
    }

    @Override
    public int getResolution() {
        return DisplayUtil.getDestinyDpi();
    }

    @Override
    public int getOrientation() {
        Configuration mConfiguration = mContext.getResources().getConfiguration(); // 获取设置的配置信息
        if (mConfiguration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            return MediaProperty.ORIENTATION_PORTRAIT;
        } else if (mConfiguration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            return MediaProperty.ORIENTATION_LANDSCAPE;
        }
        return BuildPlatform.isTV()
                ? Configuration.ORIENTATION_LANDSCAPE
                : Configuration.ORIENTATION_PORTRAIT;
    }

    @Override
    public int getPrefersColorScheme() {
        if (DisplayUtil.getHapEngine().getMode() == HapEngine.Mode.CARD) {
            if (CardConfigUtils.get(CardConfig.KEY_DARK_MODE) instanceof Integer
                    && (Integer) CardConfigUtils.get(CardConfig.KEY_DARK_MODE)
                    == DarkThemeUtil.THEME_NIGHT_YES) {
                return DarkThemeUtil.THEME_NIGHT_YES;
            }
        } else {
            int themeMode = 0;
            AppInfo appInfo = DisplayUtil.getHapEngine().getApplicationContext().getAppInfo();
            if (appInfo != null && appInfo.getDisplayInfo() != null) {
                themeMode = appInfo.getDisplayInfo().getThemeMode();
            }
            switch (themeMode) {
                case DarkThemeUtil.THEME_NIGHT_NO:
                case DarkThemeUtil.THEME_NIGHT_YES:
                    return themeMode;
                default:
                    if (DarkThemeUtil.isDarkMode()) {
                        return DarkThemeUtil.THEME_NIGHT_YES;
                    } else {
                        return DarkThemeUtil.THEME_NIGHT_NO;
                    }
            }
        }
        return 0;
    }
}

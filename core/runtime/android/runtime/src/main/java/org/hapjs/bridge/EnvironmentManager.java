/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge;

import android.content.Context;
import android.os.Build;
import android.util.DisplayMetrics;
import java.util.Locale;
import org.hapjs.model.AppInfo;
import org.hapjs.runtime.BuildConfig;
import org.hapjs.runtime.HapEngine;

public class EnvironmentManager {

    public static String buildRegisterJavascript(Context context, AppInfo appInfo) {
        return "var Env = {" + getRegisterJavaScript(context, appInfo) + "};";
    }

    public static String getRegisterJavaScript(Context context, AppInfo appInfo) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        String logLevel = "log";
        if (appInfo.getConfigInfo() != null) {
            logLevel = appInfo.getConfigInfo().getString("logLevel");
        }
        return "platform: 'android',"
                + "osVersion: '"
                + Build.VERSION.RELEASE
                + "',"
                + "osVersionInt: "
                + Build.VERSION.SDK_INT
                + ","
                + "platformVersionName: '"
                + BuildConfig.platformVersionName
                + "',"
                + "platformVersionCode: "
                + BuildConfig.platformVersion
                + ","
                + "appVersionName: '"
                + appInfo.getVersionName()
                + "',"
                + "appVersionCode: "
                + appInfo.getVersionCode()
                + ","
                + "appName: '"
                + appInfo.getName()
                + "',"
                + "logLevel: '"
                + logLevel
                + "',"
                + "density: "
                + dm.density
                + ", "
                + "densityDpi: "
                + dm.densityDpi
                + ", "
                + "deviceWidth: "
                + dm.widthPixels
                + ", "
                + "deviceHeight: "
                + dm.heightPixels
                + ", "
                + "engine: '"
                + HapEngine.getInstance(appInfo.getPackage())
                .getMode()
                .name()
                .toLowerCase(Locale.getDefault())
                + "', ";
    }
}

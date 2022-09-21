/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.debugger.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.hapjs.debugger.app.impl.BuildConfig;

public class PreferenceUtils {
    private static final String KEY_SERVER = "server";
    private static final String KEY_RUNTIME_MODE = "runtime_mode";
    private static final String DEBUG_RPK_PATH = "debug_rpk_path";
    private static final String KEY_DEBUG_PACKAGE = "debug_package";
    private static final String KEY_PLATFORM_PACKAGE = "platform_package";
    private static final String KEY_RELOAD_PACKAGE = "reload_package";
    private static final String KEY_USE_ADB = "use_adb";
    private static final String KEY_USE_ANALYZER = "use_analyzer";
    private static final String KEY_WAIT_DEVTOOLS = "wait_devtools";
    private static final String KEY_CARD_HOST_PLATFORM = "debug_card_host_platform";
    private static final String KEY_DEBUG_CARD_PATH = "debug_card_path";
    private static final String KEY_DEBUG_CARD_PACKAGE = "debug_card_package";
    private static final String KEY_LAUNCH_PARAMS = "launch_params";
    private static final String KEY_WEB_DEBUG_ENABLED = "web_debug_enabled";
    private static final String KEY_SERIAL_NUMBER = "serial_number";
    //是否为通用扫描预览
    private static final String KEY_UNIVERSAL_SCAN = "universal_scan";
    private static final String KEY_SHOW_DEBUG_HINT_VERSION = "show_debug_hint_version";

    public static String getServer(Context context) {
        return getPreference(context).getString(KEY_SERVER, "");
    }

    public static void setServer(Context context, String url) {
        getPreference(context).edit().putString(KEY_SERVER, url).apply();
    }

    public static String getDebugPackage(Context context) {
        return getPreference(context).getString(KEY_DEBUG_PACKAGE, "");
    }

    public static void setDebugPackage(Context context, String pkg) {
        getPreference(context).edit().putString(KEY_DEBUG_PACKAGE, pkg).apply();
    }

    public static String getPlatformPackage(Context context) {
        return getPreference(context).getString(KEY_PLATFORM_PACKAGE, "");
    }

    public static void setPlatformPackage(Context context, String platformPkg) {
        getPreference(context).edit().putString(KEY_PLATFORM_PACKAGE, platformPkg).apply();
    }

    public static boolean shouldReloadPackage(Context context) {
        return getPreference(context).getBoolean(KEY_RELOAD_PACKAGE, false);
    }

    public static void setReloadPackage(Context context, boolean shouldReload) {
        getPreference(context).edit().putBoolean(KEY_RELOAD_PACKAGE, shouldReload).apply();
    }

    public static boolean isUseADB(Context context) {
        return getPreference(context).getBoolean(KEY_USE_ADB, false);
    }

    public static void setUseADB(Context context, boolean useAdbDebug) {
        getPreference(context).edit().putBoolean(KEY_USE_ADB, useAdbDebug).apply();
    }

    public static boolean isUseAnalyzer(Context context) {
        return getPreference(context).getBoolean(KEY_USE_ANALYZER, true);
    }

    public static void setUseAnalyzer(Context context, boolean useAnalyzer) {
        getPreference(context).edit().putBoolean(KEY_USE_ANALYZER, useAnalyzer).apply();
    }

    public static boolean isWaitDevTools(Context context) {
        return getPreference(context).getBoolean(KEY_WAIT_DEVTOOLS, false);
    }

    public static void setWaitDevTools(Context context, boolean waitDevTools) {
        getPreference(context).edit().putBoolean(KEY_WAIT_DEVTOOLS, waitDevTools).apply();
    }

    public static int getRuntimeMode(Context context) {
        return getPreference(context).getInt(KEY_RUNTIME_MODE, 0);
    }

    public static void setRuntimeMode(Context context, int mode) {
        getPreference(context).edit().putInt(KEY_RUNTIME_MODE, mode).apply();
    }

    public static String getDebugRpkPath(Context context) {
        return getPreference(context).getString(DEBUG_RPK_PATH, "");
    }

    public static void setDebugRpkPath(Context context, String path) {
        getPreference(context).edit().putString(DEBUG_RPK_PATH, path).apply();
    }

    public static String getCardHostPlatform(Context context) {
        return getPreference(context).getString(KEY_CARD_HOST_PLATFORM, "");
    }

    public static void setCardHostPlatform(Context context, String platform) {
        getPreference(context).edit().putString(KEY_CARD_HOST_PLATFORM, platform).apply();
    }

    public static String getDebugCardPackage(Context context) {
        return getPreference(context).getString(KEY_DEBUG_CARD_PACKAGE, "");
    }

    public static void setDebugCardPackage(Context context, String pkg) {
        getPreference(context).edit().putString(KEY_DEBUG_CARD_PACKAGE, pkg).apply();
    }

    public static String getDebugCardPath(Context context) {
        return getPreference(context).getString(KEY_DEBUG_CARD_PATH, "");
    }

    public static void setDebugCardPath(Context context, String path) {
        getPreference(context).edit().putString(KEY_DEBUG_CARD_PATH, path).apply();
    }

    public static void setLaunchParams(Context context, String params) {
        getPreference(context).edit().putString(KEY_LAUNCH_PARAMS, params).apply();
    }

    public static String getLaunchParams(Context context) {
        return getPreference(context).getString(KEY_LAUNCH_PARAMS, "");
    }

    public static boolean isWebDebugEnabled(Context context) {
        return getPreference(context).getBoolean(KEY_WEB_DEBUG_ENABLED, false);
    }

    public static void setWebDebugEnabled(Context context, boolean enabled) {
        getPreference(context).edit().putBoolean(KEY_WEB_DEBUG_ENABLED, enabled).apply();
    }

    public static String getSerialNumber(Context context) {
        return getPreference(context).getString(KEY_SERIAL_NUMBER, "");
    }

    public static void setSerialNumber(Context context, String serialNumber) {
        getPreference(context).edit().putString(KEY_SERIAL_NUMBER, serialNumber).apply();
    }

    private static SharedPreferences getPreference(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static void setUniversalScan(Context context, boolean isUniversalScan) {
        getPreference(context).edit().putBoolean(KEY_UNIVERSAL_SCAN, isUniversalScan).apply();
    }

    public static boolean isUniversalScan(Context context) {
        return getPreference(context).getBoolean(KEY_UNIVERSAL_SCAN, false);
    }

    public static void setHasShownDebugHint(Context context) {
        getPreference(context).edit().putLong(KEY_SHOW_DEBUG_HINT_VERSION, BuildConfig.VERSION_CODE).apply();
    }

    public static boolean hasShownDebugHint(Context context) {
        return getPreference(context).getLong(KEY_SHOW_DEBUG_HINT_VERSION, 0) == BuildConfig.VERSION_CODE;
    }
}

/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.net;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.WebSettings;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import org.hapjs.bridge.provider.SystemSettings;
import org.hapjs.common.executors.Executors;
import org.hapjs.logging.Source;
import org.hapjs.runtime.Runtime;

public class UserAgentHelper {
    private static final String TAG = "UserAgentHelper";

    private static final String PLATFORM_USER_AGENT_FORMAT = " hap/%s/%s %s/%s";
    private static final String APPLICATION_USER_AGENT_FORMAT = " %s/%s (%s)";
    private static final String KEY_WEBKIT_USER_AGENT_VALUE = "Webkit.UserAgent.Value";
    private static final String KEY_WEBKIT_USER_AGENT_EXPIRES_IN = "Webkit.UserAgent.ExpiresIn";
    private static final String FAKE_PC_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.77 Safari/537.36";
    private static final long WEBKIT_USER_AGENT_INTERVAL = 7 * 24 * 60 * 60 * 1000L;
    private static final String CHROME_WEBVIEW_PACKAGE = "com.google.android.webview";
    private static final String USER_AGENT_TEMPLATE =
            "Mozilla/5.0 (Linux; Android %s; %s Build/%s; wv) AppleWebKit/537.36 (KHTML, like Gecko)"
                    + " Version/4.0 Chrome/%s Mobile Safari/537.36";
    private static final String FAKE_PC_CHROME_VERSION = "70.0.3538.77";

    private static String sFullHttpUserAgent;
    private static String sFullWebkitUserAgent;
    private static String sApplicationUserAgent;
    private static String sHttpUserAgentSegment;
    private static String sWebkitUserAgentSegment;
    private static String sPlatformUserAgentSegment;
    private static String sAppPackageName;
    private static String sAppVersionName;

    private static ConcurrentHashMap<String, AppInfo> sAppInfo = new ConcurrentHashMap<>();

    public static void preLoad() {
        Executors.io()
                .execute(
                        () -> {
                            // Webkit.UserAgent 获取比较耗时，进行预加载并定时更新
                            String webkitUserAgent = getWebkitUserAgentSegmentFromSettings();
                            if (webkitUserAgent == null) {
                                webkitUserAgent = getWebkitUserAgentSegmentFromSystem();
                                SystemSettings.getInstance()
                                        .putString(KEY_WEBKIT_USER_AGENT_VALUE, webkitUserAgent);
                                long newExpiresIn =
                                        System.currentTimeMillis() + WEBKIT_USER_AGENT_INTERVAL;
                                SystemSettings.getInstance()
                                        .putLong(KEY_WEBKIT_USER_AGENT_EXPIRES_IN, newExpiresIn);
                            }
                            sWebkitUserAgentSegment = webkitUserAgent;
                        });
    }

    public static void setAppInfo(String packageName, String versionName) {
        if (versionName == null || versionName.isEmpty()) {
            versionName = "Unknown";
        }
        if (!TextUtils.equals(sAppPackageName, packageName)) {
            sAppPackageName = packageName;
            clearAgentCache();
        }
        if (!TextUtils.equals(sAppVersionName, versionName)) {
            sAppVersionName = versionName;
            clearAgentCache();
        }
        AppInfo appInfo = sAppInfo.get(packageName);
        if (appInfo == null || !TextUtils.equals(appInfo.mAppVersionName, versionName)) {
            appInfo = new AppInfo(packageName, versionName);
            sAppInfo.put(packageName, appInfo);
        }
    }

    private static void clearAgentCache() {
        sFullHttpUserAgent = null;
        sFullWebkitUserAgent = null;
        sApplicationUserAgent = null;
    }

    private static AppInfo getAppInfo(String pkg) {
        return sAppInfo.get(pkg);
    }

    public static String getFullHttpUserAgent() {
        if (TextUtils.isEmpty(sFullHttpUserAgent)) {
            String userAgent = getHttpUserAgentSegment() + getApplicationUserAgent();
            sFullHttpUserAgent = checkUserAgent(userAgent);
        }
        return sFullHttpUserAgent;
    }

    public static String getFullHttpUserAgent(String pkg) {
        if (!TextUtils.isEmpty(pkg)) {
            AppInfo appInfo = getAppInfo(pkg);
            if (null != appInfo) {
                return appInfo.getFullHttpUserAgent(pkg);
            }
        }
        return getFullHttpUserAgent();
    }

    public static String getFullWebkitUserAgent() {
        if (TextUtils.isEmpty(sFullWebkitUserAgent)) {
            String userAgent = getWebkitUserAgentSegment() + getApplicationUserAgent();
            sFullWebkitUserAgent = checkUserAgent(userAgent);
        }
        return sFullWebkitUserAgent;
    }

    public static String getFullWebkitUserAgent(String pkg) {
        if (!TextUtils.isEmpty(pkg)) {
            AppInfo appInfo = getAppInfo(pkg);
            if (null != getAppInfo(pkg)) {
                return appInfo.getFullWebkitUserAgent(pkg);
            }
        }
        return getFullWebkitUserAgent();
    }

    public static String getFakePCUserAgent() {
        return FAKE_PC_USER_AGENT;
    }

    /**
     * 检查UserAgent中非法字符
     *
     * @see okhttp3.Headers.Builder#checkNameAndValue(String, String)
     */
    private static String checkUserAgent(String value) {
        StringBuilder sb = new StringBuilder();
        if (value != null) {
            for (int i = 0, length = value.length(); i < length; i++) {
                char c = value.charAt(i);
                if ((c > '\u001f' || c == '\t') && c < '\u007f') {
                    sb.append(c);
                }
            }
        }
        return sb.toString();
    }

    private static String getApplicationUserAgent() {
        if (sApplicationUserAgent == null) {
            sApplicationUserAgent =
                    getPlatformUserAgentSegment() + getApplicationUserAgentSegment(sAppPackageName);
        }
        return sApplicationUserAgent;
    }

    public static String getWebkitUserAgentSegment() {
        if (sWebkitUserAgentSegment == null) {
            sWebkitUserAgentSegment = getWebkitUserAgentSegmentFromSettings();
            if (sWebkitUserAgentSegment == null) {
                sWebkitUserAgentSegment = getWebkitUserAgentSegmentFromSystem();
            }
        }
        return sWebkitUserAgentSegment;
    }

    private static String getWebkitUserAgentSegmentFromSettings() {
        String userAgent =
                SystemSettings.getInstance().getString(KEY_WEBKIT_USER_AGENT_VALUE, null);
        long expiresIn = SystemSettings.getInstance().getLong(KEY_WEBKIT_USER_AGENT_EXPIRES_IN, 0L);
        if (!TextUtils.isEmpty(userAgent) && expiresIn > System.currentTimeMillis()) {
            return userAgent;
        }
        return null;
    }

    private static String getWebkitUserAgentSegmentFromSystem() {
        Context context = Runtime.getInstance().getContext();
        PackageManager pm = context.getPackageManager();
        PackageInfo pi = null;
        try {
            pi = pm.getPackageInfo(CHROME_WEBVIEW_PACKAGE, 0);
        } catch (PackageManager.NameNotFoundException e) {
            // ignore
        }

        String userAgent = null;
        if (pi == null) {
            Log.i(TAG, "pi is null,try to get default user agent");
            try {
                userAgent = WebSettings.getDefaultUserAgent(context);
            } catch (Exception e) {
                Log.e(TAG, "Fail to get webkit user agent", e);
                userAgent = System.getProperty("http.agent");
            }
        }
        if (TextUtils.isEmpty(userAgent)) {
            String release = Build.VERSION.RELEASE;
            String model = Build.MODEL;
            String id = Build.ID;
            String chromeVersion = pi != null ? pi.versionName : FAKE_PC_CHROME_VERSION;
            userAgent = String.format(USER_AGENT_TEMPLATE, release, model, id, chromeVersion);
        }
        return userAgent;
    }

    private static String getHttpUserAgentSegment() {
        if (TextUtils.isEmpty(sHttpUserAgentSegment)) {
            sHttpUserAgentSegment = System.getProperty("http.agent");
        }
        return sHttpUserAgentSegment;
    }

    private static String getPlatformUserAgentSegment() {
        if (sPlatformUserAgentSegment == null) {
            sPlatformUserAgentSegment =
                    String.format(
                            Locale.US,
                            PLATFORM_USER_AGENT_FORMAT,
                            org.hapjs.runtime.BuildConfig.platformVersionName,
                            Runtime.getInstance().getVendor(),
                            Runtime.getInstance().getContext().getPackageName(),
                            org.hapjs.runtime.BuildConfig.VERSION_NAME);
        }
        return sPlatformUserAgentSegment;
    }

    private static String getApplicationUserAgentSegment(String pkg) {
        if (!TextUtils.isEmpty(pkg)) {
            AppInfo appInfo = getAppInfo(pkg);
            if (null != appInfo) {
                return String.format(
                        Locale.US,
                        APPLICATION_USER_AGENT_FORMAT,
                        appInfo.mAppPackageName,
                        appInfo.mAppVersionName,
                        getSource());
            }
        }
        return "";
    }

    private static String getSource() {
        Source source = Source.currentSource();
        if (source == null) {
            return "Unknown";
        }
        return source.toSafeJson().toString();
    }

    private static class AppInfo {
        private String mAppPackageName;
        private String mAppVersionName;
        private String mFullHttpUserAgent;
        private String mFullWebkitUserAgent;
        private String mApplicationUserAgent;

        AppInfo(String appPkg, String appVsn) {
            mAppPackageName = appPkg;
            mAppVersionName = appVsn;
        }

        private String getFullHttpUserAgent(String appPkg) {
            if (TextUtils.isEmpty(mFullHttpUserAgent)) {
                String userAgent = getHttpUserAgentSegment() + getApplicationUserAgent(appPkg);
                mFullHttpUserAgent = checkUserAgent(userAgent);
            }
            return mFullHttpUserAgent;
        }

        private String getFullWebkitUserAgent(String appPkg) {
            if (TextUtils.isEmpty(mFullWebkitUserAgent)) {
                String userAgent = getWebkitUserAgentSegment() + getApplicationUserAgent(appPkg);
                mFullWebkitUserAgent = checkUserAgent(userAgent);
            }
            return mFullWebkitUserAgent;
        }

        private String getApplicationUserAgent(String pkg) {
            if (TextUtils.isEmpty(mApplicationUserAgent)) {
                mApplicationUserAgent =
                        getPlatformUserAgentSegment() + getApplicationUserAgentSegment(pkg);
            }
            return mApplicationUserAgent;
        }
    }
}

/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.hapjs.common.utils.PackageUtils;
import org.hapjs.logging.Source;
import org.hapjs.runtime.ProviderManager;
import org.hapjs.runtime.RuntimeActivity;
import org.hapjs.system.SysOpProvider;

public class SystemController {
    private static final String TAG = "SystemController";

    private static final String KEY_DISABLE_SHORTCUT_PROMPT = "__DSP__";
    private static final String KEY__DIRECT__BACK = "__DB__";
    private static final String KEY_MIN_APP_VERSION = "__MAV__";
    private boolean mDisableSystemPrompt;
    private boolean mDirectBack;
    private Map<String, Integer> mMinAppVersions = new ConcurrentHashMap<>();

    private SystemController() {
    }

    public static SystemController getInstance() {
        return Holder.INSTANCE;
    }

    public void config(Context context, Intent intent) {
        if (intent == null) {
            return;
        }
        mDisableSystemPrompt = isDisableSystemPromptBySystem(context, intent);
        mDirectBack = isDirectBackBySystem(context, intent);
        configMinAppVersionsBySystem(context, intent);
    }

    /**
     * 检查是否展示系统创建桌面快捷方式弹窗
     */
    private boolean isDisableSystemPromptBySystem(Context context, Intent intent) {
        boolean result = false;
        String uri = "";
        try {
            uri = intent.getStringExtra(RuntimeActivity.EXTRA_PATH);
            if (TextUtils.isEmpty(uri) || !uri.contains(KEY_DISABLE_SHORTCUT_PROMPT)) {
                return false;
            }
            Uri pathUri = Uri.parse(uri);
            // path 中是否指定了关闭弹窗
            result = pathUri.getBooleanQueryParameter(KEY_DISABLE_SHORTCUT_PROMPT, false);
        } catch (Exception e) {
            Log.e(TAG, "uri = " + uri, e);
        }
        if (result) {
            Source source = Source.fromIntent(intent);
            SysOpProvider provider = ProviderManager.getDefault().getProvider(SysOpProvider.NAME);
            if (provider != null) {
                return provider.isDisableShortcutPrompt(context, source);
            }
            return false;
        }

        return false;
    }

    private boolean isDirectBackBySystem(Context context, Intent intent) {
        if ((intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0) {
            return false;
        }
        boolean result = false;
        String path = "";
        try {
            path = intent.getStringExtra(RuntimeActivity.EXTRA_PATH);
            if (TextUtils.isEmpty(path) || !path.contains(KEY__DIRECT__BACK)) {
                return false;
            }
            Uri pathUri = Uri.parse(path);
            // path 中是否指定了直接退出
            result = pathUri.getBooleanQueryParameter(KEY__DIRECT__BACK, false);
        } catch (Exception e) {
            Log.e(TAG, "path = " + path, e);
        }
        if (result) {
            Source source = Source.fromIntent(intent);
            // 是否为系统 App
            if (source == null) {
                return false;
            }
            String sourcePackage = source.getPackageName();
            if (TextUtils.isEmpty(sourcePackage)) {
                return false;
            }
            return PackageUtils.isSystemAppOrSignature(context, sourcePackage);
        }
        return false;
    }

    private void configMinAppVersionsBySystem(Context context, Intent intent) {
        int version = 0;
        String path = "";
        String pkg = "";
        try {
            pkg = intent.getStringExtra(RuntimeActivity.EXTRA_APP);
            path = intent.getStringExtra(RuntimeActivity.EXTRA_PATH);

            removeMinAppVersion(pkg);
            if (TextUtils.isEmpty(pkg)
                    || TextUtils.isEmpty(path)
                    || !path.contains(KEY_MIN_APP_VERSION)) {
                return;
            }
            Uri pathUri = Uri.parse(path);
            // path 中是否指定了最小版本号
            String param = pathUri.getQueryParameter(KEY_MIN_APP_VERSION);
            version = param == null ? 0 : Integer.parseInt(param);
        } catch (Exception e) {
            Log.e(TAG, "path = " + path, e);
        }
        if (version != 0) {
            Source source = Source.fromIntent(intent);
            // 是否为系统 App
            String sourcePackage = source != null ? source.getPackageName() : "";
            boolean isSystem =
                    !TextUtils.isEmpty(sourcePackage)
                            && PackageUtils.isSystemAppOrSignature(context, sourcePackage);
            mMinAppVersions.put(pkg, isSystem ? version : 0);
        }
    }

    public void onUserLeaveHint(Context context) {
        mDirectBack = false;
    }

    public boolean isDisableSystemPrompt() {
        return mDisableSystemPrompt;
    }

    public boolean isDirectBack() {
        return mDirectBack;
    }

    public void setMinAppVersion(String pkg, int verison) {
        mMinAppVersions.put(pkg, verison);
    }

    public int getMinAppVersion(String pkg) {
        Integer version = mMinAppVersions.get(pkg);
        if (version != null) {
            return version;
        }
        return 0;
    }

    public void removeMinAppVersion(String pkg) {
        if (!TextUtils.isEmpty(pkg) && mMinAppVersions.containsKey(pkg)) {
            mMinAppVersions.remove(pkg);
        }
    }

    private static class Holder {
        static final SystemController INSTANCE = new SystemController();
    }
}

/*
 * Copyright (c) 2023-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hapjs.system.utils;

import android.content.ComponentName;
import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import org.hapjs.runtime.ProviderManager;
import org.hapjs.system.SysOpProvider;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class TalkBackUtils {
    private final static String TAG = "TalkBackUtils";
    /*
     * 获取当前开启的所有辅助功能服务
     */
    static int sTalkBackEnable = -1;
    static volatile boolean sIsForceCheck;
    static volatile boolean sIsChecking;
    static final int TALKBACK_ENABLE = 1;
    static final int TALKBACK_DISABLE = 0;
    static final int TALKBACK_DEFAULT = -1;
    static final char ENABLED_ACCESSIBILITY_SERVICES_SEPARATOR = ':';
    static final ComponentName TALKBACK_SERVICE_ENABLE = ComponentName.unflattenFromString("com.google.android.marvin.talkback/com.google.android.marvin.talkback.TalkBackService");
    final static TextUtils.SimpleStringSplitter sStringColonSplitter =
            new TextUtils.SimpleStringSplitter(ENABLED_ACCESSIBILITY_SERVICES_SEPARATOR);

    private static Set<ComponentName> getEnabledServicesFromSettings(Context context) {
        Set<ComponentName> enabledServices = null;
        if (null == context) {
            return enabledServices;
        }
        try {
            String enabledServicesSetting = Settings.Secure.getString(context.getContentResolver(), "enabled_accessibility_services");
            if (TextUtils.isEmpty(enabledServicesSetting)) {
                return Collections.emptySet();
            } else {
                enabledServices = new HashSet();
                TextUtils.SimpleStringSplitter colonSplitter = sStringColonSplitter;
                colonSplitter.setString(enabledServicesSetting);
                while (colonSplitter.hasNext()) {
                    String componentNameString = colonSplitter.next();
                    ComponentName enabledService = ComponentName.unflattenFromString(componentNameString);
                    if (enabledService != null) {
                        enabledServices.add(enabledService);
                    }
                }
                return enabledServices;
            }
        } catch (Exception e) {
            Log.e(TAG, "getEnabledServicesFromSettings error : " + e.getMessage());
        }
        return enabledServices;
    }

    public static void setIsForceCheck(boolean isForceCheck) {
        TalkBackUtils.sIsForceCheck = isForceCheck;
    }

    public static boolean isEnableTalkBack(Context context, boolean forceCheck) {
        if (sIsChecking) {
            return false;
        }
        if ((sIsForceCheck || forceCheck || sTalkBackEnable == TALKBACK_DEFAULT) && null != context) {
            sIsForceCheck = false;
            sIsChecking = true;
            boolean isEnable = false;
            SysOpProvider provider = ProviderManager.getDefault().getProvider(SysOpProvider.NAME);
            if (null != provider) {
                isEnable = provider.isEnableTalkBack(context);
            }
            if (isEnable) {
                Set<ComponentName> componentNames = getEnabledServicesFromSettings(context);
                if (null != componentNames && componentNames.contains(TALKBACK_SERVICE_ENABLE)) {
                    sTalkBackEnable = TALKBACK_ENABLE;
                    sIsChecking = false;
                    return true;
                }
            }
            sTalkBackEnable = TALKBACK_DISABLE;
            sIsChecking = false;
            return false;
        } else {
            return sTalkBackEnable == TALKBACK_ENABLE;
        }
    }
}

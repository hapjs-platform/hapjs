/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs;

import android.util.Log;
import java.util.HashMap;
import java.util.Map;
import org.hapjs.common.utils.LogUtils;
import org.hapjs.distribution.DistributionService;
import org.hapjs.logging.LogProvider;
import org.hapjs.logging.Source;
import org.hapjs.runtime.ProviderManager;

public class PlatformLogManager {
    public static final String CATEGORY_APP = "app";
    public static final String KEY_APP_LAUNCH = "launch";
    public static final String KEY_APP_PRE_LAUNCH = "preLaunch";
    public static final String KEY_APP_PRE_LOAD = "preLoad";
    public static final String KEY_APP_SCHEDULE_INSTALL = "scheduleInstall";
    public static final String KEY_APP_INSTALL_RESULT = "installResult";
    public static final String KEY_APP_SHORTCUT_PROMPT_SHOW = "shortcutPromptShow";
    public static final String KEY_APP_SHORTCUT_PROMPT_ACCEPT = "shortcutPromptAccept";
    public static final String KEY_APP_SHORTCUT_PROMPT_REJECT = "shortcutPromptReject";
    public static final String KEY_APP_SHORTCUT_CREATE_FAILED = "shortcutCreateFailed";
    public static final String KEY_APP_SHORTCUT_CREATE_SUCCESS = "shortcutCreateSuccess";
    public static final String KEY_APP_BACK_PRESSED = "backPressed";
    public static final String KEY_APP_LOADING_RESULT = "loadingResult";
    public static final String KEY_APP_ERROR = "error";
    public static final String KEY_APP_SHOW_SPLASH = "showSplash";
    public static final String KEY_APP_USAGE = "usage";
    public static final String KEY_APP_SMART_PROGRAM_SHORTCUT_UPDATE_RESULT =
            "smartProgramShortcutUpdateResult";
    public static final String KEY_APP_SMART_PROGRAM_SHORTCUT_CREATE_RESULT =
            "smartProgramShortcutCreateResult";
    public static final String PARAM_PROMPT_SOURCE = "sourceFrom";
    public static final String PARAM_LAUNCH_SOURCE = "sourceJson";
    public static final String PARAM_PROMPT_FORBIDDEN = "promptForbidden";
    public static final String PARAM_PATH = "path";
    public static final String PARAM_LOADING = "loading";
    public static final String PARAM_REASON = "reason";
    public static final String PARAM_STATUS = "status";
    public static final String PARAM_STACK_TRACE = "stackTrace";
    public static final String PARAM_CRASH_DESC = "crashDesc";
    public static final String PARAM_TAG = "tag";
    public static final String PARAM_ELAPSED_TIME = "elapsedTime";
    public static final String PARAM_SESSION_START = "sessionStart";
    public static final String PARAM_SESSION_END = "sessionEnd";
    public static final String PARAM_APP_STATUS = "appStatus";
    private static final String TAG = "PlatformLogMgr";
    private static final String PARAM_SOURCE_PKG = "sourcePkg";
    private static final String PARAM_SMART_PROGRAM_HOST = "smartProgramHost";
    private static final String PARAM_SMART_PROGRAM_HOST_SIGN = "smartProgramHostSign";
    private static final String PARAM_RESULT = "result";
    private LogProvider mProvider;

    private PlatformLogManager() {
        mProvider = (LogProvider) ProviderManager.getDefault().getProvider(LogProvider.NAME);
    }

    public static PlatformLogManager getDefault() {
        return Holder.INSTANCE;
    }

    public void logAppPreLaunch(String pkg, String path, int appStatus, Source source) {
        if (mProvider == null) {
            return;
        }
        Map<String, String> params = new HashMap<>();
        params.put(PARAM_PATH, path);
        params.put(PARAM_APP_STATUS, String.valueOf(appStatus));
        if (source != null) {
            params.put(PARAM_LAUNCH_SOURCE, source.toJson().toString());
        }
        mProvider.logCountEvent(pkg, CATEGORY_APP, KEY_APP_PRE_LAUNCH, params);
    }

    public void logAppPreLoad(String pkg) {
        if (mProvider == null) {
            return;
        }
        mProvider.logCountEvent(pkg, CATEGORY_APP, KEY_APP_PRE_LOAD);
    }

    public void logAppLaunch(String pkg, String path) {
        if (mProvider == null) {
            return;
        }
        Map<String, String> params = new HashMap<>();
        params.put(PARAM_PATH, path); // path 包含 pageName 和 parameter
        mProvider.logCountEvent(pkg, CATEGORY_APP, KEY_APP_LAUNCH, params);
    }

    public void logAppScheduleInstall(String pkg, Source source) {
        if (mProvider == null) {
            return;
        }
        Map<String, String> params = new HashMap<>();
        if (source != null) {
            params.put(PARAM_LAUNCH_SOURCE, source.toJson().toString());
        }
        mProvider.logCountEvent(pkg, CATEGORY_APP, KEY_APP_SCHEDULE_INSTALL, params);
    }

    public void logAppInstallResult(String pkg, DistributionService.InstallStatus status) {
        if (mProvider == null || status == null) {
            return;
        }
        Map<String, String> params = new HashMap<>();
        params.put(PARAM_STATUS, String.valueOf(status.getExternalStatusCode()));
        params.put(PARAM_REASON, String.valueOf(status.getErrorCode()));

        Throwable throwable = status.getInstallThrowable();
        if (throwable != null) {
            params.put(PARAM_STACK_TRACE, LogUtils.getStackTrace(throwable));
        }
        mProvider.logCountEvent(pkg, CATEGORY_APP, KEY_APP_INSTALL_RESULT, params);
    }

    // sourceFrom表示在哪种场景下弹框，比如退出时，比如通过网页发消息弹框
    public void logShortcutPromptShow(String pkg, String sourceFrom) {
        logShortcutPromptShow(pkg, sourceFrom, null);
    }

    public void logShortcutPromptShow(String pkg, String sourceFrom, Source source) {
        if (mProvider == null) {
            return;
        }
        Map<String, String> params = new HashMap<>();
        params.put(PARAM_PROMPT_SOURCE, sourceFrom); // 为了与公共字段 source 做区分
        if (source != null) {
            params.put(PARAM_LAUNCH_SOURCE, source.toJson().toString());
        }
        mProvider.logCountEvent(pkg, CATEGORY_APP, KEY_APP_SHORTCUT_PROMPT_SHOW, params);
    }

    public void logShortcutPromptAccept(String pkg, String sourceFrom) {
        logShortcutPromptAccept(pkg, sourceFrom, null);
    }

    public void logShortcutPromptAccept(String pkg, String sourceFrom, Source source) {
        if (mProvider == null) {
            return;
        }
        Map<String, String> params = new HashMap<>();
        params.put(PARAM_PROMPT_SOURCE, sourceFrom);
        if (source != null) {
            params.put(PARAM_LAUNCH_SOURCE, source.toJson().toString());
        }
        mProvider.logCountEvent(pkg, CATEGORY_APP, KEY_APP_SHORTCUT_PROMPT_ACCEPT, params);
    }

    public void logShortcutPromptReject(String pkg, boolean forbidden, String sourceFrom) {
        logShortcutPromptReject(pkg, forbidden, sourceFrom, null);
    }

    public void logShortcutPromptReject(
            String pkg, boolean forbidden, String sourceFrom, Source source) {
        if (mProvider == null) {
            return;
        }
        Map<String, String> params = new HashMap<>();
        params.put(PARAM_PROMPT_SOURCE, sourceFrom);
        if (source != null) {
            params.put(PARAM_LAUNCH_SOURCE, source.toJson().toString());
        }
        params.put(PARAM_PROMPT_FORBIDDEN, String.valueOf(forbidden));
        mProvider.logCountEvent(pkg, CATEGORY_APP, KEY_APP_SHORTCUT_PROMPT_REJECT, params);
    }

    public void logShortcutCreateFailed(String pkg, String sourceFrom, Source source) {
        if (mProvider == null) {
            return;
        }
        Map<String, String> params = new HashMap<>();
        params.put(PARAM_PROMPT_SOURCE, sourceFrom);
        if (source != null) {
            params.put(PARAM_LAUNCH_SOURCE, source.toJson().toString());
        }
        mProvider.logCountEvent(pkg, CATEGORY_APP, KEY_APP_SHORTCUT_CREATE_FAILED, params);
    }

    public void logShortcutCreateSuccess(String pkg, String sourceFrom, Source source) {
        if (mProvider == null) {
            return;
        }
        Map<String, String> params = new HashMap<>();
        params.put(PARAM_PROMPT_SOURCE, sourceFrom);
        if (source != null) {
            params.put(PARAM_LAUNCH_SOURCE, source.toJson().toString());
        }
        mProvider.logCountEvent(pkg, CATEGORY_APP, KEY_APP_SHORTCUT_CREATE_SUCCESS, params);
    }

    public void logBackPressed(String pkg, boolean loading) {
        if (mProvider == null) {
            return;
        }
        Map<String, String> params = new HashMap<>();
        params.put(PARAM_LOADING, String.valueOf(loading));
        mProvider.logCountEvent(pkg, CATEGORY_APP, KEY_APP_BACK_PRESSED, params);
    }

    public void logAppLoadingResult(String pkg, int statusCode, int reason) {
        if (mProvider == null) {
            return;
        }
        Map<String, String> params = new HashMap<>();
        params.put(PARAM_STATUS, String.valueOf(statusCode));
        params.put(PARAM_REASON, String.valueOf(reason));
        mProvider.logCountEvent(pkg, CATEGORY_APP, KEY_APP_LOADING_RESULT, params);
    }

    public void logAppError(String pkg, String tag, Throwable e, Source source) {
        if (mProvider == null) {
            return;
        }
        Map<String, String> params = new HashMap<>();
        params.put(PARAM_TAG, tag);
        params.put(PARAM_STACK_TRACE, LogUtils.getStackTrace(e));
        params.put(PARAM_CRASH_DESC, e.getMessage());
        if (source != null) {
            params.put(PARAM_LAUNCH_SOURCE, source.toJson().toString());
        }
        mProvider.logCountEvent(pkg, CATEGORY_APP, KEY_APP_ERROR, params);
    }

    public void logAppShowSplash(String pkg, long elapsedTime) {
        if (mProvider == null) {
            return;
        }
        Map<String, String> params = new HashMap<>();
        params.put(PARAM_ELAPSED_TIME, String.valueOf(elapsedTime));
        mProvider.logCountEvent(pkg, CATEGORY_APP, KEY_APP_SHOW_SPLASH, params);
    }

    public void logAppUsage(String pkg, long sessionStart) {
        if (mProvider == null) {
            return;
        }
        long sessionEnd = System.currentTimeMillis();
        if (sessionStart > sessionEnd) {
            Log.e(
                    TAG,
                    "recordAppUsage mismatch, sessionStart=" + sessionStart + ", sessionEnd="
                            + sessionEnd);
            return;
        }
        Map<String, String> params = new HashMap<>();
        params.put(PARAM_SESSION_START, String.valueOf(sessionStart));
        params.put(PARAM_SESSION_END, String.valueOf(sessionEnd));
        mProvider.logCountEvent(pkg, CATEGORY_APP, KEY_APP_USAGE, params);
    }

    public void logSmartProgramShortcutUpdateResult(
            String pkg,
            String sourcePkg,
            String smartProgramHost,
            String smartProgramHostSign,
            boolean result) {
        if (mProvider == null) {
            return;
        }
        Map<String, String> params = new HashMap<>();
        params.put(PARAM_SOURCE_PKG, sourcePkg);
        params.put(PARAM_SMART_PROGRAM_HOST, smartProgramHost);
        params.put(PARAM_SMART_PROGRAM_HOST_SIGN, smartProgramHostSign);
        params.put(PARAM_RESULT, String.valueOf(result));
        mProvider.logCountEvent(
                pkg, CATEGORY_APP, KEY_APP_SMART_PROGRAM_SHORTCUT_UPDATE_RESULT, params);
    }

    public void logSmartProgramShortcutCreateResult(
            String pkg,
            String sourcePkg,
            String smartProgramHost,
            String smartProgramHostSign,
            Source source,
            boolean result) {
        if (mProvider == null) {
            return;
        }
        Map<String, String> params = new HashMap<>();
        params.put(PARAM_SOURCE_PKG, sourcePkg);
        params.put(PARAM_SMART_PROGRAM_HOST, smartProgramHost);
        params.put(PARAM_SMART_PROGRAM_HOST_SIGN, smartProgramHostSign);
        if (source != null) {
            params.put(PARAM_LAUNCH_SOURCE, source.toJson().toString());
        }
        params.put(PARAM_RESULT, String.valueOf(result));
        mProvider.logCountEvent(
                pkg, CATEGORY_APP, KEY_APP_SMART_PROGRAM_SHORTCUT_CREATE_RESULT, params);
    }

    private static class Holder {
        static final PlatformLogManager INSTANCE = new PlatformLogManager();
    }
}

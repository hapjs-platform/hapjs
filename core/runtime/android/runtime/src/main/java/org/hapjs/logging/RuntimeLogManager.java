/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.logging;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.cache.CacheStorage;
import org.hapjs.common.executors.Executors;
import org.hapjs.common.json.JSONObject;
import org.hapjs.common.utils.LogUtils;
import org.hapjs.model.AppInfo;
import org.hapjs.render.Page;
import org.hapjs.runtime.HapEngine;
import org.hapjs.runtime.ProviderManager;
import org.hapjs.runtime.RuntimeActivity;
import org.json.JSONException;

import java.util.HashMap;
import java.util.Map;

public class RuntimeLogManager {
    public static final String CATEGORY_SERVER_ERROR = "serverError";
    public static final String KEY_SUBPACKAGEINFO_ERROR = "subpackageInfoError";
    public static final String KEY_PAY_FEATURE = "pay";
    public static final String KEY_PAGE_CHANGED = "pageChanged";
    public static final String KEY_VIDEO_FEATURE = "video";
    public static final String KEY_JS_ENV_INIT = "jsEnvInit";
    public static final String KEY_INFRAS_JS_LOAD = "infrasJsLoad";
    public static final String KEY_APP_JS_LOAD = "appJsLoad";
    // for pay feature
    public static final String PARAM_REPORT_RPK_VERSION = "rpk_version";
    public static final String PARAM_REPORT_RESULT_CODE = "result_code";
    public static final String PARAM_REPORT_ERR_MSG = "err_msg";
    public static final String PARAM_REPORT_PAY_TYPE = "pay_type";
    public static final String PARAM_REPORT_PAY_TYPE_ALI = "ali_pay";
    public static final String PARAM_REPORT_PAY_TYPE_WX = "wx_pay";
    public static final String PARAM_NATIVE_APP = "nativeApp";
    public static final String PARAM_NATIVE_ACTIVITY = "nativeActivity";
    public static final String PARAM_ROUTER_NATIVE_FROM = "routerAppFrom";
    public static final String PARAM_ROUTER_NATIVE_RESULT = "routerAppResult";
    public static final String VALUE_ROUTER_APP_FROM_ROUTER = "router";
    public static final String VALUE_ROUTER_APP_FROM_WEB = "web";
    public static final String VALUE_ROUTER_APP_FROM_PACKAGE = "package";
    public static final String VALUE_ROUTER_APP_FROM_JS_PUSH = "jsPush";
    public static final String VALUE_SUCCESS = "success";
    public static final String VALUE_FAIL = "fail";
    public static final String PARAM_DIALOG_CLICK_TYPE = "button_type";
    private static final String TAG = "RuntimeLog";
    private static final String CATEGORY_APP = "app";
    private static final String CATEGORY_PAGE_VIEW = "pageView";
    private static final String CATEGORY_PAGE_LOAD = "pageLoad";
    private static final String CATEGORY_PAGE_RENDER = "pageRender";
    private static final String CATEGORY_PAGE_ERROR = "pageError";
    private static final String CATEGORY_FEATURE_INVOKE = "featureInvoke";
    public static final String CATEGORY_FEATURE_RESULT = "featureResult";
    private static final String CATEGORY_PERMISSION = "permission";
    private static final String CATEGORY_EXTERNAL_CALL = "externalCall";
    private static final String CATEGORY_CARD = "card";
    private static final String CATEGORY_UI_THREAD = "uiThread";
    private static final String CATEGORY_JS_THREAD = "jsThread";
    private static final String CATEGORY_APP_JS_LOAD = "appJsLoad";
    private static final String CATEGORY_RENDER_ACTION_THREAD = "renderActionThread";
    private static final String CATEGORY_IO = "IO";
    private static final String CATEGORY_LAUNCHER_CREATE = "launcherCreate";
    private static final String KEY_APP_LOAD = "load";
    private static final String KEY_APP_SHOW = "show";
    private static final String KEY_APP_ROUTER = "router";
    private static final String KEY_APP_ROUTER_NO_QUERY_PARAMS = "routerNoQueryParams";
    private static final String KEY_APP_ROUTER_NATIVE_APP = "routerNativeApp";
    private static final String KEY_APP_DISK_USAGE = "diskUsage";
    private static final String KEY_PAGE_JS_HIT = "pageJsHit";
    private static final String KEY_PHONE_PROMPT = "phonePrompt";
    private static final String KEY_RESOURCE_NOT_FOUND = "resourceNotFound";
    private static final String KEY_CALL = "call";
    private static final String KEY_CARD_INSTALL = "cardInstall";
    private static final String KEY_CARD_DOWNLOAD = "cardDownload";
    private static final String KEY_CARD_UNINSTALL = "cardUninstall";
    private static final String KEY_CARD_RENDER = "cardRender";
    private static final String KEY_LAUNCHER_CREATE = "launcherCreate";
    private static final String KEY_TASK_NAME = "taskName";
    private static final String KEY_MENU_BAR_SHARE_RESULT = "menuBarShareResult";
    private static final String KEY_MENU_BAR_SHARE_ERROR = "menuBarShareError";
    private static final String KEY_MENU_BAR_SHARE_CANCEL = "menuBarShareCancel";
    private static final String KEY_ILLEGAL_ACCESS_FILE = "illegalAccessFile";

    private static final String PARAM_TIME_START = "startTime";
    private static final String PARAM_TIME_END = "endTime";
    private static final String PARAM_ACTION = "action";
    private static final String PARAM_TYPE = "type";
    private static final String PARAM_REFERER = "referer";
    private static final String PARAM_FORBIDDEN = "forbidden";
    private static final String PARAM_ACCEPT = "accept";
    private static final String PARAM_CRASH_DESC = "crashDesc";
    private static final String PARAM_STACK_TRACE = "stackTrace";
    private static final String PARAM_URI = "uri";
    private static final String PARAM_PAGE_PATH = "pagePath";
    private static final String PARAM_PHONE_COUNT = "phoneCount";
    private static final String PARAM_CLICK_COUNT = "clickCount";
    private static final String PARAM_DELETE_COUNT = "deleteCount";
    private static final String PARAM_USE_HISTORY = "useHistory";
    private static final String PARAM_INPUT_LENGTH = "inputLength";
    private static final String PARAM_HOST = "host";
    private static final String PARAM_PLATFORM = "platform";
    private static final String PARAM_RESOURCE = "resource";
    private static final String PARAM_SOURCE = "sourceJson";
    private static final String PARAM_COMPONENT = "component";
    private static final String PARAM_CALLING_PACKAGE = "callingPackage";
    private static final String PARAM_PAGE_PARAMS = "params";
    private static final String PARAM_PATH = "path";
    private static final String PARAM_RESULT_CODE = "resultCode";
    private static final String PARAM_ERROR_CODE = "errorCode";
    private static final String PARAM_MESSAGE = "message";
    private static final String PARAM_TASK_NAME = "taskName";
    private static final String PARAM_TASK_COST = "taskCost";
    private static final String PARAM_MENU_BAR_SHARE_PLATFORM = "menuBarSharePlatform";
    private static final String PARAM_FILE_PATH = "file_path";

    private static final String STATE_APP_LOAD = "appLoad";
    private static final String STATE_PAGE_VIEW = "pageView";
    private static final String STATE_PAGE_LOAD = "pageLoad";
    private static final String STATE_PAGE_RENDER = "pageRender";
    private static final String STATE_PHONE_PROMPT_START = "phonePromptStart";
    private static final String STATE_PHONE_PROMPT_CLICK = "phonePromptClick";
    private static final String STATE_PHONE_PROMPT_DELETE = "phonePromptDelete";
    private static final String STATE_APP_JS_LOAD = "appJsLoad";
    private static final String STATE_LAUNCHER_ACTIVITY_CREATE = "launcherActivityCreate";
    public static final String KEY_APP_EVENT_READERDIV_CLICK = "readerdivClick";
    public static final String KEY_APP_EVENT_RADERDIV_AD_SHOW = "readerdivAdShow";
    private static final String KEY_APP_ROUTER_DIALOG_SHOW = "routerDialogShow";
    private static final String KEY_APP_ROUTER_DIALOG_CLICK = "routerDialogClick";
    private LogProvider mProvider;
    public static final String PARAM_OUTER_APP_SOURCE_H5 = "sourceH5";
    public static final String PARAM_ROUTER_APP_RESULT_DESC = "result_desc";
    public static final String PARAM_ROUTER_RPK_FROM = "routerRpkFrom";
    public static final String PARAM_ROUTER_RPK_RESULT = "routerRpkResult";
    public static final String PARAM_ROUTER_RPK_TARGET = "target_pkg";
    public static final String KEY_RPK_ROUTER_RPK = "routerRpk";
    public static final String KEY_APP_ROUTER_RPK_DIALOG_SHOW = "routerRpkDialogShow";
    public static final String KEY_APP_ROUTER_RPK_DIALOG_CLICK = "routerRpkDialogClick";
    // share button
    public static final String KEY_APP_SHARE_BUTTON_SHOW = "shareButtonShow";
    public static final String KEY_APP_SHARE_BUTTON_CLICK = "shareButtonClick";
    //shortcut tips
    public static final String KEY_APP_EVENT_BUTTON_SHOW = "eventbuttonShow";
    public static final String KEY_APP_EVENT_BUTTON_CLICK = "eventbuttonClick";
    public static final String KEY_APP_EVENT_BUTTON_OPACITY = "eventbuttonOpacity";
    public static final String KEY_APP_EVENT_BUTTON_TIPS_SHOW = "eventbuttonTipsShow";

    //feature result
    public static final String PARAM_FEATURE_NAME = "feature";
    public static final String PARAM_FEATURE_ACTION = "action";
    public static final String PARAM_FEATURE_RESULT = "featureResult";

    private Map<Object, Object> mStates;
    private Object mStateLock;

    private RuntimeLogManager() {
        mStates = new HashMap<>();
        mStateLock = new Object();
        mProvider = (LogProvider) ProviderManager.getDefault().getProvider(LogProvider.NAME);
    }

    public static RuntimeLogManager getDefault() {
        return Holder.INSTANCE;
    }

    public void logAppLoadStart(String pkg) {
        if (mProvider == null) {
            return;
        }

        Object[] data = new Object[]{pkg, System.currentTimeMillis()};
        synchronized (mStateLock) {
            mStates.put(STATE_APP_LOAD, data);
        }
    }

    public void logAppLoadEnd(String pkg) {
        if (mProvider == null) {
            return;
        }

        long loadEnd = System.currentTimeMillis();
        Object[] data;
        synchronized (mStateLock) {
            data = (Object[]) mStates.remove(STATE_APP_LOAD);
        }
        if (data == null) {
            Log.e(TAG, "Mismatch app load record, data is null");
            return;
        }
        String dataPkg = (String) data[0];
        if (!pkg.equals(dataPkg)) {
            Log.e(TAG, "Mismatch app load record, dataPkg=" + dataPkg + ", pkg=" + pkg);
            return;
        }
        long loadStart = (Long) data[1];
        long loadCost = loadEnd - loadStart;
        if (loadCost < 0) {
            Log.e(TAG, "Mismatch app load record, loadStart=" + loadStart + ", loadEnd=" + loadEnd);
            return;
        }

        Map<String, String> params = new HashMap<>();
        params.put(PARAM_TIME_START, String.valueOf(loadStart));
        params.put(PARAM_TIME_END, String.valueOf(loadEnd));
        mProvider.logCalculateEvent(pkg, CATEGORY_APP, KEY_APP_LOAD, loadCost, params);
    }

    public void logAppDiskUsage(String pkg) {
        if (mProvider == null) {
            return;
        }

        Executors.io().execute(new DiskUsageTask(pkg));
    }

    private void doLogAppDiskUsage(String pkg, long size) {
        if (mProvider == null) {
            return;
        }

        mProvider.logCalculateEvent(pkg, CATEGORY_APP, KEY_APP_DISK_USAGE, size);
    }

    public void logAppShow(String pkg, int versionCode) {
        if (mProvider == null) {
            return;
        }
        Map<String, String> params = new HashMap<>();
        params.put(PARAM_REPORT_RPK_VERSION, String.valueOf(versionCode));
        mProvider.logCountEvent(pkg, CATEGORY_APP, KEY_APP_SHOW, params);
    }

    public void logAppRouter(String pkg, String uri) {
        if (mProvider == null) {
            return;
        }
        Map<String, String> params = new HashMap<>();
        params.put(PARAM_URI, uri);
        mProvider.logCountEvent(pkg, CATEGORY_APP, KEY_APP_ROUTER, params);
    }

    public void logAppRouterNoQueryParams(String pkg, String uri) {
        if (mProvider == null) {
            return;
        }
        Map<String, String> params = new HashMap<>();
        int queryStartIndex = uri.indexOf("?");
        if (queryStartIndex != -1) {
            uri = uri.substring(0, queryStartIndex);
        }
        params.put(PARAM_URI, uri);
        mProvider.logCountEvent(pkg, CATEGORY_APP, KEY_APP_ROUTER_NO_QUERY_PARAMS, params);
    }

    public void logAppRouterNativeApp(
            String pkg,
            String uri,
            String nativeApp,
            String nativeActivity,
            String routerAppFrom,
            boolean result,
            String resultDesc,
            String sourceH5) {
        if (mProvider == null) {
            return;
        }
        Map<String, String> params = new HashMap<>();
        params.put(PARAM_URI, uri);
        params.put(PARAM_NATIVE_APP, nativeApp);
        params.put(PARAM_NATIVE_ACTIVITY, nativeActivity);
        params.put(PARAM_ROUTER_NATIVE_FROM, routerAppFrom);
        params.put(PARAM_ROUTER_NATIVE_RESULT, result ? VALUE_SUCCESS : VALUE_FAIL);
        params.put(PARAM_ROUTER_APP_RESULT_DESC, resultDesc);
        if (!TextUtils.isEmpty(sourceH5)) {
            params.put(PARAM_OUTER_APP_SOURCE_H5, sourceH5);
        }
        mProvider.logCountEvent(pkg, CATEGORY_APP, KEY_APP_ROUTER_NATIVE_APP, params);
    }

    public void logRouterDialogShow(String pkg, String nativeApp) {
        if (mProvider == null) {
            return;
        }
        Map<String, String> params = new HashMap<>();
        params.put(PARAM_NATIVE_APP, nativeApp);
        mProvider.logCountEvent(pkg, CATEGORY_APP, KEY_APP_ROUTER_DIALOG_SHOW, params);
    }

    public void logRouterDialogClick(String pkg, String nativeApp, boolean result) {
        if (mProvider == null) {
            return;
        }
        Map<String, String> params = new HashMap<>();
        params.put(PARAM_NATIVE_APP, nativeApp);
        params.put(PARAM_DIALOG_CLICK_TYPE, result ? VALUE_SUCCESS : VALUE_FAIL);
        mProvider.logCountEvent(pkg, CATEGORY_APP, KEY_APP_ROUTER_DIALOG_CLICK, params);
    }

    public void logRouterQuickApp(String pkg, String targetPkg, String routerFrom, boolean result, String resultDesc) {
        if (mProvider == null) {
            return;
        }
        Map<String, String> params = new HashMap<>();
        params.put(PARAM_ROUTER_RPK_TARGET, targetPkg);
        params.put(PARAM_ROUTER_RPK_FROM, routerFrom);
        params.put(PARAM_ROUTER_RPK_RESULT, result ? VALUE_SUCCESS : VALUE_FAIL);
        params.put(PARAM_ROUTER_APP_RESULT_DESC, resultDesc);
        mProvider.logCountEvent(pkg, CATEGORY_APP, KEY_RPK_ROUTER_RPK, params);
    }

    public void logRouterRpkDialogShow(String pkg, String targetRpk) {
        if (mProvider == null) {
            return;
        }
        Map<String, String> params = new HashMap<>();
        params.put(PARAM_ROUTER_RPK_TARGET, targetRpk);
        mProvider.logCountEvent(pkg, CATEGORY_APP, KEY_APP_ROUTER_RPK_DIALOG_SHOW, params);
    }

    public void logRouterRpkDialogClick(String pkg, String targetRpk, boolean result) {
        if (mProvider == null) {
            return;
        }
        Map<String, String> params = new HashMap<>();
        params.put(PARAM_ROUTER_RPK_TARGET, targetRpk);
        params.put(PARAM_DIALOG_CLICK_TYPE, result ? VALUE_SUCCESS : VALUE_FAIL);
        mProvider.logCountEvent(pkg, CATEGORY_APP, KEY_APP_ROUTER_RPK_DIALOG_CLICK, params);
    }


    public void logPageViewStart(String pkg, String pageName, String referPageName) {
        if (mProvider == null) {
            return;
        }

        Object[] data = new Object[]{pkg, pageName, referPageName, System.currentTimeMillis()};
        synchronized (mStateLock) {
            mStates.put(STATE_PAGE_VIEW, data);
        }
    }

    public void logPageViewEnd(String pkg, String pageName) {
        if (mProvider == null) {
            return;
        }

        long viewEnd = System.currentTimeMillis();
        Object[] data;
        synchronized (mStateLock) {
            data = (Object[]) mStates.remove(STATE_PAGE_VIEW);
        }
        if (data == null) {
            Log.e(TAG, "Mismatch page view record, data is null");
            return;
        }
        String dataPkg = (String) data[0];
        String dataPageName = (String) data[1];
        if (!pkg.equals(dataPkg) || !pageName.equals(dataPageName)) {
            Log.e(
                    TAG,
                    "Mismatch page view record"
                            + ", dataPkg="
                            + dataPkg
                            + ", dataPageName="
                            + dataPageName
                            + ", pkg="
                            + pkg
                            + ", mPageName="
                            + pageName);
            return;
        }
        String referPageName = (String) data[2];
        long viewStart = (Long) data[3];
        long viewDuration = viewEnd - viewStart;
        if (viewDuration < 0) {
            Log.e(TAG,
                    "Mismatch page view record, viewStart=" + viewStart + ", viewEnd=" + viewEnd);
            return;
        }

        Map<String, String> params = new HashMap<>();
        params.put(PARAM_REFERER, referPageName);
        params.put(PARAM_TIME_START, String.valueOf(viewStart));
        params.put(PARAM_TIME_END, String.valueOf(viewEnd));
        mProvider.logCalculateEvent(pkg, CATEGORY_PAGE_VIEW, pageName, viewDuration, params);
    }

    public void logPageLoadStart(String pkg, String pageName) {
        if (mProvider == null) {
            return;
        }

        Object[] data = new Object[]{pkg, pageName, System.currentTimeMillis()};
        synchronized (mStateLock) {
            mStates.put(STATE_PAGE_LOAD, data);
        }
    }

    public void logPageLoadEnd(String pkg, String pageName) {
        if (mProvider == null) {
            return;
        }

        long loadEnd = System.currentTimeMillis();
        Object[] data;
        synchronized (mStateLock) {
            data = (Object[]) mStates.remove(STATE_PAGE_LOAD);
        }
        if (data == null) {
            Log.e(TAG, "Mismatch page load record, data is null");
            return;
        }
        String dataPkg = (String) data[0];
        String dataPageName = (String) data[1];
        if (!pkg.equals(dataPkg) || !pageName.equals(dataPageName)) {
            Log.e(
                    TAG,
                    "Mismatch page load record"
                            + ", dataPkg="
                            + dataPkg
                            + ", dataPageName="
                            + dataPageName
                            + ", pkg="
                            + pkg
                            + ", mPageName="
                            + pageName);
            return;
        }
        long loadStart = (Long) data[2];
        long loadCost = loadEnd - loadStart;
        if (loadCost < 0) {
            Log.e(TAG,
                    "Mismatch page load record, loadStart=" + loadStart + ", loadEnd=" + loadEnd);
            return;
        }

        Map<String, String> params = new HashMap<>();
        params.put(PARAM_TIME_START, String.valueOf(loadStart));
        params.put(PARAM_TIME_END, String.valueOf(loadEnd));
        mProvider.logCalculateEvent(pkg, CATEGORY_PAGE_LOAD, pageName, loadCost, params);
    }

    public void logPageCreateRenderStart(String pkg, String pageName) {
        logPageRenderStart(pkg, pageName, "create");
    }

    public void logPageRecreateRenderStart(String pkg, String pageName) {
        logPageRenderStart(pkg, pageName, "recreate");
    }

    public void logPageCacheRenderStart(String pkg, String pageName) {
        logPageRenderStart(pkg, pageName, "cache");
    }

    private void logPageRenderStart(String pkg, String pageName, String type) {
        if (mProvider == null) {
            return;
        }

        Object[] data = new Object[]{pkg, pageName, type, System.currentTimeMillis()};
        synchronized (mStateLock) {
            mStates.put(STATE_PAGE_RENDER, data);
        }
    }

    public void logPageRenderEnd(String pkg, String pageName) {
        if (mProvider == null) {
            return;
        }

        long loadEnd = System.currentTimeMillis();
        Object[] data;
        synchronized (mStateLock) {
            data = (Object[]) mStates.remove(STATE_PAGE_RENDER);
        }
        if (data == null) {
            // ignore
            return;
        }
        String dataPkg = (String) data[0];
        String dataPageName = (String) data[1];
        if (!pkg.equals(dataPkg) || !pageName.equals(dataPageName)) {
            Log.e(
                    TAG,
                    "Mismatch page render record"
                            + ", dataPkg="
                            + dataPkg
                            + ", dataPageName="
                            + dataPageName
                            + ", pkg="
                            + pkg
                            + ", mPageName="
                            + pageName);
            return;
        }
        String type = (String) data[2];
        long loadStart = (Long) data[3];
        long loadCost = loadEnd - loadStart;
        if (loadCost < 0) {
            Log.e(TAG,
                    "Mismatch page render record, loadStart=" + loadStart + ", loadEnd=" + loadEnd);
            return;
        }

        Map<String, String> params = new HashMap<>();
        params.put(PARAM_TYPE, type);
        params.put(PARAM_TIME_START, String.valueOf(loadStart));
        params.put(PARAM_TIME_END, String.valueOf(loadEnd));
        mProvider.logCalculateEvent(pkg, CATEGORY_PAGE_RENDER, pageName, loadCost, params);
    }

    public void logPageError(String pkg, String pageName, Exception e) {
        String debugEnabled = System.getProperty(RuntimeActivity.PROP_DEBUG, "false");
        if ("true".equals(debugEnabled)) {
            // not upload crash info when debug enabled
            return;
        }
        if (mProvider == null) {
            return;
        }

        Map<String, String> params = new HashMap<>();
        params.put(PARAM_CRASH_DESC, e.getMessage());
        params.put(PARAM_STACK_TRACE, LogUtils.getStackTrace(e));
        mProvider.logCountEvent(pkg, CATEGORY_PAGE_ERROR, pageName, params);
    }

    public void logPageChanged(Page page) {
        if (mProvider == null || page == null || page.getRequest() == null) {
            return;
        }

        String pkg = page.getRequest().getPackage();
        Map<String, String> params = new HashMap<>();
        Map<String, ?> pageParams = page.params;
        if (pageParams != null) {
            JSONObject object = new JSONObject();
            for (String key : pageParams.keySet()) {
                try {
                    object.put(key, pageParams.get(key));
                } catch (JSONException e) {
                    Log.e(TAG, "failed to add page parameter", e);
                }
            }
            params.put(PARAM_PAGE_PARAMS, object.toString());
        }
        params.put(PARAM_PATH, page.getPath());
        mProvider.logCountEvent(pkg, CATEGORY_APP, KEY_PAGE_CHANGED, params);
    }

    public void logFeatureInvoke(String pkg, String feature, String action) {
        if (mProvider == null) {
            return;
        }

        Map<String, String> params = new HashMap<>();
        params.put(PARAM_ACTION, action);
        mProvider.logCountEvent(pkg, CATEGORY_FEATURE_INVOKE, feature, params);
    }

    public void logFeatureResult(String pkg, String feature, String action, Response response) {
        if (mProvider == null) {
            return;
        }
        Map<String, String> params = new HashMap<>();
        params.put(PARAM_FEATURE_NAME, feature);
        params.put(PARAM_FEATURE_ACTION, action);
        params.put(PARAM_FEATURE_RESULT, String.valueOf(response != null ? response.getCode() : -1));
        mProvider.logCountEvent(pkg, CATEGORY_FEATURE_RESULT, CATEGORY_FEATURE_RESULT, params);
    }

    public void logPermissionPrompt(
            String pkg, String permission, boolean accept, boolean forbidden) {
        if (mProvider == null) {
            return;
        }

        Map<String, String> params = new HashMap<>();
        params.put(PARAM_ACCEPT, String.valueOf(accept));
        params.put(PARAM_FORBIDDEN, String.valueOf(forbidden));
        mProvider.logCountEvent(pkg, CATEGORY_PERMISSION, permission, params);
    }

    public void logPageJsHit(String pkg, String pagePath, boolean hit) {
        if (mProvider == null) {
            return;
        }
        Map<String, String> params = new HashMap<>();
        params.put(PARAM_PAGE_PATH, pagePath);
        mProvider.logCalculateEvent(pkg, CATEGORY_PAGE_LOAD, KEY_PAGE_JS_HIT, hit ? 1 : 0, params);
    }

    public void logPhonePromptStart(int phoneCount) {
        if (mProvider == null) {
            return;
        }
        Object[] data = new Object[]{System.currentTimeMillis(), phoneCount};
        synchronized (mStateLock) {
            mStates.remove(STATE_PHONE_PROMPT_START);
            mStates.remove(STATE_PHONE_PROMPT_CLICK);
            mStates.remove(STATE_PHONE_PROMPT_DELETE);
            mStates.put(STATE_PHONE_PROMPT_START, data);
        }
    }

    public void logPhonePromptClick(String phoneNumber) {
        if (mProvider == null) {
            return;
        }
        Object[] data;
        Object obj = mStates.get(STATE_PHONE_PROMPT_CLICK);
        if (obj != null) {
            Object[] oldData = (Object[]) obj;
            data = new Object[oldData.length + 1];
            data[0] = phoneNumber;
            System.arraycopy(oldData, 0, data, 1, oldData.length);
        } else {
            data = new Object[]{phoneNumber};
        }
        synchronized (mStateLock) {
            mStates.put(STATE_PHONE_PROMPT_CLICK, data);
        }
    }

    public void logPhonePromptDelete(String phoneNumber) {
        if (mProvider == null) {
            return;
        }
        Object[] data;
        Object obj = mStates.get(STATE_PHONE_PROMPT_DELETE);
        if (obj != null) {
            Object[] oldData = (Object[]) obj;
            data = new Object[oldData.length + 1];
            data[0] = phoneNumber;
            System.arraycopy(oldData, 0, data, 1, oldData.length);
        } else {
            data = new Object[]{phoneNumber};
        }
        synchronized (mStateLock) {
            mStates.put(STATE_PHONE_PROMPT_DELETE, data);
        }
    }

    public void logPhonePromptEnd(String inputText) {
        if (mProvider == null) {
            return;
        }
        long endTime = System.currentTimeMillis();
        Object[] startData;
        Object[] clickData;
        Object[] deleteData;
        synchronized (mStateLock) {
            startData = (Object[]) mStates.remove(STATE_PHONE_PROMPT_START);
            clickData = (Object[]) mStates.remove(STATE_PHONE_PROMPT_CLICK);
            deleteData = (Object[]) mStates.remove(STATE_PHONE_PROMPT_DELETE);
        }
        if (startData == null) {
            Log.e(TAG, "Mismatch phone prompt start record");
            return;
        }
        long startTime = (long) startData[0];
        int phoneCount = (int) startData[1];
        int clickCount = clickData != null ? clickData.length : 0;
        int deleteCount = deleteData != null ? deleteData.length : 0;
        boolean useHistory = false;
        if (clickData != null) {
            for (Object obj : clickData) {
                if (inputText.equals(obj)) {
                    useHistory = true;
                }
            }
        }
        long timeCost = endTime - startTime;
        Map<String, String> params = new HashMap<>();
        params.put(PARAM_PHONE_COUNT, String.valueOf(phoneCount));
        params.put(PARAM_CLICK_COUNT, String.valueOf(clickCount));
        params.put(PARAM_DELETE_COUNT, String.valueOf(deleteCount));
        params.put(PARAM_USE_HISTORY, String.valueOf(useHistory));
        params.put(PARAM_INPUT_LENGTH, String.valueOf(inputText.length()));
        String pkg = System.getProperty(RuntimeActivity.PROP_APP);
        mProvider.logCalculateEvent(pkg, CATEGORY_APP, KEY_PHONE_PROMPT, timeCost, params);
    }

    public void logResourceNotFound(String host, String platform, String resource, Throwable t) {
        if (mProvider == null) {
            return;
        }

        Map<String, String> params = new HashMap<>();
        params.put(PARAM_HOST, host);
        params.put(PARAM_PLATFORM, platform);
        params.put(PARAM_RESOURCE, resource);
        params.put(PARAM_STACK_TRACE, LogUtils.getStackTrace(t));
        params.put(PARAM_CRASH_DESC, t.getMessage());
        mProvider.logCountEvent(null, CATEGORY_APP, KEY_RESOURCE_NOT_FOUND, params);
    }

    public void logExternalCall(
            Context context, String callingPackage, Source source, Class component) {
        if (context == null || mProvider == null) {
            return;
        }
        String hostPackage = context.getPackageName();
        if (TextUtils.isEmpty(callingPackage) || hostPackage.equals(callingPackage)) {
            if (source != null) {
                callingPackage = source.getPackageName();
            }
        }

        if (source == null) {
            source = new Source();
            source.setPackageName(callingPackage);
        }

        if (!TextUtils.isEmpty(callingPackage) && !hostPackage.equals(callingPackage)) {
            Map<String, String> params = new HashMap<>();
            params.put(PARAM_CALLING_PACKAGE, callingPackage);
            params.put(PARAM_SOURCE, source.toJson().toString());
            params.put(PARAM_COMPONENT, component.getName());
            mProvider.logCountEvent(null, CATEGORY_EXTERNAL_CALL, KEY_CALL, params);
        }
    }

    public void logExternalCall(Context context, String callingPackage, Class component) {
        logExternalCall(context, callingPackage, null, component);
    }

    public void logExternalCall(Context context, int uid, Class component) {
        logExternalCall(context, uid, null, component);
    }

    public void logExternalCall(Context context, int uid, Source source, Class component) {
        String[] pkgs = context.getPackageManager().getPackagesForUid(uid);
        String callingPackage = pkgs != null && pkgs.length > 0 ? pkgs[0] : "";
        logExternalCall(context, callingPackage, source, component);
    }

    public void logSubpackageInfoError(String appPackage) {
        mProvider.logStringPropertyEvent(
                appPackage, CATEGORY_SERVER_ERROR, KEY_SUBPACKAGEINFO_ERROR, appPackage);
    }

    public void logPayFeature(Request request, String resultStatusCode, String msg,
                              String payType) {
        if (mProvider == null) {
            return;
        }
        Map<String, String> reportParam = new HashMap<>();
        String pkg = request.getApplicationContext().getPackage();
        Context context = request.getApplicationContext().getContext();
        AppInfo appInfo = CacheStorage.getInstance(context).getCache(pkg).getAppInfo();
        if (appInfo != null) {
            reportParam.put(PARAM_REPORT_RPK_VERSION, String.valueOf(appInfo.getVersionCode()));
        }
        reportParam.put(PARAM_REPORT_RESULT_CODE, resultStatusCode);
        reportParam.put(PARAM_REPORT_ERR_MSG, msg);
        reportParam.put(PARAM_REPORT_PAY_TYPE, payType);
        mProvider.logCountEvent(pkg, CATEGORY_FEATURE_INVOKE, KEY_PAY_FEATURE, reportParam);
    }

    public void logCardInstall(String pkg, int resultCode, int errorCode) {
        if (mProvider == null) {
            return;
        }

        Map<String, String> params = new HashMap<>();
        params.put(PARAM_RESULT_CODE, String.valueOf(resultCode));
        params.put(PARAM_ERROR_CODE, String.valueOf(errorCode));
        mProvider.logCountEvent(pkg, CATEGORY_CARD, KEY_CARD_INSTALL, params);
    }

    public void logCardDownload(String pkg, int resultCode, int errorCode) {
        if (mProvider == null) {
            return;
        }

        Map<String, String> params = new HashMap<>();
        params.put(PARAM_RESULT_CODE, String.valueOf(resultCode));
        params.put(PARAM_ERROR_CODE, String.valueOf(errorCode));
        mProvider.logCountEvent(pkg, CATEGORY_CARD, KEY_CARD_DOWNLOAD, params);
    }

    public void logCardUninstall(String pkg, int resultCode, int errorCode) {
        if (mProvider == null) {
            return;
        }

        Map<String, String> params = new HashMap<>();
        params.put(PARAM_RESULT_CODE, String.valueOf(resultCode));
        params.put(PARAM_ERROR_CODE, String.valueOf(errorCode));
        mProvider.logCountEvent(pkg, CATEGORY_CARD, KEY_CARD_UNINSTALL, params);
    }

    public void logCardRender(String pkg, String uri, boolean result, int errorCode,
                              String message) {
        if (mProvider == null) {
            return;
        }

        Map<String, String> params = new HashMap<>();
        params.put(PARAM_URI, uri);
        params.put(PARAM_RESULT_CODE, String.valueOf(result));
        params.put(PARAM_ERROR_CODE, String.valueOf(errorCode));
        params.put(PARAM_MESSAGE, message);
        mProvider.logCountEvent(pkg, CATEGORY_CARD, KEY_CARD_RENDER, params);
    }

    public void logVideoFeature(Request request, String resultStatusCode, String msg) {
        if (mProvider == null) {
            return;
        }
        Map<String, String> reportParam = new HashMap<>();
        String pkg = request.getApplicationContext().getPackage();
        Context context = request.getApplicationContext().getContext();
        AppInfo appInfo = CacheStorage.getInstance(context).getCache(pkg).getAppInfo();
        if (appInfo != null) {
            reportParam.put(PARAM_REPORT_RPK_VERSION, String.valueOf(appInfo.getVersionCode()));
        }
        reportParam.put(PARAM_REPORT_RESULT_CODE, resultStatusCode);
        reportParam.put(PARAM_REPORT_ERR_MSG, msg);
        mProvider.logCountEvent(pkg, CATEGORY_FEATURE_INVOKE, KEY_VIDEO_FEATURE, reportParam);
    }

    private void logTaskStart(String pkg, String taskName) {
        if (mProvider == null) {
            return;
        }
        Object[] data = new Object[]{pkg, System.currentTimeMillis()};
        synchronized (mStateLock) {
            mStates.put(taskName, data);
        }
    }

    private void logTaskEnd(String category, String pkg, String taskName) {
        if (mProvider == null) {
            return;
        }

        Map<String, String> params = generateParams(pkg, taskName);
        if (params == null) {
            return;
        }

        String cost = params.get(PARAM_TASK_COST);
        if (!TextUtils.isEmpty(cost)) {
            long taskCost = Long.parseLong(cost);
            mProvider.logCalculateEvent(pkg, category, KEY_TASK_NAME, taskCost, params);
        }
    }

    public void logUIThreadTaskStart(String pkg, String taskName) {
        logTaskStart(pkg, taskName);
    }

    public void logUIThreadTaskEnd(String pkg, String taskName) {
        logTaskEnd(CATEGORY_UI_THREAD, pkg, taskName);
    }

    public void logLauncherCreateStart(String pkg) {
        if (mProvider == null) {
            return;
        }
        Object[] data = new Object[]{pkg, System.currentTimeMillis()};
        synchronized (mStateLock) {
            mStates.put(STATE_LAUNCHER_ACTIVITY_CREATE, data);
        }
    }

    public void logLauncherCreateEnd(String pkg) {
        if (mProvider == null) {
            return;
        }

        long createEnd = System.currentTimeMillis();
        Object[] data;
        synchronized (mStateLock) {
            data = (Object[]) mStates.remove(STATE_LAUNCHER_ACTIVITY_CREATE);
        }

        if (data == null) {
            Log.e(TAG, "mismatch create record, data is null");
            return;
        }

        String dataPkg = (String) data[0];
        if (!pkg.equals(dataPkg)) {
            return;
        }

        long createStart = (Long) data[1];
        long createCost = createEnd - createStart;
        if (createCost < 0) {
            return;
        }

        Map<String, String> params = new HashMap<>();
        params.put(PARAM_TIME_START, String.valueOf(createStart));
        params.put(PARAM_TIME_END, String.valueOf(createEnd));
        mProvider.logCalculateEvent(
                pkg, CATEGORY_LAUNCHER_CREATE, KEY_LAUNCHER_CREATE, createCost, params);
    }

    public void logJsThreadTaskStart(String pkg, String taskName) {
        logTaskStart(pkg, taskName);
    }

    public void logJsThreadTaskEnd(String pkg, String taskName) {
        logTaskEnd(CATEGORY_JS_THREAD, pkg, taskName);
    }

    public void logAppJsLoadStart(String pkg) {
        if (mProvider == null) {
            return;
        }
        Object[] data = new Object[]{pkg, System.currentTimeMillis()};
        synchronized (mStateLock) {
            mStates.put(STATE_APP_JS_LOAD, data);
        }
    }

    public void logAppJsLoadEnd(String pkg) {
        if (mProvider == null) {
            return;
        }

        long loadEnd = System.currentTimeMillis();
        Object[] data;
        synchronized (mStateLock) {
            data = (Object[]) mStates.remove(STATE_APP_JS_LOAD);
        }

        if (data == null) {
            Log.e(TAG, "mismatch load record, data is null");
            return;
        }

        String dataPkg = (String) data[0];
        if (!pkg.equals(dataPkg)) {
            return;
        }

        long loadStart = (long) data[1];
        long loadCost = loadEnd - loadStart;
        if (loadCost < 0) {
            return;
        }

        Map<String, String> params = new HashMap<>();
        params.put(PARAM_TIME_START, String.valueOf(loadStart));
        params.put(PARAM_TIME_END, String.valueOf(loadEnd));
        mProvider.logCalculateEvent(pkg, CATEGORY_APP_JS_LOAD, KEY_APP_JS_LOAD, loadCost, params);
    }

    public void logRenderTaskStart(String pkg, String taskName) {
        logTaskStart(pkg, taskName);
    }

    public void logRenderTaskEnd(String pkg, String taskName) {
        logTaskEnd(CATEGORY_RENDER_ACTION_THREAD, pkg, taskName);
    }

    public void logAsyncThreadTaskStart(String pkg, String taskName) {
        logTaskStart(pkg, taskName);
    }

    public void logAsyncThreadTaskEnd(String pkg, String taskName) {
        logTaskEnd(CATEGORY_IO, pkg, taskName);
    }

    private Map<String, String> generateParams(String pkg, String taskName) {
        long taskEnd = System.currentTimeMillis();
        Object[] data;
        synchronized (mStateLock) {
            data = (Object[]) mStates.remove(taskName);
        }

        if (data == null) {
            return null;
        }

        String dataPkg = (String) data[0];
        if (!TextUtils.isEmpty(pkg) && !pkg.equals(dataPkg)) {
            return null;
        }

        long taskStart = (long) data[1];
        long taskCost = taskEnd - taskStart;
        if (taskCost < 0) {
            return null;
        }
        Map<String, String> params = new HashMap<>();
        params.put(PARAM_TIME_START, String.valueOf(taskStart));
        params.put(PARAM_TIME_END, String.valueOf(taskEnd));
        params.put(PARAM_TASK_NAME, taskName);
        params.put(PARAM_TASK_COST, String.valueOf(taskCost));
        return params;
    }

    public void logMenuBarShareResult(String pkg, String platform) {
        if (mProvider == null) {
            return;
        }

        Map<String, String> params = new HashMap<>();
        params.put(PARAM_MENU_BAR_SHARE_PLATFORM, platform);
        mProvider.logCountEvent(pkg, CATEGORY_APP, KEY_MENU_BAR_SHARE_RESULT, params);
    }

    public void logMenuBarShareError(String pkg, String platform) {
        if (mProvider == null) {
            return;
        }

        Map<String, String> params = new HashMap<>();
        params.put(PARAM_MENU_BAR_SHARE_PLATFORM, platform);
        mProvider.logCountEvent(pkg, CATEGORY_APP, KEY_MENU_BAR_SHARE_ERROR, params);
    }

    public void logMenuBarShareCancel(String pkg, String platform) {
        if (mProvider == null) {
            return;
        }

        Map<String, String> params = new HashMap<>();
        params.put(PARAM_MENU_BAR_SHARE_PLATFORM, platform);
        mProvider.logCountEvent(pkg, CATEGORY_APP, KEY_MENU_BAR_SHARE_CANCEL, params);
    }

    private static class Holder {
        static final RuntimeLogManager INSTANCE = new RuntimeLogManager();
    }

    public void recordIllegalAccessFile(String pkg, String filePath) {
        if (mProvider == null) {
            return;
        }
        Map<String, String> params = new HashMap<>();
        params.put(PARAM_FILE_PATH, filePath);
        mProvider.logCountEvent(pkg, CATEGORY_APP, KEY_ILLEGAL_ACCESS_FILE, params);
    }

    private static class DiskUsageTask implements Runnable {
        private String pkg;

        public DiskUsageTask(String pkg) {
            this.pkg = pkg;
        }

        @Override
        public void run() {
            long size = getDiskUsage();
            getDefault().doLogAppDiskUsage(pkg, size);
        }

        private long getDiskUsage() {
            HapEngine hapEngine = HapEngine.getInstance(pkg);
            return hapEngine.getApplicationContext().getDiskUsage()
                    + hapEngine.getResourceManager().size(hapEngine.getContext());
        }
    }
}

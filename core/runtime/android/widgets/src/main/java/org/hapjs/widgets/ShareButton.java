/*
 * Copyright (c) 2022-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import org.hapjs.bridge.Response;
import org.hapjs.bridge.annotation.WidgetAnnotation;
import org.hapjs.common.utils.HmacUtils;
import org.hapjs.common.utils.MenubarUtils;
import org.hapjs.common.utils.ThreadUtils;
import org.hapjs.component.Component;
import org.hapjs.component.Container;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.component.constants.Attributes;
import org.hapjs.component.view.gesture.GestureDelegate;
import org.hapjs.component.view.gesture.GestureHost;
import org.hapjs.component.view.gesture.IGesture;
import org.hapjs.logging.LogProvider;
import org.hapjs.logging.RuntimeLogManager;
import org.hapjs.model.AppInfo;
import org.hapjs.model.DisplayInfo;
import org.hapjs.render.Page;
import org.hapjs.render.RootView;
import org.hapjs.runtime.HapEngine;
import org.hapjs.runtime.ProviderManager;
import org.hapjs.system.SysOpProvider;
import org.hapjs.widgets.input.Button;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

@WidgetAnnotation(
        name = ShareButton.WIDGET_NAME,
        methods = {
                Component.METHOD_FOCUS,
                Component.METHOD_ANIMATE,
                Component.METHOD_GET_BOUNDING_CLIENT_RECT,
                Component.METHOD_TO_TEMP_FILE_PATH,
                Component.METHOD_TALKBACK_FOCUS,
                Component.METHOD_TALKBACK_ANNOUNCE
        }
)

public class ShareButton extends Button {

    protected static final String WIDGET_NAME = "share-button";

    private static final String TAG = "ShareButton";

    public static final String ATTR_SHARE_TITLE = "title";
    public static final String ATTR_SHARE_DESCRIPTION = "description";
    public static final String ATTR_SHARE_ICON = "icon";
    public static final String ATTR_SHARE_URL = "url";
    public static final String ATTR_SHARE_PATH = "path";
    public static final String ATTR_SHARE_PARAMS = "params";
    public static final String ATTR_PLATFORMS = "platforms";
    public static final String ATTR_USE_PAGE_PARAMS = "usepageparams";

    public static final String EVENT_SUCCESS = "success";
    public static final String EVENT_FAIL = "fail";
    public static final String EVENT_CANCEL = "cancel";
    private static final String Event_CALLBACK_CODE = "code";
    private static final String Event_CALLBACK_CONTENT = "content";

    private static final String REPORT_KEY_RPK_NAME = "quick_app_name";
    private static final String REPORT_KEY_RPK_PACKAGE = "app_package";
    private static final String REPORT_KEY_SHARE_URL = "url";
    private static final String REPORT_KEY_SHARE_PLATFORMS = "share_channel";
    private static final String REPORT_KEY_RESPONSE_CODE = "response_code";
    private static final String REPORT_KEY_RESPONSE_CONTENT = "response_content";

    private String mShareTitle;
    private String mShareDescription;
    private String mShareIcon;
    private String mShareUrl;
    private String mSharePath;
    private String mShareParams;
    private String mSharePlatforms;
    private boolean mUsePageParams;

    private boolean mIsSuccessEventRegistered = false;
    private boolean mIsFailEventRegistered = false;
    private boolean mIsCancelEventRegistered = false;

    public ShareButton(HapEngine hapEngine, Context context, Container parent, int ref, RenderEventCallback callback, Map<String, Object> savedState) {
        super(hapEngine, context, parent, ref, callback, savedState);
    }

    @Override
    protected boolean setAttribute(String key, Object attribute) {
        switch (key) {
            case ATTR_SHARE_TITLE:
                mShareTitle = Attributes.getString(attribute, "");
                return true;
            case ATTR_SHARE_DESCRIPTION:
                mShareDescription = Attributes.getString(attribute, "");
                return true;
            case ATTR_SHARE_ICON:
                mShareIcon = Attributes.getString(attribute, "");
                return true;
            case ATTR_SHARE_URL:
                mShareUrl = Attributes.getString(attribute, "");
                return true;
            case ATTR_SHARE_PATH:
                mSharePath = Attributes.getString(attribute, "");
                return true;
            case ATTR_SHARE_PARAMS:
                mShareParams = Attributes.getString(attribute, "");
                return true;
            case ATTR_PLATFORMS:
                mSharePlatforms = Attributes.getString(attribute, "");
                return true;
            case ATTR_USE_PAGE_PARAMS:
                mUsePageParams = Attributes.getBoolean(attribute, false);
                return true;
        }
        return super.setAttribute(key, attribute);
    }

    @Override
    protected boolean addEvent(String event) {
        if (TextUtils.isEmpty(event) || mHost == null) {
            return true;
        }

        switch (event) {
            case EVENT_SUCCESS:
                mIsSuccessEventRegistered = true;
                return true;
            case EVENT_FAIL:
                mIsFailEventRegistered = true;
                return true;
            case EVENT_CANCEL:
                mIsCancelEventRegistered = true;
                return true;
        }

        return super.addEvent(event);
    }

    @Override
    protected boolean removeEvent(String event) {
        if (TextUtils.isEmpty(event) || mHost == null) {
            return true;
        }

        switch (event) {
            case EVENT_SUCCESS:
                mIsSuccessEventRegistered = false;
                return true;
            case EVENT_FAIL:
                mIsFailEventRegistered = false;
                return true;
            case EVENT_CANCEL:
                mIsCancelEventRegistered = false;
                return true;
        }

        return super.removeEvent(event);
    }

    public boolean preConsumeEvent(String eventName, Map<String, Object> data, boolean immediately) {
        boolean isConsume = false;
        if (Attributes.Event.TOUCH_CLICK.equals(eventName)) {
            isConsume = true;
            startShare(eventName, data);
        }
        return isConsume;
    }

    private void startShare(String eventName, Map<String, Object> data) {
        SysOpProvider provider = ProviderManager.getDefault().getProvider(SysOpProvider.NAME);
        final Map<String, String> shareIdMap = provider.getMenuBarShareId(mContext);

        final AppInfo appInfo = mHapEngine.getApplicationContext().getAppInfo();
        String rpkName = appInfo != null ? appInfo.getName() : "";
        String rpkPackage = appInfo != null ? appInfo.getPackage() : "";
        String rpkIcon = appInfo != null ? appInfo.getIcon() : "";

        HashMap<String, String> extraShareData = new HashMap<>();
        final String defaultShareDesc = mContext.getResources().getString(org.hapjs.runtime.R.string.menubar_share_default_description);
        extraShareData.put(DisplayInfo.Style.KEY_MENUBAR_SHARE_TITLE, TextUtils.isEmpty(mShareTitle) ? rpkName : mShareTitle);
        extraShareData.put(DisplayInfo.Style.KEY_MENUBAR_SHARE_DESCRIPTION, TextUtils.isEmpty(mShareDescription) ? defaultShareDesc : mShareDescription);
        extraShareData.put(DisplayInfo.Style.KEY_MENUBAR_SHARE_ICON, TextUtils.isEmpty(mShareIcon) ? rpkIcon : mShareIcon);
        extraShareData.put(DisplayInfo.Style.PARAM_SHARE_CURRENT_PAGE, "true");
        extraShareData.put(DisplayInfo.Style.PARAM_SHARE_URL, TextUtils.isEmpty(mShareUrl) ? "" : mShareUrl);
        extraShareData.put(DisplayInfo.Style.PARAM_SHARE_PARAMS, TextUtils.isEmpty(mShareParams) ? "" : mShareParams);
        extraShareData.put(MenubarUtils.PARAM_PACKAGE, rpkPackage);
        if (!TextUtils.isEmpty(mSharePlatforms)) {
            extraShareData.put(MenubarUtils.PARAM_PLATFORMS, mSharePlatforms);
        }
        final Page page = getPage();
        JSONObject pageParams = new JSONObject();
        String defaultPagePath = "";
        if (page != null) {
            defaultPagePath = page.getPath();
        }
        String pagePath = !TextUtils.isEmpty(mSharePath) ? mSharePath : defaultPagePath;
        if (!TextUtils.isEmpty(pagePath)) {
            if (null != page && null != page.params && page.params.size() > 0) {
                try {
                    HmacUtils.mapToJSONObject(pageParams, page.params);
                } catch (JSONException e) {
                    Log.e(TAG, "initShareData mapToJSONObject error : " + e.getMessage());
                }
            }
        }
        extraShareData.put(MenubarUtils.PARAM_PAGE_PATH, TextUtils.isEmpty(pagePath) ? "" : pagePath);
        extraShareData.put(MenubarUtils.PARAM_PAGE_PARAMS, mUsePageParams ? pageParams.toString() : "");
        extraShareData.put(MenubarUtils.IS_FROM_SHARE_BUTTON, "true");

        final RootView rootView = ((RootView) getRootComponent().getHostView());

        MenubarUtils.startShare(shareIdMap, extraShareData, rootView, null, new MenubarUtils.ShareCallback() {
            @Override
            public void onShareCallback(Response response) {
                Log.d(TAG, "onShareCallback: " + response);
                Map<String, Object> params = new HashMap<>();
                params.put(Event_CALLBACK_CODE, response.getCode());
                params.put(Event_CALLBACK_CONTENT, response.getContent());
                switch (response.getCode()) {
                    case Response.CODE_SUCCESS:
                        if (mIsSuccessEventRegistered) {
                            mCallback.onJsEventCallback(getPageId(), mRef, EVENT_SUCCESS,
                                    ShareButton.this, params, null);
                        }
                        break;
                    case Response.CODE_CANCEL:
                        if (mIsCancelEventRegistered) {
                            mCallback.onJsEventCallback(getPageId(), mRef, EVENT_CANCEL,
                                    ShareButton.this, params, null);
                        }
                        break;
                    default:
                        if (mIsFailEventRegistered) {
                            mCallback.onJsEventCallback(getPageId(), mRef, EVENT_FAIL,
                                    ShareButton.this, params, null);
                        }
                        break;
                }
                fireGestureEvent(eventName, data);
                reportTraceEvent(RuntimeLogManager.KEY_APP_SHARE_BUTTON_CLICK, response);
            }
        });
    }

    private void fireGestureEvent(String eventName, Map<String, Object> data) {
        if (mContext instanceof Activity && ((Activity) mContext).isFinishing()) {
            Log.e(TAG, "fireGestureEvent Activity is finishing.");
            return;
        }
        ThreadUtils.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mHost instanceof GestureHost) {
                    GestureHost gestureHost = (GestureHost) mHost;
                    IGesture iGesture = null;
                    if (null != gestureHost) {
                        iGesture = gestureHost.getGesture();
                    }
                    if (iGesture instanceof GestureDelegate) {
                        ((GestureDelegate) iGesture).fireEvent(eventName, data, true);
                    }
                }
            }
        });
    }

    private void reportTraceEvent(String type, Response response) {
        if (null != mContext) {
            LogProvider logProvider = (LogProvider) ProviderManager.getDefault()
                    .getProvider(LogProvider.NAME);
            if (null != logProvider) {
                AppInfo appInfo = null;
                if (mHapEngine != null) {
                    appInfo = mHapEngine.getApplicationContext().getAppInfo();
                }
                HashMap<String, String> reportParams = new HashMap<>();
                String packageName = null != appInfo ? appInfo.getPackage() : "";
                reportParams.put(REPORT_KEY_RPK_NAME, null != appInfo ? appInfo.getName() : "");
                reportParams.put(REPORT_KEY_RPK_PACKAGE, packageName);
                reportParams.put(REPORT_KEY_SHARE_URL, null != mShareUrl ? mShareUrl : "");
                reportParams.put(REPORT_KEY_SHARE_PLATFORMS, assemblePlatformParams(response, mSharePlatforms));
                reportParams.put(REPORT_KEY_RESPONSE_CODE, null != response ? response.getCode() + "" : "");
                reportParams.put(REPORT_KEY_RESPONSE_CONTENT, null != response ? response.getContent() + "" : "");
                logProvider.logCountEvent(packageName, "", type, reportParams);
            }

        }
    }

    private String assemblePlatformParams(Response response, String sharePlatforms) {
        String platformParams = null;
        if (response != null && response.getCode() == Response.CODE_SUCCESS) {
            platformParams = String.valueOf(response.getContent());
        }
        if (platformParams == null) {
            if (!TextUtils.isEmpty(sharePlatforms)) {
                platformParams = sharePlatforms;
            } else {
                platformParams = "not set";
            }
        }
        return platformParams;
    }

}

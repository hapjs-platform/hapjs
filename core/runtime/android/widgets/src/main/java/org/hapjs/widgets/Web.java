/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.webkit.ValueCallback;
import androidx.collection.ArraySet;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import org.hapjs.bridge.annotation.WidgetAnnotation;
import org.hapjs.common.net.AcceptLanguageUtils;
import org.hapjs.common.utils.ColorUtil;
import org.hapjs.component.Component;
import org.hapjs.component.Container;
import org.hapjs.component.SwipeObserver;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.component.constants.Attributes;
import org.hapjs.component.view.state.State;
import org.hapjs.render.css.value.CSSValueFactory;
import org.hapjs.render.css.value.CSSValues;
import org.hapjs.runtime.HapEngine;
import org.hapjs.runtime.ProviderManager;
import org.hapjs.system.SysOpProvider;
import org.hapjs.widgets.view.NestedWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@WidgetAnnotation(
        name = Web.WIDGET_NAME,
        methods = {
                Web.METHOD_RELOAD,
                Web.METHOD_FORWARD,
                Web.METHOD_BACK,
                Web.METHOD_CAN_FORWARD,
                Web.METHOD_CAN_BACK,
                Web.METHOD_POST_MESSAGE,
                Web.METHOD_IS_SUPPORT_WEB_RTC,
                Component.METHOD_GET_BOUNDING_CLIENT_RECT,
                Component.METHOD_TO_TEMP_FILE_PATH,
                Component.METHOD_FOCUS
        })
public class Web extends Component<NestedWebView> implements SwipeObserver {
    protected static final String WIDGET_NAME = "web";
    // methods
    protected static final String METHOD_RELOAD = "reload";
    protected static final String METHOD_FORWARD = "forward";
    protected static final String METHOD_BACK = "back";
    protected static final String METHOD_CAN_FORWARD = "canForward";
    protected static final String METHOD_CAN_BACK = "canBack";
    protected static final String METHOD_POST_MESSAGE = "postMessage";
    protected static final String METHOD_IS_SUPPORT_WEB_RTC = "isSupportWebRTC";
    protected static final String ENABLE_NIGHT_MODE = "enablenightmode";
    private static final String TAG = "Web";
    // events
    private static final String EVENT_PAGE_START = "pagestart";
    private static final String EVENT_PAGE_FINISH = "pagefinish";
    private static final String EVENT_TITLE_RECEIVE = "titlereceive";
    private static final String EVENT_ERROR = "error";
    private static final String EVENT_MESSAGE = "message";
    private static final String EVENT_PROGRESS = "progress";
    // attr
    private static final String TRUSTED_URL = "trustedurl";
    private static final String ALLOW_THIRDPARTY_COOKIES = "allowthirdpartycookies";
    private static final String SUPPORT_ZOOM = "supportzoom";
    private static final String SHOW_LOADING_DIALOG = "showloadingdialog";

    private static final String USER_AGENT = "useragent";

    private static final String KEY_STATE = "state";

    private ArraySet<String> mTrustedUrls = new ArraySet<>();
    private String mTrustedSrc;
    private ArraySet<String> mDomTrustedUrls;
    private String mLastUrl;
    private boolean mIsCallFromHostViewAttached = false;
    private boolean mIsLastLoadFinish = true;
    private boolean mEnableNightMode = true;
    private boolean mHasSetForceDark = false;
    private boolean mRegisterPageStartEvent = false;
    private boolean mRegisterPageFinishEvent = false;
    private boolean mPageLoadStart = false;
    private LinkedList<String> mPendingMessages = new LinkedList<>();
    private String mUserAgent;

    public Web(
            HapEngine hapEngine,
            Context context,
            Container parent,
            int ref,
            RenderEventCallback callback,
            Map<String, Object> savedState) {
        super(hapEngine, context, parent, ref, callback, savedState);

        // set 1 as default value.
        CSSValues valueMap = CSSValueFactory.createCSSValues(State.NORMAL, "1");
        mStyleDomData.put(Attributes.Style.FLEX, valueMap);
        callback.addActivityStateListener(this);
        getRootComponent().increaseWebComponentCount();
    }

    public static String decodeUrl(String url) {
        if (TextUtils.isEmpty(url)) {
            return url;
        }
        String decodedUrl = null;
        try {
            decodedUrl = URLDecoder.decode(url, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException | IllegalArgumentException e) {
            Log.e(TAG, "decode url failed :" + url, e);
        }
        return decodedUrl;
    }

    @Override
    protected NestedWebView createViewImpl() {
        NestedWebView webView = new NestedWebView(mContext);
        if (mAttrsDomData.get(SHOW_LOADING_DIALOG) instanceof Boolean) {
            webView.setShowLoadingDialog((boolean) mAttrsDomData.get(SHOW_LOADING_DIALOG));
        }
        init(webView);
        webView.setClipChildren(false);
        return webView;
    }

    protected void init(NestedWebView webView) {
        webView.setComponent(this);
        ViewGroup.LayoutParams lp =
                new ViewGroup.MarginLayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        webView.setLayoutParams(lp);
        webView.setOnPageStartListener(
                (url, canBack, canForward) -> {
                    mPageLoadStart = true;
                    if (mRegisterPageStartEvent) {
                        Map<String, Object> params = new HashMap<>(4);
                        params.put("url", url);
                        params.put("canBack", canBack);
                        params.put("canForward", canForward);
                        mCallback.onJsEventCallback(
                                getPageId(), mRef, EVENT_PAGE_START, Web.this, params, null);
                    }

                    // jsscript copy from assets/jsscript/webview-start.js
                    String preDefineOnMessage =
                            "(function(){\n"
                                    + "  let _onmessage = system.onmessage\n"
                                    + "  const pendingMsgList = []\n"
                                    + "  const defaultOnmessage = function(data){\n"
                                    + "    pendingMsgList.push(data)\n"
                                    + "  }\n"
                                    + "  function processPendingMsg(){\n"
                                    + "    while(pendingMsgList.length > 0){\n"
                                    + "      const data = pendingMsgList.shift()\n"
                                    + "      _onmessage(data)\n"
                                    + "    }\n"
                                    + "  }\n"
                                    + "\n"
                                    + "  Object.defineProperty(system, 'onmessage', {\n"
                                    + "    set(v){\n"
                                    + "      _onmessage = v\n"
                                    +
                                    "      if(pendingMsgList.length > 0 && typeof _onmessage === 'function'){\n"
                                    + "        setTimeout(function(){\n"
                                    + "          processPendingMsg()\n"
                                    + "        }, 10)\n"
                                    + "      }\n"
                                    + "    },\n"
                                    + "    get(){\n"
                                    + "      if(typeof _onmessage === 'function'){\n"
                                    + "        return _onmessage\n"
                                    + "      } else{\n"
                                    + "        return defaultOnmessage\n"
                                    + "      }\n"
                                    + "    }\n"
                                    + "  })\n"
                                    + "})()";

                    if (null != mHost) {
                        mHost.evaluateJavascript(preDefineOnMessage, null);
                    }

                    while (!mPendingMessages.isEmpty()) {
                        String message = mPendingMessages.poll();
                        tryHandleMessage(
                                mHost.getUrl(),
                                new UrlCheckListener() {
                                    @Override
                                    public void onTrusted() {
                                        String onMessageJs =
                                                "system.onmessage(\'" + message + "\')";
                                        if (null != mHost) {
                                            mHost.evaluateJavascript(onMessageJs, null);
                                        }
                                    }

                                    @Override
                                    public void onUnTrusted() {
                                        Log.w(TAG,
                                                "post message failed, because current url not match trust url");
                                    }
                                });
                    }
                });
        webView.setOnPageFinishListener(
                (url, canBack, canForward) -> {
                    if (mRegisterPageFinishEvent) {
                        Map<String, Object> params = new HashMap();
                        params.put("url", url);
                        params.put("canBack", canBack);
                        params.put("canForward", canForward);
                        mCallback.onJsEventCallback(
                                getPageId(), mRef, EVENT_PAGE_FINISH, Web.this, params, null);
                    }
                });
    }

    @Override
    public void onHostViewAttached(ViewGroup parent) {
        mIsCallFromHostViewAttached = true;
        super.onHostViewAttached(parent);
        mIsCallFromHostViewAttached = false;
    }

    @Override
    protected boolean setAttribute(String key, Object attribute) {
        switch (key) {
            case Attributes.Style.SRC:
                // 不响应onHostViewAttached 的SRC属性提交，避免二次loadUrl
                if (mIsCallFromHostViewAttached) {
                    return false;
                }
                String url = Attributes.getString(attribute);
                if (TextUtils.isEmpty(url)) {
                    Log.e(TAG, "setAttribute: url can not be null");
                    return false;
                }
                // 不重复加载相同并且状态为正在加载中的url
                if (TextUtils.equals(url, mLastUrl) && !isLastLoadFinish()) {
                    return false;
                }
                mLastUrl = url;
                loadUrl(url);

                mTrustedUrls.remove(mTrustedSrc);
                mTrustedSrc = "\'" + url + "\'";
                if (!TextUtils.isEmpty(mTrustedSrc)) {
                    mTrustedUrls.add(mTrustedSrc);
                }
                return true;
            case TRUSTED_URL:
                if (mDomTrustedUrls == null) {
                    mDomTrustedUrls = new ArraySet<>();
                }
                mTrustedUrls.removeAll(mDomTrustedUrls);
                if (attribute instanceof JSONArray) {
                    JSONArray jsonArray = (JSONArray) attribute;
                    for (int i = 0; i < jsonArray.length(); i++) {
                        try {
                            Object trustedUrl = jsonArray.get(i);
                            if (trustedUrl instanceof JSONObject) {
                                mDomTrustedUrls.add(jsonArray.getString(i));
                            } else {
                                mDomTrustedUrls.add("\'" + jsonArray.getString(i) + "\'");
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "apply trusted url attr failed ", e);
                        }
                    }
                    mTrustedUrls.addAll(mDomTrustedUrls);
                }
                return true;
            case ALLOW_THIRDPARTY_COOKIES:
                Boolean allowThirdPartyCookies = Attributes.getBoolean(attribute, false);
                mHost.setAllowThirdPartyCookies(allowThirdPartyCookies);
                return true;
            case SUPPORT_ZOOM:
                boolean supportZoom = Attributes.getBoolean(attribute, true);
                mHost.setSupportZoom(supportZoom);
                return true;
            case SHOW_LOADING_DIALOG:
                boolean showLoadingDialog = Attributes.getBoolean(attribute, false);
                mHost.setShowLoadingDialog(showLoadingDialog);
                if (!showLoadingDialog) {
                    mHost.dismissLoadingDialog();
                }
                return true;
            case ENABLE_NIGHT_MODE:
                if (!mHasSetForceDark) {
                    mEnableNightMode = Attributes.getBoolean(attribute, true);
                    setNightMode(mHost);
                }
                return true;
            case Attributes.Style.FORCE_DARK:
                mHasSetForceDark = true;
                mEnableNightMode = Attributes.getBoolean(attribute, true);
                setNightMode(mHost);
                return true;
            case Attributes.Style.BACKGROUND_COLOR:
                if (mHost != null) {
                    String colorStr = Attributes.getString(attribute, "white");
                    int color = ColorUtil.getColor(colorStr, Color.WHITE);
                    mHost.setBackgroundColor(color);
                    return true;
                } else {
                    return false;
                }
            case USER_AGENT:
                if (mHost != null) {
                    String userAgent = Attributes.getString(attribute, NestedWebView.KEY_DEFAULT);
                    if (TextUtils.isEmpty(mUserAgent) || !mUserAgent.equalsIgnoreCase(userAgent)) {
                        mUserAgent = userAgent;
                        mHost.setUserAgent(mUserAgent);
                    }
                    return true;
                } else {
                    return false;
                }
            default:
                break;
        }
        return super.setAttribute(key, attribute);
    }

    /**
     * @param webView host
     */
    private void setNightMode(NestedWebView webView) {
        if (webView == null) {
            Log.e(TAG, "host is null");
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            SysOpProvider sysOpProvider =
                    ProviderManager.getDefault().getProvider(SysOpProvider.NAME);
            if (mEnableNightMode) {
                webView.setForceDarkAllowed(true);
            } else if (sysOpProvider.isCloseGlobalDefaultNightMode()) {
                webView.setForceDarkAllowed(false);
            } else {
                webView.setForceDarkAllowed(false);
            }
        }
    }

    @Override
    protected boolean addEvent(String event) {
        if (TextUtils.isEmpty(event) || mHost == null) {
            return true;
        }

        if (EVENT_PAGE_START.equals(event)) {
            mRegisterPageStartEvent = true;
            return true;
        } else if (EVENT_PAGE_FINISH.equals(event)) {
            mRegisterPageFinishEvent = true;
            return true;
        } else if (EVENT_TITLE_RECEIVE.equals(event)) {
            mHost.setOnTitleReceiveListener(
                    new NestedWebView.OnTitleReceiveListener() {
                        @Override
                        public void onTitleReceive(String title) {
                            Map<String, Object> params = new HashMap<>();
                            params.put("title", title);
                            mCallback.onJsEventCallback(
                                    getPageId(), mRef, EVENT_TITLE_RECEIVE, Web.this, params, null);
                        }
                    });

            return true;
        } else if (EVENT_ERROR.equals(event)) {
            mHost.setOnErrorListener(
                    new NestedWebView.OnErrorListener() {
                        @Override
                        public void onError(
                                String message,
                                String url,
                                boolean canBack,
                                boolean canGoForward,
                                NestedWebView.WebViewErrorType type,
                                int code,
                                String description,
                                boolean isAuthorized) {
                            Map<String, Object> params = new HashMap<>(8);
                            params.put("errorMsg", message);
                            params.put("url", url);
                            params.put("canBack", canBack);
                            params.put("canForward", canGoForward);
                            params.put("errorType", type.getValue());
                            params.put("code", code);
                            params.put("description", description);
                            params.put("isAuthorized", isAuthorized);
                            mCallback.onJsEventCallback(getPageId(), mRef, EVENT_ERROR, Web.this,
                                    params, null);
                        }
                    });

            return true;
        } else if (EVENT_MESSAGE.equals(event)) {
            mHost.setOnMessageListener(
                    new NestedWebView.OnMessageListener() {
                        @Override
                        public void onMessage(final String message, final String url) {
                            tryHandleMessage(
                                    url,
                                    new UrlCheckListener() {
                                        @Override
                                        public void onTrusted() {
                                            Map<String, Object> params = new HashMap<>();
                                            params.put("message", message);
                                            params.put("url", url);
                                            mCallback.onJsEventCallback(
                                                    getPageId(), mRef, EVENT_MESSAGE, Web.this,
                                                    params, null);
                                        }

                                        @Override
                                        public void onUnTrusted() {
                                            Log.w(
                                                    TAG,
                                                    "onmessage event not call, because current url not match trusted url");
                                        }
                                    });
                        }
                    });

            return true;
        } else if (EVENT_PROGRESS.equals(event)) {
            mHost.setOnProgressChangedListener(
                    new NestedWebView.OnProgressChangedListener() {
                        @Override
                        public void onProgressChanged(int i) {
                            Map<String, Object> params = new HashMap<>();
                            params.put("progress", i);
                            mCallback.onJsEventCallback(
                                    getPageId(), mRef, EVENT_PROGRESS, Web.this, params, null);
                        }
                    });
        }

        return super.addEvent(event);
    }

    @Override
    protected boolean removeEvent(String event) {
        if (TextUtils.isEmpty(event) || mHost == null) {
            return true;
        }

        if (EVENT_PAGE_START.equals(event)) {
            mRegisterPageStartEvent = false;
            return true;
        } else if (EVENT_PAGE_FINISH.equals(event)) {
            mRegisterPageFinishEvent = false;
            return true;
        } else if (EVENT_TITLE_RECEIVE.equals(event)) {
            mHost.setOnTitleReceiveListener(null);
            return true;
        } else if (EVENT_ERROR.equals(event)) {
            mHost.setOnErrorListener(null);
            return true;
        } else if (EVENT_MESSAGE.equals(event)) {
            mHost.setOnMessageListener(null);
            return true;
        } else if (EVENT_PROGRESS.equals(event)) {
            mHost.setOnProgressChangedListener(null);
            return true;
        }

        return super.removeEvent(event);
    }

    private void tryHandleMessage(String url, final UrlCheckListener listener) {
        if (TextUtils.isEmpty(url) || listener == null) {
            return;
        }

        StringBuilder builder = new StringBuilder();
        builder.append('[');
        for (int i = 0; i < mTrustedUrls.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(mTrustedUrls.valueAt(i));
        }
        builder.append(']');
        final String mTrustedUrlsStr = builder.toString();

        String checkUrlJs =
                "javascript:"
                        + "function checkUrl (url, trustedUrl) {\n"
                        + "  return trustedUrl.some(function(item) {\n"
                        + "    if (typeof item === 'string') {\n"
                        + "      if (url.endsWith('/')) {\n"
                        + "        if (!item.endsWith('/')) {\n"
                        + "          item += '/'\n"
                        + "        }\n"
                        + "      } else {\n"
                        + "        if (item.endsWith('/')) {\n"
                        + "          url += '/'\n"
                        + "        }\n"
                        + "      }\n"
                        + "      return url === item\n"
                        + "    }\n"
                        + "    else {\n"
                        + "      if (item.type === 'regexp') {\n"
                        + "        var reg = new RegExp(item.source, item.flags)\n"
                        + "        return reg.test(url)\n"
                        + "      }\n"
                        + "    }\n"
                        + "    return false\n"
                        + "  })\n"
                        + "}\n"
                        + "checkUrl(\'"
                        + url
                        + "\', "
                        + mTrustedUrlsStr
                        + ")";
        mHost.evaluateJavascript(
                checkUrlJs,
                new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String value) {
                        if ("true".equals(value)) {
                            listener.onTrusted();
                        } else {
                            checkDecodeUrl(url, mTrustedUrlsStr, listener);
                        }
                    }
                });
    }

    private void checkDecodeUrl(String url, String trustedUrlsStrs,
                                final UrlCheckListener listener) {
        if (TextUtils.isEmpty(url) || TextUtils.isEmpty(trustedUrlsStrs)) {
            if (null != listener) {
                listener.onUnTrusted();
            } else {
                Log.e(TAG, "checkDecodeUrl listener null");
            }
            return;
        }
        String decodeUrl = decodeUrl(url);
        String checkDecodeUrlJs =
                "javascript:"
                        + "function checkUrl (url, trustedUrl) {\n"
                        + "  return trustedUrl.some(function(item) {\n"
                        + "    if (typeof item === 'string') {\n"
                        + "      return url === item\n"
                        + "    }\n"
                        + "    else {\n"
                        + "      if (item.type === 'regexp') {\n"
                        + "        var reg = new RegExp(item.source, item.flags)\n"
                        + "        return reg.test(url)\n"
                        + "      }\n"
                        + "    }\n"
                        + "    return false\n"
                        + "  })\n"
                        + "}\n"
                        + "checkUrl(\'"
                        + decodeUrl
                        + "\', "
                        + trustedUrlsStrs
                        + ")";
        if (null == mHost) {
            Log.e(TAG, "checkDecodeUrl web  mHost null");
            return;
        }
        mHost.evaluateJavascript(
                checkDecodeUrlJs,
                new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String value) {
                        if ("true".equals(value)) {
                            listener.onTrusted();
                        } else {
                            listener.onUnTrusted();
                        }
                    }
                });
    }

    public void loadUrl(String url) {
        if (TextUtils.isEmpty(url) || mHost == null) {
            return;
        }
        if (TextUtils.isEmpty(mUserAgent)) {
            // update userAgent before load url
            mUserAgent = Attributes.getString(mAttrsDomData.get(USER_AGENT), NestedWebView.KEY_DEFAULT);
            mHost.setUserAgent(mUserAgent);
        }
        Map<String, String> headers = new HashMap<>(2);
        String acceptLanguage = AcceptLanguageUtils.getAcceptLanguage();
        headers.put("Accept-Language", acceptLanguage);
        mHost.loadUrl(url, headers);
    }

    public void reload() {
        if (mHost == null) {
            return;
        }
        mHost.reload();
    }

    public void forward() {
        if (mHost == null) {
            return;
        }
        mHost.goForward();
    }

    public void back() {
        if (mHost == null) {
            return;
        }
        mHost.goBack();
    }

    public void canForward(Map<String, Object> args) {
        boolean canForward;
        if (mHost == null) {
            canForward = false;
        } else {
            canForward = mHost.canGoForward();
        }
        if (args.get("callback") != null) {
            String callbackId = (String) args.get("callback");
            mCallback.onJsMethodCallback(getPageId(), callbackId, canForward);
        }
    }

    public void canBack(Map<String, Object> args) {
        boolean canBack;
        if (mHost == null) {
            canBack = false;
        } else {
            canBack = mHost.canGoBack();
        }
        if (args.get("callback") != null) {
            String callbackId = (String) args.get("callback");
            mCallback.onJsMethodCallback(getPageId(), callbackId, canBack);
        }
    }

    public void postMessage(Map<String, Object> args) {
        Object messageObj = args.get("message");
        if (messageObj != null) {
            final String message = (String) messageObj;
            tryHandleMessage(
                    mHost.getUrl(),
                    new UrlCheckListener() {
                        @Override
                        public void onTrusted() {
                            if (mPageLoadStart) {
                                String onMessageJs = "system.onmessage(\'" + message + "\')";
                                if (null != mHost) {
                                    mHost.evaluateJavascript(onMessageJs, null);
                                }
                            } else {
                                mPendingMessages.offer(message);
                            }
                        }

                        @Override
                        public void onUnTrusted() {
                            Log.w(TAG,
                                    "post message failed, because current url not match trusted url");
                        }
                    });
        }
    }

    private void isSupportWebRTC(Map<String, Object> args) {
        boolean isSupportWebRTC = false;
        if (mHost != null) {
            isSupportWebRTC = mHost.isSupportWebRTC();
        }
        if (args.get("callback") != null) {
            String callbackId = (String) args.get("callback");
            mCallback.onJsMethodCallback(getPageId(), callbackId, isSupportWebRTC);
        }
    }

    @Override
    public void invokeMethod(String methodName, Map<String, Object> args) {
        if (METHOD_RELOAD.equals(methodName)) {
            reload();
        } else if (METHOD_FORWARD.equals(methodName)) {
            forward();
        } else if (METHOD_BACK.equals(methodName)) {
            back();
        } else if (METHOD_CAN_FORWARD.equals(methodName)) {
            canForward(args);
        } else if (METHOD_CAN_BACK.equals(methodName)) {
            canBack(args);
        } else if (METHOD_POST_MESSAGE.equals(methodName)) {
            postMessage(args);
        } else if (METHOD_IS_SUPPORT_WEB_RTC.equals(methodName)) {
            isSupportWebRTC(args);
        } else if (METHOD_TO_TEMP_FILE_PATH.equals(methodName)
                || METHOD_GET_BOUNDING_CLIENT_RECT.equals(methodName)) {
            super.invokeMethod(methodName, args);
        }
    }

    @Override
    protected void onSaveInstanceState(Map<String, Object> outState) {
        super.onSaveInstanceState(outState);
        if (mHost == null) {
            return;
        }

        Bundle outBundle = new Bundle();
        mHost.saveState(outBundle);
        outState.put(KEY_STATE, outBundle);
    }

    @Override
    protected void onRestoreInstanceState(Map<String, Object> savedState) {
        super.onRestoreInstanceState(savedState);
        if (savedState == null) {
            return;
        }

        if (savedState.containsKey(KEY_STATE)) {
            Bundle savedBundle = (Bundle) savedState.get(KEY_STATE);
            mHost.restoreState(savedBundle);
        }
    }

    @Override
    public void onActivityResume() {
        super.onActivityResume();
        if (mHost != null) {
            mHost.onResume();
        }
    }

    @Override
    public void onActivityPause() {
        super.onActivityPause();
        if (mHost != null) {
            mHost.onPause();
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        if (mHost != null) {
            ViewParent viewParent = mHost.getParent();
            if (viewParent != null && viewParent instanceof ViewGroup) {
                ((ViewGroup) viewParent).removeView(mHost);
            }
            mHost.destroy();
            mHost = null;
        }
        mPendingMessages.clear();
        mCallback.removeActivityStateListener(this);
    }

    public boolean isLastLoadFinish() {
        return mIsLastLoadFinish;
    }

    public void setLastLoadFinish(boolean lastLoadFinish) {
        this.mIsLastLoadFinish = lastLoadFinish;
    }

    private interface UrlCheckListener {
        void onTrusted();

        void onUnTrusted();
    }
}

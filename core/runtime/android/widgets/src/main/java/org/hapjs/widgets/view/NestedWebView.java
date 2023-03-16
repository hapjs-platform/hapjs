/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.view;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.MimeTypeMap;
import android.webkit.PermissionRequest;
import android.webkit.SslErrorHandler;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebHistoryItem;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.collection.ArraySet;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.MotionEventCompat;
import androidx.core.view.NestedScrollingChildHelper;
import androidx.core.view.VelocityTrackerCompat;
import androidx.core.view.ViewCompat;

import org.hapjs.bridge.Callback;
import org.hapjs.bridge.Extension;
import org.hapjs.bridge.ExtensionManager;
import org.hapjs.bridge.HybridManager;
import org.hapjs.bridge.HybridView;
import org.hapjs.bridge.LifecycleListener;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.permission.HapPermissionManager;
import org.hapjs.bridge.permission.PermissionCallback;
import org.hapjs.bridge.provider.webview.WebviewSettingProvider;
import org.hapjs.common.net.UserAgentHelper;
import org.hapjs.common.utils.FeatureInnerBridge;
import org.hapjs.common.utils.FileUtils;
import org.hapjs.common.utils.NavigationUtils;
import org.hapjs.common.utils.ThreadUtils;
import org.hapjs.common.utils.UriUtils;
import org.hapjs.common.utils.WebViewUtils;
import org.hapjs.component.Component;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.component.view.ComponentHost;
import org.hapjs.component.view.NestedScrollingListener;
import org.hapjs.component.view.NestedScrollingView;
import org.hapjs.component.view.gesture.GestureHost;
import org.hapjs.component.view.gesture.IGesture;
import org.hapjs.component.view.keyevent.KeyEventDelegate;
import org.hapjs.component.view.webview.BaseWebViewClient;
import org.hapjs.model.AppInfo;
import org.hapjs.render.RootView;
import org.hapjs.render.jsruntime.serialize.Serializable;
import org.hapjs.render.vdom.DocComponent;
import org.hapjs.runtime.CheckableAlertDialog;
import org.hapjs.runtime.DarkThemeUtil;
import org.hapjs.runtime.ProviderManager;
import org.hapjs.runtime.RouterManageProvider;
import org.hapjs.system.SysOpProvider;
import org.hapjs.widgets.R;
import org.hapjs.widgets.Web;
import org.hapjs.widgets.animation.WebProgressBar;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import static org.hapjs.logging.RuntimeLogManager.VALUE_ROUTER_APP_FROM_WEB;

public class NestedWebView extends WebView
        implements ComponentHost, NestedScrollingView, GestureHost {
    protected static final String TAG = "NestedWebView";

    private static final int CHOOSE_MODE_DEFAULT = 0;
    private static final int CHOOSE_MODE_EMPTY = 1;
    private static final int CHOOSE_MODE_SPECIAL = 2;
    private static final int CHOOSE_LOW_API_MODE = 0;
    private static final int CHOOSE_HIGH_API_MODE = 1;

    private static final int LOCAL_ERROR_PAGE_BACK_STEP = -2;
    private static final int REQUEST_FILE_CODE = 1;
    private static final int REQUEST_WRITE_PERMISSION = 2;
    private static final String SSL_ERROR_ULR =
            "file:///android_asset/hap/web/ssl/error/index.html";
    private static final String HTTP_ERROR_ULR =
            "file:///android_asset/hap/web/http/error/index.html?errorCode=";
    private static final String SSL_ERROR_IN_WHITELIST_URL =
            SSL_ERROR_ULR + "?type=inWhiteList&lang=";
    private static final String SSL_ERROR_OTHER_URL = SSL_ERROR_ULR + "?type=other&lang=";
    private static final int MIN_PLATFORM_VERSION_1090 = 1090;
    private static List<String> mTrustedSslDomains;
    private static List<String> mAuthorizedSslDomains;
    private final int[] mScrollOffset = new int[2];
    private final int[] mScrollConsumed = new int[2];
    private final NestedScrollingChildHelper mChildHelper;
    private OnShouldOverrideUrlLoadingListener mOnshouldOverrideLoadingListener;
    private OnPageStartListener mOnPageStartListener;
    private OnPageFinishListener mOnPageFinishListener;
    private OnTitleReceiveListener mOnTitleReceiveListener;
    private OnErrorListener mOnErrorListener;
    private OnMessageListener mOnMessageListener;
    private int mLastY;
    private int mNestedOffsetY;
    private Component mComponent;
    private IGesture mGesture;
    private KeyEventDelegate mKeyEventDelegate;
    private ValueCallback<Uri[]> mFilePathCallback;
    private ValueCallback<Uri> mSingleFileCallback;
    private Context mContext;
    private ViewTreeObserver.OnGlobalLayoutListener mOnGlobalLayoutListener;
    private DownloadConfirmDialog mConfirmDialog;
    private int mActivePointerId = -1;
    private int mMinimumVelocity;
    private int mMaximumVelocity;
    private NestedScrollingListener mNestedScrollingListener;
    private VelocityTracker mVelocityTracker;
    private int mScrollRange;
    private View mFullScreenView;
    private int mSavedScreenOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
    private int mSavedSystemUiVisibility = -1;
    private File mCachePhotoFile = null;
    private File mCacheVideoFile = null;
    private String mLastSslErrorUrl;
    private boolean mShowLoadingDialog = false;
    private OnProgressChangedListener mOnProgressChangedListener;
    private WebSettings mSettings;
    private WebProgressBar mProgressBar;
    private int mMinPlatformVersion = 0;

    private CheckableAlertDialog mLocationDialog;
    private CheckableAlertDialog mWebRtcDialog;

    public static final String KEY_SYSTEM = "system";
    public static final String KEY_DEFAULT = "default";
    private String mSourceH5 = ""; //记录哪个网页调起的app

    // jssdk url
    private static final String JSSDK_URL = "https://quickapp/js/jssdk.hapwebview.min.js";
    private static final String JSSDK_LOCAL_PATH = "jsscript/hapjssdk.min.js";
    private static final String ERROR_MSG_URL_UNTRUSTED = "url untrusted";
    private WebviewSettingProvider mWebViewSettingProvider;

    public NestedWebView(Context context) {
        super(context);
        mContext = context;
        setBackgroundColor(Color.WHITE);
        mChildHelper = new NestedScrollingChildHelper(this);
        setNestedScrollingEnabled(true);

        final ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        initWebView();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mOnGlobalLayoutListener == null) {
            mOnGlobalLayoutListener = new KeyboardStatusListener();
        }
        // 1. WebView can't "adjustPan", because we can't get focus area in webView.
        // 2. Default "adjustResize" will be ignored by system when "fullscreen"
        // So we custom "adjustResize" when it happens
        int flags = ((Activity) getContext()).getWindow().getAttributes().flags;
        if ((flags & WindowManager.LayoutParams.FLAG_FULLSCREEN) != 0
                || (flags & WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE) == 0) {
            getViewTreeObserver().addOnGlobalLayoutListener(mOnGlobalLayoutListener);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mOnGlobalLayoutListener != null) {
            // restore content height.
            adjustKeyboard(0);
            getViewTreeObserver().removeOnGlobalLayoutListener(mOnGlobalLayoutListener);
        }
        if (mConfirmDialog != null) {
            mConfirmDialog.dismiss();
        }
        if (mWebRtcDialog != null) {
            mWebRtcDialog.dismiss();
        }
        if (mLocationDialog != null) {
            mLocationDialog.dismiss();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean result = super.onKeyDown(keyCode, event);
        return onKey(KeyEvent.ACTION_DOWN, keyCode, event, result);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        boolean result = super.onKeyUp(keyCode, event);
        return onKey(KeyEvent.ACTION_UP, keyCode, event, result);
    }

    private boolean onKey(int keyAction, int keyCode, KeyEvent event, boolean result) {
        if (mKeyEventDelegate == null) {
            mKeyEventDelegate = new KeyEventDelegate(mComponent);
        }
        result |= mKeyEventDelegate.onKey(keyAction, keyCode, event);
        return result;
    }

    private View getAdjustKeyboardHostView() {
        View hostView = null;
        if (getComponent() != null && getComponent().getRootComponent() != null) {
            hostView = getComponent().getRootComponent().getDecorLayout();
        }
        return hostView;
    }

    private void adjustKeyboard(int keyboardHeight) {
        // same implementation with system's "adjustResize"
        View hostView = getAdjustKeyboardHostView();
        if (hostView == null) {
            Log.e(TAG, "adjustKeyboard error, host view is null");
            return;
        }
        hostView.setPadding(0, 0, 0, keyboardHeight);
    }

    public void setAllowThirdPartyCookies(Boolean allowThirdPartyCookies) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // 这个接口只支持安卓5.0以上版本号,5.0以下的系统，默认是开启接收第三方cookies，且没有开放单独的接口来处理第三方cookies，故不做处理
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, allowThirdPartyCookies);
        }
    }

    public void setSupportZoom(boolean support) {
        if (mSettings != null) {
            mSettings.setSupportZoom(support);
            mSettings.setBuiltInZoomControls(support);
        }
    }

    public void setShowLoadingDialog(boolean showDialog) {
        mShowLoadingDialog = showDialog;
    }

    public void showLoadingDialog() {
        if (mShowLoadingDialog) {
            addProgressbarAndTextView();
        }
    }

    public void dismissLoadingDialog() {
        if (mProgressBar != null) {
            mProgressBar.setVisibility(GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // restore content height.
        adjustKeyboard(0);
    }

    public void setOnshouldOverrideLoadingListener(
            OnShouldOverrideUrlLoadingListener onshouldOverrideLoadingListener) {
        mOnshouldOverrideLoadingListener = onshouldOverrideLoadingListener;
    }

    public void setOnPageStartListener(OnPageStartListener l) {
        mOnPageStartListener = l;
    }

    public void setOnPageFinishListener(OnPageFinishListener l) {
        mOnPageFinishListener = l;
    }

    public void setOnTitleReceiveListener(OnTitleReceiveListener l) {
        mOnTitleReceiveListener = l;
    }

    public void setOnErrorListener(OnErrorListener l) {
        mOnErrorListener = l;
    }

    public void setOnMessageListener(OnMessageListener l) {
        mOnMessageListener = l;
    }

    public void setOnProgressChangedListener(OnProgressChangedListener l) {
        mOnProgressChangedListener = l;
    }

    private boolean isWeixinPay(String url) {
        return !TextUtils.isEmpty(url)
                && (url.startsWith("weixin://wap/pay") || url.startsWith("weixin://dl/business/"));
    }

    private boolean isAlipay(String url) {
        return !TextUtils.isEmpty(url) && (url.startsWith("alipays:") || url.startsWith("alipay"));
    }

    private boolean isQQLogin(String url) {
        return !TextUtils.isEmpty(url) && url.startsWith("wtloginmqq:");
    }

    private void addProgressbarAndTextView() {
        if (mProgressBar == null) {
            mProgressBar = new WebProgressBar(mContext);
            mProgressBar.setVisibility(VISIBLE);
            addView(mProgressBar);
        }
    }

    protected void initWebView() {
        mSettings = getSettings();
        mSettings.setJavaScriptEnabled(true);
        mSettings.setSavePassword(false);
        mSettings.setAllowFileAccess(false);
        mSettings.setAllowUniversalAccessFromFileURLs(false);
        mSettings.setAllowFileAccessFromFileURLs(false);
        try {
            removeJavascriptInterface("searchBoxJavaBridge_");
            removeJavascriptInterface("accessibility");
            removeJavascriptInterface("accessibilityTraversal");
        } catch (Exception e) {
            Log.e(TAG, "initWebView: ", e);
        }
        mSettings.setDomStorageEnabled(true);
        mSettings.setUseWideViewPort(true);
        mSettings.setSupportZoom(true);
        mSettings.setBuiltInZoomControls(true);
        mSettings.setDisplayZoomControls(false);
        mSettings.setLoadWithOverviewMode(true);
        mSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING);
        // support h5 autoplay
        mSettings.setMediaPlaybackRequiresUserGesture(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        setWebViewClient(
                new BaseWebViewClient(BaseWebViewClient.WebSourceType.WEB) {

                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        Log.d(TAG, "shouldOverrideUrlLoading");
                        Intent intent = new Intent();
                        intent.setAction(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(url));
                        intent.addCategory(Intent.CATEGORY_BROWSABLE);

                        boolean isAlipay = isAlipay(url);
                        if (isWeixinPay(url) || isAlipay || isQQLogin(url)) {
                            //不允许跳转到支付宝支付以外的页面
                            RouterManageProvider provider = ProviderManager.getDefault().getProvider(RouterManageProvider.NAME);
                            if (provider.inWebpayForbiddenList(getContext(), url)) {
                                Log.d(TAG, "in webpay forbidden list");
                                NavigationUtils.statRouterNativeApp(mContext, getAppPkg(), url, intent, VALUE_ROUTER_APP_FROM_WEB, false, "in webpay forbidden list", mSourceH5);
                                return true;
                            }
                            try {
                                mContext.startActivity(intent);
                                NavigationUtils.statRouterNativeApp(mContext, getAppPkg(), url, intent, VALUE_ROUTER_APP_FROM_WEB, true, "webview pay", mSourceH5);
                            } catch (ActivityNotFoundException e) {
                                Log.d(TAG, "Fail to launch deeplink", e);
                                NavigationUtils.statRouterNativeApp(mContext, getAppPkg(), url, intent, VALUE_ROUTER_APP_FROM_WEB, false, "no compatible activity found", mSourceH5);
                            }
                            return true;
                        }

                        if (mComponent == null) {
                            Log.e(TAG, "shouldOverrideUrlLoading error: component is null");
                            mSourceH5 = url;
                            return false;
                        }
                        boolean isIntercept = isInterceptUrl(url);
                        if (isIntercept) {
                            if (mOnshouldOverrideLoadingListener != null) {
                                mOnshouldOverrideLoadingListener.onShouldOverrideUrlLoading(view, url);
                            }
                            return true;
                        }
                        RenderEventCallback callback = mComponent.getCallback();
                        boolean result = (callback != null
                                && callback.shouldOverrideUrlLoading(url, mSourceH5, mComponent.getPageId()))
                                || !UriUtils.isWebUri(url);
                        if (!result) {
                            mSourceH5 = url;
                        }
                        return result;
                    }

                    @Override
                    public void onPageStarted(WebView view, String url, Bitmap favicon) {
                        super.onPageStarted(view, url, favicon);
                        Log.d(TAG, "onPageStarted");
                        showLoadingDialog();
                        if (mComponent != null && mComponent instanceof Web) {
                            ((Web) mComponent).setLastLoadFinish(false);
                        }
                        if (mOnPageStartListener != null) {
                            mOnPageStartListener.onPageStart(url, canGoBack(), view.canGoForward());
                        }
                    }

                    @Override
                    public void onPageFinished(WebView view, String url) {
                        super.onPageFinished(view, url);
                        Log.d(TAG, "onPageFinished ");
                        if (mShowLoadingDialog && mProgressBar != null) {
                            mProgressBar.startProgress(100, WebProgressBar.FORCE_SET_PROGRESS);
                            mProgressBar.stopProgress(true);
                        }
                        if (mComponent != null && mComponent instanceof Web) {
                            ((Web) mComponent).setLastLoadFinish(true);
                        }
                        if (mOnPageFinishListener != null) {
                            mOnPageFinishListener
                                    .onPageFinish(url, canGoBack(), view.canGoForward());
                        }
                    }

                    @Nullable
                    @Override
                    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                        // load local jssdk file
                        if (JSSDK_URL.equals(url)) {
                            AssetManager assetManager = getResources().getAssets();
                            try {
                                InputStream inputStream = assetManager.open(JSSDK_LOCAL_PATH);
                                Log.d(TAG, "load hapjssdk success");
                                return new WebResourceResponse(parseFileType(url), "UTF-8", inputStream);
                            } catch (IOException e) {
                                Log.e(TAG, "read hapjssdk file error", e);
                            }
                            return null;
                        }
                        return super.shouldInterceptRequest(view, url);
                    }

                    private String parseFileType(String filePath) {
                        if (TextUtils.isEmpty(filePath)) {
                            return "";
                        }
                        if (filePath.endsWith(".html") || filePath.endsWith(".htm")) {
                            return "text/html";
                        }
                        if (filePath.endsWith(".js")) {
                            return "text/javascript";
                        }
                        if (filePath.endsWith(".css")) {
                            return "text/css";
                        }
                        return "";
                    }

                    @Override
                    public void onReceivedError(
                            WebView view, WebResourceRequest request, WebResourceError error) {
                        dismissLoadingDialog();
                        int errorCode = -1;
                        String errorDescription = "unknown";
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            String url = request.getUrl() != null ? request.getUrl().toString() :
                                    view.getUrl();
                            if (request.isForMainFrame()) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    errorCode = error.getErrorCode();
                                    errorDescription =
                                            error.getDescription() != null
                                                    ? error.getDescription().toString() : "";
                                }
                                if (mOnErrorListener != null) {
                                    mOnErrorListener.onError(
                                            "received error",
                                            url,
                                            canGoBack(),
                                            view.canGoForward(),
                                            WebViewErrorType.NORMAL,
                                            errorCode,
                                            errorDescription,
                                            false);
                                }
                            } else {
                                Log.e(TAG, "onReceivedError in subframe, error url:" + url);
                            }
                        } else {
                            if (mOnErrorListener != null) {
                                mOnErrorListener.onError(
                                        "received error",
                                        view.getUrl(),
                                        canGoBack(),
                                        view.canGoForward(),
                                        WebViewErrorType.NORMAL,
                                        errorCode,
                                        errorDescription,
                                        false);
                            }
                        }
                    }

                    @Override
                    public void onReceivedError(
                            WebView view, int errorCode, String description, String failingUrl) {
                        dismissLoadingDialog();
                        super.onReceivedError(view, errorCode, description, failingUrl);
                        // Support below than Api 23. The main resource is unavailable.
                        if (mOnErrorListener != null) {
                            mOnErrorListener.onError(
                                    "received error",
                                    failingUrl,
                                    canGoBack(),
                                    view.canGoForward(),
                                    WebViewErrorType.NORMAL,
                                    errorCode,
                                    description,
                                    false);
                        }
                    }

                    @Override
                    public void onReceivedHttpError(
                            WebView view, WebResourceRequest request,
                            WebResourceResponse errorResponse) {
                        super.onReceivedHttpError(view, request, errorResponse);
                        dismissLoadingDialog();
                        // above Android 6.0 deal with http error
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            // ignore sub resource error
                            if (request.isForMainFrame()) {
                                if (mOnErrorListener != null) {
                                    mOnErrorListener.onError(
                                            "received http error",
                                            view.getUrl(),
                                            canGoBack(),
                                            view.canGoForward(),
                                            WebViewErrorType.HTTP,
                                            errorResponse.getStatusCode(),
                                            errorResponse.getReasonPhrase(),
                                            false);
                                }
                                final int httpErrorCode = errorResponse.getStatusCode();
                                final StringBuilder builder =
                                        new StringBuilder(HTTP_ERROR_ULR)
                                                .append(httpErrorCode)
                                                .append("&lang=")
                                                .append(Locale.getDefault().getLanguage());
                                // Avoid page load not working
                                ThreadUtils.runOnUiThreadWithDelay(
                                        new Runnable() {
                                            @Override
                                            public void run() {
                                                view.loadUrl(builder.toString());
                                            }
                                        },
                                        100);
                            } else {
                                Log.e(
                                        TAG,
                                        "onReceivedHttpError in subframe, error url:"
                                                + (request.getUrl() != null
                                                ? request.getUrl().toString() : ""));
                            }
                        } else {
                            Log.e(TAG, "onReceived http error not support");
                        }
                    }

                    @Override
                    public void onReceivedSslError(WebView view, SslErrorHandler handler,
                                                   SslError error) {
                        dismissLoadingDialog();
                        // ignore sub resource error
                        final String url = error.getUrl();
                        if (!TextUtils.isEmpty(view.getUrl()) && view.getUrl().equals(url)) {
                            // only report main resource error
                            String domain = getDomain(url);
                            boolean isAuthorized = isInAuthorizedDomains(domain);
                            if (mOnErrorListener != null) {
                                mOnErrorListener.onError(
                                        "received ssl error",
                                        view.getUrl(),
                                        canGoBack(),
                                        view.canGoForward(),
                                        WebViewErrorType.SSL,
                                        error.getPrimaryError(),
                                        error.toString(),
                                        isAuthorized);
                            }
                            mLastSslErrorUrl = url;
                            if (isDomainInWhitelist(domain)) {
                                if (isAuthorized) {
                                    handler.proceed();
                                } else {
                                    // load ssl error page with continue button
                                    String filePath = SSL_ERROR_IN_WHITELIST_URL
                                            + Locale.getDefault().getLanguage();
                                    view.loadUrl(filePath);
                                }
                            } else {
                                // load ssl error page without continue button
                                String filePath =
                                        SSL_ERROR_OTHER_URL + Locale.getDefault().getLanguage();
                                view.loadUrl(filePath);
                            }
                        } else {
                            Log.e(TAG, "onReceivedSslError in subframe, error url:" + url);
                            super.onReceivedSslError(view, handler, error);
                        }
                    }
                });

        setWebChromeClient(
                new WebChromeClient() {

                    @Override
                    public void onGeolocationPermissionsShowPrompt(
                            final String origin, final GeolocationPermissions.Callback callback) {
                        final HybridView hybridView =
                                getComponent() != null ? getComponent().getHybridView() : null;
                        if (hybridView == null) {
                            Log.e(TAG, "error: hybrid view is null.");
                            return;
                        }
                        final HybridManager hybridManager = hybridView.getHybridManager();
                        if (mLocationDialog != null) {
                            mLocationDialog.dismiss();
                        }
                        Resources res = getResources();
                        mLocationDialog = new CheckableAlertDialog(NestedWebView.this.getContext());
                        mLocationDialog.setTitle(res.getString(R.string.location_warn_title));
                        mLocationDialog.setMessage(res.getString(R.string.location_warn_message, origin));
                        mLocationDialog.setButton(DialogInterface.BUTTON_POSITIVE,
                                res.getString(R.string.location_warn_allow),
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                            String[] permissions =
                                                    new String[]{
                                                            Manifest.permission.ACCESS_COARSE_LOCATION,
                                                            Manifest.permission.ACCESS_FINE_LOCATION
                                                    };
                                            HapPermissionManager.getDefault()
                                                    .requestPermissions(
                                                            hybridManager,
                                                            permissions,
                                                            new PermissionCallback() {
                                                                @Override
                                                                public void onPermissionAccept() {
                                                                    callback.invoke(origin, true, true);
                                                                }

                                                                @Override
                                                                public void onPermissionReject(int reason, boolean dontDisturb) {
                                                                    callback.invoke(origin, false, false);
                                                                }
                                                            });
                                        } else {
                                            callback.invoke(origin, true, true);
                                        }
                                    }
                                });
                        mLocationDialog.setButton(DialogInterface.BUTTON_NEGATIVE,
                                res.getString(R.string.location_warn_reject),
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        callback.invoke(origin, false, false);
                                    }
                                });
                        DarkThemeUtil.disableForceDark(mLocationDialog);
                        mLocationDialog.show();
                    }

                    @Override
                    public void onReceivedTitle(WebView view, String title) {
                        super.onReceivedTitle(view, title);
                        if (mOnTitleReceiveListener != null) {
                            mOnTitleReceiveListener.onTitleReceive(view.getTitle());
                        }
                    }

                    // For Android 4.4
                    public void openFileChooser(
                            ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
                        if (mSingleFileCallback != null) {
                            mSingleFileCallback.onReceiveValue(null);
                        }
                        mSingleFileCallback = uploadMsg;
                        String[] types = new String[1];
                        types[0] = acceptType;
                        initChooseFile(
                                CHOOSE_LOW_API_MODE, types, false,
                                isCaptureEnabled(acceptType, capture));
                    }

                    private boolean isCaptureEnabled(String acceptType, String capture) {
                        boolean isCaptureEnabled = false;
                        if (!TextUtils.isEmpty(acceptType) && !TextUtils.isEmpty(capture)) {
                            if (acceptType.contains("image/") && "camera".equals(capture)) {
                                isCaptureEnabled = true;
                            } else if (acceptType.contains("video/")
                                    && "camcorder".equals(capture)) {
                                isCaptureEnabled = true;
                            } else if (acceptType.contains("audio/")
                                    && "microphone".equals(capture)) {
                                isCaptureEnabled = true;
                            }
                        }
                        return isCaptureEnabled;
                    }

                    // For Android 5.0 and above
                    @SuppressLint("NewApi")
                    @Override
                    public boolean onShowFileChooser(
                            WebView webView,
                            ValueCallback<Uri[]> filePathCallback,
                            final FileChooserParams fileChooserParams) {
                        if (mFilePathCallback != null) {
                            mFilePathCallback.onReceiveValue(null);
                        }
                        String[] types = null;
                        boolean isAllowMultiple = false;
                        boolean isCaptureEnabled = false;
                        if (null != fileChooserParams) {
                            types = fileChooserParams.getAcceptTypes();
                            isAllowMultiple =
                                    (fileChooserParams.getMode()
                                            == FileChooserParams.MODE_OPEN_MULTIPLE);
                            isCaptureEnabled = fileChooserParams.isCaptureEnabled();
                        }
                        mFilePathCallback = filePathCallback;
                        initChooseFile(CHOOSE_HIGH_API_MODE, types, isAllowMultiple,
                                isCaptureEnabled);
                        return true;
                    }

                    @Override
                    public void onShowCustomView(View view, CustomViewCallback callback) {
                        view.setBackgroundColor(getResources().getColor(android.R.color.black));
                        mFullScreenView = view;
                        if (mComponent != null) {
                            mComponent.setFullScreenView(mFullScreenView);
                            if (mComponent.getRootComponent() != null
                                    && mComponent.getRootComponent().getDecorLayout() != null) {
                                mComponent.getRootComponent().getDecorLayout().enterFullscreen(mComponent, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, false, false);
                            }
                        }
                    }

                    @Override
                    public void onHideCustomView() {
                        if (mFullScreenView != null) {
                            if (mComponent != null) {
                                if (mComponent.getRootComponent() != null && mComponent.getRootComponent().getDecorLayout() != null) {
                                    mComponent.getRootComponent().getDecorLayout().exitFullscreen();
                                }
                                mComponent.setFullScreenView(null);
                            }
                            mFullScreenView = null;
                        }
                    }

                    @Override
                    public void onProgressChanged(WebView view, int newProgress) {
                        super.onProgressChanged(view, newProgress);
                        if (mShowLoadingDialog && mProgressBar != null) {
                            mProgressBar.startProgress(newProgress,
                                    WebProgressBar.SET_PROGRESS_WITH_ANIMATE);
                        }
                        if (mOnProgressChangedListener != null) {
                            mOnProgressChangedListener.onProgressChanged(newProgress);
                        }
                    }

                    @Override
                    public void onPermissionRequest(PermissionRequest request) {
                        if (mContext instanceof Activity && ((Activity) mContext).isFinishing()) {
                            Log.e(TAG, "onPermissionRequest Activity is finishing,no permission dialog show.");
                            return;
                        }
                        String[] requestedResources;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                                && isSupportWebRTC()) {
                            requestedResources = request.getResources();
                            ArrayList<String> webRtcPermissions = new ArrayList<>();
                            for (String requestedResource : requestedResources) {
                                if (PermissionRequest.RESOURCE_AUDIO_CAPTURE
                                        .equalsIgnoreCase(requestedResource)) {
                                    if (!webRtcPermissions.contains(Manifest.permission.RECORD_AUDIO)) {
                                        webRtcPermissions.add(Manifest.permission.RECORD_AUDIO);
                                    }
                                } else if (PermissionRequest.RESOURCE_VIDEO_CAPTURE
                                        .equalsIgnoreCase(requestedResource)) {
                                    if (!webRtcPermissions.contains(Manifest.permission.CAMERA)) {
                                        webRtcPermissions.add(Manifest.permission.CAMERA);
                                    }
                                }
                            }
                            final HybridView hybridView =
                                    getComponent() != null ? getComponent().getHybridView() : null;
                            if (hybridView == null || hybridView.getHybridManager() == null) {
                                Log.e(TAG, "onPermissionRequest error: hybrid view or hybrid manager is null.");
                                return;
                            }
                            if (webRtcPermissions.isEmpty()) {
                                super.onPermissionRequest(request);
                                return;
                            }
                            final HybridManager hybridManager = hybridView.getHybridManager();
                            if (mWebRtcDialog != null) {
                                mWebRtcDialog.dismiss();
                            }
                            String warnMessage = null;
                            String host = request.getOrigin().getHost();
                            if (webRtcPermissions.contains(Manifest.permission.CAMERA)
                                    && webRtcPermissions.contains(Manifest.permission.RECORD_AUDIO)) {
                                        warnMessage = getResources().getString(R.string.webrtc_warn_permission_camera_and_microphone,
                                        host);
                            } else if (webRtcPermissions.contains(Manifest.permission.CAMERA)) {
                                warnMessage = getResources().getString(R.string.webrtc_warn_permission_camera,
                                        host);
                            } else if (webRtcPermissions.contains(Manifest.permission.RECORD_AUDIO)) {
                                warnMessage = getResources().getString(R.string.webrtc_warn_permission_microphone,
                                        host);
                            }
                            Resources res = getResources();
                            mWebRtcDialog = new CheckableAlertDialog(NestedWebView.this.getContext());
                            mWebRtcDialog.setTitle(res.getString(R.string.webrtc_warn_title));
                            mWebRtcDialog.setMessage(warnMessage);
                            mWebRtcDialog.setButton(
                                    DialogInterface.BUTTON_POSITIVE,
                                    res.getString(R.string.webrtc_warn_allow),
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            String[] notGrantedPermissionsArray = new String[webRtcPermissions.size()];
                                            notGrantedPermissionsArray = webRtcPermissions.toArray(notGrantedPermissionsArray);
                                            HapPermissionManager.getDefault()
                                                    .requestPermissions(hybridManager, notGrantedPermissionsArray,
                                                            new PermissionCallback() {
                                                                @Override
                                                                public void onPermissionAccept() {
                                                                    ThreadUtils.runOnUiThread(() -> {
                                                                        request.grant(request.getResources());
                                                                    });
                                                                }

                                                                @Override
                                                                public void onPermissionReject(int reason, boolean dontDisturb) {
                                                                    ThreadUtils.runOnUiThread(request::deny);
                                                                    StringBuilder builder =
                                                                            new StringBuilder("onPermissionReject reason:")
                                                                                    .append(reason)
                                                                                    .append(", request permission:");
                                                                    for (String temp : request.getResources()) {
                                                                        builder.append(temp).append(",");
                                                                    }
                                                                    Log.e(TAG, builder.toString());
                                                                }
                                                            });
                                        }
                                    });
                            mWebRtcDialog.setButton(
                                    DialogInterface.BUTTON_NEGATIVE,
                                    res.getString(R.string.webrtc_warn_reject),
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            request.deny();
                                        }
                                    });
                            DarkThemeUtil.disableForceDark(mWebRtcDialog);
                            mWebRtcDialog.show();
                        } else {
                            super.onPermissionRequest(request);
                        }
                    }

                    @Override
                    public Bitmap getDefaultVideoPoster() {
                        // default video poster
                        return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
                    }
                });

        setDownloadListener(
                new DownloadListener() {
                    @Override
                    public void onDownloadStart(
                            final String url,
                            final String userAgent,
                            final String contentDisposition,
                            final String mimetype,
                            long contentLength) {
                        if (mConfirmDialog != null) {
                            mConfirmDialog.dismiss();
                        }
                        if (mContext instanceof Activity && ((Activity) mContext).isFinishing()) {
                            Log.e(TAG,
                                    "onDownloadStart Activity is finishing,no download dialog show.");
                            return;
                        }
                        String fileName = URLUtil.guessFileName(url, Uri.decode(contentDisposition),
                                mimetype);

                        mConfirmDialog = new DownloadConfirmDialog(mContext);
                        mConfirmDialog.setContentView(R.layout.web_download_dialog);
                        TextView sizeTV = mConfirmDialog.findViewById(R.id.file_size);
                        final EditText nameET = mConfirmDialog.findViewById(R.id.file_name);
                        sizeTV.setText(
                                mContext.getString(
                                        R.string.web_dialog_file_size,
                                        FileUtils.formatFileSize(contentLength)));
                        if (!TextUtils.isEmpty(fileName)) {
                            nameET.setText(fileName);
                            nameET.setSelection(fileName.length());
                        }

                        mConfirmDialog.setTitle(R.string.web_dialog_save_file);
                        mConfirmDialog.setButton(
                                DialogInterface.BUTTON_POSITIVE,
                                R.string.text_ok,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        String downloadFileName =
                                                nameET.getText().toString().trim();
                                        if (TextUtils.isEmpty(downloadFileName)) {
                                            Toast.makeText(
                                                    mContext, R.string.web_download_invalid_url,
                                                    Toast.LENGTH_SHORT)
                                                    .show();
                                        } else if (!checkUrl(url)) {
                                            Toast.makeText(
                                                    mContext, R.string.web_download_no_file_name,
                                                    Toast.LENGTH_SHORT)
                                                    .show();
                                        } else {
                                            download(url, userAgent, contentDisposition, mimetype,
                                                    downloadFileName);
                                            mConfirmDialog.dismiss();
                                        }
                                    }
                                });
                        mConfirmDialog
                                .setButton(DialogInterface.BUTTON_NEGATIVE, R.string.text_cancel,
                                        null);
                        mConfirmDialog.show();
                    }
                });

        WebViewNativeApi nativeApi = new WebViewNativeApi();
        // Keep 'miui' package for compatible with api level 100
        addJavascriptInterface(nativeApi, "miui");
        addJavascriptInterface(nativeApi, "system");
        // only for jssdk
        H5InvokeNativeApi h5InvokeNativeApi = new H5InvokeNativeApi(this);
        addJavascriptInterface(h5InvokeNativeApi, "hapH5Invoke");
    }

    private boolean isInterceptUrl(String url) {
        ArraySet<String> interceptUrls = null;
        if (mComponent != null) {
            Web web = (Web) mComponent;
            interceptUrls = web.getInterceptUrls();
        }
        if (mComponent == null || TextUtils.isEmpty(url)
                || interceptUrls.size() == 0 || interceptUrls == null) {
            return false;
        }
        for (int i = 0; i < interceptUrls.size(); i++) {
            if (url.matches(interceptUrls.valueAt(i))) {
                return true;
            }
        }
        return false;
    }

    private boolean isDomainInWhitelist(String domain) {
        if (!TextUtils.isEmpty(domain)) {
            if (mTrustedSslDomains == null) {
                final HybridView hybridView =
                        getComponent() != null ? getComponent().getHybridView() : null;
                if (hybridView == null) {
                    Log.e(TAG, "error: hybrid view is null.");
                    return false;
                }
                final HybridManager hybridManager = hybridView.getHybridManager();
                AppInfo appInfo = hybridManager.getApplicationContext().getAppInfo();
                if (appInfo == null) {
                    Log.e(TAG, "error: AppInfo is null.");
                    return false;
                }
                mTrustedSslDomains = appInfo.getTrustedSslDomains();
            }
            return mTrustedSslDomains != null && mTrustedSslDomains.contains(domain);
        } else {
            Log.e(TAG, "error: check domain is null .");
        }
        return false;
    }

    private boolean isInAuthorizedDomains(String domain) {
        if (!TextUtils.isEmpty(domain) && mAuthorizedSslDomains != null) {
            return mAuthorizedSslDomains.contains(domain);
        }
        return false;
    }

    private String getDomain(String lastSslErrorUrl) {
        if (!TextUtils.isEmpty(lastSslErrorUrl)) {
            try {
                URL url = new URL(lastSslErrorUrl);
                return url.getHost();
            } catch (MalformedURLException e) {
                Log.e(TAG, "get domain error", e);
            }
        }
        return null;
    }

    private boolean checkUrl(String url) {
        if (TextUtils.isEmpty(url)) {
            return false;
        }
        Uri uri = Uri.parse(url);
        String scheme = uri.getScheme();
        return scheme != null && ("http".equals(scheme) || "https".equals(scheme));
    }

    private void initChooseFile(
            final int apiType, String[] types, boolean isAllowMultiple, boolean isCaptureEnabled) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        addMimeTypes(types, intent);
        if (isAllowMultiple && apiType == CHOOSE_HIGH_API_MODE) {
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        }
        if (mContext instanceof Activity) {
            String curMimeType = intent.getType();
            int chooseMode = CHOOSE_MODE_DEFAULT;
            if (TextUtils.isEmpty(curMimeType)) {
                chooseMode = CHOOSE_MODE_EMPTY;
            } else if (!curMimeType.contains("image")
                    && !curMimeType.contains("video")
                    && !curMimeType.contains("audio")) {
                // no video image audio
                chooseMode = CHOOSE_MODE_SPECIAL;
            }
            // take photo
            Intent takePhoto = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            try {
                if (null != getComponent() && getComponent().getCallback() != null) {
                    RenderEventCallback callback = getComponent().getCallback();
                    mCachePhotoFile = callback.createFileOnCache("photo", ".jpg");
                    Uri scrapUri =
                            FileProvider.getUriForFile(
                                    mContext, mContext.getPackageName() + ".file", mCachePhotoFile);
                    takePhoto.putExtra(MediaStore.EXTRA_OUTPUT, scrapUri);
                    takePhoto.setClipData(
                            ClipData.newUri(mContext.getContentResolver(), "takePhoto", scrapUri));
                    takePhoto.setFlags(
                            Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET
                                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                    | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
            } catch (IOException e) {
                Log.e(TAG, "init choose file error", e);
            }
            // video record
            Intent captureVideo = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
            try {
                if (null != getComponent() && getComponent().getCallback() != null) {
                    RenderEventCallback callback = getComponent().getCallback();
                    mCacheVideoFile = callback.createFileOnCache("video", ".mp4");
                    Uri scrapUri =
                            FileProvider.getUriForFile(
                                    mContext, mContext.getPackageName() + ".file", mCacheVideoFile);
                    captureVideo.setClipData(
                            ClipData.newUri(mContext.getContentResolver(), "takeVideo", scrapUri));
                    captureVideo.putExtra(MediaStore.EXTRA_OUTPUT, scrapUri);
                    captureVideo.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
                    captureVideo.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 60);
                    captureVideo.setFlags(
                            Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET
                                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                    | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
            } catch (IOException e) {
                Log.e(TAG, "init choose file error", e);
            }
            // audio record
            Intent audioIntent = null;
            SysOpProvider provider = ProviderManager.getDefault().getProvider(SysOpProvider.NAME);
            if (null == provider || null == provider.getAudioIntent()) {
                audioIntent = new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);
            } else {
                audioIntent = provider.getAudioIntent();
            }
            // file
            Intent fileIntent = new Intent(Intent.ACTION_GET_CONTENT);
            fileIntent.setType("*/*");
            Intent chooserIntent = null;
            if (mMinPlatformVersion <= 0) {
                final HybridView hybridView =
                        getComponent() != null ? getComponent().getHybridView() : null;
                if (hybridView == null) {
                    Log.e(TAG, "error: hybrid view is null.");
                    return;
                }
                final HybridManager hybridManager = hybridView.getHybridManager();
                if (hybridManager.getHapEngine() != null
                        && hybridManager.getHapEngine().getApplicationContext() != null
                        &&
                        hybridManager.getHapEngine().getApplicationContext().getAppInfo() != null) {
                    mMinPlatformVersion =
                            hybridManager
                                    .getHapEngine()
                                    .getApplicationContext()
                                    .getAppInfo()
                                    .getMinPlatformVersion();
                }
            }
            // 1090 support capture
            if (isCaptureEnabled && mMinPlatformVersion >= MIN_PLATFORM_VERSION_1090) {
                if (chooseMode == CHOOSE_MODE_DEFAULT) {
                    if (!TextUtils.isEmpty(curMimeType)) {
                        if (curMimeType.contains("image")) {
                            chooserIntent = takePhoto;
                        } else if (curMimeType.contains("video")) {
                            chooserIntent = captureVideo;
                        } else if (curMimeType.contains("audio")) {
                            chooserIntent = audioIntent;
                        }
                    }
                }
            }
            if (chooserIntent == null) {
                chooserIntent = Intent.createChooser(intent, null);
                if (chooseMode == CHOOSE_MODE_EMPTY) {
                    chooserIntent = Intent.createChooser(fileIntent, null);
                    chooserIntent.putExtra(
                            Intent.EXTRA_INITIAL_INTENTS,
                            new Intent[]{takePhoto, captureVideo, audioIntent});
                } else if (chooseMode == CHOOSE_MODE_SPECIAL) {
                    chooserIntent = Intent.createChooser(fileIntent, null);
                    chooserIntent.putExtra(
                            Intent.EXTRA_INITIAL_INTENTS,
                            new Intent[]{takePhoto, captureVideo, audioIntent});
                } else if (chooseMode == CHOOSE_MODE_DEFAULT) {
                    if (!TextUtils.isEmpty(curMimeType)) {
                        if (curMimeType.contains("image")) {
                            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS,
                                    new Intent[]{takePhoto});
                        } else if (curMimeType.contains("video")) {
                            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS,
                                    new Intent[]{captureVideo});
                        } else if (curMimeType.contains("audio")
                                && !"audio/*".equals(curMimeType)) {
                            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS,
                                    new Intent[]{audioIntent});
                        } else if ("audio/*".equals(curMimeType)) {
                            chooserIntent = Intent.createChooser(fileIntent, null);
                            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS,
                                    new Intent[]{audioIntent});
                        } else {
                            Log.w(TAG, "initChooseFile: curMimeType do not fit any case");
                        }
                    } else {
                        Log.w(TAG, "initChooseFile: curMimeType is empty");
                    }
                }
            }
            if (null == curMimeType
                    || curMimeType.contains("image")
                    || curMimeType.contains("video")
                    || chooseMode == CHOOSE_MODE_SPECIAL
                    || chooseMode == CHOOSE_MODE_EMPTY) {
                checkCameraPermission(chooserIntent, REQUEST_FILE_CODE, apiType);
            } else {
                if (apiType == CHOOSE_LOW_API_MODE) {
                    resolveLowApiResult();
                } else {
                    resolveHighApiResult();
                }
                ((Activity) mContext).startActivityForResult(chooserIntent, REQUEST_FILE_CODE);
            }
        }
    }

    private void checkCameraPermission(
            final Intent chooserIntent, final int code, final int apiType) {
        final HybridView hybridView =
                getComponent() != null ? getComponent().getHybridView() : null;
        if (hybridView == null) {
            Log.e(TAG, "error: hybrid view is null.");
            return;
        }
        final HybridManager hybridManager = hybridView.getHybridManager();
        HapPermissionManager.getDefault()
                .requestPermissions(
                        hybridManager,
                        new String[]{Manifest.permission.CAMERA},
                        new PermissionCallback() {
                            @Override
                            public void onPermissionAccept() {
                                NestedWebView.this.post(
                                        new Runnable() {
                                            @Override
                                            public void run() {
                                                if (apiType == CHOOSE_LOW_API_MODE) {
                                                    resolveLowApiResult();
                                                } else {
                                                    resolveHighApiResult();
                                                }
                                                ((Activity) mContext)
                                                        .startActivityForResult(chooserIntent,
                                                                code);
                                            }
                                        });
                            }

                            @Override
                            public void onPermissionReject(int reason, boolean dontDisturb) {
                                Log.d(TAG, "camera permission deny.");
                                NestedWebView.this.post(
                                        new Runnable() {
                                            @Override
                                            public void run() {
                                                if (mFilePathCallback != null) {
                                                    mFilePathCallback.onReceiveValue(null);
                                                    mFilePathCallback = null;
                                                }
                                            }
                                        });
                            }
                        });
    }

    @SuppressLint("NewApi")
    private void resolveHighApiResult() {
        final HybridView hybridView =
                getComponent() != null ? getComponent().getHybridView() : null;
        if (hybridView == null) {
            Log.e(TAG, "error: hybrid view is null.");
            return;
        }
        final HybridManager hybridManager = hybridView.getHybridManager();
        hybridManager.addLifecycleListener(
                new LifecycleListener() {
                    @Override
                    public void onActivityResult(int requestCode, int resultCode, Intent data) {
                        if (requestCode == REQUEST_FILE_CODE) {
                            Uri[] result = null;
                            Uri[] tmpResults = new Uri[1];
                            if (resultCode == Activity.RESULT_OK) {
                                if (null != data) {
                                    result = WebChromeClient.FileChooserParams
                                            .parseResult(resultCode, data);
                                }
                                if (result == null) {
                                    result = WebViewUtils.getFileUriList(data);
                                }
                                if (result == null) {
                                    // take phone or video {
                                    if (null != getComponent()
                                            && getComponent().getCallback() != null) {
                                        if (null != mCachePhotoFile
                                                && mCachePhotoFile.exists()
                                                && mCachePhotoFile.length() > 0) {
                                            tmpResults[0] = Uri.fromFile(mCachePhotoFile);
                                        }
                                        mCachePhotoFile = null;
                                        if (null != mCacheVideoFile
                                                && mCacheVideoFile.exists()
                                                && mCacheVideoFile.length() > 0) {
                                            tmpResults[0] = Uri.fromFile(mCacheVideoFile);
                                        }
                                        mCacheVideoFile = null;
                                    }
                                    result = tmpResults;
                                }
                            }
                            if (null != result && result.length > 0 && result[0] == null) {
                                Log.e(
                                        TAG,
                                        "resolveHighApiResult parseResult canceled or any other "
                                                + "  length : "
                                                + result.length);
                                result = new Uri[0];
                            }
                            if (null != mFilePathCallback) {
                                mFilePathCallback.onReceiveValue(result);
                            }
                            tmpResults[0] = null;
                            mFilePathCallback = null;
                            hybridManager.removeLifecycleListener(this);
                        }
                    }
                });
    }

    private void resolveLowApiResult() {
        final HybridView hybridView =
                getComponent() != null ? getComponent().getHybridView() : null;
        if (hybridView == null) {
            Log.e(TAG, "error: hybrid view is null.");
            return;
        }
        final HybridManager hybridManager = hybridView.getHybridManager();
        hybridManager.addLifecycleListener(
                new LifecycleListener() {
                    @Override
                    public void onActivityResult(int requestCode, int resultCode, Intent data) {
                        if (requestCode == REQUEST_FILE_CODE) {
                            Uri result = null;
                            Uri tmpResults = null;
                            if (resultCode == Activity.RESULT_OK) {
                                if (data != null) {
                                    result = data.getData();
                                }
                                if (null == data || (null != data && data.getData() == null)) {
                                    if (null != getComponent()
                                            && getComponent().getCallback() != null) {
                                        if (null != mCachePhotoFile
                                                && mCachePhotoFile.exists()
                                                && mCachePhotoFile.length() > 0) {
                                            tmpResults = Uri.fromFile(mCachePhotoFile);
                                        }
                                        mCachePhotoFile = null;
                                        if (null != mCacheVideoFile
                                                && mCacheVideoFile.exists()
                                                && mCacheVideoFile.length() > 0) {
                                            tmpResults = Uri.fromFile(mCacheVideoFile);
                                        }
                                        mCacheVideoFile = null;
                                    }
                                    result = tmpResults;
                                }
                            }
                            if (null != mSingleFileCallback) {
                                mSingleFileCallback.onReceiveValue(result);
                            }
                            mSingleFileCallback = null;
                            tmpResults = null;
                            hybridManager.removeLifecycleListener(this);
                        }
                    }
                });
    }

    private void addMimeTypes(String[] typeStrs, Intent intent) {
        if (null != intent && null != typeStrs && typeStrs.length > 0) {
            String[] mimeTypeGroup = new String[typeStrs.length];
            HashSet<String> mimeTypeSet = new HashSet<String>();
            int allSize = typeStrs.length;
            String tmpTypeStr = "";
            String mimeTypeStr = "";
            int j = 0;
            for (int i = 0; i < allSize; i++) {
                tmpTypeStr = typeStrs[i];
                tmpTypeStr = tmpTypeStr.trim();
                if (tmpTypeStr.indexOf(".") == 0) {
                    tmpTypeStr = tmpTypeStr.substring(1);
                    mimeTypeStr = MimeTypeMap.getSingleton().getMimeTypeFromExtension(tmpTypeStr);
                    if (!TextUtils.isEmpty(mimeTypeStr)) {
                        if (!mimeTypeSet.contains(mimeTypeStr)) {
                            mimeTypeGroup[j] = mimeTypeStr;
                            mimeTypeSet.add(mimeTypeStr);
                            j++;
                        }
                    }
                } else {
                    if (tmpTypeStr.contains("/")) {
                        if (!mimeTypeSet.contains(tmpTypeStr)) {
                            mimeTypeGroup[j] = tmpTypeStr;
                            mimeTypeSet.add(tmpTypeStr);
                            j++;
                        }
                    }
                }
            }
            if (!TextUtils.isEmpty(mimeTypeGroup[0])) {
                intent.setType(mimeTypeGroup[0]);
            }
        }
    }

    private void download(
            String url, String userAgent, String contentDisposition, String mimetype,
            String fileName) {
        if (TextUtils.isEmpty(url)) {
            Log.e(TAG, "error: url is empty.");
            return;
        }
        final DownloadManager.Request request =
                buildDownloadRequest(Uri.parse(url), userAgent, contentDisposition, mimetype,
                        fileName);
        if (request == null) {
            Log.e(TAG, "error: request is invalid.");
            return;
        }
        final Activity act = (Activity) mContext;
        if (act == null) {
            Log.e(TAG, "error: mContext is not an instance of Activity.");
            return;
        }
        final HybridView hybridView =
                getComponent() != null ? getComponent().getHybridView() : null;
        if (hybridView == null) {
            Log.e(TAG, "error: hybrid view is null.");
            return;
        }
        final DownloadManager downloadManager =
                (DownloadManager) getContext().getSystemService(Context.DOWNLOAD_SERVICE);
        if (downloadManager == null) {
            Log.e(TAG, "error: can not get download manager.");
            return;
        }

        request.setTitle(fileName);
        File downloadDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File downloadFile = new File(downloadDir, fileName);
        request.setDestinationUri(Uri.fromFile(downloadFile));

        if (Build.VERSION.SDK_INT >= 23) {
            if (act.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                downloadManager.enqueue(request);
            } else {
                ActivityCompat.requestPermissions(
                        act,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_WRITE_PERMISSION);

                final HybridManager hybridManager = hybridView.getHybridManager();
                hybridManager.addLifecycleListener(
                        new LifecycleListener() {

                            @Override
                            public void onRequestPermissionsResult(
                                    int requestCode, String[] permissions, int[] grantResults) {
                                super.onRequestPermissionsResult(requestCode, permissions,
                                        grantResults);
                                if (requestCode == REQUEST_WRITE_PERMISSION
                                        && grantResults != null
                                        && grantResults.length > 0
                                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                                    downloadManager.enqueue(request);
                                } else {
                                    Toast.makeText(
                                            act,
                                            getResources()
                                                    .getString(R.string.web_download_no_permission),
                                            Toast.LENGTH_SHORT)
                                            .show();
                                }
                                hybridManager.removeLifecycleListener(this);
                            }
                        });
            }
        } else {
            downloadManager.enqueue(request);
        }
    }

    protected DownloadManager.Request buildDownloadRequest(
            Uri uri, String userAgent, String contentDisposition, String mimetype,
            String fileName) {
        DownloadManager.Request request = null;
        try {
            request = new DownloadManager.Request(uri);

            request.allowScanningByMediaScanner();

            request.addRequestHeader("User-Agent", userAgent);
            request.addRequestHeader("Content-Disposition", contentDisposition);

            request.setMimeType(mimetype);

            request.setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        } catch (Exception e) {
            Log.e(TAG, "buildDownloadRequest Exception: ", e);
        }

        return request;
    }

    @Override
    public Component getComponent() {
        return mComponent;
    }

    @Override
    public void setComponent(Component component) {
        mComponent = component;
        setUserAgent(KEY_DEFAULT);
    }

    @Override
    public IGesture getGesture() {
        return mGesture;
    }

    @Override
    public void setGesture(IGesture gestureDelegate) {
        mGesture = gestureDelegate;
    }

    public boolean isSupportWebRTC() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P;
    }

    private String getAppPkg() {
        final HybridView hybridView =
                getComponent() != null ? getComponent().getHybridView() : null;
        if (hybridView == null) {
            Log.e(TAG, "error: hybrid view is null.");
            return null;
        }
        final HybridManager hybridManager = hybridView.getHybridManager();
        if (hybridManager.getHapEngine() == null) {
            Log.e(TAG, "error: hybrid hap engine is null.");
            return null;
        }
        return hybridManager.getHapEngine().getPackage();
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        boolean returnValue = false;
        if (mGesture != null) {
            // web组件只返回MotionEvent数据，不影响webview内部TouchEvent逻辑
            mGesture.onTouch(ev);
        }
        MotionEvent event = MotionEvent.obtain(ev);
        final int action = MotionEventCompat.getActionMasked(event);
        if (action == MotionEvent.ACTION_DOWN) {
            mNestedOffsetY = 0;
        }
        int eventY = (int) event.getY();
        event.offsetLocation(0, mNestedOffsetY);
        switch (action) {
            case MotionEvent.ACTION_MOVE:
                int deltaY = mLastY - eventY;
                // NestedPreScroll
                if (dispatchNestedPreScroll(0, deltaY, mScrollConsumed, mScrollOffset)) {
                    deltaY -= mScrollConsumed[1];
                    event.offsetLocation(0, mScrollOffset[1]);
                    mNestedOffsetY += mScrollOffset[1];
                }

                mLastY = eventY - mScrollOffset[1];

                int oldY = getScrollY();
                int newScrollY = Math.max(0, oldY + deltaY);
                int dyConsumed = Math.min(mScrollRange - oldY, newScrollY - oldY);
                int dyUnconsumed = deltaY - dyConsumed;

                // NestedScroll
                if (dispatchNestedScroll(0, dyConsumed, 0, dyUnconsumed, mScrollOffset)) {
                    event.offsetLocation(0, mScrollOffset[1]);
                    mNestedOffsetY += mScrollOffset[1];
                    mLastY -= mScrollOffset[1];
                }
                returnValue = super.onTouchEvent(event);
                break;
            case MotionEvent.ACTION_DOWN:
                returnValue = super.onTouchEvent(event);
                mLastY = eventY;
                initOrResetVelocityTracker();
                mActivePointerId = ev.getPointerId(0);
                mScrollRange = (int) (getScale() * getContentHeight() - getHeight());
                // start NestedScroll
                startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                returnValue = super.onTouchEvent(event);

                if (mScrollRange == 0 || mScrollOffset[1] != 0) {
                    final VelocityTracker velocityTracker = mVelocityTracker;
                    velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                    int initialVelocity =
                            (int) VelocityTrackerCompat
                                    .getYVelocity(velocityTracker, mActivePointerId);
                    if ((Math.abs(initialVelocity) > mMinimumVelocity)) {
                        if (mNestedScrollingListener != null) {
                            mNestedScrollingListener.onFling(0, -initialVelocity);
                        }
                    }
                }
                // end NestedScroll
                stopNestedScroll();
                recycleVelocityTracker();

                break;
            default:
                return super.onTouchEvent(event);
        }

        if (mVelocityTracker != null) {
            mVelocityTracker.addMovement(event);
        }
        event.recycle();
        return returnValue;
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        if (mProgressBar != null) {
            mProgressBar.scrollBy(l - oldl, oldt - t);
        }
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return mChildHelper.isNestedScrollingEnabled();
    }

    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        mChildHelper.setNestedScrollingEnabled(enabled);
    }

    @Override
    public boolean startNestedScroll(int axes) {
        return mChildHelper.startNestedScroll(axes);
    }

    @Override
    public void stopNestedScroll() {
        mChildHelper.stopNestedScroll();
    }

    @Override
    public boolean hasNestedScrollingParent() {
        return mChildHelper.hasNestedScrollingParent();
    }

    @Override
    public boolean dispatchNestedScroll(
            int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed,
            int[] offsetInWindow) {
        return mChildHelper.dispatchNestedScroll(
                dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
        return mChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        return mChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        return mChildHelper.dispatchNestedPreFling(velocityX, velocityY);
    }

    private boolean webCanGoBack() {
        boolean canGoBack = super.canGoBack();
        // http error will add web history list
        if (canGoBack) {
            WebBackForwardList webBackForwardList = copyBackForwardList();
            int currentIndex = webBackForwardList.getCurrentIndex();
            WebHistoryItem currentHistoryItem = webBackForwardList.getCurrentItem();
            int lastIndex = currentIndex - 1;
            if (lastIndex < 0) {
                return false;
            }
            WebHistoryItem lastHistoryItem = webBackForwardList.getItemAtIndex(lastIndex);
            if (isLocalHttpErrorHistoryItem(currentHistoryItem)) {
                // current load item is http error page
                if (currentIndex <= 1) {
                    canGoBack = false;
                }
            }

            if (isLocalSslErrorHistoryItem(lastHistoryItem)) {
                // last load item is ssl error page
                if (lastIndex < 1) {
                    canGoBack = false;
                }
            }
        }
        return canGoBack;
    }

    private boolean isLocalHttpErrorHistoryItem(WebHistoryItem historyItem) {
        if (historyItem != null && historyItem.getOriginalUrl() != null) {
            return historyItem.getOriginalUrl().contains(HTTP_ERROR_ULR);
        }
        return false;
    }

    private boolean isLocalSslErrorHistoryItem(WebHistoryItem historyItem) {
        if (historyItem != null && historyItem.getOriginalUrl() != null) {
            return historyItem.getOriginalUrl().contains(SSL_ERROR_ULR);
        }
        return false;
    }

    private void webInternalGoBack() {
        if (webCanGoBack()) {
            WebBackForwardList webBackForwardList = copyBackForwardList();
            int currentIndex = webBackForwardList.getCurrentIndex();
            WebHistoryItem currentHistoryItem = webBackForwardList.getCurrentItem();
            int lastIndex = currentIndex - 1;
            WebHistoryItem lastHistoryItem = webBackForwardList.getItemAtIndex(lastIndex);
            if (isLocalHttpErrorHistoryItem(currentHistoryItem)) {
                // current load item is http error page
                if (currentIndex > 1) {
                    goBackOrForward(LOCAL_ERROR_PAGE_BACK_STEP);
                } else {
                    webExit();
                }
            } else if (isLocalSslErrorHistoryItem(lastHistoryItem)) {
                // last load item is ssl error page
                if (lastIndex >= 1) {
                    goBackOrForward(LOCAL_ERROR_PAGE_BACK_STEP);
                } else {
                    webExit();
                }
            } else {
                super.goBack();
            }
        } else {
            webExit();
        }
    }

    private void webExit() {
        final HybridView hybridView =
                getComponent() != null ? getComponent().getHybridView() : null;
        if (hybridView != null) {
            hybridView.goBack();
        } else {
            Log.e(TAG, "error: hybrid view is null.");
        }
    }

    @Override
    public boolean canGoBack() {
        return webCanGoBack();
    }

    @Override
    public void goBack() {
        if (webCanGoBack()) {
            webInternalGoBack();
        } else {
            Log.e(TAG, "WebView can not go back");
        }
    }

    @Override
    public boolean shouldScrollFirst(int dy, int velocityY) {
        return true;
    }

    @Override
    public boolean nestedFling(int velocityX, int velocityY) {
        flingScroll(velocityX, velocityY);
        return true;
    }

    @Override
    public NestedScrollingListener getNestedScrollingListener() {
        return null;
    }

    @Override
    public void setNestedScrollingListener(NestedScrollingListener listener) {
        mNestedScrollingListener = listener;
    }

    @Override
    public ViewGroup getChildNestedScrollingView() {
        return null;
    }

    private void initOrResetVelocityTracker() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        } else {
            mVelocityTracker.clear();
        }
    }

    private void recycleVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    public enum WebViewErrorType {
        NORMAL(0),
        HTTP(1),
        SSL(2),
        UNKNOWN(255);
        int value;

        WebViewErrorType(int value) {
            this.value = value;
        }

        public static WebViewErrorType find(int value) {
            for (WebViewErrorType type : values()) {
                if (type.getValue() == value) {
                    return type;
                }
            }
            return UNKNOWN;
        }

        public int getValue() {
            return value;
        }
    }

    public void setUserAgent(String userAgent) {
        if (mSettings == null) {
            mSettings = getSettings();
        }
        if (TextUtils.isEmpty(userAgent) || KEY_DEFAULT.equalsIgnoreCase(userAgent)) {
            // hap userAgent
            mSettings.setUserAgentString(UserAgentHelper.getFullWebkitUserAgent(getAppPkg()));
        } else if (KEY_SYSTEM.equalsIgnoreCase(userAgent)) {
            // system userAgent
            mSettings.setUserAgentString(UserAgentHelper.getWebkitUserAgentSegment());
        } else {
            // custom userAgent
            mSettings.setUserAgentString(userAgent);
        }
    }

    public interface OnPageStartListener {
        void onPageStart(String url, boolean canGoBack, boolean canGoForward);
    }

    public interface OnShouldOverrideUrlLoadingListener {
        void onShouldOverrideUrlLoading(WebView view, String url);
    }

    public interface OnPageFinishListener {
        void onPageFinish(String url, boolean canGoBack, boolean canGoForward);
    }

    public interface OnTitleReceiveListener {
        void onTitleReceive(String title);
    }

    public interface OnErrorListener {
        void onError(
                String message,
                String url,
                boolean canBack,
                boolean canGoForward,
                WebViewErrorType type,
                int code,
                String description,
                boolean isAuthorized);
    }

    public interface OnMessageListener {
        void onMessage(String message, String url);
    }

    public interface OnProgressChangedListener {
        void onProgressChanged(int i);
    }

    private static class H5InvokeNativeApi {
        private WeakReference<NestedWebView> mWebViewRef;
        private final String QUICK_APP = "quickapp";
        private final String PARAM_FUNCTION_NAMES = "functionNames";

        public H5InvokeNativeApi(NestedWebView webView) {
            mWebViewRef = new WeakReference<>(webView);
        }

        private static String formatResultString(String[] jsCallbackIds, Response response) {
            if (jsCallbackIds == null) {
                return null;
            }
            // H5 page invoke scan feature, jsCallbackId: index:0 --> success, index:1 --> fail, index:2 ---> cancel, index:3 ---> complete
            String successId = "", failId = "", cancelId = "", completeId = "";
            if (jsCallbackIds.length > 0) {
                successId = jsCallbackIds[0];
            }
            if (jsCallbackIds.length > 1) {
                failId = jsCallbackIds[1];
            }
            if (jsCallbackIds.length > 2) {
                cancelId = jsCallbackIds[2];
            }
            if (jsCallbackIds.length > 3) {
                completeId = jsCallbackIds[3];
            }
            if (response != null) {
                Object params = null;
                try {
                    if (response.getSerializeType() == Serializable.TYPE_JSON) {
                        params = response.toJSON().get("content");
                    } else {
                        params = response.toSerializeObject().toMap().get("content");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "formatResultString error", e);
                }
                int code = response.getCode();
                if (code == Response.CODE_SUCCESS && !TextUtils.isEmpty(successId)) {
                    if (params instanceof String) {
                        return String.format("javascript:_hapInternalCall(%s, \"%s\")", successId, params);
                    }
                    return String.format("javascript:_hapInternalCall(%s, %s)", successId, params);
                } else if (code == Response.CODE_CANCEL && !TextUtils.isEmpty(cancelId)) {
                    if (params instanceof String) {
                        return String.format("javascript:_hapInternalCall(%s, \"%s\")", cancelId, params);
                    }
                    return String.format("javascript:_hapInternalCall(%s, %s)", cancelId, params);
                } else if ((code >= 200 || code < 0) && !TextUtils.isEmpty(failId)) {
                    if (params instanceof String) {
                        return String.format("javascript:_hapInternalCall(%s, \"%s\", %s)", failId, params, code);
                    }
                    return String.format("javascript:_hapInternalCall(%s, %s, %s)", failId, params, code);
                }
            }
            if (!TextUtils.isEmpty(completeId)) {
                return String.format("javascript:_hapInternalCall(%s)", completeId);
            }
            return null;
        }

        /**
         * h5 support function name list
         *
         * @param callbacks
         */

        @JavascriptInterface
        public void getH5FunctionNameList(String callbacks) {
            final NestedWebView nestedWebView = mWebViewRef.get();
            if (nestedWebView == null) {
                Log.w(TAG, "H5 invoke fail, WebView is null");
                return;
            }
            final Web web = (Web) nestedWebView.getComponent();
            if (web == null) {
                Log.w(TAG, "H5 invoke fail, Component is null");
                return;
            }
            String[] jsCallbackIds = callbacks.split(",");
            nestedWebView.post(() -> {
                // check invoke security
                WebViewUtils.checkHandleMessage(nestedWebView, nestedWebView.getUrl(), web.getTrustedUlrs(),
                        new WebViewUtils.UrlCheckListener() {
                            @Override
                            public void onTrusted() {
                                nestedWebView.post(() -> {
                                    JSONObject jsonObject = new JSONObject();
                                    List<String> functionNameList = nestedWebView.getWebViewSettingProvider() != null ?
                                            nestedWebView.getWebViewSettingProvider().getJsFunctionNameList() :
                                            null;
                                    try {
                                        JSONArray jsonArray = new JSONArray(functionNameList);
                                        jsonObject.put(PARAM_FUNCTION_NAMES, jsonArray);
                                    } catch (JSONException e) {
                                        Log.e(TAG, "JsonObject error", e);
                                    }
                                    nestedWebView.loadUrl(formatResultString(jsCallbackIds,
                                            new Response(Response.CODE_SUCCESS, jsonObject)));
                                });
                            }

                            @Override
                            public void onUnTrusted() {
                                Log.e(TAG, "H5 invoke fail, url not trusted");
                                String responseResult = formatResultString(jsCallbackIds,
                                        new Response(CallbackError.UNTRUSTED_ERROR, ERROR_MSG_URL_UNTRUSTED));
                                if (!TextUtils.isEmpty(responseResult)) {
                                    nestedWebView.post(() -> nestedWebView.loadUrl(responseResult));
                                } else {
                                    Log.e(TAG, "invoke fail untrusted, response result is null");
                                }
                            }
                        });
            });
        }

        /**
         * h5 call native feature by function name
         *
         * @param functionName feature function name
         * @param rawParams    feature params
         * @param callbacks    feature callbacks
         */
        @JavascriptInterface
        public void specialH5InvokeNative(String functionName, String rawParams, String callbacks) {
            final NestedWebView nestedWebView = mWebViewRef.get();
            if (nestedWebView == null) {
                Log.w(TAG, "H5 invoke fail, WebView is null");
                return;
            }
            final Web web = (Web) nestedWebView.getComponent();
            if (web == null) {
                Log.w(TAG, "H5 invoke fail, Component is null");
                return;
            }
            String[] jsCallbackIds = callbacks.split(",");
            nestedWebView.post(() -> {
                // check invoke security
                WebViewUtils.checkHandleMessage(nestedWebView, nestedWebView.getUrl(), web.getTrustedUlrs(), new WebViewUtils.UrlCheckListener() {
                    @Override
                    public void onTrusted() {
                        // H5 page invoke scan feature, jsCallbackId: index:0 --> success, index:1 --> fail, index:2 ---> cancel, index:3--->complete
                        if (WebviewSettingProvider.FUNCTION_GET_ENV.equals(functionName)) {
                            nestedWebView.post(() -> {
                                JSONObject jsonObject = new JSONObject();
                                try {
                                    jsonObject.put(QUICK_APP, true);
                                } catch (JSONException e) {
                                    Log.e(TAG, "JsonObject error", e);
                                }
                                nestedWebView.loadUrl(formatResultString(jsCallbackIds, new Response(Response.CODE_SUCCESS, jsonObject)));
                            });
                        } else if (WebviewSettingProvider.FUNCTION_SCAN.equals(functionName)) {
                            executeNativeFunction("system.barcode", WebviewSettingProvider.FUNCTION_SCAN, jsCallbackIds, rawParams, nestedWebView);
                        } else {
                            Log.e(TAG, "H5 invoke fail, function not support");
                            String responseResult = formatResultString(jsCallbackIds, new Response(CallbackError.UNSUPPORT_ERROR, "not support"));
                            if (!TextUtils.isEmpty(responseResult)) {
                                nestedWebView.post(() -> nestedWebView.loadUrl(responseResult));
                            } else {
                                Log.e(TAG, "invoke fail untrusted, response result is null");
                            }
                        }
                    }

                    @Override
                    public void onUnTrusted() {
                        Log.e(TAG, "H5 invoke fail, url not trusted");
                        String responseResult = formatResultString(jsCallbackIds, new Response(CallbackError.UNSUPPORT_ERROR, "untrusted"));
                        if (!TextUtils.isEmpty(responseResult)) {
                            nestedWebView.post(() -> nestedWebView.loadUrl(responseResult));
                        } else {
                            Log.e(TAG, "invoke fail untrusted, response result is null");
                        }
                    }
                });
            });
        }

        private void executeNativeFunction(String module, String functionName, String[] jsCallbackIds,
                                           String rawParams, NestedWebView nestedWebView) {
            String responseResult;
            if (nestedWebView == null || nestedWebView.getComponent() == null) {
                Log.e(TAG, "invoke fail, nestedWebView or nestedWebView component is null");
                return;
            }
            // H5 page invoke scan feature, jsCallbackId: index:0 --> success, index:1 --> fail, index:2 ---> cancel, index:3--->complete
            DocComponent docComponent = nestedWebView.getComponent().getRootComponent();
            if (docComponent == null) {
                Log.e(TAG, "invoke fail, DocComponent is null");
                return;
            }
            RootView rootView = (RootView) docComponent.getHostView();
            if (rootView == null || rootView.getJsThread() == null || rootView.getJsThread().getBridgeManager() == null) {
                Log.e(TAG, "invoke fail, RootView Illegal status");
                return;
            }
            if (TextUtils.isEmpty(module) || TextUtils.isEmpty(functionName)) {
                responseResult = formatResultString(jsCallbackIds, new Response(CallbackError.PARAMS_ERROR, "params is wrong"));
            } else {
                ExtensionManager extensionManager = rootView.getJsThread().getBridgeManager();
                if (extensionManager != null) {
                    FeatureInnerBridge.invokeWithCallback(extensionManager, module, functionName, rawParams,
                            FeatureInnerBridge.H5_JS_CALLBACK, -1, new H5Callback(extensionManager, jsCallbackIds, mWebViewRef));
                } else {
                    Log.e(TAG, "invoke fail, extensionManager is null, module :  " + module
                            + ",functionName:" + functionName + ",response result is null");
                }
                return;
            }
            if (!TextUtils.isEmpty(responseResult)) {
                nestedWebView.post(() -> nestedWebView.loadUrl(responseResult));
            } else {
                Log.e(TAG, "invoke fail, module:" + module + ",functionName:" + functionName + ",response result is null");
            }
        }

        private interface CallbackError {
            int UNTRUSTED_ERROR = -1;
            int PARAMS_ERROR = -2;
            int UNSUPPORT_ERROR = -3;
        }

        private static class H5Callback extends Callback {
            private String[] mJsCallbackIds;
            private WeakReference<NestedWebView> mWeakReference;

            public H5Callback(ExtensionManager extensionManager, String[] jsCallbackIds, WeakReference<NestedWebView> weakReference) {
                super(extensionManager, FeatureInnerBridge.H5_JS_CALLBACK, Extension.Mode.ASYNC);
                mJsCallbackIds = jsCallbackIds;
                mWeakReference = weakReference;
            }

            @Override
            protected void doCallback(Response response) {
                String resultCallbackStr = formatResultString(mJsCallbackIds, response);
                NestedWebView nestedWebView = mWeakReference != null ? mWeakReference.get() : null;
                final String callH5Callback = resultCallbackStr;
                if (nestedWebView != null) {
                    nestedWebView.post(() -> {
                        // scan result callback
                        if (!TextUtils.isEmpty(callH5Callback)) {
                            nestedWebView.loadUrl(callH5Callback);
                        } else {
                            Log.e(TAG, "call h5 call back is null");
                        }
                        // complete callback
                        String completeCallbackStr = formatResultString(mJsCallbackIds, null);
                        if (!TextUtils.isEmpty(completeCallbackStr)) {
                            nestedWebView.loadUrl(completeCallbackStr);
                        } else {
                            Log.e(TAG, "call h5 call back complete string is null");
                        }
                    });
                }
            }
        }
    }

    public WebviewSettingProvider getWebViewSettingProvider() {
        if (mWebViewSettingProvider == null) {
            mWebViewSettingProvider = ProviderManager.getDefault().getProvider(WebviewSettingProvider.NAME);
        }
        return mWebViewSettingProvider;
    }


    private class WebViewNativeApi {

        @JavascriptInterface
        public void go(String path) {
            RenderEventCallback callback = mComponent.getCallback();
            if (TextUtils.isEmpty(path) || callback == null) {
                return;
            }
            callback.loadUrl(path);
        }

        @JavascriptInterface
        public void postMessage(final String message) {
            if (mOnMessageListener != null) {
                // webview.getUrl must be call at main thread
                post(
                        new Runnable() {
                            @Override
                            public void run() {
                                mOnMessageListener.onMessage(message, getUrl());
                            }
                        });
            }
        }

        @JavascriptInterface
        public void ignoreSslError() {
            if (!TextUtils.isEmpty(mLastSslErrorUrl)) {
                if (mAuthorizedSslDomains == null) {
                    mAuthorizedSslDomains = new ArrayList<>(2);
                }
                String domain = getDomain(mLastSslErrorUrl);
                if (!TextUtils.isEmpty(domain)) {
                    mAuthorizedSslDomains.add(domain);
                }
                post(() -> loadUrl(mLastSslErrorUrl));
            } else {
                Log.e(TAG, "error: ignoreSslError mLastSslErrorUrl is null");
            }
        }

        @JavascriptInterface
        public void exitSslError() {
            post(() -> webInternalGoBack());
        }

        @JavascriptInterface
        public void webGoBack() {
            post(() -> webInternalGoBack());
        }
    }

    private class KeyboardStatusListener implements ViewTreeObserver.OnGlobalLayoutListener {
        private Rect mTempVisibleRect = new Rect();

        @Override
        public final void onGlobalLayout() {
            getWindowVisibleDisplayFrame(mTempVisibleRect);

            View contentRoot = ((ViewGroup) getRootView()).getChildAt(0);
            int[] contentRootLocation = {0, 0};
            contentRoot.getLocationOnScreen(contentRootLocation);
            int contentRootBottom = contentRootLocation[1] + contentRoot.getHeight();

            int keyboardHeight =
                    contentRootBottom - mTempVisibleRect.bottom; // considering split-screen, navBar
            if (keyboardHeight < 0) { // BugFix: android9.0 进入分屏,调整分屏高度时触发crash
                keyboardHeight = 0;
            }
            adjustKeyboard(keyboardHeight);
        }
    }

    private class DownloadConfirmDialog extends CheckableAlertDialog {

        DownloadConfirmDialog(Context context) {
            super(context);
        }

        @Override
        protected void setupClickListener(
                Button button, final int whichButton, final OnClickListener listener) {
            switch (whichButton) {
                case DialogInterface.BUTTON_POSITIVE:
                    button.setOnClickListener(
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    if (listener != null) {
                                        listener.onClick(DownloadConfirmDialog.this, whichButton);
                                    }
                                }
                            });
                    break;
                case DialogInterface.BUTTON_NEGATIVE:
                case DialogInterface.BUTTON_NEUTRAL:
                    super.setupClickListener(button, whichButton, listener);
                    break;
                default:
                    // ignore
                    break;
            }
        }

        @Override
        public void show() {
            DarkThemeUtil.disableForceDark(this);
            super.show();
        }
    }
}
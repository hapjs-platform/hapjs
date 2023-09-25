/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge.impl.webkit;

import android.app.Activity;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.util.Log;
import android.view.View;
import android.webkit.GeolocationPermissions;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import org.hapjs.bridge.HybridChromeClient;
import org.hapjs.bridge.HybridManager;
import org.hapjs.bridge.HybridResourceResponse;
import org.hapjs.bridge.HybridSettings;
import org.hapjs.bridge.HybridView;
import org.hapjs.bridge.HybridViewClient;

public class HybridViewImpl implements HybridView {
    private static final String TAG = "HybridViewImpl";
    private WebView mWebView;
    private HybridManager mHybridManager;
    private HybridViewClient mHybridViewClient;
    private HybridChromeClient mHybridChromeClient;
    private HybridSettings mHybridSettings;

    public HybridViewImpl(WebView webView) {
        mWebView = webView;
        mWebView.setWebViewClient(new InternWebViewClient());
        mWebView.setWebChromeClient(new InternWebChromeClient());
        WebSettings settings = mWebView.getSettings();
        settings.setJavaScriptEnabled(false);
        settings.setSavePassword(false);
        settings.setAllowFileAccess(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setAllowFileAccessFromFileURLs(false);
        try {
            mWebView.removeJavascriptInterface("searchBoxJavaBridge_");
            mWebView.removeJavascriptInterface("accessibility");
            mWebView.removeJavascriptInterface("accessibilityTraversal");
        } catch (Exception e) {
            Log.e(TAG, "initWebView: ", e);
        }
        mHybridSettings = new InternHybridSettings();
        mHybridManager = new HybridManager((Activity) webView.getContext(), this);
    }

    @Override
    public HybridManager getHybridManager() {
        return mHybridManager;
    }

    @Override
    public View getWebView() {
        return mWebView;
    }

    @Override
    public void setHybridViewClient(HybridViewClient client) {
        mHybridViewClient = client;
    }

    @Override
    public void setHybridChromeClient(HybridChromeClient client) {
        mHybridChromeClient = client;
    }

    @Override
    public void loadUrl(String url) {
        mWebView.loadUrl(url);
    }

    @Override
    public HybridSettings getSettings() {
        return mHybridSettings;
    }

    @Override
    public void destroy() {
        mWebView.destroy();
    }

    @Override
    public void menuButtonPressPage(OnKeyUpListener onKeyUpListener) {
    }

    @Override
    public boolean canGoBack() {
        return mWebView.canGoBack();
    }

    @Override
    public void goBack() {
        mWebView.goBack();
    }

    @Override
    public boolean needRunInBackground() {
        return false;
    }

    @Override
    public void setOnVisibilityChangedListener(OnVisibilityChangedListener l) {
        // ignore
    }

    private class InternWebViewClient extends WebViewClient {
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            if (mHybridViewClient == null) {
                super.onPageStarted(view, url, favicon);
                return;
            }
            mHybridViewClient.onPageStarted(HybridViewImpl.this, url, favicon);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            if (mHybridViewClient == null) {
                super.onPageFinished(view, url);
                return;
            }
            mHybridViewClient.onPageFinished(HybridViewImpl.this, url);
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
            if (mHybridViewClient == null) {
                return super.shouldInterceptRequest(view, url);
            }
            HybridResourceResponse hybridResourceResponse =
                    mHybridViewClient.shouldInterceptRequest(HybridViewImpl.this, url);
            return hybridResourceResponse == null
                    ? null
                    : new WebResourceResponse(
                    hybridResourceResponse.getMimeType(),
                    hybridResourceResponse.getEncoding(),
                    hybridResourceResponse.getData());
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (mHybridViewClient == null) {
                return super.shouldOverrideUrlLoading(view, url);
            }
            return mHybridViewClient.shouldOverrideUrlLoading(HybridViewImpl.this, url);
        }

        @Override
        public void onReceivedSslError(
                WebView view, android.webkit.SslErrorHandler handler, SslError error) {
            if (mHybridViewClient == null) {
                super.onReceivedSslError(view, handler, error);
                return;
            }
            mHybridViewClient.onReceivedSslError(HybridViewImpl.this, handler, error);
        }

        @Override
        public void onReceivedError(
                WebView view, int errorCode, String description, String failingUrl) {
            if (mHybridViewClient == null) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                return;
            }
            mHybridViewClient
                    .onReceivedError(HybridViewImpl.this, errorCode, description, failingUrl);
        }

        @Override
        public void onReceivedLoginRequest(WebView view, String realm, String account,
                                           String args) {
            if (mHybridViewClient == null) {
                super.onReceivedLoginRequest(view, realm, account, args);
                return;
            }
            mHybridViewClient.onReceivedLoginRequest(HybridViewImpl.this, realm, account, args);
        }
    }

    private class InternWebChromeClient extends WebChromeClient {
        @Override
        public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
            if (mHybridChromeClient == null) {
                return super.onJsAlert(view, url, message, result);
            }
            return mHybridChromeClient.onJsAlert(HybridViewImpl.this, url, message, result);
        }

        @Override
        public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
            if (mHybridChromeClient == null) {
                return super.onJsConfirm(view, url, message, result);
            }
            return mHybridChromeClient.onJsConfirm(HybridViewImpl.this, url, message, result);
        }

        @Override
        public void onProgressChanged(WebView view, int progress) {
            if (mHybridChromeClient == null) {
                super.onProgressChanged(view, progress);
                return;
            }
            mHybridChromeClient.onProgressChanged(HybridViewImpl.this, progress);
        }

        @Override
        public void onGeolocationPermissionsShowPrompt(
                String origin, GeolocationPermissions.Callback callback) {
            if (mHybridChromeClient == null) {
                super.onGeolocationPermissionsShowPrompt(origin, callback);
                return;
            }
            mHybridChromeClient.onGeolocationPermissionsShowPrompt(origin, callback);
        }

        @Override
        public void onReceivedTitle(WebView view, String title) {
            if (mHybridChromeClient == null) {
                super.onReceivedTitle(view, title);
                return;
            }
            mHybridChromeClient.onReceivedTitle(HybridViewImpl.this, title);
        }
    }

    private class InternHybridSettings extends HybridSettings {
        @Override
        public void setJavaScriptEnabled(boolean flag) {
            mWebView.getSettings().setJavaScriptEnabled(flag);
        }

        @Override
        public String getUserAgentString() {
            return mWebView.getSettings().getUserAgentString();
        }

        @Override
        public void setUserAgentString(String ua) {
            mWebView.getSettings().setUserAgentString(ua);
        }

        @Override
        public void setUseWideViewPort(boolean use) {
            mWebView.getSettings().setUseWideViewPort(use);
        }

        @Override
        public void setSupportMultipleWindows(boolean support) {
            mWebView.getSettings().setSupportMultipleWindows(support);
        }

        @Override
        public void setLoadWithOverviewMode(boolean overview) {
            mWebView.getSettings().setLoadWithOverviewMode(overview);
        }

        @Override
        public void setDomStorageEnabled(boolean flag) {
            mWebView.getSettings().setDomStorageEnabled(flag);
        }

        @Override
        public void setDatabaseEnabled(boolean flag) {
            mWebView.getSettings().setDatabaseEnabled(flag);
        }

        @Override
        public void setAllowFileAccessFromFileURLs(boolean flag) {
            mWebView.getSettings().setAllowFileAccessFromFileURLs(flag);
        }

        @Override
        public void setAllowUniversalAccessFromFileURLs(boolean flag) {
            mWebView.getSettings().setAllowUniversalAccessFromFileURLs(flag);
        }

        @Override
        public void setCacheMode(int mode) {
            mWebView.getSettings().setCacheMode(mode);
        }

        @Override
        public void setJavaScriptCanOpenWindowsAutomatically(boolean flag) {
            mWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(flag);
        }

        @Override
        public void setTextZoom(int textZoom) {
            mWebView.getSettings().setTextZoom(textZoom);
        }

        @Override
        public void setGeolocationEnabled(boolean flag) {
            mWebView.getSettings().setGeolocationEnabled(flag);
        }

        @Override
        public void setAppCacheEnabled(boolean flag) {
            if (flag){
                mWebView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
            }else {
                mWebView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
            }
        }

        @Override
        public void setAppCachePath(String appCachePath) {
            /* this method is deprecated with setAppCacheEnabled().
             *  see https://developer.android.com/sdk/api_diff/30-incr/changes/android.webkit.WebSettings .
            */
        }

        @Override
        public void setGeolocationDatabasePath(String databasePath) {
            mWebView.getSettings().setGeolocationDatabasePath(databasePath);
        }
    }
}

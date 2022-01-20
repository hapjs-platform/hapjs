/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge;

/**
 * Base class that can be used to manages settings state for a {@link HybridView}.
 */
public abstract class HybridSettings {

    /**
     * @hide Only for use by AbsWebView implementations.
     */
    protected HybridSettings() {
    }

    /**
     * @see android.webkit.WebSettings#setJavaScriptEnabled(boolean)
     */
    public void setJavaScriptEnabled(boolean flag) {
    }

    /**
     * @see android.webkit.WebSettings#getUserAgentString()
     */
    public String getUserAgentString() {
        return "";
    }

    /**
     * @see android.webkit.WebSettings#setUserAgentString(String)
     */
    public void setUserAgentString(String ua) {
    }

    /**
     * @see android.webkit.WebSettings#setUseWideViewPort(boolean)
     */
    public void setUseWideViewPort(boolean use) {
    }

    /**
     * @see android.webkit.WebSettings#setSupportMultipleWindows(boolean)
     */
    public void setSupportMultipleWindows(boolean support) {
    }

    /**
     * @see android.webkit.WebSettings#setLoadWithOverviewMode(boolean)
     */
    public void setLoadWithOverviewMode(boolean overview) {
    }

    /**
     * @see android.webkit.WebSettings#setDomStorageEnabled(boolean)
     */
    public void setDomStorageEnabled(boolean flag) {
    }

    /**
     * @see android.webkit.WebSettings#setDatabaseEnabled(boolean)
     */
    public void setDatabaseEnabled(boolean flag) {
    }

    /**
     * @see android.webkit.WebSettings#setAllowFileAccessFromFileURLs(boolean)
     */
    public void setAllowFileAccessFromFileURLs(boolean flag) {
    }

    /**
     * @see android.webkit.WebSettings#setAllowUniversalAccessFromFileURLs(boolean)
     */
    public void setAllowUniversalAccessFromFileURLs(boolean flag) {
    }

    /**
     * @see android.webkit.WebSettings#setCacheMode(int)
     */
    public void setCacheMode(int mode) {
    }

    /**
     * @see android.webkit.WebSettings#setJavaScriptCanOpenWindowsAutomatically(boolean)
     */
    public void setJavaScriptCanOpenWindowsAutomatically(boolean flag) {
    }

    /**
     * @see android.webkit.WebSettings#setTextZoom(int)
     */
    public void setTextZoom(int textZoom) {
    }

    /**
     * @see android.webkit.WebSettings#setGeolocationEnabled(boolean)
     */
    public void setGeolocationEnabled(boolean flag) {
    }

    /**
     * @see android.webkit.WebSettings#setAppCacheEnabled(boolean)
     */
    public void setAppCacheEnabled(boolean flag) {
    }

    /**
     * @see android.webkit.WebSettings#setAppCachePath(String)
     */
    public void setAppCachePath(String appCachePath) {
    }

    /**
     * @see android.webkit.WebSettings#setGeolocationDatabasePath(String)
     */
    public void setGeolocationDatabasePath(String databasePath) {
    }
}

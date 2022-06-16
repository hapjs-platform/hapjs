/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.view.webview;

import android.os.Build;
import android.util.Log;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.annotation.NonNull;

public class BaseWebViewClient extends WebViewClient {
    private static final String TAG = "BaseWebViewClient";
    public WebSourceType mSourceType = WebSourceType.UNKNOWN;

    public BaseWebViewClient(@NonNull WebSourceType sourceType) {
        this.mSourceType = sourceType;
    }

    @Override
    public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
        if (view == null) {
            Log.e(TAG, "onRenderProcessGone view is null");
            return true;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Log.e(TAG, "onRenderProcessGone low api, mSourceType:" + mSourceType.name());
            return false;
        }
        ViewParent parent = view.getParent();
        if (parent != null && parent instanceof ViewGroup) {
            ((ViewGroup) parent).removeView(view);
        }
        view.destroy();
        Log.e(TAG, "onRenderProcessGone detail did crash is " + detail.didCrash() + ", mSourceType:" + mSourceType.name());
        return true;
    }

    public enum WebSourceType {
        // web widget
        WEB,
        // qq Login
        QQ_LOGIN,
        // richtext widget
        RICH_TEXT,
        // weixin pay
        WX_PAY,
        // unknown
        UNKNOWN
    }
}

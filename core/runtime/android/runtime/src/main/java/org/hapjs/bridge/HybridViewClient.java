/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge;

import android.graphics.Bitmap;
import android.net.http.SslError;
import android.webkit.SslErrorHandler;
import java.io.IOException;
import java.util.UUID;

public class HybridViewClient {
    private static final String ASSET_PATH = "hybrid/";
    private static final String VIRTUAL_PATH = "android_asset/" + ASSET_PATH;

    public HybridViewClient() {
    }

    public void onPageStarted(HybridView view, String url, Bitmap favicon) {
        PageContext pageContext = new PageContext();
        pageContext.setId(UUID.randomUUID().toString());
        pageContext.setUrl(url);
        view.getHybridManager().onPageChange();
    }

    public void onPageFinished(HybridView view, String url) {
    }

    public HybridResourceResponse shouldInterceptRequest(HybridView view, String url) {
        HybridResourceResponse response = null;
        if (url != null && url.startsWith("http")) {
            int index = url.indexOf(VIRTUAL_PATH);
            if (index >= 0 && index + VIRTUAL_PATH.length() < url.length()) {
                String assetPath = url.substring(index + VIRTUAL_PATH.length());
                try {
                    response =
                            new HybridResourceResponse(
                                    null,
                                    null,
                                    view.getHybridManager().getActivity().getAssets()
                                            .open(ASSET_PATH + assetPath));
                } catch (IOException e) {
                    response = null;
                }
            }
        }
        return response;
    }

    public boolean shouldOverrideUrlLoading(final HybridView view, String url) {
        return false;
    }

    public void onReceivedSslError(HybridView view, SslErrorHandler handler, SslError error) {
        handler.cancel();
    }

    public void onReceivedError(
            HybridView view, int errorCode, String description, String failingUrl) {
    }

    public void onReceivedLoginRequest(HybridView view, String realm, String account, String args) {
    }
}

/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge.impl.android;

import android.app.Activity;
import android.view.View;
import org.hapjs.bridge.HybridChromeClient;
import org.hapjs.bridge.HybridManager;
import org.hapjs.bridge.HybridSettings;
import org.hapjs.bridge.HybridViewClient;
import org.hapjs.render.RootView;
import org.hapjs.render.jsruntime.JsThread;

public class HybridViewImpl implements org.hapjs.bridge.HybridView {
    private static final String TAG = "HybridViewImpl";

    private HybridManager mHybridManager;
    private RootView mRootView;
    private HybridViewClient mHybridViewClient;

    public HybridViewImpl(RootView rootView) {
        mRootView = rootView;
        mRootView.setAndroidViewClient(new InternWebViewClient());
        mHybridManager = new HybridManager((Activity) rootView.getContext(), this);
        mRootView.setResidentManager(mHybridManager.getResidentManager());
    }

    @Override
    public HybridManager getHybridManager() {
        return mHybridManager;
    }

    @Override
    public View getWebView() {
        return mRootView;
    }

    @Override
    public void setHybridViewClient(HybridViewClient client) {
        mHybridViewClient = client;
    }

    @Override
    public void setHybridChromeClient(HybridChromeClient client) {
    }

    @Override
    public void loadUrl(String url) {
        mRootView.load(url);
    }

    @Override
    public HybridSettings getSettings() {
        return new HybridSettings() {
        };
    }

    @Override
    public void destroy() {
    }

    @Override
    public void menuButtonPressPage(OnKeyUpListener onKeyUpListener) {
        mRootView.menuButtonPressPage(onKeyUpListener);
    }

    @Override
    public boolean canGoBack() {
        return mRootView.canGoBack();
    }

    @Override
    public void goBack() {
        mRootView.goBack();
    }

    @Override
    public boolean needRunInBackground() {
        return mHybridManager.getResidentManager().needRunInBackground();
    }

    @Override
    public void setOnVisibilityChangedListener(OnVisibilityChangedListener l) {
        mRootView.setOnVisibilityChangedListener(l);
    }

    private class InternWebViewClient extends AndroidViewClient {
        @Override
        public void onRuntimeCreate(RootView view) {
            JsThread jsThread = view.getJsThread();
            jsThread.getBridgeManager().setHybridManager(mHybridManager);
        }

        @Override
        public void onPageStarted(RootView view, String url) {
            if (mHybridViewClient != null) {
                mHybridViewClient.onPageStarted(HybridViewImpl.this, url, null);
            }
        }
    }
}

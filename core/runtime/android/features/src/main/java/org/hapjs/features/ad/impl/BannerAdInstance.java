/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.ad.impl;

import android.app.Activity;
import android.util.Log;
import org.hapjs.bridge.Callback;
import org.hapjs.bridge.Response;
import org.hapjs.features.ad.instance.BaseBannerAdInstance;

public class BannerAdInstance extends BaseBannerAdInstance {
    private static final String TAG = "BannerAdInstance";

    public BannerAdInstance(Activity activity, String adUnitId) {
        this(activity, adUnitId, null, 750);
    }

    public BannerAdInstance(Activity activity, String adUnitId, Style style, int designWidth) {
        super(style, designWidth);
    }

    @Override
    protected void onResize(int width, int height) {
        Log.d(TAG, "onResize: " + width + " - " + height);
        mStyle.setRealWidth(width);
        mStyle.setRealHeight(height);
        super.onResize(width, height);
    }

    @Override
    public void destroy() {
        Log.d(TAG, "destroy: ");
    }

    @Override
    public void show(Callback callback) {
        Log.d(TAG, "show: ");
        callbackDefaultMockupErrorResponse();
    }

    @Override
    public void hide(Callback callback) {
        Log.d(TAG, "hide: ");
        callbackDefaultMockupErrorResponse();
    }

    @Override
    public void release() {
        Log.d(TAG, "release: ");
    }
}

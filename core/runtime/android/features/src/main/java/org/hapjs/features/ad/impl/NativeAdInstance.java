/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.ad.impl;

import android.app.Activity;
import android.util.Log;
import org.hapjs.features.ad.instance.BaseNativeAdInstance;

public class NativeAdInstance extends BaseNativeAdInstance {
    private static final String TAG = "NativeAdInstance";

    public NativeAdInstance(Activity activity, String adUnitid) {
    }

    @Override
    public void destroy() {
        Log.d(TAG, "destroy: ");
    }

    @Override
    public void load() {
        Log.d(TAG, "load: ");
    }

    @Override
    public void reportAdShow(String adId) {
        Log.d(TAG, "reportAdShow: ");
    }

    @Override
    public void reportAdClick(String adId) {
        Log.d(TAG, "reportAdClick: ");
    }

    @Override
    public void release() {
        Log.d(TAG, "release: ");
    }
}

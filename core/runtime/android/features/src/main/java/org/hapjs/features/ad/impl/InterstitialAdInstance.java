/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.ad.impl;

import android.app.Activity;
import android.util.Log;
import org.hapjs.bridge.Callback;
import org.hapjs.bridge.Response;
import org.hapjs.features.ad.instance.BaseInterstitialAdInstance;

public class InterstitialAdInstance extends BaseInterstitialAdInstance {
    private static final String TAG = "InterstitialAdInstance";

    public InterstitialAdInstance(Activity activity, String adUnitid) {
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
    public void release() {
        Log.d(TAG, "release: ");
    }
}

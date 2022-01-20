/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.service.alipay;

import android.app.Activity;
import org.hapjs.bridge.FeatureExtension;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.FeatureExtensionAnnotation;
import org.json.JSONException;

@FeatureExtensionAnnotation(
        name = AliPay.FEATURE_NAME,
        actions = {
                @ActionAnnotation(name = AliPay.ACTION_PAY, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(name = AliPay.ACTION_GET_SDK_VERSION, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(name = AliPay.ACTION_GET_TYPE, mode = FeatureExtension.Mode.SYNC)
        })
public class AliPay extends FeatureExtension {
    protected static final String FEATURE_NAME = "service.alipay";
    protected static final String ACTION_GET_TYPE = "getType";
    protected static final String ACTION_PAY = "pay";
    protected static final String ACTION_GET_SDK_VERSION = "getVersion";
    private static final String TAG = "HybridAliPay";

    @Override
    protected Response invokeInner(final Request request) throws JSONException {
        Activity activity = request.getNativeInterface().getActivity();

        String action = request.getAction();
        if (ACTION_PAY.equals(action)) {
            pay(request);
        } else if (ACTION_GET_SDK_VERSION.equals(action)) {
            getSDKVersion(request, activity);
        } else if (ACTION_GET_TYPE.equals(action)) {
            return new Response(getType(request));
        }

        return null;
    }

    private String getType(Request request) {
        return "";
    }

    private void getSDKVersion(Request request, Activity activity) {
        request
                .getCallback()
                .callback(new Response(Response.CODE_SERVICE_UNAVAILABLE, "alipay not available!"));
    }

    private void pay(Request request) throws JSONException {
        request
                .getCallback()
                .callback(new Response(Response.CODE_SERVICE_UNAVAILABLE, "alipay not available!"));
    }

    @Override
    public String getName() {
        return FEATURE_NAME;
    }
}

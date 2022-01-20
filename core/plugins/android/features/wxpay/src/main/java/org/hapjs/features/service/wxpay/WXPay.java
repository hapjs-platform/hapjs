/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.service.wxpay;

import android.app.Activity;
import org.hapjs.bridge.FeatureExtension;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.FeatureExtensionAnnotation;
import org.hapjs.render.jsruntime.serialize.SerializeException;

@FeatureExtensionAnnotation(
        name = WXPay.FEATURE_NAME,
        actions = {
                @ActionAnnotation(name = WXPay.ACTION_PAY, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(name = WXPay.ACTION_GET_TYPE, mode = FeatureExtension.Mode.SYNC)
        })
public class WXPay extends FeatureExtension {

    protected static final String FEATURE_NAME = "service.wxpay";
    protected static final String ACTION_GET_TYPE = "getType";
    protected static final String ACTION_PAY = "pay";
    private static final String TAG = "HybridWXPay";

    @Override
    protected Response invokeInner(final Request request) throws Exception {

        final String action = request.getAction();
        if (ACTION_PAY.equals(action)) {
            wxPay(request);
        } else if (ACTION_GET_TYPE.equals(action)) {
            Activity activity = request.getNativeInterface().getActivity();
            return new Response(getSupportType(activity));
        }

        return Response.SUCCESS;
    }

    private String getSupportType(Activity activity) {
        return "";
    }

    private void wxPay(final Request request) throws SerializeException {
        request
                .getCallback()
                .callback(new Response(Response.CODE_SERVICE_UNAVAILABLE, "wxpay not available!"));
    }

    @Override
    public String getName() {
        return FEATURE_NAME;
    }
}

/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.service.pay;

import android.text.TextUtils;
import android.util.Log;
import org.hapjs.bridge.FeatureExtension;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.FeatureExtensionAnnotation;
import org.json.JSONException;
import org.json.JSONObject;

@FeatureExtensionAnnotation(
        name = Pay.FEATURE_NAME,
        actions = {
                @ActionAnnotation(name = Pay.ACTION_PAY, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(name = Pay.ACTION_GET_PROVIDER, mode = FeatureExtension.Mode.SYNC)
        })
public class Pay extends FeatureExtension {

    protected static final String FEATURE_NAME = "service.pay";
    protected static final String ACTION_PAY = "pay";
    protected static final String ACTION_GET_PROVIDER = "getProvider";
    private static final String TAG = "Pay";
    private static final String KEY_ORDER_INFO = "orderInfo";

    private static final String KEY_CODE = "code";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_RESULT = "result";

    @Override
    protected Response invokeInner(Request request) throws Exception {
        String action = request.getAction();
        if (ACTION_PAY.equals(action)) {
            invokePay(request);
        } else if (ACTION_GET_PROVIDER.equals(action)) {
            return getProvider(request);
        }
        return null;
    }

    private Response getProvider(Request request) {
        return new Response("");
    }

    protected Response invokePay(final Request request) {
        String orderInfo = null;
        try {
            JSONObject params = new JSONObject(request.getRawParams());
            orderInfo = params.getString(KEY_ORDER_INFO);
        } catch (JSONException e) {
            Log.d(TAG, "failed to get order info", e);
        }
        if (TextUtils.isEmpty(orderInfo)) {
            return new Response(Response.CODE_GENERIC_ERROR, "orderInfo is null!!!");
        }

        // pay order, success
        JSONObject result = new JSONObject();
        try {
            result.put(KEY_CODE, 0);
            result.put(KEY_MESSAGE, "pay message");
            result.put(KEY_RESULT, "pay result");
        } catch (JSONException e) {
            Log.e(TAG, "pay failed", e);
        }
        return new Response(result);
    }

    @Override
    public String getName() {
        return FEATURE_NAME;
    }
}

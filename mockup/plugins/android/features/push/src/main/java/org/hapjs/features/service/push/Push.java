/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.service.push;

import android.util.Log;
import org.hapjs.bridge.FeatureExtension;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.FeatureExtensionAnnotation;
import org.json.JSONException;
import org.json.JSONObject;

@FeatureExtensionAnnotation(
        name = Push.FEATURE_NAME,
        actions = {
                @ActionAnnotation(name = Push.ACTION_SUBSCRIBE, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(name = Push.ACTION_UNSUBSCRIBE, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(name = Push.ACTION_ON, mode = FeatureExtension.Mode.CALLBACK),
                @ActionAnnotation(name = Push.ACTION_OFF, mode = FeatureExtension.Mode.SYNC),
                @ActionAnnotation(name = Push.ACTION_GET_PROVIDER, mode = FeatureExtension.Mode.SYNC)
        })
public class Push extends FeatureExtension {
    protected static final String FEATURE_NAME = "service.push";
    protected static final String ACTION_SUBSCRIBE = "subscribe";
    protected static final String ACTION_UNSUBSCRIBE = "unsubscribe";
    protected static final String ACTION_ON = "on";
    protected static final String ACTION_OFF = "off";
    protected static final String ACTION_GET_PROVIDER = "getProvider";
    private static final String TAG = "Push";
    private static final String KEY_REG_ID = "regId";
    private static final String KEY_END_POINT = "endPoint";
    private static final String KEY_MESSAGE_ID = "messageId";
    private static final String KEY_DATA = "data";

    @Override
    protected Response invokeInner(Request request) throws Exception {
        String action = request.getAction();
        if (ACTION_SUBSCRIBE.equals(action)) {
            invokeSubscribe(request);
        } else if (ACTION_UNSUBSCRIBE.equals(action)) {
            invokeUnsubscribe(request);
        } else if (ACTION_ON.equals(action)) {
            invokeOn(request);
        } else if (ACTION_OFF.equals(action)) {
            return invokeOff(request);
        } else if (ACTION_GET_PROVIDER.equals(action)) {
            return getProvider(request);
        }
        return null;
    }

    private Response getProvider(Request request) {
        return new Response("");
    }

    private void invokeSubscribe(Request request) {
        JSONObject result = new JSONObject();
        try {
            result.put(KEY_REG_ID, "regid-abcdefg");
            result.put(KEY_END_POINT, ""); // by design
        } catch (JSONException e) {
            Log.e(TAG, "push subscribe failed", e);
        }
        request.getCallback().callback(new Response(result));
    }

    private void invokeUnsubscribe(Request request) {
        request.getCallback().callback(new Response("success"));
    }

    private void invokeOn(Request request) {
        JSONObject result = new JSONObject();
        try {
            result.put(KEY_MESSAGE_ID, "messageId-abcdefg");
            result.put(KEY_DATA, "data-abcdefg");
        } catch (JSONException e) {
            Log.e(TAG, "push on failed", e);
        }
        request.getCallback().callback(new Response(result));
    }

    private Response invokeOff(Request request) {
        return Response.SUCCESS;
    }

    @Override
    public String getName() {
        return FEATURE_NAME;
    }
}

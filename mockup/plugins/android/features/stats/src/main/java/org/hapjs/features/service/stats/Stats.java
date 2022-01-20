/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.service.stats;

import org.hapjs.bridge.FeatureExtension;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.FeatureExtensionAnnotation;
import org.json.JSONException;
import org.json.JSONObject;

@FeatureExtensionAnnotation(
        name = Stats.FEATURE_NAME,
        actions = {
                @ActionAnnotation(name = Stats.ACTION_GET_PROVIDER, mode = FeatureExtension.Mode.SYNC),
                @ActionAnnotation(name = Stats.ACTION_RECORD_COUNT_EVENT, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(
                        name = Stats.ACTION_RECORD_CALCULATE_EVENT,
                        mode = FeatureExtension.Mode.ASYNC),
        })
public class Stats extends FeatureExtension {
    protected static final String FEATURE_NAME = "service.stats";
    protected static final String ACTION_GET_PROVIDER = "getProvider";
    protected static final String ACTION_RECORD_COUNT_EVENT = "recordCountEvent";
    protected static final String ACTION_RECORD_CALCULATE_EVENT = "recordCalculateEvent";
    private static final String TAG = "Stats";
    private static final String PARAM_CATEGORY = "category";
    private static final String PARAM_KEY = "key";
    private static final String PARAM_VALUE = "value";
    private static final String PARAM_MAP = "map";

    @Override
    protected Response invokeInner(Request request) throws Exception {
        String action = request.getAction();
        if (ACTION_GET_PROVIDER.equals(request.getAction())) {
            return getProvider();
        } else if (ACTION_RECORD_COUNT_EVENT.equals(action)) {
            recordCountEvent(request);
        } else if (ACTION_RECORD_CALCULATE_EVENT.equals(action)) {
            recordCalculateEvent(request);
        }
        return null;
    }

    private Response getProvider() {
        return new Response("");
    }

    private void recordCountEvent(Request request) throws JSONException {
        JSONObject jsonParams = request.getJSONParams();
        if (jsonParams == null) {
            request.getCallback()
                    .callback(new Response(Response.CODE_ILLEGAL_ARGUMENT, "no params"));
            return;
        }
        String category = jsonParams.optString(PARAM_CATEGORY);
        String key = jsonParams.getString(PARAM_KEY);
        JSONObject mapObject = jsonParams.optJSONObject(PARAM_MAP);
        request.getCallback().callback(Response.SUCCESS);
    }

    private void recordCalculateEvent(Request request) throws JSONException {
        JSONObject jsonParams = request.getJSONParams();
        if (jsonParams == null) {
            request.getCallback()
                    .callback(new Response(Response.CODE_ILLEGAL_ARGUMENT, "no params"));
            return;
        }
        String category = jsonParams.optString(PARAM_CATEGORY);
        String key = jsonParams.getString(PARAM_KEY);
        long value = jsonParams.getLong(PARAM_VALUE);
        JSONObject mapObject = jsonParams.optJSONObject(PARAM_MAP);
        request.getCallback().callback(Response.SUCCESS);
    }

    @Override
    public String getName() {
        return FEATURE_NAME;
    }
}

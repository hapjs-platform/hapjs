/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features;

import android.util.Log;
import org.hapjs.bridge.FeatureExtension;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.FeatureExtensionAnnotation;
import org.hapjs.bridge.permission.HapCustomPermissions;
import org.json.JSONException;
import org.json.JSONObject;

@FeatureExtensionAnnotation(
        name = HealthService.FEATURE_NAME,
        actions = {
                @ActionAnnotation(
                        name = HealthService.ACTION_HAS_STEPS_OF_DAY,
                        mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(
                        name = HealthService.ACTION_GET_TODAY_STEPS,
                        mode = FeatureExtension.Mode.ASYNC,
                        permissions = {HapCustomPermissions.HAP_PERMISSION_STEP_COUNTER}),
                @ActionAnnotation(
                        name = HealthService.ACTION_GET_LAST_WEEK_STEPS,
                        mode = FeatureExtension.Mode.ASYNC,
                        permissions = {HapCustomPermissions.HAP_PERMISSION_STEP_COUNTER})
        })
public class HealthService extends FeatureExtension {

    protected static final String FEATURE_NAME = "service.health";
    protected static final String ACTION_HAS_STEPS_OF_DAY = "hasStepsOfDay";
    protected static final String ACTION_GET_TODAY_STEPS = "getTodaySteps";
    protected static final String ACTION_GET_LAST_WEEK_STEPS = "getLastWeekSteps";
    protected static final String RESULT_SUPPORT = "support";
    protected static final String RESULT_STEPS = "steps";
    protected static final String RESULT_STEPS_LIST = "stepsList";
    protected static final String RESULT_DATE = "date";
    protected static final int CODE_FEATURE_NOT_SUPPORT = Response.CODE_FEATURE_ERROR + 1;
    private static final String TAG = "HealthService";

    @Override
    protected Response invokeInner(Request request) throws Exception {
        String action = request.getAction();
        if (ACTION_HAS_STEPS_OF_DAY.equals(action)) {
            hasStepsOfDay(request);
        } else if (ACTION_GET_TODAY_STEPS.equals(action)) {
            getTodaySteps(request, action);
        } else if (ACTION_GET_LAST_WEEK_STEPS.equals(action)) {
            getLastWeekSteps(request, action);
        } else {
            Log.w(TAG, "undefined action:" + action);
            return Response.NO_ACTION;
        }
        return Response.SUCCESS;
    }

    private void hasStepsOfDay(Request request) throws JSONException {
        JSONObject result = new JSONObject();
        result.put(RESULT_SUPPORT, hasSupport(request));
        Response response = new Response(result);
        request.getCallback().callback(response);
    }

    private void getTodaySteps(Request request, String action) {
        if (hasSupport(request)) {
            doGetTodaySteps(request);
        } else {
            request
                    .getCallback()
                    .callback(new Response(CODE_FEATURE_NOT_SUPPORT, "not support get steps"));
        }
    }

    private void getLastWeekSteps(Request request, String action) {
        if (hasSupport(request)) {
            doGetLastWeekSteps(request);
        } else {
            request
                    .getCallback()
                    .callback(new Response(CODE_FEATURE_NOT_SUPPORT, "not support get steps"));
        }
    }

    protected boolean hasSupport(Request request) {
        // implement this method according your rom,default is false
        // TO DO
        return false;
    }

    protected void doGetTodaySteps(Request request) {
        // implement this method according your rom
        // TO DO
    }

    protected void doGetLastWeekSteps(Request request) {
        // implement this method according your rom
        // TO DO
    }

    @Override
    public String getName() {
        return FEATURE_NAME;
    }
}

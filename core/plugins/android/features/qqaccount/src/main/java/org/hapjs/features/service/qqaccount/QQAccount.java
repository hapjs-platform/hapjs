/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.service.qqaccount;

import android.app.Activity;
import android.util.Log;
import androidx.annotation.NonNull;
import java.util.Map;
import org.hapjs.bridge.FeatureExtension;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.FeatureExtensionAnnotation;

@FeatureExtensionAnnotation(
        name = QQAccount.FEATURE_NAME,
        actions = {
                @ActionAnnotation(name = QQAccount.ACTION_GET_TYPE, mode = FeatureExtension.Mode.SYNC),
                @ActionAnnotation(name = QQAccount.ACTION_AUTHORIZE, mode = FeatureExtension.Mode.ASYNC)
        })
public class QQAccount extends FeatureExtension {

    protected static final String FEATURE_NAME = "service.qqaccount";
    protected static final String ACTION_GET_TYPE = "getType";
    protected static final String TYPE_NONE = "NONE";
    protected static final String ACTION_AUTHORIZE = "authorize";
    private static final String TAG = "HybridQQAccount";
    private static final String KEY_QQACCOUNT_APPID = "appId";
    private static final String KEY_CLIENT_ID = "clientId";

    private String mAppId;
    private String mClientId;

    @Override
    public void setParams(Map<String, String> params) {
        super.setParams(params);
        mAppId = getParam(KEY_QQACCOUNT_APPID);
        mClientId = getParam(KEY_CLIENT_ID);
        Log.v(TAG, "Get appid " + mAppId + ", mClientId" + mClientId);
    }

    @Override
    protected Response invokeInner(Request request) {
        final String action = request.getAction();
        if (ACTION_GET_TYPE.equals(action)) {
            return getType(request);
        } else if (ACTION_AUTHORIZE.equals(action)) {
            authorize(request);
        }

        return Response.SUCCESS;
    }

    @NonNull
    private Response getType(Request request) {
        Activity activity = request.getNativeInterface().getActivity();
        return new Response(getSupportType(activity));
    }

    protected String getSupportType(Activity activity) {
        return TYPE_NONE;
    }

    private void authorize(Request request) {
        Response response =
                new Response(Response.CODE_SERVICE_UNAVAILABLE, "No avaliable authorize type.");
        request.getCallback().callback(response);
    }

    @Override
    public String getName() {
        return FEATURE_NAME;
    }
}

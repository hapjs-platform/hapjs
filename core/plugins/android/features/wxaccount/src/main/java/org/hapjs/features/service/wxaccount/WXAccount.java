/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.service.wxaccount;

import android.app.Activity;
import org.hapjs.bridge.FeatureExtension;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.FeatureExtensionAnnotation;

@FeatureExtensionAnnotation(
        name = WXAccount.FEATURE_NAME,
        actions = {
                @ActionAnnotation(name = WXAccount.ACTION_AUTHORIZE, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(name = WXAccount.ACTION_GET_TYPE, mode = FeatureExtension.Mode.SYNC)
        })
public class WXAccount extends FeatureExtension {
    protected static final String FEATURE_NAME = "service.wxaccount";

    protected static final String ACTION_GET_TYPE = "getType";
    protected static final String TYPE_NONE = "NONE";

    protected static final String ACTION_AUTHORIZE = "authorize";

    @Override
    protected Response invokeInner(Request request) {
        final String action = request.getAction();
        if (ACTION_GET_TYPE.equals(action)) {
            Activity activity = request.getNativeInterface().getActivity();
            return new Response(getType(activity));
        } else if (ACTION_AUTHORIZE.equals(action)) {
            authorize(request);
        }

        return Response.SUCCESS;
    }

    protected String getType(Activity activity) {
        return TYPE_NONE;
    }

    protected void authorize(final Request request) {
        Response response =
                new Response(Response.CODE_SERVICE_UNAVAILABLE, "wxaccount not avaliable.");
        request.getCallback().callback(response);
    }

    @Override
    public String getName() {
        return FEATURE_NAME;
    }
}

/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.service.wbaccount;

import org.hapjs.bridge.FeatureExtension;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.FeatureExtensionAnnotation;

@FeatureExtensionAnnotation(
        name = WBAccount.FEATURE_NAME,
        actions = {
                @ActionAnnotation(name = WBAccount.ACTION_GET_TYPE, mode = FeatureExtension.Mode.SYNC),
                @ActionAnnotation(name = WBAccount.ACTION_AUTHORIZE, mode = FeatureExtension.Mode.ASYNC)
        })
public class WBAccount extends FeatureExtension {

    protected static final String FEATURE_NAME = "service.wbaccount";
    protected static final String ACTION_GET_TYPE = "getType";
    protected static final String ACTION_AUTHORIZE = "authorize";

    protected static final String TYPE_NONE = "NONE";

    @Override
    public String getName() {
        return FEATURE_NAME;
    }

    @Override
    protected Response invokeInner(Request request) {
        if (ACTION_GET_TYPE.equals(request.getAction())) {
            return new Response(getType(request));
        } else if (ACTION_AUTHORIZE.equals(request.getAction())) {
            authorize(request);
        }
        return null;
    }

    protected void authorize(final Request request) {
        request
                .getCallback()
                .callback(new Response(Response.CODE_SERVICE_UNAVAILABLE,
                        "wbaccount not avaliable."));
    }

    protected String getType(Request request) {
        return TYPE_NONE;
    }
}

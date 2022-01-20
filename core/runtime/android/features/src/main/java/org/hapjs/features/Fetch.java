/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features;

import java.io.UnsupportedEncodingException;
import org.hapjs.bridge.FeatureExtension;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.FeatureExtensionAnnotation;
import org.hapjs.render.jsruntime.serialize.SerializeException;
import org.hapjs.render.jsruntime.serialize.SerializeObject;
import org.json.JSONException;

@FeatureExtensionAnnotation(
        name = Fetch.FEATURE_NAME,
        residentType = FeatureExtensionAnnotation.ResidentType.USEABLE,
        actions = {
                @ActionAnnotation(
                        name = Fetch.ACTION_FETCH,
                        mode = FeatureExtension.Mode.ASYNC,
                        normalize = FeatureExtension.Normalize.RAW)
        })
public class Fetch extends AbstractRequest {
    protected static final String FEATURE_NAME = "system.fetch";

    @Override
    public String getName() {
        return FEATURE_NAME;
    }

    @Override
    protected Response invokeInner(Request request)
            throws JSONException, UnsupportedEncodingException, SerializeException {
        if (ACTION_FETCH.equals(request.getAction())) {
            SerializeObject reader = request.getSerializeParams();
            if (reader == null) {
                request
                        .getCallback()
                        .callback(new Response(Response.CODE_ILLEGAL_ARGUMENT,
                                "serialize param is null"));
                return null;
            } else if (reader.has(PARAMS_KEY_FILES)) {
                request
                        .getCallback()
                        .callback(
                                new Response(
                                        Response.CODE_ILLEGAL_ARGUMENT,
                                        "unsupported param: " + PARAMS_KEY_FILES));
                return null;
            } else {
                return super.invokeInner(request);
            }
        }
        return null;
    }
}

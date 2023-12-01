/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge;

import android.webkit.JavascriptInterface;
import com.eclipsesource.v8.V8Object;
import org.hapjs.render.jsruntime.serialize.JavaSerializeObject;

/**
 * The interface exposed to JavaScript.
 */
public class JsInterface {

    public static final String INTERFACE_NAME = "JsBridge";

    private ExtensionManager mManager;

    public JsInterface(ExtensionManager manager) {
        mManager = manager;
    }

    /**
     * Used by JavaScript to invoke specified action in specified feature.
     *
     * @param feature   feature name.
     * @param action    action name in specified feature.
     * @param rawParams raw parameters.
     * @param callback  callback identifier.
     * @return invocation response.
     */
    @JavascriptInterface
    public Response invoke(
            String feature, String action, Object rawParams, String callback, int instanceId) {
        if (rawParams instanceof V8Object) {
            if (((V8Object) rawParams).isUndefined()) {
                return new Response(
                        Response.CODE_GENERIC_ERROR,
                        String.format("%s with action %s, rawParams can't be undefined", feature,
                                action));
            }
            rawParams = new JavaSerializeObject(V8ObjectHelper.toMap((V8Object) rawParams));
        }

        Response response = mManager.invoke(feature, action, rawParams, callback, instanceId);
        return response;
    }

    @JavascriptInterface
    public Response jsErrorInvoke(V8Object object){
        return mManager.jsErrorInvoke(object);
    }
}

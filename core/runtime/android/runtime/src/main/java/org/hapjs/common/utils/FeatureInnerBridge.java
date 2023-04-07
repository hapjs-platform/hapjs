/*
 * Copyright (c) 2022-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.utils;

import android.util.Log;

import org.hapjs.bridge.Callback;
import org.hapjs.bridge.ExtensionManager;
import org.hapjs.bridge.HybridManager;
import org.hapjs.bridge.Response;
import org.hapjs.render.jsruntime.JsThread;

public class FeatureInnerBridge {
    public static final String TAG = "FeatureInnerBridge";
    public static final String H5_JS_CALLBACK = "-2";
    public static final String MENUBAR_JS_CALLBACK = "-3";

    public static void invokeWithCallback(ExtensionManager extensionManager, String name, String action, Object rawParams,
                                          String jsCallback, int instanceId, Callback asyncResultCallback) {
        final ExtensionManager tmpExtensionManager = extensionManager;
        if (null == tmpExtensionManager) {
            Response response = new Response(Response.CODE_GENERIC_ERROR,
                    "invokeWithCallback tmpExtensionManager is null ,Refuse to use this interfaces in background: " + name);
            if (asyncResultCallback != null) {
                asyncResultCallback.callback(response);
            }
            return;
        }
        JsThread jsThread = tmpExtensionManager.getJsThread();
        if (null != jsThread) {
            jsThread.postInJsThread(new Runnable() {
                @Override
                public void run() {
                    HybridManager tmpHybridManager = tmpExtensionManager.getHybridManager();
                    if (null == tmpHybridManager) {
                        Log.e(TAG, "invokeWithCallback error tmpHybridManager null.");
                        Response response = new Response(Response.CODE_GENERIC_ERROR,
                                "invokeWithCallback tmpHybridManager is null ,Refuse to use this interfaces in background: " + name);
                        if (asyncResultCallback != null) {
                            asyncResultCallback.callback(response);
                        }
                        return;
                    }
                    tmpExtensionManager.onInvoke(name, action, rawParams, jsCallback, instanceId, asyncResultCallback);
                }
            });
        } else {
            Log.e(TAG, "invokeWithCallback jsThread null.");
            Response response = new Response(Response.CODE_GENERIC_ERROR,
                    "invokeWithCallback jsThread is null ,Refuse to use this interfaces in background: " + name);
            if (asyncResultCallback != null) {
                asyncResultCallback.callback(response);
            }
        }
    }
}

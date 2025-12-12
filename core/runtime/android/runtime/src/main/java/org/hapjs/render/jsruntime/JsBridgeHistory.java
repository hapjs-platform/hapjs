/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.jsruntime;

import android.util.Log;

import android.util.Pair;
import com.eclipsesource.v8.JavaVoidCallback;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;

import java.util.Map;

public class JsBridgeHistory extends V8Object {

    private static final String TAG = "JsBridgeHistory";
    private IJavaNative mNative;

    public final JavaVoidCallback back =
            new JavaVoidCallback() {
                @Override
                public void invoke(V8Object v8Object, V8Array args) {
                    mNative.routerBack();
                }
            };

    public final JavaVoidCallback push =
            new JavaVoidCallback() {
                @Override
                public void invoke(V8Object v8Object, V8Array args) {
                    V8Object params = args.getObject(0);
                    if (params == null) {
                        Log.e(TAG, "push params is null");
                        return;
                    }
                    try {
                        Pair<String, Map<String, String>> pair = V8ObjConverter.parseReqeustParams(params);
                        mNative.routerPush(pair.first, pair.second);
                    } finally {
                        JsUtils.release(params);
                    }
                }
            };

    public final JavaVoidCallback replace =
            new JavaVoidCallback() {
                @Override
                public void invoke(V8Object v8Object, V8Array args) {
                    V8Object params = args.getObject(0);
                    if (params == null) {
                        Log.e(TAG, "replace params is null");
                        return;
                    }
                    try {
                        Pair<String, Map<String, String>> pair =
                                V8ObjConverter.parseReqeustParams(params);
                        mNative.routerReplace(pair.first, pair.second);
                    } finally {
                        JsUtils.release(params);
                    }
                }
            };

    public final JavaVoidCallback clear =
            new JavaVoidCallback() {
                @Override
                public void invoke(V8Object v8Object, V8Array args) {
                    mNative.routerClear();
                }
            };

    public JsBridgeHistory(JsContext jsContext, IJavaNative javaNative) {
        super(jsContext.getV8());
        mNative = javaNative;
    }
}

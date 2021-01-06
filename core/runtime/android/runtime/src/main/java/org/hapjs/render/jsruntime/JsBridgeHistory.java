/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.jsruntime;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.eclipsesource.v8.JavaVoidCallback;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;

import org.hapjs.bridge.HybridRequest;
import org.hapjs.common.utils.NavigationUtils;
import org.hapjs.common.utils.RouterUtils;
import org.hapjs.render.PageManager;
import org.hapjs.render.PageNotFoundException;

import static org.hapjs.logging.RuntimeLogManager.VALUE_ROUTER_APP_FROM_JS_PUSH;

public class JsBridgeHistory extends V8Object {

    private static final String TAG = "JsBridgeHistory";
    private Context mContext;
    private JsContext mJsContext;
    private PageManager mPageManager;

    public final JavaVoidCallback back =
            new JavaVoidCallback() {
                @Override
                public void invoke(V8Object v8Object, V8Array args) {
                    RouterUtils.back(mContext, mPageManager);
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
                        String pkg = mPageManager.getAppInfo().getPackage();
                        HybridRequest request = V8ObjConverter.objToRequest(params, pkg);
                        if (NavigationUtils.navigate(
                                mContext, pkg, request, new Bundle(),
                                VALUE_ROUTER_APP_FROM_JS_PUSH, null)) {
                            return;
                        }

                        try {
                            RouterUtils.push(mPageManager, request);
                        } catch (PageNotFoundException ex) {
                            mJsContext.getJsThread().processV8Exception(ex);
                        }
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
                    HybridRequest request =
                            V8ObjConverter
                                    .objToRequest(params, mPageManager.getAppInfo().getPackage());
                    try {
                        RouterUtils.replace(mPageManager, request);
                    } finally {
                        JsUtils.release(params);
                    }
                }
            };

    public final JavaVoidCallback clear =
            new JavaVoidCallback() {
                @Override
                public void invoke(V8Object v8Object, V8Array args) {
                    mPageManager.clear();
                }
            };

    public JsBridgeHistory(Context context, JsContext jsContext) {
        super(jsContext.getV8());
        mContext = context;
        mJsContext = jsContext;
    }

    public void attach(PageManager pageManager) {
        mPageManager = pageManager;
    }
}

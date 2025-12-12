/*
 * Copyright (C) 2023, hapjs.org. All rights reserved.
 */
package org.hapjs.render.jsruntime;

import com.eclipsesource.v8.JavaCallback;
import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;
import org.hapjs.bridge.ExtensionManager;
import org.hapjs.bridge.JsInterface;
import org.hapjs.bridge.Response;

public class JsInterfaceProxy extends V8Object {
    static V8Object register(V8 v8, JsInterface jsInterface, String name) {
        JsInterfaceProxy proxy = new JsInterfaceProxy(v8, jsInterface);
        v8.add(name, proxy);
        proxy.registerJavaMethod(proxy.invoke, "invoke");
        return proxy;
    }

    private final JsInterface jsInterface;

    private final JavaCallback invoke = new JavaCallback() {
        @Override
        public Object invoke(V8Object receiver, V8Array parameters) {
            Object rawParams = parameters.get(2);
            Object instanceId = parameters.get(4);
            if (!(instanceId instanceof Integer)) {
                instanceId = -1;
            }
            Response response = jsInterface.invoke(
                    parameters.getString(0),
                    parameters.getString(1),
                    rawParams,
                    parameters.getString(3),
                    (int) instanceId);

            if (rawParams instanceof V8Object) {
                JsUtils.release((V8Object) rawParams);
            }
            return response == null ? null : response.toJavascriptResult(v8);
        }
    };

    private JsInterfaceProxy(V8 v8, JsInterface jsInterface) {
        super(v8);
        this.jsInterface = jsInterface;
    }
}
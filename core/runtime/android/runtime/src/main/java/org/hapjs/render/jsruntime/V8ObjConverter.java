/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.jsruntime;

import com.eclipsesource.v8.V8Object;
import java.util.Map;
import org.hapjs.bridge.HybridRequest;

public class V8ObjConverter {

    static HybridRequest objToRequest(V8Object obj, String appId) {
        String uri = obj.contains("uri") ? obj.getString("uri") : null;
        if (uri == null || uri.isEmpty()) {
            uri = obj.contains("path") ? obj.getString("path") : null;
            if (uri == null || uri.isEmpty()) {
                uri = obj.contains("name") ? obj.getString("name") : null;
            }
        }

        Map<String, String> params = null;
        if (obj.contains("params")) {
            V8Object v8Params = obj.getObject("params");
            try {
                params = JsUtils.v8ObjectToMap(v8Params, null);
            } finally {
                JsUtils.release(v8Params);
            }
        }

        return new HybridRequest.Builder().pkg(appId).uri(uri).params(params).build();
    }

    static String getPageUri(V8Object obj) {
        return obj.contains("uri") ? obj.getString("uri") : null;
    }
}

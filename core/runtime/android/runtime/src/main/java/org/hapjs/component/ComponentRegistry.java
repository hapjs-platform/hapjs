/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component;

import android.util.Log;
import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import org.hapjs.render.jsruntime.JsUtils;

public class ComponentRegistry {
    private static final String TAG = "ComponentRegistry";

    public static void registerBuiltInComponents(V8 v8) {
        String components = ComponentManager.getWidgetListJSONString();
        if (components != null) {
            V8Array parameters = new V8Array(v8);
            try {
                parameters.push(components);
                v8.executeVoidFunction("registerComponents", parameters);
            } finally {
                JsUtils.release(parameters);
            }
        } else {
            Log.e(TAG, "Fail to registerBuiltInComponents, components=" + components);
        }
    }
}

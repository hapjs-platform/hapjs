/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component;

import android.util.Log;

import org.hapjs.render.jsruntime.IJsEngine;

public class ComponentRegistry {
    private static final String TAG = "ComponentRegistry";

    public static void registerBuiltInComponents(IJsEngine engine) {
        String components = ComponentManager.getWidgetListJSONString();
        if (components != null) {
            engine.registerComponents(components);
        } else {
            Log.e(TAG, "Fail to registerBuiltInComponents, components=" + components);
        }
    }
}

/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component;

import android.content.Context;
import java.util.Map;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.runtime.HapEngine;

public interface ComponentCreator {

    Component createComponent(
            HapEngine hapEngine,
            Context context,
            Container parent,
            int ref,
            RenderEventCallback callback,
            Map<String, Object> componentInfo,
            Map<String, Object> savedState);
}

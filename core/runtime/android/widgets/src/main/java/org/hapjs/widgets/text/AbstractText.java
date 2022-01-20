/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.text;

import android.content.Context;
import android.view.View;
import java.util.Map;
import org.hapjs.component.Container;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.runtime.HapEngine;

abstract class AbstractText<T extends View> extends Container<T> {

    protected static final String WIDGET_NAME = "text";

    public AbstractText(
            HapEngine hapEngine,
            Context context,
            Container parent,
            int ref,
            RenderEventCallback callback,
            Map savedState) {
        super(hapEngine, context, parent, ref, callback, savedState);
    }
}

/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets;

import android.content.Context;
import android.view.View;
import java.util.Map;
import org.hapjs.component.Component;
import org.hapjs.component.Container;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.runtime.HapEngine;

public class Indicator extends Component {

    public Indicator(
            HapEngine hapEngine,
            Context context,
            Container parent,
            int ref,
            RenderEventCallback callback,
            Map<String, Object> savedState) {
        super(hapEngine, context, parent, ref, callback, savedState);
    }

    @Override
    protected View createViewImpl() {
        return null;
    }
}

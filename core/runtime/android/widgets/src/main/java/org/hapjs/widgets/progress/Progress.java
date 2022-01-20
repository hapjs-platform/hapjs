/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.progress;

import android.content.Context;
import android.view.View;
import java.util.Map;
import org.hapjs.common.utils.DisplayUtil;
import org.hapjs.component.Component;
import org.hapjs.component.Container;
import org.hapjs.component.SwipeObserver;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.runtime.HapEngine;

abstract class Progress<T extends View> extends Component<T> implements SwipeObserver {

    protected static final String WIDGET_NAME = "progress";

    protected static final int DEFAULT_COLOR = 0xff33b4ff;
    protected final int mDefaultDimension;

    public Progress(
            HapEngine hapEngine,
            Context context,
            Container parent,
            int ref,
            RenderEventCallback callback,
            Map savedState) {
        super(hapEngine, context, parent, ref, callback, savedState);
        mDefaultDimension = (int) DisplayUtil.getRealPxByWidth(32, hapEngine.getDesignWidth());
    }
}

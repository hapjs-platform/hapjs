/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.mockup.impl;

import android.content.Context;
import android.view.View;
import org.hapjs.component.Component;
import org.hapjs.widgets.canvas.CanvasProvider;

public class CanvasProviderImpl implements CanvasProvider {

    @Override
    public View createCanvasView(Context ctx, String refId, Component component) {
        return null;
    }

    @Override
    public void destroyCanvasView(View view) {
    }
}

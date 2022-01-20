/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.canvas.webgl;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.View;
import androidx.annotation.NonNull;
import org.hapjs.component.Component;
import org.hapjs.widgets.canvas.CanvasView;

public class WebGLCanvasView extends GLSurfaceView implements CanvasView {

    private Component mComponent;

    public WebGLCanvasView(Context context) {
        super(context);
    }

    @Override
    public void draw() {
    }

    @NonNull
    @Override
    public View get() {
        return this;
    }

    @Override
    public Component getComponent() {
        return mComponent;
    }

    @Override
    public void setComponent(Component component) {
        mComponent = component;
    }
}

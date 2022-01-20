/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.canvas.webgl;

import org.hapjs.widgets.canvas.CanvasContext;

public class WebGLRenderingContext extends CanvasContext {
    public WebGLRenderingContext(int pageId, int canvasId, int designWidth) {
        super(pageId, canvasId, designWidth);
    }

    @Override
    public boolean isWebGL() {
        return true;
    }

    @Override
    public String type() {
        return "webgl";
    }
}

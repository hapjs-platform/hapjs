/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.canvas;

import androidx.annotation.NonNull;
import java.util.Map;
import org.hapjs.widgets.canvas.canvas2d.CanvasContextRendering2D;

public abstract class CanvasSyncRenderAction extends Action {

    public CanvasSyncRenderAction(String action) {
        super(action);
    }

    public abstract void render(
            @NonNull CanvasContextRendering2D context, @NonNull Map<String, Object> result)
            throws Exception;
}

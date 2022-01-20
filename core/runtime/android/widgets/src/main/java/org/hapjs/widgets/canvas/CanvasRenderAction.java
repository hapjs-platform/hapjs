/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.canvas;

import androidx.annotation.NonNull;
import org.hapjs.widgets.canvas.canvas2d.CanvasContextRendering2D;

public abstract class CanvasRenderAction extends Action {

    private String mParameter;

    public CanvasRenderAction(String action, String parameter) {
        super(action);
        mParameter = parameter;
    }

    @Override
    public int hashCode() {
        return (getAction() + mParameter).hashCode();
    }

    public boolean canClear(@NonNull CanvasContextRendering2D context) {
        return false;
    }

    public boolean useCompositeCanvas() {
        return false;
    }

    public boolean supportHardware(@NonNull CanvasContextRendering2D context) {
        return true;
    }

    public abstract void render(@NonNull CanvasContextRendering2D context);
}

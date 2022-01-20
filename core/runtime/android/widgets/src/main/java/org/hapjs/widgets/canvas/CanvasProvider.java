/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.canvas;

import android.content.Context;
import android.view.View;
import org.hapjs.component.Component;

public interface CanvasProvider {

    String NAME = "canvas";

    View createCanvasView(Context ctx, String refId, Component component);

    void destroyCanvasView(View view);
}

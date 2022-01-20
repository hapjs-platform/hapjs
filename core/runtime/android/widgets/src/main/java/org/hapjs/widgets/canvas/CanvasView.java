/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.canvas;

import android.view.View;
import androidx.annotation.NonNull;
import org.hapjs.component.view.ComponentHost;

public interface CanvasView extends ComponentHost {

    void draw();

    @NonNull
    View get();
}

/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.canvas;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import org.hapjs.component.Component;
import org.hapjs.widgets.R;

public class EmptyCanvasProvider implements CanvasProvider {

    @Override
    public View createCanvasView(Context ctx, String refId, Component component) {
        TextView view = new TextView(ctx);
        view.setText(R.string.no_canvas);
        view.setGravity(Gravity.CENTER);
        return view;
    }

    @Override
    public void destroyCanvasView(View view) {
    }
}

/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.canvas.canvas2d;

import android.content.Context;
import android.graphics.Canvas;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import org.hapjs.common.utils.ThreadUtils;
import org.hapjs.component.Component;
import org.hapjs.widgets.canvas.CanvasContext;
import org.hapjs.widgets.canvas.CanvasManager;
import org.hapjs.widgets.canvas.CanvasRenderAction;
import org.hapjs.widgets.canvas.CanvasView;

public class CanvasView2D extends View implements CanvasView {

    private static final String TAG = "CanvasView2D";

    private Component mComponent;

    public CanvasView2D(Context context) {
        super(context);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mComponent == null) {
            Log.w(TAG, "mComponent is null,return");
            return;
        }

        if (getWidth() <= 0 || getHeight() <= 0) {
            Log.w(TAG, "canvas view size is zero!");
            // 在CanvasViewContainer中addView后，并没有触发CanvasView2D测量，这里在大小为0时重新触发
            ViewParent parent = getParent();
            if (parent instanceof ViewGroup) {
                ViewGroup parentView = (ViewGroup) parent;
                if ((parentView.getWidth() - parentView.getPaddingLeft()
                        - parentView.getPaddingRight()) > 0
                        && (parentView.getHeight() - parentView.getPaddingTop()
                        - parentView.getPaddingBottom())
                        > 0) {

                    ViewGroup.LayoutParams layoutParams = getLayoutParams();
                    if (layoutParams != null) {
                        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
                        layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
                        requestLayout();
                    }
                }
            }
            return;
        }

        int pageId = mComponent.getPageId();
        int ref = mComponent.getRef();

        ArrayList<CanvasRenderAction> renderActions =
                CanvasManager.getInstance().getRenderActions(pageId, ref);
        if (renderActions == null || renderActions.isEmpty()) {
            Log.e(TAG, "renderActions is empty,return," + ref);
            return;
        }

        CanvasContext context = CanvasManager.getInstance().getContext(pageId, ref);
        if (context == null || !context.is2d()) {
            Log.e(TAG, "CanvasContext is null,return" + ref);
            return;
        }

        boolean supportHardware = true;
        for (CanvasRenderAction renderAction : renderActions) {
            if (!renderAction.supportHardware((CanvasContextRendering2D) context)) {
                supportHardware = false;
                break;
            }
        }

        if (supportHardware) {
            setLayerType(View.LAYER_TYPE_HARDWARE, null);
        } else {
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        ((CanvasContextRendering2D) context).render(this, canvas, renderActions);
    }

    @Override
    public void draw() {
        ThreadUtils.runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        invalidate();
                    }
                });
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

/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.canvas;

import android.graphics.Bitmap;
import org.hapjs.component.Component;
import org.hapjs.widgets.canvas.image.CanvasImageLoader;

public abstract class CanvasImageLoadRenderAction extends CanvasRenderAction
        implements CanvasImageLoader.RecoverImageCallback {

    private int mPageId = Component.INVALID_PAGE_ID;
    private int mCanvasId = -1;
    private boolean mLoadingBitmap = false;

    public CanvasImageLoadRenderAction(String action, String parameter) {
        super(action, parameter);
    }

    public void markLoading(int pageId, int canvasId) {
        mPageId = pageId;
        mCanvasId = canvasId;

        synchronized (this) {
            mLoadingBitmap = true;
        }
    }

    public boolean isLoading() {
        return mLoadingBitmap;
    }

    @Override
    public void onSuccess(Bitmap bitmap) {
        synchronized (this) {
            mLoadingBitmap = false;
        }
        if (mPageId != Component.INVALID_PAGE_ID && mCanvasId != -1) {
            CanvasManager.getInstance().triggerRender(mPageId, mCanvasId);
        }
    }

    @Override
    public void onFailure() {
        synchronized (this) {
            mLoadingBitmap = false;
        }
    }
}

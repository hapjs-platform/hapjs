/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.view.drawable;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;

public class ImmutableLayerDrawable extends LayerDrawable {

    private Drawable[] mChildrens = null;

    public ImmutableLayerDrawable(Drawable[] layers) {
        super(layers);
        mChildrens = layers;
    }

    @Override
    public Drawable mutate() {
        return this;
    }

    // force top CSSBackgroundDrawable cover the view bounds
    public void setBounds(int left, int top, int right, int bottom) {
        super.setBounds(left, top, right, bottom);
        if (mChildrens != null
                && mChildrens.length > 0
                && mChildrens[mChildrens.length - 1] instanceof CSSBackgroundDrawable) {
            mChildrens[mChildrens.length - 1].setBounds(left, top, right, bottom);
        }
    }
}

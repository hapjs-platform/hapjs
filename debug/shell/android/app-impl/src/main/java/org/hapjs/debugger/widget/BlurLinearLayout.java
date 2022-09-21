/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.debugger.widget;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

public class BlurLinearLayout extends LinearLayout {
    private BlurShadowDrawable mBlurShadowDrawable;

    public BlurLinearLayout(Context context) {
        this(context, null);
    }

    public BlurLinearLayout(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BlurLinearLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O_MR1) {
            mBlurShadowDrawable = new BlurShadowDrawable(this, context, attrs);
            setBackground(mBlurShadowDrawable);
        }
    }
}

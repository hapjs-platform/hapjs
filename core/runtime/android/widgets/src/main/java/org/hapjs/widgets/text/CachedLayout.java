/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.text;

import android.text.Layout;

public class CachedLayout {

    private int mWidthMeasureSpec;
    private Layout mLayout;
    private CharSequence mText;

    public CachedLayout(CharSequence text, Layout layout, int widthMeasureSpec) {
        mText = text;
        mLayout = layout;
        mWidthMeasureSpec = widthMeasureSpec;
    }

    public CharSequence getText() {
        return mText;
    }

    public Layout getLayout() {
        return mLayout;
    }

    public int getWidthMeasureSpec() {
        return mWidthMeasureSpec;
    }
}

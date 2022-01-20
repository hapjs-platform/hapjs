/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.view.text;

import android.graphics.Paint;

public class LineHeightSpan implements android.text.style.LineHeightSpan {
    private final int mHeight;

    public LineHeightSpan(int height) {
        mHeight = height;
    }

    @Override
    public void chooseHeight(
            CharSequence text, int start, int end, int spanstartv, int v, Paint.FontMetricsInt fm) {
        int additional = mHeight - (fm.descent - fm.ascent);
        fm.top -= additional / 2;
        fm.ascent -= additional / 2;
        fm.descent += additional / 2;
        fm.bottom += additional / 2;
    }
}

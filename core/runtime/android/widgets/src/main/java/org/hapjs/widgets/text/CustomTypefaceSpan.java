/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.text;

import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Build;
import android.text.TextPaint;
import android.text.style.TypefaceSpan;

public class CustomTypefaceSpan extends TypefaceSpan {

    private Typeface mTypeface;

    public CustomTypefaceSpan(Typeface typeface) {
        super("");
        mTypeface = typeface;
    }

    private static void apply(Paint paint, Typeface tf) {
        if (tf == null) {
            return;
        }

        int oldStyle;

        Typeface old = paint.getTypeface();
        if (old == null) {
            oldStyle = 0;
        } else {
            oldStyle = old.getStyle();
        }

        int fake = oldStyle & ~tf.getStyle();

        if ((fake & Typeface.BOLD) != 0) {
            paint.setFakeBoldText(true);
        }

        if ((fake & Typeface.ITALIC) != 0) {
            paint.setTextSkewX(-0.25f);
        }
        // fix Chinese italic not work on Android 4.4
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP
                && (tf.getStyle() == Typeface.ITALIC || tf.getStyle() == Typeface.BOLD_ITALIC)
                && oldStyle != Typeface.ITALIC
                && oldStyle != Typeface.BOLD_ITALIC) {
            paint.setTextSkewX(-0.25f);
        }

        paint.setTypeface(tf);
    }

    @Override
    public void updateMeasureState(TextPaint paint) {
        apply(paint, mTypeface);
    }

    @Override
    public void updateDrawState(TextPaint ds) {
        apply(ds, mTypeface);
    }
}

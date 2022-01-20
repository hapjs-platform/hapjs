/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.text;

import android.graphics.Typeface;
import android.text.TextUtils;

public class TypefaceBuilder {

    public static final String LIGHTER = "lighter";
    public static final String NORMAL = "normal";
    public static final String BOLD = "bold";
    public static final String BOLDER = "bolder";

    public static final String ITALIC = "italic";

    private Typeface mTypeface;
    private int mStyle;
    private int mWeight;

    public TypefaceBuilder() {
    }

    public TypefaceBuilder(TypefaceBuilder inherited) {
        if (inherited == null) {
            return;
        }
        mTypeface = inherited.getTypeface();
        mStyle = inherited.getStyle();
        mWeight = inherited.getWeight();
    }

    public static int parseFontWeight(String fontWeightStr) {
        int weight;
        if (TextUtils.equals(fontWeightStr, TypefaceBuilder.BOLD)
                || TextUtils.equals(fontWeightStr, TypefaceBuilder.BOLDER)) {
            weight = Typeface.BOLD;
        } else /*if (TextUtils.equals(fontWeightStr, TypefaceBuilder.NORMAL)
           || TextUtils.equals(fontWeightStr, TypefaceBuilder.LIGHTER))*/ {
            weight = Typeface.NORMAL;
        }

        if (!TextUtils.isEmpty(fontWeightStr) && TextUtils.isDigitsOnly(fontWeightStr)) {
            int weightDigits = Integer.parseInt(fontWeightStr);
            weight = weightDigits < 550 ? Typeface.NORMAL : Typeface.BOLD;
        }
        return weight;
    }

    public Typeface getTypeface() {
        return mTypeface;
    }

    public void setTypeface(Typeface typeface) {
        mTypeface = typeface;
    }

    public int getWeight() {
        return mWeight;
    }

    public void setWeight(int weight) {
        mWeight = weight;
    }

    public int getStyle() {
        return mStyle;
    }

    public void setStyle(int style) {
        mStyle = style;
    }

    public Typeface build() {
        int style;
        if (mWeight == Typeface.BOLD && mStyle == Typeface.ITALIC) {
            style = Typeface.BOLD_ITALIC;
        } else if (mStyle == Typeface.ITALIC) {
            style = Typeface.ITALIC;
        } else if (mWeight == Typeface.BOLD) {
            style = Typeface.BOLD;
        } else {
            style = Typeface.NORMAL;
        }
        return Typeface.create(mTypeface, style);
    }
}

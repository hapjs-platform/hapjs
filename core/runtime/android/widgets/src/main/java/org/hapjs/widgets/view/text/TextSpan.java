/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.view.text;

import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.UnderlineSpan;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.hapjs.common.utils.ColorUtil;
import org.hapjs.common.utils.FloatUtil;
import org.hapjs.widgets.text.CustomTypefaceSpan;
import org.hapjs.widgets.text.FontParser;
import org.hapjs.widgets.text.TypefaceBuilder;

public class TextSpan {

    private TypefaceBuilder mTypefaceBuilder;
    private String mFontFamily;
    private String mColor;
    private int mFontSize;
    private int mLineHeight;
    private float mLetterSpacing;
    private int mTextDecoration = TextDecoration.NONE;

    private boolean mIsDirty;
    private FontParser mFontParser;

    public Spannable createSpanned(String text) {
        if (!TextUtils.isEmpty(text)) {
            SpannableString spannable = new SpannableString(text);
            List<SetSpanOperation> ops = createSetSpanOperation(spannable.length());
            Collections.reverse(ops);
            for (SetSpanOperation op : ops) {
                op.execute(spannable);
            }
            return spannable;
        }
        return new SpannableString("");
    }

    public boolean isDirty() {
        return mIsDirty;
    }

    public void setDirty(boolean isDirty) {
        mIsDirty = isDirty;
    }

    public void setColor(String color) {
        if (!mIsDirty) {
            mIsDirty = !(color.equals(mColor));
        }
        mColor = color;
    }

    public int getFontSize() {
        return mFontSize;
    }

    public void setFontSize(int fontSize) {
        if (!mIsDirty) {
            mIsDirty = !(fontSize == mFontSize);
        }
        mFontSize = fontSize;
    }

    public int getLineHeight() {
        return mLineHeight;
    }

    public void setLineHeight(int lineHeight) {
        if (!mIsDirty) {
            mIsDirty = !(lineHeight == mLineHeight);
        }
        mLineHeight = lineHeight;
    }

    public TypefaceBuilder getTypefaceBuilder() {
        return mTypefaceBuilder;
    }

    public String getFontFamily() {
        return mFontFamily;
    }

    public void setFontFamily(String fontFamily) {
        if (TextUtils.equals(fontFamily, mFontFamily)) {
            return;
        }
        mFontFamily = fontFamily;
    }

    public void setFontTypeface(Typeface typeface, TypefaceBuilder inherited) {
        ensureTypefaceBuilder(inherited);
        if (!mIsDirty) {
            mIsDirty = !((typeface != null && typeface.equals(mTypefaceBuilder.getTypeface())));
        }
        mTypefaceBuilder.setTypeface(typeface);
    }

    public void setFontWeight(int fontWeight, TypefaceBuilder inherited) {
        ensureTypefaceBuilder(inherited);
        if (!mIsDirty) {
            mIsDirty = !(fontWeight == mTypefaceBuilder.getWeight());
        }
        mTypefaceBuilder.setWeight(fontWeight);
    }

    public void setFontStyle(int fontStyle, TypefaceBuilder inherited) {
        ensureTypefaceBuilder(inherited);
        if (!mIsDirty) {
            mIsDirty = !(fontStyle == mTypefaceBuilder.getStyle());
        }
        mTypefaceBuilder.setStyle(fontStyle);
    }

    private void ensureTypefaceBuilder(TypefaceBuilder inherited) {
        if (mTypefaceBuilder == null) {
            mTypefaceBuilder = new TypefaceBuilder(inherited);
        }
    }

    public int getTextDecoration() {
        return mTextDecoration;
    }

    public void setTextDecoration(int textDecoration) {
        if (!mIsDirty) {
            mIsDirty = !(textDecoration == mTextDecoration);
        }
        mTextDecoration = textDecoration;
    }

    public void setLetterSpacing(float letterSpacing) {
        if (!mIsDirty) {
            mIsDirty = !(FloatUtil.floatsEqual(letterSpacing, mLetterSpacing));
        }
        mLetterSpacing = letterSpacing;
    }

    public float getLetterSpacing() {
        return mLetterSpacing;
    }

    private List<SetSpanOperation> createSetSpanOperation(int end) {
        List<SetSpanOperation> ops = new LinkedList<>();
        int start = 0;
        if (end >= start) {
            if (!TextUtils.isEmpty(mColor)) {
                ops.add(
                        new SetSpanOperation(start, end,
                                new ForegroundColorSpan(ColorUtil.getColor(mColor))));
            }
            if (mFontSize > 0) {
                ops.add(new SetSpanOperation(start, end, new AbsoluteSizeSpan(mFontSize)));
            }
            if (mLineHeight > 0) {
                ops.add(new SetSpanOperation(start, end, new LineHeightSpan(mLineHeight)));
            }

            if (mTypefaceBuilder != null) {
                ops.add(new SetSpanOperation(start, end,
                        new CustomTypefaceSpan(mTypefaceBuilder.build())));
            }
            if (mTextDecoration == TextDecoration.UNDERLINE) {
                ops.add(new SetSpanOperation(start, end, new UnderlineSpan()));
            } else if (mTextDecoration == TextDecoration.LINE_THROUGH) {
                ops.add(new SetSpanOperation(start, end, new StrikethroughSpan()));
            }
        }

        return ops;
    }

    /**
     * Command object for setSpan
     */
    private static class SetSpanOperation {

        protected int start;
        protected int end;
        protected Object what;

        SetSpanOperation(int start, int end, Object what) {
            this.start = start;
            this.end = end;
            this.what = what;
        }

        public void execute(Spannable sb) {
            sb.setSpan(what, start, end, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        }
    }
}

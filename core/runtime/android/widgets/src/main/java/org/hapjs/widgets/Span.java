/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.View;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.hapjs.bridge.annotation.WidgetAnnotation;
import org.hapjs.common.utils.ColorUtil;
import org.hapjs.component.Component;
import org.hapjs.component.Container;
import org.hapjs.component.RecyclerDataItem;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.component.constants.Attributes;
import org.hapjs.render.css.value.CSSValues;
import org.hapjs.runtime.HapEngine;
import org.hapjs.widgets.text.CustomTypefaceSpan;
import org.hapjs.widgets.text.FontParser;
import org.hapjs.widgets.text.Text;
import org.hapjs.widgets.text.TypefaceBuilder;
import org.hapjs.widgets.view.text.LineHeightSpan;
import org.hapjs.widgets.view.text.TextDecoration;

@WidgetAnnotation(name = Span.WIDGET_NAME)
public class Span extends Container<View> implements NestedInnerSpannable {

    protected static final String WIDGET_NAME = "span";
    // attribute
    protected static final String FONT_FAMILY_DESC = "fontFamilyDesc";
    private static final String TAG = "Span";
    private SpannableString mSpannable;

    private String mColor;
    private int mFontSize;
    private int mLineHeight;
    private int mTextDecoration = STYLE_UNDEFINED;
    private String mValue;
    private TypefaceBuilder mTypefaceBuilder;
    private FontParser mFontParser;
    private String mFontFamily;
    private boolean mFontNeedUpdate = true;

    private boolean mViewCreated = false;

    public Span(
            HapEngine hapEngine,
            Context context,
            Container parent,
            int ref,
            RenderEventCallback callback,
            Map<String, Object> savedState) {
        super(hapEngine, context, parent, ref, callback, savedState);
    }

    @Override
    protected View createViewImpl() {
        // just a spannable.
        mViewCreated = true;
        return null;
    }

    @Override
    public void bindAttrs(Map<String, Object> attrs) {
        super.bindAttrs(attrs);
        if (mViewCreated) {
            applyAttrs(attrs, true);
        }
    }

    @Override
    public void bindStyles(Map<String, ? extends CSSValues> attrs) {
        super.bindStyles(attrs);
        if (mViewCreated) {
            applyStyles(attrs, true);
        }
    }

    @Override
    protected boolean setAttribute(String key, Object attribute) {
        switch (key) {
            case Attributes.Style.COLOR:
                String colorStr = Attributes.getString(attribute);
                setColor(colorStr);
                return true;
            case Attributes.Style.FONT_SIZE:
                String fontSizeStr = Attributes.getString(attribute);
                setFontSize(fontSizeStr);
                return true;
            case Attributes.Style.LINE_HEIGHT:
                if (mAutoLineHeight) {
                    int lineHeight = Attributes.getFontSize(mHapEngine, getPage(), attribute, -1, this);
                    setLineHeight(lineHeight);
                } else {
                    int lineHeight = Attributes.getInt(mHapEngine, attribute, -1, this);
                    setLineHeight(lineHeight);
                }
                return true;
            case Attributes.Style.FONT_STYLE:
                String fontStyleStr = Attributes.getString(attribute);
                setFontStyle(fontStyleStr);
                return true;
            case Attributes.Style.FONT_WEIGHT:
                String fontWeightStr = Attributes.getString(attribute);
                setFontWeight(fontWeightStr);
                return true;
            case Attributes.Style.TEXT_DECORATION:
                String textDecorationStr = Attributes.getString(attribute);
                setTextDecoration(textDecorationStr);
                return true;
            case Attributes.Style.VALUE:
                String value = Attributes.getString(attribute, "");
                setText(value);
                return true;
            case FONT_FAMILY_DESC:
                String fontFamily = Attributes.getString(attribute);
                setFontFamily(fontFamily);
                return true;
            default:
                break;
        }
        return false;
    }

    @Override
    public void applySpannable() {
        mSpannable = new SpannableString(mValue);

        applyColor();
        applyFontSize();
        applyLineHeight();
        applyTextDecoration();
        applyFontFamily();
    }

    @Override
    public Spannable getSpannable() {
        return mSpannable;
    }

    private void setText(String text) {
        if (TextUtils.equals(text, mValue)) {
            return;
        }
        mValue = text;
        applySpannable();
    }

    public String getColor() {
        return mColor;
    }

    public void setColor(String colorStr) {
        if (TextUtils.equals(colorStr, mColor)) {
            return;
        }

        mColor = colorStr;
        applyColor();
    }

    public int getFontSize() {
        return mFontSize;
    }

    public void setFontSize(String fontSizeStr) {
        int fontSize = Attributes.getFontSize(mHapEngine, getPage(), fontSizeStr, this);
        if (mFontSize == fontSize) {
            return;
        }

        mFontSize = fontSize;
        applyFontSize();
    }

    public void setLineHeight(int lineHeight) {
        if (mLineHeight == lineHeight) {
            return;
        }

        mLineHeight = lineHeight;
        applyLineHeight();
    }

    public void setFontStyle(String fontStyleStr) {
        ensureTypefaceBuilder();
        int style =
                TextUtils.equals(fontStyleStr, TypefaceBuilder.ITALIC) ? Typeface.ITALIC :
                        Typeface.NORMAL;
        if (style == mTypefaceBuilder.getStyle()) {
            return;
        }
        mTypefaceBuilder.setStyle(style);
        applyFontFamily();
    }

    public void setFontWeight(String fontWeightStr) {
        ensureTypefaceBuilder();
        int weight = TypefaceBuilder.parseFontWeight(fontWeightStr);
        if (weight == mTypefaceBuilder.getWeight()) {
            return;
        }
        mTypefaceBuilder.setWeight(weight);
        applyFontFamily();
    }

    public void setTextDecoration(String textDecorationStr) {
        int textDecoration = TextDecoration.NONE;

        if (TextUtils.isEmpty(textDecorationStr)) {
            textDecoration = STYLE_UNDEFINED;
        } else if ("underline".equals(textDecorationStr)) {
            textDecoration = TextDecoration.UNDERLINE;
        } else if ("line-through".equals(textDecorationStr)) {
            textDecoration = TextDecoration.LINE_THROUGH;
        }

        if (mTextDecoration == textDecoration) {
            return;
        }

        mTextDecoration = textDecoration;
        applyTextDecoration();
    }

    public void setFontFamily(String fontFamily) {
        if (TextUtils.equals(fontFamily, mFontFamily)) {
            return;
        }
        mFontFamily = fontFamily;
        if (mFontParser == null) {
            mFontParser = new FontParser(mContext, this);
        }
        mFontParser.parse(
                fontFamily,
                new FontParser.ParseCallback() {
                    @Override
                    public void onParseComplete(final Typeface typeface) {
                        ensureTypefaceBuilder();
                        mTypefaceBuilder.setTypeface(typeface);
                        if (mFontNeedUpdate) {
                            applyFontFamily();
                        }
                    }
                });
    }

    private TypefaceBuilder getInheritedTypefaceBuilder() {
        if (getParent() instanceof Text) {
            Text parent = (Text) getParent();
            return parent.getTextSpan().getTypefaceBuilder();
        } else if (getParent() instanceof Span) {
            Span parent = (Span) getParent();
            return parent.getTypefaceBuilder() != null
                    ? parent.getTypefaceBuilder()
                    : parent.getInheritedTypefaceBuilder();
        }
        return null;
    }

    private void ensureTypefaceBuilder() {
        if (mTypefaceBuilder == null) {
            mTypefaceBuilder = new TypefaceBuilder(getInheritedTypefaceBuilder());
        }
    }

    private void removeSpans(Class<?> clazz) {
        if (mSpannable == null) {
            return;
        }
        Object[] spansToRemove = mSpannable.getSpans(0, mSpannable.length(), clazz);
        for (Object span : spansToRemove) {
            mSpannable.removeSpan(span);
        }
    }

    private void applyColor() {
        if (mSpannable == null) {
            return;
        }

        removeSpans(ForegroundColorSpan.class);
        String color = mColor;
        if (TextUtils.isEmpty(color)) {
            color = getInheritedColor();
        }

        if (!TextUtils.isEmpty(color)) {
            mSpannable.setSpan(
                    new ForegroundColorSpan(ColorUtil.getColor(color)),
                    0,
                    mSpannable.length(),
                    Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        }

        applyParentSpannable();
    }

    private void applyFontSize() {
        if (mSpannable == null) {
            return;
        }

        int fontSize = mFontSize;
        if (fontSize <= 0) {
            fontSize = getInheritedFontSize();
        }
        removeSpans(AbsoluteSizeSpan.class);
        if (fontSize > 0) {
            mSpannable.setSpan(
                    new AbsoluteSizeSpan(fontSize),
                    0,
                    mSpannable.length(),
                    Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        }

        applyParentSpannable();
    }

    private void applyLineHeight() {
        if (mSpannable == null) {
            return;
        }

        removeSpans(LineHeightSpan.class);
        if (mLineHeight > 0) {
            mSpannable.setSpan(
                    new LineHeightSpan(mLineHeight),
                    0,
                    mSpannable.length(),
                    Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        }

        applyParentSpannable();
    }

    private void applyTextDecoration() {
        if (mSpannable == null) {
            return;
        }

        removeSpans(StrikethroughSpan.class);
        removeSpans(UnderlineSpan.class);
        if (mTextDecoration == TextDecoration.LINE_THROUGH) {
            mSpannable.setSpan(
                    new StrikethroughSpan(), 0, mSpannable.length(),
                    Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        } else if (mTextDecoration == TextDecoration.UNDERLINE) {
            mSpannable.setSpan(
                    new UnderlineSpan(), 0, mSpannable.length(),
                    Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        }

        applyParentSpannable();
    }

    public void applyFontFamily() {
        if (mSpannable == null) {
            return;
        }
        ensureTypefaceBuilder();
        removeSpans(CustomTypefaceSpan.class);
        mSpannable.setSpan(
                new CustomTypefaceSpan(mTypefaceBuilder.build()),
                0,
                mSpannable.length(),
                Spannable.SPAN_INCLUSIVE_EXCLUSIVE);

        applyParentSpannable();
    }

    private void applyParentSpannable() {
        if (getParentText() != null) {
            Text text = getParentText();
            text.setDirty(true);
            text.updateSpannable();
        }
    }

    public void onViewAttachedToWindow() {
        mFontNeedUpdate = true;
        Text parent = getParentText();
        if (parent != null && parent.isDirty()) {
            applyFontFamily();
        }
    }

    public void onViewDetachedFromWindow() {
        mFontNeedUpdate = false;
    }

    public void onParentFontParseComplete(Typeface typeface) {
        if (TextUtils.isEmpty(mFontFamily)) {
            ensureTypefaceBuilder();
            mTypefaceBuilder.setTypeface(typeface);
            applyFontFamily();
        }
    }

    @Override
    public void addChild(Component child, int index) {
        if (child == null) {
            mCallback.onJsException(
                    new IllegalArgumentException("Cannot add a null child component to Container"));
            return;
        }

        if (!(child instanceof Span || child instanceof Image)) {
            Log.w(TAG, "text not support child:" + child.getClass().getSimpleName());
            return;
        }

        if (index < 0 || index > getChildCount()) {
            index = getChildCount();
        }
        mChildren.add(index, child);
        applyParentSpannable();
    }

    @Override
    public List<Spannable> getChildrenSpannable() {
        if (getChildCount() <= 0) {
            return null;
        }
        List<Spannable> childrenSpannable = new ArrayList<>();
        for (Component comp : getChildren()) {
            if (comp instanceof Span) {
                Span span = (Span) comp;
                List<Spannable> childrenSpan = span.getChildrenSpannable();
                if (childrenSpan != null) {
                    childrenSpannable.addAll(childrenSpan);
                } else {
                    childrenSpannable.add(span.getSpannable());
                }
            } else if (comp instanceof Image) {
                Image image = (Image) comp;
                childrenSpannable.add(image.getSpannable());
            }
        }
        return childrenSpannable;
    }

    public TypefaceBuilder getTypefaceBuilder() {
        return mTypefaceBuilder;
    }

    public String getInheritedColor() {
        if (getParent() instanceof Span) {
            Span parent = (Span) getParent();
            return !TextUtils.isEmpty(parent.getColor()) ? parent.getColor() :
                    parent.getInheritedColor();
        } else if (getParent() instanceof Text) {
            Text parent = (Text) getParent();
            return parent.getColor();
        }
        return null;
    }

    public int getInheritedFontSize() {
        if (getParent() instanceof Span) {
            Span parent = (Span) getParent();
            return parent.getFontSize() != 0 ? parent.getFontSize() : parent.getInheritedFontSize();
        } else if (getParent() instanceof Text) {
            Text parent = (Text) getParent();
            return Math.round(parent.getFontSize());
        }
        return 0;
    }

    private Text getParentText() {
        Container parent = mParent;
        while (parent != null && !(parent instanceof Text)) {
            parent = parent.getParent();
        }
        return parent == null ? null : (Text) parent;
    }

    static class RecyclerItem extends Container.RecyclerItem {

        public RecyclerItem(int ref, RecyclerDataItem.ComponentCreator componentCreator) {
            super(ref, componentCreator);
        }

        @Override
        public void onDataChanged() {
            super.onDataChanged();
            if (getParent() != null) {
                getParent().onDataChanged();
            }
        }
    }
}

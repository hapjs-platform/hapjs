/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.text;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.LeadingMarginSpan;
import android.util.Log;
import android.view.Choreographer;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import com.facebook.fbui.textlayoutbuilder.TextLayoutBuilder;
import java.util.List;
import java.util.Map;
import org.hapjs.bridge.annotation.TypeAnnotation;
import org.hapjs.bridge.annotation.WidgetAnnotation;
import org.hapjs.common.utils.ColorUtil;
import org.hapjs.common.utils.DisplayUtil;
import org.hapjs.component.Component;
import org.hapjs.component.Container;
import org.hapjs.component.RecyclerDataItem;
import org.hapjs.component.SwipeObserver;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.component.constants.Attributes;
import org.hapjs.render.Page;
import org.hapjs.render.css.value.CSSValues;
import org.hapjs.runtime.HapEngine;
import org.hapjs.widgets.A;
import org.hapjs.widgets.Image;
import org.hapjs.widgets.Span;
import org.hapjs.widgets.view.text.LineHeightSpan;
import org.hapjs.widgets.view.text.TextDecoration;
import org.hapjs.widgets.view.text.TextLayoutView;
import org.hapjs.widgets.view.text.TextSpan;

@WidgetAnnotation(
        name = AbstractText.WIDGET_NAME,
        methods = {
                Component.METHOD_ANIMATE,
                Component.METHOD_GET_BOUNDING_CLIENT_RECT,
                Component.METHOD_TO_TEMP_FILE_PATH,
                Component.METHOD_FOCUS,
                Component.METHOD_TALKBACK_FOCUS,
                Component.METHOD_TALKBACK_ANNOUNCE
        },
        types = {@TypeAnnotation(name = Text.TYPE_TEXT, isDefault = true)})
public class Text extends AbstractText<TextLayoutView> implements SwipeObserver {

    protected static final String TAG = "Text";
    protected static final String TYPE_TEXT = "text";

    // attribute
    protected static final String FONT_FAMILY_DESC = "fontFamilyDesc";
    static final String DEFAULT_FONT_SIZE = "30px";
    static final String DEFAULT_COLOR = "#8a000000";
    protected final TextLayoutBuilder mLayoutBuilder = new TextLayoutBuilder();
    protected String mText;
    protected TextSpan mTextSpan = new TextSpan();
    protected String mTextIndent = "";
    private LeadingMarginSpan.Standard mTextIndentSpan;
    private String mColorStr;
    private FontParser mFontParser;
    private boolean mFontNeedUpdate = true;
    private ViewTreeObserver.OnPreDrawListener mPreDrawListener;
    private Choreographer.FrameCallback mSetTextCallback;
    private String mAriaLabel;

    public Text(
            HapEngine hapEngine,
            Context context,
            Container parent,
            int ref,
            RenderEventCallback callback,
            Map<String, Object> savedState) {
        super(hapEngine, context, parent, ref, callback, savedState);

        mLayoutBuilder
                .setTextSize(Attributes.getFontSize(mHapEngine, getPage(), DEFAULT_FONT_SIZE))
                .setTextColor(ColorUtil.getColor(DEFAULT_COLOR));
    }

    @Override
    protected TextLayoutView createViewImpl() {
        TextLayoutView textLayoutView = new TextLayoutView(mContext);
        textLayoutView.setComponent(this);
        textLayoutView.setGravity(getDefaultVerticalGravity());
        return textLayoutView;
    }

    protected void initTextFontLevel() {
        Page page = super.initFontLevel();
        mLayoutBuilder.setTextSize(Attributes.getFontSize(mHapEngine, page, DEFAULT_FONT_SIZE, this))
                .setTextColor(ColorUtil.getColor(DEFAULT_COLOR));
    }

    @Override
    public void applyAttrs(Map<String, Object> attrs, boolean force) {
        super.applyAttrs(attrs, force);
        updateSpannable();
        applyContentDescription();
    }

    @Override
    public void applyStyles(Map<String, ? extends CSSValues> attrs, boolean force) {
        super.applyStyles(attrs, force);
        updateSpannable();
        applyContentDescription();
    }

    // todo remove setHostView
    @Override
    public void setHostView(View view) {
        super.setHostView(view);
        setDirty(true);
        updateSpannable();
    }

    public void updateSpannable() {
        if (isDirty()) {
            postSetTextCallback();
        }
    }

    private void applyContentDescription() {
        if (mHost == null) {
            return;
        }

        String contentDescription;
        if (!TextUtils.isEmpty(mAriaLabel)) {
            contentDescription = mAriaLabel;
        } else {
            contentDescription = mText;
        }

        mHost.setContentDescription(contentDescription);
    }

    private void postSetTextCallback() {
        if (mSetTextCallback != null) {
            return;
        }
        mSetTextCallback =
                frameTimeNanos -> {
                    if (mHost != null) {
                        mHost.setText(applySpannable());
                    }
                    mSetTextCallback = null;
                };
        Choreographer.getInstance().postFrameCallback(mSetTextCallback);
    }

    private void removeSetTextCallback() {
        if (mSetTextCallback != null) {
            Choreographer.getInstance().removeFrameCallback(mSetTextCallback);
            mSetTextCallback = null;
        }
    }

    protected CharSequence applySpannable() {
        mTextSpan.setDirty(false);

        // apply host data.
        CharSequence hostText = "";
        if (!TextUtils.isEmpty(mText) && mTextSpan != null) {
            hostText = mTextSpan.createSpanned(mText);
        }

        if (mChildren.isEmpty() && TextUtils.isEmpty(mTextIndent)) {
            return hostText;
        }

        SpannableStringBuilder stringBuilder = new SpannableStringBuilder(hostText);
        for (Component child : mChildren) {
            if (child instanceof Span) {
                Span span = (Span) child;
                List<Spannable> childrenSpannable = span.getChildrenSpannable();
                if (childrenSpannable != null && !childrenSpannable.isEmpty()) {
                    for (int i = 0; i < childrenSpannable.size(); i++) {
                        Spannable s = childrenSpannable.get(i);
                        if (s != null) {
                            stringBuilder.append(s);
                        }
                    }
                } else if (span.getSpannable() != null) {
                    stringBuilder.append(span.getSpannable());
                }
            } else if (child instanceof A) {
                A a = (A) child;
                stringBuilder.append(a.applySpannable());
            } else if (child instanceof Image) {
                Image image = (Image) child;
                Spannable imageSpannable = image.getSpannable();
                if (imageSpannable != null) {
                    stringBuilder.append(imageSpannable);
                }
            }
        }
        // To ensure the line_height attribute take effect, apply it globally
        if (mTextSpan != null && mTextSpan.getLineHeight() > 0) {
            stringBuilder.setSpan(
                    new LineHeightSpan(mTextSpan.getLineHeight()),
                    0,
                    stringBuilder.length(),
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        }
        if (!mTextIndent.contains("%")) {
            mTextIndentSpan = new LeadingMarginSpan.Standard(parseTextIndent(mTextIndent), 0);
        }

        if (mTextIndentSpan != null) {
            stringBuilder.setSpan(
                    mTextIndentSpan, 0, stringBuilder.length(),
                    SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        if (mTextIndent.contains("%")
                && mHost != null
                && mPreDrawListener == null
                && mHost.isAttachedToWindow()) {
            mPreDrawListener =
                    new ViewTreeObserver.OnPreDrawListener() {
                        @Override
                        public boolean onPreDraw() {
                            if (!TextUtils.isEmpty(mTextIndent) && mTextIndent.contains("%")) {
                                int d = parseTextIndent(mTextIndent);
                                mTextIndentSpan = new LeadingMarginSpan.Standard(d, 0);
                                setDirty(true);
                                updateSpannable();
                            }

                            if (mHost != null && mPreDrawListener != null) {
                                mHost.getViewTreeObserver()
                                        .removeOnPreDrawListener(mPreDrawListener);
                                mPreDrawListener = null;
                            }
                            return true;
                        }
                    };
            mHost.getViewTreeObserver().addOnPreDrawListener(mPreDrawListener);
        }

        return stringBuilder;
    }

    public TextLayoutBuilder getLayoutBuilder() {
        return mLayoutBuilder;
    }

    @Override
    public void addChild(Component child, int index) {
        if (child == null) {
            throw new IllegalArgumentException("Cannot add a null child component to Container");
        }

        if (!(child instanceof Span || child instanceof A || child instanceof Image)) {
            Log.w(TAG, "text not support child:" + child.getClass().getSimpleName());
            return;
        }

        if (index < 0 || index > getChildCount()) {
            index = getChildCount();
        }
        mChildren.add(index, child);
        mTextSpan.setDirty(true);
        updateSpannable();
    }

    @Override
    public void removeChild(Component child) {
        if (child instanceof Span) {
            mChildren.remove(child);
            mTextSpan.setDirty(true);
        } else if (child instanceof A) {
            mChildren.remove(child);
            mTextSpan.setDirty(true);
        } else if (child instanceof Image) {
            mChildren.remove(child);
            mTextSpan.setDirty(true);
        }

        updateSpannable();
    }

    @Override
    protected boolean setAttribute(String key, Object attribute) {
        switch (key) {
            case Attributes.Style.LINES:
                int lines = Attributes.getInt(mHapEngine, attribute, -1);
                setLines(lines);
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
            case Attributes.Style.AUTO_LINE_HEIGHT:
                boolean autoLineHeight = Attributes.getBoolean(attribute, false);
                mAutoLineHeight = autoLineHeight;
                return true;
            case Attributes.Style.COLOR:
                String colorStr = Attributes.getString(attribute, getDefaultColor());
                setColor(colorStr);
                return true;
            case Attributes.Style.FONT_SIZE:
                int defaultFontSize = Attributes.getFontSize(mHapEngine, getPage(), getDefaultFontSize(), this);
                int fontSize = Attributes.getFontSize(mHapEngine, getPage(), attribute, defaultFontSize, this);
                setFontSize(fontSize);
                return true;
            case Attributes.Style.FONT_STYLE:
                String fontStyleStr = Attributes.getString(attribute, "normal");
                setFontStyle(fontStyleStr);
                return true;
            case Attributes.Style.FONT_WEIGHT:
                String fontWeightStr = Attributes.getString(attribute, "normal");
                setFontWeight(fontWeightStr);
                return true;
            case Attributes.Style.TEXT_DECORATION:
                String textDecorationStr = Attributes.getString(attribute, "none");
                setTextDecoration(textDecorationStr);
                return true;
            case Attributes.Style.TEXT_ALIGN:
                String textAlignStr = Attributes.getString(attribute, "left");
                setTextAlign(textAlignStr);
                return true;
            case Attributes.Style.VALUE:
            case Attributes.Style.CONTENT:
                String text = Attributes.getString(attribute, "");
                setText(text);
                return true;
            case Attributes.Style.TEXT_OVERFLOW:
                String textOverflow = Attributes.getString(attribute, Attributes.TextOverflow.CLIP);
                setTextOverflow(textOverflow);
                return true;
            case Attributes.Style.TEXT_INDENT:
                String textIndent = Attributes.getString(attribute, "");
                setTextIdent(textIndent);
                return true;
            case FONT_FAMILY_DESC:
                String fontFamily = Attributes.getString(attribute, null);
                setFontFamily(fontFamily);
                return true;
            case Attributes.Style.LETTER_SPACING:
                String letterSpacing = Attributes.getString(attribute, null);
                parseLetterSpacing(letterSpacing);
                return true;
            default:
                break;
        }
        return super.setAttribute(key, attribute);
    }

    public Object getAttribute(String key) {
        switch (key) {
            case Attributes.Style.LINES:
                return getLines();
            case Attributes.Style.LINE_HEIGHT:
                return null;
            case Attributes.Style.COLOR:
                return getColor();
            case Attributes.Style.FONT_SIZE:
                return getFontSize();
            case Attributes.Style.FONT_STYLE:
                return getFontStyle();
            case Attributes.Style.FONT_WEIGHT:
                return getFontWeight();
            case Attributes.Style.TEXT_DECORATION:
                return getTextDecoration();
            case Attributes.Style.TEXT_ALIGN:
                return getTextAlign();
            case Attributes.Style.VALUE:
            case Attributes.Style.CONTENT:
                return mText;
            case Attributes.Style.TEXT_OVERFLOW:
                return null;
            case Attributes.Style.TEXT_INDENT:
                return getTextIndent();
            default:
                break;
        }
        return super.getAttribute(key);
    }

    private int parseTextIndent(String textIndent) {
        if (TextUtils.isEmpty(textIndent)) {
            return 0;
        }
        textIndent = textIndent.trim();
        if (textIndent.endsWith("%")) {
            float percentResult = Attributes.getPercent(textIndent, 0);
            return Math.round(percentResult * mHost.getWidth());
        } else if (textIndent.endsWith("em")) {
            float em = Attributes.getEm(textIndent, 0);
            return Math.round(getFontSize() * em);
        } else if (textIndent.endsWith("px")) {
            return Attributes.getInt(mHapEngine, textIndent, 0);
        } else if (textIndent.endsWith("cm")) {
            float cm = Attributes.getCm(textIndent, 0);
            return DisplayUtil.parseCmToPx(mContext, cm);
        }
        return 0;
    }

    private void setTextIdent(String textIndent) {
        if (mHost == null) {
            return;
        }
        mTextIndent = textIndent;
        mTextSpan.setDirty(true);
        updateSpannable();
    }

    private String getTextIndent() {
        return mTextIndent;
    }

    public int getLines() {
        return mLayoutBuilder.getMaxLines();
    }

    public void setLines(int lines) {
        if (lines < 0) {
            lines = Integer.MAX_VALUE;
        }
        if (lines != mLayoutBuilder.getMaxLines()) {
            mTextSpan.setDirty(true);
            mLayoutBuilder.setMaxLines(lines);
        }
    }

    public void setLineHeight(int lineHeight) {
        if (lineHeight <= 0) {
            return;
        }

        mTextSpan.setLineHeight(lineHeight);

        updateSpannable();
    }

    public String getColor() {
        return mColorStr == null ? getDefaultColor() : mColorStr;
    }

    public void setColor(String colorStr) {
        if (TextUtils.isEmpty(colorStr)) {
            return;
        }

        mTextSpan.setColor(colorStr);
        mLayoutBuilder.setTextColor(ColorUtil.getColor(colorStr));
        mColorStr = colorStr;

        updateSpannable();
    }

    public float getFontSize() {
        return mLayoutBuilder.getTextSize();
    }

    public void setFontSize(int fontSize) {
        if (fontSize <= 0) {
            return;
        }

        mTextSpan.setFontSize(fontSize);
        mLayoutBuilder.setTextSize(fontSize);

        updateSpannable();
    }

    public String getFontStyle() {
        Typeface tf = mLayoutBuilder.getTypeface();
        if (tf == null || tf.getStyle() == Typeface.NORMAL) {
            return "normal";
        } else if (tf.getStyle() == Typeface.ITALIC || tf.getStyle() == Typeface.BOLD_ITALIC) {
            return "italic";
        } else {
            return null;
        }
    }

    public void setFontStyle(String fontStyleStr) {
        int style =
                TextUtils.equals(fontStyleStr, TypefaceBuilder.ITALIC) ? Typeface.ITALIC :
                        Typeface.NORMAL;
        mTextSpan.setFontStyle(style, getInheritedTypefaceBuilder());
        updateSpannable();
    }

    public String getFontWeight() {
        Typeface tf = mLayoutBuilder.getTypeface();
        if (tf == null || tf.getStyle() == Typeface.NORMAL) {
            return "normal";
        } else if (tf.getStyle() == Typeface.BOLD || tf.getStyle() == Typeface.BOLD_ITALIC) {
            return "bold";
        } else {
            return null;
        }
    }

    public void setFontWeight(String fontWeightStr) {
        int weight = TypefaceBuilder.parseFontWeight(fontWeightStr);
        mTextSpan.setFontWeight(weight, getInheritedTypefaceBuilder());
        updateSpannable();
    }

    public String getTextDecoration() {
        switch (mTextSpan.getTextDecoration()) {
            case TextDecoration.UNDERLINE:
                return "underline";
            case TextDecoration.LINE_THROUGH:
                return "line-throught";
            case TextDecoration.NONE:
            default:
                return "none";
        }
    }

    public void setTextDecoration(String textDecorationStr) {
        if (TextUtils.isEmpty(textDecorationStr)) {
            return;
        }

        int textDecoration = TextDecoration.NONE;
        if ("underline".equals(textDecorationStr)) {
            textDecoration = TextDecoration.UNDERLINE;
        } else if ("line-through".equals(textDecorationStr)) {
            textDecoration = TextDecoration.LINE_THROUGH;
        }

        mTextSpan.setTextDecoration(textDecoration);

        updateSpannable();
    }

    public String getTextAlign() {
        Layout.Alignment alignment = mLayoutBuilder.getAlignment();
        if (alignment == Layout.Alignment.ALIGN_CENTER) {
            return "center";
        } else if (alignment == Layout.Alignment.ALIGN_OPPOSITE) {
            return "right";
        } else {
            return "left";
        }
    }

    public void setTextAlign(String textAlignStr) {
        if (TextUtils.isEmpty(textAlignStr) || mHost == null) {
            return;
        }

        Layout.Alignment alignment = Layout.Alignment.ALIGN_NORMAL;
        if ("center".equals(textAlignStr)) {
            alignment = Layout.Alignment.ALIGN_CENTER;
        } else if ("right".equals(textAlignStr)) {
            alignment = Layout.Alignment.ALIGN_OPPOSITE;
        }
        if (mLayoutBuilder.getAlignment() != alignment) {
            mLayoutBuilder.setAlignment(alignment);
            mTextSpan.setDirty(true);
        }

        updateSpannable();
    }

    protected int getDefaultVerticalGravity() {
        return Gravity.CENTER_VERTICAL;
    }

    public String getText() {
        return mText;
    }

    public void setText(String text) {
        if (text.equals(mText)) {
            return;
        }

        mTextSpan.setDirty(true);
        mText = text;

        updateSpannable();
    }

    @Override
    public void setAriaLabel(String ariaLabel) {
        if (mHost == null) {
            return;
        }

        mAriaLabel = ariaLabel;
    }

    public void setTextOverflow(String textOverflow) {
        if (TextUtils.isEmpty(textOverflow)) {
            return;
        }

        TextUtils.TruncateAt ellipsize;
        if (Attributes.TextOverflow.ELLIPSIS.equals(textOverflow)) {
            ellipsize = TextUtils.TruncateAt.END;
        } else {
            ellipsize = null;
        }

        if (ellipsize != mLayoutBuilder.getEllipsize()) {
            mTextSpan.setDirty(true);
            mLayoutBuilder.setEllipsize(ellipsize);
        }
    }

    public void setFontFamily(final String fontFamily) {
        if (TextUtils.equals(fontFamily, mTextSpan.getFontFamily())) {
            return;
        }
        mTextSpan.setFontFamily(fontFamily);
        if (mFontParser == null) {
            mFontParser = new FontParser(mContext, this);
        }
        mFontParser.parse(
                fontFamily,
                new FontParser.ParseCallback() {
                    @Override
                    public void onParseComplete(final Typeface typeface) {
                        mTextSpan.setFontTypeface(typeface, getInheritedTypefaceBuilder());

                        // If the view of component detached, we should interrupt the callback, because the
                        // view may be reused by other component of which font family may be overridden.
                        // But we should update it when the view of component is attached again.
                        // (see #onViewAttachedToWindow())
                        if (mFontNeedUpdate) {
                            updateSpannable();
                        }
                        for (Component child : mChildren) {
                            if (child instanceof Span) {
                                ((Span) child).onParentFontParseComplete(typeface);
                            }
                        }
                    }
                });
    }
    private void parseLetterSpacing(String letterspacing) {
        if (letterspacing == null) {
            return;
        } else if (letterspacing.endsWith("normal")) {
            setLetterSpacing(0.0f);
        } else if (letterspacing.endsWith("dp")) {
            float dp = Attributes.getFloat(mHapEngine, letterspacing, 0, this);
            setLetterSpacing(dp/getFontSize());
        } else if (letterspacing.endsWith("px")) {
            float px = Attributes.getFloat(mHapEngine, letterspacing, 0, this);
            setLetterSpacing(px/getFontSize());
        } else if (letterspacing.endsWith("%")) {
            float percent = Attributes.getPercent(letterspacing, 0);
            setLetterSpacing(percent * getFontSize()/100);
        }
    }

    private void setLetterSpacing(float spacing) {
        mTextSpan.setLetterSpacing(spacing);
        mLayoutBuilder.setLetterSpacing(spacing);
        updateSpannable();
    }

    public TextSpan getTextSpan() {
        return mTextSpan;
    }

    protected TypefaceBuilder getInheritedTypefaceBuilder() {
        return null;
    }

    protected String getDefaultFontSize() {
        return DEFAULT_FONT_SIZE;
    }

    protected String getDefaultColor() {
        return DEFAULT_COLOR;
    }

    public boolean isDirty() {
        return mTextSpan.isDirty();
    }

    public void setDirty(boolean isDirty) {
        mTextSpan.setDirty(isDirty);
    }

    public void onViewAttachedToWindow() {
        mFontNeedUpdate = true;
        if (mTextSpan.isDirty()) {
            updateSpannable();
        }
        for (Component component : mChildren) {
            if (component instanceof Span) {
                ((Span) component).onViewAttachedToWindow();
            }
        }
    }

    public void onViewDetachedFromWindow() {
        mFontNeedUpdate = false;
        for (Component component : mChildren) {
            if (component instanceof Span) {
                ((Span) component).onViewDetachedFromWindow();
            }
        }
        if (mHost != null && mPreDrawListener != null) {
            mHost.getViewTreeObserver().removeOnPreDrawListener(mPreDrawListener);
            mPreDrawListener = null;
        }
    }

    @Override
    public void destroy() {
        removeSetTextCallback();
        super.destroy();
    }

    public static class RecyclerItem extends Container.RecyclerItem {

        public RecyclerItem(int ref, ComponentCreator componentCreator) {
            super(ref, componentCreator);
        }

        @Override
        public void onChildAdded(RecyclerDataItem child, int index) {
            super.onChildAdded(child, index);
            onDataChanged();
        }

        @Override
        public void onChildRemoved(RecyclerDataItem child, int index) {
            super.onChildRemoved(child, index);
            onDataChanged();
        }

        @Override
        public void onDataChanged() {
            super.onDataChanged();
            if (getParent() instanceof Text.RecyclerItem) {
                getParent().onDataChanged();
            }
        }
    }
}

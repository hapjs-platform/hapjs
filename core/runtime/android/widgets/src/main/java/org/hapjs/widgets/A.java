/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.view.MotionEvent;
import android.view.View;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hapjs.bridge.annotation.WidgetAnnotation;
import org.hapjs.component.Component;
import org.hapjs.component.Container;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.component.constants.Attributes;
import org.hapjs.component.view.gesture.GestureDispatcher;
import org.hapjs.runtime.HapEngine;
import org.hapjs.widgets.text.Text;
import org.hapjs.widgets.text.TypefaceBuilder;
import org.hapjs.widgets.view.text.TextLayoutView;

@WidgetAnnotation(
        name = A.WIDGET_NAME,
        methods = {
                Component.METHOD_ANIMATE,
                Component.METHOD_GET_BOUNDING_CLIENT_RECT,
                Component.METHOD_TO_TEMP_FILE_PATH,
                Component.METHOD_FOCUS,
                Component.METHOD_TALKBACK_FOCUS,
                Component.METHOD_TALKBACK_ANNOUNCE
        })
public class A extends Text {

    protected static final String WIDGET_NAME = "a";

    private String mHRef;
    private boolean mStyledText = false;

    public A(
            HapEngine hapEngine,
            Context context,
            Container parent,
            int ref,
            RenderEventCallback callback,
            Map<String, Object> savedState) {
        super(hapEngine, context, parent, ref, callback, savedState);
        if (parent instanceof Text) {
            mStyledText = true;
        }
    }

    @Override
    protected TextLayoutView createViewImpl() {
        if (mStyledText) {
            return null;
        }
        return super.createViewImpl();
    }

    @Override
    protected boolean setAttribute(String key, Object attribute) {
        switch (key) {
            case Attributes.Style.HREF:
                String href = Attributes.getString(attribute);
                setHRef(href);
                return true;
            default:
                break;
        }
        return super.setAttribute(key, attribute);
    }

    @Override
    public void addChild(Component child, int index) {
        if (child instanceof InnerSpannable) {
            mChildren.add(child);
            setDirty(true);
        }
        updateSpannable();
    }

    @Override
    public void removeChild(Component child) {
        if (child instanceof InnerSpannable) {
            mChildren.remove(child);
            setDirty(true);
        }
        updateSpannable();
    }

    public void setHRef(final String href) {
        mTextSpan.setDirty(true);
        mHRef = href;
        updateSpannable();
    }

    @Override
    public CharSequence applySpannable() {
        mTextSpan.setDirty(false);

        // apply host data.
        CharSequence hostText = "";
        if (!TextUtils.isEmpty(mText)) {
            Spannable spannable = mTextSpan.createSpanned(mText);
            initSpannableClick(spannable);
            hostText = spannable;
        }

        if (mChildren.isEmpty()) {
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
            } else if (child instanceof Image) {
                Image image = (Image) child;
                Spannable imageSpannable = image.getSpannable();
                if (imageSpannable != null) {
                    stringBuilder.append(imageSpannable);
                }
            }
        }
        if (!TextUtils.isEmpty(stringBuilder.toString()) && TextUtils.isEmpty(mText)) {
            initSpannableClick(stringBuilder);
        }

        return stringBuilder;
    }

    private void initSpannableClick(Spannable spannable) {
        if (null != spannable) {
            final boolean isClickEventDefined = getDomEvents().contains(Attributes.Event.CLICK);
            final boolean isHRefDefined = !TextUtils.isEmpty(mHRef);
            if (isHRefDefined || isClickEventDefined) {
                spannable.setSpan(
                        new ClickableSpan() {

                            @Override
                            public void onClick(View widget) {
                                if (isClickEventDefined && mParent instanceof Text) {
                                    GestureDispatcher dispatcher =
                                            GestureDispatcher
                                                    .createInstanceIfNecessary(getCallback());
                                    dispatcher.put(getPageId(), mRef, Attributes.Event.CLICK, null,
                                            null);
                                }
                                if (!TextUtils.isEmpty(mHRef)) {
                                    mCallback.loadUrl(mHRef);
                                    Map<String, Object> visitedMap = new HashMap<>();
                                    visitedMap.put("visited", "true");
                                    applyAttrs(visitedMap, true);
                                    updateSpannable();
                                }
                            }

                            @Override
                            public void updateDrawState(TextPaint ds) {
                                // no style.
                            }
                        },
                        0,
                        spannable.length(),
                        Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            }
            addTouchListener(
                    TOUCH_TYPE_ACTIVE,
                    new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                                case MotionEvent.ACTION_DOWN:
                                    mHost.setPressed(true);
                                    break;
                                case MotionEvent.ACTION_UP:
                                case MotionEvent.ACTION_OUTSIDE:
                                case MotionEvent.ACTION_CANCEL:
                                    mHost.setPressed(false);
                                    break;
                                default:
                                    break;
                            }
                            return false;
                        }
                    });
        }
    }

    @Override
    public void setDirty(boolean isDirty) {
        if (mStyledText) {
            if (mParent instanceof Text) {
                ((Text) mParent).setDirty(isDirty);
            }
        } else {
            mTextSpan.setDirty(isDirty);
        }
    }

    @Override
    public void updateSpannable() {
        if (mStyledText) {
            if (mParent instanceof Text) {
                Text parentText = (Text) mParent;
                if (mTextSpan.isDirty()) {
                    parentText.setDirty(true);
                }
                parentText.updateSpannable();
            }
        } else {
            super.updateSpannable();
        }
    }

    @Override
    protected TypefaceBuilder getInheritedTypefaceBuilder() {
        if (getParent() instanceof Text) {
            Text parent = (Text) getParent();
            return parent.getTextSpan().getTypefaceBuilder();
        }
        return null;
    }
}

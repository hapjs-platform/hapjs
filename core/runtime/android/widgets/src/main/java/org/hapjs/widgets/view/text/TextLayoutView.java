/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.view.text;

import android.content.Context;
import android.graphics.Canvas;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.text.style.ImageSpan;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import com.facebook.fbui.textlayoutbuilder.TextLayoutBuilder;
import com.facebook.yoga.YogaNode;
import org.hapjs.component.Component;
import org.hapjs.component.utils.YogaUtil;
import org.hapjs.component.view.ComponentHost;
import org.hapjs.component.view.gesture.GestureHost;
import org.hapjs.component.view.gesture.IGesture;
import org.hapjs.component.view.helper.StateHelper;
import org.hapjs.component.view.keyevent.KeyEventDelegate;
import org.hapjs.widgets.text.Text;

public class TextLayoutView extends View implements ComponentHost, GestureHost {

    private Text mComponent;
    private int mGravity = Gravity.TOP | Gravity.LEFT;
    private IGesture mGesture;
    private KeyEventDelegate mKeyEventDelegate;

    private int mLastLayoutMeasureSpec = MeasureSpec.UNSPECIFIED;
    private Layout mLayout;
    private CharSequence mText;
    private CharSequence mLastText;

    public TextLayoutView(Context context) {
        super(context);
    }

    public void setText(CharSequence text) {
        if (TextUtils.isEmpty(mText) && TextUtils.isEmpty(text)) {
            return;
        }

        mText = text;

        if (mLayout == null) {
            requestLayout();
            invalidate();
            return;
        }

        Layout oldLayout = mLayout;
        mLayout = buildLayout(mLastLayoutMeasureSpec); // 根据 buildLayout 的尺寸决定是否需要 requestLayout
        if ((null == mLastText || !mLastText.toString().equals(mText.toString()))
                || (!isSizeEquals(oldLayout, mLayout))) {
            requestLayout();
        }
        invalidate();
        mLastText = text;
    }

    public boolean containClickSpan() {
        if (mText instanceof Spannable) {
            ClickableSpan[] spans =
                    ((Spannable) mText).getSpans(0, mText.length() - 1, ClickableSpan.class);
            return (spans.length > 0);
        }
        return false;
    }

    private boolean containImageSpan() {
        if (mText instanceof Spannable) {
            ImageSpan[] spans = ((Spannable) mText).getSpans(
                    0,
                    mText.length() - 1,
                    ImageSpan.class);
            return (spans.length > 0);
        }
        return false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.save();

        if (mLayout != null) {
            int paddingLeft = getPaddingLeft();
            int paddingRight = getPaddingRight();
            float x = paddingLeft;
            if (mLayout.getLineCount() == 1) {
                if (mLayout.getAlignment() == Layout.Alignment.ALIGN_CENTER) {
                    x = (getWidth() - paddingLeft - paddingRight - mLayout.getWidth()) / 2.0f
                            + paddingLeft;
                } else if (mLayout.getAlignment() == Layout.Alignment.ALIGN_OPPOSITE) {
                    x = getWidth() - paddingRight - mLayout.getWidth();
                }
            }

            float paddingTop = getPaddingTop() + mLayout.getSpacingAdd() / 2;
            float paddingBottom = (getPaddingBottom() + mLayout.getSpacingAdd() / 2);
            float y = paddingTop;
            float layoutHeight = mLayout.getHeight();
            // Above API 26,Layout#getLineCount() may be larger than max line count.
            boolean needClip = mComponent.getLines() < mLayout.getLineCount();
            if (needClip) {
                layoutHeight = mLayout.getLineBottom(mComponent.getLines() - 1);
            }
            if ((mGravity & Gravity.VERTICAL_GRAVITY_MASK) == Gravity.CENTER_VERTICAL) {
                y = (getHeight() - (layoutHeight + paddingTop + paddingBottom)) / 2 + paddingTop;
            } else if ((mGravity & Gravity.VERTICAL_GRAVITY_MASK) == Gravity.BOTTOM) {
                y = getHeight() - paddingBottom - layoutHeight;
            }

            if (needClip) {
                canvas.clipRect(x, y, mLayout.getWidth() + x, layoutHeight + y);
            }
            canvas.translate(x, y);
            mLayout.draw(canvas);
        }

        canvas.restore();
    }

    private boolean isSizeEquals(Layout first, Layout second) {
        if (first == second) {
            return true;
        }
        if (first == null || second == null) {
            return false;
        }
        return first.getWidth() == second.getWidth() && first.getHeight() == second.getHeight();
    }

    @Override
    public void requestLayout() {
        YogaNode node = YogaUtil.getYogaNode(this);
        if (node != null) {
            node.dirty();
        }
        super.requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int widthMeasureSpecWithoutPadding = getWidthMeasureNoPadding(widthMeasureSpec);
        updateLayoutIfNecessary(widthMeasureSpecWithoutPadding);

        int width = 0;
        int height = 0;
        if (widthMode == MeasureSpec.EXACTLY) {
            width = widthSize;
        } else {
            if (mLayout != null) {
                width = mLayout.getWidth();
            }
            width += getPaddingLeft() + getPaddingRight();

            width = Math.max(width, getSuggestedMinimumWidth());

            if (widthMode == MeasureSpec.AT_MOST) {
                width = Math.min(widthSize, width);
            } else if (widthMode == MeasureSpec.UNSPECIFIED) {
                // yoga bug: use [size, UNSPECIFIED] to measure node with percent width.
                width = Math.max(widthSize, width);
            }
        }

        if (heightMode == MeasureSpec.EXACTLY) {
            height = heightSize;
        } else {
            if (mLayout != null) {
                // Above API 26,Layout#getLineCount() may be larger than max line count.
                int layoutHeight = mLayout.getHeight();
                if (mComponent.getLines() < mLayout.getLineCount()) {
                    layoutHeight = mLayout.getLineBottom(mComponent.getLines() - 1);
                }
                height = Math.round(layoutHeight + mLayout.getSpacingAdd());
            }
            height += getPaddingTop() + getPaddingBottom();

            height = Math.max(height, getSuggestedMinimumHeight());

            if (heightMode == MeasureSpec.AT_MOST) {
                height = Math.min(heightSize, height);
            }
        }

        setMeasuredDimension(width, height);
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setText(mText);
    }

    private void updateLayoutIfNecessary(int widthMeasureSpec) {
        if (mLayout != null) {
            if (mLastLayoutMeasureSpec == widthMeasureSpec) {
                return;
            }

            // YogaLayout 在 onLayout 阶段会用缓存的 layoutSize EXACTLY measure 所有的子View. 这种情况不用重新计算Layout
            int widthMode = View.MeasureSpec.getMode(widthMeasureSpec);
            int widthSize = View.MeasureSpec.getSize(widthMeasureSpec);
            if (widthMode == View.MeasureSpec.EXACTLY && widthSize == mLayout.getWidth()) {
                return;
            }
        }

        mLayout = buildLayout(widthMeasureSpec);
        mLastLayoutMeasureSpec = widthMeasureSpec;
    }

    private Layout buildLayout(int widthMeasureSpec) {
        if (TextUtils.isEmpty(mText)) {
            return null;
        }
        TextLayoutBuilder layoutBuilder = mComponent.getLayoutBuilder();
        int widthMode = View.MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = View.MeasureSpec.getSize(widthMeasureSpec);
        layoutBuilder.setWidth(widthSize, convertToLayoutBuilderMode(widthMode));
        layoutBuilder.setText(mText);
        // 包含ImageSpan的TextLayout不加入缓存，避免因缓存静态引用造成图片资源不能及时回收
        layoutBuilder.setShouldCacheLayout(!containImageSpan());
        return layoutBuilder.build();
    }

    private int convertToLayoutBuilderMode(int widthMode) {
        switch (widthMode) {
            case View.MeasureSpec.UNSPECIFIED:
                return TextLayoutBuilder.MEASURE_MODE_UNSPECIFIED;
            case View.MeasureSpec.EXACTLY:
                return TextLayoutBuilder.MEASURE_MODE_EXACTLY;
            case View.MeasureSpec.AT_MOST:
                return TextLayoutBuilder.MEASURE_MODE_AT_MOST;
            default:
                break;
        }
        throw new IllegalStateException();
    }

    private int getWidthMeasureNoPadding(int widthMeasureSpec) {
        int widthMode = View.MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = View.MeasureSpec.getSize(widthMeasureSpec);

        int widthNoPadding = widthSize - getPaddingLeft() - getPaddingRight();
        if (widthNoPadding < 0) {
            widthNoPadding = 0;
        }

        return View.MeasureSpec.makeMeasureSpec(widthNoPadding, widthMode);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean result = false;
        if (mLayout != null && mLayout.getText() instanceof Spanned) {
            if (updateSelection(event, (Spanned) mLayout.getText(), mLayout)) {
                result = true;
            }
        }
        result |= super.onTouchEvent(event);
        if (mGesture != null) {
            result |= mGesture.onTouch(event);
        }

        return result;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean result = super.onKeyDown(keyCode, event);
        return onKey(KeyEvent.ACTION_DOWN, keyCode, event, result);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        boolean result = super.onKeyUp(keyCode, event);
        return onKey(KeyEvent.ACTION_UP, keyCode, event, result);
    }

    private boolean onKey(int keyAction, int keyCode, KeyEvent event, boolean result) {
        if (mKeyEventDelegate == null) {
            mKeyEventDelegate = new KeyEventDelegate(mComponent);
        }
        result |= mKeyEventDelegate.onKey(keyAction, keyCode, event);
        return result;
    }

    private boolean updateSelection(MotionEvent event, Spanned buffer, Layout layout) {
        int action = event.getAction();

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_DOWN) {
            int x = (int) event.getX();
            int y = (int) event.getY();

            x -= getPaddingLeft();
            y -= getPaddingTop();

            x += getScrollX();
            y += getScrollY();

            int line = layout.getLineForVertical(y);
            int off = layout.getOffsetForHorizontal(line, x);

            ClickableSpan[] link = buffer.getSpans(off, off, ClickableSpan.class);

            if (link.length != 0) {
                if (action == MotionEvent.ACTION_UP) {
                    link[0].onClick(this);
                } else {
                    if (buffer instanceof Spannable) {
                        Selection.setSelection(
                                (Spannable) buffer, buffer.getSpanStart(link[0]),
                                buffer.getSpanEnd(link[0]));
                    }
                }

                return true;
            } else {
                if (buffer instanceof Spannable) {
                    Selection.removeSelection((Spannable) buffer);
                }
            }
        }

        return false;
    }

    public void setGravity(int gravity) {
        mGravity = gravity;
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        StateHelper.onStateChanged(this, mComponent);
    }

    @Override
    public Component getComponent() {
        return mComponent;
    }

    @Override
    public void setComponent(Component component) {
        mComponent = (Text) component;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mComponent.onViewAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mComponent.onViewDetachedFromWindow();
    }

    @Override
    public IGesture getGesture() {
        return mGesture;
    }

    @Override
    public void setGesture(IGesture gestureDelegate) {
        mGesture = gestureDelegate;
    }
}

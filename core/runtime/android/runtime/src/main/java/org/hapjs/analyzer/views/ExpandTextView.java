/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.analyzer.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

import org.hapjs.runtime.R;

public class ExpandTextView extends AppCompatTextView {
    private static final String ELLIPSIZE = "...";
    private static final String DEFAULT_MORE = "Show More";
    private static final String DEFAULT_LESS = "Show Less";
    public static final int MAX_AVAILABLE_TEXT_LEN = 3000;
    private CharSequence mOriginText;
    private CharSequence mShowMoreText = DEFAULT_MORE;
    private CharSequence mShowLessText = DEFAULT_LESS;
    private int mShowMoreTextColor = Color.TRANSPARENT;
    private int mShowLessTextColor = Color.TRANSPARENT;
    private int mLines = 1; // When the number of lines exceeds mLines, the display is expanded, otherwise it is displayed normally. The default value is 1.
    private boolean mExpanded;
    private int mExpectedLines;
    private Callback mCallback;
    private OnClickListener mOnClickListener;

    public interface Callback {
        void onExpand();

        void onCollapse();
    }

    public ExpandTextView(Context context) {
        super(context);
        setMovementMethod(LinkMovementMethod.getInstance());
    }

    public ExpandTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ExpandTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.ExpandTextView, defStyleAttr, 0);
        CharSequence moreText = ta.getText(R.styleable.ExpandTextView_moreText);
        if (TextUtils.isEmpty(moreText)) {
            moreText = DEFAULT_MORE;
        }
        mShowMoreText = moreText;
        CharSequence lessText = ta.getText(R.styleable.ExpandTextView_lessText);
        if (TextUtils.isEmpty(lessText)) {
            lessText = DEFAULT_LESS;
        }
        mShowLessText = lessText;
        mShowMoreTextColor = ta.getColor(R.styleable.ExpandTextView_moreTextColor, Color.TRANSPARENT);
        mShowLessTextColor = ta.getColor(R.styleable.ExpandTextView_lessTextColor, Color.TRANSPARENT);
        int expandLines = ta.getInt(R.styleable.ExpandTextView_expandLines, 1);
        if (expandLines > 0) {
            mLines = expandLines;
        }
        ta.recycle();
        setMovementMethod(LinkMovementMethod.getInstance());
    }

    public void setRenderText(CharSequence text, boolean expand) {
        mExpanded = expand;
        setRenderText(text);
    }

    private void setRenderText(CharSequence text) {
        setText(text);
        mOriginText = text;
        post(() -> setupButtons(true));
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    @Override
    public void setLines(int lines) {
        throw new UnsupportedOperationException("ExpandTextView does not support the method of setLines(int lines) to be called");
    }

    /**
     * Set new text content according to mExpanded
     * @param originText It is true only when called in the method {@link #setRenderText(CharSequence text)}
     */
    private void setupButtons(boolean originText) {
        if (originText) {
            // Get the number of rows required for normal display
            mExpectedLines = getLineCount();
        }
        if (mLines <= 0 || mExpectedLines <= 0) {
            return;
        }
        if (mExpanded) {
            setMaxLines(Integer.MAX_VALUE);
            setupShowLessButton();
        } else {
            setMaxLines(mLines);
            setupShowMoreButton();
        }
    }

    /**
     * try to show suffix of "show more" if expected number of rows is greater than mLines
     */
    private void setupShowMoreButton() {
        if (TextUtils.isEmpty(mOriginText)) {
            return;
        }
        if (mLines >= mExpectedLines) {
            SpannableString spannableString = new SpannableString(mOriginText);
            spannableString.setSpan(new ClickableSpan() {
                                        @Override
                                        public void updateDrawState(TextPaint ds) {
                                            ds.setUnderlineText(false);
                                        }

                                        @Override
                                        public void onClick(@Nullable View view) {
                                            if (mOnClickListener != null) {
                                                mOnClickListener.onClick(view);
                                            }
                                        }
                                    },
                    0,
                    mOriginText.length(), 0);
            setText(spannableString, TextView.BufferType.SPANNABLE);
            return;
        }
        mExpanded = false;
        int start = 0;
        int end;
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < mLines - 1; i++) {
            end = getLayout().getLineEnd(i);
            builder.append(mOriginText.toString().substring(start, end));
            start = end;
        }
        // Other text content except the last line
        String preText = builder.toString();
        // The text content of the last line
        end = getLayout().getLineEnd(mLines - 1);
        String lastLineText = mOriginText.toString().substring(start, end);
        // Prevent the inconsistency of Chinese and English widths from causing incomplete display of the "More" button
        String tempText = lastLineText + ELLIPSIZE + mShowMoreText;
        float measureLength = getPaint().measureText(tempText);
        while (measureLength >= getWidth() - getPaddingLeft() - getPaddingRight() && lastLineText.length() > 0) {
            lastLineText = lastLineText.substring(0, lastLineText.length() - 1);
            tempText = lastLineText + ELLIPSIZE + mShowMoreText;
            measureLength = getPaint().measureText(tempText);
        }
        tempText = preText + tempText;
        SpannableString spannableString = new SpannableString(tempText);
        spannableString.setSpan(new ClickableSpan() {
                                    @Override
                                    public void updateDrawState(TextPaint ds) {
                                        ds.setUnderlineText(false);
                                    }

                                    @Override
                                    public void onClick(@Nullable View view) {
                                        mExpanded = true;
                                        post(() -> setupButtons(false));
                                    }
                                },
                tempText.length() - mShowMoreText.length(),
                tempText.length(), 0);
        spannableString.setSpan(new ForegroundColorSpan(mShowMoreTextColor),
                tempText.length() - mShowMoreText.length(),
                tempText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(new ClickableSpan() {
                                    @Override
                                    public void updateDrawState(TextPaint ds) {
                                        ds.setUnderlineText(false);
                                    }

                                    @Override
                                    public void onClick(@Nullable View view) {
                                        if (mOnClickListener != null) {
                                            mOnClickListener.onClick(view);
                                        }
                                    }
                                },
                0,
                tempText.length() - mShowMoreText.length(), 0);
        setText(spannableString, TextView.BufferType.SPANNABLE);
        if (mCallback != null) {
            mCallback.onCollapse();
        }
    }

    /**
     * try to show suffix of "show less" when expected number of rows is greater than mLines
     */
    private void setupShowLessButton() {
        if (TextUtils.isEmpty(mOriginText)) {
            return;
        }
        if (mLines >= mExpectedLines) {
            SpannableString spannableString = new SpannableString(mOriginText);
            spannableString.setSpan(new ClickableSpan() {
                                        @Override
                                        public void updateDrawState(TextPaint ds) {
                                            ds.setUnderlineText(false);
                                        }

                                        @Override
                                        public void onClick(@Nullable View view) {
                                            if (mOnClickListener != null) {
                                                mOnClickListener.onClick(view);
                                            }
                                        }
                                    },
                    0,
                    mOriginText.length(), 0);
            setText(spannableString, TextView.BufferType.SPANNABLE);
            return;
        }
        mExpanded = true;
        String text = mOriginText + ELLIPSIZE + mShowLessText;
        SpannableString spannableString = new SpannableString(text);

        spannableString.setSpan(new ClickableSpan() {
                                    @Override
                                    public void updateDrawState(TextPaint ds) {
                                        ds.setUnderlineText(false);
                                    }

                                    @Override
                                    public void onClick(@Nullable View view) {
                                        mExpanded = false;
                                        post(() -> setupButtons(false));
                                    }
                                },
                text.length() - mShowLessText.length(),
                text.length(), 0);
        spannableString.setSpan(new ForegroundColorSpan(mShowLessTextColor),
                text.length() - mShowLessText.length(),
                text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(new ClickableSpan() {
                                    @Override
                                    public void updateDrawState(TextPaint ds) {
                                        ds.setUnderlineText(false);
                                    }

                                    @Override
                                    public void onClick(@Nullable View view) {
                                        if (mOnClickListener != null) {
                                            mOnClickListener.onClick(view);
                                        }
                                    }
                                },
                0,
                text.length() - mShowLessText.length(), 0);
        setText(spannableString, TextView.BufferType.SPANNABLE);
        if (mCallback != null) {
            mCallback.onExpand();
        }
    }

    @Override
    public void setOnClickListener(OnClickListener l) {
        mOnClickListener = l;
    }

    public void setShowMoreText(String showMoreText) {
        mShowMoreText = showMoreText;
        setRenderText(mOriginText);
    }

    public void setShowLessText(String showLessText) {
        mShowLessText = showLessText;
        setRenderText(mOriginText);
    }

    public void setShowMoreColor(int color) {
        mShowMoreTextColor = color;
        setRenderText(mOriginText);
    }

    public void setShowLessTextColor(int color) {
        mShowLessTextColor = color;
        setRenderText(mOriginText);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mOriginText = getText();
        if (mShowMoreTextColor == Color.TRANSPARENT) {
            mShowMoreTextColor = getTextColors().getDefaultColor();
        }
        if (mShowLessTextColor == Color.TRANSPARENT) {
            mShowLessTextColor = getTextColors().getDefaultColor();
        }
    }
}

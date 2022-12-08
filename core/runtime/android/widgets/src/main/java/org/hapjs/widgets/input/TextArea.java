/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.input;

import android.content.Context;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.TextView;
import java.util.HashMap;
import java.util.Map;
import org.hapjs.bridge.annotation.WidgetAnnotation;
import org.hapjs.common.utils.ColorUtil;
import org.hapjs.component.Component;
import org.hapjs.component.Container;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.component.constants.Attributes;
import org.hapjs.render.Page;
import org.hapjs.runtime.HapEngine;
import org.hapjs.widgets.view.text.FlexEditText;

@WidgetAnnotation(
        name = TextArea.WIDGET_NAME,
        methods = {
                Component.METHOD_FOCUS,
                Component.METHOD_ANIMATE,
                Component.METHOD_GET_BOUNDING_CLIENT_RECT,
                Component.METHOD_TO_TEMP_FILE_PATH,
                Edit.METHOD_SELECT,
                Edit.METHOD_SET_SELECTION_RANGE,
                Edit.METHOD_GET_SELECTION_RANGE,
                Component.METHOD_TALKBACK_FOCUS,
                Component.METHOD_TALKBACK_ANNOUNCE
        })
public class TextArea extends Edit {

    protected static final String WIDGET_NAME = "textarea";
    private static final String HEIGHT = "height";
    private static final String LINE_COUNT = "lineCount";
    private static final String EVENT_LINE_CHANGE = "linechange";
    private TextLineWatcherRunnable mTextLineWatcherRunnable;
    private TextWatcher mTextLineWatcher;
    private boolean mIsDestroy = false;

    public TextArea(
            HapEngine hapEngine,
            Context context,
            Container parent,
            int ref,
            RenderEventCallback callback,
            Map<String, Object> savedState) {
        super(hapEngine, context, parent, ref, callback, savedState);
    }

    @Override
    protected TextView createViewImpl() {
        FlexEditText editText = new FlexEditText(mContext);
        editText.setComponent(this);
        initDefaultView(editText);
        return editText;
    }

    @Override
    protected void initDefaultView(TextView editText) {
        Page page = initFontLevel();
        editText.setTextSize(TypedValue.COMPLEX_UNIT_PX, Attributes.getFontSize(mHapEngine, page, DEFAULT_FONT_SIZE, this));
        editText.setTextColor(ColorUtil.getColor(DEFAULT_COLOR));
        editText.setHintTextColor(ColorUtil.getColor(DEFAULT_PLACEHOLDER_COLOR));
        editText.setBackground(null);
        editText.setGravity(Gravity.TOP | Gravity.START);

        int minWidth = Attributes.getInt(mHapEngine, DEFAULT_WIDTH, this);
        editText.setMinWidth(minWidth);
        editText.setMinimumWidth(minWidth);

        int minHeight = editText.getLineHeight() * 2;
        editText.setMinHeight(minHeight);
        editText.setMinimumHeight(minHeight);
        setTextWatcher(editText);
    }

    @Override
    protected boolean setAttribute(String key, Object attribute) {
        switch (key) {
            case Attributes.Style.LINES:
                int lines = Attributes.getInt(mHapEngine, attribute, -1);
                setLines(lines);
                return true;
            default:
                break;
        }
        return super.setAttribute(key, attribute);
    }

    @Override
    protected boolean addEvent(String event) {
        if (TextUtils.isEmpty(event) || mHost == null) {
            return false;
        }
        if (EVENT_LINE_CHANGE.equals(event)) {
            if (mTextLineWatcherRunnable == null) {
                mTextLineWatcherRunnable = new TextLineWatcherRunnable();
            }
            if (mTextLineWatcher == null) {
                mTextLineWatcher =
                        new TextWatcher() {
                            private String beforeText;

                            @Override
                            public void beforeTextChanged(CharSequence s, int start, int count,
                                                          int after) {
                                beforeText = s == null ? "" : s.toString();
                            }

                            @Override
                            public void onTextChanged(CharSequence s, int start, int before,
                                                      int count) {
                            }

                            @Override
                            public void afterTextChanged(Editable s) {
                                String lastValue = s == null ? "" : s.toString();
                                if (lastValue.equals(beforeText)) {
                                    return;
                                }
                                if (mHost.getHandler() != null) {
                                    Handler handler = mHost.getHandler();
                                    handler.removeCallbacks(mTextLineWatcherRunnable);
                                    if (TextUtils.isEmpty(lastValue)) {
                                        mTextLineWatcherRunnable.mLineCount = 0;
                                    } else {
                                        mTextLineWatcherRunnable.mLineCount = mHost.getLineCount();
                                    }
                                    mTextLineWatcherRunnable.mLineHeight = mHost.getHeight();
                                    handler.postDelayed(mTextLineWatcherRunnable, 16);
                                }
                            }
                        };
            }
            mHost.removeTextChangedListener(mTextLineWatcher);
            mHost.addTextChangedListener(mTextLineWatcher);
            return true;
        }
        return super.addEvent(event);
    }

    @Override
    protected boolean removeEvent(String event) {
        if (EVENT_LINE_CHANGE.equals(event)) {
            mHost.removeTextChangedListener(mTextLineWatcher);
            return true;
        }
        return super.removeEvent(event);
    }

    @Override
    public void destroy() {
        mIsDestroy = true;
        super.destroy();
    }

    private void setLines(int lines) {
        if (mHost == null) {
            return;
        }

        mHost.setMaxLines(lines);
        mHost.setGravity(
                (Gravity.HORIZONTAL_GRAVITY_MASK & mHost.getGravity())
                        | getDefaultVerticalGravity());
    }

    private class TextLineWatcherRunnable implements Runnable {
        public int mLineHeight = 0;
        public int mLineCount = 0;

        @Override
        public void run() {
            if (mCallback == null || mIsDestroy) {
                return;
            }
            Map<String, Object> params = new HashMap<>(2);
            params.put(HEIGHT, mLineHeight);
            params.put(LINE_COUNT, mLineCount);
            mCallback.onJsEventCallback(
                    getPageId(), mRef, EVENT_LINE_CHANGE, TextArea.this, params, null);
        }
    }
}

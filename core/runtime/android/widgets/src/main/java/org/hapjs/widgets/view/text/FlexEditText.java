/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.view.text;

import android.content.Context;
import android.text.Editable;
import android.text.Spannable;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;
import android.view.inputmethod.InputMethodManager;
import androidx.appcompat.widget.AppCompatAutoCompleteTextView;
import org.hapjs.common.compat.BuildPlatform;
import org.hapjs.component.Component;
import org.hapjs.component.view.ComponentHost;
import org.hapjs.component.view.gesture.GestureHost;
import org.hapjs.component.view.gesture.IGesture;
import org.hapjs.component.view.helper.StateHelper;
import org.hapjs.component.view.keyevent.KeyEventDelegate;
import org.hapjs.widgets.input.Edit;

public class FlexEditText extends AppCompatAutoCompleteTextView
        implements ComponentHost, GestureHost {
    private Component mComponent;
    private KeyEventDelegate mKeyEventDelegate;

    private SelectionChangeListener mSelectionChangeListener;

    private IGesture mGesture;
    private boolean mAutoCompleted = true;
    private boolean mLastWindowFocus = false;

    public FlexEditText(Context context) {
        super(context);
        setThreshold(0);
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        StateHelper.onStateChanged(this, mComponent);
        int[] states = getDrawableState();
        boolean windowfocused = false;
        for (int i = 0; i < states.length; i++) {
            if (states[i] == android.R.attr.state_window_focused) {
                windowfocused = true;
            }
        }
        InputMethodManager imm =
                (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && isFocused() && windowfocused && mLastWindowFocus) {
            imm.showSoftInput(this, 0);
        }
        mLastWindowFocus = windowfocused;
    }

    @Override
    public Component getComponent() {
        return mComponent;
    }

    @Override
    public void setComponent(Component component) {
        mComponent = component;
    }

    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        super.onSelectionChanged(selStart, selEnd);
        if (mSelectionChangeListener != null) {
            mSelectionChangeListener.onSelectionChange(selStart, selEnd);
        }
    }

    public void setOnSelectionChangeListener(SelectionChangeListener selectionChangeListener) {
        mSelectionChangeListener = selectionChangeListener;
    }

    @Override
    public IGesture getGesture() {
        return mGesture;
    }

    public void setGesture(IGesture gestureDelegate) {
        mGesture = gestureDelegate;
    }

    public boolean isAutoCompleted() {
        return mAutoCompleted;
    }

    public void setAutoCompleted(boolean autoCompleted) {
        this.mAutoCompleted = autoCompleted;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean result = super.onTouchEvent(event);
        if (mGesture != null) {
            result |= mGesture.onTouch(event);
        }
        // filter all when user click
        if (event.getAction() == MotionEvent.ACTION_DOWN && isAutoCompleted()) {
            performFiltering("", 0);
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

    @Override
    protected void performFiltering(CharSequence text, int keyCode) {
        if (getFilter() == null) {
            return;
        }
        super.performFiltering(text, keyCode);
    }

    @Override
    public boolean enoughToFilter() {
        if (!isAutoCompleted()) {
            return false;
        }
        // allow to filter when empty content
        if (getText().length() == 0) {
            return true;
        }
        return super.enoughToFilter();
    }

    @Override
    public boolean onTextContextMenuItem(int id) {
        if (id == android.R.id.paste) {
            super.onTextContextMenuItem(android.R.id.paste);
            Editable editable = getText();
            if (mComponent != null && mComponent instanceof Edit && editable != null) {
                ((Edit) mComponent).forceUpdateSpannable(editable.toString());
            }
            return true;
        }
        return super.onTextContextMenuItem(id);
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        InputConnection inputConnection = super.onCreateInputConnection(outAttrs);
        if (inputConnection != null) {
            return new CustomInputConnection(inputConnection, false);
        }
        return null;
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        // BugFix: https://issuetracker.google.com/issues/37055966
        if (isFocused()
                && keyCode == KeyEvent.KEYCODE_BACK
                && event.getAction() == KeyEvent.ACTION_UP) {
            if (!BuildPlatform.isTV()) {
                clearFocus();
            }
        }
        return super.onKeyPreIme(keyCode, event);
    }

    public interface SelectionChangeListener {
        void onSelectionChange(int selStart, int selEnd);
    }

    class CustomInputConnection extends InputConnectionWrapper {

        public CustomInputConnection(InputConnection target, boolean mutable) {
            super(target, mutable);
        }

        @Override
        public boolean commitText(CharSequence text, int newCursorPosition) {
            if (TextUtils.isEmpty(text)) {
                return false;
            }
            Spannable spannable = null;
            if (mComponent != null && mComponent instanceof Edit) {
                spannable = ((Edit) mComponent).generateSpannable(text.toString());
            }
            if (spannable == null) {
                return super.commitText(text, newCursorPosition);
            } else {
                return super.commitText(spannable, newCursorPosition);
            }
        }
    }
}

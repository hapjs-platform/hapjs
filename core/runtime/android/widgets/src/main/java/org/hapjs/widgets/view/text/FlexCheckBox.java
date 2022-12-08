/*
 * Copyright (c) 2021-present,  the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.view.text;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.AppCompatCheckBox;
import org.hapjs.component.Component;
import org.hapjs.component.view.ComponentHost;
import org.hapjs.component.view.gesture.GestureHost;
import org.hapjs.component.view.gesture.IGesture;
import org.hapjs.component.view.helper.StateHelper;
import org.hapjs.component.view.keyevent.KeyEventDelegate;
import org.hapjs.widgets.R;

public class FlexCheckBox extends AppCompatCheckBox implements ComponentHost, GestureHost {
    private Component mComponent;
    private KeyEventDelegate mKeyEventDelegate;

    private IGesture mGesture;
    private boolean mIsEnableTalkBack;
    private String mValue;

    public FlexCheckBox(Context context) {
        super(context);
    }

    public FlexCheckBox(Context context, boolean isEnableTalkBack) {
        super(context);
        mIsEnableTalkBack = isEnableTalkBack;
    }

    public void setValue(String value) {
        this.mValue = value;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        initTalkBack(info);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void initTalkBack(AccessibilityNodeInfo info) {
        if (mIsEnableTalkBack && null != info) {
            info.removeAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK);
            info.setClassName("");
            info.setCheckable(false);
            info.setClickable(false);
            String realTextStr = "";
            if (!TextUtils.isEmpty(mValue)) {
                realTextStr = mValue;
            }
            if (TextUtils.isEmpty(realTextStr)) {
                info.setText((isChecked() ? getResources().getString(R.string.talkback_selected) : getResources().getString(R.string.talkback_unselected))
                        + " "
                        + getResources().getString(R.string.talkback_checkbox)
                        + " "
                        + getResources().getString(R.string.talkback_no_use));
            } else {
                info.setText((isChecked() ? getResources().getString(R.string.talkback_selected) : getResources().getString(R.string.talkback_unselected))
                        + " "
                        + realTextStr
                        + " "
                        + getResources().getString(R.string.talkback_press_active_min)
                        + (isChecked() ? getResources().getString(R.string.talkback_cancel_select) : getResources().getString(R.string.talkback_select)));
            }
        }
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
        mComponent = component;
    }

    @Override
    public IGesture getGesture() {
        return mGesture;
    }

    @Override
    public void setGesture(IGesture gestureDelegate) {
        mGesture = gestureDelegate;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean result = super.onTouchEvent(event);
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
}

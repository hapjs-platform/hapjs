/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.view.keyevent;

import android.view.KeyEvent;
import android.view.View;
import org.hapjs.component.Component;
import org.hapjs.component.view.gesture.GestureDelegate;
import org.hapjs.component.view.gesture.GestureHost;

public class KeyEventDelegate {
    public static final String KEY_CODE = "code";
    public static final String KEY_ACTION = "action";
    public static final String KEY_REPEAT = "repeatCount";
    public static final String KEY_HASHCODE = "hashcode";

    private Component mComponent;

    public KeyEventDelegate(Component component) {
        mComponent = component;
    }

    public boolean onKey(int keyAction, int keyCode, KeyEvent event) {
        // only pass keycode bellow
        boolean pass = false;
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_BUTTON_SELECT:
            case KeyEvent.KEYCODE_BUTTON_A:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
                pass = true;
                break;
            default:
                break;
        }
        if (!pass) {
            return false;
        }

        /**
         * for KeyEventManager fired keyEvents,return false to use Android default function or perform
         * click
         */
        if (KeyEventManager.getInstance().contains(event.hashCode())) {
            KeyEventManager.getInstance().remove(event.hashCode());
            performClick(event.getKeyCode(), event);
            return false;
        }

        KeyEventManager.getInstance()
                .put(event.hashCode(), event, mComponent.getPageId(), mComponent.getHostView());
        return mComponent.onHostKey(keyAction, keyCode, event);
    }

    private void performClick(int keyCode, KeyEvent event) {
        if (!KeyEventManager.isConfirmKey(keyCode) || KeyEvent.ACTION_UP != event.getAction()) {
            return;
        }
        View hostView = mComponent.getHostView();
        if (hostView instanceof GestureHost) {
            GestureHost gestureHost = (GestureHost) hostView;
            if (gestureHost.getGesture() != null
                    && gestureHost.getGesture() instanceof GestureDelegate) {
                GestureDelegate gestureDelegate = (GestureDelegate) gestureHost.getGesture();
                gestureDelegate.onKeyEventClick(keyCode, event);
            }
        }
    }
}

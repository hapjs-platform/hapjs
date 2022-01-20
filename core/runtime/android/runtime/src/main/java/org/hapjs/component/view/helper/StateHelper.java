/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.view.helper;

import android.view.MotionEvent;
import android.view.View;
import java.util.HashMap;
import java.util.Map;
import org.hapjs.component.Component;
import org.hapjs.component.view.state.State;

public class StateHelper {
    public static void onStateChanged(View view, Component component) {
        if (component == null) {
            return;
        }

        int[] states = view.getDrawableState();
        boolean focus = false;
        boolean checked = false;
        boolean active = false;
        for (int i = 0; i < states.length; i++) {
            if (states[i] == android.R.attr.state_checked) {
                checked = true;
            }
            if (states[i] == android.R.attr.state_focused) {
                focus = true;
            }
            if (states[i] == android.R.attr.state_pressed
                    || states[i] == android.R.attr.state_selected
                    || states[i] == android.R.attr.state_activated) {
                active = true;
            }
        }

        Map<String, Boolean> stateMap = new HashMap<>();
        if (component.getStateValue(State.CHECKED) != checked) {
            stateMap.put(State.CHECKED, checked);
        }
        if (component.getStateValue(State.FOCUS) != focus) {
            stateMap.put(State.FOCUS, focus);
        }
        if (component.getStateValue(State.ACTIVE) != active) {
            stateMap.put(State.ACTIVE, active);
        }
        component.onStateChanged(stateMap);
    }

    public static void onActiveStateChanged(Component component, MotionEvent event) {
        boolean active;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_MOVE:
                active = true;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL:
            default:
                active = false;
                break;
        }

        if (component.getStateValue(State.ACTIVE) != active) {
            Map<String, Boolean> stateMap = new HashMap<>();
            stateMap.put(State.ACTIVE, active);
            component.onStateChanged(stateMap);
        }
    }

    public static void onActiveStateChanged(Component component, boolean activeStateValue) {
        if (component.getStateValue(State.ACTIVE) != activeStateValue) {
            Map<String, Boolean> stateMap = new HashMap<>();
            stateMap.put(State.ACTIVE, activeStateValue);
            component.onStateChanged(stateMap);
        }
    }
}

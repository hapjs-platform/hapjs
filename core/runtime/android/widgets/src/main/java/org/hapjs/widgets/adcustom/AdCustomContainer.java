/*
 * Copyright (c) 2023, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.adcustom;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.hapjs.component.Component;
import org.hapjs.component.view.ComponentHost;
import org.hapjs.component.view.gesture.GestureHost;
import org.hapjs.component.view.gesture.IGesture;


public class AdCustomContainer extends FrameLayout implements ComponentHost, GestureHost {
    private Component mComponent;
    private IGesture mGesture;

    public AdCustomContainer(Context context) {
        super(context);
    }

    @Override
    public void setComponent(Component component) {
        mComponent = component;
    }

    @Override
    public Component getComponent() {
        return mComponent;
    }

    @Override
    public void setGesture(IGesture gestureDelegate) {
        mGesture = gestureDelegate;
    }

    @Override
    public IGesture getGesture() {
        return mGesture;
    }

}

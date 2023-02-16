/*
 * Copyright (c) 2023, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hapjs.widgets.ad;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.hapjs.component.Component;
import org.hapjs.component.view.ComponentHost;
import org.hapjs.component.view.gesture.GestureHost;
import org.hapjs.component.view.gesture.IGesture;

public class AdContainer extends FrameLayout implements ComponentHost, GestureHost {
    private static final String TAG = "AdContainer";

    private Component mComponent;
    private IGesture mGesture;
    private View mAdView;//广告sdk渲染的广告UI，可能为View或者ViewGroup

    public AdContainer(Context context) {
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

    @Override
    public void removeView(View view) {
        if (mAdView != null && mAdView instanceof ViewGroup) {
            ((ViewGroup) mAdView).removeView(view);
        } else {
            super.removeView(view);
        }
    }

    @Override
    public void addView(View child, int index) {
        if (mAdView != null && mAdView instanceof ViewGroup) {
            ((ViewGroup) mAdView).addView(child, index);
        } else {
            super.addView(child, index);
        }
    }

    public void setAdView(View adView) {
        this.mAdView = adView;
    }
}

/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component;

import android.content.Context;
import android.view.ViewGroup;
import java.util.Map;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.component.view.UnsupportedView;
import org.hapjs.runtime.HapEngine;

public class Unsupported extends Container<UnsupportedView> {
    protected static final String WIDGET_NAME = "unsupported";
    private String mWidgetRealName = "";

    public Unsupported(
            HapEngine hapEngine,
            Context context,
            Container parent,
            int ref,
            RenderEventCallback callback,
            Map<String, Object> savedState) {
        super(hapEngine, context, parent, ref, callback, savedState);
    }

    @Override
    protected UnsupportedView createViewImpl() {
        UnsupportedView unsupportedView = new UnsupportedView(mContext);
        ViewGroup.LayoutParams params =
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        unsupportedView.setLayoutParams(params);
        unsupportedView.setComponent(this);
        unsupportedView.setWidgetName(mWidgetRealName);
        return unsupportedView;
    }

    public void setWidgetRealName(String name) {
        mWidgetRealName = name;
        UnsupportedView unsupportedView = mHost;
        if (unsupportedView != null) {
            unsupportedView.setWidgetName(name);
        }
    }
}

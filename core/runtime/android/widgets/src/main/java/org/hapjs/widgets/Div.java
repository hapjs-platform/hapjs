/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets;

import android.content.Context;
import java.util.Map;
import org.hapjs.bridge.annotation.WidgetAnnotation;
import org.hapjs.component.Component;
import org.hapjs.component.Container;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.component.constants.Attributes;
import org.hapjs.component.view.flexbox.PercentFlexboxLayout;
import org.hapjs.runtime.HapEngine;

@WidgetAnnotation(
        name = Div.WIDGET_NAME,
        methods = {
                Component.METHOD_ANIMATE,
                Component.METHOD_GET_BOUNDING_CLIENT_RECT,
                Component.METHOD_TO_TEMP_FILE_PATH,
                Component.METHOD_FOCUS,
                Component.METHOD_TALKBACK_FOCUS,
                Component.METHOD_TALKBACK_ANNOUNCE
        })
public class Div extends Container<PercentFlexboxLayout> {

    protected static final String WIDGET_NAME = "div";

    private boolean mEnableVideoFullscreenContainer = false;

    public Div(
            HapEngine hapEngine,
            Context context,
            Container parent,
            int ref,
            RenderEventCallback callback,
            Map<String, Object> savedState) {

        super(hapEngine, context, parent, ref, callback, savedState);
    }

    @Override
    protected PercentFlexboxLayout createViewImpl() {
        PercentFlexboxLayout percentFlexboxLayout = new PercentFlexboxLayout(mContext);
        percentFlexboxLayout.setComponent(this);
        mNode = percentFlexboxLayout.getYogaNode();
        return percentFlexboxLayout;
    }

    @Override
    protected boolean setAttribute(String key, Object attribute) {
        switch (key) {
            case Attributes.Style.VIDEO_FULLSCREEN_CONTAINER:
                mEnableVideoFullscreenContainer = Attributes.getBoolean(attribute, false);
                return true;
            default:
                break;
        }
        return super.setAttribute(key, attribute);
    }

    public boolean enableVideoFullscreenContainer() {
        return mEnableVideoFullscreenContainer;
    }
}

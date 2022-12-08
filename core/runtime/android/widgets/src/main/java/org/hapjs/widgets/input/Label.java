/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.input;

import android.content.Context;
import android.view.View;
import java.util.Map;
import org.hapjs.bridge.annotation.WidgetAnnotation;
import org.hapjs.component.Component;
import org.hapjs.component.Container;
import org.hapjs.component.SingleChoice;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.component.constants.Attributes;
import org.hapjs.runtime.HapEngine;
import org.hapjs.widgets.text.Text;
import org.hapjs.widgets.view.text.TextLayoutView;

@WidgetAnnotation(
        name = Label.WIDGET_NAME,
        methods = {
                Component.METHOD_ANIMATE,
                Component.METHOD_GET_BOUNDING_CLIENT_RECT,
                Component.METHOD_FOCUS,
                Component.METHOD_TO_TEMP_FILE_PATH,
                Component.METHOD_TALKBACK_FOCUS,
                Component.METHOD_TALKBACK_ANNOUNCE
        })
public class Label extends Text {

    protected static final String WIDGET_NAME = "label";

    private String mTargetId;

    public Label(
            HapEngine hapEngine,
            Context context,
            Container parent,
            int ref,
            RenderEventCallback callback,
            Map<String, Object> savedState) {
        super(hapEngine, context, parent, ref, callback, savedState);
    }

    @Override
    protected TextLayoutView createViewImpl() {
        TextLayoutView textLayoutView = super.createViewImpl();
        textLayoutView.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Component component = getRootComponent().findComponentById(mTargetId);
                        if (component == null) {
                            return;
                        }

                        component.callOnClick();
                        if (component instanceof SingleChoice) {
                            ((SingleChoice) component).setChecked(true);
                        } else if (component instanceof CheckBox) {
                            ((CheckBox) component).toggle();
                        } else {
                            component.focus(true);
                        }
                    }
                });

        return textLayoutView;
    }

    @Override
    protected boolean setAttribute(String key, Object attribute) {
        switch (key) {
            case Attributes.Style.TARGET:
                mTargetId = Attributes.getString(attribute);
                return true;
            default:
                break;
        }
        return super.setAttribute(key, attribute);
    }

    @Override
    protected boolean addEvent(String event) {
        // label提供touch事件
        if (Attributes.Event.TOUCH_START.equals(event)
                || Attributes.Event.TOUCH_MOVE.equals(event)
                || Attributes.Event.TOUCH_END.equals(event)
                || Attributes.Event.TOUCH_CANCEL.equals(event)
                || Attributes.Event.TOUCH_CLICK.equals(event)
                || Attributes.Event.TOUCH_LONG_PRESS.equals(event)) {
            return super.addEvent(event);
        }
        return true;
    }
}

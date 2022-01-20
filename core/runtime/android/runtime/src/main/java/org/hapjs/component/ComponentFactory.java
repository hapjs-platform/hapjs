/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component;

import android.content.Context;
import java.util.Map;
import org.hapjs.bridge.Widget;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.runtime.HapEngine;

public class ComponentFactory {
    private static final String KEY_TYPE = "type";

    public static Component createComponent(
            HapEngine hapEngine,
            Context context,
            String element,
            Container parent,
            int ref,
            RenderEventCallback callback,
            Map<String, Object> componentInfo,
            Map<String, Object> savedState) {
        switch (element) {
            case "body": {
                return new Scroller(hapEngine, context, parent, ref, callback, savedState);
            }
            default:
                break;
        }
        Widget widget = getWidget(element, componentInfo);
        if (widget != null) {
            return widget.createComponent(
                    hapEngine, context, parent, ref, callback, componentInfo, savedState);
        } else {
            if (callback != null) {
                callback.onJsException(
                        new IllegalArgumentException("Unsupported element:" + element));
            }
            Unsupported unsupported =
                    new Unsupported(hapEngine, context, parent, ref, callback, savedState);
            unsupported.setWidgetRealName(element);
            return unsupported;
        }
    }

    public static Widget getWidget(String tagName, Map<String, Object> componentInfo) {
        String type = null;
        Object typeObject = componentInfo.get(KEY_TYPE);
        if (typeObject != null) {
            type = typeObject.toString();
        }

        Widget widget = ComponentManager.getWidgetMap().get(Widget.getComponentKey(tagName, type));
        if (widget == null && type != null) {
            widget = ComponentManager.getWidgetMap().get(Widget.getComponentKey(tagName, null));
        }
        return widget;
    }

    public static Class<? extends Component> getComponetClass(
            String tagName, Map<String, Object> componentInfo) {
        Widget widget = getWidget(tagName, componentInfo);
        return widget == null ? null : widget.getClazz();
    }
}

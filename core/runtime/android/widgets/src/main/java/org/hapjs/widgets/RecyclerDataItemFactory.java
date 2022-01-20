/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets;

import android.content.Context;
import java.util.Map;
import org.hapjs.bridge.Widget;
import org.hapjs.component.Component;
import org.hapjs.component.ComponentFactory;
import org.hapjs.component.Container;
import org.hapjs.component.RecyclerDataItem;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.runtime.HapEngine;
import org.hapjs.widgets.list.List;
import org.hapjs.widgets.list.ListItem;
import org.hapjs.widgets.sectionlist.SectionGroup;
import org.hapjs.widgets.sectionlist.SectionHeader;
import org.hapjs.widgets.sectionlist.SectionItem;
import org.hapjs.widgets.sectionlist.SectionList;
import org.hapjs.widgets.text.Text;

public class RecyclerDataItemFactory implements RecyclerDataItem.Creator {
    private static RecyclerDataItemFactory mInstance;

    private RecyclerDataItemFactory() {
    }

    public static RecyclerDataItemFactory getInstance() {
        if (mInstance == null) {
            mInstance = new RecyclerDataItemFactory();
        }
        return mInstance;
    }

    public RecyclerDataItem createRecyclerItem(
            HapEngine hapEngine,
            Context context,
            String element,
            int ref,
            RenderEventCallback callback,
            Map<String, Object> componentInfo) {
        Widget widget = ComponentFactory.getWidget(element, componentInfo);

        if (widget == null) {
            throw new IllegalArgumentException("Unsupported element:" + element);
        }

        RecyclerDataItem.ComponentCreator componentCreator =
                new RecyclerDataItem.ComponentCreator(hapEngine, context, callback, widget);

        switch (widget.getName()) {
            case "swiper":
                return new Swiper.RecyclerItem(ref, componentCreator);
            case "list":
                return new List.RecyclerItem(ref, componentCreator);
            case "list-item":
                return new ListItem.RecyclerItem(ref, componentCreator);
            case "text":
                return new Text.RecyclerItem(ref, componentCreator);
            case "span":
                return new Span.RecyclerItem(ref, componentCreator);
            case SectionList.WIDGET_NAME:
                return new SectionList.RecyclerItem(ref, componentCreator);
            case SectionGroup.WIDGET_NAME:
                return new SectionGroup.RecyclerItem(ref, componentCreator);
            case SectionHeader.WIDGET_NAME:
                return new SectionHeader.RecyclerItem(ref, componentCreator);
            case SectionItem.WIDGET_NAME:
                return new SectionItem.RecyclerItem(ref, componentCreator);
            default:
                break;
        }

        if (Container.class.isAssignableFrom(widget.getClazz())) {
            return new Container.RecyclerItem(ref, componentCreator);
        }

        return new Component.RecyclerItem(ref, componentCreator);
    }
}

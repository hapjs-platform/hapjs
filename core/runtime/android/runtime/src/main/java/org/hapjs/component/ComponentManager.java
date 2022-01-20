/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.hapjs.bridge.MetaDataSet;
import org.hapjs.bridge.Widget;
import org.hapjs.runtime.CardConfig;

public class ComponentManager {

    private static final String TAG = "ComponentManager";
    private static final List<Widget> WIDGET_LIST = new ArrayList<>();
    private static final Map<String, Widget> WIDGET_MAP = new HashMap<>();
    private static boolean sCardModeEnabled = false;

    public static Map<String, Widget> getWidgetMap() {
        if (WIDGET_MAP.isEmpty()) {
            generateMap();
        }
        return WIDGET_MAP;
    }

    public static String getWidgetListJSONString() {
        return MetaDataSet.getInstance().getWidgetListJSONString(sCardModeEnabled);
    }

    public static void configCardBlacklist() {
        sCardModeEnabled = true;
        Map<String, CardConfig.ComponentBlacklistItem> blacklist =
                CardConfig.getInstance().getComponentBlacklistMap();
        getWidgetList();
        Iterator<Widget> iterator = WIDGET_LIST.iterator();
        while (iterator.hasNext()) {
            Widget widget = iterator.next();
            String name = widget.getName();
            CardConfig.ComponentBlacklistItem blacklistItem = blacklist.get(name);
            if (blacklistItem == null) {
                continue;
            }

            boolean removed = false;

            List<String> blacklistMethods = blacklistItem.methods;
            if (blacklistMethods != null && !blacklistMethods.isEmpty()) {
                widget.removeMethods(blacklistMethods);
                removed = true;
            }

            List<String> blacklistTypes = blacklistItem.types;
            if (blacklistTypes != null && !blacklistTypes.isEmpty()) {
                widget.removeTypes(blacklistTypes);
                removed = true;
            }

            // Nothing removed means remove widget completely
            if (!removed) {
                iterator.remove();
            }
            generateMap();
        }
    }

    private static void generateMap() {
        WIDGET_MAP.clear();
        if (WIDGET_LIST.isEmpty()) {
            getWidgetList();
        }
        for (Widget widget : WIDGET_LIST) {
            List<String> keys = widget.getComponentKeys();
            for (String key : keys) {
                WIDGET_MAP.put(key, widget);
            }
        }
    }

    public static List<Widget> getWidgetList() {
        List<Widget> widgetList = MetaDataSet.getInstance().getWidgetList();
        if (widgetList != null) {
            WIDGET_LIST.clear();
            WIDGET_LIST.addAll(widgetList);
        }
        return WIDGET_LIST;
    }
}

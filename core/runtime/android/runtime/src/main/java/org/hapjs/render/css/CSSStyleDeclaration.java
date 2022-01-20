/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.css;

import java.util.ArrayList;
import java.util.ListIterator;
import java.util.Map;
import org.hapjs.render.css.property.CSSPropertyBuilder;
import org.hapjs.render.css.value.CSSValueFactory;
import org.hapjs.render.css.value.CSSValues;

/**
 * #myId : { --------------------------- | "width" : 100px, | | "width:active" : 150px, | | "height"
 * : 200px | -> CSSStyleDeclaration --------------------------- }
 */
public class CSSStyleDeclaration extends OrderedConcurrentHashMap<String, CSSProperty> {
    // key 为样式名称加状态, 如 "width:active"

    private static String middleLineToHump(String para) {
        StringBuilder result = new StringBuilder();
        String[] a = para.split("-");
        for (String s : a) {
            if (!para.contains("-")) {
                result.append(s);
                continue;
            }
            if (result.length() == 0) {
                result.append(s.toLowerCase());
            } else {
                result.append(s.substring(0, 1).toUpperCase());
                result.append(s.substring(1).toLowerCase());
            }
        }
        return result.toString();
    }

    // For inspector
    public int getLength() {
        return mEntryKeyQueue.size();
    }

    public ListIterator<String> getReversedListIterator() {
        return new ArrayList<>(mEntryKeyQueue).listIterator(mEntryKeyQueue.size());
    }

    public ListIterator<String> getSortedListIterator() {
        return new ArrayList<>(mEntryKeyQueue).listIterator();
    }

    // For inspector
    public CSSProperty getProperty(String propertyName) {
        return get(propertyName);
    }

    // For inspector
    public Object getPropertyValue(String propertyName) {
        CSSProperty object = get(propertyName);
        return object == null ? null : object.getValue();
    }

    void setProperty(CSSProperty property) {
        if (property == null) {
            return;
        }
        add(property.getNameWithState(), property);
    }

    // For inspector
    public void setInspectorProperty(String nameWithState, String valueText, boolean disabled) {
        nameWithState = middleLineToHump(nameWithState);

        CSSProperty cssProperty =
                new CSSPropertyBuilder()
                        .setNameWithState(nameWithState)
                        .setDisable(disabled)
                        .setValue(valueText)
                        .build();
        setProperty(cssProperty);
    }

    public void setAllProperty(CSSStyleDeclaration cssStyleDeclaration) {
        if (cssStyleDeclaration == null) {
            return;
        }
        for (String key : cssStyleDeclaration.mEntryKeyQueue) {
            add(key, cssStyleDeclaration.mEntriesMap.get(key));
        }
    }

    public Map<String, CSSValues> convertStyleProps() {
        return CSSValueFactory.createCSSValuesMap(this);
    }
}

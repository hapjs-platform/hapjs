/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.css.value;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import org.hapjs.render.css.CSSProperty;
import org.hapjs.render.css.CSSStyleDeclaration;

public class CSSValueFactory {

    public static CSSValues createCSSValues(String state, Object value) {
        CSSValueMap values = new CSSValueMap();
        values.put(state, value);
        return values;
    }

    public static Map<String, CSSValues> createCSSValuesMap(
            CSSStyleDeclaration cssStyleDeclaration) {
        Map<String, CSSValues> result = new LinkedHashMap<>();
        Iterator<String> cssPropertyIterator = cssStyleDeclaration.iterator();
        while (cssPropertyIterator.hasNext()) {
            String entryKey = cssPropertyIterator.next();
            CSSProperty item = cssStyleDeclaration.getProperty(entryKey);
            String nameWithOutState = item.getNameWithoutState();
            String state = item.getState();
            Object value = item.getValue();

            CSSValues values = result.get(nameWithOutState);
            if (values == null) {
                values = createCSSValues(state, value);
            }
            ((CSSValueMap) values).put(state, value);

            result.put(nameWithOutState, values);
        }
        return result;
    }
}

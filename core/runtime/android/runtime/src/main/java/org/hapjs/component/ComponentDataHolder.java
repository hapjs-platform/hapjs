/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component;

import java.util.Map;
import java.util.Set;
import org.hapjs.render.css.value.CSSValues;

public interface ComponentDataHolder {
    int getRef();

    Map<String, Object> getAttrsDomData();

    Map<String, CSSValues> getStyleDomData();

    Set<String> getDomEvents();

    void bindAttrs(Map<String, Object> attrs);

    void bindStyles(Map<String, ? extends CSSValues> attrs);

    void bindEvents(Set<String> events);

    void removeEvents(Set<String> events);

    void invokeMethod(String methodName, Map<String, Object> args);
}

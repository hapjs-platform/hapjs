/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.inspector.elements;

import com.facebook.stetho.common.Accumulator;
import com.facebook.stetho.common.ThreadBound;
import javax.annotation.Nullable;
import org.hapjs.inspector.DOMAccumulator;
import org.hapjs.render.css.CSSStyleDeclaration;

public interface NodeDescriptor<E> extends ThreadBound {
    // INSPECTOR ADD
    void getBoxModel(E element, DOMAccumulator accumulator);

    void hook(E element);

    void unhook(E element);

    NodeType getNodeType(E element);

    String getNodeName(E element);

    String getLocalName(E element);

    @Nullable
    String getNodeValue(E element);

    void getChildren(E element, Accumulator<Object> children);

    void getAttributes(E element, AttributeAccumulator attributes);

    void setAttributesAsText(E element, String text);

    // INSPECTOR ADD
    void setAttributesAsText(E element, String name, String text);

    void getStyleRuleNames(E element, StyleRuleNameAccumulator accumulator);

    void getStyles(E element, String ruleName, StyleAccumulator accumulator);

    void getComputedStyles(E element, ComputedStyleAccumulator accumulator);

    void setStyle(E element, String ruleName, String name, String value);

    // INSPECTOR ADD BEGIN
    void setStyle(E element, String ruleName, CSSStyleDeclaration style);

    void getInlineStyle(E element, StyleAccumulator accumulator);

    void setOuterHTML(E element, String outerHTML);
    // INSPECTOR END

}

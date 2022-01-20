/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.css;

import java.util.List;

public class CSSRuleList {
    private CSSRule[] cssRules;

    CSSRuleList(List<CSSRule> cssRuleList) {
        cssRules = cssRuleList.toArray(new CSSRule[0]);
    }

    public CSSRule[] getCssRules() {
        return cssRules;
    }

    CSSRule item(int index) {
        return cssRules[index];
    }

    int length() {
        return cssRules.length;
    }
}

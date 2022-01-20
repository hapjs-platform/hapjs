/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.css;

public class CSSInlineStyleRule extends CSSStyleRule {

    public CSSInlineStyleRule() {
        super();
        setDeclaration(new CSSStyleDeclaration());
    }

    // For inspector
    @Override
    public String getSelectorText() {
        return "INLINE";
    }

    // For inspector
    @Override
    public long getOrder() {
        return 0;
    }

    // For inspector
    @Override
    public boolean getEditable() {
        return true;
    }
}

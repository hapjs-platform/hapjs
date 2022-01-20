/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.css;

/**
 * -------------------------------------------------------------- | --------- | | | #myId | : ->
 * Selector | | --------- | | { | | --------------------------- | | | "width" : 100px, | | | |
 * "width:active" : 150px, | | | | "height" : 200px | -> CSSStyleDeclaration | |
 * --------------------------- | | } | -> CSSStyleRule
 * --------------------------------------------------------------
 */
public class CSSStyleRule extends CSSRule {
    private static final String TAG = "CSSStyleRule";

    private Selector[] mSelectors;
    private CSSStyleDeclaration mDeclaration;
    private String mSelectorText;
    private long mOrder;

    CSSStyleRule() {
    }

    CSSStyleRule(CSSStyleSheet ss, String selectorText, CSSStyleDeclaration declaration) {
        mSelectorText = selectorText;
        mSelectors = CSSParser.parseSelector(selectorText);
        mDeclaration = declaration;
    }

    // For inspector "name"
    public String getSelectorText() {
        return mSelectorText;
    }

    Selector[] getSelectors() {
        return mSelectors;
    }

    // For inspector "mOrder"
    public long getOrder() {
        return mOrder;
    }

    void setOrder(long order) {
        mOrder = order;
    }

    // For inspector "editable"
    public boolean getEditable() {
        return true;
    }

    // For inspector "style"
    public CSSStyleDeclaration getDeclaration() {
        return mDeclaration;
    }

    public void setDeclaration(CSSStyleDeclaration declaration) {
        mDeclaration = declaration;
    }

    @Override
    public int getType() {
        return CSSRule.STYLE_RULE;
    }
}

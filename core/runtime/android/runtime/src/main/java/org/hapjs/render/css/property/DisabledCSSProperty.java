/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.css.property;

import org.hapjs.render.css.CSSProperty;

/**
 * web 调试器可以选择禁用某一条样式
 */
class DisabledCSSProperty implements CSSProperty {

    private final CSSProperty mCSSProperty;

    DisabledCSSProperty(CSSProperty cssProperty) {
        mCSSProperty = cssProperty;
    }

    @Override
    public String getNameWithState() {
        return mCSSProperty.getNameWithState();
    }

    @Override
    public String getNameWithoutState() {
        return mCSSProperty.getNameWithoutState();
    }

    @Override
    public String getState() {
        return mCSSProperty.getState();
    }

    @Override
    public String getInspectorName() {
        return mCSSProperty.getInspectorName();
    }

    /**
     * 禁用后, runtime 获得的 value 是 UNDEFINED
     */
    @Override
    public Object getValue() {
        return CSSProperty.UNDEFINED;
    }

    /**
     * 禁用后, web 调试器依然可以获得 value
     */
    @Override
    public String getValueText() {
        return mCSSProperty.getValueText();
    }

    @Override
    public boolean getDisabled() {
        return true;
    }

    @Override
    public String toString() {
        return getNameWithState() + ":" + getValueText();
    }
}

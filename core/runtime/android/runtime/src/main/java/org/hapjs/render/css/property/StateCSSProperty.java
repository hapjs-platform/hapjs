/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.css.property;

import org.hapjs.render.css.CSSProperty;

class StateCSSProperty implements CSSProperty {

    private final CSSProperty mCSSProperty;
    private final String mState;
    private final String mNameWithState;

    StateCSSProperty(CSSProperty cssProperty, String state) {
        mCSSProperty = cssProperty;
        mState = state;
        mNameWithState = cssProperty.getNameWithoutState() + ":" + mState;
    }

    @Override
    public String getNameWithState() {
        return mNameWithState;
    }

    @Override
    public String getNameWithoutState() {
        return mCSSProperty.getNameWithoutState();
    }

    @Override
    public Object getValue() {
        return mCSSProperty.getValue();
    }

    @Override
    public String getState() {
        return mState;
    }

    @Override
    public String getInspectorName() {
        return CSSPropertyBuilder.humpToMiddleLine(getNameWithState());
    }

    @Override
    public String getValueText() {
        return mCSSProperty.getValueText();
    }

    @Override
    public boolean getDisabled() {
        return mCSSProperty.getDisabled();
    }

    @Override
    public String toString() {
        return getNameWithState() + ":" + getValueText();
    }
}

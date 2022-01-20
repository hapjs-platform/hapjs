/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.css.property;

import org.hapjs.component.view.state.State;
import org.hapjs.render.css.CSSProperty;

class NormalCSSProperty implements CSSProperty {

    private final Object mValue;
    private final String mName;

    NormalCSSProperty(String name, Object value) {
        mName = name;
        mValue = value;
    }

    public String getNameWithState() {
        return mName;
    }

    public String getNameWithoutState() {
        return mName;
    }

    public Object getValue() {
        return mValue;
    }

    public String getState() {
        return State.NORMAL;
    }

    // For inspector "mName"
    public String getInspectorName() {
        return CSSPropertyBuilder.humpToMiddleLine(mName);
    }

    // For inspector "mValue"
    public String getValueText() {
        return String.valueOf(mValue);
    }

    // For inspector "disabled"
    public boolean getDisabled() {
        return false;
    }

    @Override
    public String toString() {
        return getNameWithState() + ":" + getValueText();
    }
}

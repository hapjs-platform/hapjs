/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.css.property;

import org.hapjs.common.json.JSONArray;
import org.hapjs.common.json.JSONObject;
import org.hapjs.component.view.state.State;
import org.hapjs.render.css.CSSProperty;
import org.json.JSONException;

public class CSSPropertyBuilder {

    private String mNameWithOutState = null;
    private Object mValue = CSSProperty.UNDEFINED;
    private String mState = State.NORMAL;
    private boolean mDisable = false;

    public CSSPropertyBuilder(CSSProperty cssProperty) {
        mNameWithOutState = cssProperty.getNameWithoutState();
        mValue = cssProperty.getValue();
        mState = cssProperty.getState();
        mDisable = cssProperty.getDisabled();
    }

    public CSSPropertyBuilder() {
    }

    /**
     * 驼峰命名转为中划线命名
     */
    static String humpToMiddleLine(String para) {
        StringBuilder sb = new StringBuilder(para);
        int temp = 0; // 定位
        if (!para.contains("-")) {
            for (int i = 0; i < para.length(); i++) {
                if (Character.isUpperCase(para.charAt(i))) {
                    sb.insert(i + temp, "-");
                    temp += 1;
                }
            }
        }
        return sb.toString().toLowerCase();
    }

    public CSSPropertyBuilder setValue(Object value) {
        mValue = value;
        return this;
    }

    public CSSPropertyBuilder setDisable(boolean disable) {
        mDisable = disable;
        return this;
    }

    public CSSPropertyBuilder setNameWithoutState(String nameWithOutState) {
        mNameWithOutState = nameWithOutState;
        return this;
    }

    public CSSPropertyBuilder setNameWithState(String nameWithState) {
        int index = nameWithState.indexOf(":");
        if (index >= 0) {
            mNameWithOutState = nameWithState.substring(0, index);
            mState = nameWithState.substring(index + 1);
        } else {
            mNameWithOutState = nameWithState;
        }
        return this;
    }

    public CSSPropertyBuilder setState(String state) {
        mState = state;
        return this;
    }

    public CSSProperty build() {
        if (mNameWithOutState == null) {
            throw new IllegalArgumentException("mNameWithOutState cannot be null");
        }

        // org.hapjs.common.json.JSONArray to org.json.JSONArray, component is using org.json.JSONArray
        try {
            if (mValue instanceof JSONArray) {
                mValue = new org.json.JSONArray(mValue.toString());
            } else if (mValue instanceof JSONObject) {
                mValue = new org.json.JSONObject(mValue.toString());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        CSSProperty result = new NormalCSSProperty(mNameWithOutState, mValue); // TODO parse mValue
        if (!State.NORMAL.equals(mState)) {
            result = new StateCSSProperty(result, mState);
        }
        if (mDisable) {
            result = new DisabledCSSProperty(result);
        }
        return result;
    }
}

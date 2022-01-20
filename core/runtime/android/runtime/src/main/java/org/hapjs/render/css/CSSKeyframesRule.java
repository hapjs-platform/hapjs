/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.css;

import org.json.JSONObject;

/**
 * ------------- CSSKeyframesRule ------------- "@KEYFRAMES": { "Go": [ { "backgroundColor":
 * "#f76160", "time": 0 }, { "backgroundColor": "#09ba07", "time": 100 } ] }
 * ---------------------------------------------
 *
 * <p>"#myId" : { "animationName": "Go" }
 */
class CSSKeyframesRule extends CSSRule {
    private final JSONObject mDeclaration;

    CSSKeyframesRule(JSONObject declaration) {
        mDeclaration = declaration;
    }

    @Override
    public int getType() {
        return CSSRule.KEYFRAME_RULE;
    }

    /**
     * @param animationName 动画名称
     * @return animationKeyframes 动画的关键帧信息
     */
    Object getKeyframes(String animationName) {
        Object value = mDeclaration.opt(animationName);
        if (value != null) {
            return value;
        }
        return CSSProperty.UNDEFINED;
    }
}

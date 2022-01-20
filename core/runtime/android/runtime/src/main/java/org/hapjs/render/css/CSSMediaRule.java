/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.css;

import org.hapjs.render.css.media.MediaList;

/**
 * "@MEDIA": [{ "condition": "screen and (max-width: 300)", ".content1": { "backgroundColor":
 * "#ADD8E6" * }, ".content2": { "color": "#FFFFFF", "width": "500px", "height": "500px",
 * "fontSize": "20px", "backgroundColor": "#0000FF" * } }] 包含一条condition和其对应的n条.class
 */
public class CSSMediaRule extends CSSRule {

    private CSSRuleList mCssRuleList;
    private MediaList mMediaList;

    CSSMediaRule(CSSRuleList cssRuleList, MediaList mediaList) {
        mCssRuleList = cssRuleList;
        mMediaList = mediaList;
    }

    public CSSRuleList getCssRuleList() {
        return mCssRuleList;
    }

    public MediaList getMediaList() {
        return mMediaList;
    }

    @Override
    public int getType() {
        return CSSRule.MEDIA_RULE;
    }
}

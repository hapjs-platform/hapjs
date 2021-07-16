/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.css;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hapjs.render.css.media.MediaList;
import org.hapjs.render.css.media.MediaPropertyInfo;

public class CSSStyleSheet {
    private static final String TAG = "CSSStyleSheet";
    private CSSRuleList mCSSRules;
    private CSSKeyframesRule mCSSKeyframesRule;
    private CSSFontFaceRule mCSSFontFaceRule;
    private List<CSSMediaRule> mCssMediaRules;
    private Set<Node> mOwners = Collections.synchronizedSet(new HashSet<>());
    private int mStyleObjectId;

    public void setCssMediaRules(List<CSSMediaRule> cssMediaRule) {
        mCssMediaRules = cssMediaRule;
    }

    public synchronized void addOwner(Node owner) {
        mOwners.add(owner);
    }

    public synchronized void removeOwner(Node owner) {
        mOwners.remove(owner);
    }

    public synchronized Set<Node> getOwners() {
        return mOwners;
    }

    public void setCSSRules(CSSRuleList cssRules) {
        this.mCSSRules = cssRules;
    }

    public CSSRuleList getCSSRuleList() {
        return mCSSRules;
    }

    public void setStyleFromInspector(String ruleName, CSSStyleDeclaration declaration) {
        for (int i = 0; i < mCSSRules.length(); i++) {
            if (mCSSRules.item(i).getType() == CSSRule.STYLE_RULE) {
                if (((CSSStyleRule) mCSSRules.item(i)).getSelectorText().equals(ruleName)) {
                    ((CSSStyleRule) mCSSRules.item(i)).setDeclaration(declaration);
                }
            }
        }
    }

    CSSFontFaceRule getCSSFontFaceRule() {
        return mCSSFontFaceRule;
    }

    void setCSSFontFaceRule(CSSFontFaceRule fontFaceRule) {
        mCSSFontFaceRule = fontFaceRule;
    }

    CSSKeyframesRule getCSSKeyframesRule() {
        return mCSSKeyframesRule;
    }

    void setCSSKeyframesRule(CSSKeyframesRule keyframesRule) {
        mCSSKeyframesRule = keyframesRule;
    }

    /**
     * @return changed CSSMediaRule's CSSRuleList
     */
    public List<CSSRuleList> updateMediaPropertyInfo(MediaPropertyInfo info) {
        List<CSSRuleList> cssMediaRules = null;
        for (int i = 0; i < mCSSRules.length(); i++) {
            if (mCSSRules.item(i).getType() == CSSRule.MEDIA_RULE) {
                CSSMediaRule cssMediaRule = (CSSMediaRule) mCSSRules.item(i);

                MediaList mediaList = cssMediaRule.getMediaList();
                if (mediaList.updateMediaPropertyInfo(info)) {

                    if (cssMediaRules == null) {
                        cssMediaRules = new ArrayList<>();
                    }

                    cssMediaRules.add(cssMediaRule.getCssRuleList());
                }
            }
        }
        return cssMediaRules;
    }

    public int getStyleObjectId() {
        return mStyleObjectId;
    }

    public void setStyleObjectId(int styleObjectId) {
        mStyleObjectId = styleObjectId;
    }
}

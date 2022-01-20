/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.inspector;

import android.util.ArrayMap;
import android.util.Log;
import java.util.Map;
import org.hapjs.component.Component;
import org.hapjs.render.VDomChangeAction;
import org.hapjs.render.css.CSSInlineStyleRule;
import org.hapjs.render.css.MatchedCSSRuleList;
import org.hapjs.render.vdom.VDocument;
import org.hapjs.render.vdom.VElement;

public class InspectorVElement {
    public static final int ID_DOC = VElement.ID_DOC;
    public static final int ID_BODY = VElement.ID_BODY;
    public static final String TAG_BODY = VElement.TAG_BODY;

    private static final String TAG = "InspectorVElement";

    protected InspectorVDocument mDoc;
    protected InspectorVGroup mParent;
    protected int mVId;
    protected String mTagName;

    protected CSSInlineStyleRule mInlineCSSRule;
    protected MatchedCSSRuleList mMatchedCSSRuleList;
    protected Map<String, Object> mAttrsMap = new ArrayMap<>();

    public InspectorVElement(InspectorVDocument doc, VDomChangeAction action) {
        mDoc = doc;
        mVId = action.vId;
        mTagName = action.tagName;
        mMatchedCSSRuleList = action.matchedCSSRuleList;
        mInlineCSSRule = (CSSInlineStyleRule) action.inlineCSSRule;
        mAttrsMap.putAll(action.attributes);
    }

    public InspectorVElement(InspectorVDocument doc, int id, String tagName) {
        mDoc = doc;
        mVId = id;
        mTagName = tagName;
    }

    public String getTagName() {
        return mTagName;
    }

    public int getVId() {
        return mVId;
    }

    public InspectorVGroup getParent() {
        return mParent;
    }

    public Component getComponent() {
        VDocument vdoc = mDoc.getVDocument();
        VElement ele = vdoc.getElementById(mVId);
        if (ele == null) {
            Log.e(TAG, "velement is null, vid=" + mVId);
            return null;
        }
        return ele.getComponent();
    }

    public void updateStyleRules(VDomChangeAction action) {
        mInlineCSSRule = (CSSInlineStyleRule) action.inlineCSSRule;
        mMatchedCSSRuleList = action.matchedCSSRuleList;
        for (VDomChangeAction child : action.children) {
            updateStyleRules(child);
        }
    }

    public void updateAttrs(VDomChangeAction action) {
        mAttrsMap.putAll(action.attributes);
    }

    public CSSInlineStyleRule getInlineCSSRule() {
        return mInlineCSSRule;
    }

    public void setInlineCSSRule(CSSInlineStyleRule inlineCSSRule) {
        mInlineCSSRule = inlineCSSRule;
    }

    public MatchedCSSRuleList getMatchedCSSRuleList() {
        return mMatchedCSSRuleList;
    }

    public void setMatchedCSSRuleList(MatchedCSSRuleList matchedCSSRuleList) {
        mMatchedCSSRuleList = matchedCSSRuleList;
    }

    public Map<String, Object> getAttrsMap() {
        return mAttrsMap;
    }
}

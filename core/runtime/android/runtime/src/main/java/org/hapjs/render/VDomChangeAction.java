/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render;

import android.util.ArrayMap;
import androidx.collection.ArraySet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hapjs.render.css.CSSStyleRule;
import org.hapjs.render.css.MatchedCSSRuleList;
import org.hapjs.render.css.Node;
import org.hapjs.render.css.value.CSSValues;
import org.hapjs.runtime.inspect.InspectorVElementType;

public class VDomChangeAction implements RenderAction {

    public static final int ACTION_ADD = 1;
    public static final int ACTION_REMOVE = 2;
    public static final int ACTION_MOVE = 3;
    public static final int ACTION_UPDATE_STYLE = 4;
    public static final int ACTION_UPDATE_ATTRS = 5;
    public static final int ACTION_ADD_EVENT = 6;
    public static final int ACTION_REMOVE_EVENT = 7;
    public static final int ACTION_PRE_CREATE_BODY = 8;
    public static final int ACTION_CREATE_BODY = 9;
    public static final int ACTION_UPDATE_FINISH = 10;
    public static final int ACTION_CREATE_FINISH = 11;
    public static final int ACTION_UPDATE_TITLE_BAR = 12;
    public static final int ACTION_EXIT_FULLSCREEN = 13;
    public static final int ACTION_UPDATE_STATUS_BAR = 14;
    public static final int ACTION_STATISTICS = 15;
    public static final int ACTION_PAGE_SCROLL = 16;
    public static final int ACTION_HIDE_SKELETON = 17;
    public static final int ACTION_SET_SECURE = 18;
    public final Set<String> events = new ArraySet<>();
    public final Map<String, Object> attributes = new ArrayMap<>();
    public final Map<String, CSSValues> styles = new LinkedHashMap<>();
    public final Map<String, Object> titles = new ArrayMap<>();
    public final Map<String, Object> status = new ArrayMap<>();
    public final Map<String, Object> extra = new ArrayMap<>();
    public final Map<String, Object> scrolls = new ArrayMap<>();
    public final List<VDomChangeAction> children = new ArrayList<>();
    final public List<String> hooks = new ArrayList<>();
    public boolean jsCallbacks = false;
    public int pageId;
    public int action;
    public int vId;
    public int parentVId = -1;
    public int index = -1;
    public String tagName;
    public boolean isSecure;
    public CSSStyleRule inlineCSSRule;
    public MatchedCSSRuleList matchedCSSRuleList;
    public InspectorVElementType inspectorVElementType = InspectorVElementType.NONE;
    private Node mNode;

    public VDomChangeAction() {
    }

    public Node getNode() {
        return mNode;
    }

    public void setNode(Node node) {
        mNode = node;
    }

    @Override
    public String toString() {
        return "pageId:"
                + pageId
                + ", action:"
                + action
                + ", vId:"
                + vId
                + ", parentVId:"
                + parentVId
                + ", index:"
                + index
                + ", tagName:"
                + tagName
                + ", events:"
                + events
                + ", attributes:"
                + attributes
                + ", style:"
                + styles
                + ", children:"
                + children
                + ", inspectorVElementType:"
                + inspectorVElementType
                + ", hooks:"
                + hooks
                + ", jsCallbacks:"
                + jsCallbacks;
    }
}

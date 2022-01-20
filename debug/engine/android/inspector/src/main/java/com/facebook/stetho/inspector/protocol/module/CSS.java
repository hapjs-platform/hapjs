/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.inspector.protocol.module;

import android.annotation.SuppressLint;
import com.facebook.stetho.common.ArrayListAccumulator;
import com.facebook.stetho.common.ListUtil;
import com.facebook.stetho.common.LogUtil;
import com.facebook.stetho.common.StringUtil;
import com.facebook.stetho.common.Util;
import com.facebook.stetho.inspector.elements.ComputedStyleAccumulator;
import com.facebook.stetho.inspector.elements.Document;
import com.facebook.stetho.inspector.elements.NodeDescriptor;
import com.facebook.stetho.inspector.elements.Origin;
import com.facebook.stetho.inspector.elements.StyleAccumulator;
import com.facebook.stetho.inspector.elements.StyleRuleNameAccumulator;
import com.facebook.stetho.inspector.helper.ChromePeerManager;
import com.facebook.stetho.inspector.helper.PeersRegisteredListener;
import com.facebook.stetho.inspector.jsonrpc.JsonRpcPeer;
import com.facebook.stetho.inspector.jsonrpc.JsonRpcResult;
import com.facebook.stetho.inspector.protocol.ChromeDevtoolsDomain;
import com.facebook.stetho.inspector.protocol.ChromeDevtoolsMethod;
import com.facebook.stetho.json.ObjectMapper;
import com.facebook.stetho.json.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import org.hapjs.inspector.InspectorVDocument;
import org.hapjs.inspector.InspectorVElement;
import org.hapjs.inspector.InspectorVGroup;
import org.hapjs.inspector.VDocumentProvider;
import org.hapjs.render.css.CSSStyleDeclaration;
import org.hapjs.render.css.CSSStyleRule;
import org.hapjs.render.css.MatchedCSSRuleList;
import org.json.JSONObject;

public class CSS implements ChromeDevtoolsDomain {
    // INSPECTOR ADD BEGIN
    private static final String INLINE_RULENAME = "INLINE";
    private static final String PROPERTY_BLANK = "    ";
    private final ChromePeerManager mPeerManager;
    private final Document mDocument;
    private final ObjectMapper mObjectMapper;
    // INSPECTOR END

    public CSS(Document document) {
        mDocument = Util.throwIfNull(document);
        mObjectMapper = new ObjectMapper();
        mPeerManager = new ChromePeerManager();
        mPeerManager.setListener(new PeerManagerListener());
    }

    @ChromeDevtoolsMethod
    public void enable(JsonRpcPeer peer, JSONObject params) {
    }

    @ChromeDevtoolsMethod
    public void disable(JsonRpcPeer peer, JSONObject params) {
    }

    @ChromeDevtoolsMethod
    public JsonRpcResult getComputedStyleForNode(JsonRpcPeer peer, JSONObject params) {
        final GetComputedStyleForNodeRequest request =
                mObjectMapper.convertValue(params, GetComputedStyleForNodeRequest.class);

        final GetComputedStyleForNodeResult result = new GetComputedStyleForNodeResult();
        result.computedStyle = new ArrayList<>();

        mDocument.postAndWait(
                new Runnable() {
                    @Override
                    public void run() {
                        Object element = mDocument.getElementForNodeId(request.nodeId);

                        if (element == null) {
                            LogUtil.e(
                                    "Tried to get the style of an element that does not exist, using nodeid="
                                            + request.nodeId);

                            return;
                        }

                        mDocument.getElementComputedStyles(
                                element,
                                new ComputedStyleAccumulator() {
                                    @Override
                                    public void store(String name, String value) {
                                        final CSSComputedStyleProperty property =
                                                new CSSComputedStyleProperty();
                                        property.name = name;
                                        property.value = value;
                                        result.computedStyle.add(property);
                                    }
                                });
                    }
                });

        return result;
    }

    @SuppressLint("DefaultLocale")
    @ChromeDevtoolsMethod
    public JsonRpcResult getMatchedStylesForNode(JsonRpcPeer peer, JSONObject params) {
        final GetMatchedStylesForNodeRequest request =
                mObjectMapper.convertValue(params, GetMatchedStylesForNodeRequest.class);

        final GetMatchedStylesForNodeResult result = new GetMatchedStylesForNodeResult();
        result.matchedCSSRules = new ArrayList<>();
        result.inherited = Collections.emptyList();
        result.pseudoElements = Collections.emptyList();

        mDocument.postAndWait(
                new Runnable() {
                    @Override
                    public void run() {
                        final Object elementForNodeId =
                                mDocument.getElementForNodeId(request.nodeId);

                        if (elementForNodeId == null) {
                            LogUtil.w(
                                    "Failed to get style of an element that does not exist, nodeid="
                                            + request.nodeId);
                            return;
                        }

                        // INSPECTOR ADD
                        result.inlineStyle = handleInlineStyle(request.nodeId);
                        mDocument.getElementStyleRuleNames(
                                elementForNodeId,
                                new StyleRuleNameAccumulator() {
                                    @Override
                                    public void store(String ruleName, final boolean editable) {
                                        final ArrayList<CSSProperty> properties = new ArrayList<>();

                                        final RuleMatch match = new RuleMatch();
                                        match.matchingSelectors = ListUtil.newImmutableList(0);

                                        final Selector selector = new Selector();
                                        // INSPECTOR MOD:
                                        // selector.value = ruleName;
                                        selector.text = ruleName;

                                        final CSSRule rule = new CSSRule();
                                        rule.origin = Origin.REGULAR;
                                        rule.selectorList = new SelectorList();
                                        rule.selectorList.selectors =
                                                ListUtil.newImmutableList(selector);
                                        // INSPECTOR MOD
                                        // rule.style = new CSSStyle();
                                        rule.style =
                                                new CSSStyle(request.nodeId, ruleName, editable);
                                        // INSPECTOR DEL BEGIN
                                        /*
                                        rule.style.cssProperties = properties;
                                        rule.style.shorthandEntries = Collections.emptyList();

                                        if (editable) {
                                          rule.style.styleSheetId = String.format(
                                              "%s.%s",
                                              Integer.toString(request.nodeId),
                                              //INSPECTOR MOD
                                              //selector.value);
                                              selector.text);
                                          //INSPECTOR ADD:
                                          rule.style.range = new SourceRange();
                                        }
                                        */
                                        // INSPECTOR END

                                        mDocument.getElementStyles(
                                                elementForNodeId,
                                                ruleName,
                                                new StyleAccumulator() {
                                                    @Override
                                                    // INSPECTOR MOD
                                                    // public void store(String name, String value, boolean isDefault) {
                                                    public void store(String name, String value,
                                                                      boolean disabled) {
                                                        // INSPECTOR MOD
                                                        // final CSSProperty property = new CSSProperty();
                                                        final CSSProperty property =
                                                                new CSSProperty(name, value,
                                                                        disabled);
                                                        // INSPECTOR DEL BEGIN
                                                        /*
                                                        property.name = name;
                                                        property.value = value;
                                                        */
                                                        // INSPECTOR END
                                                        properties.add(property);
                                                    }
                                                });

                                        // INSPECTOR ADD
                                        rule.style.buildCSSStyle(ruleName, properties);
                                        match.rule = rule;
                                        result.matchedCSSRules.add(match);
                                    }
                                });
                    }
                });

        return result;
    }

    // INSPECTOR ADD BEGIN
    @ChromeDevtoolsMethod
    public JsonRpcResult getInlineStylesForNode(JsonRpcPeer peer, JSONObject params) {
        final GetInlineStylesForNodeRequest request =
                mObjectMapper.convertValue(params, GetInlineStylesForNodeRequest.class);
        final GetInlineStylesForNodeResult result = new GetInlineStylesForNodeResult();
        mDocument.postAndWait(
                new Runnable() {
                    @Override
                    public void run() {
                        result.inlineStyle = handleInlineStyle(request.nodeId);
                    }
                });
        return result;
    }

    private CSSStyle handleInlineStyle(final int nodeId) {
        final Object elementForNodeId = mDocument.getElementForNodeId(nodeId);
        if (elementForNodeId == null) {
            LogUtil.w("Failed to get style of an element that does not exist, nodeid=" + nodeId);
            return null;
        }
        boolean editable = true;
        InspectorVElement tmpElement = (InspectorVElement) elementForNodeId;
        if (tmpElement.getVId() == InspectorVElement.ID_BODY
                || tmpElement.getVId() == InspectorVElement.ID_DOC) {
            editable = false;
        }
        CSSStyle inlineStyle = new CSSStyle(nodeId, INLINE_RULENAME, editable);
        final ArrayList<CSSProperty> inlineProperties = new ArrayList<>();
        mDocument.getElementInlineStyles(
                elementForNodeId,
                new StyleAccumulator() {
                    @Override
                    public void store(String name, String value, boolean disabled) {
                        final CSSProperty property = new CSSProperty(name, value, disabled);
                        inlineProperties.add(property);
                    }
                });

        inlineStyle.buildCSSStyle(INLINE_RULENAME, inlineProperties);
        return inlineStyle;
    }
    // INSPECTOR END

    @ChromeDevtoolsMethod
    public SetPropertyTextResult setPropertyText(JsonRpcPeer peer, JSONObject params) {
        final SetPropertyTextRequest request =
                mObjectMapper.convertValue(params, SetPropertyTextRequest.class);

        // INSPECTOR ADD BEGIN:
        if (request == null) {
            return null;
        }
        // END
        final String[] parts = request.styleSheetId.split("\\.", 2);
        final int nodeId = Integer.parseInt(parts[0]);
        final String ruleName = parts[1];

        final String value;
        final String key;
        if (request.text == null || !request.text.contains(":")) {
            key = null;
            value = null;
        } else {
            final String[] keyValue = request.text.split(":", 2);
            key = keyValue[0].trim();
            value = StringUtil.removeAll(keyValue[1], ';').trim();
        }

        final SetPropertyTextResult result = new SetPropertyTextResult();
        result.style = new CSSStyle();
        result.style.styleSheetId = request.styleSheetId;
        result.style.cssProperties = new ArrayList<>();
        result.style.shorthandEntries = Collections.emptyList();

        mDocument.postAndWait(
                new Runnable() {
                    @Override
                    public void run() {
                        final Object elementForNodeId = mDocument.getElementForNodeId(nodeId);

                        if (elementForNodeId == null) {
                            LogUtil.w(
                                    "Failed to get style of an element that does not exist, nodeid="
                                            + nodeId);
                            return;
                        }

                        if (key != null) {
                            mDocument.setElementStyle(elementForNodeId, ruleName, key, value);
                        }

                        mDocument.getElementStyles(
                                elementForNodeId,
                                ruleName,
                                new StyleAccumulator() {
                                    @Override
                                    public void store(String name, String value,
                                                      boolean isDefault) {
                                        final CSSProperty property = new CSSProperty();
                                        property.name = name;
                                        property.value = value;
                                        result.style.cssProperties.add(property);
                                    }
                                });
                    }
                });

        return result;
    }

    // INSPECTOR ADD BEGIN
    @ChromeDevtoolsMethod
    public SetStyleTextsResult setStyleTexts(JsonRpcPeer peer, JSONObject params) {
        final SetStyleTextsRequest request =
                mObjectMapper.convertValue(params, SetStyleTextsRequest.class);
        // INSPECTOR ADD BEGIN:
        if (request == null) {
            return null;
        }
        // END

        final List<StyleDeclarationEdit> edits = request.edits;

        final SetStyleTextsResult result = new SetStyleTextsResult();

        result.styles = new ArrayList<>();

        mDocument.postAndWait(
                new Runnable() {
                    @Override
                    public void run() {
                        for (StyleDeclarationEdit edit : edits) {
                            String[] parts = edit.styleSheetId.split("\\.", 2);
                            if (parts.length != 2) {
                                LogUtil.e("CSS", "setStyleTexts failed, edit.styleSheetId:"
                                        + edit.styleSheetId);
                                continue;
                            }
                            int nodeId = Integer.parseInt(parts[0]);
                            String cssSelectorText = parts[1];

                            CSSStyleDeclaration hybridStyle = handleSetStyleCSSText(edit.text);
                            ArrayList<InspectorVElement> matchedNodeList =
                                    lookupMatchedNodeListByCSSRule(cssSelectorText);
                            if (matchedNodeList.size() == 0) {
                                LogUtil.w(
                                        "Failed to find any matched VDom element for rule name: "
                                                + cssSelectorText);
                                continue;
                            }
                            // update android view component style
                            for (int i = 0; i < matchedNodeList.size(); ++i) {
                                InspectorVElement elementForCSSRule = matchedNodeList.get(i);
                                mDocument.setElementStyle(elementForCSSRule, cssSelectorText,
                                        hybridStyle);
                            }
                            // prepare the CDP message to be sent;
                            CSSStyle style = new CSSStyle(nodeId, cssSelectorText, true);
                            final ArrayList<CSSProperty> cssProperties = new ArrayList<>();
                            ListIterator<String> iterator = hybridStyle.getReversedListIterator();
                            while (iterator.hasPrevious()) {
                                String entryKey = iterator.previous();
                                org.hapjs.render.css.CSSProperty hybridProperty =
                                        hybridStyle.getProperty(entryKey);
                                String name = hybridProperty.getInspectorName();
                                String value = hybridProperty.getValueText();
                                boolean disabled = hybridProperty.getDisabled();
                                CSSProperty property = new CSSProperty(name, value, disabled);
                                cssProperties.add(property);
                            }
                            style.buildCSSStyle(cssSelectorText, cssProperties);
                            result.styles.add(style);
                        }
                    }
                });

        return result;
    }

    /**
     * lookup the matched VDom elements list for rule name;
     *
     * @param cssSelectorText String
     * @return ArrayList<InspectorVElement> object for matched VDom element list
     */
    private ArrayList<InspectorVElement> lookupMatchedNodeListByCSSRule(String cssSelectorText) {
        // get VDocumentRoot
        VDocumentProvider mDocProvider = VDocumentProvider.getCurrent();
        Object root = mDocProvider.getRootElement();
        NodeDescriptor rootDescriptor = mDocProvider.getNodeDescriptor(root);
        // get VInpsectorDocument
        ArrayListAccumulator<Object> accumulator = new ArrayListAccumulator<>();
        rootDescriptor.getChildren(root, accumulator);
        InspectorVDocument vdoc = (InspectorVDocument) accumulator.get(0);
        ArrayList<InspectorVElement> result = new ArrayList<InspectorVElement>();
        traverseInspectorVDomTree(vdoc, cssSelectorText, result);
        return result;
    }

    /**
     * traverse the InspectorVDom tree to find out the node list that can be represented by the rule
     * name;
     *
     * @param root:           InspectorVElement tree node
     * @param cssSelectorText String
     * @param result          the matched VDom element list
     */
    private void traverseInspectorVDomTree(
            InspectorVElement root, String cssSelectorText, ArrayList<InspectorVElement> result) {
        MatchedCSSRuleList m = root.getMatchedCSSRuleList();
        if (m != null) {
            for (int j = 0; j < m.length(); ++j) {
                org.hapjs.render.css.CSSRule cssRule =
                        (org.hapjs.render.css.CSSRule) m.getCSSStyleRule(j);
                if (((CSSStyleRule) cssRule).getSelectorText().contains(cssSelectorText)) {
                    result.add(root);
                }
            }
        }

        if (root instanceof InspectorVGroup) {
            for (int i = 0; i < ((InspectorVGroup) root).getChildren().size(); ++i) {
                InspectorVElement n = ((InspectorVGroup) root).getChildren().get(i);
                traverseInspectorVDomTree(n, cssSelectorText, result);
            }
        }
    }

    /**
     * @param requestText: cssText
     * @return {CSSStyleDeclaration}： list of CSSProperty instance, e.g. {name: '', value: '',
     * disabled: }
     */
    private CSSStyleDeclaration handleSetStyleCSSText(final String requestText) {
        // filter whole CSSText
        final String[] properties = requestText.replaceAll(";", "").split("\n");
        CSSStyleDeclaration hybridStyle = new CSSStyleDeclaration();
        try {
            for (int i = 0; i < properties.length; i++) {
                // filter whole CSSProperty
                String property = properties[i].trim();
                if (property.isEmpty()) {
                    continue;
                }
                if (!property.contains(":")) {
                    LogUtil.e("CSS", "setStyleTexts failed, reqText:" + requestText);
                    continue;
                }
                final boolean isCommented =
                        property.length() > 4
                                && property.indexOf("/*") == 0
                                && property.lastIndexOf("*/") == property.length() - 2;
                if (isCommented) {
                    property = property.substring(2, property.length() - 2).trim();
                }
                // filter each key:value of CSSProperty
                final int pos = property.lastIndexOf(':');
                String name = property.substring(0, pos).trim();
                String value = property.substring(pos + 1).trim();
                if (name.isEmpty() || value.isEmpty()) {
                    continue;
                }
                boolean disabled = isCommented;
                hybridStyle.setInspectorProperty(name, value, disabled);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hybridStyle;
    }

    private static class StyleDeclarationEdit implements JsonRpcResult {
        @JsonProperty(required = true)
        public String styleSheetId;

        @JsonProperty
        public SourceRange range;

        @JsonProperty(required = true)
        public String text;
    }

    private static class SetStyleTextsRequest implements JsonRpcResult {
        @JsonProperty(required = true)
        public List<StyleDeclarationEdit> edits;
    }

    private static class SetStyleTextsResult implements JsonRpcResult {
        @JsonProperty(required = true)
        public List<CSSStyle> styles;
    }

    private static class GetInlineStylesForNodeRequest {
        @JsonProperty(required = true)
        public int nodeId;
    }

    private static class GetInlineStylesForNodeResult implements JsonRpcResult {
        @JsonProperty
        public CSSStyle inlineStyle;
    }
    // INSPECTOR END

    private static class CSSComputedStyleProperty {
        @JsonProperty(required = true)
        public String name;

        @JsonProperty(required = true)
        public String value;
    }

    private static class RuleMatch {
        @JsonProperty
        public CSSRule rule;

        @JsonProperty
        public List<Integer> matchingSelectors;
    }

    private static class SelectorList {
        @JsonProperty
        public List<Selector> selectors;

        @JsonProperty
        public String text;
    }

    private static class SourceRange {
        @JsonProperty(required = true)
        public int startLine;

        @JsonProperty(required = true)
        public int startColumn;

        @JsonProperty(required = true)
        public int endLine;

        @JsonProperty(required = true)
        public int endColumn;

        // INSPECTOR ADD BEGIN
        public SourceRange() {
            startLine = 0;
            startColumn = 0;
            endLine = 0;
            endColumn = 0;
        }
        // END
    }

    private static class Selector {
        @JsonProperty(required = true)
        // INSPECTOR MOD
        // public String value;
        public String text; // see potocol.json id Value, the name is "text"

        @JsonProperty
        public SourceRange range;
    }

    private static class CSSRule {
        @JsonProperty
        public String styleSheetId;

        @JsonProperty(required = true)
        public SelectorList selectorList;

        @JsonProperty
        public Origin origin;

        @JsonProperty
        public CSSStyle style;
    }

    private static class CSSStyle {
        @JsonProperty
        public String styleSheetId;

        @JsonProperty(required = true)
        public List<CSSProperty> cssProperties;

        @JsonProperty
        public List<ShorthandEntry> shorthandEntries;

        @JsonProperty
        public String cssText;

        @JsonProperty
        public SourceRange range;

        // INSPECTOR ADD BEGIN
        private boolean editable;

        private CSSStyle() {
        }

        private CSSStyle(int nodeId, String rulename, boolean editable) {
            styleSheetId = String.format("%s.%s", Integer.toString(nodeId), rulename);
            shorthandEntries = Collections.emptyList();
            cssProperties = new ArrayList<>();
            this.editable = editable;
        }

        private CSSStyle setRange(int startLine, String ruleName) {
            if (editable) {
                range = new SourceRange();
                range.startLine = startLine;
                range.startColumn = ruleName.length() + 2; // plus whitespace and brace
                range.endLine = startLine + cssProperties.size() + 1;
                range.endColumn = 0;
            }
            return this;
        }

        private CSSStyle setCSSText() {
            StringBuilder sb = new StringBuilder("\n");
            for (int i = 0; i < cssProperties.size(); i++) {
                sb.append(PROPERTY_BLANK + cssProperties.get(i).text + "\n");
            }
            cssText = sb.toString();
            return this;
        }

        private CSSStyle setCSSPropertiesRange() {
            for (int i = 0; i < cssProperties.size(); i++) {
                CSSProperty cssProperty = cssProperties.get(i);
                cssProperty.setRange(i + 1, cssProperty.text);
            }
            return this;
        }

        public void buildCSSStyle(String ruleName, List<CSSProperty> properties) {
            // build CSSStyle must follow the orders:
            cssProperties = properties;
            setCSSPropertiesRange();
            setCSSText();
            setRange(0, ruleName);
        }
        // INSPECTOR END
    }

    private static class ShorthandEntry {
        @JsonProperty(required = true)
        public String name;

        @JsonProperty(required = true)
        public String value;

        @JsonProperty
        // INSPECTOR MOD:
        // public Boolean imporant;
        public Boolean important; // spell error
    }

    private static class CSSProperty {
        @JsonProperty(required = true)
        public String name;

        @JsonProperty(required = true)
        public String value;

        @JsonProperty
        public Boolean important;

        @JsonProperty
        public Boolean implicit;

        @JsonProperty
        public String text;

        @JsonProperty
        public Boolean parsedOk;

        @JsonProperty
        public Boolean disabled;

        @JsonProperty
        public SourceRange range;

        // INSPECTOR ADD BEGIN
        private CSSProperty() {
        }

        private CSSProperty(String name, String value, boolean disabled) {
            this.name = name;
            this.value = value;
            this.text = name + ": " + value + ";";
            this.range = new SourceRange();
            this.disabled = disabled;
            if (disabled) {
                this.text = "/* " + this.text + " */";
            }
        }

        private void setRange(int startLine, String propertyText) {
            range.startLine = startLine;
            range.startColumn = PROPERTY_BLANK.length();
            range.endLine = startLine;
            range.endColumn = PROPERTY_BLANK.length() + propertyText.length();
        }
        // INSPECTOR END
    }

    private static class PseudoIdMatches {
        @JsonProperty(required = true)
        public int pseudoId;

        @JsonProperty(required = true)
        public List<RuleMatch> matches;

        public PseudoIdMatches() {
            this.matches = new ArrayList<>();
        }
    }

    private static class GetComputedStyleForNodeRequest {
        @JsonProperty(required = true)
        public int nodeId;
    }

    private static class InheritedStyleEntry {
        @JsonProperty(required = true)
        public CSSStyle inlineStyle;

        @JsonProperty(required = true)
        public List<RuleMatch> matchedCSSRules;
    }

    private static class GetComputedStyleForNodeResult implements JsonRpcResult {
        @JsonProperty(required = true)
        public List<CSSComputedStyleProperty> computedStyle;
    }

    private static class GetMatchedStylesForNodeRequest implements JsonRpcResult {
        @JsonProperty(required = true)
        public int nodeId;

        @JsonProperty
        public Boolean excludePseudo;

        @JsonProperty
        public Boolean excludeInherited;
    }

    private static class GetMatchedStylesForNodeResult implements JsonRpcResult {
        @JsonProperty
        public List<RuleMatch> matchedCSSRules;

        @JsonProperty
        public List<PseudoIdMatches> pseudoElements;

        @JsonProperty
        public List<InheritedStyleEntry> inherited;

        // INSPECTOR ADD
        @JsonProperty
        public CSSStyle inlineStyle;
    }

    private static class SetPropertyTextRequest implements JsonRpcResult {
        @JsonProperty(required = true)
        public String styleSheetId;

        @JsonProperty(required = true)
        public String text;
    }

    private static class SetPropertyTextResult implements JsonRpcResult {
        @JsonProperty(required = true)
        public CSSStyle style;
    }

    private final class PeerManagerListener extends PeersRegisteredListener {
        @Override
        protected synchronized void onFirstPeerRegistered() {
            mDocument.addRef();
        }

        @Override
        protected synchronized void onLastPeerUnregistered() {
            mDocument.release();
        }
    }
}

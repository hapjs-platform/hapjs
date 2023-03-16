/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.action;

import android.text.TextUtils;
import android.util.ArrayMap;
import java.util.Iterator;
import java.util.Map;
import org.hapjs.common.json.JSONArray;
import org.hapjs.common.json.JSONObject;
import org.hapjs.logging.RuntimeLogManager;
import org.hapjs.render.ComponentAction;
import org.hapjs.render.DebugUtils;
import org.hapjs.render.Page;
import org.hapjs.render.VDomChangeAction;
import org.hapjs.render.css.CSSParser;
import org.hapjs.render.css.CSSStyleDeclaration;
import org.hapjs.render.css.CSSStyleSheet;
import org.hapjs.render.css.MatchedCSSRuleList;
import org.json.JSONException;

class RenderActionParser {

    private static String sPackage;

    static VDomChangeAction objToChangeActions(
            int pageId, String method, JSONArray args, RenderActionDocument document, String pkg)
            throws JSONException {
        if (TextUtils.isEmpty(method)) {
            return null;
        }

        sPackage = pkg;
        VDomChangeAction action = new VDomChangeAction();
        action.pageId = pageId;
        switch (method) {
            case "createBody": {
                if (DebugUtils.DBG) {
                    DebugUtils.startRecord("JsToNative_generateDom");
                }
                createBody(document, action, args.getJSONObject(0));
                break;
            }
            case "addElement": {
                addElement(document, action, args.getInt(0), args.getJSONObject(1), args.getInt(2));
                break;
            }
            case "removeElement": {
                removeElement(document, action, args.getInt(0));
                break;
            }
            case "moveElement": {
                moveElement(action, args.getInt(0), args.getInt(1), args.getInt(2));
                break;
            }
            case "updateStyle": { // update style (inline style change)
                updateInlineStyle(document, action, args.getInt(0), args.getJSONObject(1));
                break;
            }
            case "updateStyles": { // update style (id or class change), or inspector set styles
                updateStyles(document, action, args.getInt(0), args.getJSONObject(1));
                break;
            }
            case "updateAttrs": {
                updateAttrs(action, args.getInt(0), args.getJSONObject(1));
                break;
            }
            case "addEvent": {
                addEvent(action, args.getInt(0), args.getString(1));
                break;
            }
            case "removeEvent": {
                removeEvent(action, args.getInt(0), args.getString(1));
                break;
            }
            case "createFinish": {
                if (DebugUtils.DBG) {
                    DebugUtils.endRecord("JsToNative_generateDom");
                }
                if(args.length() != 0){
                    parseJsCallback(args.getJSONObject(0),action);
                }
                action.action = VDomChangeAction.ACTION_CREATE_FINISH;
                break;
            }
            case "updateFinish": {
                if(args.length() != 0){
                    parseJsCallback(args.getJSONObject(0),action);
                }
                action.action = VDomChangeAction.ACTION_UPDATE_FINISH;
                break;
            }
            case "updateTitleBar": {
                action.action = VDomChangeAction.ACTION_UPDATE_TITLE_BAR;
                JSONObject obj = args.getJSONObject(0);
                parseTitleBarInfo(obj, action);
                break;
            }
            case "updateStatusBar": {
                action.action = VDomChangeAction.ACTION_UPDATE_STATUS_BAR;
                JSONObject obj = args.getJSONObject(0);
                parseStatusBarInfo(obj, action);
                break;
            }
            case "updateProps": {
                updateProps(document, action, args.getInt(0), args.getJSONObject(1));
                break;
            }
            case "updateStyleObject": {
                updateStyleObject(
                        document,
                        args.getInt(0),
                        args.getBoolean(1),
                        args.getString(2),
                        args.getInt(3),
                        args.getJSONObject(4));
                return null;
            }
            case "exitFullscreen": {
                action.action = VDomChangeAction.ACTION_EXIT_FULLSCREEN;
                break;
            }
            case "statistics": {
                action.action = VDomChangeAction.ACTION_STATISTICS;
                JSONObject obj = args.getJSONObject(0);
                parseStatisticsData(obj, action);
                break;
            }
            case "scrollTo": {
                action.action = VDomChangeAction.ACTION_PAGE_SCROLL;
                JSONObject obj = args.getJSONObject(0);
                parseScrollData(obj, action, Page.PAGE_SCROLL_TYPE_TO);
                break;
            }
            case "scrollBy": {
                action.action = VDomChangeAction.ACTION_PAGE_SCROLL;
                JSONObject obj = args.getJSONObject(0);
                parseScrollData(obj, action, Page.PAGE_SCROLL_TYPE_BY);
                break;
            }
            case "hideSkeleton": {
                action.action = VDomChangeAction.ACTION_HIDE_SKELETON;
                break;
            }
            case "setSecure": {
                action.action = VDomChangeAction.ACTION_SET_SECURE;
                JSONArray array = args.getJSONArray(0);
                action.isSecure = array.length() > 0 ? array.getBoolean(0) : false;
                break;
            }
            default:
                throw new IllegalArgumentException("Unsupported method:" + method);
        }

        return action;
    }

    private static void updateStyleObject(
            RenderActionDocument document,
            int ref,
            boolean isDocLevel,
            String name,
            int styleObjectId,
            JSONObject styleObject)
            throws JSONException {
        CSSStyleSheet ss = CSSParser.parseCSSStyleSheet(styleObject);
        if (isDocLevel) {
            document.registerDocLevelStyleSheet(styleObjectId, ss);
        } else {
            document.registerStyleSheet(styleObjectId, ss);
        }
    }

    private static void updateProps(
            RenderActionDocument document, VDomChangeAction action, int id, JSONObject eleInfo)
            throws JSONException {
        action.action = VDomChangeAction.ACTION_UPDATE_STYLE;
        action.vId = id;

        RenderActionNode node = document.findOrCreateNode(id);

        if (eleInfo.has("prop")) {
            JSONObject prop = eleInfo.getJSONObject("prop");

            if (prop.has("_useParentStyle")) {
                action.action = VDomChangeAction.ACTION_UPDATE_STYLE;
                boolean useParentStyle = prop.getBoolean("_useParentStyle");
                node.setUseParentStyle(useParentStyle);

                updateStyles(node, action);
            }
        }
    }

    private static void removeEvent(VDomChangeAction action, int id, String event)
            throws JSONException {
        action.action = VDomChangeAction.ACTION_REMOVE_EVENT;
        action.vId = id;
        action.events.add(event);
    }

    private static void addEvent(VDomChangeAction action, int id, String event)
            throws JSONException {
        action.action = VDomChangeAction.ACTION_ADD_EVENT;
        action.vId = id;
        action.events.add(event);
    }

    private static void updateAttrs(VDomChangeAction action, int id, JSONObject eleInfo)
            throws JSONException {
        action.action = VDomChangeAction.ACTION_UPDATE_ATTRS;
        action.vId = id;
        parseAttr(eleInfo, action);
    }

    private static void updateInlineStyle(
            RenderActionDocument document, VDomChangeAction action, int id, JSONObject eleInfo)
            throws JSONException {
        action.action = VDomChangeAction.ACTION_UPDATE_STYLE;
        action.vId = id;

        // find node
        RenderActionNode node = document.findOrCreateNode(action.vId);

        if (eleInfo.has("style")) { // TODO key: inlineStyle or style?
            CSSStyleDeclaration diffProps =
                    CSSParser.parseInlineStyle(node, eleInfo.getJSONObject("style"));
            node.updateInlineStyles(diffProps);

            // calculate css inline style, only update diff
            action.styles.putAll(diffProps.convertStyleProps());
        }
        action.inlineCSSRule = node.getInlineStyle();
    }

    private static void updateStyles(
            RenderActionDocument document, VDomChangeAction action, int id, JSONObject eleInfo)
            throws JSONException {
        action.action = VDomChangeAction.ACTION_UPDATE_STYLE;
        action.vId = id;
        parseAttr(eleInfo, action);

        // find node
        RenderActionNode node = document.findOrCreateNode(action.vId);

        if (!action.attributes.isEmpty()) {
            node.updateCSSAttrs(action.attributes);
        }

        if (eleInfo.has("style")) { // TODO remove this (only from inspector)
            CSSStyleDeclaration diffProps =
                    CSSParser.parseInlineStyle(node, eleInfo.getJSONObject("style"));
            node.updateInlineStyles(diffProps);
        }

        updateStyles(node, action);
    }

    public static void updateStyles(RenderActionNode node, VDomChangeAction action) {
        RuntimeLogManager.getDefault().logRenderTaskStart(sPackage, "CSSCalculator");
        updateStyles(node, action, node.shouldRestyling());
        RuntimeLogManager.getDefault().logRenderTaskEnd(sPackage, "CSSCalculator");
    }

    private static void updateStyles(
            RenderActionNode node, VDomChangeAction action, boolean updateChild) {
        action.vId = node.getVId();
        MatchedCSSRuleList matchedStyles = node.calMatchedStyles();
        action.matchedCSSRuleList = matchedStyles;
        action.inlineCSSRule = node.getInlineStyle();
        action.setNode(node);
        // calculate css style
        action.styles.putAll(node.calFinalStyle(matchedStyles).convertStyleProps());

        if (updateChild) {
            // SynchronizedList 在增强for循环中是非线程安全的，需要同步
            synchronized (node.getChildren()){
                for (RenderActionNode child : node.getChildren()) {
                    VDomChangeAction childAction = new VDomChangeAction();
                    updateStyles(child, childAction, true);
                    action.children.add(childAction);
                }
            }
        }
    }

    public static void updateParent(
            RenderActionDocument document, RenderActionNode node, VDomChangeAction action) {
        if (document != null && node != null && action != null) {
            if (action.parentVId != -1) {
                synchronized (document) {
                    RenderActionNode parent = document.findNodeById(action.parentVId);
                    RenderActionNode oldParent = node.getParent(); // 如果已经有父节点，判断是否需要更新
                    if (parent != null) {
                        if (oldParent != parent) {
                            if (oldParent != null) {
                                oldParent.removeChild(node); // 需要更新父节点，从旧父节点中移除
                            }
                            node.setParent(parent);
                            parent.appendChild(node); // 这里目前不需要关心顺序
                        }
                    } else {
                        // 如果没有获取到父节点，设置当前节点为脏节点
                        node.setDirty(true);
                    }
                }
            }
        }
    }

    private static void moveElement(VDomChangeAction action, int id, int parentId, int index)
            throws JSONException {
        action.action = VDomChangeAction.ACTION_MOVE;
        action.vId = id;
        action.parentVId = parentId;
        action.index = index;
    }

    private static void removeElement(
            RenderActionDocument document, VDomChangeAction action, int id) {
        action.action = VDomChangeAction.ACTION_REMOVE;
        action.vId = id;
        document.removeNode(id);
    }

    private static void addElement(
            RenderActionDocument document,
            VDomChangeAction action,
            int parentId,
            JSONObject eleInfo,
            int index)
            throws JSONException {
        action.action = VDomChangeAction.ACTION_ADD;
        action.parentVId = parentId;
        action.index = index;
        parseAddElementInfo(document, eleInfo, action);
    }

    private static void createBody(
            RenderActionDocument document, VDomChangeAction action, JSONObject eleInfo)
            throws JSONException {
        action.action = VDomChangeAction.ACTION_CREATE_BODY;
        parseAddElementInfo(document, eleInfo, action);
    }

    private static void createRenderActionNode(
            RenderActionDocument document, VDomChangeAction action, JSONObject eleInfo)
            throws JSONException {
        RenderActionNode node = document.findOrCreateNode(action.vId, action.tagName);

        updateParent(document, node, action);
        node.updateCSSAttrs(action.attributes);

        if (eleInfo.has("prop")) {
            JSONObject prop = eleInfo.getJSONObject("prop");
            int styleObjectId = prop.getInt("_styleObjectId");
            node.setStyleObjectId(styleObjectId);

            if (prop.has("_useParentStyle")) {
                boolean useParentStyle = prop.getBoolean("_useParentStyle");
                node.setUseParentStyle(useParentStyle);
            }
        }

        CSSStyleSheet cacheStyle = document.findStyleSheetById(node.getStyleObjectId());
        if (cacheStyle == null && eleInfo.has("styleObject")) {
            CSSStyleSheet ss = CSSParser.parseCSSStyleSheet(eleInfo.getJSONObject("styleObject"));
            ss.addOwner(node);
            document.registerStyleSheet(node.getStyleObjectId(), ss);
        } else if (cacheStyle != null) {
            cacheStyle.addOwner(node);
        }

        if (eleInfo.has("inlineStyle")) {
            CSSStyleDeclaration props =
                    CSSParser.parseInlineStyle(node, eleInfo.getJSONObject("inlineStyle"));
            node.updateInlineStyles(props); // should update after setting of parent and styleSheet
        }

        updateStyles(node, action);
    }

    private static void parseAddElementInfo(
            RenderActionDocument document, JSONObject eleInfo, VDomChangeAction action)
            throws JSONException {
        parseRef(eleInfo, action);
        parseTagName(eleInfo, action);
        parseAttr(eleInfo, action);
        parseEvent(eleInfo, action);
        parseHooks(eleInfo,action);

        // create node
        createRenderActionNode(document, action, eleInfo);
        parseChild(document, eleInfo, action);
    }

    private static void parseRef(JSONObject eleInfo, VDomChangeAction action) throws JSONException {
        if (eleInfo.has("ref")) {
            action.vId = Integer.parseInt(eleInfo.getString("ref"));
        }
    }

    private static void parseTagName(JSONObject eleInfo, VDomChangeAction action)
            throws JSONException {
        if (eleInfo.has("type")) {
            action.tagName = eleInfo.getString("type").intern();
        }
    }

    private static void parseEvent(JSONObject eleInfo, VDomChangeAction action)
            throws JSONException {
        if (eleInfo.has("event")) {
            JSONArray arr = eleInfo.getJSONArray("event");
            final int N = arr.length();
            for (int i = 0; i < N; i++) {
                action.events.add(arr.getString(i));
            }
        }
    }

    private static void parseChild(
            RenderActionDocument document, JSONObject eleInfo, VDomChangeAction action)
            throws JSONException {
        if (eleInfo.has("children")) {
            JSONArray arr = eleInfo.getJSONArray("children");
            final int N = arr.length();
            for (int i = 0; i < N; i++) {
                VDomChangeAction a = new VDomChangeAction();
                a.pageId = action.pageId;
                a.action = VDomChangeAction.ACTION_ADD;
                a.parentVId = action.vId;
                a.index = i;
                JSONObject obj = arr.getJSONObject(i);
                parseAddElementInfo(document, obj, a);
                action.children.add(a);
            }
        }
    }

    private static void parseAttr(JSONObject jsObj, VDomChangeAction action) throws JSONException {
        if (jsObj.has("attr")) {
            JSONObject attr = jsObj.getJSONObject("attr");
            for (Iterator keys = attr.keys(); keys.hasNext(); ) {
                String key = ((String) keys.next()).intern();
                Object value = getTransformedJSONObject(attr, key);
                action.attributes.put(key, value);
            }
        }
    }

    private static void parseTitleBarInfo(JSONObject jsObj, VDomChangeAction action)
            throws JSONException {
        for (Iterator keys = jsObj.keys(); keys.hasNext(); ) {
            String key = (String) keys.next();
            Object value = getTransformedJSONObject(jsObj, key);
            action.titles.put(key, value);
        }
    }

    private static void parseStatusBarInfo(JSONObject jsObj, VDomChangeAction action)
            throws JSONException {
        for (Iterator keys = jsObj.keys(); keys.hasNext(); ) {
            String key = (String) keys.next();
            Object value = getTransformedJSONObject(jsObj, key);
            action.status.put(key, value);
        }
    }

    private static void parseStatisticsData(JSONObject jsObj, VDomChangeAction action)
            throws JSONException {
        for (Iterator keys = jsObj.keys(); keys.hasNext(); ) {
            String key = (String) keys.next();
            Object value = getTransformedJSONObject(jsObj, key);
            action.extra.put(key, value);
        }
    }

    private static void parseScrollData(JSONObject jsObj, VDomChangeAction action,
                                        String scrollType)
            throws JSONException {
        action.scrolls.put(Page.KEY_PAGE_SCROLL_TYPE, scrollType);
        for (Iterator keys = jsObj.keys(); keys.hasNext(); ) {
            String key = (String) keys.next();
            Object value = getTransformedJSONObject(jsObj, key);
            action.scrolls.put(key, value);
        }
    }

    private static void parseHooks(JSONObject eleInfo, VDomChangeAction action) throws JSONException {
        if (eleInfo.has("hooks")) {
            JSONArray arr = eleInfo.getJSONArray("hooks");
            final int N = arr.length();
            for (int i = 0; i < N; i++) {
                action.hooks.add(arr.getString(i));
            }
        }
    }


    private static void parseJsCallback(JSONObject eleInfo, VDomChangeAction action) throws JSONException {
        if (eleInfo.has("jsCallbacks")) {
            action.jsCallbacks = eleInfo.getBoolean("jsCallbacks");
        }
    }

    static ComponentAction objToComponentAction(
            String component, String ref, String method, JSONArray args) throws JSONException {
        ComponentAction action = new ComponentAction();
        action.component = component;
        action.ref = Integer.parseInt(ref);
        action.method = method;

        if (args != null && args.length() > 0) {
            JSONObject arg0 = args.getJSONObject(0);
            Map<String, Object> argMap = new ArrayMap<>();
            for (Iterator keys = arg0.keys(); keys.hasNext(); ) {
                String key = (String) keys.next();
                Object value = getTransformedJSONObject(arg0, key);
                argMap.put(key, value);
                action.args = argMap;
            }
        }

        return action;
    }

    private static Object getTransformedJSONObject(JSONObject jsObj, String key)
            throws JSONException {
        Object obj = jsObj.get(key);
        // org.hapjs.common.json.JSONArray to org.json.JSONArray, component is using org.json.JSONArray
        if (obj instanceof JSONObject) {
            obj = new org.json.JSONObject(obj.toString());
        } else if (obj instanceof JSONArray) {
            obj = new org.json.JSONArray(obj.toString());
        }
        return obj;
    }
}

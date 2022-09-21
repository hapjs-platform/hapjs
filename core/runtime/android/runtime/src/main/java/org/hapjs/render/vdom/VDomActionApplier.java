/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.vdom;

import android.content.Context;
import android.util.Log;
import android.view.View;
import java.util.Map;
import org.hapjs.bridge.HostCallbackManager;
import org.hapjs.component.Component;
import org.hapjs.component.ComponentDataHolder;
import org.hapjs.component.ComponentFactory;
import org.hapjs.component.Container;
import org.hapjs.component.Recycler;
import org.hapjs.component.RecyclerDataItem;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.logging.CardLogManager;
import org.hapjs.render.CallBackJsUtils;
import org.hapjs.render.DebugUtils;
import org.hapjs.render.RootView;
import org.hapjs.render.VDomChangeAction;
import org.hapjs.render.css.Node;
import org.hapjs.render.jsruntime.JsThread;
import org.hapjs.render.skeleton.SkeletonProvider;
import org.hapjs.runtime.HapEngine;
import org.hapjs.runtime.inspect.InspectorManager;
import org.hapjs.runtime.inspect.InspectorVElementType;
import org.json.JSONArray;

public class VDomActionApplier {

    private static final String TAG = "VDomActionApplier";

    private RecyclerDataItem.Creator mRecyclerDataItemCreator;

    public VDomActionApplier() {
    }

    private static Component generateComponent(
            HapEngine hapEngine,
            Context context,
            VDomChangeAction action,
            Container parent,
            RenderEventCallback renderEventCallback,
            Map<String, Object> savedState) {
        Component component =
                ComponentFactory.createComponent(
                        hapEngine,
                        context,
                        action.tagName,
                        parent,
                        action.vId,
                        renderEventCallback,
                        action.attributes,
                        savedState);
        Node node = action.getNode();
        // must be called before createview because createview will bindstyle
        if (node != null) {
            component.setCssNode(node);
            node.setComponent(component);
        }
        component.bindAttrs(action.attributes);
        component.bindStyles(action.styles);
        component.bindEvents(action.events);
        component.applyHook(action.hooks);
        component.createView();

        return component;
    }

    public void applyChangeAction(
            HapEngine hapEngine,
            Context context,
            JsThread jsThread,
            VDomChangeAction action,
            VDocument doc,
            RenderEventCallback renderEventCallback) {
        if (DebugUtils.DBG) {
            Log.d(TAG, "applyChangeAction:" + action);
        }
        switch (action.action) {
            case VDomChangeAction.ACTION_PRE_CREATE_BODY: {
                VElement bodyEle = doc.getElementById(VElement.ID_BODY);
                if (bodyEle == null) {
                    createBody(hapEngine, context, doc, renderEventCallback);
                }
                break;
            }
            case VDomChangeAction.ACTION_CREATE_BODY: {
                VElement bodyEle = doc.getElementById(VElement.ID_BODY);
                if (bodyEle == null) {
                    bodyEle = createBody(hapEngine, context, doc, renderEventCallback);
                }
                // add body
                addElement(hapEngine, context, action, doc, renderEventCallback, (VGroup) bodyEle);
                action.inspectorVElementType = InspectorVElementType.VGROUP;
                if(action.hooks.contains("mounted")){
                    CallBackJsUtils.getInstance().callBackJs(jsThread,action.pageId,CallBackJsUtils.TYPE_NODE_MOUNTED,action.vId);
                }
                break;
            }
            case VDomChangeAction.ACTION_ADD: {
                VGroup parent = (VGroup) doc.getElementById(action.parentVId);
                if (parent == null) {
                    Log.e(TAG, "parent is null, " + action);
                    return;
                }
                addElement(hapEngine, context, action, doc, renderEventCallback, parent);
                if (action.hooks.contains("mounted")) {
                    CallBackJsUtils.getInstance().callBackJs(jsThread, action.pageId, CallBackJsUtils.TYPE_NODE_MOUNTED, action.vId);
                }
                break;
            }
            case VDomChangeAction.ACTION_UPDATE_FINISH: {
                if(action.jsCallbacks){
                    CallBackJsUtils.getInstance().callBackJs(jsThread,action.pageId,CallBackJsUtils.TYPE_PAGE_UPDATE_FINISH,action.vId);
                }
                break;
            }
            case VDomChangeAction.ACTION_CREATE_FINISH: {
                doc.setCreateFinishFlag(true);
                jsThread.postInitializePage(action.pageId);
                if (HapEngine.getInstance(jsThread.getAppInfo().getPackage()).isCardMode()) {
                    HostCallbackManager.getInstance().onCardCreate(action.pageId);
                }
                if (action.jsCallbacks) {
                    CallBackJsUtils.getInstance().callBackJs(jsThread, action.pageId, CallBackJsUtils.TYPE_PAGE_CREATE_FINISH, action.vId);
                }
                break;
            }
            case VDomChangeAction.ACTION_ADD_EVENT: {
                VElement ele = doc.getElementById(action.vId);
                if (ele == null) {
                    Log.e(TAG, "ele is null, " + action);
                    return;
                }
                ele.getComponentDataHolder().bindEvents(action.events);
                break;
            }
            case VDomChangeAction.ACTION_REMOVE_EVENT: {
                VElement ele = doc.getElementById(action.vId);
                if (ele == null) {
                    Log.e(TAG, "ele is null, " + action);
                    return;
                }
                ele.getComponentDataHolder().removeEvents(action.events);
                break;
            }
            case VDomChangeAction.ACTION_MOVE: {
                VElement ele = doc.getElementById(action.vId);
                if (ele == null) {
                    Log.e(TAG, "ele is null, " + action);
                    return;
                }
                VGroup parent = ele.getParent();
                VGroup newParent = (VGroup) doc.getElementById(action.parentVId);
                int oldIndex = parent.getChildren().indexOf(ele);
                if (oldIndex == action.index && parent == newParent) {
                    return;
                }

                parent.removeChild(ele);
                if (newParent == null) {
                    Log.e(TAG, "newParent is null, " + action);
                    return;
                }
                newParent.addChild(ele, action.index);
                break;
            }
            case VDomChangeAction.ACTION_REMOVE: {
                VElement ele = doc.getElementById(action.vId);
                if (ele == null) {
                    Log.e(TAG, "ele is null, " + action);
                    return;
                }
                if (ele.getComponent() != null &&
                        ele.getComponent().getHook() != null &&
                        ele.getComponent().getHook().contains("destroy")) {
                    CallBackJsUtils.getInstance().callBackJs(jsThread, action.pageId, CallBackJsUtils.TYPE_NODE_DESTROY, action.vId);
                }
                ele.getParent().removeChild(ele);
                break;
            }
            case VDomChangeAction.ACTION_UPDATE_STYLE: {
                VElement ele = doc.getElementById(action.vId);
                if (ele == null) {
                    Log.w(TAG, "ele is null, " + action);
                    return;
                }

                CallBackJsUtils.getInstance().getCallBackJsParams(ele,action);
                updateStyles(doc, action);
                if (ele.getComponent() != null &&
                        ele.getComponent().getHook() != null &&
                        ele.getComponent().getHook().contains("update")) {
                    if (!action.attributes.isEmpty()) {
                        ele.getComponent().getAttrsDomData().putAll(action.attributes);
                    }
                    CallBackJsUtils.getInstance().callBackJs(jsThread, action.pageId, CallBackJsUtils.TYPE_NODE_UPDATE, action.vId);
                }
                break;
            }
            case VDomChangeAction.ACTION_UPDATE_ATTRS: {
                VElement ele = doc.getElementById(action.vId);
                if (ele == null) {
                    Log.e(TAG, "ele is null, " + action);
                    return;
                }

                CallBackJsUtils.getInstance().getCallBackJsParams(ele,action);

                if (action.attributes.containsKey("type")) {
                    VGroup parent = ele.getParent();
                    if (parent == null) {
                        Log.e(
                                TAG,
                                "element update attrs error not found parrent id:"
                                        + action.vId
                                        + ",type:"
                                        + action.tagName
                                        + ", parentId:"
                                        + action.parentVId);
                        return;
                    }

                    Class<? extends Component> clazz =
                            ComponentFactory.getComponetClass(ele.mTagName, action.attributes);
                    ComponentDataHolder dataHolder = ele.getComponentDataHolder();

                    if (ele.isComponentClassMatch(clazz)) {
                        dataHolder.bindAttrs(action.attributes);
                    } else {
                        VDomChangeAction newAction = new VDomChangeAction();
                        newAction.action = VDomChangeAction.ACTION_ADD;
                        newAction.pageId = action.pageId;
                        newAction.tagName = ele.mTagName;
                        newAction.vId = action.vId;
                        newAction.parentVId = action.parentVId;
                        newAction.index = parent.getChildren().indexOf(ele);
                        newAction.attributes.putAll(dataHolder.getAttrsDomData());
                        newAction.attributes.putAll(action.attributes);
                        newAction.styles.putAll(dataHolder.getStyleDomData());
                        newAction.styles.putAll(action.styles);
                        newAction.events.addAll(dataHolder.getDomEvents());
                        newAction.events.addAll(action.events);
                        newAction.children.addAll(action.children);
                        parent.removeChild(ele);
                        addElement(hapEngine, context, newAction, doc, renderEventCallback, parent);
                    }
                } else {
                    ele.getComponentDataHolder().bindAttrs(action.attributes);
                }

                if (ele.getComponent() != null &&
                        ele.getComponent().getHook() != null &&
                        ele.getComponent().getHook().contains("update")) {
                    CallBackJsUtils.getInstance().callBackJs(jsThread, action.pageId, CallBackJsUtils.TYPE_NODE_UPDATE, action.vId);
                }
                break;
            }
            case VDomChangeAction.ACTION_UPDATE_TITLE_BAR: {
                DocComponent docComponent = (DocComponent) doc.getComponent();
                docComponent.updateTitleBar(action.titles, action.pageId);
                break;
            }
            case VDomChangeAction.ACTION_EXIT_FULLSCREEN: {
                DocComponent docComponent = (DocComponent) doc.getComponent();
                docComponent.exitFullscreen();
                break;
            }
            case VDomChangeAction.ACTION_UPDATE_STATUS_BAR: {
                DocComponent docComponent = (DocComponent) doc.getComponent();
                docComponent.updateStatusBar(action.status, action.pageId);
                break;
            }
            case VDomChangeAction.ACTION_STATISTICS: {
                recordStatistics(doc, action);
                break;
            }
            case VDomChangeAction.ACTION_PAGE_SCROLL: {
                DocComponent docComponent = doc.getComponent();
                docComponent.scrollPage(hapEngine, action.scrolls, action.pageId);
                break;
            }
            case VDomChangeAction.ACTION_HIDE_SKELETON: {
                doc.setCpHideSkeletonFlag(true);
                RootView.onHandleSkeletonHide(SkeletonProvider.HIDE_SOURCE_CP, doc);
                break;
            }
            case VDomChangeAction.ACTION_SET_SECURE: {
                DocComponent docComponent = doc.getComponent();
                docComponent.setSecure(action.pageId, action.isSecure);
                break;
            }
            default:
                throw new IllegalArgumentException("Unsupported action:" + action.action);
        }
        InspectorManager.getInspector().onAppliedChangeAction(context, jsThread, action);
    }

    private VElement createBody(
            HapEngine hapEngine,
            Context context,
            VDocument doc,
            RenderEventCallback renderEventCallback) {
        VDomChangeAction bodyAction = new VDomChangeAction();
        bodyAction.tagName = "body";
        bodyAction.vId = VElement.ID_BODY;
        Container bodyView =
                (Container)
                        generateComponent(
                                hapEngine, context, bodyAction, doc.getComponent(),
                                renderEventCallback, null);
        VElement bodyEle = new VGroup(doc, VElement.ID_BODY, VElement.TAG_BODY, bodyView);
        doc.addChild(bodyEle);
        return bodyEle;
    }

    private void addElement(
            HapEngine hapEngine,
            Context context,
            VDomChangeAction action,
            VDocument doc,
            RenderEventCallback renderEventCallback,
            VGroup parent) {
        VElement ele = createElement(hapEngine, context, action, doc, renderEventCallback, parent);
        parent.addChild(ele, action.index);

        final int N = action.children.size();
        for (int i = 0; i < N; i++) {
            addElement(
                    hapEngine, context, action.children.get(i), doc, renderEventCallback,
                    (VGroup) ele);
        }
    }

    public VElement createElement(
            HapEngine hapEngine,
            Context context,
            VDomChangeAction action,
            VDocument doc,
            RenderEventCallback renderEventCallback,
            VGroup parent) {
        if (parent.getComponentDataHolder() instanceof Container.RecyclerItem) {
            RecyclerDataItem recyclerItem =
                    generateRecyclerItem(hapEngine, context, action, renderEventCallback);
            return createElement(action, doc, recyclerItem);
        }

        Component newComponent =
                generateComponent(
                        hapEngine,
                        context,
                        action,
                        (Container) parent.getComponent(),
                        renderEventCallback,
                        null);

        if (newComponent instanceof Recycler) {
            mRecyclerDataItemCreator = ((Recycler) newComponent).getRecyclerDataItemCreator();
            RecyclerDataItem recyclerDataItem =
                    generateRecyclerItem(hapEngine, context, action, renderEventCallback);
            action.inspectorVElementType = InspectorVElementType.VGROUP;
            return new VGroup(
                    doc,
                    action.vId,
                    action.tagName,
                    (Recycler) newComponent,
                    (Container.RecyclerItem) recyclerDataItem);
        }

        return createElement(action, doc, newComponent);
    }

    private VElement createElement(VDomChangeAction action, VDocument doc, Component component) {
        if (component instanceof Container) {
            action.inspectorVElementType = InspectorVElementType.VGROUP;
            return new VGroup(doc, action.vId, action.tagName, component);
        } else {
            action.inspectorVElementType = InspectorVElementType.VELEMENT;
            return new VElement(doc, action.vId, action.tagName, component);
        }
    }

    private VElement createElement(
            VDomChangeAction action, VDocument doc, RecyclerDataItem recyclerDataItem) {
        if (recyclerDataItem instanceof Container.RecyclerItem) {
            action.inspectorVElementType = InspectorVElementType.VGROUP;
            return new VGroup(doc, action.vId, action.tagName, recyclerDataItem);
        } else {
            action.inspectorVElementType = InspectorVElementType.VELEMENT;
            return new VElement(doc, action.vId, action.tagName, recyclerDataItem);
        }
    }

    private RecyclerDataItem generateRecyclerItem(
            HapEngine hapEngine,
            Context context,
            VDomChangeAction action,
            RenderEventCallback renderEventCallback) {
        RecyclerDataItem recyclerItem =
                mRecyclerDataItemCreator.createRecyclerItem(
                        hapEngine, context, action.tagName, action.vId, renderEventCallback,
                        action.attributes);
        recyclerItem.bindAttrs(action.attributes);
        recyclerItem.bindStyles(action.styles);
        recyclerItem.bindEvents(action.events);
        if (action.getNode() != null) {
            recyclerItem.setCssNode(action.getNode());
            action.getNode().setRecyclerDataItem(recyclerItem);
        }
        return recyclerItem;
    }

    private void updateStyles(VDocument doc, VDomChangeAction action) {
        VElement ele = doc.getElementById(action.vId);
        if (ele == null) {
            Log.e(TAG, "ele is null, " + action);
            return;
        }
        ele.getComponentDataHolder().bindStyles(action.styles);

        for (VDomChangeAction child : action.children) {
            updateStyles(doc, child);
        }
    }

    /**
     * js 侧发送统计数据，目前仅在卡片模式下统计 click 事件
     */
    private void recordStatistics(VDocument doc, VDomChangeAction action) {
        if (!CardLogManager.hasListener()) {
            return;
        }
        Map<String, Object> extra = action.extra;
        Object type = extra.get("type"); // 统计时间类型
        if (!"click".equals(type)) {
            return;
        }
        Object listeners = extra.get("listeners"); // 处理了该事件的所有 view
        if (listeners instanceof JSONArray) {
            for (int i = 0; i < ((JSONArray) listeners).length(); i++) {
                Object ref = ((JSONArray) listeners).opt(i);
                if (ref instanceof String) {
                    int vid = Integer.parseInt((String) ref);
                    VElement ele = doc.getElementById(vid);
                    if (ele != null) {
                        Component component = ele.getComponent();
                        DocComponent rootComponent = component.getRootComponent();
                        if (rootComponent != null) {
                            RootView rootView = (RootView) rootComponent.getHostView();
                            View view = component.getHostView();
                            CardLogManager.logClickEvent(rootView.getUrl(), ele.getTagName(), view);
                        }
                    }
                }
            }
        }
    }
}

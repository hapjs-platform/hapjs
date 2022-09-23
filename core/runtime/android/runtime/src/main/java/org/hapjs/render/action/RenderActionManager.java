/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.action;

import android.util.Log;
import android.util.SparseArray;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import org.hapjs.common.executors.Executors;
import org.hapjs.common.json.JSONArray;
import org.hapjs.common.json.JSONObject;
import org.hapjs.logging.RuntimeLogManager;
import org.hapjs.render.ComponentAction;
import org.hapjs.render.Page;
import org.hapjs.render.RenderAction;
import org.hapjs.render.RenderActionPackage;
import org.hapjs.render.RootView;
import org.hapjs.render.VDomChangeAction;
import org.hapjs.render.css.CSSCalculator;
import org.hapjs.render.css.CSSRuleList;
import org.hapjs.render.css.CSSStyleDeclaration;
import org.hapjs.render.css.CSSStyleSheet;
import org.hapjs.render.css.MatchedCSSStyleSheet;
import org.hapjs.render.css.Node;
import org.hapjs.render.css.media.MediaPropertyInfo;
import org.hapjs.render.css.media.MediaPropertyInfoImpl;
import org.hapjs.render.jsruntime.JsBridge;
import org.json.JSONException;

/**
 * 主要处理 js 传来的 action，此处通过 RenderActionParser 构建 Action 对象，再将其传给 RootView
 */
public class RenderActionManager {
    private static final String TAG = "RenderActionManager";

    private RenderActionThread mRenderActionThread;
    private JsBridge.JsBridgeCallback mCallback;
    private String mPackage;

    private SparseArray<RenderActionDocument> mPages = new SparseArray<>();

    private RenderWorker mLastWorker;

    public RenderActionManager() {
        mRenderActionThread = new RenderActionThread();
    }

    public void attach(JsBridge.JsBridgeCallback callback) {
        mCallback = callback;
        if (mCallback instanceof RootView) {
            mPackage = ((RootView) mCallback).getPackage();
        }
    }

    public void post(Runnable runnable) {
        mRenderActionThread.post(runnable);
    }

    public void callNative(int pageId, String argsString) {
        RenderActionDocument document = mPages.get(pageId);
        if (document == null) {
            document = new RenderActionDocument(pageId);
            mPages.put(pageId, document);
        }

        RenderWorker worker = new RenderWorker(pageId, argsString, document, mLastWorker);
        mLastWorker = worker;
        Executors.io().execute(worker); // start render actions
    }

    public void sendRenderActions(RenderActionPackage renderActionPackage) {
        mCallback.onSendRenderActions(renderActionPackage);
    }

    private VDomChangeAction getDomChangeAction(
            int pageId, String module, String method, JSONArray args, RenderActionDocument document)
            throws JSONException {
        switch (module) {
            case "dom":
                return RenderActionParser
                        .objToChangeActions(pageId, method, args, document, mPackage);
            default:
                Log.e(TAG, "Unsupported callNative module:" + module);
                break;
        }
        return null;
    }

    // For inspector
    public CSSStyleDeclaration processInspectorCSSStyleDeclaration(
            CSSStyleDeclaration declaration) {
        return declaration;
    }

    /**
     * 调试器修改样式之后, 对样式表和节点样式进行更新
     *
     * @param pageId      页面
     * @param elementId   节点
     * @param ruleName    css规则名 如 "div #myId"
     * @param declaration 修改后的样式
     */
    public void setStyleFromInspector(
            int pageId, int elementId, String ruleName, CSSStyleDeclaration declaration) {
        if (Thread.currentThread() != mRenderActionThread) {
            throw new IllegalStateException("Call must RenderActionThread");
        }

        RenderActionNode node = mPages.get(pageId).findOrCreateNode(elementId, null);
        if ("INLINE".equals(ruleName)) {
            node.getInlineStyle().setDeclaration(declaration);
        } else {
            MatchedCSSStyleSheet ss = node.getMatchedStyleSheet();
            // TODO inspector update doc level stylesheet
            if (ss != null && ss.getNodeCSSStyleSheet() != null) {
                ss.getNodeCSSStyleSheet().setStyleFromInspector(ruleName, declaration);
            }
        }

        VDomChangeAction action = new VDomChangeAction();
        action.action = VDomChangeAction.ACTION_UPDATE_STYLE;
        action.vId = elementId;

        RenderActionParser.updateStyles(node, action);

        RenderActionPackage renderActionPackage = new RenderActionPackage(pageId);
        renderActionPackage.renderActionList.add(action);
        sendRenderActions(renderActionPackage);
    }

    /**
     * MediaPropertyInfo更新, 触发样式的重新计算
     *
     * @param info
     */
    public void onMediaPropertyInfoChanged(Page page, MediaPropertyInfo info) {
        if (Thread.currentThread() != mRenderActionThread) {
            throw new IllegalStateException("onMediaPropertyInfoChanged");
        }
        if (page == null) {
            return;
        }

        RenderActionDocument actionDocument = mPages.get(page.pageId);
        if (actionDocument == null) {
            return;
        }

        updateMediaQueryRules(actionDocument, info);
    }

    private void updateMediaQueryRules(RenderActionDocument document, MediaPropertyInfo info) {
        SparseArray<CSSStyleSheet> ssArray = document.getCSSStyleSheets();
        Set<RenderActionNode> nodes = null;

        for (int i = 0; i < ssArray.size(); i++) {
            CSSStyleSheet ss = ssArray.valueAt(i);
            List<CSSRuleList> cssRuleLists = ss.updateMediaPropertyInfo(info);

            if (cssRuleLists == null) {
                continue;
            }

            if (nodes == null) {
                nodes = new HashSet<>();
            }
            //SynchronizedSet 在增强for循环中是非线程安全的，需要同步
            synchronized (ss.getOwners()){
                for (Node node: ss.getOwners()) {
                    getCSSRuleMatchedNodes((RenderActionNode) node, cssRuleLists, nodes);
                }
            }
        }

        if (nodes != null) {
            updateStyles(document, nodes);
        }
    }

    private void getCSSRuleMatchedNodes(
            RenderActionNode node, List<CSSRuleList> cssRuleLists, Set<RenderActionNode> nodes) {
        // match current node.
        if (CSSCalculator.match(cssRuleLists, node)) {
            node.setRestyling(true);
            nodes.add(node);
        }

        // match child node.
        List<RenderActionNode> children = node.getChildren();
        synchronized (children) {
            for (RenderActionNode child : children) {
                getCSSRuleMatchedNodes(child, cssRuleLists, nodes);
            }
        }
    }

    private void updateStyles(RenderActionDocument document, Set<RenderActionNode> nodes) {
        RenderActionPackage renderActionPackage = new RenderActionPackage(document.getPageId());
        for (RenderActionNode node : nodes) {
            VDomChangeAction action = new VDomChangeAction();
            action.action = VDomChangeAction.ACTION_UPDATE_STYLE;
            action.vId = node.getVId();

            RenderActionParser.updateStyles(node, action);

            renderActionPackage.renderActionList.add(action);
        }

        sendRenderActions(renderActionPackage);
    }

    public void destroyPage(final int pageId) {
        post(
                new Runnable() {
                    @Override
                    public void run() {
                        mPages.remove(pageId);
                    }
                });
    }

    public void release() {
        mRenderActionThread.doShutdown();
    }

    public RenderActionDocument getOrCreateDocument(int pageId) {
        RenderActionDocument document = mPages.get(pageId);
        if (document == null) {
            document = new RenderActionDocument(pageId);
            mPages.put(pageId, document);
        }
        return document;
    }

    public void updateMediaPropertyInfo(final Page page) {
        post(
                new Runnable() {
                    @Override
                    public void run() {
                        onMediaPropertyInfoChanged(page, new MediaPropertyInfoImpl());
                    }
                });
    }

    public void showSkeleton(String packageName, org.json.JSONObject parseResult) {
        mCallback.onRenderSkeleton(packageName, parseResult);
    }

    public class RenderWorker implements Runnable {

        private RenderWorker mPreWorker;
        private int mPageId;
        private String mArgsString;
        private RenderActionDocument mDocument;
        private CountDownLatch mLatch;

        public RenderWorker(
                int pageId, String argsString, RenderActionDocument document,
                RenderWorker preWorker) {
            mPageId = pageId;
            mArgsString = argsString;
            mDocument = document;
            mPreWorker = preWorker;

            mLatch = new CountDownLatch(1);
        }

        @Override
        public void run() {
            try {
                RuntimeLogManager.getDefault().logRenderTaskStart(mPackage, "renderActions");
                renderActions();
                RuntimeLogManager.getDefault().logRenderTaskEnd(mPackage, "renderActions");
            } catch (JSONException e) {
                Log.e(TAG, "render worker error", e);
                e.printStackTrace();
            }
        }

        private void renderActions() throws JSONException {

            int pageId = mPageId;
            RenderActionDocument document = mDocument;

            RenderActionPackage renderActionPackage = new RenderActionPackage(mPageId);
            JSONArray args = new JSONArray(mArgsString);
            final int N = args.length();
            for (int i = 0; i < N; i++) {
                RenderAction action = null;
                JSONObject param = args.getJSONObject(i);
                JSONArray argsArray = param.getJSONArray("args");
                if (param.has("module")) {
                    action =
                            getDomChangeAction(
                                    pageId,
                                    param.getString("module"),
                                    param.getString("method"),
                                    argsArray,
                                    document);
                } else if (param.has("component")) {
                    action =
                            RenderActionParser.objToComponentAction(
                                    param.getString("component"),
                                    param.getString("ref"),
                                    param.getString("method"),
                                    argsArray);
                }

                if (action != null) {
                    if (action instanceof ComponentAction) {
                        sendActions(
                                renderActionPackage,
                                false); // ComponentAction(eg: 'focus') should be called after View created, so send
                        // it now
                        renderActionPackage = new RenderActionPackage(pageId);
                    }
                    renderActionPackage.renderActionList.add(action);
                }
            }

            sendActions(renderActionPackage, true);
        }

        private void sendActions(RenderActionPackage renderActionPackage, boolean isCountDown) {
            if (renderActionPackage.renderActionList.size() <= 0) {
                if (isCountDown) {
                    mLatch.countDown();
                    mDocument = null;
                }
                return;
            }
            // 等待之前的 package 发送完毕再发送
            if (mPreWorker != null) {
                try {
                    mPreWorker.mLatch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    mPreWorker = null;
                }
            }

            // 之前有的 node 没有父节点, 如果存在后代选择器, 需要重新计算
            checkDirty(renderActionPackage);

            sendRenderActions(renderActionPackage);
            if (isCountDown) {
                mLatch.countDown();
                mDocument = null;
            }
        }

        private void checkDirty(RenderActionPackage actionPackage) {
            List<RenderAction> actions = actionPackage.renderActionList;
            for (RenderAction renderAction : actions) {
                if (!(renderAction instanceof VDomChangeAction)) {
                    continue;
                }
                VDomChangeAction action = (VDomChangeAction) renderAction;
                RenderActionNode node = mDocument.findNodeById(action.vId);
                if (node == null) {
                    continue;
                }
                if (!node.isDirty()) {
                    continue;
                }
                node.setDirty(false);

                // node is dirty, 重新计算
                RenderActionParser.updateParent(mDocument, node, action);
                RenderActionParser.updateStyles(node, action);
            }
        }
    }
}

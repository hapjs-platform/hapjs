/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.action;

import android.text.TextUtils;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hapjs.component.Component;
import org.hapjs.component.RecyclerDataItem;
import org.hapjs.render.css.CSSStyleSheet;

public class RenderActionDocument extends RenderActionNode {

    private SparseArray<RenderActionNode> mNodes = new SparseArray<>();
    private SparseArray<CSSStyleSheet> mCSSStyleSheets = new SparseArray<>();
    private int mPageId;

    // docLevel 级别的样式表, 对所有节点生效.
    private List<CSSStyleSheet> mDocLevelStyleSheets = new ArrayList<>();

    RenderActionDocument(int pageId) {
        super(null, "document", 0);
        mPageId = pageId;
    }

    int getPageId() {
        return mPageId;
    }

    RenderActionNode findOrCreateNode(int id) {
        return findOrCreateNode(id, null);
    }

    synchronized RenderActionNode findOrCreateNode(int id, String tagName) {
        RenderActionNode node = mNodes.get(id);
        if (node == null) {
            node = new RenderActionNode(this, tagName, id);
            mNodes.put(id, node);
        }
        if (!TextUtils.isEmpty(tagName)) {
            node.setTagName(tagName);
        }
        return node;
    }

    synchronized RenderActionNode findNodeById(int id) {
        return mNodes.get(id);
    }

    void removeNode(int id) {
        removeNode(findNodeById(id));
    }

    synchronized void removeNode(RenderActionNode node) {
        if (node == null) {
            return;
        }

        if (node.getParent() != null) {
            node.getParent().removeChild(node);
        }
        removeNodeInternal(node);
    }

    private void removeNodeInternal(RenderActionNode node) {
        if (node == null) {
            return;
        }
        mNodes.remove(node.getVId());

        clearNodeReference(node);

        synchronized (node.getChildren()) {
            Iterator<RenderActionNode> iterator = node.getChildren().iterator();
            while (iterator.hasNext()) {
                RenderActionNode child = iterator.next();
                removeNodeInternal(child);
                iterator.remove();
            }
        }
    }

    private void clearNodeReference(RenderActionNode node) {
        //remove node from component.mCssNode
        Component component = node.getComponent();
        if (component != null) {
            component.setCssNode(null);
        }
        node.setComponent(null);

        //remove node from recyclerDataItem.mCssNode
        RecyclerDataItem recyclerDataItem = node.getRecyclerDataItem();
        if (recyclerDataItem != null) {
            recyclerDataItem.setCssNode(null);
        }
        node.setRecyclerDataItem(null);

        //remove node from set cssStyleSheet.mOwners
        CSSStyleSheet cssStyleSheet = mCSSStyleSheets.get(node.getStyleObjectId());
        if (cssStyleSheet != null) {
            cssStyleSheet.removeOwner(node);
        }

        node.setParent(null);
    }

    synchronized CSSStyleSheet findStyleSheetById(int styleObjectId) {
        return mCSSStyleSheets.get(styleObjectId);
    }

    public synchronized void registerStyleSheet(int id, CSSStyleSheet cssStyleSheet) {
        mCSSStyleSheets.put(id, cssStyleSheet);
    }

    public synchronized SparseArray<CSSStyleSheet> getCSSStyleSheets() {
        return mCSSStyleSheets;
    }

    public synchronized void registerDocLevelStyleSheet(int id, CSSStyleSheet cssStyleSheet) {
        mDocLevelStyleSheets.add(cssStyleSheet);
        mCSSStyleSheets.put(id, cssStyleSheet);
    }

    synchronized List<CSSStyleSheet> getDocStyleSheet() {
        return mDocLevelStyleSheets;
    }
}

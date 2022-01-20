/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.action;

import java.util.HashMap;
import org.hapjs.render.css.CSSStyleSheet;

public class RenderActionDocumentMock extends RenderActionDocument {
    private HashMap<Integer, RenderActionNode> mNodes = new HashMap<>();
    private HashMap<Integer, CSSStyleSheet> mCSSStyleSheets = new HashMap<>();

    public RenderActionDocumentMock(int pageId) {
        super(pageId);
    }

    public RenderActionNode createNode(int id, String tagName) {
        RenderActionNode node = new RenderActionNode(this, tagName, id);
        mNodes.put(id, node);
        return node;
    }

    @Override
    RenderActionNode findNodeById(int id) {
        return mNodes.get(id);
    }

    @Override
    CSSStyleSheet findStyleSheetById(int styleObjectId) {
        return mCSSStyleSheets.get(styleObjectId);
    }

    @Override
    public void registerStyleSheet(int id, CSSStyleSheet cssStyleSheet) {
        mCSSStyleSheets.put(id, cssStyleSheet);
    }
}

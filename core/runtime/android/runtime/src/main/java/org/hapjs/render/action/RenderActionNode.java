/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.action;

import android.text.TextUtils;
import java.util.List;
import java.util.Map;
import org.hapjs.render.css.CSSStyleSheet;
import org.hapjs.render.css.MatchedCSSStyleSheet;
import org.hapjs.render.css.Node;

public class RenderActionNode extends Node {

    private final RenderActionDocument mDocument;
    private boolean mUseParentStyle = false;
    private int mStyleObjectId = 0; // default same with JS framework
    private int mVId;

    RenderActionNode(RenderActionDocument document, String tagName, int id) {
        super(tagName);
        mDocument = document;
        mVId = id;
    }

    public int getVId() {
        return mVId;
    }

    synchronized int getStyleObjectId() {
        return mStyleObjectId;
    }

    public synchronized void setStyleObjectId(int styleObjectId) {
        mStyleObjectId = styleObjectId;
    }

    synchronized void setUseParentStyle(boolean useParentStyle) {
        mUseParentStyle = useParentStyle;
    }

    void updateCSSAttrs(Map<String, Object> attributes) {
        Object classPlain = attributes.get("class");
        if (classPlain != null) {
            classPlain =
                    (String.valueOf(classPlain))
                            .replace("@appRootElement", ""); // TODO FixBug @appRootElement
            setCSSClass(String.valueOf(classPlain));
        }

        Object idPlain = attributes.get("id");
        if (idPlain != null) {
            setCSSId(String.valueOf(idPlain)); // idPlain maybe Integer
        }
        Object restylingPlain = attributes.get("descrestyling");
        if (restylingPlain == null) {
            // 兼容 desc-restyling 的写法(前端框架会将 desc-restyling 转为 descRestyling)
            restylingPlain = attributes.get("descRestyling");
        }
        if (restylingPlain != null
                && TextUtils.equals(restylingPlain.toString().toLowerCase(), "false")) {
            setRestyling(false);
        } else if (restylingPlain != null) {
            setRestyling(true);
        } else {
            // do nothing when restylingPlain is null
            // updateStyle 时也会调用此方法更新属性,但只传入了更改的属性,因此未更改属性应保持不变
        }
    }

    @Override
    public RenderActionNode getParent() {
        if (mDocument != null) {
            synchronized (mDocument) {
                return (RenderActionNode) super.getParent();
            }
        } else {
            // current is document
            synchronized (this) {
                return (RenderActionNode) super.getParent();
            }
        }
    }

    @Override
    public List<RenderActionNode> getChildren() {
        return (List<RenderActionNode>) super.getChildren();
    }

    /**
     * @return 当前节点匹配的样式表, 包括 docLevel 的样式表, 节点样式表
     */
    @Override
    public MatchedCSSStyleSheet getMatchedStyleSheet() {
        MatchedCSSStyleSheet matchedCSSStyleSheet = new MatchedCSSStyleSheet();
        matchedCSSStyleSheet.setNodeCSSStyleSheet(getMatchedNodeStyleSheet());
        matchedCSSStyleSheet.setDocLevelCSSStyleSheet(mDocument.getDocStyleSheet());
        return matchedCSSStyleSheet;
    }

    /**
     * @return 当前节点匹配的 节点样式表, 节点样式表只对其子节点生效
     */
    private CSSStyleSheet getMatchedNodeStyleSheet() {
        RenderActionNode curr = this;
        if (curr.mUseParentStyle) {
            curr = curr.getParent();
        }
        while (curr != null && curr.mStyleObjectId == 0) {
            curr = curr.getParent();
        }
        if (curr == null) {
            setDirty(true);
            return null;
        }
        CSSStyleSheet styleSheet = mDocument.findStyleSheetById(curr.mStyleObjectId);
        if (styleSheet == null) {
            setDirty(true);
            return null;
        }
        return styleSheet;
    }

    public RenderActionDocument getDocument() {
        return mDocument;
    }
}

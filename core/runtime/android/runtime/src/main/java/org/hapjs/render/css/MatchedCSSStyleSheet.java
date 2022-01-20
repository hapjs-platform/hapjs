/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.css;

import java.util.List;

public class MatchedCSSStyleSheet {
    // 节点样式表只对其子节点生效
    private CSSStyleSheet mNodeCSSStyleSheet;
    // docLevel 级别的样式表, 对所有节点生效.
    private List<CSSStyleSheet> mDocLevelCSSStyleSheet;

    public void setDocLevelCSSStyleSheet(List<CSSStyleSheet> docLevelCSSStyleSheet) {
        mDocLevelCSSStyleSheet = docLevelCSSStyleSheet;
    }

    public CSSStyleSheet getNodeCSSStyleSheet() {
        return mNodeCSSStyleSheet;
    }

    public void setNodeCSSStyleSheet(CSSStyleSheet nodeCSSStyleSheet) {
        mNodeCSSStyleSheet = nodeCSSStyleSheet;
    }

    // index 越大, 样式表优先级越高
    public CSSStyleSheet get(int index) {
        if (index < 0 || index >= size()) {
            throw new IndexOutOfBoundsException("" + index);
        }
        if (index < mDocLevelCSSStyleSheet.size()) {
            return mDocLevelCSSStyleSheet.get(index);
        }

        return mNodeCSSStyleSheet;
    }

    public int size() {
        int size = 0;
        if (mDocLevelCSSStyleSheet != null) {
            size += mDocLevelCSSStyleSheet.size();
        }
        if (mNodeCSSStyleSheet != null) {
            size++;
        }
        return size;
    }

    Object getKeyframes(String animationName) {
        Object result = CSSProperty.UNDEFINED;

        for (int i = 0; i < size(); i++) {
            final CSSStyleSheet cssStyleSheet = get(i);
            if (cssStyleSheet == null) {
                continue;
            }
            final CSSKeyframesRule rule = cssStyleSheet.getCSSKeyframesRule();
            if (rule == null) {
                continue;
            }

            Object value = rule.getKeyframes(animationName);
            if (value != CSSProperty.UNDEFINED) {
                result = value;
            }
        }
        return result;
    }

    Object getFontFace(String fontName) {
        Object result = CSSProperty.UNDEFINED;

        for (int i = 0; i < size(); i++) {
            final CSSStyleSheet styleSheet = get(i);
            if (styleSheet == null) {
                continue;
            }

            final CSSFontFaceRule rule = styleSheet.getCSSFontFaceRule();
            if (rule == null) {
                continue;
            }

            Object value = rule.getFontFace(fontName);
            if (value != CSSProperty.UNDEFINED) {
                result = value;
            }
        }

        return result;
    }
}

/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.css;

import android.text.TextUtils;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.hapjs.common.json.JSONArray;
import org.hapjs.common.json.JSONObject;
import org.hapjs.render.css.media.CSSMediaParser;
import org.hapjs.render.css.media.MediaList;
import org.hapjs.render.css.property.CSSPropertyBuilder;
import org.json.JSONException;

public class CSSParser {
    private static final String TAG = "CSSParser";
    private static final String DESC_SELECTOR_REGEX = ".*\\s+.*";
    private static final String DESC_SELECTOR_SPLIT = "\\s+";

    private static final String CHILD_SELECTOR_REGEX = ".*\\s*>\\s*.*";
    private static final String CHILD_SELECTOR_SPLIT = "\\s*>\\s*";

    private static final String SELECTORS_SPLIT = "\\s*,\\s*";

    private static final String KEY_KEYFRAMES = "@KEYFRAMES";
    private static final String KEY_FONT_FACE = "@FONT-FACE";
    private static final String KEY_INFO = "@info";
    private static final String KEY_MEDIA = "@MEDIA";
    private static final String KEY_MEDIA_DESC = "condition";
    private static final String KEY_AT = "@";

    private static final String STYLE_OBJECT_ID = "styleObjectId";

    private static long sStyleSheetCount = 0L;
    private static long SCORE_ORDER_MEDIA_OFFSET = 100000L;

    public static CSSStyleSheet parseCSSStyleSheet(JSONObject plain) throws JSONException {
        sStyleSheetCount++;

        CSSStyleSheet ss = new CSSStyleSheet();
        CSSRuleList cssRuleList = parseCssRuleList(ss, plain, false, 0);
        ss.setCSSRules(cssRuleList);
        return ss;
    }

    /**
     * @param node        节点
     * @param inlineStyle 内联样式, 如 '{ "width" : "10px" }'
     * @return 解析后的内联样式
     */
    public static CSSStyleDeclaration parseInlineStyle(Node node, JSONObject inlineStyle)
            throws JSONException {
        CSSStyleDeclaration inlineDeclaration = parseCSSStyleDeclaration(inlineStyle);

        CSSCalculator.addExtraDeclaration(node, inlineDeclaration);
        return inlineDeclaration;
    }

    private static CSSStyleDeclaration parseCSSStyleDeclaration(JSONObject declaration)
            throws JSONException {
        CSSStyleDeclaration dec = new CSSStyleDeclaration();

        if (declaration != null) {
            Iterator<String> keys = declaration.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                if (key.startsWith("_")) { // _meta
                    continue;
                }

                CSSProperty prop =
                        new CSSPropertyBuilder().setNameWithState(key)
                                .setValue(declaration.get(key)).build();
                dec.setProperty(prop);
            }
        }

        return dec;
    }

    /**
     * @param ss             传入处理的样式表
     * @param plain
     * @param isCSSMediaRule 标志是否是CSSMediaList里所包含的CSSRule.如果是,赋予最高的优先级
     * @param order
     * @return 返回的是一个样式表里的一个CSSRuleList
     * @throws JSONException
     */
    private static CSSRuleList parseCssRuleList(
            CSSStyleSheet ss, JSONObject plain, boolean isCSSMediaRule, int order)
            throws JSONException {
        List<CSSRule> cssRules = new ArrayList<>();
        Iterator<String> keys = plain.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            if (TextUtils.isEmpty(key)) {
                continue;
            }

            if (KEY_KEYFRAMES.equals(key)) {
                JSONObject jsonObject = plain.getJSONObject(key);
                org.json.JSONObject realJsonObject = new org.json.JSONObject(jsonObject.toString());
                CSSKeyframesRule keyframesRule = new CSSKeyframesRule(realJsonObject);
                ss.setCSSKeyframesRule(keyframesRule);
                cssRules.add(keyframesRule);
                continue;
            }

            if (KEY_FONT_FACE.equals(key)) {
                JSONObject jsonObject = plain.getJSONObject(key);
                org.json.JSONObject realJsonObject = new org.json.JSONObject(jsonObject.toString());
                CSSFontFaceRule fontFaceRule = new CSSFontFaceRule(realJsonObject);
                ss.setCSSFontFaceRule(fontFaceRule);
                cssRules.add(fontFaceRule);
                continue;
            }

            if (KEY_MEDIA.equals(key)) {
                JSONArray jsonArray = plain.getJSONArray(key);
                List<CSSMediaRule> cssMediaRules = new ArrayList<>();
                for (int i = 0; i < jsonArray.length(); i++) {
                    CSSMediaRule cssMediaRule =
                            parseCSSMediaRule(ss, jsonArray.getJSONObject(i), ++order);
                    if (cssMediaRule != null) {
                        cssMediaRules.add(cssMediaRule);
                        cssRules.add(cssMediaRule);
                    }
                }
                ss.setCssMediaRules(cssMediaRules);
                continue;
            }

            if (KEY_INFO.equals(key)) {
                JSONObject jsonObject = plain.getJSONObject(key);
                org.json.JSONObject realJsonObject = new org.json.JSONObject(jsonObject.toString());
                int styleObjectId = realJsonObject.optInt(STYLE_OBJECT_ID);
                ss.setStyleObjectId(styleObjectId);
            }

            if (key.startsWith(KEY_AT)) { // @KEYFRAMES, @FONT-FACE ...
                continue;
            }

            CSSStyleDeclaration declaration = parseCSSStyleDeclaration(plain.getJSONObject(key));
            CSSStyleRule cssRule = new CSSStyleRule(ss, key, declaration);
            if (isCSSMediaRule) {
                cssRule.setOrder(sStyleSheetCount + order + SCORE_ORDER_MEDIA_OFFSET);
            } else {
                cssRule.setOrder(sStyleSheetCount + order);
            }
            ++order;
            cssRules.add(cssRule);
        }

        return new CSSRuleList(cssRules);
    }

    static Selector[] parseSelector(String selectorText) {
        selectorText = selectorText.trim();
        String[] selectorTexts;
        if (selectorText.contains(",")) {
            selectorTexts = selectorText.split(SELECTORS_SPLIT);
        } else {
            selectorTexts = new String[] {selectorText};
        }
        Selector[] selectors = new Selector[selectorTexts.length];
        for (int i = 0; i < selectorTexts.length; i++) {
            selectors[i] = parseSingleSelector(selectorTexts[i]);
        }

        return selectors;
    }

    static Selector parseSingleSelector(String plain) {
        plain = plain.trim();
        String[] csTexts;
        if (plain.contains(">")) {
            csTexts = plain.split(CHILD_SELECTOR_SPLIT);
        } else {
            csTexts = new String[] {plain};
        }
        Selector parentSelector = null;
        for (String csText : csTexts) {
            parentSelector = parseChildSelectorParent(parentSelector, csText);
        }
        return parentSelector;
    }

    private static Selector parseChildSelectorParent(Selector parentSelector, String descText) {
        String[] descTexts;
        if (descText.contains(" ")) {
            descTexts = descText.split(DESC_SELECTOR_SPLIT);
        } else {
            descTexts = new String[] {descText};
        }

        if (parentSelector == null) {
            parentSelector = parseSimpleSelector(descTexts[0]);
        } else {
            parentSelector =
                    SelectorFactory
                            .createChildSelector(parentSelector, parseSimpleSelector(descTexts[0]));
        }

        for (int i = 1; i < descTexts.length; i++) {
            parentSelector =
                    SelectorFactory.createDescendantSelector(
                            parentSelector, parseSimpleSelector(descTexts[i]));
        }

        return parentSelector;
    }

    private static SelectorFactory.SimpleSelector parseSimpleSelector(String plain) {
        String[] split = plain.split(":");
        if (split.length > 1) {
            if (split[0].startsWith("#")) {
                SelectorFactory.SimpleSelector idSelector =
                        SelectorFactory.createIdSelector(split[0].substring(1));
                return SelectorFactory.createStateSelector(idSelector, split[1]);
            }
            if (split[0].startsWith(".")) {
                SelectorFactory.SimpleSelector classSelector =
                        SelectorFactory.createClassSelector(split[0].substring(1));
                return SelectorFactory.createStateSelector(classSelector, split[1]);
            }
            SelectorFactory.SimpleSelector elementSelector =
                    SelectorFactory.createElementSelector(split[0]);
            return SelectorFactory.createStateSelector(elementSelector, split[1]);
        } else {
            if (plain.startsWith("#")) {
                return SelectorFactory.createIdSelector(plain.substring(1));
            }
            if (plain.startsWith(".")) {
                return SelectorFactory.createClassSelector(plain.substring(1));
            }
            return SelectorFactory.createElementSelector(plain);
        }
    }

    /**
     * @param ss
     * @param decl
     * @return 一条样式查询 condition 和对应的 CSSRuleList
     * @throws JSONException
     */
    static CSSMediaRule parseCSSMediaRule(CSSStyleSheet ss, JSONObject decl, int order)
            throws JSONException {
        Object condition = decl.opt(KEY_MEDIA_DESC);
        if (condition != null) {
            // 媒体查询 解析媒体查询condition语句
            MediaList mediaList = CSSMediaParser.parseMediaList((String) condition);
            // 在 整体json串 里面去掉 condition　封装成 CSSRule ; condition 另存为 medialist
            decl.remove(KEY_MEDIA_DESC);
            CSSRuleList cssRuleList = parseCssRuleList(ss, decl, true, order);
            return new CSSMediaRule(cssRuleList, mediaList);
        }
        return null;
    }
}

/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.css;

import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.hapjs.render.css.media.MediaPropertyInfoImpl;
import org.hapjs.render.css.property.CSSPropertyBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CSSCalculator {

    static final long ID_SCORE = 1000000L;
    static final long CLASS_SCORE = 1000L;
    static final long ELEMENT_SCORE = 1L;
    static final long SCORE_ORDER_OFFSET = 1000000L;

    private static final String TAG = "CSSCalculator";
    private static final String KEY_ANIMATION_NAME = "animationName";
    private static final String KEY_PAGE_ANIMATION_NAME = "pageAnimationName";
    private static final String KEY_ANIMATION_KEYFRAMES = "animationKeyframes";
    private static final String KEY_PAGE_ANIMATION_KEYFRAMES = "pageAnimationKeyframes";
    private static final String KEY_FONT_FAMILY = "fontFamily";
    private static final String KEY_FONT_FAMILY_DESC = "fontFamilyDesc";
    private static final String KEY_FONT_NAME = "fontName";

    /**
     * @param node          节点
     * @param matchedStyles 节点从样式表中匹配的样式
     * @return 合并后的最终样式
     */
    static CSSStyleDeclaration calFinalStyle(Node node, MatchedCSSRuleList matchedStyles) {
        CSSStyleDeclaration finalStyle = node.getFinalStyle();

        // change to empty value to override old value
        Iterator<String> cssPropertyIterator = finalStyle.iterator();
        while (cssPropertyIterator.hasNext()) {
            String entryKey = cssPropertyIterator.next();
            CSSProperty old = finalStyle.getProperty(entryKey);
            CSSProperty undefined =
                    new CSSPropertyBuilder(
                            old) // too many undefined value will be create TODO optimize: just set null
                            .setValue(CSSProperty.UNDEFINED)
                            .build();
            finalStyle.setProperty(undefined);
        }

        if (matchedStyles != null) {
            for (int i = 0; i < matchedStyles.length(); i++) {
                finalStyle.setAllProperty(matchedStyles.getCSSStyleRule(i).getDeclaration());
            }
        }

        finalStyle.setAllProperty(node.getInlineStyle().getDeclaration());

        // 添加 animationKeyframes, fontFamilyDesc
        addExtraDeclaration(node, finalStyle);
        return finalStyle;
    }

    static void addExtraDeclaration(Node node, CSSStyleDeclaration declaration) {
        MatchedCSSStyleSheet ss = node.getMatchedStyleSheet();
        CSSStyleDeclaration extraDeclaration = CSSCalculator.getExtraDeclaration(ss, declaration);
        declaration.setAllProperty(extraDeclaration);
    }

    /**
     * 根据样式表中的 @KEYFRAMES, @FONT-FACE 信息 查找 animationName, fontFamily 对应的 animationKeyframes,
     * fontFamilyDesc
     *
     * @return animationKeyframes, fontFamilyDesc
     */
    private static CSSStyleDeclaration getExtraDeclaration(
            MatchedCSSStyleSheet ss, CSSStyleDeclaration declaration) {
        if (ss == null || declaration == null) {
            return null;
        }

        CSSStyleDeclaration result = null;
        Iterator<String> cssPropertyIterator = declaration.iterator();
        while (cssPropertyIterator.hasNext()) {
            String entryKey = cssPropertyIterator.next();
            CSSProperty item = declaration.get(entryKey);
            CSSProperty cssProperty;

            if (KEY_ANIMATION_NAME.equals(item.getNameWithoutState())) {

                Object keyframes = getKeyframes(ss, (String) item.getValue());

                cssProperty =
                        new CSSPropertyBuilder(item)
                                .setNameWithoutState(KEY_ANIMATION_KEYFRAMES)
                                .setValue(keyframes)
                                .build();

            } else if (KEY_PAGE_ANIMATION_NAME.equals(item.getNameWithoutState())) {
                Object keyframes = getKeyframes(ss, (String) item.getValue());

                cssProperty =
                        new CSSPropertyBuilder(item)
                                .setNameWithoutState(KEY_PAGE_ANIMATION_KEYFRAMES)
                                .setValue(keyframes)
                                .build();
            } else if (KEY_FONT_FAMILY.equals(item.getNameWithoutState())) {

                Object fontFaces = getFontFaces(ss, (String) item.getValue());
                cssProperty =
                        new CSSPropertyBuilder(item)
                                .setNameWithoutState(KEY_FONT_FAMILY_DESC)
                                .setValue(fontFaces)
                                .build();

            } else {
                continue;
            }

            if (result == null) {
                result = new CSSStyleDeclaration();
            }
            result.setProperty(cssProperty);
        }

        return result;
    }

    private static String clearRedundantQuotationMark(String src) {
        if (TextUtils.isEmpty(src)) {
            return CSSProperty.UNDEFINED;
        }
        if ((src.startsWith("'") && src.endsWith("'"))
                || (src.startsWith("\"") && src.endsWith("\""))) {
            return src.substring(1, src.length() - 1);
        }
        return src;
    }

    /**
     * @param fontFamily 以 ',' 分隔的一组字体 如 ['sans', 'serif']
     * @return 字体详细路径信息
     */
    private static Object getFontFaces(MatchedCSSStyleSheet ss, String fontFamily) {
        String fontFamilyWithoutQuotation = clearRedundantQuotationMark(fontFamily).trim();
        String[] fontFamilies;
        if (fontFamilyWithoutQuotation.contains(",")) {
            fontFamilies = fontFamilyWithoutQuotation.split(",");
        } else {
            fontFamilies = new String[] {fontFamilyWithoutQuotation};
        }
        return getFontFaces(ss, fontFamilies);
    }

    private static Object getFontFaces(MatchedCSSStyleSheet ss, String[] fontNames) {
        JSONArray result = new JSONArray();

        for (String fontName : fontNames) {
            fontName = fontName.trim();
            if (TextUtils.isEmpty(fontName)) {
                continue;
            }

            Object value = ss.getFontFace(fontName);
            if (value == CSSProperty.UNDEFINED) {
                value = generateDefaultFontFace(fontName); // 所有样式表都不包含, 构造一个默认值
            }
            result.put(value);
        }
        return result;
    }

    private static JSONObject generateDefaultFontFace(String fontName) {
        JSONObject font = new JSONObject();
        try {
            font.put(KEY_FONT_NAME, fontName);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return font;
    }

    /**
     * @param animationName 逗号分隔的一组动画名，如 Color, Width
     * @return 动画详细信息
     */
    private static Object getKeyframes(MatchedCSSStyleSheet ss, String animationName) {
        String animationNameClearance = clearRedundantQuotationMark(animationName).trim();
        String[] animationNames;
        if (animationNameClearance.contains(",")) {
            animationNames = animationNameClearance.split(",");
        } else {
            animationNames = new String[] {animationNameClearance};
        }
        JSONArray result = new JSONArray();
        for (String name : animationNames) {
            name = name.trim();
            if (TextUtils.isEmpty(name)) {
                continue;
            }
            Object value = ss.getKeyframes(name);
            if (value instanceof JSONArray) {
                extractJsonArray((JSONArray) value, name, result);
            }
        }
        return result;
    }

    /**
     * 把节点的selector和样式表的selector进行比较.当值一致时后续会被调用,则加入MatchedCssStyleSheet
     *
     * @param node 节点
     * @return 从样式表中匹配出的样式
     */
    static MatchedCSSRuleList calMatchedStyles(Node node) {
        MatchedCSSStyleSheet ss = node.getMatchedStyleSheet();
        if (ss == null) {
            return null;
        }

        List<CSSStyleRule> matchedStyleRuleList = null;

        for (int i = 0; i < ss.size(); i++) {
            final CSSStyleSheet cssStyleSheet = ss.get(i);
            if (cssStyleSheet == null) {
                continue;
            }

            final CSSRuleList cssRuleList = cssStyleSheet.getCSSRuleList();
            if (cssRuleList == null) {
                continue;
            }
            for (int j = 0; j < cssRuleList.length(); j++) {
                CSSRule rule = cssRuleList.item(j);
                // deal media rule  to  matchedStyleRuleList
                if (rule.getType() == CSSRule.MEDIA_RULE) {
                    CSSMediaRule cssMediaRule = (CSSMediaRule) rule;
                    // 更新媒体查询中的媒体属性
                    cssMediaRule.getMediaList()
                            .updateMediaPropertyInfo(new MediaPropertyInfoImpl());
                    // 判断是否符合媒体查询
                    if (cssMediaRule.getCssRuleList() != null
                            && cssMediaRule.getCssRuleList().getCssRules() != null
                            && cssMediaRule.getMediaList().getResult()) {
                        for (CSSRule cssMediarule : cssMediaRule.getCssRuleList().getCssRules()) {
                            CSSStyleRule cssMediaStyleRule = (CSSStyleRule) cssMediarule;
                            if (!match(cssMediaStyleRule, node)) {
                                continue;
                            }
                            if (matchedStyleRuleList == null) {
                                matchedStyleRuleList = new ArrayList<>();
                            }
                            matchedStyleRuleList.add(cssMediaStyleRule);
                        }
                    }
                }

                if (rule.getType() != CSSRule.STYLE_RULE) {
                    continue;
                }

                CSSStyleRule cssRule = (CSSStyleRule) rule;
                if (!match(cssRule, node)) {
                    continue;
                }

                if (matchedStyleRuleList == null) {
                    matchedStyleRuleList = new ArrayList<>();
                }

                matchedStyleRuleList.add(cssRule);
            }
        }

        return new MatchedCSSRuleList(matchedStyleRuleList, node);
    }

    public static boolean match(List<CSSRuleList> cssRuleLists, Node node) {
        for (CSSRuleList cssRuleList : cssRuleLists) {
            for (int i = 0; i < cssRuleList.length(); i++) {

                if (cssRuleList.item(i).getType() == CSSRule.STYLE_RULE) {
                    CSSStyleRule cssRule = (CSSStyleRule) cssRuleList.item(i);
                    if (match(cssRule, node)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 样式表中的selector和node的selector相比较
     *
     * @param cssStyleRule
     * @param node
     * @return　是否相同
     */
    private static boolean match(CSSStyleRule cssStyleRule, Node node) {
        Selector[] selectors = cssStyleRule.getSelectors();
        for (Selector selector : selectors) {
            if (selector.match(cssStyleRule, node, node)) {
                return true;
            }
        }
        return false;
    }

    static long calculateScore(CSSStyleRule cssStyleRule, Node node) {
        Selector[] selectors = cssStyleRule.getSelectors();

        long maxScore = 0;
        for (Selector selector : selectors) {
            if (selector.match(cssStyleRule, node, node) && selector.getScore() > maxScore) {
                maxScore = selector.getScore();
            }
        }
        return maxScore * SCORE_ORDER_OFFSET + cssStyleRule.getOrder();
    }

    /**
     * 将 JSONArray 中的 JSONObject 提取并添加 动画名字对象，保存到结果中，例如:
     *
     * <pre>
     *  [{"backgroundColor":"#f76160","time":0},{"backgroundColor":"#09ba07","time":100}]
     *  提取为
     *  {"backgroundColor":"#f76160","time":0},{"backgroundColor":"#09ba07","time":100},{"animationName": "Color"}
     * </pre>
     *
     * @param jsonArray       一个非null的JSONArray对象
     * @param animationName   作为标志一个关键帧动画结束的哨兵帧的元素值
     * @param resultJsonArray 保存的结果
     */
    private static void extractJsonArray(
            @NonNull JSONArray jsonArray, String animationName,
            @NonNull JSONArray resultJsonArray) {
        int len = jsonArray.length();
        for (int i = 0; i < len; i++) {
            try {
                resultJsonArray.put(jsonArray.get(i));
            } catch (JSONException e) {
                Log.e(TAG, "extractJSONArray: " + e);
            }
        }
        JSONObject nameObj = new JSONObject();
        try {
            resultJsonArray.put(nameObj.put(KEY_ANIMATION_NAME, animationName));
        } catch (JSONException e) {
            Log.e(TAG, "extractJSONArray: " + e);
        }
    }
}

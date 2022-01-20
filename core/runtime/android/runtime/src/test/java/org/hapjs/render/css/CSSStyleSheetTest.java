/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.css;

import static org.junit.Assert.assertEquals;

import org.hapjs.render.action.RenderActionDocumentMock;
import org.hapjs.render.action.RenderActionNode;
import org.json.JSONException;
import org.junit.Test;

public class CSSStyleSheetTest {

    @Test
    public void simpleRule() throws JSONException {
        String cssRule =
                "{\n"
                        + "                          '#idTest1': {\n"
                        + "                            'ca1': 'ca1-id-1'\n"
                        + "                          },\n"
                        + "                          '.class-test1': {\n"
                        + "                            'ca1': 'ca1-class-1'\n"
                        + "                          },\n"
                        + "                          '.class-test2': {\n"
                        + "                            'ca1': 'ca1-class-2'\n"
                        + "                          },\n"
                        + "                          div: {\n"
                        + "                            'ca1': 'ca1-tag-1'\n"
                        + "                          }\n"
                        + "                        }";

        RenderActionDocumentMock document = new RenderActionDocumentMock(0);
        document.registerStyleSheet(
                1, CSSParser.parseCSSStyleSheet(new org.hapjs.common.json.JSONObject(cssRule)));

        // 新增元素
        RenderActionNode node = document.createNode(1, "div");
        node.setStyleObjectId(1);
        node.setParent(document);

        CSSStyleDeclaration dec = computeCssStyleDeclaration(node);
        assertEquals(dec.getLength(), 1);
        assertEquals(dec.getPropertyValue("ca1"), "ca1-tag-1");

        // 设置class
        node.setCSSClass(" class-test1 ");
        dec = computeCssStyleDeclaration(node);
        assertEquals(dec.getLength(), 1);
        assertEquals(dec.getPropertyValue("ca1"), "ca1-class-1");

        node.setCSSClass("class-test2 class-test1");
        dec = computeCssStyleDeclaration(node);
        assertEquals(dec.getLength(), 1);
        assertEquals(dec.getPropertyValue("ca1"), "ca1-class-2");

        // 设置id
        node.setCSSId("idTest1");
        dec = computeCssStyleDeclaration(node);
        assertEquals(dec.getLength(), 1);
        assertEquals(dec.getPropertyValue("ca1"), "ca1-id-1");

        node.setCSSId("");
        dec = computeCssStyleDeclaration(node);
        assertEquals(dec.getLength(), 1);
        assertEquals(dec.getPropertyValue("ca1"), "ca1-class-2");

        // 设置class
        node.setCSSClass("class-test2");
        dec = computeCssStyleDeclaration(node);
        assertEquals(dec.getLength(), 1);
        assertEquals(dec.getPropertyValue("ca1"), "ca1-class-2");

        node.setCSSClass("class-test2 class-test1");
        dec = computeCssStyleDeclaration(node);
        assertEquals(dec.getLength(), 1);
        assertEquals(dec.getPropertyValue("ca1"), "ca1-class-2");

        node.setCSSClass("");
        dec = computeCssStyleDeclaration(node);
        assertEquals(dec.getLength(), 1);
        assertEquals(dec.getPropertyValue("ca1"), "ca1-tag-1");
    }

    private CSSStyleDeclaration computeCssStyleDeclaration(RenderActionNode node) {
        CSSStyleDeclaration dec;
        dec = node.calFinalStyle(node.calMatchedStyles());
        return dec;
    }

    @Test
    public void descCSSTest() throws JSONException {
        String cssRule =
                "{\n"
                        + "  '#idTest1': {\n"
                        + "    'ca1': 'ca1-desc-idTest1-1'\n"
                        + "  },\n"
                        + "  '.class-test1': {\n"
                        + "    'ca1': 'ca1-desc-class-test1-1'\n"
                        + "  },\n"
                        + "  'div': {\n"
                        + "    'ca1': 'ca1-desc-div-1'\n"
                        + "  },\n"
                        + "  'div text': {\n"
                        + "    'ca1': 'ca1-desc-div-text-1'\n"
                        + "  },\n"
                        + "  '.doc-page #idTest1': {\n"
                        + "    'ca1': 'ca1-desc-doc-page-idTest1-1'\n"
                        + "  },\n"
                        + "  '.doc-page .class-test1': {\n"
                        + "    'ca1': 'ca1-desc-doc-page-class-test1-1'\n"
                        + "  }\n"
                        + "}";

        RenderActionDocumentMock document = new RenderActionDocumentMock(0);
        document.registerStyleSheet(
                1, CSSParser.parseCSSStyleSheet(new org.hapjs.common.json.JSONObject(cssRule)));

        // 新增元素
        RenderActionNode div = document.createNode(1, "div");
        div.setStyleObjectId(1);
        div.setParent(document);

        CSSStyleDeclaration dec = computeCssStyleDeclaration(div);
        assertEquals(dec.getLength(), 1);
        assertEquals(dec.getPropertyValue("ca1"), "ca1-desc-div-1");

        // 新增元素
        RenderActionNode text = document.createNode(1, "text");
        text.setParent(div);

        dec = computeCssStyleDeclaration(text);
        assertEquals(dec.getLength(), 1);
        assertEquals(dec.getPropertyValue("ca1"), "ca1-desc-div-text-1");

        // 设置class
        div.setCSSClass("doc-page");
        text.setCSSClass("class-test1");

        dec = computeCssStyleDeclaration(text);
        assertEquals(dec.getLength(), 1);
        assertEquals(dec.getPropertyValue("ca1"), "ca1-desc-doc-page-class-test1-1");

        // 设置ID
        text.setCSSId("idTest1");

        dec = computeCssStyleDeclaration(text);
        assertEquals(dec.getLength(), 1);
        assertEquals(dec.getPropertyValue("ca1"), "ca1-desc-doc-page-idTest1-1");
    }

    @Test
    public void parse() {
        String[] a = "div >  ".split(">");
        assertEquals(a.length, 2);

        a = "div >  ".split("\\s*>\\s*");
        assertEquals(a.length, 1);

        Selector selector = CSSParser.parseSingleSelector("div");
        assertEquals(selector.getSelectorType(), Selector.SAC_ELEMENT_NODE_SELECTOR);

        selector = CSSParser.parseSingleSelector("#myId");
        assertEquals(selector.getSelectorType(), Selector.SAC_CONDITIONAL_SELECTOR);

        selector = CSSParser.parseSingleSelector("#myClass");
        assertEquals(selector.getSelectorType(), Selector.SAC_CONDITIONAL_SELECTOR);

        // desc
        selector = CSSParser.parseSingleSelector("div #myId");
        assertEquals(selector.getSelectorType(), Selector.SAC_DESCENDANT_SELECTOR);
        SelectorFactory.DescendantSelector descSelector =
                (SelectorFactory.DescendantSelector) selector;
        assertEquals(
                descSelector.mAncestorSelector.getSelectorType(),
                Selector.SAC_ELEMENT_NODE_SELECTOR);
        assertEquals(descSelector.mSimpleSelector.getSelectorType(),
                Selector.SAC_CONDITIONAL_SELECTOR);

        selector = CSSParser.parseSingleSelector(".myClass > div   #myId   ");
        assertEquals(selector.getSelectorType(), Selector.SAC_DESCENDANT_SELECTOR);
        descSelector = (SelectorFactory.DescendantSelector) selector;
        assertEquals(descSelector.mAncestorSelector.getSelectorType(), Selector.SAC_CHILD_SELECTOR);
        assertEquals(descSelector.mSimpleSelector.getSelectorType(),
                Selector.SAC_CONDITIONAL_SELECTOR);

        selector = CSSParser.parseSingleSelector("  div  #myId");
        assertEquals(selector.getSelectorType(), Selector.SAC_DESCENDANT_SELECTOR);
        descSelector = (SelectorFactory.DescendantSelector) selector;
        assertEquals(
                descSelector.mAncestorSelector.getSelectorType(),
                Selector.SAC_ELEMENT_NODE_SELECTOR);
        assertEquals(descSelector.mSimpleSelector.getSelectorType(),
                Selector.SAC_CONDITIONAL_SELECTOR);

        selector = CSSParser.parseSingleSelector("  .myClass   >   div #myId");
        assertEquals(selector.getSelectorType(), Selector.SAC_DESCENDANT_SELECTOR);
        descSelector = (SelectorFactory.DescendantSelector) selector;
        assertEquals(descSelector.mAncestorSelector.getSelectorType(), Selector.SAC_CHILD_SELECTOR);
        assertEquals(descSelector.mSimpleSelector.getSelectorType(),
                Selector.SAC_CONDITIONAL_SELECTOR);

        // child
        selector = CSSParser.parseSingleSelector(".myClass > text");
        assertEquals(selector.getSelectorType(), Selector.SAC_CHILD_SELECTOR);
        SelectorFactory.ChildSelector childSelector = (SelectorFactory.ChildSelector) selector;
        assertEquals(
                childSelector.mAncestorSelector.getSelectorType(),
                Selector.SAC_CONDITIONAL_SELECTOR);
        assertEquals(
                childSelector.mSimpleSelector.getSelectorType(),
                Selector.SAC_ELEMENT_NODE_SELECTOR);

        selector = CSSParser.parseSingleSelector("div #myId > .myClass");
        assertEquals(selector.getSelectorType(), Selector.SAC_CHILD_SELECTOR);
        childSelector = (SelectorFactory.ChildSelector) selector;
        assertEquals(
                childSelector.mAncestorSelector.getSelectorType(),
                Selector.SAC_DESCENDANT_SELECTOR);
        assertEquals(
                childSelector.mSimpleSelector.getSelectorType(), Selector.SAC_CONDITIONAL_SELECTOR);

        selector = CSSParser.parseSingleSelector(".myClass   > text  ");
        assertEquals(selector.getSelectorType(), Selector.SAC_CHILD_SELECTOR);
        childSelector = (SelectorFactory.ChildSelector) selector;
        assertEquals(
                childSelector.mAncestorSelector.getSelectorType(),
                Selector.SAC_CONDITIONAL_SELECTOR);
        assertEquals(
                childSelector.mSimpleSelector.getSelectorType(),
                Selector.SAC_ELEMENT_NODE_SELECTOR);

        selector = CSSParser.parseSingleSelector("  div   #myId >   .myClass");
        assertEquals(selector.getSelectorType(), Selector.SAC_CHILD_SELECTOR);
        childSelector = (SelectorFactory.ChildSelector) selector;
        assertEquals(
                childSelector.mAncestorSelector.getSelectorType(),
                Selector.SAC_DESCENDANT_SELECTOR);
        assertEquals(
                childSelector.mSimpleSelector.getSelectorType(), Selector.SAC_CONDITIONAL_SELECTOR);
    }
}

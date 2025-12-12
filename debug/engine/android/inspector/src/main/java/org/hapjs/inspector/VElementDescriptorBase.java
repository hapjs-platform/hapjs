/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.inspector;

import android.graphics.Rect;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import com.facebook.stetho.common.Accumulator;
import com.facebook.stetho.inspector.elements.AttributeAccumulator;
import com.facebook.stetho.inspector.elements.ComputedStyleAccumulator;
import com.facebook.stetho.inspector.elements.Descriptor;
import com.facebook.stetho.inspector.elements.NodeType;
import com.facebook.stetho.inspector.elements.StyleAccumulator;
import com.facebook.stetho.inspector.elements.StyleRuleNameAccumulator;
import com.facebook.stetho.inspector.elements.android.HighlightableDescriptor;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import javax.annotation.Nullable;
import org.hapjs.bridge.MetaDataSet;
import org.hapjs.bridge.Widget;
import org.hapjs.common.utils.DisplayUtil;
import org.hapjs.common.utils.FloatUtil;
import org.hapjs.component.Component;
import org.hapjs.component.constants.Attributes;
import org.hapjs.render.RootView;
import org.hapjs.render.action.RenderActionManager;
import org.hapjs.render.css.CSSInlineStyleRule;
import org.hapjs.render.css.CSSProperty;
import org.hapjs.render.css.CSSStyleDeclaration;
import org.hapjs.render.css.CSSStyleRule;
import org.hapjs.render.css.MatchedCSSRuleList;
import org.hapjs.render.jsruntime.AppJsThread;
import org.hapjs.render.jsruntime.JsThread;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class VElementDescriptorBase<E extends InspectorVElement> extends Descriptor<E>
        implements HighlightableDescriptor<E> {
    public static final String JSON_NAME = "name";
    public static final String JSON_VALUE = "value";
    private static final String TAG = "VElementDescriptorBase";
    private static final String ID_NAME = "ref";
    private static final String SET_STYLE_JSFUNCTION = "setPageElementStyles";
    private static final String SET_OUTER_HTML_JSFUNCTION = "replacePageElementWithHtml";
    private static final String SET_PAGE_ELEMENT_ATTRS_JSFUNCTION = "setPageElementAttrs";
    private static HashMap<Class<?>, String> _classMap;

    static {
        _classMap = new HashMap<Class<?>, String>();
    }

    private Rect mOffsetViewBounds = new Rect();

    private static String setStylesJsCode(
            int pageId, int elementId, String ruleName, String cssText) {
        String jsCode =
                "JSON.stringify("
                        + SET_STYLE_JSFUNCTION
                        + "("
                        + pageId
                        + ","
                        + elementId
                        + " ,'"
                        + ruleName
                        + "',"
                        + cssText
                        + ") )";
        return jsCode;
    }

    private static String setOuterHTMLJsCode(int pageId, int elementId, String htmlStr) {
        String jsCode =
                "JSON.stringify("
                        + SET_OUTER_HTML_JSFUNCTION
                        + "("
                        + pageId
                        + ","
                        + elementId
                        + " , "
                        + htmlStr
                        + ") )";
        return jsCode;
    }

    private static String setPageElementAttrsJsCode(int pageId, int elementId, String jsonParams) {
        String jsCode =
                SET_PAGE_ELEMENT_ATTRS_JSFUNCTION + "(" + pageId + "," + elementId + ","
                        + jsonParams + ")";
        return jsCode;
    }

    private static String toPixel(int value, int designWidth) {
        value = (int) DisplayUtil.getDesignPxByWidth(value, designWidth);
        return Integer.toString(value) + "px";
    }

    protected static String getVElementName(Class<?> clazz) {
        List<Widget> widgets = MetaDataSet.getInstance().getWidgetList();
        if (widgets != null) {
            for (Widget w : widgets) {
                try {
                    String type = w.getName();
                    Class<?> compClazz = w.getClazz();
                    _classMap.put(compClazz, type);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        String name = _classMap.get(clazz);

        return name;
    }

    @Override
    public String getNodeName(E element) {
        if (element == null) {
            return "Unknown Element";
        }

        int id = element.getVId();
        if (InspectorVElement.ID_BODY == id) {
            return "BODY";
        } else if (InspectorVElement.ID_DOC == id) {
            return "document";
        }
        return element.getTagName();
    }

    @Override
    public String getLocalName(E element) {
        if (element == null) {
            return "Unknown Element";
        }

        int id = element.getVId();
        if (InspectorVElement.ID_BODY == id) {
            return "body";
        } else if (InspectorVElement.ID_DOC == id) {
            return "document";
        }
        return element.getTagName();
    }

    @Override
    public void getAttributes(E element, AttributeAccumulator attributes) {
        if (element == null) {
            return;
        }

        int id = element.getVId();
        if (id == InspectorVElement.ID_BODY || id == InspectorVElement.ID_DOC) {
            return;
        } else {
            attributes.store(ID_NAME, String.valueOf(id));
        }

        int pageId = VDocumentProvider.getPageId();
        Map<String, Object> eleAttr = element.getAttrsMap();
        if (eleAttr == null) {
            return;
        }

        for (String key : eleAttr.keySet()) {
            try {
                Object value = eleAttr.get(key);
                String strValue = null;
                if (value instanceof String) {
                    strValue = (String) value;
                } else {
                    strValue = String.valueOf(value);
                }

                attributes.store(key, strValue);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void setAttributesAsText(E element, String text) {
        if (element == null) {
            return;
        }

        Component comp = element.getComponent();

        Map<String, String> attributeToValueMap = parseSetAttributesAsTextArg(text);
        if (attributeToValueMap != null && comp != null) {
            comp.bindAttrs(attributeToValueMap);
        }
    }

    @Override
    public void setAttributesAsText(E element, String name, String text) {
        if (element == null
                || element.getVId() == InspectorVElement.ID_BODY
                || element.getVId() == InspectorVElement.ID_DOC) {
            return;
        }

        final Map<String, String> attributeToValueMap = parseSetAttributesAsTextArg(text);

        try {
            int elementId = element.getVId();
            int pageId = VDocumentProvider.getPageId();
            JSONArray paramsJson = new JSONArray();
            if (!TextUtils.isEmpty(name)
                    && (!attributeToValueMap.containsKey(name) || text.isEmpty())) {
                // this attr is deleted
                JSONObject jsonObject = new JSONObject();
                jsonObject.put(JSON_NAME, name);
                jsonObject.put(JSON_VALUE, "");
                paramsJson.put(jsonObject);
            }
            for (String key : attributeToValueMap.keySet()) {
                String value = attributeToValueMap.get(key);
                JSONObject jsonObject = new JSONObject();
                jsonObject.put(JSON_NAME, key);
                jsonObject.put(JSON_VALUE, value);
                paramsJson.put(jsonObject);
            }
            V8Inspector.getInstance()
                    .executeJsCode(
                            setPageElementAttrsJsCode(pageId, elementId, paramsJson.toString()));
        } catch (JSONException e) {
            Log.e("VElementDescriptorBase", "setAttributesAsText: ", e);
        }
    }

    @Override
    public void getInlineStyle(E element, StyleAccumulator accumulator) {
        if (element == null
                || element.getVId() == InspectorVElement.ID_BODY
                || element.getVId() == InspectorVElement.ID_DOC) {
            return;
        }
        int vId = element.getVId();
        int pageId = VDocumentProvider.getPageId();

        CSSInlineStyleRule inlineStyle = element.getInlineCSSRule();
        if (inlineStyle == null) {
            return;
        }
        CSSStyleDeclaration style = inlineStyle.getDeclaration();
        ListIterator<String> iterator = style.getReversedListIterator();
        while (iterator.hasPrevious()) {
            String entryKey = iterator.previous();
            CSSProperty property = style.getProperty(entryKey);
            String key = property.getInspectorName();
            String value = property.getValueText();
            boolean disabled = property.getDisabled();
            accumulator.store(key, value, disabled);
        }
    }

    /**
     * @param element
     * @param accumulator mResultJson
     *                    {"inlineStyle":{"name":"INLINE","order":0,"score":{"sum":1000000000},"style":[]
     *                    ,"editable":true},"matchedCSSRules":[{"name":".item-container","order":6
     *                    ,"score":{"sum":1000},"style":[{"name":"margin-left","value":"30px","disabled":false}
     *                    ,{"name":"margin-right","value":"30px","disabled":false},{"name":"background-color"
     *                    ,"value":"#ffffff","disabled":false},{"name":"flex-direction","value":"column"
     *                    ,"disabled":false},{"name":"margin-bottom","value":"30px","disabled":false}],"disabled":{}
     *                    ,"editable":true}]}
     */
    @Override
    public void getStyleRuleNames(E element, StyleRuleNameAccumulator accumulator) {
        if (element == null
                || element.getVId() == InspectorVElement.ID_BODY
                || element.getVId() == InspectorVElement.ID_DOC) {
            return;
        }
        int vId = element.getVId();
        int pageId = VDocumentProvider.getPageId();
        MatchedCSSRuleList styleRuleList = element.getMatchedCSSRuleList();
        if (styleRuleList == null) {
            return;
        }

        // styleRuleList中css rule是按得分降序排列的,
        // 构造的getMatchedStylesForNode CDP消息的matchedCSSRules数组也应该是
        // 降序排列, devtools才能正确显示css rule的优先级顺序;
        // 因此, 此循环索引需要由小到大．
        for (int i = 0; i < styleRuleList.length(); i++) {
            CSSStyleRule cssStyleRule = styleRuleList.getCSSStyleRule(i);
            String ruleName = cssStyleRule.getSelectorText();
            boolean editable = cssStyleRule.getEditable();
            accumulator.store(ruleName, editable);
        }
    }

    /**
     * @param element
     * @param ruleName
     * @param accumulator mResultJson
     *                    {"inlineStyle":{"name":"INLINE","order":0,"score":{"sum":1000000000},"style":[]
     *                    ,"editable":true},"matchedCSSRules":[{"name":".item-container","order":6
     *                    ,"score":{"sum":1000},"style":[{"name":"margin-left","value":"30px","disabled":false}
     *                    ,{"name":"margin-right","value":"30px","disabled":false},{"name":"background-color"
     *                    ,"value":"#fffffe","disabled":false},{"name":"flex-direction","value":"column"
     *                    ,"disabled":false},{"name":"margin-bottom","value":"30px","disabled":false}],"disabled":{}
     *                    ,"editable":true}]}
     */
    @Override
    public void getStyles(E element, String ruleName, StyleAccumulator accumulator) {
        if (element == null
                || element.getVId() == InspectorVElement.ID_BODY
                || element.getVId() == InspectorVElement.ID_DOC) {
            return;
        }
        int vId = element.getVId();
        int pageId = VDocumentProvider.getPageId();
        MatchedCSSRuleList styleRuleList = element.getMatchedCSSRuleList();
        if (styleRuleList == null) {
            return;
        }

        CSSStyleRule styleRule = null;
        for (int i = styleRuleList.length() - 1; i >= 0; i--) {
            styleRule = styleRuleList.getCSSStyleRule(i);
            String tmpName = styleRule.getSelectorText();
            if (tmpName.equals(ruleName)) {
                break;
            }
        }
        if (styleRule != null) {
            CSSStyleDeclaration style = styleRule.getDeclaration();
            ListIterator<String> iterator = style.getReversedListIterator();
            while (iterator.hasPrevious()) {
                String entryKey = iterator.previous();
                CSSProperty property = style.getProperty(entryKey);
                String key = property.getInspectorName();
                String value = property.getValueText();
                boolean disabled = property.getDisabled();
                accumulator.store(key, value, disabled);
            }
        }
    }

    @Override
    public void setStyle(E element, String ruleName, String name, String value) {
        if (element == null
                || element.getVId() == InspectorVElement.ID_BODY
                || element.getVId() == InspectorVElement.ID_DOC) {
            Log.w("VDOM setStyle", "element is null");
            return;
        }
        int elementId = element.getVId();
        int pageId = VDocumentProvider.getPageId();
        String styleJsonText = value;
        V8Inspector.getInstance()
                .executeJsCode(setStylesJsCode(pageId, elementId, ruleName, styleJsonText));
    }

    @Override
    public void setStyle(E element, final String ruleName, final CSSStyleDeclaration style) {
        if (element == null
                || element.getVId() == InspectorVElement.ID_BODY
                || element.getVId() == InspectorVElement.ID_DOC) {
            Log.w("VDOM setStyle", "element is null");
            return;
        }
        final int elementId = element.getVId();
        final int pageId = VDocumentProvider.getPageId();
        // INSPECTOR MOD
        //        final RenderActionManager renderManager = V8Inspector.getInstance().
        //                getRootView().getJsThread().getRenderActionManager();
        RootView rootView = V8Inspector.getInstance().getRootView();
        if (rootView == null) {
            Log.w("VDOM setStyle", "rootView is null");
            return;
        }
        AppJsThread jsThread = rootView.getJsThread();
        if (jsThread == null) {
            Log.w("VDOM setStyle", "jsThread is null");
            return;
        }
        final RenderActionManager renderManager = jsThread.getRenderActionManager();
        if (renderManager == null) {
            Log.w("VDOM setStyle", "renderManager is null");
            return;
        }
        // END
        renderManager.post(
                new Runnable() {
                    @Override
                    public void run() {
                        renderManager.setStyleFromInspector(pageId, elementId, ruleName, style);
                    }
                });
    }

    @Override
    public void setOuterHTML(E element, String outerHTML) {
        if (element == null) {
            Log.w("VDOM setOuterHTML", "element is null");
            return;
        }
        int elementId = element.getVId();
        int pageId = VDocumentProvider.getPageId();
        V8Inspector.getInstance()
                .executeJsCode(setOuterHTMLJsCode(pageId, elementId, filterSymbols(outerHTML)));
    }

    // Modify it from "string" method of JSONStringer which is private.
    private String filterSymbols(String value) {
        StringBuilder out = new StringBuilder("");
        out.append("\"");
        for (int i = 0, length = value.length(); i < length; i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                case '\\':
                case '/':
                    out.append('\\').append(c);
                    break;
                case '\t':
                    out.append("\\t");
                    break;
                case '\b':
                    out.append("\\b");
                    break;
                case '\n':
                    out.append("\\n");
                    break;
                case '\r':
                    out.append("\\r");
                    break;
                case '\f':
                    out.append("\\f");
                    break;
                default:
                    if (c <= 0x1F) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                    break;
            }
        }
        out.append("\"");
        return out.toString();
    }

    /*
     *
     */
    @Override
    public void getComputedStyles(E element, ComputedStyleAccumulator styles) {
        if (element == null) {
            Log.w("VDOM getComputedStyle", "element is null");
            return;
        }
        Component comp = element.getComponent();
        if (comp == null) {
            Log.w("VDOM getComputedStyle", "try get component from element:" + element + " failed");
            return;
        }

        View view = comp.getHostView();
        if (view == null) {
            Log.w("VDOM getComputedStyle", "try get the view failed from element:" + element);
            return;
        }

        int designWidth = V8Inspector.getInstance().getHapEngine().getDesignWidth();

        try {
            // returns the visible bounds
            view.getDrawingRect(mOffsetViewBounds);
            // calculates the relative coordinates to the parent
            ((ViewGroup) view.getRootView())
                    .offsetDescendantRectToMyCoords(view, mOffsetViewBounds);
            styles.store("offsetTop", toPixel(mOffsetViewBounds.top, designWidth));
            styles.store("offsetLeft", toPixel(mOffsetViewBounds.left, designWidth));

            // position
            styles.store("left", toPixel(view.getLeft(), designWidth));
            styles.store("top", toPixel(view.getTop(), designWidth));
            styles.store("right", toPixel(view.getRight(), designWidth));
            styles.store("bottom", toPixel(view.getBottom(), designWidth));
            styles.store("width", toPixel(view.getWidth(), designWidth));
            styles.store("height", toPixel(view.getHeight(), designWidth));
            // border
            styles.store(
                    "border-left-width",
                    toPixel((int) comp.getBorderWidth(Attributes.Style.BORDER_LEFT_WIDTH),
                            designWidth));
            styles.store(
                    "border-top-width",
                    toPixel((int) comp.getBorderWidth(Attributes.Style.BORDER_TOP_WIDTH),
                            designWidth));
            styles.store(
                    "border-right-width",
                    toPixel((int) comp.getBorderWidth(Attributes.Style.BORDER_RIGHT_WIDTH),
                            designWidth));
            styles.store(
                    "border-bottom-width",
                    toPixel((int) comp.getBorderWidth(Attributes.Style.BORDER_BOTTOM_WIDTH),
                            designWidth));
            // padding
            styles.store("padding-left", toPixel(view.getPaddingLeft(), designWidth));
            styles.store("padding-top", toPixel(view.getPaddingTop(), designWidth));
            styles.store("padding-right", toPixel(view.getPaddingRight(), designWidth));
            styles.store("padding-bottom", toPixel(view.getPaddingBottom(), designWidth));
            // margin

            styles.store(
                    "margin-left",
                    toPixel(comp.getMargin(Attributes.Style.MARGIN_LEFT), designWidth));
            styles.store("margin-top",
                    toPixel(comp.getMargin(Attributes.Style.MARGIN_TOP), designWidth));
            styles.store(
                    "margin-right",
                    toPixel(comp.getMargin(Attributes.Style.MARGIN_RIGHT), designWidth));
            styles.store(
                    "margin-bottom",
                    toPixel(comp.getMargin(Attributes.Style.MARGIN_BOTTOM), designWidth));

            // border-radius
            String borderRadiusString = toPixel(0, designWidth);
            float borderRadius = comp.getBorderRadius();
            if (!FloatUtil.isUndefined(borderRadius)) {
                borderRadiusString = toPixel((int) borderRadius, designWidth);
            }
            styles.store("border-radius", borderRadiusString);

            // view isShown
            styles.store("isShown", String.valueOf(view.isShown()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void getBoxModel(E element, final DOMAccumulator accumulator) {
        if (element == null) {
            Log.w("VDOM getBoxModel", "element is null");
            return;
        }
        final Component comp = element.getComponent();
        if (comp == null) {
            Log.w("VDOM getBoxModel", "try get component failed from element:" + element);
            return;
        }

        View view = comp.getHostView();
        if (view == null) {
            Log.w("VDOM getBoxModel", "try get the view failed from element:" + element);
            return;
        }
        int[] width = new int[1];
        int[] height = new int[1];
        final int[] content = new int[8];
        final int[] padding = new int[8];
        final int[] margin = new int[8];
        final int[] border = new int[8];
        // height, width
        int designWidth = V8Inspector.getInstance().getHapEngine().getDesignWidth();
        width[0] = (int) DisplayUtil.getDesignPxByWidth(view.getWidth(), designWidth);
        height[0] = (int) DisplayUtil.getDesignPxByWidth(view.getHeight(), designWidth);
        accumulator.store("width", width);
        accumulator.store("height", height);
        // margin
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        final int left = location[0];
        final int top = location[1];
        final int right = left + view.getWidth();
        final int bottom = top + view.getHeight();
        margin[0] = left;
        margin[1] = top;
        margin[2] = right;
        margin[3] = top;
        margin[4] = right;
        margin[5] = bottom;
        margin[6] = left;
        margin[7] = bottom;
        accumulator.store("margin", margin);
        // border
        try {
            int borderLeft = margin[0] + comp.getMargin(Attributes.Style.MARGIN_LEFT);
            int borderTop = margin[1] + comp.getMargin(Attributes.Style.MARGIN_TOP);
            int borderRight = margin[2] - comp.getMargin(Attributes.Style.MARGIN_RIGHT);
            int borderBottom = margin[5] - comp.getMargin(Attributes.Style.MARGIN_BOTTOM);
            border[0] = borderLeft;
            border[1] = borderTop;
            border[2] = borderRight;
            border[3] = borderTop;
            border[4] = borderRight;
            border[5] = borderBottom;
            border[6] = borderLeft;
            border[7] = borderBottom;
            accumulator.store("border", border);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Runnable obj =
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            // padding
                            int paddingLeft =
                                    border[0] + (int) comp
                                            .getBorderWidth(Attributes.Style.BORDER_LEFT_WIDTH);
                            int paddingTop =
                                    border[1] + (int) comp
                                            .getBorderWidth(Attributes.Style.BORDER_TOP_WIDTH);
                            int paddingRight =
                                    border[2] - (int) comp
                                            .getBorderWidth(Attributes.Style.BORDER_RIGHT_WIDTH);
                            int paddingBottom =
                                    border[5] - (int) comp
                                            .getBorderWidth(Attributes.Style.BORDER_BOTTOM_WIDTH);
                            padding[0] = paddingLeft;
                            padding[1] = paddingTop;
                            padding[2] = paddingRight;
                            padding[3] = paddingTop;
                            padding[4] = paddingRight;
                            padding[5] = paddingBottom;
                            padding[6] = paddingLeft;
                            padding[7] = paddingBottom;
                            accumulator.store("padding", padding);
                            // content
                            int contentLeft = padding[0]
                                    + (int) comp.getPadding(Attributes.Style.PADDING_LEFT);
                            int contentTop = padding[1]
                                    + (int) comp.getPadding(Attributes.Style.PADDING_TOP);
                            int contentRight = padding[2]
                                    - (int) comp.getPadding(Attributes.Style.PADDING_RIGHT);
                            int contentBottom = padding[5]
                                    - (int) comp.getPadding(Attributes.Style.PADDING_BOTTOM);
                            content[0] = contentLeft;
                            content[1] = contentTop;
                            content[2] = contentRight;
                            content[3] = contentTop;
                            content[4] = contentRight;
                            content[5] = contentBottom;
                            content[6] = contentLeft;
                            content[7] = contentBottom;
                            accumulator.store("content", content);
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            synchronized (this) {
                                notifyAll();
                            }
                        }
                    }
                };
        view.post(obj);
    }

    @Nullable
    public View getViewAndBoundsForHighlighting(E element, Rect bounds) {
        try {
            if (element == null) {
                Log.w(TAG, "element is null");
                return null;
            }
            Component comp = element.getComponent();
            if (comp == null) {
                Log.w(TAG, "try get component from element:" + element + " failed");
                return null;
            }
            return comp.getHostView();
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    public Object getElementToHighlightAtPosition(E element, int x, int y, Rect bounds) {
        try {
            if (element != null) {
                Component comp = element.getComponent();
                if (comp != null) {
                    View view = comp.getHostView();
                    if (null != view) {
                        bounds.set(0, 0, view.getWidth(), view.getHeight());
                    } else {
                        Log.w(TAG, "Host view is null");
                    }
                } else {
                    Log.w(TAG, "try get component from element:" + element + " failed");
                }
            } else {
                Log.w(TAG, "element is null");
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception of set view bounds", e);
        }
        return element;
    }

    @Override
    public void hook(E element) {
    }

    @Override
    public void unhook(E element) {
    }

    @Override
    public NodeType getNodeType(E element) {
        return NodeType.ELEMENT_NODE;
    }

    @Override
    public String getNodeValue(E element) {
        return "";
    }

    @Override
    public void getChildren(E element, Accumulator<Object> children) {
    }
}

/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.analyzer.tools;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.hapjs.analyzer.Analyzer;
import org.hapjs.analyzer.model.AnalyzerConstant;
import org.hapjs.analyzer.views.tree.NodeItemInfo;
import org.hapjs.cache.Cache;
import org.hapjs.cache.CacheStorage;
import org.hapjs.common.utils.DisplayUtil;
import org.hapjs.common.utils.FloatUtil;
import org.hapjs.component.Component;
import org.hapjs.component.ComponentDataHolder;
import org.hapjs.component.constants.Attributes;
import org.hapjs.component.constants.Edge;
import org.hapjs.component.constants.Spacing;
import org.hapjs.component.view.ComponentHost;
import org.hapjs.component.view.drawable.CSSBackgroundDrawable;
import org.hapjs.component.view.state.State;
import org.hapjs.model.AppInfo;
import org.hapjs.model.ConfigInfo;
import org.hapjs.render.css.value.CSSValues;
import org.hapjs.render.vdom.VDocument;
import org.hapjs.render.vdom.VElement;
import org.hapjs.render.vdom.VGroup;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class InspectorUtils {
    private static final String TAG = "InspectorUtils";
    public static final int MAX_VIRTUAL_DOM_DEPTH = 14;
    public static final int MAX_VIEW_TREE_DEPTH = 20;

    public static NodeItemInfo convertVirtualDomNodeInfo(VElement element) {
        NodeItemInfo info = new NodeItemInfo();
        info.data = element;
        StringBuilder builder = new StringBuilder();
        builder.append('<');
        if (element instanceof VDocument) {
            builder.append("template");
        } else {
            builder.append(element.getTagName());
        }
        builder.append(' ');
        ComponentDataHolder recyclerItem = element.getComponentDataHolder();
        Map<String, Object> attrsDomData = recyclerItem.getAttrsDomData();
        if (attrsDomData != null) {
            Set<Map.Entry<String, Object>> entrySet = attrsDomData.entrySet();
            for (Map.Entry<String, Object> entry : entrySet) {
                String key = entry.getKey();
                Object value = entry.getValue();
                builder.append(key).append('=').append(value).append(' ');
            }
        }
        Map<String, CSSValues> styleDomData = recyclerItem.getStyleDomData();
        if (styleDomData != null) {
            Set<Map.Entry<String, CSSValues>> entrySet = styleDomData.entrySet();
            if (entrySet.size() > 0) {
                builder.append("style=\"");
                for (Map.Entry<String, CSSValues> entry : entrySet) {
                    String key = entry.getKey();
                    CSSValues value = entry.getValue();
                    if (value != null) {
                        builder.append(key).append(':').append(value.get(State.NORMAL)).append(';');
                    }
                }
                builder.append('\"');
            }
        }
        builder.append('>');
        info.title = builder.toString();
        return info;
    }

    public static NodeItemInfo convertNativeViewNodeInfo(View view) {
        NodeItemInfo info = new NodeItemInfo();
        info.data = view;
        StringBuilder builder = new StringBuilder();
        String name = view.getClass().getSimpleName();
        if (TextUtils.isEmpty(name)) {
            name = view.getClass().getName();
            int index = name.lastIndexOf('.');
            if (index > 0) {
                name = name.substring(index + 1);
            }
        }
        builder.append(name);
        if (view instanceof TextView) {
            TextView tv = (TextView) view;
            CharSequence text = tv.getText();
            if (!TextUtils.isEmpty(text)) {
                builder.append(" (").append(text).append(')');
            }
        } else if (view instanceof ComponentHost) {
            Component component = ((ComponentHost) view).getComponent();
            if (component != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> attrsDomData = component.getAttrsDomData();
                if (attrsDomData != null) {
                    Object value = attrsDomData.get(Attributes.Style.VALUE);
                    if (value == null) {
                        value = attrsDomData.get(Attributes.Style.CONTENT);
                    }
                    if (value != null && !TextUtils.isEmpty(value.toString())) {
                        builder.append(" (").append(value).append(')');
                    }
                }
            }
        }
        info.title = builder.toString();
        return info;
    }

    public static Integer getVirtualDomLayerCount(VElement rootElement, TraverseCallback<VElement, Integer> traverseCallback) {
        return traverseTree(new AbsVDomTreeConverter() {
            @Override
            public VElement getRootNode() {
                return rootElement;
            }
        }, traverseCallback);
    }

    public static Integer getViewTreeLayerCount(View rootView, TraverseCallback<View, Integer> traverseCallback) {
        return traverseTree(new AbsViewTreeConverter() {
            @Override
            public View getRootNode() {
                return rootView;
            }
        }, traverseCallback);
    }

    /**
     * Calculate the ratio of the number of off-screen views to the total number of views
     * @param rootView
     * @param minTotalCount The result will be calculated if the total number of views is greater than this value, otherwise the result will be 0
     * @return
     */
    public static float getOuterViewRatio(View rootView, int minTotalCount) {
        Rect screenRect = new Rect();
        rootView.getRootView().getGlobalVisibleRect(screenRect);
        List<Integer> data = traverseTree(new AbsViewTreeConverter() {
            @Override
            public View getRootNode() {
                return rootView;
            }
        }, new TraverseCallback<View, List<Integer>>() {
            @Override
            public List<Integer> initData() {
                return Arrays.asList(0, 0); // 0：innerCount; 1: outerCount
            }

            @Override
            public List<Integer> onTraverseNode(View node, int layer, List<Integer> data) {
                if (node != null) {
                    if (!(node instanceof ViewGroup) || ((ViewGroup) node).getChildCount() <= 0) {
                        if (isOuter(node, screenRect)) {
                            data.set(1, data.get(1) + 1);
                        } else {
                            data.set(0, data.get(0) + 1);
                        }
                    }
                }
                return data;
            }
        });
        float ratio = 0;
        if (data.size() >= 2) {
            float total = data.get(0) + data.get(1);
            if (total > minTotalCount) {
                ratio = data.get(1) / total;
            }
            Log.d(TAG, "AnalyzerPanel_LOG getOuterViewRatio in - out : " + data.get(0) + " - " + data.get(1) + ", ratio = " + ratio);
        }
        return ratio;
    }

    private static <T, K> K traverseTree(LayerNodeConverter<T> converter, TraverseCallback<T, K> traverseCallback) {
        K data = null;
        if (traverseCallback != null) {
            data = traverseCallback.initData();
            traverseCallback.onTraverseStart();
        }
        NodeFactory<LayerInfo<T>> nodeFactory = new NodeFactory<>(LayerInfo::new);
        Deque<LayerInfo<T>> deque = new ArrayDeque<>();
        deque.add(nodeFactory.getNode().set(1, converter.getRootNode()));

        while (!deque.isEmpty()) {
            LayerInfo<T> layerNode = deque.removeFirst();
            int currentLayer = layerNode.layer;
            T currentNode = layerNode.node;
            layerNode.reset();
            nodeFactory.recycle(layerNode);
            if (traverseCallback != null) {
                data = traverseCallback.onTraverseNode(currentNode, currentLayer, data);
            }
            if (converter.isGroup(currentNode) && converter.getChildCount(currentNode) > 0) {
                int childCount = converter.getChildCount(currentNode);
                for (int i = 0; i < childCount; i++) {
                    T child = converter.getChildAt(currentNode, i);
                    deque.add(nodeFactory.getNode().set(currentLayer + 1, child));
                }
            }
        }
        if (traverseCallback != null) {
            traverseCallback.onTraverseFinished();
        }
        return data;
    }

    /**
     * Determine whether the view is outside the scope of root
     * @param currentNode
     * @param rootViewRect
     * @return
     */
    private static boolean isOuter(View currentNode, Rect rootViewRect) {
        if (currentNode == null || rootViewRect == null) {
            return false;
        }
        try {
            Rect rect = new Rect();
            currentNode.getDrawingRect(rect);
            ((ViewGroup) currentNode.getRootView()).offsetDescendantRectToMyCoords(currentNode, rect);
            if (rect.bottom > rect.top && rect.right > rect.left) {
                // 屏幕之外
                return rect.bottom <= rootViewRect.top || rect.top >= rootViewRect.bottom || (rect.left >= rootViewRect.right) || rect.right <= rootViewRect.left;
            }
        } catch (Exception e) {
            Log.e(TAG, "AnalyzerPanel_LOG fail to determine if it is out of screen: ", e);
        }
        return false;
    }

    public abstract static class TraverseCallback<T, K> {
        public abstract K initData();

        void onTraverseStart() {}

        public abstract K onTraverseNode(T node, int layer, K data);

        void onTraverseFinished() {}
    }

    private interface LayerNodeConverter<T> {
        boolean isGroup(T node);

        int getChildCount(T groupNode);

        T getChildAt(T groupNode, int index);

        T getRootNode();
    }

    private static abstract class AbsViewTreeConverter implements LayerNodeConverter<View>{
        @Override
        public boolean isGroup(View node) {
            return node instanceof ViewGroup;
        }

        @Override
        public int getChildCount(View groupNode) {
            if (!isGroup(groupNode)) {
                return 0;
            }
            return ((ViewGroup) groupNode).getChildCount();
        }

        @Override
        public View getChildAt(View groupNode, int index) {
            ViewGroup viewGroup = (ViewGroup) groupNode;
            return viewGroup.getChildAt(index);
        }
    }

    private static abstract class AbsVDomTreeConverter implements LayerNodeConverter<VElement>{
        @Override
        public boolean isGroup(VElement node) {
            return node instanceof VGroup;
        }

        @Override
        public int getChildCount(VElement groupNode) {
            if (!isGroup(groupNode)) {
                return 0;
            }
            return ((VGroup) groupNode).getChildren().size();
        }

        @Override
        public VElement getChildAt(VElement groupNode, int index) {
            if (groupNode instanceof VGroup) {
                VGroup vGroup = (VGroup) groupNode;
                return vGroup.getChildren().get(index);
            } else {
                return null;
            }
        }
    }

    private static class LayerInfo<T> {
        int layer;
        T node;

        public LayerInfo<T> set(int layer, T node) {
            this.layer = layer;
            this.node = node;
            return this;
        }

        public LayerInfo<T> reset() {
            this.layer = -1;
            this.node = null;
            return this;
        }
    }

    public static Map<String, String> dumpVirtualDomProperties(VElement element) {
        ComponentDataHolder dataHolder = element.getComponentDataHolder();
        if (dataHolder == null) {
            return Collections.emptyMap();
        }

        Map<String, String> props = new LinkedHashMap<>();
        Map<String, CSSValues> styleDomData = dataHolder.getStyleDomData();
        if (styleDomData != null) {
            Set<Map.Entry<String, CSSValues>> entrySet = styleDomData.entrySet();
            for (Map.Entry<String, CSSValues> entry : entrySet) {
                String styleName = entry.getKey();
                CSSValues styleValue = entry.getValue();
                Object value;
                if (styleValue != null && (value = styleValue.get(State.NORMAL)) != null) {
                    props.put(styleName, value.toString());
                }
            }
        }

        Map<String, Object> attrsDomData = dataHolder.getAttrsDomData();
        if (attrsDomData != null) {
            Set<Map.Entry<String, Object>> entrySet = attrsDomData.entrySet();
            for (Map.Entry<String, Object> entry : entrySet) {
                String attr = entry.getKey();
                Object value = entry.getValue();
                if (value != null) {
                    props.put(attr, value.toString());
                }
            }
        }
        return props;
    }

    public static Map<String, String> dumpCSSBoxProperties(Component comp, Context context) {
        if (comp == null) {
            Log.w(TAG, "dumpCSSBoxProperties fail because component is null");
            return Collections.emptyMap();
        }
        View view = comp.getHostView();
        if (view == null) {
            Log.w(TAG, "dumpCSSBoxProperties fail because view is null");
            return Collections.emptyMap();
        }
        Map<String, String> props = new LinkedHashMap<>();
        String packageName = Analyzer.get().getPackageName();
        if (TextUtils.isEmpty(packageName)) {
            Log.w(TAG, "dumpCSSBoxProperties fail because packageName is null");
            return Collections.emptyMap();
        }
        Cache cache = CacheStorage.getInstance(context).getCache(packageName);
        AppInfo appInfo = cache.getAppInfo(true);
        ConfigInfo info = appInfo == null ? null : appInfo.getConfigInfo();
        int designWidth = info == null ? ConfigInfo.DEFAULT_DESIGN_WIDTH : info.getDesignWidth();
        try {
            // margin
            props.put(AnalyzerConstant.MARGIN_LEFT, toPixel(comp.getMargin(Attributes.Style.MARGIN_LEFT), designWidth));
            props.put(AnalyzerConstant.MARGIN_TOP, toPixel(comp.getMargin(Attributes.Style.MARGIN_TOP), designWidth));
            props.put(AnalyzerConstant.MARGIN_RIGHT, toPixel(comp.getMargin(Attributes.Style.MARGIN_RIGHT), designWidth));
            props.put(AnalyzerConstant.MARGIN_BOTTOM, toPixel(comp.getMargin(Attributes.Style.MARGIN_BOTTOM), designWidth));
            // border
            props.put(AnalyzerConstant.BORDER_LEFT_WIDTH, toPixel((int) comp.getBorderWidth(Attributes.Style.BORDER_LEFT_WIDTH), designWidth));
            props.put(AnalyzerConstant.BORDER_TOP_WIDTH, toPixel((int) comp.getBorderWidth(Attributes.Style.BORDER_TOP_WIDTH), designWidth));
            props.put(AnalyzerConstant.BORDER_RIGHT_WIDTH, toPixel((int) comp.getBorderWidth(Attributes.Style.BORDER_RIGHT_WIDTH), designWidth));
            props.put(AnalyzerConstant.BORDER_BOTTOM_WIDTH, toPixel((int) comp.getBorderWidth(Attributes.Style.BORDER_BOTTOM_WIDTH), designWidth));
            // padding
            props.put(AnalyzerConstant.PADDING_LEFT, toPixel(view.getPaddingLeft(), designWidth));
            props.put(AnalyzerConstant.PADDING_TOP, toPixel(view.getPaddingTop(), designWidth));
            props.put(AnalyzerConstant.PADDING_RIGHT, toPixel(view.getPaddingRight(), designWidth));
            props.put(AnalyzerConstant.PADDING_BOTTOM, toPixel(view.getPaddingBottom(), designWidth));
            // size
            props.put(AnalyzerConstant.WIDTH, String.valueOf(toPixel(view.getWidth(), designWidth)));
            props.put(AnalyzerConstant.HEIGHT, String.valueOf(toPixel(view.getHeight(), designWidth)));
        } catch (Exception e) {
            Log.e(TAG, "AnalyzerPanel_LOG dumpVirtualDomProperties fail: ", e);
        }
        return props;
    }

    private static String toPixel(int value, int designWidth) {
        return String.valueOf((int) DisplayUtil.getDesignPxByWidth(value, designWidth));
    }

    public static Map<String, String> dumpNativeViewProperties(View view) {
        Map<String, String> props = new LinkedHashMap<>();
        props.put(AnalyzerConstant.WIDTH, String.valueOf(view.getWidth()));
        props.put(AnalyzerConstant.HEIGHT, String.valueOf(view.getHeight()));
        props.put(AnalyzerConstant.LEFT, String.valueOf(view.getLeft()));
        props.put(AnalyzerConstant.TOP, String.valueOf(view.getTop()));
        props.put(AnalyzerConstant.RIGHT, String.valueOf(view.getRight()));
        props.put(AnalyzerConstant.BOTTOM, String.valueOf(view.getBottom()));
        props.put(AnalyzerConstant.PADDING_LEFT, String.valueOf(view.getPaddingLeft()));
        props.put(AnalyzerConstant.PADDING_TOP, String.valueOf(view.getPaddingTop()));
        props.put(AnalyzerConstant.PADDING_RIGHT, String.valueOf(view.getPaddingRight()));
        props.put(AnalyzerConstant.PADDING_BOTTOM, String.valueOf(view.getPaddingBottom()));
        props.put(AnalyzerConstant.VISIBILITY, view.getVisibility() == View.VISIBLE
                ? AnalyzerConstant.VISIBLE :
                (view.getVisibility() == View.INVISIBLE ?
                        AnalyzerConstant.INVISIBLE : AnalyzerConstant.GONE));
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) layoutParams;
            props.put(AnalyzerConstant.MARGIN_LEFT, String.valueOf(marginLayoutParams.leftMargin));
            props.put(AnalyzerConstant.MARGIN_TOP, String.valueOf(marginLayoutParams.topMargin));
            props.put(AnalyzerConstant.MARGIN_RIGHT, String.valueOf(marginLayoutParams.rightMargin));
            props.put(AnalyzerConstant.MARGIN_BOTTOM, String.valueOf(marginLayoutParams.bottomMargin));
        }
        if (view instanceof TextView) {
            TextView tv = (TextView) view;
            CharSequence text = tv.getText();
            if (text != null) {
                props.put(AnalyzerConstant.VALUE, text.toString());
            }
        }
        if (view instanceof ComponentHost) {
            Component component = ((ComponentHost) view).getComponent();
            if (component != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> attrsDomData = component.getAttrsDomData();
                if (attrsDomData != null) {
                    Object value = attrsDomData.get(Attributes.Style.VALUE);
                    if (value == null) {
                        value = attrsDomData.get(Attributes.Style.CONTENT);
                    }
                    if (value != null) {
                        props.put(AnalyzerConstant.VALUE, value.toString());
                    }
                }
            }
        }
        Drawable background = view.getBackground();
        if (background instanceof CSSBackgroundDrawable) {
            CSSBackgroundDrawable cssBackgroundDrawable = (CSSBackgroundDrawable) background;
            float borderWidth = cssBackgroundDrawable.getBorderWidth(Spacing.ALL);
            if (FloatUtil.isUndefined(borderWidth) && borderWidth > 0) {
                props.put(AnalyzerConstant.BORDER_LEFT_WIDTH, String.valueOf(borderWidth));
                props.put(AnalyzerConstant.BORDER_TOP_WIDTH, String.valueOf(borderWidth));
                props.put(AnalyzerConstant.BORDER_RIGHT_WIDTH, String.valueOf(borderWidth));
                props.put(AnalyzerConstant.BORDER_BOTTOM_WIDTH, String.valueOf(borderWidth));
            } else {
                float borderLeftWidth = cssBackgroundDrawable.getBorderWidth(Spacing.LEFT);
                if (FloatUtil.isUndefined(borderLeftWidth) && borderLeftWidth > 0f) {
                    props.put(AnalyzerConstant.BORDER_LEFT_WIDTH, String.valueOf(borderLeftWidth));
                } else {
                    props.put(AnalyzerConstant.BORDER_LEFT_WIDTH, "0");
                }
                float borderTopWidth = cssBackgroundDrawable.getBorderWidth(Spacing.TOP);
                if (FloatUtil.isUndefined(borderTopWidth) && borderTopWidth != 0f) {
                    props.put(AnalyzerConstant.BORDER_TOP_WIDTH, String.valueOf(borderTopWidth));
                } else {
                    props.put(AnalyzerConstant.BORDER_TOP_WIDTH, "0");
                }
                float borderRightWidth = cssBackgroundDrawable.getBorderWidth(Spacing.RIGHT);
                if (FloatUtil.isUndefined(borderRightWidth) && borderRightWidth != 0f) {
                    props.put(AnalyzerConstant.BORDER_RIGHT_WIDTH, String.valueOf(borderRightWidth));
                } else {
                    props.put(AnalyzerConstant.BORDER_RIGHT_WIDTH, "0");
                }
                float borderBottomWidth = cssBackgroundDrawable.getBorderWidth(Spacing.BOTTOM);
                if (FloatUtil.isUndefined(borderBottomWidth) && borderBottomWidth != 0f) {
                    props.put(AnalyzerConstant.BORDER_BOTTOM_WIDTH, String.valueOf(borderBottomWidth));
                } else {
                    props.put(AnalyzerConstant.BORDER_BOTTOM_WIDTH, "0");
                }
            }
            int borderColor = cssBackgroundDrawable.getBorderColor(Edge.ALL);
            if (borderColor != 0) {
                props.put(AnalyzerConstant.BORDER_LEFT_COLOR, String.format("#%06X", borderColor));
                props.put(AnalyzerConstant.BORDER_TOP_COLOR, String.format("#%06X", borderColor));
                props.put(AnalyzerConstant.BORDER_RIGHT_COLOR, String.format("#%06X", borderColor));
                props.put(AnalyzerConstant.BORDER_BOTTOM_COLOR, String.format("#%06X", borderColor));
            } else {
                int borderLeftColor = cssBackgroundDrawable.getBorderColor(Edge.LEFT);
                props.put(AnalyzerConstant.BORDER_LEFT_COLOR, String.format("#%06X", borderLeftColor));
                int borderTopColor = cssBackgroundDrawable.getBorderColor(Edge.TOP);
                props.put(AnalyzerConstant.BORDER_TOP_COLOR, String.format("#%06X", borderTopColor));
                int borderRightColor = cssBackgroundDrawable.getBorderColor(Edge.RIGHT);
                props.put(AnalyzerConstant.BORDER_RIGHT_COLOR, String.format("#%06X", borderRightColor));
                int borderBottomColor = cssBackgroundDrawable.getBorderColor(Edge.BOTTOM);
                props.put(AnalyzerConstant.BORDER_BOTTOM_COLOR, String.format("#%06X", borderBottomColor));
            }
            float radius = cssBackgroundDrawable.getRadius();
            if (radius > 0) {
                props.put(AnalyzerConstant.BORDER_RADIUS, String.valueOf(radius));
            }
            props.put(AnalyzerConstant.BACKGROUND_COLOR, String.format("#%06X", 0xFFFFFF & cssBackgroundDrawable.getColor()));
            props.put(AnalyzerConstant.OPACITY, String.valueOf(cssBackgroundDrawable.getOpacity()));
        }
        return props;
    }

    private static String extractDigital(String value) {
        if (TextUtils.isEmpty(value)) {
            return "0";
        }
        if (value.endsWith("%")) {
            return "auto";
        }
        String digits = value.replaceAll("[^0-9.-]", "");
        int dotIndex = digits.indexOf('.');
        if (dotIndex >= 0) {
            int len = digits.length();
            if (len - 1 > dotIndex) {
                try {
                    double d = Double.parseDouble(digits);
                    d = Math.round(d);
                    return String.valueOf((int) d);
                } catch (Exception e) {
                    return digits.substring(0, dotIndex);
                }
            } else {
                return digits.substring(0, dotIndex);
            }
        } else {
            return digits;
        }
    }

    private static class NodeFactory<T> {
        Deque<T> mDeque = new ArrayDeque<>();
        private NodeCreater<T> mNodeCreater;

        interface NodeCreater<T> {
            T createNode();
        }
        T getNode() {
            if (mDeque.isEmpty()) {
                return mNodeCreater.createNode();
            }
            return mDeque.removeFirst();
        }
        NodeFactory(NodeCreater<T> creator) {
            mNodeCreater = creator;
        }

        void recycle(T node) {
            mDeque.addLast(node);
        }
    }

}

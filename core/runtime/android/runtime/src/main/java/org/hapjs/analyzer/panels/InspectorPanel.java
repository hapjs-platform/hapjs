/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.analyzer.panels;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.hapjs.analyzer.AnalyzerContext;
import org.hapjs.analyzer.model.AnalyzerConstant;
import org.hapjs.analyzer.tools.AnalyzerThreadManager;
import org.hapjs.analyzer.tools.InspectorUtils;
import org.hapjs.analyzer.views.CSSBox;
import org.hapjs.analyzer.views.tree.NodeItemInfo;
import org.hapjs.analyzer.views.tree.RecyclerTreeView;
import org.hapjs.analyzer.views.tree.TreeNode;
import org.hapjs.common.executors.Executors;
import org.hapjs.common.utils.DisplayUtil;
import org.hapjs.common.utils.ThreadUtils;
import org.hapjs.component.Component;
import org.hapjs.render.DecorLayout;
import org.hapjs.render.Page;
import org.hapjs.render.vdom.VDocument;
import org.hapjs.render.vdom.VElement;
import org.hapjs.render.vdom.VGroup;
import org.hapjs.runtime.BuildConfig;
import org.hapjs.runtime.R;
import org.hapjs.runtime.Runtime;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class InspectorPanel extends CollapsedPanel implements AnalyzerContext.AnalyzerCallback {
    public static final String NAME = "inspector";
    private static final int COLOR_ENCIRCLE_VIEW = 0xFF345FFF;
    private EncircleObject mEncircle;
    private View mDomTreeContent;
    private View mViewTreeContent;
    private RecyclerTreeView mVirtualDomTree;
    private RecyclerTreeView mNativeViewTree;
    private CSSBox mCSSBox;
    private LinearLayout mAttrDisplay;

    public InspectorPanel(Context context) {
        super(context, NAME,BOTTOM);
    }

    @Override
    protected int panelLayoutId() {
        return R.layout.layout_analyzer_inspector;
    }

    @Override
    protected void onCreateFinish() {
        super.onCreateFinish();
        View btnNative = findViewById(R.id.btn_analyzer_inspector_native);
        View btnVdom = findViewById(R.id.btn_analyzer_inspector_vdom);
        findViewById(R.id.btn_analyzer_inspector_refresh).setOnClickListener(v -> refreshWhole());
        btnVdom.setSelected(true);
        btnVdom.setOnClickListener(v -> {
            mViewTreeContent.setVisibility(View.GONE);
            mDomTreeContent.setVisibility(View.VISIBLE);
            btnVdom.setSelected(true);
            btnNative.setSelected(false);
            setUpRecyclerView(mVirtualDomTree);
        });
        if (BuildConfig.DEBUG) {
            btnNative.setVisibility(VISIBLE);
            btnNative.setOnClickListener(v -> {
                mDomTreeContent.setVisibility(View.GONE);
                mViewTreeContent.setVisibility(View.VISIBLE);
                btnVdom.setSelected(false);
                btnNative.setSelected(true);
                setUpRecyclerView(mNativeViewTree);
            });
        } else {
            btnNative.setVisibility(GONE);
        }
        mVirtualDomTree = findViewById(R.id.analyzer_inspector_vdom_tree);
        mNativeViewTree = findViewById(R.id.analyzer_inspector_native_tree);
        mDomTreeContent = findViewById(R.id.analyzer_inspector_vdom_content);
        mViewTreeContent = findViewById(R.id.analyzer_inspector_native_content);
        setUpRecyclerView(mVirtualDomTree);
        mCSSBox = findViewById(R.id.analyzer_inspector_cssbox);
        mAttrDisplay = findViewById(R.id.analyzer_inspector_attr_display);
        mVirtualDomTree.setOnNodeItemClickListener((holder, position, node) -> {
            NodeItemInfo nodeItemInfo = node.data;
            VElement element = (VElement) nodeItemInfo.data;
            mCSSBox.setNative(false);
            Map<String, String> domProperties = InspectorUtils.dumpVirtualDomProperties(element);
            Map<String, String> cssBoxValues = InspectorUtils.dumpCSSBoxProperties(element.getComponent(), mContext);
            updateDisplay(domProperties, cssBoxValues);
            int nodePageId = nodeItemInfo.pageId;
            int pageId = getAnalyzerContext().getCurrentPageId();
            if (pageId == -1 || pageId != nodePageId) {
                return;
            }
            // mark the corresponding view
            Component component = element.getComponent();
            if (component != null) {
                markSingleView(component.getHostView());
            }
        });

        mNativeViewTree.setOnNodeItemClickListener((holder, position, node) -> {
            NodeItemInfo nodeItemInfo = node.data;
            int nodePageId = nodeItemInfo.pageId;
            View targetView = (View) nodeItemInfo.data;
            mCSSBox.setNative(true);
            Map<String, String> nativeProperties = InspectorUtils.dumpNativeViewProperties(targetView);
            updateDisplay(nativeProperties, nativeProperties);
            int pageId = getAnalyzerContext().getCurrentPageId();
            if (pageId == -1 || pageId != nodePageId) {
                return;
            }
            markSingleView(targetView);
        });
        setControlView(findViewById(R.id.btn_analyzer_inspector_ctl_line));
        addDragShieldView(Arrays.asList(findViewById(R.id.analyzer_inspector_attr_display_container), findViewById(R.id.analyzer_inspector_vdom_native_container)));
    }

    private void markSingleView(View view) {
        encircleSingleView(view);
    }

    @Override
    void onShow() {
        super.onShow();
        loadTreeNodesInBackground();
        AnalyzerContext analyzerContext = getAnalyzerContext();
        if (analyzerContext == null) {
            return;
        }
        analyzerContext.addAnalyzerCallback(this);
    }

    @Override
    void onDismiss() {
        super.onDismiss();
        clearMark();
        AnalyzerContext analyzerContext = getAnalyzerContext();
        if (analyzerContext == null) {
            return;
        }
        analyzerContext.removePageChangedCallback(this);
    }

    /**
     * Mark the selected View, the selected View will be marked with a box
     * In a page, one and only one view can be marked, that is, marking a new view will overwrite the old view
     *
     * @param target
     */
    private void encircleSingleView(View target) {
        encircleSingleView(target, COLOR_ENCIRCLE_VIEW);
    }

    private void encircleSingleView(View target, int color) {
        if (mEncircle != null) {
            View view = mEncircle.mView;
            view.getOverlay().remove(mEncircle.mDrawable);
            mEncircle = null;
        }
        if (target == null) {
            return;
        }
        EncircleDrawable encircleDrawable = new EncircleDrawable(color);
        encircleDrawable.attachView(target);
        target.getOverlay().add(encircleDrawable);
        EncircleObject encircleObject = new EncircleObject();
        encircleObject.mView = target;
        encircleObject.mDrawable = encircleDrawable;
        mEncircle = encircleObject;
    }

    public void clearMark() {
        encircleSingleView(null, COLOR_ENCIRCLE_VIEW);
    }

    private void loadTreeNodesInBackground() {
        Executors.computation().execute(() -> loadTreeNodes());
    }

    private void loadTreeNodes() {
        VDocument document = getAnalyzerContext().getCurrentDocument();
        if (document == null) {
            return;
        }
        loadVirtualDoms(document);

        loadNativeViews(document);
    }

    private void loadVirtualDoms(VDocument root) {
        int pageId = root.getComponent().getPageId();
        Map<VElement, TreeNode<NodeItemInfo>> nodes = new HashMap<>();
        Deque<VElement> elements = new ArrayDeque<>();
        elements.add(root);
        while (!elements.isEmpty()) {
            try {
                VElement element = elements.removeFirst();
                boolean isGroup = element instanceof VGroup;
                if (isGroup) {
                    VGroup vGroup = (VGroup) element;
                    @SuppressWarnings("unchecked")
                    List<VElement> children = vGroup.getChildren();
                    if (children.size() > 0) {
                        elements.addAll(children);
                    }
                }
                if (element.getParent() instanceof VDocument && isGroup && TextUtils.equals(VGroup.TAG_BODY, element.getTagName())) {
                    // ignore scroller
                    continue;
                }
                NodeItemInfo nodeItemInfo = InspectorUtils.convertVirtualDomNodeInfo(element);
                nodeItemInfo.pageId = pageId;
                TreeNode<NodeItemInfo> node = new TreeNode<>(nodeItemInfo);
                node.expanded = isGroup && (((VGroup) element).getChildren().size() > 0);
                nodes.put(element, node);
                TreeNode<NodeItemInfo> parentNode = nodes.get(element.getParent());
                if (parentNode != null) {
                    parentNode.addChild(node);
                } else if (element.getParent() != null) {
                    // Associate the child of the scroller to root
                    TreeNode<NodeItemInfo> rootNode = nodes.get(root);
                    if (rootNode != null) {
                        rootNode.addChild(node);
                    }
                }
            } catch (Exception e) {
                // ignore
            }
        }
        TreeNode<NodeItemInfo> rootNode = nodes.get(root);
        nodes.clear();
        if (rootNode == null) {
            return;
        }
        List<TreeNode<NodeItemInfo>> treeNodes = new ArrayList<>();
        treeNodes.add(rootNode);
        treeNodes.addAll(rootNode.getTotalChildren());
        ThreadUtils.runOnUiThread(() -> notifyVirtualDomData(treeNodes));
    }

    private void loadNativeViews(VDocument document) {
        int pageId = document.getComponent().getPageId();
        DecorLayout decorLayout = document.getComponent().getDecorLayout();
        Map<View, TreeNode<NodeItemInfo>> nodes = new HashMap<>();
        Deque<View> views = new ArrayDeque<>();
        views.add(decorLayout);
        while (!views.isEmpty()) {
            try {
                View view = views.removeFirst();
                boolean isGroup = view instanceof ViewGroup;
                if (isGroup) {
                    ViewGroup viewGroup = (ViewGroup) view;
                    int childCount = viewGroup.getChildCount();
                    for (int i = 0; i < childCount; i++) {
                        views.add(viewGroup.getChildAt(i));
                    }
                }
                NodeItemInfo nodeItemInfo = InspectorUtils.convertNativeViewNodeInfo(view);
                nodeItemInfo.pageId = pageId;
                TreeNode<NodeItemInfo> node = new TreeNode<>(nodeItemInfo);
                node.expanded = isGroup && (((ViewGroup) view).getChildCount() > 0);
                nodes.put(view, node);
                TreeNode<NodeItemInfo> parentNode = nodes.get(view.getParent());
                if (parentNode != null) {
                    parentNode.addChild(node);
                }
            } catch (Exception e) {
                // ignore
            }
        }
        TreeNode<NodeItemInfo> rootNode = nodes.get(decorLayout);
        nodes.clear();
        if (rootNode == null) {
            return;
        }
        List<TreeNode<NodeItemInfo>> treeNodes = new ArrayList<>();
        treeNodes.add(rootNode);
        treeNodes.addAll(rootNode.getTotalChildren());
        ThreadUtils.runOnUiThread(() -> notifyNativeViewData(treeNodes));
    }


    private void notifyVirtualDomData(List<TreeNode<NodeItemInfo>> treeNodes) {
        mVirtualDomTree.setData(treeNodes);
    }

    private void notifyNativeViewData(List<TreeNode<NodeItemInfo>> treeNodes) {
        mNativeViewTree.setData(treeNodes);
    }

    private void clearDomData() {
        mVirtualDomTree.setData(null);
    }

    private void clearViewData() {
        mNativeViewTree.setData(null);
    }

    private void updateDisplay(Map<String, String> properties, Map<String, String> cssBoxValues) {
        applyPropertyToCSSBox(cssBoxValues);
        updatePropertiesDisplay(properties);
    }

    private void applyPropertyToCSSBox(Map<String, String> properties) {
        mCSSBox.setMarginLeftText(properties.get(AnalyzerConstant.MARGIN_LEFT));
        mCSSBox.setMarginTopText(properties.get(AnalyzerConstant.MARGIN_TOP));
        mCSSBox.setMarginRightText(properties.get(AnalyzerConstant.MARGIN_RIGHT));
        mCSSBox.setMarginBottomText(properties.get(AnalyzerConstant.MARGIN_BOTTOM));
        mCSSBox.setBorderLeftText(properties.get(AnalyzerConstant.BORDER_LEFT_WIDTH));
        mCSSBox.setBorderTopText(properties.get(AnalyzerConstant.BORDER_BOTTOM_WIDTH));
        mCSSBox.setBorderRightText(properties.get(AnalyzerConstant.BORDER_RIGHT_WIDTH));
        mCSSBox.setBorderBottomText(properties.get(AnalyzerConstant.BORDER_BOTTOM_WIDTH));
        mCSSBox.setPaddingLeftText(properties.get(AnalyzerConstant.PADDING_LEFT));
        mCSSBox.setPaddingTopText(properties.get(AnalyzerConstant.PADDING_TOP));
        mCSSBox.setPaddingRightText(properties.get(AnalyzerConstant.PADDING_RIGHT));
        mCSSBox.setPaddingBottomText(properties.get(AnalyzerConstant.PADDING_BOTTOM));
        mCSSBox.setWidthText(properties.get(AnalyzerConstant.WIDTH));
        mCSSBox.setHeightText(properties.get(AnalyzerConstant.HEIGHT));
        mCSSBox.update();
    }

    private void updatePropertiesDisplay(Map<String, String> properties) {
        mAttrDisplay.removeAllViews();
        Set<Map.Entry<String, String>> entrySet = properties.entrySet();
        for (Map.Entry<String, String> entry : entrySet) {
            String value = entry.getValue();
            if (TextUtils.isEmpty(value)) {
                continue;
            }
            String key = entry.getKey();
            TextView propertyItem = new TextView(getContext());
            propertyItem.setMaxLines(1);
            propertyItem.setSingleLine(true);
            propertyItem.setTextColor(Color.WHITE);
            propertyItem.setTextSize(13);//13sp
            String itemText = key + ": " + value;
            propertyItem.setText(itemText);
            mAttrDisplay.addView(propertyItem);
        }
    }

    @Override
    public void onPageChanged(int oldIndex, int newIndex, Page oldPage, Page newPage) {

    }

    @Override
    public void onPageElementCreateFinish(Page page) {
        AnalyzerThreadManager.getInstance().getMainHandler().postDelayed(this::refreshWhole, 1000);
    }

    @Override
    public void onFeatureInvoke(String name, String action, Object rawParams, String jsCallback, int instanceId) {

    }

    private void refreshWhole() {
        clearDomData();
        clearViewData();
        loadTreeNodesInBackground();
    }

    private static class EncircleObject {
        View mView;
        EncircleDrawable mDrawable;
    }

    private static class EncircleDrawable extends GradientDrawable {

        EncircleDrawable(int color) {
            super();
            Context context = Runtime.getInstance().getContext();
            int borderWidth = DisplayUtil.dip2Pixel(context, 2);
            setStroke(borderWidth, color);
        }

        void attachView(View view) {
            setBounds(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        }
    }
}

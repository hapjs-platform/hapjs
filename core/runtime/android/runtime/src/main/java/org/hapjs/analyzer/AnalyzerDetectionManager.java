/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.analyzer;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import org.hapjs.analyzer.model.NoticeMessage;
import org.hapjs.analyzer.panels.PanelDisplay;
import org.hapjs.analyzer.tools.AnalyzerHelper;
import org.hapjs.analyzer.tools.AnalyzerThreadManager;
import org.hapjs.analyzer.tools.InspectorUtils;
import org.hapjs.render.Page;
import org.hapjs.render.vdom.VDocument;
import org.hapjs.render.vdom.VElement;
import org.hapjs.runtime.R;
import org.hapjs.runtime.Runtime;

import java.util.ArrayList;
import java.util.List;

/**
 * Obtain data, detect anomalies, and issue Notice
 */
public class AnalyzerDetectionManager {
    private static final String TAG = "AnalyzerDetectionManage";
    private static final long SECOND_TO_MILLIS = 1000;
    private static final int RECOMMEND_MAX_FEATURE_INVOKE_NUM = 40;
    public static final String EVENT_FORWARD_PAGE_START = "event_page_forward_start"; // Start of page creation
    public static final String EVENT_FORWARD_PAGE_FINISHED = "event_page_forward_finish"; // End of page creation
    public static final String EVENT_FEATURE_INVOKE = "event_feature_invoke"; // Feature invoke
    private PageForwardInfo mPagePerfInfo = new PageForwardInfo();
    private boolean mIsPageCreating = false;
    private int mFeatureInvokeDuringCreatePage = 0;
    private int mFeatureInvoke = 0;

    private static class Holder {
        public static AnalyzerDetectionManager INSTANCE = new AnalyzerDetectionManager();
    }

    private AnalyzerDetectionManager() {

    }

    public static AnalyzerDetectionManager getInstance() {
        return Holder.INSTANCE;
    }

    public void recordAnalyzerEvent(String event) {
        if (!Analyzer.get().isInit() || TextUtils.isEmpty(event)) {
            return;
        }
        switch (event) {
            case EVENT_FORWARD_PAGE_START:
                mIsPageCreating = true;
                mFeatureInvokeDuringCreatePage = 0;
                mPagePerfInfo.clear();
                mPagePerfInfo.setCreatePageStart(System.currentTimeMillis());
                break;
            case EVENT_FORWARD_PAGE_FINISHED:
                mIsPageCreating = false;
                mPagePerfInfo.setCreatePageFinish(System.currentTimeMillis());
                break;
            case EVENT_FEATURE_INVOKE:
                if (mIsPageCreating) {
                    mFeatureInvokeDuringCreatePage++;
                }
                mFeatureInvoke++;
                break;
            default:
                break;
        }
    }

    /**
     * Get the time of page switching
     * @return
     */
    public String getPageForwardTime() {
        if (mPagePerfInfo != null) {
            long time = mPagePerfInfo.getCreatePageFinish() - mPagePerfInfo.getCreatePageStart();
            if (time > 0 && time < 5 * SECOND_TO_MILLIS) {
                return String.valueOf(time);
            }
        }
        return null;
    }

    /**
     * Detection after 3 seconds:
     * page level + number of off-screen rendering elements + too many subtags (list instead)
     * @param page
     */
    public void detectLayout(Page page) {
        if (page == null) {
            return;
        }
        AnalyzerThreadManager.getInstance().getAnalyzerHandler().postDelayed(() -> {
            AnalyzerContext analyzerContext = Analyzer.get().getAnalyzerContext();
            if (analyzerContext == null) {
                return;
            }
            Page currentPage = analyzerContext.getCurrentPage();
            if (currentPage != page) {
                Log.w(TAG, "AnalyzerPanel_LOG detectLayout, page has changed");
                return;
            }
            VDocument document = analyzerContext.getCurrentDocument();
            if (document == null) {
                return;
            }
            Log.d(TAG, "AnalyzerPanel_LOG detectLayout for page " + page.getName());
            // VDOM level depth detection + children element number detection
            List<Integer> vDomDepthHighLightViews = new ArrayList<>();
            Integer virtualDomLayerCount = InspectorUtils.getVirtualDomLayerCount(document, new InspectorUtils.TraverseCallback<VElement, Integer>(){
                @Override
                public Integer initData() {
                    return 1;
                }

                @Override
                public Integer onTraverseNode(VElement node, int layer, Integer data) {
                    if (layer > InspectorUtils.MAX_VIRTUAL_DOM_DEPTH) {
                        vDomDepthHighLightViews.add(node.getVId());
                    }
                    return Math.max(data, layer);
                }
            });
            if (virtualDomLayerCount > InspectorUtils.MAX_VIRTUAL_DOM_DEPTH) {
                NoticeMessage.UIAction vDomUiAction = new NoticeMessage.UIAction.Builder().pageId(page.getPageId()).componentIds(vDomDepthHighLightViews).build();
                String pageName = page.getName();
                Context context = Analyzer.get().getApplicationContext();
                if (context != null) {
                    String vDomWarn = context.getResources().getQuantityString(R.plurals.analyzer_inspector_vdom_check_warning,
                            InspectorUtils.MAX_VIRTUAL_DOM_DEPTH, pageName, virtualDomLayerCount, InspectorUtils.MAX_VIRTUAL_DOM_DEPTH);
                    NoticeMessage warn = NoticeMessage.warn(pageName, vDomWarn);
                    warn.setAction(vDomUiAction);
                    AnalyzerHelper.getInstance().notice(warn);
                }
            }
            // Native view level detection
            List<View> viewsToHighLightForViewTree = new ArrayList<>();
            int viewTreeLayerCount = InspectorUtils.getViewTreeLayerCount(document.getComponent().getDecorLayout(), new InspectorUtils.TraverseCallback<View, Integer>() {
                @Override
                public Integer initData() {
                    return 1;
                }

                @Override
                public Integer onTraverseNode(View node, int layer, Integer data) {
                    if (layer > InspectorUtils.MAX_VIEW_TREE_DEPTH) {
                        viewsToHighLightForViewTree.add(node);
                    }
                    return Math.max(data, layer);
                }
            });
            NoticeMessage.UIAction viewTreeUiAction = new NoticeMessage.UIAction.Builder().pageId(page.getPageId()).views(viewsToHighLightForViewTree).build();
            if (viewTreeLayerCount > InspectorUtils.MAX_VIEW_TREE_DEPTH) {
                String pageName = page.getName();
                Context context = Analyzer.get().getApplicationContext();
                if (context != null) {
                    String nativeWarn = context.getResources().getQuantityString(R.plurals.analyzer_inspector_native_check_warning,
                            InspectorUtils.MAX_VIEW_TREE_DEPTH, pageName, viewTreeLayerCount, InspectorUtils.MAX_VIEW_TREE_DEPTH);
                    NoticeMessage warn = NoticeMessage.warn(pageName, nativeWarn);
                    warn.setAction(viewTreeUiAction);
                    AnalyzerHelper.getInstance().notice(warn);
                }
            }
            // The proportion of off-screen rendering elements
            float outerViewRatio = InspectorUtils.getOuterViewRatio(document.getComponent().getDecorLayout(), 40);
            if (outerViewRatio >= 0.5) {
                String pageName = page.getName();
                int pageId = page.getPageId();
                Context context = Analyzer.get().getApplicationContext();
                if (context != null) {
                    String nativeWarn = context.getString(R.string.analyzer_outer_view_warning, pageName);
                    NoticeMessage warn = NoticeMessage.warn(pageName, nativeWarn);
                    warn.setClickCallback(message -> {
                        int currentPageId = analyzerContext.getCurrentPageId();
                        if (pageId == currentPageId) {
                            PanelDisplay panelDisplay = analyzerContext.getPanelDisplay();
                            if (panelDisplay != null) {
                                panelDisplay.open3DViews(true, false);
                                Toast.makeText(context, context.getString(R.string.analyzer_outer_view_warning_toast), Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Log.w(TAG, "AnalyzerPanel_LOG Not the current page, do not display 3d view");
                        }
                    });
                    AnalyzerHelper.getInstance().notice(warn);
                }
            }
        }, 3 * SECOND_TO_MILLIS);
    }

    /**
     * Detect whether the number of feature invokes during the startup phase exceeds the threshold
     */
    public void detectFeatureInvoke(Page page) {
        Log.i(TAG, "AnalyzerPanel_LOG detectFeatureInvoke: " + mFeatureInvokeDuringCreatePage);
        if (mFeatureInvokeDuringCreatePage > RECOMMEND_MAX_FEATURE_INVOKE_NUM) {
            String pageName = page.getName();
            Context context = Runtime.getInstance().getContext();
            String nativeWarn = context.getResources().getQuantityString(R.plurals.analyzer_feature_invoke_check_warning,
                    RECOMMEND_MAX_FEATURE_INVOKE_NUM, pageName, mFeatureInvokeDuringCreatePage, RECOMMEND_MAX_FEATURE_INVOKE_NUM);
            NoticeMessage warn = NoticeMessage.warn(pageName, nativeWarn);
            AnalyzerHelper.getInstance().notice(warn);
        }
    }

    /**
     * Get the number of feature invokes in the past period of time
     */
    public int getAndResetFeatureInvokeTimes() {
        int result = mFeatureInvoke;
        mFeatureInvoke = 0;
        return result;
    }

    private static class PageForwardInfo {
        private long mCreatePageStart;
        private long mCreatePageFinish;

        long getCreatePageStart() {
            return mCreatePageStart;
        }

        void setCreatePageStart(long mCreatePageStart) {
            this.mCreatePageStart = mCreatePageStart;
        }

        long getCreatePageFinish() {
            return mCreatePageFinish;
        }

        void setCreatePageFinish(long mCreatePageFinish) {
            this.mCreatePageFinish = mCreatePageFinish;
        }

        public void clear() {
            mCreatePageStart = 0;
            mCreatePageFinish = 0;
        }
    }
}

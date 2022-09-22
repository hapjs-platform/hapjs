/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.analyzer;

import android.text.TextUtils;

import org.hapjs.analyzer.model.NoticeMessage;
import org.hapjs.analyzer.panels.AnalyzerPanel;
import org.hapjs.analyzer.panels.ConsolePanel;
import org.hapjs.analyzer.panels.InspectorPanel;
import org.hapjs.analyzer.panels.NetworkPanel;
import org.hapjs.analyzer.panels.PanelDisplay;
import org.hapjs.runtime.ProviderManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Report the event of the analyzer panel
 */
public class AnalyzerStatisticsManager {
    private static final AnalyzerStatisticsManager INSTANCE = new AnalyzerStatisticsManager();
    public static final String EVENT_DEBUG_APP = "debug_app";
    static final String EVENT_ANALYZER_ENABLE = "analyzer_enable";
    private static final String EVENT_ANALYZER_VIEW3D = "analyzer_view3d_show";
    private static final String EVENT_ANALYZER_QUOTA = "analyzer_quota_show";
    private static final String EVENT_ANALYZER_CONSOLE_SHOW = "analyzer_console_show";
    private static final String EVENT_ANALYZER_INSPECTOR = "analyzer_inspector_show";
    private static final String EVENT_ANALYZER_NET = "analyzer_net_show";
    private static final String EVENT_ANALYZER_NOTICE_COPY = "analyzer_notice_copy";
    private static final String EVENT_ANALYZER_NOTICE_CLICK = "analyzer_notice_click";
    private static final String PARAM_EVENT_NAME = "event_name";
    private static final String PARAM_RPK_PACKAGE = "rpk_pkg";
    private static final String PARAM_NOTICE_NUM = "notice_count";
    private static final String PARAM_NOTICE_MESSAGE_TYPE = "notice_msg_type";
    private static final String PARAM_NOTICE_MESSAGE_CONTENT = "notice_msg_content";
    private static final String PARAM_NOTICE_PAGE_NAME = "notice_page_name";
    private final AnalyzerStatisticsProvider mStatisticsProvider;
    private String mAppPkg;

    public static AnalyzerStatisticsManager getInstance() {
        return INSTANCE;
    }

    private AnalyzerStatisticsManager() {
        mStatisticsProvider = ProviderManager.getDefault().getProvider(AnalyzerStatisticsProvider.NAME);
    }

    void setDebugPackage(String packageName) {
        mAppPkg = packageName;
    }

    private void recordAnalyzerEvent(String event, Map<String, String> params) {
        if (mStatisticsProvider == null || TextUtils.isEmpty(mAppPkg) || TextUtils.isEmpty(event)) {
            return;
        }
        if (params == null) {
            params = new HashMap<>(2);
        }
        params.put(PARAM_EVENT_NAME, event);
        params.put(PARAM_RPK_PACKAGE, mAppPkg);
        mStatisticsProvider.report(mAppPkg, event, params);
    }

    public void recordAnalyzerEvent(String event) {
        recordAnalyzerEvent(event, null);
    }

    public void recordPanelShow(AnalyzerPanel panel) {
        if (panel != null) {
            recordPanelShow(panel.getName());
        }
    }

    public void recordPanelShow(String panelName) {
        String event = "";
        switch (panelName) {
            case ConsolePanel.NAME:
                event = EVENT_ANALYZER_CONSOLE_SHOW;
                break;
            case InspectorPanel.NAME:
                event = EVENT_ANALYZER_INSPECTOR;
                break;
            case PanelDisplay.NAME_VIEW_3D:
                event = EVENT_ANALYZER_VIEW3D;
                break;
            case PanelDisplay.NAME_QUOTA_PANEL:
                event = EVENT_ANALYZER_QUOTA;
                break;
            case NetworkPanel.NAME:
                event = EVENT_ANALYZER_NET;
                break;
            default:
        }
        recordAnalyzerEvent(event);
    }

    public void recordCopyNoticeMessage(int count) {
        Map<String, String> params = new HashMap<>(4);
        params.put(PARAM_NOTICE_NUM, String.valueOf(count));
        recordAnalyzerEvent(EVENT_ANALYZER_NOTICE_COPY, params);
    }

    public void recordNoticeClick(NoticeMessage message) {
        if (message != null) {
            Map<String, String> params = new HashMap<>(8);
            params.put(PARAM_NOTICE_MESSAGE_TYPE, message.getLevel());
            params.put(PARAM_NOTICE_MESSAGE_CONTENT, message.getMessage());
            params.put(PARAM_NOTICE_PAGE_NAME, message.getPageName());
            recordAnalyzerEvent(EVENT_ANALYZER_NOTICE_CLICK, params);
        }
    }
}

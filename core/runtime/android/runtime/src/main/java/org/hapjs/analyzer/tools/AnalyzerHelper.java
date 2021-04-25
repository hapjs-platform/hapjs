/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.analyzer.tools;

import org.hapjs.analyzer.Analyzer;
import org.hapjs.analyzer.AnalyzerContext;
import org.hapjs.analyzer.model.NoticeMessage;
import org.hapjs.analyzer.panels.NoticePanel;
import org.hapjs.analyzer.panels.PanelDisplay;
import org.hapjs.render.Page;

public class AnalyzerHelper {

    private static volatile AnalyzerHelper INSTANCE = null;
    private static final Object LOCK = new Object();

    public static AnalyzerHelper getInstance() {
        if (INSTANCE == null) {
            synchronized (LOCK) {
                if (INSTANCE == null) {
                    INSTANCE = new AnalyzerHelper();
                }
            }
        }
        return INSTANCE;
    }

    private AnalyzerHelper() {
    }

    public boolean isInAnalyzerMode() {
        return Analyzer.get().isInit();
    }

    public Page getCurrentPage(){
        AnalyzerContext analyzerContext = Analyzer.get().getAnalyzerContext();
        return analyzerContext == null ? null : analyzerContext.getCurrentPage();
    }

    public void notice(NoticeMessage message) {
        noticeMessageInMainThread(message);
    }

    private void noticeMessageInMainThread(NoticeMessage message) {
        AnalyzerContext context = Analyzer.get().getAnalyzerContext();
        if (context == null) {
            return;
        }
        AnalyzerThreadManager.getInstance().getMainHandler().post(() -> {
            PanelDisplay panelDisplay = context.getPanelDisplay();
            if (panelDisplay != null) {
                panelDisplay.showNoticePanel();
                NoticePanel noticePanel = panelDisplay.getNoticePanel();
                if (noticePanel != null) {
                    noticePanel.pushNoticeMessage(message);
                }
            }
        });
    }

    /**
     * Calculate the number of pixels based on the string
     * Example of parsed string format: 1920x1200 (from Fresco's RequestListener callback)
     */
    public float parsePixelsNumFromString(String string){
        float size = -1;
        String[] xes = string.split("x");
        try {
            if (xes.length == 2 && Float.parseFloat(xes[0]) > 0 && Float.parseFloat(xes[1]) > 0) {
                size = Float.parseFloat(xes[0]) * Float.parseFloat(xes[1]);
            }
        } catch (Exception ex) {
            // ignore
        }
        return size;
    }
}

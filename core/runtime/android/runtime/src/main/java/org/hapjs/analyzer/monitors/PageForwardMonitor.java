/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.analyzer.monitors;

import android.os.Handler;
import android.text.TextUtils;

import org.hapjs.analyzer.AnalyzerDetectionManager;
import org.hapjs.analyzer.monitors.abs.AbsMonitor;
import org.hapjs.analyzer.tools.AnalyzerThreadManager;

public class PageForwardMonitor extends AbsMonitor<String> {

    public static final String NAME = "pageForward";
    private Handler mMainHandler = AnalyzerThreadManager.getInstance().getMainHandler();

    public PageForwardMonitor() {
        super(NAME);
    }

    @Override
    protected void onStart() {

    }

    @Override
    protected void onStop() {

    }

    public void applyPageForwardTime() {
        Pipeline<String> pipeline = getPipeline();
        if (pipeline == null) {
            return;
        }
        AnalyzerDetectionManager detectionManager = AnalyzerDetectionManager.getInstance();
        if (detectionManager != null) {
            String pageForwardTime = detectionManager.getPageForwardTime();
            if (!TextUtils.isEmpty(pageForwardTime)) {
                mMainHandler.post(() -> pipeline.output(pageForwardTime));
            }
        }
    }
}

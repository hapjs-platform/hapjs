/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.analyzer.monitors;

import android.os.Handler;

import org.hapjs.analyzer.AnalyzerDetectionManager;
import org.hapjs.analyzer.monitors.abs.AbsTimerMonitor;
import org.hapjs.analyzer.tools.AnalyzerThreadManager;

public class FeatureInvokeMonitor extends AbsTimerMonitor<String> {
    public static final String DEFAULT = "-";
    public static final String NAME = "featureInvoke";
    private Handler mMainHandler = AnalyzerThreadManager.getInstance().getMainHandler();

    public FeatureInvokeMonitor() {
        super(NAME, false);
    }

    @Override
    protected void onStart() {
        super.onStart();
        AnalyzerDetectionManager.getInstance().getAndResetFeatureInvokeTimes();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Pipeline<String> pipeline = getPipeline();
        if (pipeline == null) {
            return;
        }
        mMainHandler.post(() -> pipeline.output(DEFAULT));
    }

    @Override
    protected void loop() {
        Pipeline<String> pipeline = getPipeline();
        if (pipeline == null) {
            return;
        }
        AnalyzerDetectionManager detectionManager = AnalyzerDetectionManager.getInstance();
        if (detectionManager != null) {
            int times = detectionManager.getAndResetFeatureInvokeTimes();
            if (times >= 0) {
                mMainHandler.post(() -> pipeline.output(String.valueOf(times)));
            }
        }
    }
}

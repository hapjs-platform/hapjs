/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.analyzer;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;

import org.hapjs.analyzer.monitors.CpuMonitor;
import org.hapjs.analyzer.monitors.FeatureInvokeMonitor;
import org.hapjs.analyzer.monitors.FpsMonitor;
import org.hapjs.analyzer.monitors.LogcatMonitor;
import org.hapjs.analyzer.monitors.MemoryMonitor;
import org.hapjs.analyzer.monitors.PageForwardMonitor;
import org.hapjs.analyzer.monitors.abs.Monitor;
import org.hapjs.bridge.HybridRequest;
import org.hapjs.common.utils.ReflectUtils;
import org.hapjs.render.RootView;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Analyzer {
    private static final String TAG = "Analyzer";
    public static final String USE_ANALYZER = "use_analyzer";
    private static Analyzer INSTANCE = new Analyzer();
    private AnalyzerContext mAnalyzerContext;
    private Application mApplication;
    private ActivityCallback mActivityCallback;
    private String mPackageName;
    private boolean hasInit;

    public static Analyzer get() {
        return INSTANCE;
    }

    private Analyzer() {
    }

    public Application getApplicationContext() {
        return mApplication;
    }

    public void init(RootView rootView, String url) {
        Log.i(TAG, "AnalyzerPanel_LOG init, url: " + url);
        if (!isAnalyzerEnable(url) || rootView == null) {
            // If have enabled the analyzer panel, remove the related view
            if (hasInit) {
                exitAnalyzer();
            }
            return;
        }
        String packageName = new HybridRequest.Builder().uri(url).build().getPackage();
        if (TextUtils.isEmpty(packageName)) {
            Log.d(TAG, "AnalyzerPanel_LOG init analyzer panel fail,the package is empty!");
            return;
        }
        if (TextUtils.equals(mPackageName, packageName) && hasInit) {
            if (mAnalyzerContext != null && mAnalyzerContext.getRootView() != rootView) {
                // Restart debugging when the app process is retained,
                // part of the content in the alarm panel will disappear and will not be displayed after re-detection
                mAnalyzerContext.initAnalyzerContext(rootView);
            }
            return;
        }
        mPackageName = packageName;
        AnalyzerStatisticsManager.getInstance().setDebugPackage(packageName);
        attachRootView(rootView);
        Context application = rootView.getContext().getApplicationContext();
        if (application instanceof Application) {
            mApplication = (Application) application;
            mActivityCallback = new ActivityCallback();
            ((Application) application).registerActivityLifecycleCallbacks(mActivityCallback);
        }
        AnalyzerStatisticsManager.getInstance().recordAnalyzerEvent(AnalyzerStatisticsManager.EVENT_ANALYZER_ENABLE);
        hasInit = true;
    }

    public boolean isInit() {
        return hasInit;
    }

    public String getPackageName() {
        return mPackageName;
    }

    private boolean isAnalyzerEnable(String url) {
        if (TextUtils.isEmpty(url)) {
            return false;
        }
        Uri uri = Uri.parse(url);
        String useAnalyzer = uri.getQueryParameter(USE_ANALYZER);
        try {
            return Boolean.parseBoolean(useAnalyzer);
        } catch (Exception e) {
            //ignore
        }
        return false;
    }

    private void exitAnalyzer() {
        if (mApplication != null && mActivityCallback != null) {
            mApplication.unregisterActivityLifecycleCallbacks(mActivityCallback);
            mActivityCallback = null;
            mApplication = null;
        }

        if (mAnalyzerContext != null) {
            mAnalyzerContext.destroy();
            mAnalyzerContext = null;
        }
        hasInit = false;
        mPackageName = null;
    }

    private void attachRootView(RootView rootView) {
        mAnalyzerContext = new AnalyzerContext(rootView);
        List<Monitor> monitors = createMonitors();
        mAnalyzerContext.attachMonitors(monitors);
    }

    public AnalyzerContext getAnalyzerContext() {
        return mAnalyzerContext;
    }

    private List<Monitor> createMonitors() {
        return Arrays.asList(new FpsMonitor(),
                new MemoryMonitor(),
                new CpuMonitor(),
                new LogcatMonitor(),
                new PageForwardMonitor(),
                new FeatureInvokeMonitor());
    }

    private class ActivityCallback implements Application.ActivityLifecycleCallbacks {

        private int mActivityCount;

        ActivityCallback() {
            Object activityThread = ReflectUtils.callStaticMethod("android.app.ActivityThread", "currentActivityThread", null);
            if (activityThread != null) {
                Object activities = ReflectUtils.getObjectField(activityThread, "mActivities");
                if (activities instanceof ArrayMap) {
                    @SuppressWarnings("unchecked")
                    ArrayMap<IBinder, Object> ac = (ArrayMap<IBinder, Object>) activities;
                    Set<Map.Entry<IBinder, Object>> entries = ac.entrySet();
                    for (Map.Entry<IBinder, Object> entry : entries) {
                        Object activityRecord = entry.getValue();
                        if (activityRecord != null) {
                            Activity activity = (Activity) ReflectUtils.getObjectField(activityRecord, "activity");
                            if (activity != null) {
                                mActivityCount++;
                            }
                        }
                    }
                }
            }
        }

        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            mActivityCount++;
        }

        @Override
        public void onActivityStarted(Activity activity) {

        }

        @Override
        public void onActivityResumed(Activity activity) {

        }

        @Override
        public void onActivityPaused(Activity activity) {

        }

        @Override
        public void onActivityStopped(Activity activity) {

        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

        }

        @Override
        public void onActivityDestroyed(Activity activity) {
            mActivityCount--;
            if (mActivityCount <= 0) {
                exitAnalyzer();
            }
        }
    }
}

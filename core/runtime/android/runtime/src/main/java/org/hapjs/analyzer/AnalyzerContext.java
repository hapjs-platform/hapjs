/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hapjs.analyzer;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;

import org.hapjs.analyzer.monitors.abs.Monitor;
import org.hapjs.analyzer.panels.PanelDisplay;
import org.hapjs.analyzer.tools.AnalyzerThreadManager;
import org.hapjs.bridge.ExtensionManager;
import org.hapjs.bridge.FeatureInvokeListener;
import org.hapjs.bridge.impl.android.AndroidViewClient;
import org.hapjs.common.utils.ThreadUtils;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.model.AppInfo;
import org.hapjs.render.DecorLayout;
import org.hapjs.render.Page;
import org.hapjs.render.PageManager;
import org.hapjs.render.RootView;
import org.hapjs.render.VDomChangeAction;
import org.hapjs.render.jsruntime.JsThread;
import org.hapjs.render.vdom.DocComponent;
import org.hapjs.render.vdom.VDocument;
import org.hapjs.render.vdom.VDomActionApplier;
import org.hapjs.runtime.HapEngine;
import org.hapjs.runtime.R;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;

public class AnalyzerContext {
    private static final String TAG = "AnalyzerContext";
    private Context mContext;
    private ViewGroup mOverlay;
    private SoftReference<RootView> mRootViewRef;
    private List<Monitor> mMonitors;
    private PanelDisplay mPanelDisplay;
    private AppInfo mAppInfo;
    private List<AnalyzerCallback> mPageChangedCallbacks = new ArrayList<>();

    AnalyzerContext(RootView rootView) {
        initAnalyzerContext(rootView);
    }

    final void initAnalyzerContext(RootView rootView) {
        mContext = rootView.getContext();
        mRootViewRef = new SoftReference<>(rootView);
        initDisplay(rootView);
        // use the moment of ACTION_CREATE_FINISH as the end time of page creation
        VDomActionApplier vdomActionApplier = rootView.mVdomActionApplier;
        rootView.mVdomActionApplier = new VDomActionApplier() {
            @Override
            public void applyChangeAction(HapEngine hapEngine, Context context, JsThread jsThread, VDomChangeAction action, VDocument doc, RenderEventCallback renderEventCallback) {
                vdomActionApplier.applyChangeAction(hapEngine, context, jsThread, action, doc, renderEventCallback);
                if (action.action == VDomChangeAction.ACTION_CREATE_FINISH) {
                    //page create finish
                    Page page;
                    PageManager pageManager = getPageManager();
                    if (pageManager != null && (page = pageManager.getPageById(action.pageId)) != null) {
                        for (AnalyzerCallback pageChangedCallback : mPageChangedCallbacks) {
                            pageChangedCallback.onPageElementCreateFinish(page);
                        }
                    }
                }
            }
        };
        AndroidViewClient androidViewClient = rootView.getAndroidViewClient();
        rootView.setAndroidViewClient(new AndroidViewClient() {
            @Override
            public void onRuntimeCreate(RootView view) {
                super.onRuntimeCreate(view);
                if (androidViewClient != null) {
                    androidViewClient.onRuntimeCreate(view);
                }
            }

            @Override
            public void onRuntimeDestroy(RootView view) {
                super.onRuntimeDestroy(view);
                if (androidViewClient != null) {
                    androidViewClient.onRuntimeDestroy(view);
                }
            }

            @Override
            public void onApplicationCreate(RootView view, AppInfo appInfo) {
                super.onApplicationCreate(view, appInfo);
                if (androidViewClient != null) {
                    androidViewClient.onApplicationCreate(rootView, appInfo);
                }
                ThreadUtils.runOnUiThread(() -> onAppCreated(appInfo));
            }

            @Override
            public boolean shouldOverrideUrlLoading(RootView view, String url) {
                if (androidViewClient != null) {
                    return androidViewClient.shouldOverrideUrlLoading(view, url);
                }
                return false;

            }

            @Override
            public void onPageStarted(RootView view, String url) {
                super.onPageStarted(view, url);
                if (androidViewClient != null) {
                    androidViewClient.onPageStarted(view, url);
                }
            }
        });
    }

    /**
     * Initialize PanelDisplay, PanelDisplay mainly manages the display of the panel
     *
     * @param rootView
     */
    private void initDisplay(RootView rootView) {
        if (rootView == null) {
            return;
        }
        // If it has been initialized, only update PanelDisplay
        if (mPanelDisplay != null) {
            mPanelDisplay.reset(rootView);
            return;
        }
        FrameLayout overlay = new FrameLayout(mContext);
        ViewGroup.LayoutParams p = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        overlay.setLayoutParams(p);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            overlay.setForceDarkAllowed(false);
        }
        overlay.setId(R.id.panel_overlay);
        mOverlay = overlay;
        mPanelDisplay = new PanelDisplay(overlay, rootView);
        // RootView is added to content later, so post processing here
        AnalyzerThreadManager.getInstance().getMainHandler().post(() -> {
            ViewGroup parent = (ViewGroup) rootView.getParent();
            if (parent == null || mOverlay == null) {
                return;
            }
            parent.removeView(parent.findViewById(R.id.panel_overlay));
            parent.addView(mOverlay);
        });
    }

    private void onAppCreated(AppInfo appInfo) {
        Log.d(TAG, "AnalyzerPanel_LOG onAppCreated");
        mAppInfo = appInfo;
        if (mPanelDisplay == null) {
            Log.e(TAG, "AnalyzerPanel_LOG onAppCreated fail because mPanelDisplay is null");
            return;
        }
        mPanelDisplay.open();
        // Page life cycle callback
        PageManager pageManager = getPageManager();
        if (pageManager != null) {
            PageManager.PageChangedListener pageChangedListener = pageManager.getPageChangedListener();
            pageManager.setPageChangedListener(new PageManager.PageChangedListener() {
                @Override
                public void onPagePreChange(int oldIndex, int newIndex, Page oldPage, Page newPage) {
                    if (pageChangedListener != null) {
                        pageChangedListener.onPagePreChange(oldIndex, newIndex, oldPage, newPage);
                    }
                }

                @Override
                public void onPageChanged(int oldIndex, int newIndex, Page oldPage, Page newPage) {
                    for (AnalyzerCallback pageChangedCallback : mPageChangedCallbacks) {
                        pageChangedCallback.onPageChanged(oldIndex, newIndex, oldPage, newPage);
                    }
                    if (pageChangedListener != null) {
                        pageChangedListener.onPageChanged(oldIndex, newIndex, oldPage, newPage);
                    }
                }

                @Override
                public void onPageRemoved(int index, Page page) {
                    if (pageChangedListener != null) {
                        pageChangedListener.onPageRemoved(index, page);
                    }
                }
            });
        } else {
            Log.e(TAG, "AnalyzerPanel_LOG register pageChangedListener fail");
        }
        // Feature call callback
        RootView rootView = getRootView();
        if (rootView != null && rootView.getJsThread() != null && rootView.getJsThread().getBridgeManager() != null) {
            ExtensionManager bridgeManager = rootView.getJsThread().getBridgeManager();
            FeatureInvokeListener featureInvokeListener = bridgeManager.getFeatureInvokeListener();
            bridgeManager.setFeatureInvokeListener(new FeatureInvokeListener() {
                @Override
                public void invoke(String name, String action, Object rawParams, String jsCallback, int instanceId) {
                    for (AnalyzerCallback pageChangedCallback : mPageChangedCallbacks) {
                        pageChangedCallback.onFeatureInvoke(name, action, rawParams, jsCallback, instanceId);
                    }
                    if (featureInvokeListener != null) {
                        featureInvokeListener.invoke(name, action, rawParams, jsCallback, instanceId);
                    }
                }
            });
        } else {
            Log.e(TAG, "AnalyzerPanel_LOG register featureInvokeListener fail");
        }
    }

    public RootView getRootView() {
        return mRootViewRef == null ? null : mRootViewRef.get();
    }

    public PageManager getPageManager() {
        RootView rootView = getRootView();
        return rootView == null ? null : rootView.getPageManager();
    }

    public Page getCurrentPage() {
        PageManager pageManager = getPageManager();
        return pageManager == null ? null : pageManager.getCurrPage();
    }

    public AppInfo getAppInfo() {
        return mAppInfo;
    }

    void attachMonitors(List<Monitor> monitors) {
        mMonitors = new ArrayList<>(monitors);
    }

    public Monitor getMonitor(String name) {
        for (Monitor monitor : mMonitors) {
            if (TextUtils.equals(monitor.getName(), name)) {
                return monitor;
            }
        }
        return null;
    }

    private void stopMonitors() {
        for (Monitor monitor : mMonitors) {
            monitor.stop();
        }
    }

    public PanelDisplay getPanelDisplay() {
        return mPanelDisplay;
    }

    public VDocument getCurrentDocument() {
        RootView rootView = getRootView();
        if (rootView == null) {
            return null;
        }
        return rootView.getDocument();
    }

    public int getCurrentPageId() {
        DocComponent rootComponent = getRootComponent();
        if (rootComponent == null) {
            return -1;
        }
        return rootComponent.getPageId();
    }

    public DocComponent getRootComponent() {
        RootView rootView = getRootView();
        if (rootView == null) {
            return null;
        }
        DocComponent root = null;
        if (rootView.getDocument() != null) {
            root = rootView.getDocument().getComponent();
        }
        return root;
    }

    public DecorLayout getDecorLayout() {
        DocComponent rootComponent = getRootComponent();
        if (rootComponent == null) {
            return null;
        }
        return rootComponent.getDecorLayout();
    }

    public void addAnalyzerCallback(AnalyzerCallback callback) {
        if (mPageChangedCallbacks.contains(callback)) {
            return;
        }
        mPageChangedCallbacks.add(callback);
    }

    public void removePageChangedCallback(AnalyzerCallback callback) {
        mPageChangedCallbacks.remove(callback);
    }

    // Callback of the analyzer panel: including page life cycle, Feature call
    public interface AnalyzerCallback {
        void onPageChanged(int oldIndex, int newIndex, Page oldPage, Page newPage);

        void onPageElementCreateFinish(Page page);

        void onFeatureInvoke(String name, String action, Object rawParams, String jsCallback, int instanceId);
    }

    public void destroy() {
        stopMonitors();
        if (mOverlay != null) {
            ViewParent parent = mOverlay.getParent();
            if (parent instanceof ViewGroup) {
                ((ViewGroup) parent).removeView(mOverlay);
                mOverlay = null;
            }
        }
        mPanelDisplay.removeAllHighlight();
        mPanelDisplay.destroyPanel();
        mPanelDisplay = null;
        mPageChangedCallbacks.clear();
        mRootViewRef = null;
    }
}

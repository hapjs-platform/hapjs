/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.analyzer.panels;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import org.hapjs.analyzer.Analyzer;
import org.hapjs.analyzer.AnalyzerContext;
import org.hapjs.analyzer.AnalyzerDetectionManager;
import org.hapjs.analyzer.AnalyzerStatisticsManager;
import org.hapjs.analyzer.monitors.CpuMonitor;
import org.hapjs.analyzer.monitors.FeatureInvokeMonitor;
import org.hapjs.analyzer.monitors.FpsMonitor;
import org.hapjs.analyzer.monitors.MemoryMonitor;
import org.hapjs.analyzer.monitors.PageForwardMonitor;
import org.hapjs.analyzer.monitors.abs.Monitor;
import org.hapjs.analyzer.tools.DragViewManager;
import org.hapjs.analyzer.views.View3D;
import org.hapjs.common.utils.DisplayUtil;
import org.hapjs.render.Page;
import org.hapjs.render.RootView;
import org.hapjs.runtime.R;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class PanelDisplay implements View.OnClickListener, AnalyzerContext.AnalyzerCallback {

    private static final String TAG = "PanelDisplay";
    public static final String NAME_VIEW_3D = "view3D";
    public static final String NAME_QUOTA_PANEL = "quota";
    private static final int DP_QUOTA_BTN_COLLAPSE = 6;
    private static final int DP_QUOTA_BTN_EXPAND = 10;
    private Context mContext;
    private @DragViewManager.HorizontalPosition int mHorizontalPosition = DragViewManager.POSITION_RIGHT;
    private List<AbsPanel> mPanels = new ArrayList<>(4);
    private View mPanelLayout;
    private final View3D mView3D;
    private ViewGroup mPanelTop;
    private ViewGroup mDraggableLayout;
    private ViewGroup mQuotaPanel;
    private ViewGroup mMenu;
    private ViewGroup mPanelBottom;
    private PanelVisibilityChangeCallback mPanelVisibilityChangeCallback;
    private TextView tvCpu;
    private TextView tvMemory;
    private TextView tvFps;
    private TextView tvPageForward;
    private TextView tvFeatureInvoke;
    private View mQuotaBtn;
    private View mInspectorBtn;
    private View mView3DBtn;
    private View mConsoleBtn;
    private View mNetworkBtn;
    private List<View> mQuotaDynamicLayoutBtns = new ArrayList<>(4);
    private View mLogoBtnContainer;
    private View mButtonsContent;
    private View mButtonCollapse;
    private boolean mReShowShowQuota = false;
    private int mBgRightRes = R.drawable.ic_analyzer_main_bar_bg_right;
    private int mBgLeftRes = R.drawable.ic_analyzer_main_bar_bg_left;
    private int mBgRes = R.drawable.ic_analyzer_main_bar_bg;

    public PanelDisplay(ViewGroup overlay, RootView rootView) {
        mContext = overlay.getContext();
        View3D view3D = makeupView3D(overlay, mContext);
        view3D.setTargetView(rootView);
        mView3D = view3D;
        mPanelLayout = makeupPanelLayout(overlay, mContext);
        NetworkPanel networkPanel = preCreateNetworkPanel();
        if (networkPanel != null) {
            networkPanel.initWebSocket();
        }
    }

    /**
     * Reset PanelDisplay
     * 1. 3d view: modify the root element and hide it
     * 2. Clear NoticePanel
     * 3. Clear NetworkPanel data
     *
     * @param newRootView
     */
    public void reset(RootView newRootView) {
        if (mView3D != null) {
            close3DView();
            mView3D.setTargetView(newRootView);
        }
        NoticePanel noticePanel = getNoticePanel();
        if (noticePanel != null) {
            noticePanel.close(false);
        }
        NetworkPanel networkPanel = getPanel(NetworkPanel.class);
        if (networkPanel != null) {
            networkPanel.reset();
        }
    }

    private View3D makeupView3D(ViewGroup overlay, Context context) {
        View3D view3D = new View3D(context);
        view3D.setBackgroundColor(Color.WHITE);
        FrameLayout.LayoutParams view3DParam = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        view3D.setLayoutParams(view3DParam);
        // view3D is below the panel and above the RootView.
        overlay.addView(view3D);
        view3D.setVisibility(View.GONE);
        return view3D;
    }

    private View makeupPanelLayout(ViewGroup overlay, Context context) {
        View panelLayout = LayoutInflater.from(context).inflate(R.layout.layout_analyzer_main, overlay, false);
        mPanelTop = panelLayout.findViewById(R.id.analyzer_main_container_top);
        mPanelBottom = panelLayout.findViewById(R.id.analyzer_main_container_bottom);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        overlay.addView(panelLayout, params);
        return panelLayout;
    }

    void addTopPanel(AnalyzerPanel panel) {
        addPanelInternal(mPanelTop, panel.getPanelView());
    }

    void addBottomPanel(AnalyzerPanel panel) {
        addPanelInternal(mPanelBottom, panel.getPanelView());
    }

    private void addPanelInternal(ViewGroup parent, View panelView) {
        parent.addView(panelView);
        panelView.setTag(parent);
    }

    void removePanel(AnalyzerPanel panel) {
        View panelView = panel.getPanelView();
        ViewGroup parent = (ViewGroup) panelView.getTag();
        if (parent != null) {
            parent.removeView(panelView);
        }
    }

    boolean isPanelShowing(AnalyzerPanel panel) {
        View panelView = panel.getPanelView();
        ViewParent parent = panelView.getParent();
        return parent == mPanelTop || parent == mPanelBottom;
    }

    public void open() {
        mQuotaBtn = mPanelLayout.findViewById(R.id.btn_analyzer_quota);
        mView3DBtn = mPanelLayout.findViewById(R.id.btn_analyzer_view3d);
        mInspectorBtn = mPanelLayout.findViewById(R.id.btn_analyzer_view_inspector);
        mConsoleBtn = mPanelLayout.findViewById(R.id.btn_analyzer_console);
        mNetworkBtn = mPanelLayout.findViewById(R.id.btn_analyzer_network);
        mButtonsContent = mPanelLayout.findViewById(R.id.analyzer_main_menu_buttons);
        View mLogoBtn = mPanelLayout.findViewById(R.id.launch_logo_btn);
        mLogoBtnContainer = mPanelLayout.findViewById(R.id.analyzer_main_logo_btn_container);
        mButtonCollapse = mPanelLayout.findViewById(R.id.btn_analyzer_buttons_collapse);
        mQuotaDynamicLayoutBtns.add(mView3DBtn);
        mQuotaDynamicLayoutBtns.add(mInspectorBtn);
        mQuotaDynamicLayoutBtns.add(mConsoleBtn);
        mQuotaDynamicLayoutBtns.add(mNetworkBtn);
        mQuotaBtn.setOnClickListener(this);
        mInspectorBtn.setOnClickListener(this);
        mView3DBtn.setOnClickListener(this);
        mConsoleBtn.setOnClickListener(this);
        mNetworkBtn.setOnClickListener(this);
        mLogoBtn.setOnClickListener(this);
        mButtonCollapse.setOnClickListener(this);
        tvCpu = mPanelLayout.findViewById(R.id.launch_panel_cpu_value);
        tvMemory = mPanelLayout.findViewById(R.id.launch_panel_memory_value);
        tvFps = mPanelLayout.findViewById(R.id.launch_panel_frame_value);
        tvPageForward = mPanelLayout.findViewById(R.id.launch_panel_page_forward_value);
        tvFeatureInvoke = mPanelLayout.findViewById(R.id.launch_panel_feature_invoke_value);
        setPanelVisibilityChangeCallback((panel, visible) -> {
            if (panel instanceof InspectorPanel && !visible) {
                mInspectorBtn.setSelected(false);
            } else if (panel instanceof ConsolePanel && !visible) {
                mConsoleBtn.setSelected(false);
            } else if (panel instanceof NetworkPanel && !visible) {
                mNetworkBtn.setSelected(false);
            }
        });
        mDraggableLayout = mPanelLayout.findViewById(R.id.analyzer_main_menu_quota_container);
        mQuotaPanel = mPanelLayout.findViewById(R.id.analyzer_main_quota_panel);
        mMenu = mPanelLayout.findViewById(R.id.analyzer_main_menu_layout);
        DragViewManager dragViewManager = new DragViewManager(mDraggableLayout, Arrays.asList(mLogoBtn, mLogoBtnContainer));
        dragViewManager.setOnDragListener(new DragViewManager.OnDragListener() {
            @Override
            public void onMoveToEdge(View target, int lastPosition, int newPosition) {
                Log.d(TAG, "AnalyzerPanel_LOG PanelDisplay onMoveToEdge: " + lastPosition + " -> " + newPosition);
                if(lastPosition != mHorizontalPosition){
                    Log.e(TAG, "AnalyzerPanel_LOG PanelDisplay onMoveToEdge: lastPosition != mHorizontalPosition");
                    return;
                }
                if(lastPosition != DragViewManager.POSITION_UNKNOWN && newPosition != DragViewManager.POSITION_UNKNOWN && lastPosition != newPosition){
                    View parent = (View) mDraggableLayout.getParent();
                    ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) mDraggableLayout.getLayoutParams();
                    if (newPosition == DragViewManager.POSITION_LEFT) {
                        layoutParams.endToEnd = -1;
                        layoutParams.startToStart = parent.getId();
                    } else if (newPosition == DragViewManager.POSITION_RIGHT) {
                        layoutParams.startToStart = -1;
                        layoutParams.endToEnd = parent.getId();
                    }
                    mDraggableLayout.setLayoutParams(layoutParams);
                    target.setTranslationX(0);
                    // reverse the index between quota panel and menu
                    int quotaIndex = mDraggableLayout.indexOfChild(mQuotaPanel);
                    if (mDraggableLayout.getChildCount() == 2 && (quotaIndex == 1 || quotaIndex == 0)) {
                        LinearLayout.LayoutParams quotaPanelLayoutParams = (LinearLayout.LayoutParams) mQuotaPanel.getLayoutParams();
                        int marginStart = quotaPanelLayoutParams.getMarginStart();
                        // reverse margin values between left and right
                        quotaPanelLayoutParams.setMarginStart(quotaPanelLayoutParams.getMarginEnd());
                        quotaPanelLayoutParams.setMarginEnd(marginStart);
                        mDraggableLayout.removeView(mQuotaPanel);
                        mQuotaPanel.setLayoutParams(quotaPanelLayoutParams);
                        mDraggableLayout.addView(mQuotaPanel, quotaIndex == 1 ? 0 : -1);
                    } else {
                        Log.e(TAG, "AnalyzerPanel_LOG PanelDisplay count of draggableLayout's children is invalid, " + mDraggableLayout.getChildCount());
                    }
                    // reverse the index between menu content and collapse btn
                    if (mMenu.getChildCount() == 2) {
                        int collapseIndex = mMenu.indexOfChild(mButtonCollapse);
                        mMenu.removeView(mButtonCollapse);
                        mButtonCollapse.setPadding(mButtonCollapse.getPaddingRight(), mButtonCollapse.getPaddingTop(), mButtonCollapse.getPaddingLeft(), mButtonCollapse.getPaddingBottom());
                        mButtonsContent.setPadding(mButtonsContent.getPaddingRight(), mButtonsContent.getPaddingTop(), mButtonsContent.getPaddingLeft(), mButtonsContent.getPaddingBottom());
                        mMenu.addView(mButtonCollapse, collapseIndex == 1 ? 0 : -1);
                        int bgRes = newPosition == DragViewManager.POSITION_RIGHT ? mBgRightRes : mBgLeftRes;
                        mMenu.setBackgroundResource(bgRes);
                        mLogoBtnContainer.setBackgroundResource(bgRes);
                    } else {
                        Log.e(TAG, "AnalyzerPanel_LOG PanelDisplay count of menu's children is invalid, " + mMenu.getChildCount());
                    }
                }
                mLogoBtnContainer.setBackgroundResource(newPosition == DragViewManager.POSITION_RIGHT ? mBgRightRes : mBgLeftRes);
                if (mReShowShowQuota) {
                    mQuotaPanel.setVisibility(View.VISIBLE);
                    mReShowShowQuota = false;
                }
                mHorizontalPosition = newPosition;
            }

            @Override
            public void onDragging(View target) {
                if (mQuotaPanel.getVisibility() == View.VISIBLE) {
                    mQuotaPanel.setVisibility(View.GONE);
                    mReShowShowQuota = true;
                }
                mLogoBtnContainer.setBackgroundResource(mBgRes);
            }
        });
        AnalyzerContext analyzerContext = Analyzer.get().getAnalyzerContext();
        if (analyzerContext != null) {
            analyzerContext.addAnalyzerCallback(this);
        }
    }

    /**
     * Open 3d view
     * @param showOutLine Whether to display the screen outline
     */
    public void open3DViews(boolean showOutLine, boolean report) {
        mView3D.setVisibility(View.VISIBLE);
        mView3D.setLayerInteractionEnabled(true);
        mView3D.setOutLine(showOutLine);
        mLogoBtnContainer.setVisibility(View.GONE);
        mMenu.setVisibility(View.VISIBLE);
        mView3DBtn.setSelected(true);
        if (report) {
            AnalyzerStatisticsManager.getInstance().recordPanelShow(NAME_VIEW_3D);
        }
    }

    private void showInspectorPanel() {
        if (isConsolePanelShowing()) {
            dismissConsolePanel();
        }
        if (isNetworkPanelShowing()) {
            dismissNetworkPanel();
        }
        showPanel(InspectorPanel.class, true, () -> {
            FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            p.gravity = Gravity.BOTTOM;
            return p;
        });
    }

    private boolean isInspectorPanelShowing() {
        InspectorPanel inspectorPanel = getPanel(InspectorPanel.class);
        return inspectorPanel != null && inspectorPanel.isShowing();
    }

    private void dismissInspectorPanel() {
        dismissPanel(InspectorPanel.class);
    }

    public InspectorPanel getInspectorPanel() {
        return getPanel(InspectorPanel.class);
    }

    public void showNoticePanel() {
        showPanel(NoticePanel.class, true, () -> {
            FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            p.gravity = Gravity.TOP;
            return p;
        });
    }

    public boolean isNoticePanelShowing() {
        NoticePanel noticePanel = getPanel(NoticePanel.class);
        return noticePanel != null && noticePanel.isShowing();
    }

    public void dismissNoticePanel() {
        dismissPanel(NoticePanel.class);
    }

    public NoticePanel getNoticePanel() {
        return getPanel(NoticePanel.class);
    }

    private void showConsolePanel() {
        if (isInspectorPanelShowing()) {
            dismissInspectorPanel();
        }
        if (isNetworkPanelShowing()) {
            dismissNetworkPanel();
        }
        showPanel(ConsolePanel.class, true, () -> {
            FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            p.gravity = Gravity.BOTTOM;
            return p;
        });
    }

    private boolean isConsolePanelShowing() {
        ConsolePanel consolePanel = getPanel(ConsolePanel.class);
        return consolePanel != null && consolePanel.isShowing();
    }

    void dismissConsolePanel() {
        dismissPanel(ConsolePanel.class);
    }

    private void showNetworkPanel() {
        if (isInspectorPanelShowing()) {
            dismissInspectorPanel();
        }
        if (isConsolePanelShowing()) {
            dismissConsolePanel();
        }
        showPanel(NetworkPanel.class, true, () -> {
            FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            p.gravity = Gravity.BOTTOM;
            return p;
        });
    }

    private NetworkPanel preCreateNetworkPanel() {
        NetworkPanel panel = createPanel(NetworkPanel.class, () -> {
            FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            p.gravity = Gravity.BOTTOM;
            return p;
        });
        return panel;
    }

    private boolean isNetworkPanelShowing() {
        NetworkPanel networkPanel = getPanel(NetworkPanel.class);
        return networkPanel != null && networkPanel.isShowing();
    }

    private void dismissNetworkPanel() {
        dismissPanel(NetworkPanel.class);
    }

    private boolean isView3DShowing(){
        return mView3DBtn.isSelected();
    }

    private <T extends AbsPanel> T showPanel(Class<T> cls, boolean animation, InitialPanelParamCallback paramCallback) {
        T panel = getPanel(cls);
        if (panel == null) {
            panel = createPanel(cls, paramCallback);
        }
        if (panel != null && !panel.isShowing()) {
            panel.show(animation);
        }
        return panel;
    }

    private void dismissPanel(Class<? extends AbsPanel> cls) {
        AnalyzerPanel panel = getPanel(cls);
        if (panel == null || !panel.isShowing()) {
            return;
        }
        panel.dismiss();
    }

    void onPanelDismiss(AnalyzerPanel panel) {
        if (mPanelVisibilityChangeCallback != null) {
            mPanelVisibilityChangeCallback.onVisibleChange(panel, false);
        }
    }

    void onPanelShow(AnalyzerPanel panel) {
        if (mPanelVisibilityChangeCallback != null) {
            mPanelVisibilityChangeCallback.onVisibleChange(panel, true);
        }
    }

    private <T extends AbsPanel> T createPanel(Class<T> cls, @Nullable InitialPanelParamCallback paramCallback) {
        try {
            Constructor<T> constructor = cls.getConstructor(Context.class);
            T panel = constructor.newInstance(mContext);
            if (paramCallback != null) {
                panel.setLayoutParams(paramCallback.createParams());
            }
            mPanels.add(panel);
            return panel;
        } catch (Exception e) {
            Log.e(TAG, "AnalyzerPanel_LOG PanelDisplay createPanel fail", e);
        }
        return null;
    }

    private <T extends AbsPanel> T getPanel(Class<T> cls) {
        for (AbsPanel panel : mPanels) {
            if (cls.isInstance(panel)) {
                return (T) panel;
            }
        }
        return null;
    }

    public void destroyPanel() {
        for (AbsPanel panel : mPanels) {
            panel.onDestroy();
        }
        mPanels.clear();
    }

    public void removeAllHighlight() {
        NoticePanel noticePanel = getPanel(NoticePanel.class);
        if (noticePanel != null) {
            noticePanel.removeAllNoticeHighlight();
        }
        InspectorPanel inspectorPanel = getPanel(InspectorPanel.class);
        if (inspectorPanel != null) {
            inspectorPanel.clearMark();
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (R.id.btn_analyzer_quota == id) {
            mQuotaBtn.setSelected(!mQuotaBtn.isSelected());
            boolean selected = mQuotaBtn.isSelected();
            if (selected) {
                openQuotaPanel();
            } else {
                closeQuotaPanel();
            }
            // Dynamically adjust the margin of the menu button
            int marginStartPx = DisplayUtil.dip2Pixel(mContext, selected ? DP_QUOTA_BTN_COLLAPSE : DP_QUOTA_BTN_EXPAND);
            LinearLayout.LayoutParams layoutParams;
            for (View btn : mQuotaDynamicLayoutBtns) {
                layoutParams = (LinearLayout.LayoutParams) btn.getLayoutParams();
                layoutParams.setMarginStart(marginStartPx);
                btn.setLayoutParams(layoutParams);
            }
        } else if (R.id.btn_analyzer_view_inspector == id) {
            mInspectorBtn.setSelected(!mInspectorBtn.isSelected());
            if (mInspectorBtn.isSelected()) {
                showInspectorPanel();
            } else {
               dismissInspectorPanel();
            }
        } else if (R.id.btn_analyzer_view3d == id) {
            if (mView3DBtn.isSelected()) {
                close3DView();
            } else {
                show3DView();
            }
        } else if (R.id.btn_analyzer_console == id) {
            mConsoleBtn.setSelected(!mConsoleBtn.isSelected());
            if (mConsoleBtn.isSelected()) {
                showConsolePanel();
            } else {
                dismissConsolePanel();
            }
        } else if (R.id.btn_analyzer_network == id) {
            mNetworkBtn.setSelected(!mNetworkBtn.isSelected());
            if (mNetworkBtn.isSelected()) {
                showNetworkPanel();
            } else {
                dismissNetworkPanel();
            }
        } else if (R.id.launch_logo_btn == id) {
            mLogoBtnContainer.setVisibility(View.GONE);
            mMenu.setVisibility(View.VISIBLE);
        } else if (R.id.btn_analyzer_buttons_collapse == id) {
            mMenu.setVisibility(View.GONE);
            mLogoBtnContainer.setVisibility(View.VISIBLE);
        }
    }

    private void openQuotaPanel() {
        mQuotaPanel.setVisibility(View.VISIBLE);
        FpsMonitor fpsMonitor = getMonitor(FpsMonitor.NAME);
        if (fpsMonitor != null) {
            fpsMonitor.setPipeline(data -> tvFps.setText(String.valueOf(data)));
            fpsMonitor.start();
        }

        MemoryMonitor memoryMonitor = getMonitor(MemoryMonitor.NAME);
        if (memoryMonitor != null) {
            memoryMonitor.setPipeline(data -> tvMemory.setText(String.format(Locale.US, "%.0fm", data)));
            memoryMonitor.start();
        }

        CpuMonitor cpuMonitor = getMonitor(CpuMonitor.NAME);
        if (cpuMonitor != null) {
            cpuMonitor.setPipeline(data -> tvCpu.setText(data));
            cpuMonitor.start();
        }

        PageForwardMonitor pageForwardMonitor = getMonitor(PageForwardMonitor.NAME);
        if (pageForwardMonitor != null) {
            pageForwardMonitor.setPipeline(data -> tvPageForward.setText(String.format(Locale.US, "%sms", data)));
            pageForwardMonitor.applyPageForwardTime();
        }

        FeatureInvokeMonitor featureInvokeMonitor = getMonitor(FeatureInvokeMonitor.NAME);
        if (featureInvokeMonitor != null) {
            featureInvokeMonitor.setPipeline(data -> tvFeatureInvoke.setText(data));
            featureInvokeMonitor.start();
        }
        AnalyzerStatisticsManager.getInstance().recordPanelShow(NAME_QUOTA_PANEL);
    }

    private void closeQuotaPanel() {
        mQuotaPanel.setVisibility(View.GONE);
        FpsMonitor fpsMonitor = getMonitor(FpsMonitor.NAME);
        if (fpsMonitor != null) {
            fpsMonitor.setPipeline(null);
            fpsMonitor.stop();
        }

        MemoryMonitor memoryMonitor = getMonitor(MemoryMonitor.NAME);
        if (memoryMonitor != null) {
            memoryMonitor.setPipeline(null);
            memoryMonitor.stop();
        }

        CpuMonitor cpuMonitor = getMonitor(CpuMonitor.NAME);
        if (cpuMonitor != null) {
            cpuMonitor.setPipeline(null);
            cpuMonitor.stop();
        }
        PageForwardMonitor pageForwardMonitor = getMonitor(PageForwardMonitor.NAME);
        if (pageForwardMonitor != null) {
            pageForwardMonitor.setPipeline(null);
            pageForwardMonitor.stop();
        }
        FeatureInvokeMonitor featureInvokeMonitor = getMonitor(FeatureInvokeMonitor.NAME);
        if (featureInvokeMonitor != null) {
            featureInvokeMonitor.setPipeline(null);
            featureInvokeMonitor.stop();
        }
    }

    private void close3DView() {
        if (mView3DBtn.isSelected()) {
            mView3DBtn.setSelected(false);
            mView3D.setVisibility(View.GONE);
            mView3D.setLayerInteractionEnabled(false);
            mView3D.reset();
        }
    }

    private void show3DView() {
        if (!mView3DBtn.isSelected()) {
            mView3DBtn.setSelected(true);
            open3DViews(false, true);
        }
    }

    /**
     * If the 3d view is being displayed, hide it and intercept the event
     */
    public boolean onBackPressed() {
        if (isView3DShowing()) {
            close3DView();
            return true;
        } else {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends Monitor> T getMonitor(String name) {
        AnalyzerContext analyzerContext = Analyzer.get().getAnalyzerContext();
        return (T)analyzerContext.getMonitor(name);
    }

    @Override
    public void onPageChanged(int oldIndex, int newIndex, Page oldPage, Page newPage) {
        if (newIndex > oldIndex) {
            AnalyzerDetectionManager.getInstance().recordAnalyzerEvent(AnalyzerDetectionManager.EVENT_FORWARD_PAGE_START);
        }
    }

    @Override
    public void onPageElementCreateFinish(Page page) {
        // refresh the data of page forward
        AnalyzerDetectionManager.getInstance().recordAnalyzerEvent(AnalyzerDetectionManager.EVENT_FORWARD_PAGE_FINISHED);
        PageForwardMonitor pageForwardMonitor = getMonitor(PageForwardMonitor.NAME);
        if (pageForwardMonitor != null) {
            pageForwardMonitor.applyPageForwardTime();
        }
        AnalyzerDetectionManager.getInstance().detectLayout(page);
        AnalyzerDetectionManager.getInstance().detectFeatureInvoke(page);
    }

    @Override
    public void onFeatureInvoke(String name, String action, Object rawParams, String jsCallback,
                                int instanceId) {
        AnalyzerDetectionManager.getInstance().recordAnalyzerEvent(AnalyzerDetectionManager.EVENT_FEATURE_INVOKE);
    }

    interface InitialPanelParamCallback {
        @NonNull
        ViewGroup.LayoutParams createParams();
    }

    private void setPanelVisibilityChangeCallback(PanelVisibilityChangeCallback panelVisibilityChangeCallback) {
        mPanelVisibilityChangeCallback = panelVisibilityChangeCallback;
    }

    public interface PanelVisibilityChangeCallback {
        void onVisibleChange(AnalyzerPanel panel, boolean visible);
    }
}

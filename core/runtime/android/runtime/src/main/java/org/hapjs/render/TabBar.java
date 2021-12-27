/*
 * Copyright (c) 2023, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hapjs.render;

import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.hapjs.bridge.HybridRequest;
import org.hapjs.common.utils.ColorUtil;
import org.hapjs.common.utils.RouterUtils;
import org.hapjs.common.utils.ThreadUtils;
import org.hapjs.model.AppInfo;
import org.hapjs.model.DisplayInfo;
import org.hapjs.model.TabBarInfo;
import org.hapjs.render.jsruntime.JsThread;
import org.hapjs.runtime.R;
import org.hapjs.runtime.adapter.TabBarItemAdapter;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.hapjs.logging.RuntimeLogManager.VALUE_ROUTER_APP_FROM_ROUTER;


public class TabBar {
    private static final String TAG = "TabBar";
    private static final int DEFAULT_TABBAR_HEIGHT = 48;
    private List<TabBarInfo> mTabBarItemDatas = new ArrayList<>();
    private static Object LOCK_TABBAR_DATA = new Object();
    private View mTabBarView = null;

    public void initTabBarView(RootView rootView) {
        if (null == rootView) {
            Log.w(TAG, "initTabBarView rootView is null.");
            return;
        }
        AppInfo appInfo = rootView.getAppInfo();
        Context context = rootView.getContext();
        if (null == appInfo || null == context) {
            Log.w(TAG, "initTabBarView appInfo or context is null.");
            return;
        }
        String isValidTabBarStr = getDefaultStyle(appInfo, DisplayInfo.Style.KEY_TABBAR_DATA, null, null);
        boolean isValidTabBar = DisplayInfo.KEY_VALID.equals(isValidTabBarStr);
        if (!isValidTabBar) {
            return;
        }
        mTabBarView = rootView.findViewById(R.id.tabbar_container);
        if (null == mTabBarView) {
            mTabBarView = LayoutInflater.from(context).inflate(R.layout.tabbar_view, null);
            int tabbarHeight = (int) (DEFAULT_TABBAR_HEIGHT *
                    context.getResources().getDisplayMetrics().density);
            FrameLayout.LayoutParams tabbarParams =
                    new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, tabbarHeight);
            tabbarParams.gravity = Gravity.BOTTOM;
            rootView.addView(mTabBarView, tabbarParams);
        }
        initTabBarList(context, rootView, mTabBarView, appInfo);
    }

    public void updateTabBarData(RootView rootView, JSONObject tabbarData) {
        if (null == tabbarData || null == rootView) {
            Log.w(TAG, "updateTabBarData tabbarData or rootView is null.");
            return;
        }
        int index = -1;
        String tabbarName = "";
        String iconPath = "";
        String iconSelectedPath = "";
        String pagePath = "";
        String pageParams = "";
        if (tabbarData.has(TabBarInfo.KEY_TABBAR_INDEX)) {
            try {
                index = tabbarData.getInt(TabBarInfo.KEY_TABBAR_INDEX);
            } catch (Exception e) {
                Log.w(TAG, "updateTabBarData KEY_TABBAR_INDEX error : " + e.getMessage());
            }
        }
        if (index == -1) {
            Log.w(TAG, "updateTabBarData index is not valid.");
            return;
        }
        if (tabbarData.has(TabBarInfo.KEY_TABBAR_TEXT)) {
            try {
                tabbarName = tabbarData.getString(TabBarInfo.KEY_TABBAR_TEXT);
            } catch (Exception e) {
                Log.w(TAG, "updateTabBarData KEY_TABBAR_TEXT error : " + e.getMessage());
            }
        }
        if (tabbarData.has(TabBarInfo.KEY_TABBAR_ICONPATH)) {
            try {
                iconPath = tabbarData.getString(TabBarInfo.KEY_TABBAR_ICONPATH);
            } catch (Exception e) {
                Log.w(TAG, "updateTabBarData KEY_TABBAR_ICONPATH error : " + e.getMessage());
            }
        }
        if (tabbarData.has(TabBarInfo.KEY_TABBAR_SELECTEDICONPATH)) {
            try {
                iconSelectedPath = tabbarData.getString(TabBarInfo.KEY_TABBAR_SELECTEDICONPATH);
            } catch (Exception e) {
                Log.w(TAG, "updateTabBarData KEY_TABBAR_SELECTEDICONPATH error : " + e.getMessage());
            }
        }
        if (tabbarData.has(TabBarInfo.KEY_TABBAR_PAGE_PATH)) {
            try {
                pagePath = tabbarData.getString(TabBarInfo.KEY_TABBAR_PAGE_PATH);
            } catch (Exception e) {
                Log.w(TAG, "updateTabBarData KEY_TABBAR_PAGEPATH error : " + e.getMessage());
            }
        }
        if (tabbarData.has(TabBarInfo.KEY_TABBAR_PAGEPARAMS)) {
            try {
                pageParams = tabbarData.getString(TabBarInfo.KEY_TABBAR_PAGEPARAMS);
            } catch (Exception e) {
                Log.w(TAG, "updateTabBarData KEY_TABBAR_PAGEPARAMS error : " + e.getMessage());
            }
        }
        final int realIndex = index;
        TabBarInfo tabBarInfo = null;
        synchronized (LOCK_TABBAR_DATA) {
            if (realIndex < mTabBarItemDatas.size()) {
                tabBarInfo = mTabBarItemDatas.get(realIndex);
            }
        }
        if (null != tabBarInfo) {
            if (!TextUtils.isEmpty(tabbarName)) {
                tabBarInfo.setTabBarText(tabbarName);
            }
            if (!TextUtils.isEmpty(iconPath)) {
                tabBarInfo.setTabBarIconPath(iconPath);
                if (null != rootView) {
                    tabBarInfo.setTabBarIconUri(rootView.tryParseUri(iconPath));
                } else {
                    Log.w(TAG, "updateTabBarData setTabBarIconUri  rootView is not valid.");
                }
            }
            if (!TextUtils.isEmpty(iconSelectedPath)) {
                tabBarInfo.setTabBarSelectedIconPath(iconSelectedPath);
                if (null != rootView) {
                    tabBarInfo.setTabBarSelectedIconUri(rootView.tryParseUri(iconSelectedPath));
                } else {
                    Log.w(TAG, "updateTabBarData setTabBarSelectedIconUri  rootView is not valid.");
                }
            }
            if (!TextUtils.isEmpty(pagePath)) {
                tabBarInfo.setTabBarPagePath(pagePath);
            }
            if (!TextUtils.isEmpty(pageParams)) {
                tabBarInfo.setTabBarPageParams(pageParams);
            }
            ThreadUtils.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    RecyclerView tabBarRecyclerView = rootView.findViewById(R.id.tabbar_recyclerview);
                    if (null != tabBarRecyclerView) {
                        RecyclerView.Adapter adapter = tabBarRecyclerView.getAdapter();
                        if (adapter instanceof TabBarItemAdapter && realIndex < adapter.getItemCount()) {
                            adapter.notifyItemChanged(realIndex);
                        } else {
                            Log.w(TAG, "updateTabBarData adapter null or index error : " + realIndex);
                        }
                    } else {
                        Log.w(TAG, "updateTabBarData tabBarRecyclerView null.");
                    }
                }
            });
        }
    }

    private String getDefaultStyle(AppInfo appInfo, String key, String extraValue, String defaultValue) {
        if (null == appInfo) {
            Log.w(TAG, "getDefaultStyle appInfo is null.");
            return defaultValue;
        }
        DisplayInfo displayInfo = appInfo.getDisplayInfo();
        if (displayInfo == null) {
            return defaultValue;
        }

        String parseValue = extraValue;
        if (TextUtils.isEmpty(parseValue)) {
            DisplayInfo.Style defaultStyle = displayInfo.getDefaultStyle();
            if (defaultStyle != null) {
                parseValue = defaultStyle.get(key);
            }
        }
        if (TextUtils.isEmpty(parseValue)) {
            return defaultValue;
        }
        return parseValue;
    }

    private void initTabBarList(Context context, RootView rootView, View parentView, AppInfo appInfo) {
        if (null == context || null == parentView
                || null == appInfo || null == rootView) {
            Log.w(TAG, "initTabBarList context parentView or appInfo or rootView is null.");
            return;
        }
        String colorStr = getDefaultStyle(appInfo, DisplayInfo.Style.KEY_TABBAR_COLOR, null, null);
        String selectedColorStr = getDefaultStyle(appInfo, DisplayInfo.Style.KEY_TABBAR_SELECTEDCOLOR, null, null);
        String backgroundColorStr = getDefaultStyle(appInfo, DisplayInfo.Style.KEY_TABBAR_BGCOLOR, null, null);
        int textColor = Color.BLACK;
        int selectedTextColor = Color.BLACK;
        int backgroundColor = Color.WHITE;
        Page curPage = null;
        String curPath = null;
        if (!TextUtils.isEmpty(colorStr)) {
            textColor = ColorUtil.getColor(colorStr, Color.BLACK);
        }
        if (!TextUtils.isEmpty(selectedColorStr)) {
            selectedTextColor = ColorUtil.getColor(selectedColorStr, Color.BLACK);
        }
        if (!TextUtils.isEmpty(backgroundColorStr)) {
            backgroundColor = ColorUtil.getColor(backgroundColorStr, Color.WHITE);
        }
        parentView.setBackgroundColor(backgroundColor);
        DisplayInfo displayInfo = appInfo.getDisplayInfo();
        PageManager pageManager = rootView.getPageManager();
        if (null != pageManager) {
            curPage = pageManager.getCurrPage();
        }
        if (null != curPage) {
            curPath = curPage.getPath();
        }
        List<TabBarInfo> tabBarInfos = null;
        boolean isTabPath = false;
        if (null != displayInfo) {
            tabBarInfos = displayInfo.getTabBarInfos();
            TabBarInfo tabBarInfo = null;
            String iconPath = "";
            String selectedIconPath = "";
            if (null != tabBarInfos) {
                int size = tabBarInfos.size();
                String tabBarPath = null;
                for (int i = 0; i < size; i++) {
                    tabBarInfo = tabBarInfos.get(i);
                    if (null != tabBarInfo) {
                        iconPath = tabBarInfo.getTabBarIconPath();
                        selectedIconPath = tabBarInfo.getTabBarSelectedIconPath();
                        if (!TextUtils.isEmpty(curPath)) {
                            tabBarPath = tabBarInfo.getTabBarPagePath();
                            if (!TextUtils.isEmpty(tabBarPath) && !tabBarPath.startsWith("/")) {
                                tabBarPath = "/" + tabBarPath;
                            }
                            if (curPath.equals(tabBarPath)) {
                                isTabPath = true;
                                tabBarInfo.setSelected(true);
                            } else {
                                tabBarInfo.setSelected(false);
                            }
                        }
                    }
                    if (null != rootView && !TextUtils.isEmpty(iconPath)) {
                        tabBarInfo.setTabBarIconUri(rootView.tryParseUri(iconPath));
                    }
                    if (null != rootView && !TextUtils.isEmpty(selectedIconPath)) {
                        tabBarInfo.setTabBarSelectedIconUri(rootView.tryParseUri(selectedIconPath));
                    }
                }
            }
        }
        if (isTabPath) {
            if (mTabBarView.getVisibility() != View.VISIBLE) {
                mTabBarView.setVisibility(View.VISIBLE);
            }
        } else {
            if (mTabBarView.getVisibility() != View.GONE) {
                mTabBarView.setVisibility(View.GONE);
            }
        }
        synchronized (LOCK_TABBAR_DATA) {
            mTabBarItemDatas.clear();
        }
        List<TabBarInfo> datas = new ArrayList<>();
        RecyclerView tabBarRecyclerView = parentView.findViewById(R.id.tabbar_recyclerview);
        TabBarItemAdapter tabBarItemAdapter = new TabBarItemAdapter(context, datas, textColor, selectedTextColor);
        synchronized (LOCK_TABBAR_DATA) {
            if (null != tabBarInfos && tabBarInfos.size() > 0) {
                mTabBarItemDatas.addAll(tabBarInfos);
            }
        }
        if (mTabBarItemDatas.size() > 0) {
            tabBarItemAdapter.updateData(mTabBarItemDatas, mTabBarItemDatas.size());
        }
        if (null != tabBarRecyclerView) {
            tabBarRecyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
            tabBarRecyclerView.setAdapter(tabBarItemAdapter);
        } else {
            Log.w(TAG, "initTabBarList tabBarRecyclerView is null.");
        }
        tabBarItemAdapter.setItemClickListner(new TabBarItemAdapter.ItemClickListner() {
            @Override
            public void onItemClick(int position, View itemView) {
                String pagePathStr = "";
                String pageParamsStr = "";
                TabBarInfo itemData = null;
                if (null != tabBarItemAdapter) {
                    List<TabBarInfo> datas = tabBarItemAdapter.getItemDatas();
                    if (null != datas && datas.size() > position) {
                        itemData = datas.get(position);
                        pagePathStr = itemData.getTabBarPagePath();
                        pageParamsStr = itemData.getTabBarPageParams();
                    }
                }
                if (!TextUtils.isEmpty(pagePathStr)) {
                    routerTabBarUri(rootView, appInfo, pagePathStr, pageParamsStr);
                } else {
                    Log.w(TAG, "initTabBarList onItemClick pagePathStr is empty.");
                }
            }
        });
    }

    public boolean notifyTabBarChange(RootView rootView, String routerPath) {
        boolean isValid = false;
        if (TextUtils.isEmpty(routerPath) || null == rootView) {
            Log.w(TAG, "notifyTabBarChange routerPath is empty or rootView is null.");
            return false;
        }
        int valideIndex = -1;
        synchronized (LOCK_TABBAR_DATA) {
            int allSize = mTabBarItemDatas.size();
            TabBarInfo tabBarInfo = null;
            String path = "";
            if (allSize > 0) {
                for (int i = 0; i < allSize; i++) {
                    tabBarInfo = mTabBarItemDatas.get(i);
                    path = tabBarInfo.getTabBarPagePath();
                    if (!TextUtils.isEmpty(path) && !path.startsWith("/")) {
                        path = "/" + path;
                    }
                    if (!TextUtils.isEmpty(path) && path.equals(routerPath)) {
                        valideIndex = i;
                        break;
                    }
                }
                if (valideIndex == -1) {
                    Log.w(TAG, "notifyTabBarChange is not valid compare path : " + routerPath);
                    return isValid;
                }
                for (int i = 0; i < allSize; i++) {
                    tabBarInfo = mTabBarItemDatas.get(i);
                    if (i == valideIndex) {
                        tabBarInfo.setSelected(true);
                    } else {
                        tabBarInfo.setSelected(false);
                    }
                }
            }
        }
        if (valideIndex == -1) {
            Log.w(TAG, "notifyTabBarChange valideIndex -1 size 0.");
            return false;
        }
        View tabBarRecyclerView = rootView.findViewById(R.id.tabbar_recyclerview);
        RecyclerView.Adapter adapter = null;
        TabBarItemAdapter tmpTabBarItemAdapter = null;
        RecyclerView recyclerView = null;
        if (tabBarRecyclerView instanceof RecyclerView) {
            recyclerView = (RecyclerView) tabBarRecyclerView;
        }
        if (null != recyclerView) {
            adapter = recyclerView.getAdapter();
        }
        if (adapter instanceof TabBarItemAdapter) {
            tmpTabBarItemAdapter = (TabBarItemAdapter) adapter;
        }
        if (null == tmpTabBarItemAdapter) {
            Log.w(TAG, "notifyTabBarChange tmpTabBarItemAdapter is null.");
            return isValid;
        }
        final TabBarItemAdapter tabBarItemAdapter = tmpTabBarItemAdapter;
        isValid = true;
        ThreadUtils.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (null != tabBarItemAdapter) {
                    tabBarItemAdapter.updateData(mTabBarItemDatas, mTabBarItemDatas.size());
                } else {
                    Log.w(TAG, "notifyTabBarChange tabBarItemAdapter is null.");
                }

            }
        });
        return isValid;
    }

    private void routerTabBarUri(RootView rootView, AppInfo appInfo, String path, String params) {
        if (TextUtils.isEmpty(path) || null == appInfo || null == rootView) {
            Log.e(TAG, "routerTabBarUri params appInfo or rootView is null.");
            return;
        }
        HybridRequest request = null;
        try {
            request = parseRouterRequest(appInfo, path, params);
        } catch (Exception e) {
            Log.e(TAG, "routerTabBarUri error : " + e.getMessage());
        }
        if (null == request) {
            Log.w(TAG, "routerTabBarUri request is null.");
            return;
        }
        request.setTabRequest(true);
        PageManager pageManager = null;
        if (null != rootView) {
            pageManager = rootView.getPageManager();
        }
        final PageManager tmpPageManager = pageManager;
        final HybridRequest tmpRequest = request;
        if (null != tmpPageManager) {
            JsThread jsThread = rootView.getJsThread();
            if (null != jsThread) {
                jsThread.postInJsThread(new Runnable() {
                    @Override
                    public void run() {
                        RouterUtils.routerTabBar(tmpPageManager, -1, tmpRequest, VALUE_ROUTER_APP_FROM_ROUTER, null);
                    }
                });
            } else {
                Log.w(TAG, "routerTabBarUri jsThread is null.");
            }
        } else {
            Log.w(TAG, "routerTabBarUri tmpPageManager is null.");
        }
    }

    private HybridRequest parseRouterRequest(AppInfo appInfo, String pagePath, String params) throws Exception {
        if (TextUtils.isEmpty(pagePath) || null == appInfo) {
            Log.w(TAG, "parseRouterRequest pagePath or appInfo is null.");
            return null;
        }
        HybridRequest.Builder builder = new HybridRequest.Builder();
        builder.pkg(appInfo.getPackage());
        String url = pagePath;
        JSONObject rawPageParams = null;
        try {
            if (!TextUtils.isEmpty(params)) {
                rawPageParams = new JSONObject(params);
            }
        } catch (Exception e) {
            Log.w(TAG, "parseRouterRequest rawPageParams is null : " + e.getMessage());
        }
        builder.uri(url);
        if (rawPageParams != null) {
            Map<String, String> pageParams = new HashMap<>();
            Iterator<String> keys = rawPageParams.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                pageParams.put(key, rawPageParams.getString(key));
            }
            builder.params(pageParams);
        }
        return builder.build();
    }

    public boolean prepareTabBarPath(boolean isTabBarPage, String path) {
        boolean isTabPath = isTabBarPage;
        if (TextUtils.isEmpty(path)) {
            if (null != mTabBarView &&
                    mTabBarView.getVisibility() != View.GONE) {
                mTabBarView.setVisibility(View.GONE);
            }
            return isTabPath;
        }
        if (!isTabPath) {
            synchronized (LOCK_TABBAR_DATA) {
                int allSize = mTabBarItemDatas.size();
                TabBarInfo tabBarInfo = null;
                String tabBarPath = null;
                for (int i = 0; i < allSize; i++) {
                    tabBarInfo = mTabBarItemDatas.get(i);
                    tabBarPath = tabBarInfo.getTabBarPagePath();
                    if (!TextUtils.isEmpty(tabBarPath) && !tabBarPath.startsWith("/")) {
                        tabBarPath = "/" + tabBarPath;
                    }
                    if (null != tabBarInfo && path.equals(tabBarPath)) {
                        isTabPath = true;
                        break;
                    }
                }
            }
        }
        if (null != mTabBarView) {
            if (isTabPath) {
                if (mTabBarView.getVisibility() != View.VISIBLE) {
                    mTabBarView.setVisibility(View.VISIBLE);
                }
            } else {
                if (mTabBarView.getVisibility() != View.GONE) {
                    mTabBarView.setVisibility(View.GONE);
                }
            }
        } else {
            Log.w(TAG, "prepareTabBarPath mTabBarView is null.");
        }
        return isTabPath;
    }

    public void clearTabBar() {
        mTabBarView = null;
    }

}

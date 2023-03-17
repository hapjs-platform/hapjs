/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.utils;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import org.hapjs.bridge.ApplicationContext;
import org.hapjs.bridge.HybridRequest;
import org.hapjs.cache.CacheStorage;
import org.hapjs.card.sdk.utils.CardConfigUtils;
import org.hapjs.logging.LogHelper;
import org.hapjs.logging.RuntimeLogManager;
import org.hapjs.logging.Source;
import org.hapjs.model.AppInfo;
import org.hapjs.render.Page;
import org.hapjs.render.PageManager;
import org.hapjs.render.PageNotFoundException;
import org.hapjs.render.RootView;
import org.hapjs.runtime.HapEngine;

import static org.hapjs.logging.RuntimeLogManager.VALUE_ROUTER_APP_FROM_ROUTER;

public class RouterUtils {
    public static final String EXTRA_HAP_NAME = "HAP_NAME";
    public static final String EXTRA_HAP_PACKAGE = "HAP_PACKAGE";
    public static final String EXTRA_HAP_SIGNATURE = "HAP_SIGNATURE";
    public static final String EXTRA_HAP_SOURCE_ENTRY = "HAP_SOURCE_ENTRY";
    public static final String EXTRA_CARD_HOST_SOURCE = "CARD_HOST_SOURCE";
    public static final String EXTRA_SESSION = "SESSION";
    private static final String TAG = "RouterUtils";

    public static boolean router(Context context, PageManager pageManager, HybridRequest request) {
        return router(context, pageManager, -1, request, VALUE_ROUTER_APP_FROM_ROUTER, null);
    }

    public static boolean router(
            Context context,
            PageManager pageManager,
            int pageId,
            HybridRequest request,
            String routerAppFrom,
            String sourceH5) {
        if (pageManager == null) {
            return false;
        }
        recordAppRouterStats(pageManager, request);
        try {
            return pushPage(pageManager, pageId, request);
        } catch (PageNotFoundException e) {
            return pushExternal(context, pageManager, request, routerAppFrom, sourceH5);
        }
    }

    public static boolean switchTab(Context context, PageManager pageManager, HybridRequest request) {
        if (pageManager == null) {
            return false;
        }
        RootView rootView = null;
        PageManager.PageChangedListener pageChangedListener = pageManager.getPageChangedListener();
        if (pageChangedListener instanceof RootView) {
            rootView = ((RootView) pageChangedListener);
        }
        if (null == rootView) {
            Log.w(TAG, "switchTab rootView is null.");
            return false;
        }
        boolean isValid = false;
        if (null != request) {
            String path = request.getUriWithoutParams();
            if (!TextUtils.isEmpty(path)) {
                isValid = rootView.notifyTabBarChange(path);
            }
            if (isValid) {
                request.setTabRequest(true);
                return routerTabBar(pageManager, -1, request, VALUE_ROUTER_APP_FROM_ROUTER, null);
            } else {
                Log.w(TAG, "switchTab request not isValid  path :  " + path);
                return false;
            }
        } else {
            Log.w(TAG, "switchTab request is null.");
            return false;
        }
    }

    public static boolean routerTabBar(PageManager pageManager,
                                       int pageId, HybridRequest request, String routerAppFrom, String sourceH5) {
        if (pageManager == null) {
            return false;
        }
        recordAppRouterStats(pageManager, request);
        try {
            return pushPage(pageManager, pageId, request);
        } catch (PageNotFoundException e) {
            Log.w(TAG, "routerTabBar PageNotFoundException : " + e.getMessage());
            return false;
        }
    }

    public static boolean push(PageManager pageManager, HybridRequest request)
            throws PageNotFoundException {
        if (pageManager == null) {
            return false;
        }
        recordAppRouterStats(pageManager, request);
        return pushPage(pageManager, -1, request);
    }

    private static boolean pushPage(PageManager pageManager, int pageId, HybridRequest request)
            throws PageNotFoundException {
        Page page = null;
        try {
            page = pageManager.buildPage(request);
            if (null != request && null != page
                    && request.isTabRequest()) {
                page.setTabPage(true);
            }
        } catch (PageNotFoundException e) {
            if (!HapEngine.getInstance(request.getPackage()).isCardMode()
                    && request instanceof HybridRequest.HapRequest
                    &&
                    TextUtils.equals(request.getPackage(), pageManager.getAppInfo().getPackage())) {
                Page currentPage = pageManager.getCurrPage();
                if (currentPage != null && currentPage.isPageNotFound()) {
                    pageManager.replace(pageManager.buildErrorPage(request, true));
                } else {
                    pageManager.push(pageManager.buildErrorPage(request, false));
                }
                return false;
            }
            throw e;
        }
        if (page != null) {
            Page currPage = pageManager.getPageById(pageId);
            if (request.isDeepLink()
                    && currPage != null
                    && TextUtils.equals(page.getPath(), currPage.getPath())) {
                // TODO: call page's onNewIntent callback
                return false;
            } else {
                pageManager.push(page);
                return true;
            }
        }
        return false;
    }

    private static boolean pushExternal(
            Context context, PageManager pageManager, HybridRequest request, String routerAppFrom, String sourceH5) {
        String pkg = pageManager.getAppInfo().getPackage();
        Bundle extras = getAppInfoExtras(context, pageManager.getAppInfo());
        if (request instanceof HybridRequest.HapRequest) {
            if (!HapEngine.getInstance(request.getPackage()).isCardMode()
                    && TextUtils.equals(request.getPackage(), pkg)) {
                Page currentPage = pageManager.getCurrPage();
                if (currentPage != null && currentPage.isPageNotFound()) {
                    pageManager.replace(pageManager.buildErrorPage(request, true));
                } else {
                    pageManager.push(pageManager.buildErrorPage(request, false));
                }
                return false;
            }
            HybridRequest.HapRequest hapRequest = (HybridRequest.HapRequest) request;
            // Allowed to open hap package only
            return PackageUtils.openHapPackage(context, pkg, pageManager, hapRequest, extras, routerAppFrom);
        } else {
            if (UriUtils.isHybridUri(request.getUri())) {
                PackageUtils.openHapPackage(context, pkg, pageManager, request, extras, routerAppFrom);
                return true;
            }
            if (!request.isDeepLink()) {
                ApplicationContext appContext = HapEngine.getInstance(pkg).getApplicationContext();
                if (DocumentUtils.open(appContext, request.getUri(), extras, routerAppFrom, sourceH5)) {
                    return true;
                }
            }
            return NavigationUtils.navigate(context, pkg, request, extras, routerAppFrom, sourceH5);
        }
    }

    private static Bundle getAppInfoExtras(Context context, AppInfo appInfo) {
        String appName = appInfo.getName();
        String appPackage = appInfo.getPackage();
        String signature = CacheStorage.getInstance(context).getPackageSign(appPackage);
        Bundle extras = new Bundle();
        extras.putString(EXTRA_HAP_NAME, appName);
        extras.putString(EXTRA_HAP_PACKAGE, appPackage);
        extras.putString(EXTRA_HAP_SIGNATURE, signature);
        Source source = Source.currentSource();
        if (source != null) {
            extras.putString(EXTRA_HAP_SOURCE_ENTRY, source.getEntry().toJson().toString());
        }
        // card 模式下传递宿主设置的 source 信息
        if (HapEngine.getInstance(appPackage).isCardMode()) {
            Source hostSource = CardConfigUtils.getHostSource();
            if (hostSource != null) {
                extras.putString(EXTRA_CARD_HOST_SOURCE, hostSource.toJson().toString());
            }
        }
        String session = LogHelper.getSession(appPackage);
        if (!TextUtils.isEmpty(session)) {
            extras.putString(EXTRA_SESSION, session);
        }
        return extras;
    }

    public static void replace(PageManager pageManager, HybridRequest request) {
        if (pageManager == null) {
            return;
        }
        recordAppRouterStats(pageManager, request);
        Page page;
        try {
            page = pageManager.buildPage(request);
        } catch (PageNotFoundException e) {
            Page currentPage = pageManager.getCurrPage();
            page =
                    pageManager.buildErrorPage(request,
                            currentPage != null && currentPage.isPageNotFound());
        }
        pageManager.replace(page);
    }

    public static void replaceLeftPage(PageManager pageManager, HybridRequest request) {
        if (pageManager == null) {
            return;
        }
        recordAppRouterStats(pageManager, request);
        Page page;
        try {
            page = pageManager.buildPage(request);
        } catch (PageNotFoundException e) {
            Page leftPage = pageManager.getMultiWindowLeftPage();
            page = pageManager.buildErrorPage(request, leftPage != null && leftPage.isPageNotFound());
        }
        pageManager.replaceLeftPage(page);
    }

    public static boolean back(Context context, PageManager pageManager) {
        if (pageManager != null && pageManager.getCurrIndex() > 0) {
            pageManager.back();
            return true;
        } else if (context instanceof Activity) {
            final Activity activity = (Activity) context;
            activity.runOnUiThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            activity.onBackPressed();
                        }
                    });
            return true;
        }
        return false;
    }

    public static boolean back(Context context, PageManager pageManager, HybridRequest request) {
        if (request == null) {
            return back(context, pageManager);
        }

        try {
            Page page = pageManager.buildPage(request);
            if (page != null) {
                return pageManager.back(page.getPath());
            }
        } catch (PageNotFoundException e) {
            // normal back
            return back(context, pageManager);
        }
        return false;
    }

    private static void recordAppRouterStats(PageManager pageManager, HybridRequest request) {
        String pkg = pageManager.getAppInfo().getPackage();
        RuntimeLogManager.getDefault().logAppRouter(pkg, request.getUri());
        RuntimeLogManager.getDefault().logAppRouterNoQueryParams(pkg, request.getUri());
    }

    public static void exit(Context context, PageManager pageManager) {
        if (pageManager != null) {
            pageManager.clear(true);
        }
        back(context, pageManager);
    }
}

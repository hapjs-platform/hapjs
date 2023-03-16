/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render;

import android.content.Context;
import android.util.Log;

import org.hapjs.bridge.ApplicationContext;
import org.hapjs.bridge.HybridRequest;
import org.hapjs.common.executors.Executors;
import org.hapjs.common.utils.UriUtils;
import org.hapjs.io.AssetSource;
import org.hapjs.io.JavascriptReader;
import org.hapjs.io.RpkSource;
import org.hapjs.io.Source;
import org.hapjs.io.TextReader;
import org.hapjs.model.AppInfo;
import org.hapjs.model.RoutableInfo;
import org.hapjs.model.SubpackageInfo;
import org.hapjs.runtime.HapEngine;
import org.hapjs.runtime.Runtime;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AppResourcesLoader {
    private static final String TAG = "ResourcesPreloader";

    private static final String APP_JS = "app.js";
    private static final String APP_CSS_JSON = "app.css.json";
    public static final String APP_CHUNKS_JSON = "app-chunks.json";
    public static final String PAGE_CHUNKS_JSON = "page-chunks.json";

    private static Map<String, AppResources> sAppResources = new HashMap<>();

    public static void preload(Context context, HybridRequest request) {
        String pkg = request.getPackage();
        AppResources appResources = new AppResources();
        sAppResources.put(pkg, appResources);
        Context appContext = context.getApplicationContext();
        Executors.io().execute(() -> doPreload(appContext, request, appResources));
    }

    private static void doPreload(Context context, HybridRequest request, AppResources appResources) {
        String pkg = request.getPackage();
        ApplicationContext appContext = HapEngine.getInstance(pkg).getApplicationContext();
        AppInfo appInfo = appContext.getAppInfo(false);
        if (appInfo == null) {
            Log.i(TAG, "app not install. skip preloading");
            return;
        }

        appResources.subpackageInfos = appInfo.getSubpackageInfos();

        if (appResources.appJs == null) {
            appResources.appJs = ensureNotNull(loadAppJs(context, pkg));
        }
        if (appResources.appCss == null) {
            appResources.appCss = ensureNotNull(loadAppCss(context, pkg));
        }
        if (!appResources.chunksMap.containsKey(APP_CHUNKS_JSON)) {
            appResources.chunksMap.put(APP_CHUNKS_JSON, loadJsChunks(pkg, APP_CHUNKS_JSON));
        }

        Page page;
        try {
            page = PageManager.buildPage(request, appInfo, null);
        } catch (PageNotFoundException e) {
            Log.w(TAG, "page not found", e);
            return;
        }

        if (!appResources.pageJsMap.containsKey(page.getPath())) {
            appResources.pageJsMap.put(page.getPath(), loadPageJs(context, pkg, page));
        }

        if (!appResources.pageCssMap.containsKey(page.getPath())) {
            appResources.pageCssMap.put(page.getPath(), loadPageCss(context, pkg, page));
        }

        List<SubpackageInfo> subpackageInfos = appResources.subpackageInfos;
        if (subpackageInfos == null || subpackageInfos.isEmpty()) {
            appResources.chunksMap.put(PAGE_CHUNKS_JSON, loadJsChunks(pkg, PAGE_CHUNKS_JSON));
        } else {
            SubpackageInfo currPageSubpackage = null;
            for (SubpackageInfo subpackageInfo : subpackageInfos) {
                if (subpackageInfo.containPath(page.getPath())) {
                    String path = subpackageInfo.getResource() + "/" + PAGE_CHUNKS_JSON;
                    appResources.chunksMap.put(path, loadJsChunks(pkg, path));
                    currPageSubpackage = subpackageInfo;
                }
            }
            for (SubpackageInfo subpackageInfo : subpackageInfos) {
                if (subpackageInfo != currPageSubpackage) {
                    String path = subpackageInfo.getResource() + "/" + PAGE_CHUNKS_JSON;
                    appResources.chunksMap.put(path, loadJsChunks(pkg, path));
                }
            }
            if (currPageSubpackage == null) {
                Log.w(TAG, "subpackage not found for page: " + page.getPath());
            }
        }
    }

    private static String ensureNotNull(String str) {
        return str == null ? "" : str;
    }

    public static String getAppJs(Context context, String pkg) {
        String appJs;
        AppResources appResources = sAppResources.get(pkg);
        if (appResources != null && appResources.appJs != null) {
            appJs = appResources.appJs;
        } else {
            appJs = loadAppJs(context, pkg);
        }
        return appJs;
    }

    private static String loadAppJs(Context context, String pkg) {
        RpkSource rpkSource = new RpkSource(context, pkg, APP_JS);
        return JavascriptReader.get().read(rpkSource);
    }

    public static String getAppCss(Context context, String pkg) {
        String appCss;
        AppResources appResources = sAppResources.get(pkg);
        if (appResources != null && appResources.appCss != null) {
            appCss = appResources.appCss;
        } else {
            appCss = loadAppCss(context, pkg);
        }
        return appCss;
    }

    private static String loadAppCss(Context context, String pkg) {
        Source cssSource = new RpkSource(context, pkg, APP_CSS_JSON);
        return TextReader.get().read(cssSource);
    }

    public static String getJsChunks(String pkg, String path) {
        String chunks;
        AppResources appResources = sAppResources.get(pkg);
        if (appResources != null && appResources.chunksMap.containsKey(path)) {
            chunks = appResources.chunksMap.get(path);
        } else {
            chunks = loadJsChunks(pkg, path);
        }
        return chunks;
    }

    private static String loadJsChunks(String pkg, String path) {
        RpkSource jsChunksSource = new RpkSource(Runtime.getInstance().getContext(), pkg, path);
        return TextReader.get().read(jsChunksSource);
    }

    public static String getPageJs(Context context, String pkg, Page page) {
        String pageJs;
        AppResources appResources = sAppResources.get(pkg);
        if (appResources != null && appResources.pageJsMap.containsKey(page.getPath())) {
            pageJs = appResources.pageJsMap.get(page.getPath());
        } else {
            pageJs = loadPageJs(context, pkg, page);
        }
        return pageJs;
    }

    private static String loadPageJs(Context context, String pkg, Page page) {
        RoutableInfo routableInfo = page.getRoutableInfo();
        String jsuri = routableInfo.getUri();
        Source jssource;
        if (UriUtils.isAssetUri(jsuri)) {
            jssource = new AssetSource(context, UriUtils.getAssetPath(jsuri));
        } else {
            jssource = new RpkSource(context, pkg, jsuri);
        }
        return JavascriptReader.get().read(jssource);
    }

    public static String getPageCss(Context context, String pkg, Page page) {
        String pageCss;
        AppResources appResources = sAppResources.get(pkg);
        if (appResources != null && appResources.pageCssMap.containsKey(page.getPath())) {
            pageCss = appResources.pageCssMap.get(page.getPath());
        } else {
            pageCss = loadPageCss(context, pkg, page);
        }
        return pageCss;
    }

    private static String loadPageCss(Context context, String pkg, Page page) {
        RoutableInfo routableInfo = page.getRoutableInfo();
        final String jsuri = routableInfo.getUri();
        final String cssuri = jsuri.replace(".js", ".css.json");
        final Source csssource = new RpkSource(context, pkg, cssuri);
        return TextReader.get().read(csssource);
    }

    public static boolean hasAppResourcesPreloaded(String pkg) {
        AppResources appResources = sAppResources.get(pkg);
        return appResources != null
                && appResources.appJs != null
                && appResources.appCss != null
                && appResources.chunksMap.containsKey(APP_CHUNKS_JSON);
    }

    public static void clearPreloadedResources(String pkg) {
        sAppResources.remove(pkg);
    }

    private static class AppResources {
        List<SubpackageInfo> subpackageInfos;
        String appJs;
        String appCss;
        Map<String, String> pageJsMap = new HashMap<>();
        Map<String, String> pageCssMap = new HashMap<>();
        Map<String, String> chunksMap = new HashMap<>();

        AppResources() {
        }
    }
}

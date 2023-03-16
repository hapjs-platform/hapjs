/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render;

import static org.hapjs.bridge.HybridRequest.INTENT_ACTION;
import static org.hapjs.bridge.HybridRequest.INTENT_FROM_EXTERNAL;
import static org.hapjs.bridge.HybridRequest.INTENT_URI;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hapjs.bridge.HybridRequest;
import org.hapjs.common.utils.FoldingUtils;
import org.hapjs.common.utils.ThreadUtils;
import org.hapjs.common.utils.UriUtils;
import org.hapjs.logging.RuntimeLogManager;
import org.hapjs.model.AppInfo;
import org.hapjs.model.PageInfo;
import org.hapjs.model.RoutableInfo;
import org.hapjs.model.RouterInfo;
import org.hapjs.runtime.HapEngine;

public class PageManager {

    public static final String ABOUT_PAGE_PATH = "file:///android_asset/app/about-page.js";
    private static final String TAG = "PageManager";

    private static final int MSG_PUSH = 1;
    private static final int MSG_REPLACE = 2;
    private static final int MSG_BACK = 3;
    private static final int MSG_CLEAR = 4;
    private static final int MSG_FINISH = 5;
    private static final int MSG_LEFT_REPLACE = 6;

    private AppInfo mAppInfo;
    private List<Page> mPageInfos = new ArrayList<>();
    private PageChangedListener mPageChangedListener;
    private Handler mHandler;
    public PageCache mPageCache;
    private int mMultiWindowLeftPageId = -1;

    public PageManager(PageChangedListener pageChangeListener, AppInfo appInfo) {
        mPageChangedListener = pageChangeListener;
        mAppInfo = appInfo;
        mHandler = new HandlerImpl();
        mPageCache = new PageCache(mPageChangedListener);
    }

    public void setPageChangedListener(PageChangedListener pageChangedListener) {
        mPageChangedListener = pageChangedListener;
    }

    public PageChangedListener getPageChangedListener() {
        return mPageChangedListener;
    }

    public AppInfo getAppInfo() {
        return mAppInfo;
    }

    public void setAppInfo(AppInfo appInfo) {
        mAppInfo = appInfo;
    }

    public Page getPage(int index) {
        if (index < 0) {
            return null;
        }

        if (index >= mPageInfos.size()) {
            throw new IllegalArgumentException("Index out of bound. index:" + index);
        }
        return mPageInfos.get(index);
    }

    public Page getPageById(int id) {
        final int N = mPageInfos.size();
        for (int i = 0; i < N; i++) {
            Page page = mPageInfos.get(i);
            if (page.pageId == id) {
                return page;
            }
        }
        return null;
    }

    private int getPageIndexByPath(String path) {
        if (TextUtils.isEmpty(path)) {
            return -1;
        }
        final int N = mPageInfos.size();
        for (int i = N - 1; i >= 0; i--) {
            Page page = mPageInfos.get(i);
            if (path.equals(page.getPath())) {
                return i;
            }
        }
        return -1;
    }

    private int getEldestPageIndexByPath(String path) {
        if (TextUtils.isEmpty(path)) {
            return -1;
        }
        final int N = mPageInfos.size();
        for (int i = 0; i < N; i++) {
            Page page = mPageInfos.get(i);
            if (path.equals(page.getPath())) {
                return i;
            }
        }
        return -1;
    }

    public int getPageCount() {
        return mPageInfos.size();
    }

    public int getCurrIndex() {
        return mPageInfos.size() - 1;
    }

    public List<Page> getPageInfos() {
        return mPageInfos;
    }

    @Nullable
    public Page getCurrPage() {
        if (mPageInfos.size() == 0) {
            return null;
        }
        return mPageInfos.get(mPageInfos.size() - 1);
    }

    public Page getPrePage(Page page) {
        int index = mPageInfos.indexOf(page);
        int prePageIndex = index - 1;
        return getPage(prePageIndex);
    }

    public void updateMultiWindowLeftPage(Page leftPage) {
        if (mPageInfos.size() == 0 || leftPage == null) {
            Page lastLeftPage = getMultiWindowLeftPage();
            if (lastLeftPage != null) {
                lastLeftPage.setIsMultiWindowLeftPage(false);
            }
            mMultiWindowLeftPageId = -1;
            return;
        }
        leftPage.setIsMultiWindowLeftPage(true);
        mMultiWindowLeftPageId = leftPage.getPageId();
    }

    public Page updateMultiWindowLeftPageWhenNewCreate() {
        if (mPageInfos.size() <= 1) {
            Page lastLeftPage = getMultiWindowLeftPage();
            if (lastLeftPage != null) {
                lastLeftPage.setIsMultiWindowLeftPage(false);
            }
            mMultiWindowLeftPageId = -1;
            return null;
        }

        Page leftPage = null;
        if (MultiWindowManager.isNavigationMode()) {
            leftPage = mPageInfos.get(0);
        } else if (MultiWindowManager.isShoppingMode()) {
            leftPage = mPageInfos.get(getCurrIndex() - 1);
        }
        if (leftPage != null) {
            leftPage.setIsMultiWindowLeftPage(true);
            mMultiWindowLeftPageId = leftPage.getPageId();
        }
        return leftPage;
    }

    public int getMultiWindowLeftPageId() {
        return mMultiWindowLeftPageId;
    }

    private boolean hasMultiWindowLeftPage() {
        return FoldingUtils.isMultiWindowMode() && mMultiWindowLeftPageId >= 0;
    }

    @Nullable
    public Page getMultiWindowLeftPage() {
        if (mPageInfos.size() == 0 || !hasMultiWindowLeftPage()) {
            return null;
        }
        return getPageById(mMultiWindowLeftPageId);
    }

    private int getMultiWindowLeftPageIndex() {
        if (mPageInfos.size() == 0 || !hasMultiWindowLeftPage()) {
            return -1;
        }
        for (int i = 0; i < mPageInfos.size(); i++) {
            Page page = mPageInfos.get(i);
            if (page.getPageId() == mMultiWindowLeftPageId) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Build page by request
     *
     * @param request the request
     * @return a page object if found
     * @throws PageNotFoundException throw if page not found
     */
    public Page buildPage(HybridRequest request) throws PageNotFoundException {
        Page page;
        if (request instanceof HybridRequest.HapRequest) {
            HybridRequest.HapRequest hapRequest = (HybridRequest.HapRequest) request;

            page = mPageCache.get(request.getUri());
            if (page != null) {
                if (page.getCacheExpiredTime() > 0
                        && System.currentTimeMillis() - page.getPageLastUsedTime()
                        > page.getCacheExpiredTime()) {
                    page.cleanCache();
                    mPageCache.remove(request.getUri());
                    page = buildPageByUri(hapRequest, mAppInfo, getCurrIndex());
                    if (mPageInfos.size() != 0 && page.shouldCache()) {
                        mPageCache.put(request.getUri(), page);
                    }
                }
            } else {
                page = buildPageByUri(hapRequest, mAppInfo, getCurrIndex());
                // 首页不放入子页面缓存池，配置不缓存不放入子页面缓存
                if (mPageInfos.size() != 0 && page.shouldCache()) {
                    Log.d(TAG, "ifCache:" + page.shouldCache());
                    mPageCache.put(request.getUri(), page);
                }
            }
        } else {
            page = buildPageByFilter(request, mAppInfo, getCurrPage());
        }
        if (page != null) {
            page.setRequest(request);
        }
        return page;
    }

    public static Page buildPage(HybridRequest request, AppInfo appInfo, Page currPage) throws PageNotFoundException {
        Page page;
        if (request instanceof HybridRequest.HapRequest) {
            HybridRequest.HapRequest hapRequest = (HybridRequest.HapRequest) request;
            page = buildPageByUri(hapRequest, appInfo, -1);
        } else {
            page = buildPageByFilter(request, appInfo, currPage);
        }
        return page;
    }

    public void clearCachePage() {
        if (mPageCache != null) {
            mPageCache.clear();
        }
    }

    public Page buildAboutPage() {
        PageInfo pageInfo =
                new PageInfo(
                        "aboutPage", null, ABOUT_PAGE_PATH, "about-page", null,
                        PageInfo.MODE_SINGLE_TASK);
        Map<String, Object> intent = new HashMap<>();
        intent.put(INTENT_URI, ABOUT_PAGE_PATH);
        intent.put(INTENT_ACTION, "view");
        intent.put(INTENT_FROM_EXTERNAL, false);
        Page aboutPage =
                new Page(mAppInfo, pageInfo, null, intent, IdGenerator.generatePageId(), null);
        aboutPage.setInnerPageTag(Page.PAGE_TAG_MENUBAR_ABOUT);
        aboutPage.setPageShowTitlebar(false);
        return aboutPage;
    }

    public Page buildHomePage() {
        Map<String, Object> intent = new HashMap<>();
        intent.put(INTENT_URI, "/");
        intent.put(INTENT_ACTION, "view");
        intent.put(INTENT_FROM_EXTERNAL, false);
        PageInfo pageInfo = mAppInfo.getRouterInfo().getEntry();
        Page page = new Page(mAppInfo, pageInfo, null, intent, IdGenerator.generatePageId(), null);
        return page;
    }

    public Page buildErrorPage(HybridRequest request, boolean useDefault) {
        Page errorPage =
                new Page(
                        mAppInfo,
                        mAppInfo.getRouterInfo().getErrorPage(useDefault),
                        request.getParams(),
                        request.getIntent(),
                        IdGenerator.generatePageId(),
                        null);
        errorPage.setPageNotFound(true);
        errorPage.setTargetPageUri(request.getUri());
        return errorPage;
    }

    private static Page buildPageByUri(HybridRequest.HapRequest request, AppInfo appInfo, int currIndex) throws PageNotFoundException {
        if (!appInfo.getPackage().equals(request.getPackage())) {
            throw new PageNotFoundException("request is not for current app: " + request.getUri());
        }

        RoutableInfo routableInfo;
        String path = request.getPagePath();
        RouterInfo routerInfo = appInfo.getRouterInfo();
        boolean isPageNotFound = false;
        if (HapEngine.getInstance(request.getPackage()).isCardMode()) {
            routableInfo = routerInfo.getCardInfoByPath(request.getPagePath());
        } else {
            String pageName = request.getPageName();
            if (TextUtils.isEmpty(pageName)) {
                routableInfo = appInfo.getRouterInfo().getPageInfoByPath(path);
                if (routableInfo == null && "/".equals(path)) {
                    routableInfo = appInfo.getRouterInfo().getEntry();
                }
            } else {
                routableInfo = appInfo.getRouterInfo().getPageInfoByName(pageName);
            }
        }

        if (routableInfo == null) {
            RuntimeLogManager.getDefault()
                    .logPageError(
                            request.getPackage(),
                            request.getPagePath(),
                            new PageNotFoundException(
                                    "Page not found, hybridUrl=" + request.getUri()));

            if (!HapEngine.getInstance(request.getPackage()).isCardMode()
                    && routerInfo != null
                    && currIndex < 0) {
                isPageNotFound = true;
                Log.w(TAG, "Page not found router to entry, hybridUrl:" + request.getUri());
                routableInfo = routerInfo.getErrorPage(false);
            }
        }
        if (routableInfo == null) {
            throw new PageNotFoundException("Page not found, hybridUrl=" + request.getUri());
        }

        Page page =
                new Page(
                        appInfo,
                        routableInfo,
                        request.getParams(),
                        request.getIntent(),
                        IdGenerator.generatePageId(),
                        request.getLaunchFlags());
        if (isPageNotFound) {
            page.setPageNotFound(true);
            page.setTargetPageUri(request.getUri());
        }
        return page;
    }

    private HybridRequest parseRequest(String pkg, String uri, Map<String, String> pageParams) {
        HybridRequest.Builder builder = new HybridRequest.Builder();
        builder.pkg(pkg);
        builder.uri(uri);
        if (null != pageParams) {
            builder.params(pageParams);
        }
        return builder.build();
    }

    private static Page buildPageByFilter(
            HybridRequest request, AppInfo appInfo, Page currPage) throws PageNotFoundException {
        if (request == null) {
            throw new PageNotFoundException("request is null.");
        }
        RouterInfo routerInfo = appInfo.getRouterInfo();
        if (routerInfo != null) {
            PageInfo pageInfo = routerInfo.getPageInfoByFilter(request);
            if (pageInfo != null) {
                return new Page(
                        appInfo,
                        pageInfo,
                        request.getParams(),
                        request.getIntent(),
                        IdGenerator.generatePageId(),
                        request.getLaunchFlags());
            }
        }

        if (!request.isDeepLink() && HybridRequest.ACTION_VIEW.equals(request.getAction())) {
            String uri = request.getUri();
            if (UriUtils.isWebUri(uri)) {
                return WebPage.create(appInfo, currPage, request);
            }
        }

        throw new PageNotFoundException("No page found for request: " + request);
    }

    public void push(final Page page) {
        if (!ThreadUtils.isInMainThread()) {
            mHandler.obtainMessage(MSG_PUSH, page).sendToTarget();
            return;
        }
        if (null != page && !page.isTabPage()
                && prepareTabBar(page, false)) {
            Log.w(TAG, "push page path is not valid tabbar path : " + page.getPath());
            return;
        }
        List<String> flags = page.getLaunchFlags();
        if (flags != null) {
            if (flags.contains(PageInfo.FLAG_CLEAR_TASK) && mPageInfos.size() > 0) {
                clearPageTask(page);
                return;
            }
        }
        if (page.isTabPage()) {
            if (mPageInfos.size() > 0) {
                clearPageTask(page);
                return;
            }
        }
        RoutableInfo routableInfo = page.getRoutableInfo();
        if (routableInfo != null) {
            String path = page.getPath();
            String launchMode = routableInfo.getLaunchMode();
            if (!TextUtils.isEmpty(launchMode)) {
                switch (launchMode) {
                    case PageInfo.MODE_SINGLE_TASK:
                        int newIndex = getEldestPageIndexByPath(path);
                        if (newIndex >= 0) {
                            Page oldPage = mPageInfos.get(newIndex);
                            oldPage.params = page.params;
                            oldPage.setShouldRefresh(true);
                            back(newIndex - mPageInfos.size() + 1);
                            return;
                        }
                        break;
                    case PageInfo.MODE_STANDARD:
                    default:
                        break;
                }
            }
        }
        Page oldPage = getCurrPage();
        int oldIndex = getCurrIndex();
        int newIndex = oldIndex + 1;

        if (FoldingUtils.isMultiWindowMode() && isRepeatPushRightPage(oldPage, page)) {
            replace(page);
            return;
        }

        mPageChangedListener.onPagePreChange(oldIndex, newIndex, oldPage, page);
        mPageInfos.add(page);
        mPageChangedListener.onPageChanged(oldIndex, newIndex, oldPage, page);
    }

    private boolean isRepeatPushRightPage(Page oldPage, Page newPage) {
        if (oldPage == null || newPage == null) {
            return false;
        }
        HybridRequest oldRequest = oldPage.getRequest();
        HybridRequest newRequest = newPage.getRequest();
        if (oldRequest == null || newRequest == null) {
            return false;
        }
        return TextUtils.equals(oldRequest.getUri(), newRequest.getUri());
    }

    private void clearPageTask(Page page) {
        String path = page.getPath();
        int newIndex = getEldestPageIndexByPath(path);
        if (newIndex == 0) {
            Page newPage = mPageInfos.get(newIndex);
            newPage.params = page.params;
            newPage.setShouldRefresh(true);
            back(-mPageInfos.size() + 1);
        } else if (newIndex > 0) {
            int oldCurrIndex = getCurrIndex();
            Page oldPage = getPage(oldCurrIndex);
            Page newPage = getPage(newIndex);
            newPage.params = page.params;
            newPage.setShouldRefresh(true);
            mPageChangedListener.onPagePreChange(oldCurrIndex, 0, oldPage, newPage);
            for (int i = oldCurrIndex; i > newIndex; i--) {
                Page middlePage = mPageInfos.remove(i);
                mPageChangedListener.onPageRemoved(i, middlePage);
            }
            for (int i = newIndex - 1; i >= 0; i--) {
                Page middlePage = mPageInfos.remove(i);
                mPageChangedListener.onPageRemoved(i, middlePage);
            }
            mPageChangedListener.onPageChanged(oldCurrIndex, 0, oldPage, newPage);
        } else {
            clearAll();
            push(page);
        }
    }

    public void clearAll() {
        if (!ThreadUtils.isInMainThread()) {
            mHandler.sendEmptyMessage(MSG_CLEAR);
            return;
        }

        while (mPageInfos.size() > 1) {
            int index = mPageInfos.size() - 2;
            Page page = mPageInfos.remove(index);
            mPageChangedListener.onPageRemoved(index, page);
        }
        if (mPageInfos.size() == 1) {
            Page page = mPageInfos.remove(0);
            mPageChangedListener.onPageRemoved(0, page);
        }
        mMultiWindowLeftPageId = -1;
    }

    public void replace(Page page) {
        if (!ThreadUtils.isInMainThread()) {
            mHandler.obtainMessage(MSG_REPLACE, page).sendToTarget();
            return;
        }
        if (null != page && !page.isTabPage()
                && prepareTabBar(page, false)) {
            Log.w(TAG, "replace page path is not valid tabbar path : " + page.getPath());
            return;
        }
        Page oldPage = getCurrPage();
        int index = getCurrIndex();
        if (index < 0 || index >= mPageInfos.size()) {
            Log.e(TAG, "replace fail! size=" + mPageInfos.size() + " index=" + index);
            return;
        }
        if (oldPage != null) {
            page.setReferrer(oldPage.getReferrer());
        }
        mPageChangedListener.onPagePreChange(index, index, oldPage, page);
        mPageChangedListener.onPageRemoved(index, oldPage);
        mPageInfos.set(index, page);
        mPageChangedListener.onPageChanged(index, index, oldPage, page);
    }

    public void replaceLeftPage(Page leftPage) {
        if (!ThreadUtils.isInMainThread()) {
            mHandler.obtainMessage(MSG_LEFT_REPLACE, leftPage).sendToTarget();
            return;
        }
        if (null != leftPage && !leftPage.isTabPage()
                && prepareTabBar(leftPage, false)) {
            Log.w(TAG, "replace left page path is not valid tabbar path : " + leftPage.getPath());
            return;
        }
        Page oldLeftPage = getMultiWindowLeftPage();
        int leftPageIndex = getMultiWindowLeftPageIndex();
        if (leftPageIndex < 0 || leftPageIndex >= mPageInfos.size()) {
            Log.e(TAG, "replace left page fail! size=" + mPageInfos.size() + " left page index=" + leftPageIndex);
            return;
        }
        if (leftPage != null && oldLeftPage != null) {
            leftPage.setReferrer(oldLeftPage.getReferrer());
        }
        mPageChangedListener.onPagePreChange(leftPageIndex, leftPageIndex, oldLeftPage, leftPage);
        mPageChangedListener.onPageRemoved(leftPageIndex, oldLeftPage);
        mPageInfos.set(leftPageIndex, leftPage);
        mPageChangedListener.onPageChanged(leftPageIndex, leftPageIndex, oldLeftPage, leftPage);
    }

    public void back() {
        back(-1);
    }

    public boolean back(String path) {
        int index = getPageIndexByPath(path);
        if (index < 0) {
            back(-1);
            return false;
        }
        if (index == mPageInfos.size() - 1) {
            return true;
        }
        back(-(mPageInfos.size() - 1 - index));
        return true;
    }

    private void back(int changeIndex) {
        if (!ThreadUtils.isInMainThread()) {
            mHandler.obtainMessage(MSG_BACK, changeIndex, changeIndex).sendToTarget();
            return;
        }

        go(changeIndex);
    }

    public void clear() {
        clear(false);
    }

    public void clear(boolean isExit) {
        if (!ThreadUtils.isInMainThread()) {
            mHandler.obtainMessage(MSG_CLEAR, isExit).sendToTarget();
            return;
        }

        int pageCountLimit = 1;
        if (!isExit && hasMultiWindowLeftPage()) {
            pageCountLimit = 2;
        }
        int offset = 2;
        while (mPageInfos.size() > pageCountLimit) {
            int index = mPageInfos.size() - offset;
            if (mPageInfos.get(index).getPageId() == mMultiWindowLeftPageId) {
                offset += 1;
                continue;
            }
            Page page = mPageInfos.remove(index);
            mPageChangedListener.onPageRemoved(index, page);
        }
    }

    /**
     * 在折叠屏设备上，当设备折叠或者展开时，重建当前Page对象
     *
     * @return 重建后的Page对象
     * @throws PageNotFoundException page找不到的异常
     */
    public Page reloadOnFoldableDevice() throws PageNotFoundException {
        for (Page page : mPageInfos) {
            page.setShouldReload(true);
        }

        Page oldCurrPage = getCurrPage();
        if (oldCurrPage == null) {
            //do nothing
            return null;
        }
        Page page = null;
        if (null != oldCurrPage.getRoutableInfo()
                && ABOUT_PAGE_PATH.equals(oldCurrPage.getRoutableInfo().getUri())
                && oldCurrPage.getState() == Page.STATE_VISIBLE) {
            if (ABOUT_PAGE_PATH.equals(oldCurrPage.getRoutableInfo().getUri())) {
                page = buildAboutPage();
                replace(page);
            }
        } else if (null == oldCurrPage.getRequest()
                && null != mAppInfo
                && null != mAppInfo.getRouterInfo()
                && null != mAppInfo.getRouterInfo().getEntry()
                && oldCurrPage.getRoutableInfo() == mAppInfo.getRouterInfo().getEntry()
                && oldCurrPage.getState() == Page.STATE_VISIBLE) {
            page = buildHomePage();
            replace(page);
        } else if (oldCurrPage.getState() == Page.STATE_VISIBLE) {
            page = buildPage(oldCurrPage.getRequest());
            replace(page);
        }
        return page;
    }

    public Page reloadLeftPageOnFoldableDevice() throws PageNotFoundException {
        Page oldLeftPage = getMultiWindowLeftPage();
        if (oldLeftPage == null) {
            return null;
        }
        Page leftPage = null;
        if (null != oldLeftPage.getRoutableInfo()
                && ABOUT_PAGE_PATH.equals(oldLeftPage.getRoutableInfo().getUri())) {
            leftPage = buildAboutPage();
            replaceLeftPage(leftPage);
        } else if (null == oldLeftPage.getRequest()
                && null != mAppInfo
                && null != mAppInfo.getRouterInfo()
                && null != mAppInfo.getRouterInfo().getEntry()
                && oldLeftPage.getRoutableInfo() == mAppInfo.getRouterInfo().getEntry()) {
            leftPage = buildHomePage();
            replaceLeftPage(leftPage);
        } else {
            leftPage = buildPage(oldLeftPage.getRequest());
            replaceLeftPage(leftPage);
        }
        return leftPage;
    }

    public void reload() throws PageNotFoundException {
        for (Page page : mPageInfos) {
            page.setShouldReload(true);
        }

        Page oldCurrPage = getCurrPage();
        if (oldCurrPage == null) {
            // do nothing
            return;
        }
        if (oldCurrPage.getState() == Page.STATE_VISIBLE) {
            replace(buildPage(oldCurrPage.getRequest()));
        }
    }

    private void go(int changeIndex) {
        if (changeIndex > 0) {
            Log.w(TAG, "Not supported for go forward. index:" + changeIndex);
            return;
        }
        final int oldCurrIndex = getCurrIndex();
        int newCurrIndex = oldCurrIndex + changeIndex;
        Page oldPage = getPage(oldCurrIndex);
        Page newPage = getPage(newCurrIndex);
        if (null != newPage) {
            prepareTabBar(newPage, true);
        }
        mPageChangedListener.onPagePreChange(oldCurrIndex, newCurrIndex, oldPage, newPage);
        for (int i = oldCurrIndex; i > newCurrIndex && i >= 0; i--) {
            Page page = mPageInfos.remove(i);
            mPageChangedListener.onPageRemoved(i, page);
        }
        mPageChangedListener.onPageChanged(oldCurrIndex, newCurrIndex, oldPage, newPage);
    }

    public void finish(int pageId) {
        if (!ThreadUtils.isInMainThread()) {
            mHandler.obtainMessage(MSG_FINISH, pageId).sendToTarget();
            return;
        }
        Page page = getPageById(pageId);
        if (null != page) {
            Page currentPage = getCurrPage();
            if (page == currentPage) {
                back();
            } else {
                for (int i = 0; i < mPageInfos.size(); ++i) {
                    if (page == mPageInfos.get(i)) {
                        mPageInfos.remove(page);
                        mPageChangedListener.onPageRemoved(i, page);
                        break;
                    }
                }
            }
        }
    }

    public interface PageChangedListener {
        void onPagePreChange(int oldIndex, int newIndex, Page oldPage, Page newPage);

        void onPageChanged(int oldIndex, int newIndex, Page oldPage, Page newPage);

        void onPageRemoved(int index, Page page);
    }

    private boolean prepareTabBar(Page page, boolean isBack) {
        if (!ThreadUtils.isInMainThread()) {
            Log.w(TAG, "prepareTabBar not in main thread.");
            return false;
        }
        boolean isTabBar = false;
        if (null == page) {
            Log.w(TAG, "prepareTabBar page is null.");
            return false;
        }
        String path = page.getPath();
        RootView rootView = null;
        if (mPageChangedListener instanceof RootView) {
            rootView = ((RootView) mPageChangedListener);
        }
        if (null != rootView) {
            isTabBar = rootView.prepareTabBarPath(page.isTabPage(), path);
            if (isTabBar) {
                if (isBack) {
                    rootView.notifyTabBarChange(path);
                } else if (mPageInfos.size() == 0) {
                    isTabBar = false;
                    rootView.notifyTabBarChange(path);
                }
            }
        } else {
            Log.w(TAG, "prepareTabBar rootView null.");
        }
        return isTabBar;
    }

    private class HandlerImpl extends Handler {
        HandlerImpl() {
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_PUSH:
                    push((Page) msg.obj);
                    break;
                case MSG_REPLACE:
                    replace((Page) msg.obj);
                    break;
                case MSG_BACK:
                    back(msg.arg1);
                    break;
                case MSG_CLEAR:
                    clear((boolean) msg.obj);
                    break;
                case MSG_FINISH:
                    finish((Integer) msg.obj);
                    break;
                case MSG_LEFT_REPLACE:
                    replaceLeftPage((Page) msg.obj);
                default:
                    break;
            }
        }
    }
}

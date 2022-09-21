/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
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
    public PageCache mPageCache;
    private AppInfo mAppInfo;
    private List<Page> mPageInfos = new ArrayList<>();
    private PageChangedListener mPageChangedListener;
    private Handler mHandler;

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
                    page = buildPageByUri(hapRequest);
                    if (mPageInfos.size() != 0 && page.shouldCache()) {
                        mPageCache.put(request.getUri(), page);
                    }
                }
            } else {
                page = buildPageByUri(hapRequest);
                // 首页不放入子页面缓存池，配置不缓存不放入子页面缓存
                if (mPageInfos.size() != 0 && page.shouldCache()) {
                    Log.d(TAG, "ifCache:" + page.shouldCache());
                    mPageCache.put(request.getUri(), page);
                }
            }
        } else {
            page = buildPageByFilter(request);
        }
        if (page != null) {
            page.setRequest(request);
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

    private Page buildPageByUri(HybridRequest.HapRequest request) throws PageNotFoundException {
        if (!mAppInfo.getPackage().equals(request.getPackage())) {
            throw new PageNotFoundException("request is not for current app: " + request.getUri());
        }

        RoutableInfo routableInfo;
        String path = request.getPagePath();
        RouterInfo routerInfo = mAppInfo.getRouterInfo();
        boolean isPageNotFound = false;
        if (HapEngine.getInstance(request.getPackage()).isCardMode()) {
            routableInfo = routerInfo.getCardInfoByPath(request.getPagePath());
        } else {
            String pageName = request.getPageName();
            if (TextUtils.isEmpty(pageName)) {
                routableInfo = mAppInfo.getRouterInfo().getPageInfoByPath(path);
                if (routableInfo == null && "/".equals(path)) {
                    routableInfo = mAppInfo.getRouterInfo().getEntry();
                }
            } else {
                routableInfo = mAppInfo.getRouterInfo().getPageInfoByName(pageName);
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
                    && getCurrIndex() < 0) {
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
                        mAppInfo,
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

    private Page buildPageByFilter(HybridRequest request) throws PageNotFoundException {
        if (request == null) {
            throw new PageNotFoundException("request is null.");
        }
        RouterInfo routerInfo = mAppInfo.getRouterInfo();
        if (routerInfo != null) {
            PageInfo pageInfo = routerInfo.getPageInfoByFilter(request);
            if (pageInfo != null) {
                return new Page(
                        mAppInfo,
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
                return WebPage.create(this, request);
            }
        }

        throw new PageNotFoundException("No page found for request: " + request);
    }

    public void push(final Page page) {
        if (!ThreadUtils.isInMainThread()) {
            mHandler.obtainMessage(MSG_PUSH, page).sendToTarget();
            return;
        }

        List<String> flags = page.getLaunchFlags();
        if (flags != null) {
            if (flags.contains(PageInfo.FLAG_CLEAR_TASK) && mPageInfos.size() > 0) {
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

        mPageChangedListener.onPagePreChange(oldIndex, newIndex, oldPage, page);
        mPageInfos.add(page);
        mPageChangedListener.onPageChanged(oldIndex, newIndex, oldPage, page);
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
    }

    public void replace(Page page) {
        if (!ThreadUtils.isInMainThread()) {
            mHandler.obtainMessage(MSG_REPLACE, page).sendToTarget();
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
        if (!ThreadUtils.isInMainThread()) {
            mHandler.sendEmptyMessage(MSG_CLEAR);
            return;
        }

        while (mPageInfos.size() > 1) {
            int index = mPageInfos.size() - 2;
            Page page = mPageInfos.remove(index);
            mPageChangedListener.onPageRemoved(index, page);
        }
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
                    clear();
                    break;
                case MSG_FINISH:
                    finish((Integer) msg.obj);
                    break;
                default:
                    break;
            }
        }
    }
}

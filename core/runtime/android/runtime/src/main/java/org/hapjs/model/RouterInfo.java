/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.model;

import android.text.TextUtils;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.hapjs.bridge.HybridRequest;
import org.json.JSONObject;

public class RouterInfo {

    private static final String KEY_ENTRY = "entry";
    private static final String KEY_ERROR_PAGE = "errorPage";
    private static final String KEY_BACKGROUND = "background";
    private static final String KEY_PAGES = "pages";
    private static final String KEY_CARDS = "widgets";

    private PageInfo mEntry;
    private PageInfo mErrorPage;
    private PageInfo mDefaultErrorPage;
    private String mBackground;
    private Map<String, PageInfo> mPageInfos;
    private Map<String, CardInfo> mCardInfos;

    public static RouterInfo parse(JSONObject routerObject) {
        RouterInfo routerInfo = new RouterInfo();
        if (routerObject != null) {
            String entryName = routerObject.optString(KEY_ENTRY);
            routerInfo.mBackground = routerObject.optString(KEY_BACKGROUND);

            JSONObject pagesObject = routerObject.optJSONObject(KEY_PAGES);
            routerInfo.mPageInfos = parsePageInfos(pagesObject);
            routerInfo.mEntry = routerInfo.mPageInfos.get(entryName);

            routerInfo.mDefaultErrorPage =
                    new PageInfo(
                            "errorPage",
                            null,
                            "file:///android_asset/app/error-page.js",
                            "error-page",
                            null,
                            PageInfo.MODE_SINGLE_TASK);
            String errorPageName = routerObject.optString(KEY_ERROR_PAGE);
            PageInfo pageInfo = null;
            if (!TextUtils.isEmpty(errorPageName)) {
                pageInfo = routerInfo.mPageInfos.get(errorPageName);
            }
            if (pageInfo == null) {
                routerInfo.mErrorPage = routerInfo.mDefaultErrorPage;
            } else {
                routerInfo.mErrorPage = pageInfo;
            }

            JSONObject cardsObject = routerObject.optJSONObject(KEY_CARDS);
            routerInfo.mCardInfos = parseCardInfos(cardsObject);
        }
        return routerInfo;
    }

    private static Map<String, PageInfo> parsePageInfos(JSONObject pagesObject) {
        if (pagesObject == null) {
            return Collections.emptyMap();
        }
        Iterator<String> keys = pagesObject.keys();
        Map<String, PageInfo> pageInfos = new HashMap<>();
        while (keys.hasNext()) {
            String key = keys.next();
            PageInfo pageInfo = PageInfo.parse(key, pagesObject.optJSONObject(key));
            pageInfos.put(key, pageInfo);
        }
        return pageInfos;
    }

    private static Map<String, CardInfo> parseCardInfos(JSONObject widgetsObject) {
        if (widgetsObject == null) {
            return Collections.emptyMap();
        }
        Iterator<String> keys = widgetsObject.keys();
        Map<String, CardInfo> cardInfos = new HashMap<>();
        while (keys.hasNext()) {
            String key = keys.next();
            CardInfo cardInfo = CardInfo.parse(key, widgetsObject.optJSONObject(key));
            cardInfos.put(key, cardInfo);
        }
        return cardInfos;
    }

    public PageInfo getEntry() {
        return mEntry;
    }

    public PageInfo getErrorPage(boolean useDefault) {
        return useDefault ? mDefaultErrorPage : mErrorPage;
    }

    public String getBackground() {
        return mBackground;
    }

    public Map<String, PageInfo> getPageInfos() {
        return mPageInfos;
    }

    public Map<String, CardInfo> getCardInfos() {
        return mCardInfos;
    }

    public PageInfo getPageInfoByName(String pageName) {
        if (mPageInfos != null) {
            return mPageInfos.get(pageName);
        }
        return null;
    }

    public PageInfo getPageInfoByPath(String path) {
        if (mPageInfos == null || path == null) {
            return null;
        }

        if ("/".equals(path)) {
            return mEntry;
        }

        for (PageInfo page : mPageInfos.values()) {
            if (path.equals(page.getPath())) {
                return page;
            }
        }
        return null;
    }

    public PageInfo getPageInfoByFilter(HybridRequest request) {
        if (mPageInfos == null) {
            return null;
        }
        for (PageInfo page : mPageInfos.values()) {
            if (page.match(request)) {
                return page;
            }
        }
        return null;
    }

    public CardInfo getCardInfoByPath(String path) {
        if (mCardInfos == null || path == null) {
            return null;
        }

        for (CardInfo cardInfo : mCardInfos.values()) {
            if (path.equals(cardInfo.getPath())) {
                return cardInfo;
            }
        }

        return null;
    }
}

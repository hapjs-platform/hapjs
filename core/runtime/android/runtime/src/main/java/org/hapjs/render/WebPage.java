/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hapjs.bridge.HybridRequest;
import org.hapjs.common.utils.ColorUtil;
import org.hapjs.model.AppInfo;
import org.hapjs.model.PageInfo;

public class WebPage extends Page {
    private static final String PAGE_NAME = "System.Web";
    private static final String PAGE_PATH = "/system.web";
    private static final String PAGE_BASE_URL = "file:///android_asset/js/app/";
    private static final String PAGE_WEB_JS_PATH = "/web.js";

    private static final String PARAM_URL = "url";
    private static final String PARAM_ALLOW_THIRD_PARTY_COOKIES = "allowthirdpartycookies";
    private static final String PARAM_SHOW_LOADING_DIALOG = "showloadingdialog";
    private static final String PARAM_TITLE_BAR_TEXT_COLOR = "titleBarTextColor";
    private static final String PARAM_TITLE_BAR_BACKGROUND_COLOR = "titleBarBackgroundColor";
    private static final String PARAM_USER_AGENT = "useragent";

    private WebPage(
            AppInfo appInfo,
            PageInfo pageInfo,
            Map<String, ?> params,
            Map<String, ?> intent,
            int pageId,
            List<String> launchFlags) {
        super(appInfo, pageInfo, params, intent, pageId, launchFlags);
    }

    public static WebPage create(AppInfo appInfo, Page currPage, HybridRequest request) {
        PageInfo webInfo =
                new PageInfo(
                        PAGE_NAME,
                        PAGE_PATH,
                        PAGE_BASE_URL + appInfo.getConfigInfo().getDslName() + PAGE_WEB_JS_PATH,
                        "web",
                        null,
                        PageInfo.MODE_STANDARD);
        Map<String, Object> params = new HashMap<>();
        params.put(PARAM_URL, request.getUri());
        params.put(PARAM_ALLOW_THIRD_PARTY_COOKIES, request.isAllowThirdPartyCookies());
        params.put(PARAM_SHOW_LOADING_DIALOG, request.isShowLoadingDialog());
        params.put(PARAM_USER_AGENT, request.getUserAgent());
        if (currPage != null) {
            int titleBarTextColor = currPage.getTitleBarTextColor();
            int titleBarBackgroundColor = currPage.getTitleBarBackgroundColor();
            params.put(PARAM_TITLE_BAR_TEXT_COLOR, ColorUtil.getColorText(titleBarTextColor));
            params.put(PARAM_TITLE_BAR_BACKGROUND_COLOR,
                    ColorUtil.getColorText(titleBarBackgroundColor));
        }
        WebPage webPage =
                new WebPage(
                        appInfo,
                        webInfo,
                        params,
                        request.getIntent(),
                        IdGenerator.generatePageId(),
                        request.getLaunchFlags());
        webPage.setRequest(request);
        return webPage;
    }

    @Override
    public boolean hasTitleBar() {
        return true;
    }
}

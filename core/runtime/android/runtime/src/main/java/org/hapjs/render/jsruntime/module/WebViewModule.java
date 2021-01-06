/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.jsruntime.module;

import org.hapjs.bridge.Extension;
import org.hapjs.bridge.HybridRequest;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.ModuleExtensionAnnotation;
import org.hapjs.common.utils.RouterUtils;
import org.hapjs.model.AppInfo;
import org.hapjs.render.PageManager;
import org.hapjs.render.PageNotFoundException;
import org.hapjs.render.RootView;
import org.hapjs.render.jsruntime.serialize.SerializeException;
import org.hapjs.render.jsruntime.serialize.SerializeObject;

@ModuleExtensionAnnotation(
        name = WebViewModule.NAME,
        actions = {
                @ActionAnnotation(name = WebViewModule.ACTION_LOAD_URL, mode = Extension.Mode.SYNC)})
public class WebViewModule extends ModuleExtension {
    protected static final String NAME = "system.webview";
    protected static final String ACTION_LOAD_URL = "loadUrl";
    protected static final String PARAM_URL = "url";
    protected static final String PARAM_ALLOW_THIRD_PARTY_COOKIES = "allowthirdpartycookies";
    protected static final String PARAM_SHOW_LOADING_DIALOG = "showloadingdialog";
    private static final String PARAM_USER_AGENT = "useragent";

    private PageManager mPageManager;

    @Override
    public void attach(RootView rootView, PageManager pageManager, AppInfo appInfo) {
        mPageManager = pageManager;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Response invokeInner(Request request) throws Exception {
        String action = request.getAction();
        SerializeObject params = request.getSerializeParams();

        if (ACTION_LOAD_URL.equals(action)) {
            return loadUrl(params);
        } else {
            return Response.NO_ACTION;
        }
    }

    private Response loadUrl(SerializeObject params)
            throws PageNotFoundException, SerializeException {
        String url = params.getString(PARAM_URL);
        boolean allowThridPartyCookies = params.optBoolean(PARAM_ALLOW_THIRD_PARTY_COOKIES);
        boolean showLoadingDialog = params.optBoolean(PARAM_SHOW_LOADING_DIALOG, false);
        String userAgent = params.optString(PARAM_USER_AGENT, "");
        HybridRequest request = new HybridRequest.Builder()
                .pkg(mPageManager.getAppInfo().getPackage())
                .uri(url)
                .allowThirdPartyCookies(allowThridPartyCookies)
                .showLoadingDialog(showLoadingDialog)
                .userAgent(userAgent)
                .build();
        RouterUtils.push(mPageManager, request);
        return Response.SUCCESS;
    }
}

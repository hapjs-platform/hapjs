/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.jsruntime.module;

import android.content.Context;
import android.util.Log;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.hapjs.bridge.Extension;
import org.hapjs.bridge.HybridRequest;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.ModuleExtensionAnnotation;
import org.hapjs.common.utils.RouterUtils;
import org.hapjs.model.AppInfo;
import org.hapjs.render.Page;
import org.hapjs.render.PageManager;
import org.hapjs.render.RootView;
import org.hapjs.render.jsruntime.serialize.SerializeException;
import org.hapjs.render.jsruntime.serialize.SerializeObject;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@ModuleExtensionAnnotation(
        name = RouterModule.NAME,
        actions = {
                @ActionAnnotation(name = RouterModule.ACTION_BACK, mode = Extension.Mode.SYNC),
                @ActionAnnotation(name = RouterModule.ACTION_PUSH, mode = Extension.Mode.SYNC),
                @ActionAnnotation(name = RouterModule.ACTION_REPLACE, mode = Extension.Mode.SYNC),
                @ActionAnnotation(name = RouterModule.ACTION_CLEAR, mode = Extension.Mode.SYNC),
                @ActionAnnotation(name = RouterModule.ACTION_GET_LENGTH, mode = Extension.Mode.SYNC),
                @ActionAnnotation(name = RouterModule.ACTION_GET_PAGE_LIST, mode = Extension.Mode.SYNC),
                @ActionAnnotation(name = RouterModule.ACTION_GET_STATE, mode = Extension.Mode.SYNC),
                @ActionAnnotation(name = RouterModule.ACTION_SWITCH_TAB, mode = Extension.Mode.SYNC),
        })
public class RouterModule extends ModuleExtension {

    protected static final String NAME = "system.router";
    protected static final String ACTION_BACK = "back";
    protected static final String ACTION_PUSH = "push";
    protected static final String ACTION_SWITCH_TAB = "switchTab";
    protected static final String ACTION_REPLACE = "replace";
    protected static final String ACTION_CLEAR = "clear";
    protected static final String ACTION_GET_LENGTH = "getLength";
    protected static final String ACTION_GET_PAGE_LIST = "getPages";
    protected static final String ACTION_GET_STATE = "getState";
    protected static final String RESULT_INDEX = "index";
    protected static final String RESULT_NAME = "name";
    protected static final String RESULT_PATH = "path";
    private static final String TAG = "RouterModule";
    private Context mContext;
    private PageManager mPageManager;

    @Override
    public void attach(RootView rootView, PageManager pageManager, AppInfo appInfo) {
        mContext = rootView.getContext(); // must use an activity context
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
        if (ACTION_BACK.equals(action)) {
            return back(params);
        } else if (ACTION_PUSH.equals(action)) {
            return push(params);
        } else if (ACTION_REPLACE.equals(action)) {
            return replace(params);
        } else if (ACTION_CLEAR.equals(action)) {
            return clear();
        } else if (ACTION_GET_LENGTH.equals(action)) {
            return getLength();
        } else if (ACTION_GET_PAGE_LIST.equals(action)) {
            return getPageList();
        } else if (ACTION_GET_STATE.equals(action)) {
            return getState();
        } else if (ACTION_SWITCH_TAB.equals(action)) {
            return switchTab(params);
        } else {
            return Response.NO_ACTION;
        }
    }

    private Response back(SerializeObject params) {
        HybridRequest request = null;
        try {
            request = parsePathRequest(params);
        } catch (SerializeException e) {
            Log.e(TAG, "no uri param, default back");
        }
        boolean result = RouterUtils.back(mContext, mPageManager, request);
        return result ? Response.SUCCESS : Response.ERROR;
    }

    private Response push(SerializeObject params) throws SerializeException {
        HybridRequest request = parseRequest(params);
        boolean result = RouterUtils.router(mContext, mPageManager, request);
        return result ? Response.SUCCESS : Response.ERROR;
    }

    private Response switchTab(SerializeObject params) throws SerializeException {
        HybridRequest request = parseRequest(params);
        boolean result = RouterUtils.switchTab(mContext, mPageManager, request);
        return result ? Response.SUCCESS : Response.ERROR;
    }

    private Response replace(SerializeObject params) throws SerializeException {
        if (mPageManager == null) {
            return Response.ERROR;
        }
        HybridRequest request = parseRequest(params);
        RouterUtils.replace(mPageManager, request);
        return Response.SUCCESS;
    }

    private Response clear() {
        if (mPageManager == null) {
            return Response.ERROR;
        }
        mPageManager.clear();
        return Response.SUCCESS;
    }

    private HybridRequest parsePathRequest(SerializeObject params) throws SerializeException {
        HybridRequest.Builder builder = new HybridRequest.Builder();
        builder.pkg(mPageManager.getAppInfo().getPackage());
        builder.uri(params.getString("path"));
        return builder.build();
    }

    private HybridRequest parseRequest(SerializeObject params) throws SerializeException {
        HybridRequest.Builder builder = new HybridRequest.Builder();
        builder.pkg(mPageManager.getAppInfo().getPackage());
        builder.uri(params.optString("uri"));
        SerializeObject rawPageParams = params.optSerializeObject("params");
        if (rawPageParams != null) {
            Map<String, String> pageParams = new HashMap<>();
            Iterator<String> keys = rawPageParams.keySet().iterator();
            while (keys.hasNext()) {
                String key = keys.next();
                pageParams.put(key, rawPageParams.getString(key));
            }
            builder.params(pageParams);
        }
        return builder.build();
    }

    private Response getLength() {
        if (mPageManager == null) {
            return Response.ERROR;
        }
        return new Response(mPageManager.getPageCount());
    }

    private Response getPageList() throws JSONException {
        if (mPageManager == null) {
            return Response.ERROR;
        }

        JSONArray pageArray = new JSONArray();
        for (Page page : mPageManager.getPageInfos()) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id", page.getPageId());
            jsonObject.put("name", page.getName());
            jsonObject.put("path", page.getPath());
            pageArray.put(jsonObject);
        }
        return new Response(pageArray);
    }

    private Response getState() throws JSONException {
        if (mPageManager == null) {
            return new Response(null);
        }
        Page page = mPageManager.getCurrPage();
        JSONObject pageObject = null;
        if (page != null) {
            pageObject = new JSONObject();
            pageObject.put(RESULT_INDEX, mPageManager.getCurrIndex());
            pageObject.put(RESULT_NAME, page.getName());
            pageObject.put(RESULT_PATH, page.getPath());
        }
        return new Response(pageObject);
    }
}

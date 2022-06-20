/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.jsruntime.module;

import android.graphics.Rect;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import org.hapjs.bridge.Extension;
import org.hapjs.bridge.NativeInterface;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.ModuleExtensionAnnotation;
import org.hapjs.common.utils.DisplayUtil;
import org.hapjs.model.AppInfo;
import org.hapjs.render.DecorLayout;
import org.hapjs.render.Display;
import org.hapjs.render.PageManager;
import org.hapjs.render.RootView;
import org.hapjs.render.vdom.DocComponent;
import org.hapjs.render.vdom.VDocument;
import org.hapjs.runtime.HapEngine;
import org.json.JSONException;
import org.json.JSONObject;

@ModuleExtensionAnnotation(
        name = PageModule.NAME,
        actions = {
                @ActionAnnotation(name = PageModule.ACTION_FINISH_PAGE, mode = Extension.Mode.SYNC),
                @ActionAnnotation(name = PageModule.ACTION_GET_MENUBAR_RECT, mode = Extension.Mode.SYNC),
                @ActionAnnotation(name = PageModule.ACTION_SET_MENUBAR_DATA, mode = Extension.Mode.SYNC),
                @ActionAnnotation(name = PageModule.ACTION_GET_MENUBAR_BOUNDING_RECT, mode = Extension.Mode.SYNC)
        }
)
public class PageModule extends ModuleExtension {

    protected static final String NAME = "system.page";
    protected static final String ACTION_FINISH_PAGE = "finishPage";
    protected static final String ACTION_GET_MENUBAR_RECT = "getMenuBarRect";
    protected static final String ACTION_GET_MENUBAR_BOUNDING_RECT = "getMenuBarBoundingRect";
    protected static final String RESULT_MENU_BAR_WIDTH = "menuBarWidth";
    protected static final String RESULT_MENU_BAR_HEIGHT = "menuBarHeight";
    protected static final String RESULT_MENU_BAR_LEFT = "menuBarLeft";
    protected static final String RESULT_MENU_BAR_TOP = "menuBarTop";
    protected static final String RESULT_MENU_BAR_RIGHT = "menuBarRight";
    protected static final String RESULT_MENU_BAR_BOTTOM = "menuBarBottom";
    protected static final String ACTION_SET_MENUBAR_DATA = "setMenubarData";
    private static final String TAG = "PageModule";
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
        String params = request.getRawParams();
        if (ACTION_FINISH_PAGE.equals(action)) {
            return finishPage(params);
        } else if (ACTION_GET_MENUBAR_RECT.equals(action)) {
            return getMenuBarRect(request);
        } else if (ACTION_GET_MENUBAR_BOUNDING_RECT.equals(action)) {
            return getMenuBarBoundingRect(request);
        } else if (ACTION_SET_MENUBAR_DATA.equals(action)) {
            return setMenuBarData(request);
        }
        return Response.NO_ACTION;
    }

    private Response finishPage(String params) {
        int pageId = Integer.parseInt(params);
        mPageManager.finish(pageId);
        return Response.SUCCESS;
    }

    private Response getMenuBarRect(Request request) throws JSONException {
        JSONObject result = new JSONObject();
        Display display = getDisPlay(request);
        View menubarView = null;
        if (null != display) {
            menubarView = display.getMenuBar();
        }
        Rect rect = new Rect();
        if (null != menubarView) {
            menubarView.getGlobalVisibleRect(rect);
        }
        int designWidth = -1;
        HapEngine hapEngine = null;
        if (null != request) {
            hapEngine = request.getHapEngine();
        }
        if (null != hapEngine) {
            designWidth = hapEngine.getDesignWidth();
        }
        if (null != menubarView && designWidth > 0) {
            result.put(RESULT_MENU_BAR_WIDTH, DisplayUtil.getDesignPxByWidth(menubarView.getWidth(), designWidth));
            result.put(RESULT_MENU_BAR_HEIGHT, DisplayUtil.getDesignPxByWidth(menubarView.getHeight(), designWidth));
            result.put(RESULT_MENU_BAR_LEFT, DisplayUtil.getDesignPxByWidth(rect.left, designWidth));
            result.put(RESULT_MENU_BAR_TOP, DisplayUtil.getDesignPxByWidth(rect.top, designWidth));
            result.put(RESULT_MENU_BAR_RIGHT, DisplayUtil.getDesignPxByWidth(rect.right, designWidth));
            result.put(RESULT_MENU_BAR_BOTTOM, DisplayUtil.getDesignPxByWidth(rect.bottom, designWidth));
        } else {
            result.put(RESULT_MENU_BAR_WIDTH, null != menubarView ? menubarView.getWidth() : -1);
            result.put(RESULT_MENU_BAR_HEIGHT, null != menubarView ? menubarView.getHeight() : -1);
            result.put(RESULT_MENU_BAR_LEFT, rect.left);
            result.put(RESULT_MENU_BAR_TOP, rect.top);
            result.put(RESULT_MENU_BAR_RIGHT, rect.right);
            result.put(RESULT_MENU_BAR_BOTTOM, rect.bottom);
        }
        return new Response(result);
    }

    private Response getMenuBarBoundingRect(Request request) throws JSONException {
        JSONObject result = new JSONObject();
        Display display = getDisPlay(request);
        View menubarView = null;
        if (null != display) {
            menubarView = display.getMenuBar();
        }
        Rect rect = new Rect();
        if (null != menubarView) {
            menubarView.getGlobalVisibleRect(rect);
        }
        int designWidth = -1;
        HapEngine hapEngine = null;
        if (null != request) {
            hapEngine = request.getHapEngine();
        }
        if (null != hapEngine) {
            designWidth = hapEngine.getDesignWidth();
        }
        if (null != menubarView && designWidth > 0) {
            result.put(RESULT_MENU_BAR_WIDTH, DisplayUtil.getDesignPxByWidth(menubarView.getWidth(), designWidth));
            result.put(RESULT_MENU_BAR_HEIGHT, DisplayUtil.getDesignPxByWidth(menubarView.getHeight(), designWidth));
            result.put(RESULT_MENU_BAR_LEFT, DisplayUtil.getDesignPxByWidth(rect.left, designWidth));
            result.put(RESULT_MENU_BAR_TOP, DisplayUtil.getDesignPxByWidth(rect.top, designWidth));
            result.put(RESULT_MENU_BAR_RIGHT, DisplayUtil.getDesignPxByWidth(rect.right, designWidth));
            result.put(RESULT_MENU_BAR_BOTTOM, DisplayUtil.getDesignPxByWidth(rect.bottom, designWidth));
        } else {
            result.put(RESULT_MENU_BAR_WIDTH, -1);
            result.put(RESULT_MENU_BAR_HEIGHT, -1);
            result.put(RESULT_MENU_BAR_LEFT, rect.left);
            result.put(RESULT_MENU_BAR_TOP, rect.top);
            result.put(RESULT_MENU_BAR_RIGHT, rect.right);
            result.put(RESULT_MENU_BAR_BOTTOM, rect.bottom);
            Log.w(TAG, "getMenuBarBoundingRect menubarView or designWidth is not valid.");
        }
        return new Response(result);
    }

    private Response setMenuBarData(Request request) {
        if (null == request) {
            Log.e(TAG, "setMenuBarData request is null.");
            return Response.ERROR;
        }
        JSONObject params = null;
        try {
            JSONObject jsonParams = request.getJSONParams();
            if (null != jsonParams && jsonParams.has("attr")) {
                params = jsonParams.getJSONObject("attr");
            }
        } catch (JSONException e) {
            Log.e(TAG, " invokeInner setMenubarData jsonParams is null.");
            return Response.ERROR;
        }
        Display display = getDisPlay(request);
        if (null != display) {
            display.refreshMenubarShareData(params);
            return Response.SUCCESS;
        }
        return Response.ERROR;
    }

    private Display getDisPlay(Request request) {
        NativeInterface nativeInterface = null;
        RootView rootView = null;
        VDocument vDocument = null;
        DocComponent docComponent = null;
        DecorLayout decorLayout = null;
        Display display = null;
        if (null != request) {
            nativeInterface = request.getNativeInterface();
        }
        if (null != nativeInterface) {
            rootView = nativeInterface.getRootView();
        }
        if (null != rootView) {
            vDocument = rootView.getDocument();
        }
        if (null != vDocument) {
            docComponent = vDocument.getComponent();
        }
        if (null != docComponent) {
            ViewGroup viewGroup = docComponent.getInnerView();
            if (viewGroup instanceof DecorLayout) {
                decorLayout = (DecorLayout) viewGroup;
            }
        }
        if (null != decorLayout) {
            display = decorLayout.getDecorLayoutDisPlay();
        }
        return display;
    }
}

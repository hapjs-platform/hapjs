/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.canvas;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import java.util.Map;
import org.hapjs.bridge.Callback;
import org.hapjs.bridge.Extension;
import org.hapjs.bridge.LifecycleListener;
import org.hapjs.bridge.NativeInterface;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.WidgetExtension;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.WidgetExtensionAnnotation;
import org.hapjs.bridge.storage.file.InternalUriUtils;
import org.hapjs.common.utils.UriUtils;
import org.hapjs.component.Component;
import org.hapjs.render.Page;
import org.hapjs.render.PageManager;
import org.hapjs.render.jsruntime.serialize.JavaSerializeObject;
import org.hapjs.runtime.HapEngine;
import org.hapjs.widgets.canvas.image.CanvasImageHelper;
import org.json.JSONObject;

@WidgetExtensionAnnotation(
        name = CanvasExtension.FEATURE_NAME,
        actions = {
                @ActionAnnotation(name = CanvasExtension.ACTION_ENABLE, mode = Extension.Mode.SYNC),
                @ActionAnnotation(name = CanvasExtension.ACTION_GET_CONTEXT, mode = Extension.Mode.SYNC),
                @ActionAnnotation(name = CanvasExtension.ACTION_PRELOAD_IMAGE, mode = Extension.Mode.ASYNC),
                @ActionAnnotation(name = CanvasExtension.ACTION_CALL_NATIVE_2D, mode = Extension.Mode.SYNC),
                @ActionAnnotation(
                        name = CanvasExtension.ACTION_CALL_NATIVE_2D_SYNC,
                        mode = Extension.Mode.SYNC)
        })
public class CanvasExtension extends WidgetExtension implements Canvas.CanvasLifecycle {

    protected static final String FEATURE_NAME = "system.canvas";
    protected static final String ACTION_ENABLE = "enable";
    protected static final String ACTION_GET_CONTEXT = "getContext";
    protected static final String ACTION_PRELOAD_IMAGE = "preloadImage";
    protected static final String ACTION_CALL_NATIVE_2D = "canvasNative2D";
    protected static final String ACTION_CALL_NATIVE_2D_SYNC = "canvasNative2DSync";
    private static final String TAG = "CanvasExtension";
    private boolean mHasRegisterLifecycleListener = false;

    @Override
    public String getName() {
        return FEATURE_NAME;
    }

    @Override
    protected Response invokeInner(Request request) throws Exception {
        String action = request.getAction();
        if (TextUtils.isEmpty(action)) {
            return Response.NO_ACTION;
        }

        if (!mHasRegisterLifecycleListener) {
            NativeInterface nativeInterface = request.getNativeInterface();
            if (nativeInterface != null) {
                nativeInterface.addLifecycleListener(
                        new LifecycleListener() {
                            @Override
                            public void onDestroy() {
                                super.onDestroy();
                                CanvasActionHandler.getInstance().exist();
                            }
                        });
                mHasRegisterLifecycleListener = true;
            }
        }

        if (ACTION_GET_CONTEXT.equals(action)) {
            // getContext
            getContext(request);
            return Response.SUCCESS;
        } else if (ACTION_PRELOAD_IMAGE.equals(action)) {
            // preloadImage
            preloadImage(request);
            return Response.SUCCESS;
        } else if (ACTION_CALL_NATIVE_2D.equals(action)) {
            // canvasNative2D
            canvasNative2D(request);
            return Response.SUCCESS;
        } else if (ACTION_CALL_NATIVE_2D_SYNC.equals(action)) {
            // canvasNative2DSync
            return canvasNative2DSync(request);
        }
        return Response.NO_ACTION;
    }

    @Override
    public void destroy(String refId) {
    }

    /**
     * 触发组件canvas调用getContext，无需返回值
     *
     * @param request
     */
    private void getContext(Request request) {
        try {
            String packageName = request.getHapEngine().getPackage();
            CanvasManager.getInstance().setPackageName(packageName);

            JSONObject params = request.getJSONParams();
            if (params != null) {
                int pageId = Integer.parseInt(params.optString("pageId", ""));
                int ref = Integer.parseInt(params.optString("componentId", ""));
                String type = params.optString("type", "");
                CanvasManager.getInstance().getContext(pageId, ref, type);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void preloadImage(Request request) {
        try {
            JSONObject jsonParams = request.getJSONParams();
            String url = jsonParams.optString("url");
            Object id = jsonParams.opt("id");
            Callback callback = request.getCallback();

            Uri uri = parseUri(url, request);
            CanvasImageHelper.getInstance().loadImage(uri, id, callback);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Uri parseUri(String url, Request request) {
        Uri uri = null;

        if (!TextUtils.isEmpty(url)) {
            uri = UriUtils.computeUri(url);
            if (uri == null) {
                String packageName = request.getApplicationContext().getPackage();
                PageManager pageManager =
                        request.getNativeInterface().getRootView().getPageManager();
                if (pageManager != null) {
                    Page currPage = pageManager.getCurrPage();
                    if (currPage != null) {
                        uri =
                                HapEngine.getInstance(packageName)
                                        .getResourceManager()
                                        .getResource(url, currPage.getPath());
                    }
                }
            }

            if (uri != null && InternalUriUtils.isInternalUri(uri)) {
                uri = request.getApplicationContext().getUnderlyingUri(uri.toString());
            }
        }

        return uri;
    }

    private void canvasNative2D(Request request) {
        try {
            JSONObject jsonParams = request.getJSONParams();
            int pageId = jsonParams.optInt("pageId", Component.INVALID_PAGE_ID);
            if (pageId == Component.INVALID_PAGE_ID) {
                Log.e(TAG, "canvasNative2D,pageId is invalid,termination!");
                return;
            }

            int ref = jsonParams.optInt("componentId");
            String commands = jsonParams.optString("commands");
            CanvasActionHandler.getInstance().processAsyncActions(pageId, ref, commands);
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

    private Response canvasNative2DSync(Request request) {

        try {
            JSONObject jsonParams = request.getJSONParams();
            int pageId = jsonParams.optInt("pageId", Component.INVALID_PAGE_ID);
            if (pageId == Component.INVALID_PAGE_ID) {
                JSONObject info = new JSONObject();
                info.put("error", "invalid pageId");
                return new Response(info);
            }

            int ref = jsonParams.optInt("componentId");
            String command = jsonParams.optString("commands");
            Map<String, Object> result =
                    CanvasActionHandler.getInstance().processSyncAction(pageId, ref, command);
            return new Response(new JavaSerializeObject(result));
        } catch (Exception e) {
            return getExceptionResponse(request.getAction(), e);
        }
    }

    @Override
    public boolean isBuiltInExtension() {
        return true;
    }
}

/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.inspector;

import static com.facebook.stetho.inspector.ChromeDiscoveryHandler.setSuccessfulResponse;

import android.graphics.Rect;
import android.util.Log;
import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;
import com.facebook.stetho.common.android.HandlerUtil;
import com.facebook.stetho.server.SocketLike;
import com.facebook.stetho.server.SocketLikeHandler;
import com.facebook.stetho.server.http.ExactPathMatcher;
import com.facebook.stetho.server.http.HandlerRegistry;
import com.facebook.stetho.server.http.HttpHandler;
import com.facebook.stetho.server.http.HttpStatus;
import com.facebook.stetho.server.http.LightHttpBody;
import com.facebook.stetho.server.http.LightHttpRequest;
import com.facebook.stetho.server.http.LightHttpResponse;
import com.facebook.stetho.server.http.LightHttpServer;
import com.facebook.stetho.server.http.RegexpPathMatcher;
import java.io.IOException;
import java.util.regex.Pattern;
import org.hapjs.common.utils.DisplayUtil;
import org.hapjs.render.DecorLayout;
import org.hapjs.render.Page;
import org.hapjs.render.PageManager;
import org.hapjs.render.RootView;
import org.hapjs.render.jsruntime.AppJsThread;
import org.hapjs.render.vdom.DocComponent;
import org.hapjs.render.vdom.VDocument;
import org.json.JSONArray;
import org.json.JSONObject;

public class CDPHttpSocketLikeHandler implements SocketLikeHandler {
    public static final String PATH_PAGES = "/pages";
    public static final String PATH_RUNTIME = "/runtime";
    public static final String PATH_SCREEN = "/screen";
    private static final String TAG = "CDPSocketHandler";
    private final LightHttpServer mServer;

    public CDPHttpSocketLikeHandler() {
        HandlerRegistry registry = new HandlerRegistry();

        CDPHttpHandler cdpHttpHandler = new CDPHttpHandler();
        registry.register(new ExactPathMatcher(PATH_PAGES), cdpHttpHandler);
        registry.register(new ExactPathMatcher(PATH_RUNTIME), cdpHttpHandler);
        registry.register(new ExactPathMatcher(PATH_SCREEN), cdpHttpHandler);
        registry.register(
                new RegexpPathMatcher(Pattern.compile("/sourcemap/.*")),
                new SourcemapHttpSocketLikeHandler.SourcemapHttpHandler());

        mServer = new LightHttpServer(registry);
    }

    @Override
    public void onAccepted(SocketLike socket) throws IOException {
        mServer.serve(socket);
    }

    public static class CDPHttpHandler implements HttpHandler {

        @Override
        public boolean handleRequest(
                SocketLike socket, final LightHttpRequest request, final LightHttpResponse response)
                throws IOException {
            String path = request.uri.getPath();
            response.addHeader("Access-Control-Allow-Origin", "*");
            response.addHeader("Access-Control-Allow-Methods", "*");
            response.addHeader("Access-Control-Allow-Headers", "*");
            response.addHeader("Access-Control-Expose-Headers", "*");
            response.addHeader("Access-Control-Max-Age", "3600");

            if ("options".equals(request.method.toLowerCase())) {
                Log.e(TAG, "options handle");
                response.body = LightHttpBody.create("", "text/plain");
                return true;
            }

            Log.d(TAG, "start handling path = " + path);
            try {
                if (PATH_PAGES.equals(path)) {
                    RootView rootView = V8Inspector.getInstance().getRootView();
                    if (rootView != null) {
                        PageManager pageManager = rootView.mPageManager;
                        JSONObject pages = new JSONObject();
                        JSONArray pageArray = new JSONArray();
                        for (Page p : pageManager.getPageInfos()) {
                            JSONObject j = new JSONObject();
                            j.put("pageId", p.getPageId());
                            j.put("name", p.getName());
                            j.put("path", p.getPath());
                            pageArray.put(j);
                        }
                        pages.put("pages", pageArray);
                        setSuccessfulResponse(
                                response,
                                LightHttpBody.create(pages.toString(), "application/json"));
                    } else {
                        Log.e(TAG, "can't get pages.");
                        response.code = HttpStatus.HTTP_INTERNAL_SERVER_ERROR;
                        response.body = LightHttpBody.create("can't find rootview", "text/plain");
                    }
                } else if (PATH_RUNTIME.equals(path)) {
                    RootView rootView = V8Inspector.getInstance().getRootView();
                    if (rootView != null) {
                        final AppJsThread jsThread = rootView.getJsThread();
                        // Stetho doesn't has request body, so here we are
                        Runnable r =
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        String message = request.getFirstHeaderValue("message");
                                        Log.d(TAG, "message = " + message);
                                        String result = jsThread.getEngine().executeObjectScriptAndStringify("v = " + message);
                                        Log.d(TAG, "result = " + result);

                                        setSuccessfulResponse(
                                                response,
                                                LightHttpBody.create(result, "application/json"));
                                    }
                                };
                        HandlerUtil.postAndWait(jsThread.getHandler(), r);

                    } else {
                        Log.e(TAG, "can't get pages.");
                        response.code = HttpStatus.HTTP_INTERNAL_SERVER_ERROR;
                        response.body = LightHttpBody.create("can't find rootview", "text/plain");
                    }
                } else if (PATH_SCREEN.equals(path)) {
                    RootView rootView = V8Inspector.getInstance().getRootView();
                    if (rootView != null) {
                        int designWidth = V8Inspector.getInstance().getHapEngine().getDesignWidth();
                        JSONObject result = new JSONObject();
                        JSONObject screen = new JSONObject();
                        screen.put(
                                "height",
                                DisplayUtil.getDesignPxByWidth(rootView.getHeight(), designWidth)
                                        + "px");
                        screen.put(
                                "width",
                                DisplayUtil.getDesignPxByWidth(rootView.getWidth(), designWidth)
                                        + "px");
                        result.put("screen", screen);
                        VDocument document = rootView.getDocument();
                        if (document != null) {
                            DocComponent component = document.getComponent();
                            if (component != null) {
                                DecorLayout decorLayout = component.getDecorLayout();
                                if (decorLayout != null) {
                                    Rect rect = decorLayout.getContentInsets();
                                    JSONObject contentInsets = new JSONObject();
                                    contentInsets.put(
                                            "left",
                                            DisplayUtil.getDesignPxByWidth(rect.left, designWidth)
                                                    + "px");
                                    contentInsets.put(
                                            "top",
                                            DisplayUtil.getDesignPxByWidth(rect.top, designWidth)
                                                    + "px");
                                    result.put("contentInsets", contentInsets);
                                }
                            }
                        }
                        setSuccessfulResponse(
                                response,
                                LightHttpBody.create(result.toString(), "application/json"));
                    } else {
                        Log.e(TAG, "can't get screen.");
                        response.code = HttpStatus.HTTP_INTERNAL_SERVER_ERROR;
                        response.body = LightHttpBody.create("can't find rootview", "text/plain");
                    }
                } else {
                    response.code = HttpStatus.HTTP_NOT_IMPLEMENTED;
                    response.reasonPhrase = "Not implemented";
                    response.body =
                            LightHttpBody.create("No support for " + path + "\n", "text/plain");
                }
            } catch (Exception e) {
                response.code = HttpStatus.HTTP_INTERNAL_SERVER_ERROR;
                response.reasonPhrase = "Internal server error";
                response.body = LightHttpBody.create(e.toString() + "\n", "text/plain");
            }

            Log.d(TAG, "done handling path = " + path);
            return true;
        }
    }
}

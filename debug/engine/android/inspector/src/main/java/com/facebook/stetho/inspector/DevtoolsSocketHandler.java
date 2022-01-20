/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.inspector;

import static org.hapjs.inspector.CDPHttpSocketLikeHandler.PATH_PAGES;
import static org.hapjs.inspector.CDPHttpSocketLikeHandler.PATH_RUNTIME;
import static org.hapjs.inspector.CDPHttpSocketLikeHandler.PATH_SCREEN;

import android.content.Context;
import com.facebook.stetho.inspector.protocol.ChromeDevtoolsDomain;
import com.facebook.stetho.server.SocketLike;
import com.facebook.stetho.server.SocketLikeHandler;
import com.facebook.stetho.server.http.ExactPathMatcher;
import com.facebook.stetho.server.http.HandlerRegistry;
import com.facebook.stetho.server.http.LightHttpServer;
import com.facebook.stetho.server.http.RegexpPathMatcher;
import com.facebook.stetho.websocket.WebSocketHandler;
import java.io.IOException;
import java.util.regex.Pattern;
import org.hapjs.inspector.CDPHttpSocketLikeHandler;
import org.hapjs.inspector.ReportInspectorInfo;
import org.hapjs.inspector.SourcemapHttpSocketLikeHandler;

public class DevtoolsSocketHandler implements SocketLikeHandler {
    private final Context mContext;
    private final Iterable<ChromeDevtoolsDomain> mModules;
    private final LightHttpServer mServer;

    public DevtoolsSocketHandler(Context context, Iterable<ChromeDevtoolsDomain> modules) {
        mContext = context;
        mModules = modules;
        mServer = createServer();
    }

    private LightHttpServer createServer() {
        HandlerRegistry registry = new HandlerRegistry();

        ChromeDiscoveryHandler discoveryHandler =
                new ChromeDiscoveryHandler(mContext, ChromeDevtoolsServer.PATH);
        discoveryHandler.register(registry);
        registry.register(
                new ExactPathMatcher(ChromeDevtoolsServer.PATH),
                new WebSocketHandler(new ChromeDevtoolsServer(mModules)));

        // INSPECTOR ADD
        CDPHttpSocketLikeHandler.CDPHttpHandler cdpHttpHandler =
                new CDPHttpSocketLikeHandler.CDPHttpHandler();
        registry.register(new ExactPathMatcher(PATH_PAGES), cdpHttpHandler);
        registry.register(new ExactPathMatcher(PATH_RUNTIME), cdpHttpHandler);
        registry.register(new ExactPathMatcher(PATH_SCREEN), cdpHttpHandler);
        registry.register(
                new RegexpPathMatcher(Pattern.compile("/sourcemap/.*")),
                new SourcemapHttpSocketLikeHandler.SourcemapHttpHandler());
        ReportInspectorInfo.getInstance().register(mContext, registry);

        return new LightHttpServer(registry);
    }

    @Override
    public void onAccepted(SocketLike socket) throws IOException {
        mServer.serve(socket);
    }
}

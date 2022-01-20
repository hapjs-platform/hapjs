/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.inspector;

import android.net.Uri;
import android.text.TextUtils;
import com.facebook.stetho.server.SocketLike;
import com.facebook.stetho.server.SocketLikeHandler;
import com.facebook.stetho.server.http.HandlerRegistry;
import com.facebook.stetho.server.http.HttpHandler;
import com.facebook.stetho.server.http.HttpStatus;
import com.facebook.stetho.server.http.LightHttpBody;
import com.facebook.stetho.server.http.LightHttpRequest;
import com.facebook.stetho.server.http.LightHttpResponse;
import com.facebook.stetho.server.http.LightHttpServer;
import com.facebook.stetho.server.http.RegexpPathMatcher;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;
import org.hapjs.runtime.inspect.InspectorManager;

public class SourcemapHttpSocketLikeHandler implements SocketLikeHandler {
    private static final String TAG = "SourcemapSocketHandler";
    private final LightHttpServer mServer;

    public SourcemapHttpSocketLikeHandler() {
        HandlerRegistry registry = new HandlerRegistry();
        registry.register(
                new RegexpPathMatcher(Pattern.compile("/sourcemap/.*")),
                new SourcemapHttpHandler());
        mServer = new LightHttpServer(registry);
    }

    private static String getPagePath(List<String> pathSegments) {

        int pathLength = pathSegments.size();
        if (pathLength > 1) {
            Uri.Builder builder = new Uri.Builder();
            for (int i = 1; i < pathLength; i++) {
                builder.appendEncodedPath(pathSegments.get(i));
            }
            return builder.toString();
        }
        return "/";
    }

    @Override
    public void onAccepted(SocketLike socket) throws IOException {
        mServer.serve(socket);
    }

    public static class SourcemapHttpHandler implements HttpHandler {
        private static final String RESPONSE_HEADER_ALLOW_ORIGIN = "Access-Control-Allow-Origin";

        @Override
        public boolean handleRequest(
                SocketLike socket, LightHttpRequest request, LightHttpResponse response)
                throws IOException {
            boolean isGetMethod = "GET".equals(request.method);

            if (isGetMethod) {
                String path = getPagePath(request.uri.getPathSegments());
                String packageName = V8Inspector.getInstance().getDebugPackage();

                if (TextUtils.isEmpty(packageName)) {
                    response.code = HttpStatus.HTTP_NOT_FOUND;
                    response.reasonPhrase = "Not found";
                    response.body = LightHttpBody.create("No map file found\n", "text/plain");
                    return true;
                }

                String content = InspectorManager.getInstance().getSourcemap(packageName, path);

                if (TextUtils.isEmpty(content)) {
                    response.code = HttpStatus.HTTP_NOT_FOUND;
                    response.reasonPhrase = "Not found";
                    response.body = LightHttpBody.create("No map file found\n", "text/plain");
                } else {
                    response.code = HttpStatus.HTTP_OK;
                    response.reasonPhrase = "OK";
                    response.addHeader(RESPONSE_HEADER_ALLOW_ORIGIN, "*");
                    response.body = LightHttpBody.create(content, "text/plain; charset=utf-8");
                }
            } else {
                response.code = HttpStatus.HTTP_NOT_IMPLEMENTED;
                response.reasonPhrase = "Not implemented";
                response.body =
                        LightHttpBody.create(request.method + " not implemented", "text/plain");
            }
            return true;
        }
    }
}

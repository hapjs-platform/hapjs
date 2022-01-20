/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.inspector;

import com.facebook.stetho.inspector.network.NetworkEventReporter;
import com.facebook.stetho.inspector.network.NetworkEventReporterImpl;
import com.facebook.stetho.inspector.network.SimpleBinaryInspectorWebSocketFrame;
import com.facebook.stetho.inspector.network.SimpleTextInspectorWebSocketFrame;
import javax.annotation.Nullable;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class StethoWebSocketFactory implements WebSocket.Factory {

    private OkHttpClient mClient;
    private NetworkEventReporter mReporter = NetworkEventReporterImpl.get();
    private StethoWebSocket mSocket;

    public StethoWebSocketFactory(OkHttpClient okHttpClient) {
        mClient = okHttpClient;
    }

    @Override
    public WebSocket newWebSocket(Request request, WebSocketListener listener) {
        String requestId = mReporter.nextRequestId();
        WebSocketListener wrappedListener = new StethoWebSocketListener(listener, requestId);
        WebSocket wrappedSocket = mClient.newWebSocket(request, wrappedListener);
        mSocket = new StethoWebSocket(wrappedSocket, requestId);
        return mSocket;
    }

    class StethoWebSocketListener extends WebSocketListener {

        private WebSocketListener wrappedListener;
        private String requestId;

        public StethoWebSocketListener(WebSocketListener wrappedListener, String requestId) {
            this.wrappedListener = wrappedListener;
            this.requestId = requestId;
        }

        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            wrappedListener.onOpen(mSocket, response); // must return the StethoSocket
            mReporter.webSocketCreated(requestId, webSocket.request().url().toString());
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            wrappedListener.onMessage(webSocket, text);
            mReporter
                    .webSocketFrameReceived(new SimpleTextInspectorWebSocketFrame(requestId, text));
        }

        @Override
        public void onMessage(WebSocket webSocket, ByteString bytes) {
            wrappedListener.onMessage(webSocket, bytes);
            mReporter.webSocketFrameReceived(
                    new SimpleBinaryInspectorWebSocketFrame(requestId, bytes.toByteArray()));
        }

        @Override
        public void onClosed(WebSocket webSocket, int code, String reason) {
            wrappedListener.onClosed(webSocket, code, reason);
            mReporter.webSocketClosed(requestId);
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, @Nullable Response response) {
            wrappedListener.onFailure(webSocket, t, response);
            mReporter.webSocketFrameError(requestId, t.getMessage());
        }
    }

    class StethoWebSocket implements WebSocket {
        private WebSocket wrappedSocket;
        private String requestId;

        public StethoWebSocket(WebSocket wrappedSocket, String requestId) {
            this.wrappedSocket = wrappedSocket;
            this.requestId = requestId;
        }

        @Override
        public Request request() {
            return wrappedSocket.request();
        }

        @Override
        public long queueSize() {
            return wrappedSocket.queueSize();
        }

        @Override
        public boolean send(String text) {
            mReporter.webSocketFrameSent(new SimpleTextInspectorWebSocketFrame(requestId, text));
            return wrappedSocket.send(text);
        }

        @Override
        public boolean send(ByteString bytes) {
            mReporter.webSocketFrameSent(
                    new SimpleBinaryInspectorWebSocketFrame(requestId, bytes.toByteArray()));
            return wrappedSocket.send(bytes);
        }

        @Override
        public boolean close(int code, @Nullable String reason) {
            return wrappedSocket.close(code, reason);
        }

        @Override
        public void cancel() {
            wrappedSocket.cancel();
        }
    }
}

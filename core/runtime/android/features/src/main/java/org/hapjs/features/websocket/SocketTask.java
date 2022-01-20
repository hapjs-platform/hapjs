/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.websocket;

import android.util.Log;
import com.eclipsesource.v8.utils.typedarrays.ArrayBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import okhttp3.Headers;
import okhttp3.WebSocketListener;
import okio.ByteString;
import org.hapjs.bridge.InstanceManager;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.common.net.HttpConfig;
import org.hapjs.common.net.NetworkReportManager;
import org.hapjs.render.jsruntime.serialize.JavaSerializeObject;
import org.hapjs.render.jsruntime.serialize.SerializeObject;
import org.hapjs.runtime.RuntimeActivity;
import org.hapjs.runtime.inspect.InspectorManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Okhttp websocket wrapper public test server: wss://echo.websocket.org ArrayBuffer test server:
 * ws://demos.kaazing.com/echo
 */
public class SocketTask implements InstanceManager.IInstance {
    public static final int CODE_CLOSE = 1000;
    public static final int CODE_RELEASE = 1001;
    public static final int CODE_ABNORMAL = 1006;
    private static final String TAG = "SocketTask";
    private okhttp3.WebSocket.Factory mWebSocketFactory;
    private okhttp3.Request mOkhttpRequest;
    private okhttp3.WebSocket mWebSocket;
    private Map<String, Request> mRequestMap = new HashMap<>();

    public SocketTask(String url, JSONObject jsonHeader, JSONArray protocols) {
        String debugEnabled = System.getProperty(RuntimeActivity.PROP_DEBUG, "false");
        mWebSocketFactory =
                Boolean.parseBoolean(debugEnabled)
                        ? InspectorManager.getInspector().getWebSocketFactory()
                        : HttpConfig.get().getOkHttpClient();
        createRequest(url, jsonHeader, protocols);
    }

    public void connectSocket() {
        if (mWebSocketFactory == null || mOkhttpRequest == null) {
            throw new IllegalStateException("websocket: init connect error");
        }
        mWebSocketFactory.newWebSocket(
                mOkhttpRequest,
                new WebSocketListener() {
                    @Override
                    public void onOpen(okhttp3.WebSocket webSocket, okhttp3.Response response) {
                        super.onOpen(webSocket, response);
                        mWebSocket = webSocket;
                        onSocketOpen();
                    }

                    @Override
                    public void onMessage(okhttp3.WebSocket webSocket, String text) {
                        super.onMessage(webSocket, text);
                        onSocketMessage(text);
                    }

                    @Override
                    public void onMessage(okhttp3.WebSocket webSocket, ByteString bytes) {
                        super.onMessage(webSocket, bytes);
                        onSocketMessage(bytes);
                    }

                    @Override
                    public void onClosed(okhttp3.WebSocket webSocket, int code, String reason) {
                        super.onClosed(webSocket, code, reason);
                        onSocketClose(code, reason, true);
                    }

                    @Override
                    public void onClosing(okhttp3.WebSocket webSocket, int code, String reason) {
                        super.onClosing(webSocket, code, reason);
                        webSocket.close(code, reason);
                    }

                    @Override
                    public void onFailure(
                            okhttp3.WebSocket webSocket, Throwable t, okhttp3.Response response) {
                        super.onFailure(webSocket, t, response);
                        String errorMsg;
                        if (response != null) {
                            errorMsg = response.message();
                        } else {
                            errorMsg = t.toString();
                            Log.w(TAG, errorMsg);
                        }
                        if (null == mWebSocket) {
                            // 如果在连接建立之前返回异常,H5会在调用onerror后会调用onclose
                            onSocketError(errorMsg);
                            // 由于这里无法获取code，设置默认code
                            onSocketClose(CODE_ABNORMAL, errorMsg, false);
                        } else {
                            // 服务器关闭，网络断开之类的情况调用onclose方法和H5行为一致
                            onSocketClose(CODE_ABNORMAL, errorMsg, false);
                        }
                    }
                });
    }

    public boolean send(String data) {
        return mWebSocket != null && mWebSocket.send(data);
    }

    public boolean send(ByteString data) {
        return mWebSocket != null && mWebSocket.send(data);
    }

    public boolean release(int code, String reason) {
        if (mWebSocket != null && mWebSocket.close(code, reason)) {
            releaseResource();
            mWebSocket = null;
            return true;
        }
        return false;
    }

    @Override
    public void release() {
        release(CODE_RELEASE, "client released");
    }

    // 注册事件
    public void register(org.hapjs.bridge.Request request) {
        String action = request.getAction();
        switch (action) {
            case WebSocket.EVENT_OPEN:
                // 可能websocket的open回调已经完成，立刻触发js回调
                if (mWebSocket != null) {
                    request.getCallback().callback(Response.SUCCESS);
                }
                mRequestMap.put(action, request);
                break;
            case WebSocket.EVENT_MESSAGE:
            case WebSocket.EVENT_CLOSE:
            case WebSocket.EVENT_ERROR:
                mRequestMap.put(action, request);
                break;
            default:
                break;
        }
    }

    private void onSocketOpen() {
        Request request = mRequestMap.get(WebSocket.EVENT_OPEN);
        if (request != null) {
            request.getCallback().callback(Response.SUCCESS);
        }
    }

    private void onSocketMessage(String data) {
        Request request = mRequestMap.get(WebSocket.EVENT_MESSAGE);
        if (request != null) {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put(WebSocket.RESULT_DATA, data);
                request.getCallback().callback(new Response(jsonObject));
            } catch (JSONException e) {
                Log.e(TAG, "onSocketMessage String error", e);
            }
        }
    }

    private void onSocketMessage(ByteString byteString) {
        Request request = mRequestMap.get(WebSocket.EVENT_MESSAGE);
        if (request != null) {
            byte[] bytes = byteString != null ? byteString.toByteArray() : new byte[0];
            SerializeObject serializeObject = new JavaSerializeObject();
            serializeObject.put(WebSocket.RESULT_DATA, new ArrayBuffer(bytes));
            request.getCallback().callback(new Response(serializeObject));
        }
    }

    private void onSocketClose(int code, String reason, boolean wasClean) {
        Request request = mRequestMap.get(WebSocket.EVENT_CLOSE);
        if (request != null) {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put(WebSocket.RESULT_REASON, reason);
                jsonObject.put(WebSocket.RESULT_CODE, code);
                jsonObject.put(WebSocket.RESULT_WAS_CLEAN, wasClean);
                request.getCallback().callback(new Response(jsonObject));
            } catch (JSONException e) {
                Log.e(TAG, "onSocketClose error", e);
            }
        }
        mRequestMap.clear();
    }

    private void onSocketError(String data) {
        Request request = mRequestMap.get(WebSocket.EVENT_ERROR);
        if (request != null) {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put(WebSocket.RESULT_DATA, data);
                request.getCallback().callback(new Response(jsonObject));
            } catch (JSONException e) {
                Log.e(TAG, "onSocketError error", e);
            }
        }
    }

    private void createRequest(String url, JSONObject jsonHeader, JSONArray protocols) {
        Headers headers = getHeaders(jsonHeader, protocols);
        mOkhttpRequest = new okhttp3.Request.Builder().url(url).headers(headers).build();
        NetworkReportManager.getInstance()
                .reportNetwork(getFeatureName(), url, NetworkReportManager.REPORT_LEVEL_SOCKET);
    }

    private Headers getHeaders(JSONObject jsonHeader, JSONArray protocols) {
        Headers.Builder builder = new Headers.Builder();
        addHeader(jsonHeader, builder);
        addProtocol(protocols, builder);
        return builder.build();
    }

    // 添加头
    private void addHeader(JSONObject jsonHeader, Headers.Builder builder) {
        if (jsonHeader == null || builder == null) {
            return;
        }
        Iterator<String> keys = jsonHeader.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object value = null;
            try {
                value = jsonHeader.get(key);
                if (value instanceof JSONArray) {
                    JSONArray values = (JSONArray) value;
                    for (int i = 0; i < values.length(); i++) {
                        builder.add(key, values.optString(i));
                    }
                } else {
                    builder.add(key, value.toString());
                }
            } catch (JSONException ignored) {
                Log.e(TAG, "add header error", ignored);
            }
        }
    }

    // 添加子协议
    private void addProtocol(JSONArray protocols, Headers.Builder builder) {
        if (protocols == null || protocols.length() == 0 || builder == null) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < protocols.length(); i++) {
            try {
                String protocol = protocols.getString(i);
                if (i > 0) {
                    sb.append(",");
                }
                sb.append(protocol);
            } catch (JSONException ignored) {
                Log.e(TAG, "add protocol error", ignored);
            }
        }
        builder.add("sec-websocket-protocol", sb.toString());
    }

    private void releaseResource() {
        mWebSocketFactory = null;
        mOkhttpRequest = null;
    }


    @Override
    public String getFeatureName() {
        return WebSocket.FEATURE_NAME;
    }
}

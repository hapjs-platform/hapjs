/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.websocket;

import static org.hapjs.bridge.Response.CODE_GENERIC_ERROR;
import static org.hapjs.bridge.Response.CODE_SERVICE_UNAVAILABLE;

import android.text.TextUtils;
import android.util.Log;
import com.eclipsesource.v8.V8ArrayBuffer;
import com.eclipsesource.v8.utils.ArrayBuffer;
import com.eclipsesource.v8.utils.TypedArray;
import java.nio.ByteBuffer;
import okio.ByteString;

import org.hapjs.bridge.FeatureExtension;
import org.hapjs.bridge.InstanceManager;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.FeatureExtensionAnnotation;
import org.hapjs.render.jsruntime.serialize.SerializeObject;
import org.hapjs.render.jsruntime.serialize.TypedArrayProxy;
import org.json.JSONObject;

@FeatureExtensionAnnotation(
        name = WebSocket.FEATURE_NAME,
        residentType = FeatureExtensionAnnotation.ResidentType.USEABLE,
        actions = {
                @ActionAnnotation(
                        name = WebSocket.ACTION_SEND,
                        mode = FeatureExtension.Mode.ASYNC,
                        normalize = FeatureExtension.Normalize.RAW),
                @ActionAnnotation(name = WebSocket.ACTION_CLOSE, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(
                        name = WebSocket.EVENT_OPEN,
                        mode = FeatureExtension.Mode.CALLBACK,
                        type = FeatureExtension.Type.EVENT,
                        alias = WebSocket.EVENT_OPEN_ALIAS),
                @ActionAnnotation(
                        name = WebSocket.EVENT_MESSAGE,
                        mode = FeatureExtension.Mode.CALLBACK,
                        type = FeatureExtension.Type.EVENT,
                        alias = WebSocket.EVENT_MESSAGE_ALIAS),
                @ActionAnnotation(
                        name = WebSocket.EVENT_ERROR,
                        mode = FeatureExtension.Mode.CALLBACK,
                        type = FeatureExtension.Type.EVENT,
                        alias = WebSocket.EVENT_ERROR_ALIAS),
                @ActionAnnotation(
                        name = WebSocket.EVENT_CLOSE,
                        mode = FeatureExtension.Mode.CALLBACK,
                        type = FeatureExtension.Type.EVENT,
                        alias = WebSocket.EVENT_CLOSE_ALIAS),
        })
public class WebSocket extends FeatureExtension {
    protected static final String FEATURE_NAME = "system.websocket";
    protected static final String ACTION_SEND = "send";
    protected static final String ACTION_CLOSE = "close";
    protected static final String EVENT_OPEN_ALIAS = "onopen";
    protected static final String EVENT_OPEN = "__onopen";
    protected static final String EVENT_MESSAGE_ALIAS = "onmessage";
    protected static final String EVENT_MESSAGE = "__onmessage";
    protected static final String EVENT_ERROR_ALIAS = "onerror";
    protected static final String EVENT_ERROR = "__onerror";
    protected static final String EVENT_CLOSE_ALIAS = "onclose";
    protected static final String EVENT_CLOSE = "__onclose";
    protected static final String PARAMS_KEY_DATA = "data";
    protected static final String PARAMS_KEY_CODE = "code";
    protected static final String PARAMS_KEY_REASON = "reason";
    protected static final String RESULT_DATA = "data";
    protected static final String RESULT_CODE = "code";
    protected static final String RESULT_WAS_CLEAN = "wasClean";
    protected static final String RESULT_REASON = "reason";
    private static final String TAG = "WebSocket";

    @Override
    public String getName() {
        return FEATURE_NAME;
    }

    @Override
    protected Response invokeInner(Request request) throws Exception {
        String action = request.getAction();
        if (ACTION_SEND.equals(action)) {
            send(request);
        } else if (ACTION_CLOSE.equals(action)) {
            close(request);
        } else if (EVENT_OPEN.equals(action)
                || EVENT_MESSAGE.equals(action)
                || EVENT_CLOSE.equals(action)
                || EVENT_ERROR.equals(action)) {
            handleEventRequest(request);
        } else {
            return Response.ERROR;
        }

        return Response.SUCCESS;
    }

    private void send(Request request) throws Exception {
        int instanceId = request.getInstanceId();

        SocketTask socketTask = (SocketTask) InstanceManager.getInstance().getInstance(instanceId);

        if (socketTask == null) {
            request.getCallback()
                    .callback(new Response(CODE_SERVICE_UNAVAILABLE, "no such ws instance"));
            return;
        }

        SerializeObject params = request.getSerializeParams();
        Object dataObj = params.opt(PARAMS_KEY_DATA);
        Log.d(TAG, "invoke send: instanceId = " + instanceId + ", dataObj = " + dataObj);

        boolean sendOk = false;
        if (dataObj instanceof SerializeObject) {
            String str = ((SerializeObject) dataObj).toJSONObject().toString();
            sendOk = !TextUtils.isEmpty(str) && socketTask.send(str);

        } else if (dataObj instanceof ByteBuffer) {
            ByteString data = getByteString((ByteBuffer) dataObj);
            sendOk = null != data && socketTask.send(data);
        } else if (dataObj instanceof TypedArrayProxy) {
            ByteBuffer buffer = ((TypedArrayProxy) dataObj).getBuffer();
            ByteString data = getByteString(buffer);
            sendOk = null != data && socketTask.send(data);
        } else if (dataObj instanceof byte[]) {
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(((byte[]) dataObj).length);
            byteBuffer.put((byte[]) dataObj);
            byteBuffer.rewind();
            ByteString data = getByteString(byteBuffer);
            sendOk = null != data && socketTask.send(data);
        } else if (null != dataObj) {
            String str = dataObj.toString();
            sendOk = !TextUtils.isEmpty(str) && socketTask.send(str);
        }

        if (sendOk) {
            request.getCallback().callback(Response.SUCCESS);
        } else {
            request.getCallback().callback(new Response(CODE_GENERIC_ERROR, "ws send failed"));
        }
    }

    private ByteString getByteString(ByteBuffer buffer) {
        ByteString data = null;
        if (null != buffer) {
            try {
                buffer.position(0);
                data = ByteString.of(buffer);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Fail to read ArrayBuffer: ", e);
            } catch (Exception e) {
                Log.e(TAG, "Fail to read ArrayBuffer: ", e);
            }
        }
        return data;
    }

    private void close(Request request) throws Exception {
        JSONObject jsonParams = request.getJSONParams();
        int code = jsonParams.optInt(PARAMS_KEY_CODE, SocketTask.CODE_CLOSE);
        String reason = jsonParams.optString(PARAMS_KEY_REASON);
        SocketTask socketTask =
                (SocketTask) InstanceManager.getInstance().getInstance(request.getInstanceId());
        if (socketTask != null && socketTask.release(code, reason)) {
            request.getCallback().callback(Response.SUCCESS);
            InstanceManager.getInstance().removeInstance(request.getInstanceId());
        } else {
            request.getCallback().callback(Response.ERROR);
        }
    }

    private void handleEventRequest(Request request) throws Exception {
        SocketTask socketTask =
                (SocketTask) InstanceManager.getInstance().getInstance(request.getInstanceId());
        if (socketTask != null) {
            socketTask.register(request);
        }
    }

    @Override
    public boolean isBuiltInExtension() {
        return true;
    }
}

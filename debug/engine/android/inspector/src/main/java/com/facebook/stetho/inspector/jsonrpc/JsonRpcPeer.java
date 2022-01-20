/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.inspector.jsonrpc;

import android.database.Observable;
import com.facebook.stetho.common.Util;
import com.facebook.stetho.inspector.jsonrpc.protocol.JsonRpcRequest;
import com.facebook.stetho.inspector.protocol.module.Page;
import com.facebook.stetho.json.ObjectMapper;
import com.facebook.stetho.websocket.SimpleSession;
import java.nio.channels.NotYetConnectedException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import org.hapjs.debug.log.DebuggerLogUtil;
import org.json.JSONObject;

@ThreadSafe
public class JsonRpcPeer {
    private final SimpleSession mPeer;
    private final ObjectMapper mObjectMapper;
    @GuardedBy("this")
    private final Map<Long, PendingRequest> mPendingRequests = new HashMap<>();
    private final DisconnectObservable mDisconnectObservable = new DisconnectObservable();
    @GuardedBy("this")
    private long mNextRequestId;

    public JsonRpcPeer(ObjectMapper objectMapper, SimpleSession peer) {
        mObjectMapper = objectMapper;
        mPeer = Util.throwIfNull(peer);
    }

    public SimpleSession getWebSocket() {
        return mPeer;
    }

    public void invokeMethod(
            String method, Object paramsObject, @Nullable PendingRequestCallback callback)
            throws NotYetConnectedException {
        Util.throwIfNull(method);

        Long requestId = (callback != null) ? preparePendingRequest(callback) : null;

        // magic, can basically convert anything for some amount of runtime overhead...
        JSONObject params = mObjectMapper.convertValue(paramsObject, JSONObject.class);

        JsonRpcRequest message = new JsonRpcRequest(requestId, method, params);
        String requestString;
        JSONObject jsonObject = mObjectMapper.convertValue(message, JSONObject.class);
        try {
            requestString = jsonObject.toString();
            mPeer.sendText(requestString);
        } catch (Error e) {
            if (Objects.equals(method, "Page.screencastFrame")) {
                Page.ScreencastFrameEvent event = (Page.ScreencastFrameEvent) paramsObject;
                // 一个空 String 所占空间为：
                // 对象头（8 字节）+ 引用 (4 字节 )  + char 数组（16 字节）+ 1个 int（4字节）+ 1个long（8字节）= 40 字节。
                int dataMemo = (event.data.length() * 2 + 40) / 1024;
                DebuggerLogUtil.logBreadcrumb(
                        "invokeMethod, method:" + method + ", dataMemo=" + dataMemo + "KB");
            } else {
                DebuggerLogUtil.logBreadcrumb("invokeMethod, method:" + method);
            }
            DebuggerLogUtil.logException(e);
        }
    }

    public void registerDisconnectReceiver(DisconnectReceiver callback) {
        mDisconnectObservable.registerObserver(callback);
    }

    public void unregisterDisconnectReceiver(DisconnectReceiver callback) {
        mDisconnectObservable.unregisterObserver(callback);
    }

    public void invokeDisconnectReceivers() {
        mDisconnectObservable.onDisconnect();
    }

    private synchronized long preparePendingRequest(PendingRequestCallback callback) {
        long requestId = mNextRequestId++;
        mPendingRequests.put(requestId, new PendingRequest(requestId, callback));
        return requestId;
    }

    public synchronized PendingRequest getAndRemovePendingRequest(long requestId) {
        return mPendingRequests.remove(requestId);
    }

    private static class DisconnectObservable extends Observable<DisconnectReceiver> {
        public void onDisconnect() {
            for (int i = 0, n = mObservers.size(); i < n; ++i) {
                final DisconnectReceiver observer = mObservers.get(i);
                observer.onDisconnect();
            }
        }
    }
}

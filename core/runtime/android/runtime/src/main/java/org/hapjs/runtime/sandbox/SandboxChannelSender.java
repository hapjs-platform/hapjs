/*
 * Copyright (C) 2023, hapjs.org. All rights reserved.
 */
package org.hapjs.runtime.sandbox;

import android.os.Handler;
import android.os.ParcelFileDescriptor;
import java.util.Map;
import org.hapjs.bridge.Response;
import org.hapjs.render.jsruntime.IJavaNative;

// The channel end in sandbox process sending requests to app process.
public class SandboxChannelSender extends ChannelSender implements IJavaNative {
    public SandboxChannelSender(ParcelFileDescriptor readSide, ParcelFileDescriptor writeSide, Handler handler) {
        super(readSide, writeSide, handler);
    }

    @Override
    public void callNative(int pageId, String argsString) {
        invokeAsync(SandboxIpcMethods.CALL_NATIVE, pageId, argsString);
    }

    @Override
    public int getViewId(int ref) {
        return invokeSync(SandboxIpcMethods.GET_VIEW_ID, int.class, ref);
    }

    @Override
    public String readDebugAsset(String path) {
        return invokeSync(SandboxIpcMethods.READ_DEBUG_ASSET, String.class, path);
    }

    @Override
    public void onKeyEventCallback(boolean consumed, int hash) {
        invokeAsync(SandboxIpcMethods.ON_KEY_EVENT_CALLBACK, consumed, hash);
    }

    @Override
    public Response invoke(String feature, String action, Object rawParams, String callback, int instanceId) {
        return invokeSync(SandboxIpcMethods.INVOKE_FEATURE, Response.class, feature, action, rawParams, callback, instanceId);
    }

    @Override
    public void routerBack() {
        invokeAsync(SandboxIpcMethods.ROUTER_BACK);
    }

    @Override
    public void routerClear() {
        invokeAsync(SandboxIpcMethods.ROUTER_CLEAR);
    }

    @Override
    public void routerReplace(String uri, Map<String, String> params) {
        invokeAsync(SandboxIpcMethods.ROUTER_REPLACE, uri, params);
    }

    @Override
    public void routerPush(String uri, Map<String, String> params) {
        invokeAsync(SandboxIpcMethods.ROUTER_PUSH, uri, params);
    }

    @Override
    public void inspectorResponse(int sessionId, int callId, String message) {
        invokeAsync(SandboxIpcMethods.INSPECTOR_RESPONSE, sessionId, callId, message);
    }

    @Override
    public void inspectorSendNotification(int sessionId, int callId, String message) {
        invokeAsync(SandboxIpcMethods.INSPECTOR_SEND_NOTIFICATION, sessionId, callId, message);
    }

    @Override
    public void inspectorRunMessageLoopOnPause(int contextGroupId) {
        invokeAsync(SandboxIpcMethods.INSPECTOR_RUN_MESSAGE_LOOP_ON_PAUSE, contextGroupId);
    }

    @Override
    public void inspectorQuitMessageLoopOnPause() {
        invokeAsync(SandboxIpcMethods.INSPECTOR_QUIT_MESSAGE_LOOP_ON_PAUSE);
    }

    @Override
    public boolean profilerIsEnabled() {
        return invokeSync(SandboxIpcMethods.PROFILER_IS_ENABLED, boolean.class);
    }

    @Override
    public void profilerRecord(String msg, long threadId) {
        invokeAsync(SandboxIpcMethods.PROFILER_RECORD, msg, threadId);
    }

    @Override
    public void profilerSaveProfilerData(String data) {
        invokeAsync(SandboxIpcMethods.PROFILER_SAVE_PROFILER_DATA, data);
    }

    @Override
    public void profilerTimeEnd(String msg) {
        invokeAsync(SandboxIpcMethods.PROFILER_TIME_END, msg);
    }

    @Override
    public void onV8Exception(StackTraceElement[] stack, String msg) {
        invokeAsync(SandboxIpcMethods.ON_V8_EXCEPTION, stack, msg);
    }

    @Override
    public void requestAnimationFrameNative() {
        invokeAsync(SandboxIpcMethods.REQUEST_ANIMATION_FRAME_NATIVE);
    }
}

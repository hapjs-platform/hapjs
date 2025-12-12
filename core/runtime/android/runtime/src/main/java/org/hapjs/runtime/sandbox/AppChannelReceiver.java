/*
 * Copyright (C) 2023, hapjs.org. All rights reserved.
 */
package org.hapjs.runtime.sandbox;

import android.os.ParcelFileDescriptor;
import java.util.Map;
import org.hapjs.bridge.Response;
import org.hapjs.render.jsruntime.IJavaNative;

// The channel end in app process receiving requests from sandbox process.
public class AppChannelReceiver extends ChannelReceiver {
    private static final String TAG = "AppChannelReceiver";

    protected IJavaNative mNative;

    public AppChannelReceiver(ParcelFileDescriptor readSide, ParcelFileDescriptor writeSide, IJavaNative javaNative) {
        super(readSide, writeSide, TAG);
        mNative = javaNative;
    }

    @Override
    protected String getQuickAppPkg() {
        return mNative.getQuickAppPkg();
    }

    @Override
    protected Object onInvoke(String method, Object[] args) {
        switch (method) {
            case SandboxIpcMethods.CALL_NATIVE:
                return callNative(args);
            case SandboxIpcMethods.GET_VIEW_ID:
                return getViewId(args);
            case SandboxIpcMethods.READ_DEBUG_ASSET:
                return readDebugAsset(args);
            case SandboxIpcMethods.ON_KEY_EVENT_CALLBACK:
                return onKeyEventCallback(args);
            case SandboxIpcMethods.INVOKE_FEATURE:
                return invokeFeature(args);
            case SandboxIpcMethods.ROUTER_BACK:
                return routerBack(args);
            case SandboxIpcMethods.ROUTER_PUSH:
                return routerPush(args);
            case SandboxIpcMethods.ROUTER_CLEAR:
                return routerClear(args);
            case SandboxIpcMethods.ROUTER_REPLACE:
                return routerReplace(args);
            case SandboxIpcMethods.INSPECTOR_RESPONSE:
                return inspectResponse(args);
            case SandboxIpcMethods.INSPECTOR_SEND_NOTIFICATION:
                return inspectorSendNotification(args);
            case SandboxIpcMethods.INSPECTOR_RUN_MESSAGE_LOOP_ON_PAUSE:
                return inspectorRunMessageLoopOnPause(args);
            case SandboxIpcMethods.INSPECTOR_QUIT_MESSAGE_LOOP_ON_PAUSE:
                return inspectorQuitMessageLoopOnPause(args);
            case SandboxIpcMethods.PROFILER_IS_ENABLED:
                return profilerIsEnabled(args);
            case SandboxIpcMethods.PROFILER_RECORD:
                return profilerRecord(args);
            case SandboxIpcMethods.PROFILER_SAVE_PROFILER_DATA:
                return profilerSaveProfilerData(args);
            case SandboxIpcMethods.PROFILER_TIME_END:
                return profilerTimeEnd(args);
            case SandboxIpcMethods.ON_V8_EXCEPTION:
                return onV8Exception(args);
            case SandboxIpcMethods.REQUEST_ANIMATION_FRAME_NATIVE:
                return requestAnimationFrameNative(args);
            default:
                throw new RuntimeException("unknown method: " + method);
        }
    }

    private Object callNative(Object[] args) {
        int pageId = (Integer) args[0];
        String renderCommands = (String) args[1];
        mNative.callNative(pageId, renderCommands);
        return null;
    }

    private Object getViewId(Object[] args) {
        int ref = (Integer) args[0];
        int viewId = mNative.getViewId(ref);
        return viewId;
    }

    private Object readDebugAsset(Object[] args) {
        String path = (String) args[0];
        String asset = mNative.readDebugAsset(path);
        return asset;
    }

    private Object onKeyEventCallback(Object[] args) {
        boolean consumed = (Boolean) args[0];
        int hash = (Integer) args[1];
        mNative.onKeyEventCallback(consumed, hash);
        return null;
    }

    private Object invokeFeature(Object[] args) {
        String feature = (String) args[0];
        String action = (String) args[1];
        Object rawParams = (Object) args[2];
        String callback = (String) args[3];
        int instanceId = (Integer) args[4];
        Response response = mNative.invoke(feature, action, rawParams, callback, instanceId);
        return response;
    }

    private Object routerBack(Object[] args) {
        mNative.routerBack();
        return null;
    }

    private Object routerPush(Object[] args) {
        String uri = (String) args[0];
        Map<String, String> params = (Map<String, String>) args[1];
        mNative.routerPush(uri, params);
        return null;
    }

    private Object routerReplace(Object[] args) {
        String uri = (String) args[0];
        Map<String, String> params = (Map<String, String>) args[1];
        mNative.routerReplace(uri, params);
        return null;
    }

    private Object routerClear(Object[] args) {
        mNative.routerClear();
        return null;
    }

    private Object inspectResponse(Object[] args) {
        int sessionId = (int) args[0];
        int callId = (int) args[1];
        String message = (String) args[2];
        mNative.inspectorResponse(sessionId, callId, message);
        return null;
    }

    private Object inspectorSendNotification(Object[] args) {
        int sessionId = (int) args[0];
        int callId = (int) args[1];
        String message = (String) args[2];
        mNative.inspectorSendNotification(sessionId, callId, message);
        return null;
    }

    private Object inspectorRunMessageLoopOnPause(Object[] args) {
        int contextGroupId = (int) args[0];
        mNative.inspectorRunMessageLoopOnPause(contextGroupId);
        return null;
    }

    private Object inspectorQuitMessageLoopOnPause(Object[] args) {
        mNative.inspectorQuitMessageLoopOnPause();
        return null;
    }

    private Object profilerIsEnabled(Object[] args) {
        return mNative.profilerIsEnabled();
    }

    private Object profilerRecord(Object[] args) {
        String msg = (String) args[0];
        long threadId = (long) args[1];
        mNative.profilerRecord(msg, threadId);
        return null;
    }

    private Object profilerSaveProfilerData(Object[] args) {
        String data = (String) args[0];
        mNative.profilerSaveProfilerData(data);
        return null;
    }

    private Object profilerTimeEnd(Object[] args) {
        String msg = (String) args[0];
        mNative.profilerTimeEnd(msg);
        return null;
    }

    private Object onV8Exception(Object[] args) {
        StackTraceElement[] stack = (StackTraceElement[]) args[0];
        String msg = (String) args[1];
        mNative.onV8Exception(stack, msg);
        return null;
    }

    private Object requestAnimationFrameNative(Object[] args) {
        mNative.requestAnimationFrameNative();
        return null;
    }
}

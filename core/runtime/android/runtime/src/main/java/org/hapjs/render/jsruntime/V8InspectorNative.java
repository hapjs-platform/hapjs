/*
 * Copyright (C) 2023, hapjs.org. All rights reserved.
 */
package org.hapjs.render.jsruntime;

import androidx.annotation.Keep;

import com.eclipsesource.v8.V8;

@Keep
public class V8InspectorNative {
    private static final String TAG = "V8InspectorNative";

    public interface InspectorNativeCallback {
        void inspectorResponse(int sessionId, int callId, String message);
        void inspectorSendNotification(int sessionId, int callId, String message);
        void inspectorRunMessageLoopOnPause(int contextGroupId);
        void inspectorQuitMessageLoopOnPause();
    }

    static {
        System.loadLibrary("inspector");
    }

    private InspectorNativeCallback mInspectorNativeCallback;

    public V8InspectorNative(InspectorNativeCallback inspectorNativeCallback) {
        mInspectorNativeCallback = inspectorNativeCallback;
    }

    //@callby native
    @Keep
    public void sendResponse(int sessionId, int callId, String message) {
        mInspectorNativeCallback.inspectorResponse(sessionId, callId, message);
    }

    //@callby native
    @Keep
    public void sendNotification(int sessionId, int callId, String message) {
        mInspectorNativeCallback.inspectorSendNotification(sessionId, callId, message);
    }

    //@callby native
    @Keep
    public void runMessageLoopOnPause(int contextGroupId) {
        mInspectorNativeCallback.inspectorRunMessageLoopOnPause(contextGroupId);
    }

    //@callby native
    @Keep
    public void quitMessageLoopOnPause() {
        mInspectorNativeCallback.inspectorQuitMessageLoopOnPause();
    }

    public native void nativeHandleMessage(long ptr, int sessionId, String message);
    public native long initNative(boolean autoEnable, int sessionId);
    public native void nativeSetV8Context(long ptr, V8 v8, int isJsContextReCreated);
    public native void nativeDisposeV8Context(long ptr);
    public native void nativeDestroy(long ptr);
    public native void nativeBeginLoadJsCode(String uri, String content);
    public native void nativeEndLoadJsCode(String uri);
    public native String nativeExecuteJsCode(long ptr, String jsCode);
    public native void nativeFrontendReload(long ptr);
}


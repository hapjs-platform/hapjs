/*
 * Copyright (C) 2023, hapjs.org. All rights reserved.
 */
package org.hapjs.render.jsruntime;

import java.util.Map;
import org.hapjs.bridge.Response;

// Object representing the native/java side
public interface IJavaNative {
    void setQuickAppPkg(String pkg);
    String getQuickAppPkg();

    void callNative(int pageId, String argsString);
    int getViewId(int ref);
    String readDebugAsset(String path);
    void onKeyEventCallback(boolean consumed, int hash);
    Response invoke(String feature, String action, Object rawParams, String callback, int instanceId);

    void routerBack();
    void routerClear();
    void routerReplace(String uri, Map<String, String> params);
    void routerPush(String uri, Map<String, String> params);

    void inspectorResponse(int sessionId, int callId, String message);
    void inspectorSendNotification(int sessionId, int callId, String message);
    void inspectorRunMessageLoopOnPause(int contextGroupId);
    void inspectorQuitMessageLoopOnPause();

    boolean profilerIsEnabled();
    void profilerRecord(String msg, long threadId);
    void profilerSaveProfilerData(String data);
    void profilerTimeEnd(String msg);

    void onV8Exception(StackTraceElement[] stack, String msg);

    void requestAnimationFrameNative();
}

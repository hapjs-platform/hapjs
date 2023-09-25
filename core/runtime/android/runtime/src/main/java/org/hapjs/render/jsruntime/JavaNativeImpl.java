/*
 * Copyright (C) 2023, hapjs.org. All rights reserved.
 */
package org.hapjs.render.jsruntime;

import android.os.Environment;
import android.util.Log;
import android.view.Choreographer;
import java.io.File;
import java.util.Map;
import org.hapjs.bridge.HybridRequest;
import org.hapjs.bridge.Response;
import org.hapjs.common.executors.Executors;
import org.hapjs.common.utils.RouterUtils;
import org.hapjs.common.utils.ViewIdUtils;
import org.hapjs.component.view.keyevent.KeyEventManager;
import org.hapjs.io.FileSource;
import org.hapjs.io.JavascriptReader;
import org.hapjs.render.PageNotFoundException;
import org.hapjs.render.RootView;
import org.hapjs.render.action.RenderActionManager;
import org.hapjs.runtime.inspect.InspectorManager;

public class JavaNativeImpl implements IJavaNative {
    private static final String TAG = "NativeImpl";

    private RenderActionManager mRenderActionManager;
    private JsEngineImpl.FrameCallback mFrameCallback;
    protected RootView mRootView;

    public JavaNativeImpl(RenderActionManager renderActionManager, JsEngineImpl.FrameCallback frameCallback) {
        mRenderActionManager = renderActionManager;
        mFrameCallback = frameCallback;
    }

    public void attachView(RootView rootView) {
        mRootView = rootView;
    }

    @Override
    public void setQuickAppPkg(String pkg) {
    }

    @Override
    public String getQuickAppPkg() {
        return mRootView == null ? null : mRootView.getPackage();
    }

    @Override
    public void callNative(int pageId, String argsString) {
        mRenderActionManager.callNative(pageId, argsString);
    }

    @Override
    public int getViewId(int ref) {
        return ViewIdUtils.getViewId(ref);
    }

    @Override
    public String readDebugAsset(String path) {
        String script = null;
        if (path.startsWith("/js")) {
            String newPath = path.replace("/js", "");
            File file = new File(Environment.getExternalStorageDirectory(), "quickapp/assets/js" + newPath);
            script = JavascriptReader.get().read(new FileSource(file));
            if (script != null) {
                Log.d(TAG, String.format("load %s from sdcard success", file.getAbsolutePath()));
            }
        }
        return script;
    }

    @Override
    public void onKeyEventCallback(boolean consumed, int hashcode) {
        KeyEventManager.getInstance().injectKeyEvent(consumed, mRootView, hashcode);
    }

    @Override
    public Response invoke(String feature, String action, Object rawParams, String callback, int instanceId) {
        return mRootView.getJsThread().getBridgeManager().invoke(feature, action, rawParams, callback, instanceId);
    }

    @Override
    public void routerBack() {
        RouterUtils.back(mRootView.getContext(), mRootView.getPageManager());
    }

    @Override
    public void routerClear() {
        mRootView.getPageManager().clear();
    }

    @Override
    public void routerReplace(String uri, Map<String, String> params) {
        HybridRequest request = new HybridRequest.Builder().pkg(mRootView.getPackage()).uri(uri).params(params).build();
        RouterUtils.replace(mRootView.getPageManager(), request);
    }

    @Override
    public void routerPush(String uri, Map<String, String> params) {
        HybridRequest request = new HybridRequest.Builder().pkg(mRootView.getPackage()).uri(uri).params(params).build();
        try {
            RouterUtils.push(mRootView.getPageManager(), request);
        } catch (PageNotFoundException ex) {
            mRootView.getJsThread().processV8Exception(ex);
        }
    }

    @Override
    public void inspectorResponse(int sessionId, int callId, String message) {
        InspectorManager.getInspector().inspectorResponse(sessionId, callId, message);
    }

    @Override
    public void inspectorSendNotification(int sessionId, int callId, String message) {
        InspectorManager.getInspector().inspectorSendNotification(sessionId, callId, message);
    }

    @Override
    public void inspectorRunMessageLoopOnPause(int contextGroupId) {
        InspectorManager.getInspector().inspectorRunMessageLoopOnPause(contextGroupId);
    }

    @Override
    public void inspectorQuitMessageLoopOnPause() {
        InspectorManager.getInspector().inspectorQuitMessageLoopOnPause();
    }

    @Override
    public boolean profilerIsEnabled() {
        return ProfilerHelper.profilerIsEnabled();
    }

    @Override
    public void profilerRecord(String msg, long threadId) {
        ProfilerHelper.profilerRecord(msg, threadId);
    }

    @Override
    public void profilerSaveProfilerData(String data) {
        ProfilerHelper.profilerSaveProfilerData(data);
    }

    @Override
    public void profilerTimeEnd(String msg) {
        ProfilerHelper.profilerTimeEnd(msg);
    }

    @Override
    public void onV8Exception(StackTraceElement[] stack, String msg) {
        Exception ex = new Exception("V8Exception: " + msg);
        ex.setStackTrace(stack);
        mRootView.getJsThread().processV8Exception(ex);
    }

    @Override
    public void requestAnimationFrameNative() {
        Executors.ui().execute(() -> {
            if (mFrameCallback != null) {
                Choreographer choreographer = Choreographer.getInstance();
                choreographer.postFrameCallback(frameTimeNanos -> mFrameCallback.onFrameCallback(frameTimeNanos));
            }
        });
    }
}

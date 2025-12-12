/*
 * Copyright (C) 2023, hapjs.org. All rights reserved.
 */

package org.hapjs.render.jsruntime;

import android.content.Context;
import com.eclipsesource.v8.JavaVoidCallback;
import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;
import org.hapjs.bridge.JsInterface;

public class JsBridgeRegisterHelper {
    private Context mContext;
    private IJavaNative mNative;
    private JsContext mJsContext;
    private String mPkg;
    private JsThread mJsThread;
    private long mThreadId;

    private V8 mV8;
    private JsBridge mJsBridge;
    private JsBridgeTimer mJsTimer;
    private JsBridgeHistory mJsBridgeHistory;
    private Profiler mProfiler;
    private V8Object mJsInterfaceProxy;

    public JsBridgeRegisterHelper(Context context, JsContext jsContext,
                                  JsThread jsThread, long threadId, IJavaNative javaNative) {
        mContext = context;
        mJsContext = jsContext;
        mV8 = jsContext.getV8();
        mJsThread = jsThread;
        mThreadId = threadId;
        mNative = javaNative;
        mJsBridge = new JsBridge(mContext, mNative);
    }

    public void setJavaNative(IJavaNative javaNative) {
        mNative = javaNative;
    }

    public void attach(String pkg) {
        mPkg = pkg;

        mJsBridge.attach(mPkg);
        mJsBridge.register(mV8);
    }

    public void registerBridge() {
        V8 v8 = mV8;
        v8.registerJavaMethod(keyEventCallback, "callKeyEvent");

        mJsTimer = new JsBridgeTimer(mJsContext, mJsThread.getHandler(), mNative);
        JsUtils.registerAllPublicMethodsToRoot(mJsTimer);

        mProfiler = new Profiler(v8, mThreadId, mNative, mJsThread);
        v8.add("profiler", mProfiler);
        mProfiler.registerJavaMethod(mProfiler.isEnabled, "isEnabled");
        mProfiler.registerJavaMethod(mProfiler.record, "record");
        mProfiler.registerJavaMethod(mProfiler.time, "time");
        mProfiler.registerJavaMethod(mProfiler.timeEnd, "timeEnd");
        mProfiler.registerJavaMethod(mProfiler.saveProfilerData, "saveProfilerData");

        mJsBridgeHistory = new JsBridgeHistory(mJsContext, mNative);
        v8.add("history", mJsBridgeHistory);
        mJsBridgeHistory.registerJavaMethod(mJsBridgeHistory.back, "back");
        mJsBridgeHistory.registerJavaMethod(mJsBridgeHistory.push, "push");
        mJsBridgeHistory.registerJavaMethod(mJsBridgeHistory.replace, "replace");
        mJsBridgeHistory.registerJavaMethod(mJsBridgeHistory.clear, "clear");

        JsInterface jsInterface = new JsInterface(mNative);
        mJsInterfaceProxy = JsInterfaceProxy.register(v8, jsInterface, JsInterface.INTERFACE_NAME);
    }

    public void unregister() {
        JsUtils.release(mJsTimer, mProfiler, mJsBridgeHistory, mJsInterfaceProxy);
        mJsTimer = null;
        mProfiler = null;
        mJsBridgeHistory = null;
        mJsInterfaceProxy = null;
    }

    private final JavaVoidCallback keyEventCallback = new JavaVoidCallback() {
        @Override
        public void invoke(V8Object v8Object, V8Array args) {
            try {
                boolean consumed = Boolean.parseBoolean(args.get(0).toString());
                int hashcode = Integer.parseInt(args.get(1).toString());
                mNative.onKeyEventCallback(consumed, hashcode);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                JsUtils.release(args);
            }
        }
    };

    public void destroyPage(int pageId) {
        mJsTimer.clearTimers(pageId);
    }

    public void onFrameCallback(long frameTimeNanos) {
        mJsTimer.onFrameCallback(frameTimeNanos);
    }
}

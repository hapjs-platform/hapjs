/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.inspector;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;
import com.eclipsesource.v8.V8Value;
import com.facebook.stetho.Stetho;
import com.facebook.stetho.common.UncheckedCallable;
import com.facebook.stetho.common.android.HandlerUtil;
import com.facebook.stetho.inspector.elements.android.ActivityTracker;
import com.facebook.stetho.inspector.jsonrpc.JsonRpcPeer;
import com.facebook.stetho.inspector.protocol.module.Console;
import com.facebook.stetho.json.ObjectMapper;
import com.facebook.stetho.json.annotation.JsonProperty;
import com.facebook.stetho.websocket.CloseCodes;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import okhttp3.Interceptor;
import okhttp3.WebSocket;
import org.hapjs.common.net.HttpConfig;
import org.hapjs.debug.log.DebuggerLogUtil;
import org.hapjs.inspector.reflect.Field;
import org.hapjs.inspector.reflect.Method;
import org.hapjs.render.Page;
import org.hapjs.render.RootView;
import org.hapjs.render.VDomChangeAction;
import org.hapjs.render.jsruntime.IJsEngine;
import org.hapjs.render.jsruntime.JsThread;
import org.hapjs.runtime.HapEngine;
import org.hapjs.runtime.ProviderManager;
import org.hapjs.runtime.inspect.InspectorManager;
import org.hapjs.runtime.inspect.InspectorProvider;
import org.json.JSONArray;
import org.json.JSONObject;

public class V8Inspector implements InspectorProvider {
    private static final String TAG = "V8Inspector";

    private static final String PARAM_KEY_DEBUG_SERVER = "DEBUG_SERVER";
    private static final String PARAM_KEY_DEBUG_PACKAGE = "DEBUG_PACKAGE";
    private static final String PARAM_KEY_DEBUG_USE_ADB = "DEBUG_USE_ADB";
    private static final String PARAM_KEY_DEBUG_SERIAL_NUMBER = "DEBUG_SERIAL_NUMBER";
    private static final String PARAM_KEY_DEBUG_TARGET = "DEBUG_TARGET"; // Possible value: skeleton
    private static final String INSPECTOR_HEAD = "inspect://";
    private static final int JsThread_MSG_SHUTDOWN = 12;
    private static final int JsThread_MSG_TERMINATE = 17;
    private static int mReloadCount = 0;
    private static V8Inspector instance;
    private static Method MessageQueue_next =
            new Method("android/os/MessageQueue", "next", "()Landroid/os/Message;");
    private static Field Message_target =
            new Field("android/os/Message", "target", "Landroid/os/Handler;");
    private static Method Message_recycleUnchecked =
            android.os.Build.VERSION.SDK_INT >= 21
                    ? new Method("android/os/Message", "recycleUnchecked", "()V")
                    : null;

    private final List<VDomChangeActionMessage> mCachedVDomActionMessages = new ArrayList<>();
    private Map<Integer, JsonRpcPeer> mPeerMaps = new ConcurrentHashMap<Integer, JsonRpcPeer>();
    private MessageHandler mHandler;
    private H mSendHandler;
    private HandlerThread mSendThread;
    private boolean mUseLocalSocket = false;
    private String mRemoteAddr = "0";
    private int mLastSessionId = 0;
    private ConsoleMessageCache mCachedMessages = new ConsoleMessageCache();
    private boolean mConsoleEnabled = false;
    private boolean mDomEnabled = false;
    private Looper mPausedLooper = null;
    private boolean mAutoEnable = false;
    private String mUrl;
    private String mDebugPackage;
    private boolean mUseADB = false;
    private String mDebugTarget = "";
    private String mSerialNumber = "";
    private WeakReference<View> mRootView;
    private HapEngine mHapEngine;
    private boolean mIsJsContextFirstCreated = true;
    private PageChangeMessage mCachedPageChangeMessage;
    private List<ProtocolMessage> mCachedProtocolMessages = new ArrayList<ProtocolMessage>();
    private List<DestroyNativeInfo> mDestroyNatives = new ArrayList<DestroyNativeInfo>();

    public V8Inspector() {
        mSendThread = new HandlerThread("v8Inspector_sender");
        mSendThread.start();
        mSendHandler = new H(mSendThread.getLooper());
    }

    public static V8Inspector getInstance() {
        return Holder.INSTANCE;
    }

    private static String toJSON(Object object, V8ObjectCheck checker) {
        if (object instanceof V8Array) {
            StringBuilder builder = new StringBuilder();
            V8Array arr = (V8Array) object;
            builder.append("[");
            int length = arr.length();
            for (int i = 0; i < length; i++) {
                if (i > 0) {
                    builder.append(",");
                }
                builder.append(toJSON(arr.get(i), checker));
            }
            builder.append("]");
            return builder.toString();

        } else if (object instanceof V8Object) {
            StringBuilder builder = new StringBuilder();
            V8Object v8obj = (V8Object) object;
            builder.append("{");
            String[] keys = v8obj.getKeys();
            int count = 0;
            for (int i = 0; i < keys.length; i++) {
                if (checker == null || checker.accept(keys[i])) {
                    if (count > 0) {
                        builder.append(",");
                    }
                    builder.append(keys[i]);
                    builder.append(":");
                    builder.append(toJSON(v8obj.get(keys[i]), null));
                    count++;
                }
            }

            builder.append("}");
            return builder.toString();
        } else if (object instanceof String) {
            return "\"" + (String) object + "\"";
        } else {
            return object.toString();
        }
    }

    private static void recycleMessage(Message msg) {
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            Message_recycleUnchecked.invokeVoid(msg);
        } else {
            msg.recycle();
        }
    }

    private static Console.MessageLevel getConsoleMessageLevel(int level) {
        switch (level) {
            case InspectorProvider.CONSOLE_WARN:
                return Console.MessageLevel.WARNING;
            case InspectorProvider.CONSOLE_DEBUG:
                return Console.MessageLevel.DEBUG;
            case InspectorProvider.CONSOLE_ERROR:
                return Console.MessageLevel.ERROR;
            default:
                return Console.MessageLevel.LOG;
        }
    }

    private static void sendConsoleMessage(JsonRpcPeer peer, String messageStr, int level) {
        if (TextUtils.isEmpty(messageStr)) {
            return;
        }
        Console.MessageAddedRequest messageAddedRequest = new Console.MessageAddedRequest();
        Console.ConsoleMessage message = new Console.ConsoleMessage();
        List<Console.Parameter> listParams = new ArrayList<>();
        message.source = Console.MessageSource.JAVASCRIPT;
        message.level = getConsoleMessageLevel(level);
        try {
            JSONArray paramsJson = new JSONArray(messageStr);
            for (int i = 0; i < paramsJson.length(); i++) {
                JSONObject param = paramsJson.getJSONObject(i);
                // "value" of Console.Parameter can be [int, string, object, boolean, ...].
                // However, the type has shown in Console.Parameter.type. So we need to coercion
                // the value as a string type.
                if (!param.optString("value").isEmpty()) {
                    param.put("value", param.optString("value"));
                }
                Console.Parameter consoleParameters =
                        new ObjectMapper().convertValue(param, Console.Parameter.class);
                listParams.add(consoleParameters);
            }
            message.parameters = listParams;
        } catch (org.json.JSONException e) {
            message.text = messageStr;
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return;
        }
        messageAddedRequest.message = message;
        peer.invokeMethod("Console.messageAdded", messageAddedRequest, null);
    }

    public HandlerThread getSendThread() {
        return mSendThread;
    }

    public void init(Context context, String server) {
        DebuggerLogUtil.logBreadcrumb("V8Inspector.init, server=" + server);
        mUrl = server;
        mIsJsContextFirstCreated = true;
        DebuggerLogUtil.logBreadcrumb("debug native app");
        registerInspector(context);
        processInspectRequest(server, context);
        Stetho.initializeWithDefaults(context);
        if (context instanceof Activity) {
            ActivityTracker.get().add((Activity) context);
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        context.registerReceiver(new LockScreenBroadcastReceiver(), intentFilter);
    }

    public void init(Context context, Map<String, Object> params) {
        mDebugPackage = (String) params.get(PARAM_KEY_DEBUG_PACKAGE);
        mHapEngine = HapEngine.getInstance(mDebugPackage);
        mSerialNumber = ((String) params.get(PARAM_KEY_DEBUG_SERIAL_NUMBER));
        mDebugTarget = ((String) params.get(PARAM_KEY_DEBUG_TARGET));
        Object useADB = params.get(PARAM_KEY_DEBUG_USE_ADB);
        if (useADB != null) {
            mUseADB = ((Boolean) useADB);
        }
        init(context, (String) params.get(PARAM_KEY_DEBUG_SERVER));
    }

    private void registerInspector(Context context) {
        ProviderManager.getDefault().addProvider(InspectorProvider.NAME, this);
        InspectorManager.update();
    }

    public boolean useLocalSocket() {
        return mUseLocalSocket;
    }

    public String getRemoteAddr() {
        return mRemoteAddr;
    }

    public String getUrl() {
        return mUrl;
    }

    public String getDebugPackage() {
        return mDebugPackage;
    }

    public HapEngine getHapEngine() {
        return mHapEngine;
    }

    public boolean isUseADB() {
        return mUseADB;
    }

    public String getDebugTatget() {
        return mDebugTarget;
    }

    public String getSerialNumber() {
        return mSerialNumber == null ? "" : mSerialNumber;
    }

    public RootView getRootView() {
        return mRootView == null ? null : (RootView) mRootView.get();
    }

    @Override
    public void setRootView(final View view) {
        mRootView = new WeakReference<>(view);
    }

    // process inspector url
    @Override
    public boolean processInspectRequest(String url, Context context) {
        ReportInspectorInfo rii = ReportInspectorInfo.getInstance();
        rii.registerReportURL(url);
        rii.register(context, null);
        return url.startsWith(INSPECTOR_HEAD);
    }

    public String executeJsCode(final String jsCode) {
        if (mHandler == null) {
            return null;
        }
        final MessageHandler h = mHandler;
        return HandlerUtil.postAndWait(
                h,
                new UncheckedCallable<String>() {
                    @Override
                    public String call() {
                        return h.mEngine.inspectorExecuteJsCode(h.mNativePtr, jsCode);
                    }
                });
    }

    @Override
    public boolean isInspectorReady() {
        return mReloadCount > 0;
    }

    private V8Array createV8Paramters(V8 v8, Object[] args) {
        final V8Array paramters = new V8Array(v8);
        for (Object arg : args) {
            if (arg instanceof Integer) {
                paramters.push(((Integer) arg).intValue());
            } else if (arg instanceof Float) {
                paramters.push(((Float) arg).floatValue());
            } else if (arg instanceof Double) {
                paramters.push(((Double) arg).doubleValue());
            } else if (arg instanceof Long) {
                paramters.push(((Long) arg).doubleValue());
            } else if (arg instanceof String) {
                paramters.push((String) arg);
            } else if (arg instanceof V8Value) {
                paramters.push((V8Value) arg);
            } else if (arg == null) {
                paramters.pushNull();
            } else {
                // TODO throw exception
                paramters.pushUndefined();
            }
        }
        return paramters;
    }

    public void handleMessage(JsonRpcPeer peer, String message) {
        int hashcode = peer.hashCode();
        mLastSessionId = hashcode;
        if (!mPeerMaps.containsKey(Integer.valueOf(hashcode))) {
            mPeerMaps.put(Integer.valueOf(hashcode), peer);
        }

        if (mHandler != null) {
            mHandler.obtainMessage(MessageHandler.HANDLE_MESSAGE, hashcode, 0, message)
                    .sendToTarget();
        } else {
            mCachedProtocolMessages.add(new ProtocolMessage(hashcode, message));
        }
    }


    @Override
    public void inspectorResponse(int sessionId, int callId, String message) {
        mSendHandler.obtainMessage(H.SEND_MESSAGE, sessionId, 0, message).sendToTarget();
    }

    @Override
    public void inspectorSendNotification(int sessionId, int callId, String message) {
        inspectorResponse(sessionId, callId, message);
    }

    @Override
    public void onPagePreChange(
            final int oldIndex, final int newIndex, final Page oldPage, final Page newPage) {
        if (VDocumentProvider.getCurrent() != null) {
            mSendHandler.post(
                    new Runnable() {
                        @Override
                        public void run() {
                            VDocumentProvider.getCurrent()
                                    .onPagePreChange(oldIndex, newIndex, oldPage, newPage);
                        }
                    });
        }
    }

    @Override
    public void onPageChanged(
            final int oldIndex, final int newIndex, final Page oldPage, final Page newPage) {
        if (VDocumentProvider.getCurrent() == null || !mDomEnabled) {
            mCachedPageChangeMessage = new PageChangeMessage(oldIndex, newIndex, oldPage, newPage);
            return;
        }
        mSendHandler.post(
                new Runnable() {
                    @Override
                    public void run() {
                        for (JsonRpcPeer peer : mPeerMaps.values()) {
                            peer.invokeMethod("Page.changed", null, null);
                        }
                        VDocumentProvider.getCurrent()
                                .onPageChanged(oldIndex, newIndex, oldPage, newPage);
                    }
                });
    }

    @Override
    public void onPageRemoved(final int index, final Page page) {
        if (VDocumentProvider.getCurrent() != null) {
            mSendHandler.post(
                    new Runnable() {
                        @Override
                        public void run() {
                            for (JsonRpcPeer peer : mPeerMaps.values()) {
                                peer.invokeMethod("Page.removed", null, null);
                            }
                            VDocumentProvider.getCurrent().onPageRemoved(index, page);
                        }
                    });
        }
    }

    @Override
    public void onAppliedChangeAction(
            final Context context, final JsThread jsThread, final VDomChangeAction action) {
        if (VDocumentProvider.getCurrent() == null || !mDomEnabled) {
            mCachedVDomActionMessages.add(new VDomChangeActionMessage(context, action));
            return;
        }

        mSendHandler.post(
                new Runnable() {
                    @Override
                    public void run() {
                        for (VDomChangeActionMessage message : mCachedVDomActionMessages) {
                            VDocumentProvider.getCurrent()
                                    .onAppliedChangeAction(message.mContext, jsThread,
                                            message.mAction);
                        }
                        mCachedVDomActionMessages.clear();

                        if (action != null) {
                            VDocumentProvider.getCurrent()
                                    .onAppliedChangeAction(context, jsThread, action);
                        }
                    }
                });
    }

    @Override
    public synchronized void onJsContextCreated(IJsEngine engine) {
        if (mHandler != null && mHandler.mEngine == engine) {
            return;
        }
        if (mHandler != null && mHandler.mNativePtr != 0 && mHandler.mEngine != null) {
            mDestroyNatives.add(new DestroyNativeInfo(mHandler.mEngine, mHandler.mNativePtr));
        }
        long nativePtr = engine.inspectorInit(mAutoEnable, mLastSessionId);
        if (mIsJsContextFirstCreated) {
            engine.inspectorSetV8Context(nativePtr, 0);
            mIsJsContextFirstCreated = false;
        } else {
            engine.inspectorSetV8Context(nativePtr, 1);
        }
        //start the thread handler
        mHandler = new MessageHandler(nativePtr, engine, Looper.myLooper());
        for (ProtocolMessage pm : mCachedProtocolMessages) {
            mHandler
                    .obtainMessage(MessageHandler.HANDLE_MESSAGE, pm.peerHasCode, 0, pm.message)
                    .sendToTarget();
        }
        mCachedProtocolMessages.clear();
        mCachedMessages.clear();
    }

    private void onPageVisibleChanged(boolean visible) {
        mSendHandler
                .obtainMessage(H.PAGE_VISIBILITY_CHANGED, mLastSessionId, -1, visible)
                .sendToTarget();
    }

    @Override
    public synchronized void onJsContextDispose(IJsEngine engine) {
        if (mHandler != null && mHandler.mEngine == engine) {
            engine.inspectorDisposeV8Context(mHandler.mNativePtr);
            engine.inspectorDestroy(mHandler.mNativePtr);
            mHandler.mNativePtr = 0;
            mHandler.mEngine = null;
            mHandler = null;
            mAutoEnable = true;
        } else {
            int i = 0;
            DestroyNativeInfo info = null;
            for (i = 0; i < mDestroyNatives.size(); i++) {
                info = mDestroyNatives.get(i);
                if (info.engine == engine) {
                    break;
                }
            }

            if (info == null || i >= mDestroyNatives.size()) {
                return;
            }
            mDestroyNatives.remove(i);
            if (info.nativePtr != 0) {
                engine.inspectorDisposeV8Context(info.nativePtr);
                engine.inspectorDestroy(info.nativePtr);
            }
        }
    }

    @Override
    public Interceptor getNetworkInterceptor() {
        return new com.facebook.stetho.okhttp3.StethoInterceptor();
    }

    @Override
    public WebSocket.Factory getWebSocketFactory() {
        return new StethoWebSocketFactory(HttpConfig.get().getOkHttpClient());
    }

    @Override
    public void onConsoleMessage(int level, String msg) {
        if (msg == null || msg.isEmpty()) {
            return;
        }
        mCachedMessages.append(level, msg);

        if (mConsoleEnabled) {
            mSendHandler.obtainMessage(H.CONSOLE_MESSAGEADDED, mLastSessionId, level, msg)
                    .sendToTarget();
        }
    }

    @Override
    public void onBeginLoadJsCode(String uri, String content) {
        mHandler.mEngine.inspectorBeginLoadJsCode(uri, content);
    }

    @Override
    public void onEndLoadJsCode(String uri) {
        mHandler.mEngine.inspectorEndLoadJsCode(uri);
    }

    public void domEnabled() {
        mDomEnabled = true;
        if (mCachedPageChangeMessage != null) {
            onPageChanged(
                    mCachedPageChangeMessage.mOldIndex,
                    mCachedPageChangeMessage.mNewIndex,
                    mCachedPageChangeMessage.mOldPage,
                    mCachedPageChangeMessage.mNewPage);
            mCachedPageChangeMessage = null;
        }
        onAppliedChangeAction(null, null, null);
    }

    public void consoleEnabled(JsonRpcPeer peer) {
        long it = mCachedMessages.begin();
        while (!mCachedMessages.isEnd(it)) {
            String messageStr = mCachedMessages.getMessage(it);
            int level = mCachedMessages.getLevel(it);
            sendConsoleMessage(peer, messageStr, level);
            it = mCachedMessages.next(it);
        }
        mConsoleEnabled = true;
    }

    public void consoleDisabled(JsonRpcPeer peer) {
        mConsoleEnabled = false;
    }

    /*
     * runMessageLoopOnPause函数被V8Inspector::runMessageLoopOnPause
     * 该函数是为了实现V8DebuggerClient对应的函数。
     * 该函数在v8虚拟机发生断点的时候被调用。当v8虚拟机发生断点时，v8虚拟机
     * 停止运行js脚本，然后调用runMessageLoopOnPause，等待上层传递给它命令（单步、继续等）
     * 然后再继续执行。
     * runMessageLoopOnPause函数必须处于阻塞状态，直到收到上层传递的命令后，才能退出，
     * 从而让v8虚拟机继续执行。
     *
     */
    //@Called by native
    public void inspectorRunMessageLoopOnPause(int contextGroupId) {
        mPausedLooper = Looper.myLooper();
        if (mPausedLooper == null) {
            return;
        }

        final MessageQueue queue = Looper.myQueue();
        for (; ; ) {
            Message msg = (Message) MessageQueue_next.invokeObject(queue);

            if (msg == null) {
                return;
            }

            if (msg.getCallback() instanceof ReloadPageCallback) {
                Handler handlerTarget = (Handler) Message_target.get(msg);
                if (handlerTarget != null) {
                    handlerTarget.post(msg.getCallback());
                    if (mReloadCount > 2) {
                        // 当调试进程启动时, 产生LoadJsRuntimeCallback, mReloadCount=1;
                        // 当第一次点击浏览器刷新时, 产生ReloadPageCallback, mReloadCount=2, 若刷新在
                        // LoadJsRuntime过程中,则不能terminate, 需要让JsRuntime加载完成;
                        // 故 mReloadCount > 2 方可执行terminate.
                        handlerTarget.dispatchMessage(createTerminateMessage());
                    }
                }
                break;
            }

            if (msg.getCallback() instanceof LoadJsRuntimeCallback) {
                Handler handlerTarget = (Handler) Message_target.get(msg);
                if (handlerTarget != null) {
                    handlerTarget.post(msg.getCallback());
                }
                break;
            }

            if (msg.what == MessageHandler.PAUSE_QUIT) {
                recycleMessage(msg);
                break;
            }

            Handler target = (Handler) Message_target.get(msg);
            if (target != null) {
                if (msg.what == JsThread_MSG_SHUTDOWN) {
                    // Terminating current javascript execution at once
                    // when received shutdown message
                    target.dispatchMessage(createTerminateMessage());
                    target.sendEmptyMessage(JsThread_MSG_SHUTDOWN);
                    return;
                }
                target.dispatchMessage(msg);
            }
            recycleMessage(msg);
        }
    }

    @Override
    public void inspectorQuitMessageLoopOnPause() {
        if (mPausedLooper != null) {
            mHandler.obtainMessage(MessageHandler.PAUSE_QUIT, 0, 0, null).sendToTarget();
        }
    }

    private void sendPageVisibilityChangedMessage(JsonRpcPeer peer, boolean visible) {
        com.facebook.stetho.inspector.protocol.module.Page.ScreencastVisibilityChangedEvent event =
                new com.facebook.stetho.inspector.protocol.module.Page.ScreencastVisibilityChangedEvent();
        event.visible = visible;
        peer.invokeMethod("Page.screencastVisibilityChanged", event, null);
    }

    final void destroy() {
        if (mHandler != null && mHandler.mNativePtr != 0) {
            mHandler.mEngine.inspectorDestroy(mHandler.mNativePtr);
            mHandler.mNativePtr = 0;
        }
    }

    private Message createTerminateMessage() {
        Message msg = new Message();
        msg.what = JsThread_MSG_TERMINATE;
        return msg;
    }

    public void reload() {
        RootView rootView = VDocumentProvider.getCurrent().getRootView();
        Runnable runnable;
        mReloadCount++;
        if (mReloadCount == 1) {
            runnable = new LoadJsRuntimeCallback();
        } else {
            runnable = new ReloadPageCallback();
        }
        // INSPECTOR MOD
        if (rootView == null) {
            Log.e(TAG, "Debug reloaded error caused by jsthread is null");
            return;
        }
        JsThread jsthread = rootView.getJsThread();
        if (jsthread == null) {
            Log.e(TAG, "Debug reloaded error caused by jsthread is null");
            return;
        }
        Handler handler = jsthread.getHandler();
        if (handler == null) {
            Log.e(TAG, "Debug reloaded error cuased by handler is null");
            return;
        }
        // END
        handler.post(runnable);
    }

    public void stop(Boolean platform) {
        Stetho.stop();
        if (platform) {
            for (JsonRpcPeer peer : mPeerMaps.values()) {
                peer.getWebSocket().close(CloseCodes.NORMAL_CLOSURE, "ServerSocket closed");
            }
        }
    }

    private native void nativeHandleMessage(long ptr, int sessionId, String message);

    private native long initNative(boolean autoEnable, int sessionId);

    private native void nativeSetV8Context(long ptr, V8 v8, int isJsContextReCreated);

    private native void nativeDisposeV8Context(long ptr);

    private native void nativeDestroy(long ptr);

    private native void nativeBeginLoadJsCode(String uri, String content); // TODO: delete it

    private native void nativeEndLoadJsCode(String uri); // TODO: delete it

    private native String nativeExecuteJsCode(long ptr, String jsCode);

    private native String nativeHandleConsoleMessage(long ptr, V8Value v8Array);

    private native void nativeFrontendReload(long ptr);

    public static interface V8ObjectCheck {
        public boolean accept(String key);
    }

    private static class VDomChangeActionMessage {
        Context mContext;
        VDomChangeAction mAction;

        VDomChangeActionMessage(Context context, VDomChangeAction action) {
            mContext = context;
            mAction = action;
        }
    }

    private static class ProtocolMessage {
        public int peerHasCode;
        public String message;

        public ProtocolMessage(int hashcode, String msg) {
            peerHasCode = hashcode;
            message = msg;
        }
    }

    private static class PageChangeMessage {
        int mOldIndex;
        int mNewIndex;
        Page mOldPage;
        Page mNewPage;

        PageChangeMessage(int oldIndex, int newIndex, Page oldPage, Page newPage) {
            mOldIndex = oldIndex;
            mNewIndex = newIndex;
            mOldPage = oldPage;
            mNewPage = newPage;
        }
    }

    private static class DestroyNativeInfo {
        IJsEngine engine;
        long nativePtr;

        DestroyNativeInfo(IJsEngine engine, long nptr) {
            this.engine = engine;
            this.nativePtr = nptr;
        }
    }

    private static class Holder {
        private static final V8Inspector INSTANCE = new V8Inspector();
    }

    private static class PageReloadParams {
        @JsonProperty(required = true)
        public boolean ignoreCache;

        @JsonProperty
        public String scriptToEvaluateOnLoad;
    }

    private class MessageHandler extends Handler {

        static final int HANDLE_MESSAGE = 1;
        static final int PAUSE_QUIT = -1;
        long mNativePtr;
        IJsEngine mEngine;

        MessageHandler (long nativeptr, IJsEngine engine, Looper looper) {
            super(looper);
            this.mEngine = engine;
            this.mNativePtr = nativeptr;
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == HANDLE_MESSAGE) {
                mEngine.inspectorHandleMessage(mNativePtr, msg.arg1, (String) (msg.obj));
            }
        }
    }

    private class H extends Handler {
        static final int SEND_MESSAGE = 2;
        static final int CONSOLE_MESSAGEADDED = 3;
        static final int PAGE_VISIBILITY_CHANGED = 4;

        H(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == SEND_MESSAGE) {
                JsonRpcPeer peer = mPeerMaps.get(msg.arg1);
                if (peer != null) {
                    peer.getWebSocket().sendText((String) msg.obj);
                }
            } else if (msg.what == CONSOLE_MESSAGEADDED) {
                JsonRpcPeer peer = mPeerMaps.get(msg.arg1);
                if (peer != null) {
                    int level = msg.arg2;
                    String messageStr = (String) msg.obj;
                    sendConsoleMessage(peer, messageStr, level);
                }
            } else if (msg.what == PAGE_VISIBILITY_CHANGED) {
                JsonRpcPeer peer = mPeerMaps.get(msg.arg1);
                if (peer != null) {
                    boolean visible = ((Boolean) msg.obj);
                    sendPageVisibilityChangedMessage(peer, visible);
                }
            }
        }
    }

    private class ReloadPageCallback implements Runnable {
        @Override
        public void run() {
            try {
                RootView rootView = VDocumentProvider.getCurrent().getRootView();
                // MOD INSPECTOR
                if (rootView == null) {
                    Log.e(TAG, "ReloadPageCallback run: rootView is null");
                    return;
                }
                // END
                MessageHandler h = mHandler;
                rootView.reloadCurrentPage();
                h.mEngine.inspectorFrontendReload(h.mNativePtr);
            } catch (NoSuchMethodError e) {
                Log.e(TAG, "Reload current page error", e);
            }
        }
    }

    private class LoadJsRuntimeCallback implements Runnable {
        @Override
        public void run() {
            try {
                RootView rootView = VDocumentProvider.getCurrent().getRootView();
                // MOD INSPECTOR
                if (rootView == null) {
                    Log.e(TAG, "LoadJsRuntimeCallback run: rootView is null");
                    return;
                }
                // END
                rootView.startJsApp();
            } catch (NoSuchMethodError e) {
                Log.e(TAG, "Start JsRuntime error", e);
            }
        }
    }

    private class LockScreenBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_SCREEN_ON.equals(action)) {
                onPageVisibleChanged(true);
            } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                onPageVisibleChanged(false);
            }
        }
    }
}

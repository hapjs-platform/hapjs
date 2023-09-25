/*
 * Copyright (C) 2023, hapjs.org. All rights reserved.
 */

package org.hapjs.render.jsruntime;

import android.content.Context;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import com.eclipsesource.v8.V8;
import java.util.Map;
import org.hapjs.logging.RuntimeLogManager;
import org.hapjs.runtime.ProviderManager;
import org.hapjs.runtime.sandbox.SyncWaiter;

public class SandboxJsThread extends JsThread {
    private static final String TAG = "SandboxJsThread";

    private Context mContext;

    private ParcelFileDescriptor[] mPositiveDescriptors;
    private ParcelFileDescriptor[] mPassiveDescriptors;

    private JsContext mJsContext;
    private JsBridgeRegisterHelper mJsBridgeRegisterHelper;

    public class H extends JsThread.H {
        static final private int MSG_CODE_BASE = 100;
        static final private int MSG_REGISTER_COMPONENTS = MSG_CODE_BASE + 1;
        static final private int MSG_KEY_PRESS_PAGE = MSG_CODE_BASE + 2;
        static final private int MSG_MENU_BUTTON_PRESS_PAGE = MSG_CODE_BASE + 3;
        static final private int INSPECTOR_HANDLE_MESSAGE = MSG_CODE_BASE + 4;
        static final private int INSPECTOR_INIT = MSG_CODE_BASE + 5;
        static final private int INSPECTOR_SET_V8_CONTEXT = MSG_CODE_BASE + 6;
        static final private int INSPECTOR_DISPOSE_V8_CONTEXT = MSG_CODE_BASE + 7;
        static final private int INSPECTOR_DESTROY = MSG_CODE_BASE + 8;
        static final private int INSPECTOR_BEGIN_LOAD_JS_CODE = MSG_CODE_BASE + 9;
        static final private int INSPECTOR_END_LOAD_JS_CODE = MSG_CODE_BASE + 10;
        static final private int INSPECTOR_EXECUTE_JS_CODE = MSG_CODE_BASE + 11;
        static final private int INSPECTOR_FRONTEND_RELOAD = MSG_CODE_BASE + 12;
        static final private int MSG_EXECUTE_OBJECT_SCRIPT_AND_STRINGIFY = MSG_CODE_BASE + 13;
        static final private int ON_FRAME_CALLBACK = MSG_CODE_BASE + 14;

        H(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REGISTER_COMPONENTS: {
                    registerComponents(msg.obj);
                    break;
                }
                case MSG_KEY_PRESS_PAGE: {
                    keyPressPage(msg.obj);
                    break;
                }
                case MSG_MENU_BUTTON_PRESS_PAGE: {
                    menuButtonPressPage(msg.obj);
                    break;
                }
                case INSPECTOR_HANDLE_MESSAGE: {
                    inspectorHandleMessage(msg.obj);
                    break;
                }
                case INSPECTOR_INIT: {
                    inspectorInit(msg.obj);
                    break;
                }
                case INSPECTOR_SET_V8_CONTEXT: {
                    inspectorSetV8Context(msg.obj);
                    break;
                }
                case INSPECTOR_DISPOSE_V8_CONTEXT: {
                    inspectorDisposeV8Context(msg.obj);
                    break;
                }
                case INSPECTOR_DESTROY: {
                    inspectorDestroy(msg.obj);
                    break;
                }
                case INSPECTOR_BEGIN_LOAD_JS_CODE: {
                    inspectorBeginLoadJsCode(msg.obj);
                    break;
                }
                case INSPECTOR_END_LOAD_JS_CODE: {
                    inspectorEndLoadJsCode(msg.obj);
                    break;
                }
                case INSPECTOR_EXECUTE_JS_CODE: {
                    inspectorExecuteJsCode(msg.obj);
                    break;
                }
                case INSPECTOR_FRONTEND_RELOAD: {
                    inspectorFrontendReload(msg.obj);
                    break;
                }
                case MSG_EXECUTE_OBJECT_SCRIPT_AND_STRINGIFY: {
                    executeObjectScriptAndStringify(msg.obj);
                    break;
                }
                case ON_FRAME_CALLBACK: {
                    onFrameCallback(msg.obj);
                    break;
                }
                default: {
                    super.handleMessage(msg);
                    break;
                }
            }
        }
    }

    public SandboxJsThread(Context context, ParcelFileDescriptor[] positiveDescriptors, ParcelFileDescriptor[] passiveDescriptors) {
        super("SandboxJsThread");
        mContext = context;
        mPositiveDescriptors = positiveDescriptors;
        mPassiveDescriptors = passiveDescriptors;

        Message.obtain(mHandler, JsThread.H.MSG_INIT).sendToTarget();
    }

    @Override
    protected H createHandler() {
        return new H(getLooper());
    }

    public V8 getV8() {
        return mJsContext.getV8();
    }

    @Override
    protected void onInit() {
        RuntimeLogManager.getDefault().logJsThreadTaskStart(mContext.getPackageName(), RuntimeLogManager.KEY_JS_ENV_INIT);
        doInit();
        RuntimeLogManager.getDefault().logJsThreadTaskEnd(mContext.getPackageName(), RuntimeLogManager.KEY_JS_ENV_INIT);
    }

    private void doInit() {
        mJsContext = new JsContext(this);
        SandboxProvider provider = ProviderManager.getDefault().getProvider(SandboxProvider.NAME);
        provider.createSandboxChannelReceiver(mPassiveDescriptors[0], mPassiveDescriptors[1], this);
        mNative = provider.createSandboxChannelSender(mPositiveDescriptors[0], mPositiveDescriptors[1], mHandler);
        mJsBridgeRegisterHelper = new JsBridgeRegisterHelper(mContext, mJsContext, this, getId(), mNative);
        mJsBridgeRegisterHelper.registerBridge();

        mEngine = provider.createEngineImpl(mJsContext, new InspectorNativeCallback(),
                e -> mNative.onV8Exception(e.getStackTrace(), e.getMessage()),
                frameTimeNanos -> mJsBridgeRegisterHelper.onFrameCallback(frameTimeNanos));
    }

    public void postOnAttach(String environmentScript, String pkg) {
        Object[] params = new Object[] {environmentScript, pkg};
        Message.obtain(mHandler, H.MSG_ATTACH, params).sendToTarget();
    }

    @Override
    protected void onAttach(Object msgObj) {
        Object[] params = (Object[]) msgObj;
        String environmentScript = (String) params[0];
        String pkg = (String) params[1];
        mNative.setQuickAppPkg(pkg);
        mEngine.setQuickAppPkg(pkg);
        mEngine.onAttach(environmentScript, pkg);
        mJsBridgeRegisterHelper.attach(pkg);
    }

    public void postCreateApplication(int appId, String js, String css, String metaInfo) {
        Object[] params = new Object[]{appId, js, css, metaInfo};
        mHandler.obtainMessage(H.MSG_CREATE_APPLICATION, params).sendToTarget();
    }

    public void postOnRequestApplication(int appId) {
        mHandler.obtainMessage(H.MSG_ON_REQUEST_APPLICATION, new Object[] {appId}).sendToTarget();
    }

    public void postOnShowApplication(int appId) {
        mHandler.obtainMessage(H.MSG_ON_SHOW_APPLICATION, new Object[] {appId}).sendToTarget();
    }

    public void postOnHideApplication(int appId) {
        mHandler.obtainMessage(H.MSG_ON_HIDE_APPLICATION, new Object[] {appId}).sendToTarget();
    }

    public boolean postBackPressPage(int pageId) {
        SyncWaiter<Boolean> waiter = new SyncWaiter<>(false);
        mHandler.obtainMessage(H.MSG_BACK_PRESS, new Object[] {pageId, waiter}).sendToTarget();
        return waiter.waitAndGetResult();
    }

    @Override
    protected boolean backPress(Object msgObj) {
        Object[] params = (Object[]) msgObj;
        SyncWaiter waiter = (SyncWaiter) params[1];
        boolean consumed = super.backPress(msgObj);
        waiter.setResult(consumed);
        return consumed;
    }

    public boolean postMenuButtonPressPage(int pageId) {
        SyncWaiter<Boolean> waiter = new SyncWaiter<>(false);
        mHandler.obtainMessage(H.MSG_MENU_BUTTON_PRESS_PAGE, new Object[] {pageId, waiter}).sendToTarget();
        return waiter.waitAndGetResult();
    }

    @Override
    protected boolean menuButtonPressPage(Object msgObj) {
        Object[] params = (Object[]) msgObj;
        SyncWaiter waiter = (SyncWaiter) params[1];
        boolean consumed = super.menuButtonPressPage(msgObj);
        waiter.setResult(consumed);
        return consumed;
    }

    public boolean postKeyPressPage(int pageId, Map<String, Object> params) {
        SyncWaiter<Boolean> waiter = new SyncWaiter<>(false);
        Object[] obj = new Object[]{pageId, params, waiter};
        mHandler.obtainMessage(H.MSG_KEY_PRESS_PAGE, obj).sendToTarget();
        return waiter.waitAndGetResult();
    }

    private void keyPressPage(Object msgObj) {
        int pageId = (int) ((Object[]) msgObj)[0];
        Map<String, Object> params = (Map<String, Object>) ((Object[]) msgObj)[1];
        SyncWaiter waiter = (SyncWaiter) ((Object[]) msgObj)[2];
        waiter.setResult(mEngine.keyPressPage(pageId, params));
    }

    public boolean postMenuPressPage(int pageId) {
        SyncWaiter<Boolean> waiter = new SyncWaiter<>(false);
        mHandler.obtainMessage(H.MSG_MENU_PRESS, new Object[] {pageId, waiter}).sendToTarget();
        return waiter.waitAndGetResult();
    }

    @Override
    protected boolean onMenuPress(Object msgObj) {
        Object[] params = (Object[]) msgObj;
        int pageId = (int) params[0];
        SyncWaiter waiter = (SyncWaiter) params[1];
        boolean consumed = mEngine.menuPressPage(pageId);
        waiter.setResult(consumed);
        return consumed;
    }

    public void postCreatePage(int appId, int pageId, String js, String css, Map<String, ?> params,
                                Map<String, ?> intent, Map<String, ?> meta) {
        Object[] obj = new Object[]{appId, pageId, js, css, params, intent, meta};
        Message.obtain(mHandler, H.MSG_CREATE_PAGE, obj).sendToTarget();
    }

    public void postDestroyPage(int pageId) {
        Message.obtain(mHandler, H.MSG_DESTROY_PAGE, new Object[] {pageId}).sendToTarget();
    }

    @Override
    protected void destroyPage(Object msgObj) {
        super.destroyPage(msgObj);
        int pageId = (int) ((Object[]) msgObj)[0];
        mJsBridgeRegisterHelper.destroyPage(pageId);
    }

    public void postDestroyApplication(int appId) {
        Message.obtain(mHandler, H.MSG_DESTROY_APPLICATION, new Object[] {appId}).sendToTarget();
    }

    public void postRegisterComponents(String components) {
        Message.obtain(mHandler, H.MSG_REGISTER_COMPONENTS, new Object[] {components}).sendToTarget();
    }

    private void registerComponents(Object msgObj) {
        String components = (String) ((Object[]) msgObj)[0];
        mEngine.registerComponents(components);
    }

    public void postTerminateExecution() {
        Message.obtain(mHandler, H.MSG_TERMINATE_EXECUTION).sendToTarget();
    }

    public void postInspectorHandleMessage(Object[] args) {
        Message.obtain(mHandler, H.INSPECTOR_HANDLE_MESSAGE, args).sendToTarget();
    }

    private void inspectorHandleMessage(Object msgObj) {
        Object[] params = (Object[]) msgObj;
        long ptr = (long) params[0];
        int sessionId = (int) params[1];
        String message = (String) params[2];
        mEngine.inspectorHandleMessage(ptr, sessionId, message);
    }

    public long postInspectorInit(Object[] args) {
        SyncWaiter<Long> waiter = new SyncWaiter<Long>(0l);
        Object[] obj = new Object[]{args, waiter};
        Message.obtain(mHandler, H.INSPECTOR_INIT, obj).sendToTarget();
        return waiter.waitAndGetResult();
    }

    private void inspectorInit(Object msgObj) {
        Object[] params = (Object[]) msgObj;
        Object[] args = (Object[]) params[0];
        boolean autoEnable = (boolean) args[0];
        int sessionId = (int) args[1];
        SyncWaiter<Long> waiter = (SyncWaiter<Long>) params[1];
        waiter.setResult(mEngine.inspectorInit(autoEnable, sessionId));
    }

    public void postInspectorSetV8Context(Object[] args) {
        Message.obtain(mHandler, H.INSPECTOR_SET_V8_CONTEXT, args).sendToTarget();
    }

    private void inspectorSetV8Context(Object msgObj) {
        Object[] params = (Object[]) msgObj;
        long ptr = (long) params[0];
        int isJsContextReCreated = (int) params[1];
        mEngine.inspectorSetV8Context(ptr, isJsContextReCreated);
    }

    public void postInspectorDisposeV8Context(Object[] args) {
        Message.obtain(mHandler, H.INSPECTOR_DISPOSE_V8_CONTEXT, args).sendToTarget();
    }

    private void inspectorDisposeV8Context(Object msgObj) {
        Object[] params = (Object[]) msgObj;
        long ptr = (long) params[0];
        mEngine.inspectorDisposeV8Context(ptr);
    }

    public void postInspectorDestroy(Object[] args) {
        Message.obtain(mHandler, H.INSPECTOR_DESTROY, args).sendToTarget();
    }

    private void inspectorDestroy(Object msgObj) {
        Object[] params = (Object[]) msgObj;
        long ptr = (long) params[0];
        mEngine.inspectorDestroy(ptr);
    }

    public void postInspectorBeginLoadJsCode(Object[] args) {
        Message.obtain(mHandler, H.INSPECTOR_BEGIN_LOAD_JS_CODE, args).sendToTarget();
    }

    private void inspectorBeginLoadJsCode(Object msgObj) {
        Object[] params = (Object[]) msgObj;
        String uri = (String) params[0];
        String content = (String) params[1];
        mEngine.inspectorBeginLoadJsCode(uri, content);
    }

    public void postInspectorEndLoadJsCode(Object[] args) {
        Message.obtain(mHandler, H.INSPECTOR_END_LOAD_JS_CODE, args).sendToTarget();
    }

    private void inspectorEndLoadJsCode(Object msgObj) {
        Object[] params = (Object[]) msgObj;
        String uri = (String) params[0];
        mEngine.inspectorEndLoadJsCode(uri);
    }

    public String postInspectorExecuteJsCode(Object[] args) {
        SyncWaiter<String> waiter = new SyncWaiter<>("");
        Object[] obj = new Object[]{args, waiter};
        Message.obtain(mHandler, H.INSPECTOR_EXECUTE_JS_CODE, obj).sendToTarget();
        return waiter.waitAndGetResult();
    }

    private void inspectorExecuteJsCode(Object msgObj) {
        Object[] params = (Object[]) msgObj;
        Object[] args = (Object[]) params[0];
        long ptr = (long) args[0];
        String jsCode = (String) args[1];
        SyncWaiter<String> waiter = (SyncWaiter<String>) params[1];
        waiter.setResult(mEngine.inspectorExecuteJsCode(ptr, jsCode));
    }

    public void postInspectorFrontendReload(Object[] args) {
        Message.obtain(mHandler, H.INSPECTOR_FRONTEND_RELOAD, args).sendToTarget();
    }

    private void inspectorFrontendReload(Object msgObj) {
        Object[] params = (Object[]) msgObj;
        long ptr = (long) params[0];
        mEngine.inspectorFrontendReload(ptr);
    }

    public String postExecuteObjectScriptAndStringify(String script) {
        SyncWaiter<String> waiter = new SyncWaiter<>("");
        Object[] obj = new Object[]{script, waiter};
        Message.obtain(mHandler, H.MSG_EXECUTE_OBJECT_SCRIPT_AND_STRINGIFY, obj).sendToTarget();
        return waiter.waitAndGetResult();
    }

    protected void executeObjectScriptAndStringify(Object msgObj) {
        Object[] params = (Object[]) msgObj;
        String script = (String) params[0];
        SyncWaiter<String> waiter = (SyncWaiter<String>) params[1];
        waiter.setResult(mEngine.executeObjectScriptAndStringify(script));
    }

    public void onFrameCallback(Object[] args) {
        Message.obtain(mHandler, H.ON_FRAME_CALLBACK, args).sendToTarget();
    }

    private void onFrameCallback(Object msgObj) {
        Object[] params = (Object[]) msgObj;
        long frameTimeNanos = (long) params[0];
        mEngine.onFrameCallback(frameTimeNanos);
    }

    @Override
    protected void doShutdown() {
        mJsBridgeRegisterHelper.unregister();
        super.doShutdown();
    }

    private class InspectorNativeCallback implements V8InspectorNative.InspectorNativeCallback{
        @Override
        public void inspectorResponse(int sessionId, int callId, String message) {
            mNative.inspectorResponse(sessionId, callId, message);
        }

        @Override
        public void inspectorSendNotification(int sessionId, int callId, String message) {
            mNative.inspectorSendNotification(sessionId, callId, message);
        }

        @Override
        public void inspectorRunMessageLoopOnPause(int contextGroupId) {
            mNative.inspectorRunMessageLoopOnPause(contextGroupId);
        }

        @Override
        public void inspectorQuitMessageLoopOnPause() {
            mNative.inspectorQuitMessageLoopOnPause();
        }
    }
}

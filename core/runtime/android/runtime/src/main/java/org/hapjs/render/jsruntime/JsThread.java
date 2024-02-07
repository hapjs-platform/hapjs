/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.jsruntime;

import static org.hapjs.render.RootView.MSG_APP_LOAD_END;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import org.hapjs.component.constants.Attributes;
import org.hapjs.logging.RuntimeLogManager;
import org.hapjs.runtime.ResourceConfig;
import org.hapjs.runtime.sandbox.SyncWaiter;
import org.json.JSONObject;

public abstract class JsThread extends HandlerThread {

    public static final String CONFIGURATION_TYPE_LOCALE = "locale";
    public static final String CONFIGURATION_TYPE_THEME_MODE = "themeMode";
    public static final String CONFIGURATION_TYPE_ORIENTATION = "orientation";
    public static final String CONFIGURATION_TYPE_SCREEN_SIZE = "screenSize";
    public static final String INFRASJS_SNAPSHOT_SO_NAME = "infrasjs_snapshot";
    public static final boolean HAS_INFRASJS_SNAPSHOT;
    private static final String TAG = "JsThread";
    private static final int STATE_NONE = -1;
    private static final int STATE_RUNTIME_INITED = 0;
    private static final int STATE_DESTROYING = 1;
    private static final int STATE_DESTROYED = 2;

    static {
        boolean hasInfraSnapshot;
        try {
            System.loadLibrary(INFRASJS_SNAPSHOT_SO_NAME);
            hasInfraSnapshot = true;
        } catch (UnsatisfiedLinkError e) {
            hasInfraSnapshot = false;
        }
        HAS_INFRASJS_SNAPSHOT = hasInfraSnapshot;
    }

    static public class JsEventCallbackData {

        public int pageId;
        public int elementId;
        public String eventName;
        public Map<String, Object> params;
        public Map<String, Object> attributes;

        public JsEventCallbackData() {}

        public JsEventCallbackData(int pageId, int elementId, String eventName,
                                   Map<String, Object> params, Map<String, Object> attributes) {
            this.pageId = pageId;
            this.elementId = elementId;
            this.eventName = eventName;
            this.params = params;
            this.attributes = attributes;
        }
    }

    static public class JsMethodCallbackData {

        public int pageId;
        public String callbackId;
        public Object[] params;

        public JsMethodCallbackData() {}

        public JsMethodCallbackData(int pageId, String callbackId, Object... params) {
            this.pageId = pageId;
            this.callbackId = callbackId;
            this.params = params;
        }
    }

    public class H extends Handler {
        static final protected int MSG_INIT = 1;
        static final protected int MSG_ATTACH = 2;
        static final protected int MSG_EXECUTE_SCRIPT = 3;
        static final protected int MSG_CREATE_APPLICATION = 4;
        static final protected int MSG_DESTROY_APPLICATION = 5;
        static final protected int MSG_CREATE_PAGE = 6;
        static final protected int MSG_RECREATE_PAGE = 7;
        static final protected int MSG_DESTROY_PAGE = 8;
        static final protected int MSG_FIRE_EVENT = 9;
        static final protected int MSG_BACK_PRESS = 10;
        static final protected int MSG_BLOCK = 11;
        static final protected int MSG_SHUTDOWN = 12;
        static final protected int MSG_MENU_PRESS = 13;
        static final protected int MSG_FIRE_CALLBACK = 14;
        static final protected int MSG_ORIENTATION_CHANGE = 15;
        static final protected int MSG_EXECUTE_FUNCTION = 16;
        static final protected int MSG_TERMINATE_EXECUTION = 17;
        static final protected int MSG_FOLD_CARD = 18;
        static final protected int MSG_REFRESH_PAGE = 19;
        static final protected int MSG_UPDATE_LOCALE = 20;
        static final protected int MSG_NOTIFY_CONFIGURATION_CHANGED = 21;
        static final protected int MSG_PAGE_NOT_FOUND = 22;
        static final protected int MSG_ON_REQUEST_APPLICATION = 23;
        static final protected int MSG_ON_SHOW_APPLICATION = 24;
        static final protected int MSG_ON_HIDE_APPLICATION = 25;
        static final protected int MSG_REGISTER_BUNDLE_CHUNKS = 26;
        static final protected int MSG_PAGE_SCROLL = 27;
        static final protected int MSG_PAGE_REACH_TOP = 28;
        static final protected int MSG_PAGE_REACH_BOTTOM = 29;
        static final protected int MSG_FIRE_KEY_EVENT = 30;
        static final protected int MSG_ON_MENU_BUTTON_PRESS = 31;

        H(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_INIT:
                    onInit();
                    if (mEngine == null || mNative == null) {
                        throw new RuntimeException("must initialize mEngine and mNative in onInit");
                    }
                    break;
                case MSG_ATTACH:
                    onAttach(msg.obj);
                    break;
                case MSG_EXECUTE_SCRIPT: {
                    executeVoidScript(msg.obj);
                    break;
                }
                case MSG_EXECUTE_FUNCTION: {
                    executeVoidFunction(msg.obj);
                    break;
                }
                case MSG_CREATE_APPLICATION: {
                    createApplication(msg.obj);
                    break;
                }
                case MSG_DESTROY_APPLICATION: {
                    destroyApplication(msg.obj);
                    break;
                }
                case MSG_CREATE_PAGE: {
                    createPage(msg.obj);
                    break;
                }
                case MSG_RECREATE_PAGE: {
                    recreatePage(msg.obj);
                    break;
                }
                case MSG_PAGE_NOT_FOUND: {
                    notifyPageNotFound(msg.obj);
                    break;
                }
                case MSG_REFRESH_PAGE: {
                    refreshPage(msg.obj);
                    break;
                }
                case MSG_DESTROY_PAGE:
                    destroyPage(msg.obj);
                    break;
                case MSG_FIRE_EVENT: {
                    fireEvent(msg.obj);
                    break;
                }
                case MSG_FIRE_KEY_EVENT: {
                    fireKeyEvent(msg.obj);
                    break;
                }
                case MSG_BACK_PRESS: {
                    backPress(msg.obj);
                    break;
                }
                case MSG_ON_MENU_BUTTON_PRESS: {
                    menuButtonPressPage(msg.obj);
                    break;
                }
                case MSG_FIRE_CALLBACK: {
                    fireCallback(msg.obj);
                    break;
                }
                case MSG_BLOCK: {
                    doBlock();
                    break;
                }
                case MSG_SHUTDOWN: {
                    doShutdown();
                    break;
                }
                case MSG_MENU_PRESS: {
                    onMenuPress(msg.obj);
                    break;
                }
                case MSG_ORIENTATION_CHANGE: {
                    onOrientationChange(msg.obj);
                    break;
                }
                case MSG_UPDATE_LOCALE: {
                    updateLocale(msg.obj);
                    break;
                }
                case MSG_NOTIFY_CONFIGURATION_CHANGED: {
                    notifyConfigurationChanged(msg.obj);
                    break;
                }
                case MSG_TERMINATE_EXECUTION: {
                    // TODO: Fix this. Terminate may cause unknown error.
                    terminateExecution();
                    break;
                }
                case MSG_FOLD_CARD: {
                    onFoldCard(msg.obj);
                    break;
                }
                case MSG_ON_REQUEST_APPLICATION: {
                    onRequestApplication(msg.obj);
                    break;
                }
                case MSG_ON_SHOW_APPLICATION: {
                    onShowApplication(msg.obj);
                    break;
                }
                case MSG_ON_HIDE_APPLICATION: {
                    onHideApplication(msg.obj);
                    break;
                }
                case MSG_REGISTER_BUNDLE_CHUNKS: {
                    registerBundleChunks(msg.obj);
                    break;
                }
                case MSG_PAGE_SCROLL: {
                    onPageScroll(msg.obj);
                    break;
                }
                case MSG_PAGE_REACH_TOP: {
                    onPageReachTop(msg.obj);
                    break;
                }
                case MSG_PAGE_REACH_BOTTOM: {
                    onPageReachBottom(msg.obj);
                    break;
                }
                default: {
                    super.handleMessage(msg);
                    break;
                }
            }
        }
    }

    private static final String INFRAS_SNAPSHOT_SO_NAME = "libinfrasjs_snapshot.so";

    protected IJsEngine mEngine;
    protected IJavaNative mNative;
    protected final H mHandler;

    protected JsThread(String name) {
        super(name);

        start();

        mHandler = createHandler();
    }

    protected H createHandler() {
        return new H(getLooper());
    }

    protected abstract void onInit();

    protected abstract void onAttach(Object msgObj);

    public Handler getHandler() {
        return mHandler;
    }

    public IJsEngine getEngine() {
        return mEngine;
    }

    public IJavaNative getNative() {
        return mNative;
    }

    public void postExecuteScript(String script) {
        Message.obtain(mHandler, H.MSG_EXECUTE_SCRIPT, new Object[] {script}).sendToTarget();
    }

    public void postExecuteVoidScript(String script, String scriptName, int lineNumber) {
        Object[] obj = new Object[]{script, scriptName, lineNumber};
        mHandler.obtainMessage(H.MSG_EXECUTE_SCRIPT, obj).sendToTarget();
    }

    protected void executeVoidScript(Object msgObj) {
        Object[] args = (Object[]) msgObj;
        String script = (String) args[0];
        mEngine.executeVoidScript(script, null, 0);
    }

    public void postExecuteFunction(String name, Object... params) {
        Object[] args = new Object[] {name, params};
        Message.obtain(mHandler, H.MSG_EXECUTE_FUNCTION, args).sendToTarget();
    }

    protected void executeVoidFunction(Object msgObj) {
        Object[] args = (Object[]) msgObj;
        String func = (String) args[0];
        Object[] params = (Object[]) args[1];
        mEngine.executeVoidFunction(func, params);
    }

    protected void createApplication(Object msgObj) {
        Object[] args = (Object[]) msgObj;
        int appId = (int) args[0];
        String js = (String) args[1];
        String css = (String) args[2];
        String metaInfo = (String) args[3];
        mEngine.createApplication(appId, js, css, metaInfo);

        RuntimeLogManager.getDefault().logAppLoadEnd(mEngine.getQuickAppPkg());
    }

    protected void destroyApplication(Object msgObj) {
        Object[] args = (Object[]) msgObj;
        int appId = (int) args[0];
        mEngine.destroyApplication(appId);
    }

    protected void createPage(Object msgObj) {
        Object[] args = (Object[]) msgObj;
        int appId = (int) args[0];
        int pageId = (int) args[1];
        String js = (String) args[2];
        String css = (String) args[3];
        HashMap<String, ?> params = (HashMap<String, ?>) args[4];
        HashMap<String, ?> intent = (HashMap<String, ?>) args[5];
        HashMap<String, ?> meta = (HashMap<String, ?>) args[6];
        mEngine.createPage(appId, pageId, js, css, params, intent, meta);
    }

    public void postRecreatePage(int pageId) {
        mHandler.obtainMessage(H.MSG_RECREATE_PAGE, new Object[]{pageId}).sendToTarget();
    }

    protected void recreatePage(Object msgObj) {
        Object[] args = (Object[]) msgObj;
        int pageId = (int) args[0];
        mEngine.recreatePage(pageId);
    }

    public void postPageNotFound(int appId, String pageUri, int pageId) {
        Message.obtain(mHandler, H.MSG_PAGE_NOT_FOUND, new Object[] {appId, pageUri, pageId}).sendToTarget();
    }

    protected void notifyPageNotFound(Object msgObj) {
        Object[] args = (Object[]) msgObj;
        int appId = (int) args[0];
        String pageUri = (String) args[1];
        mEngine.notifyPageNotFound(appId, pageUri);
    }

    public void postRefreshPage(int pageId, Map<String, ?> params, Map<String, ?> intent) {
        Object[] obj = new Object[]{pageId, params, intent};
        Message.obtain(mHandler, H.MSG_REFRESH_PAGE, obj).sendToTarget();
    }

    protected void refreshPage(Object msgObj) {
        Object[] args = (Object[]) msgObj;
        int pageId = (int) args[0];
        Map<String, ?> params = (Map<String, ?>) args[1];
        Map<String, ?> intent = (Map<String, ?>) args[2];
        mEngine.refreshPage(pageId, params, intent);
    }

    protected void destroyPage(Object msgObj) {
        Object[] args = (Object[]) msgObj;
        int pageId = (int) args[0];
        mEngine.destroyPage(pageId);
    }

    public void postFireEvent(List<JsEventCallbackData> data) {
        if (data == null || data.isEmpty()) {
            return;
        }
        Message.obtain(mHandler, JsThread.H.MSG_FIRE_EVENT, new Object[] {data.get(0).pageId, data, null}).sendToTarget();
    }

    protected void fireEvent(Object msgObj) {
        Object[] args = (Object[]) msgObj;
        List<JsEventCallbackData> data = (List<JsEventCallbackData>) args[1];
        mEngine.fireEvent(data);
    }

    public void postFireKeyEvent(JsEventCallbackData data) {
        Message.obtain(mHandler, JsThread.H.MSG_FIRE_KEY_EVENT, new Object[] {data}).sendToTarget();
    }

    protected void fireKeyEvent(Object msgObj) {
        Object[] args = (Object[]) msgObj;
        AppJsThread.JsEventCallbackData data = (AppJsThread.JsEventCallbackData) args[0];
        if (Attributes.Event.KEY_EVENT.equals(data.eventName)) {
            fireKeyEvent(data);
        } else if (Attributes.Event.KEY_EVENT_PAGE.equals(data.eventName)) {
            firePageKeyEvent(data);
        }
    }

    protected void fireKeyEvent(JsEventCallbackData data) {
        mEngine.fireKeyEvent(data);
    }

    protected boolean firePageKeyEvent(JsEventCallbackData data) {
        return mEngine.keyPressPage(data.pageId, data.params);
    }

    protected void post(Runnable runnable) {
        mHandler.postAtFrontOfQueue(runnable);
    }

    public void postInJsThread(Runnable runnable) {
        mHandler.post(runnable);
    }

    public <T> T postAndWait(Callable<T> callable) throws Exception {
        if (Looper.myLooper() == mHandler.getLooper()) {
            return callable.call();
        } else {
            SyncWaiter<Object> waiter = new SyncWaiter<>((T) null);
            mHandler.post(() -> {
                try {
                    waiter.setResult(callable.call());
                } catch (Exception e) {
                    waiter.setResult(e);
                }
            });
            Object result = waiter.waitAndGetResult();
            if (result instanceof Exception) {
                throw (Exception) result;
            }
            return (T) result;
        }
    }

    protected boolean backPress(Object msgObj) {
        Object[] args = (Object[]) msgObj;
        int pageId = (int) args[0];
        return mEngine.backPressPage(pageId);
    }

    protected boolean menuButtonPressPage(Object msgObj) {
        Object[] args = (Object[]) msgObj;
        return mEngine.menuButtonPressPage((int) args[0]);
    }

    public void postFireCallback(JsThread.JsMethodCallbackData datas) {
        Message.obtain(mHandler, H.MSG_FIRE_CALLBACK, new Object[] {datas}).sendToTarget();
    }

    protected void fireCallback(Object msgObj) {
        Object[] args = (Object[]) msgObj;
        JsThread.JsMethodCallbackData data = (AppJsThread.JsMethodCallbackData) args[0];
        mEngine.fireCallback(data);
    }

    public void block(long delay) {
        mHandler.sendEmptyMessageDelayed(H.MSG_BLOCK, delay);
    }

    protected void doBlock() {
        mEngine.block();
    }

    public void unblock() {
        mHandler.removeMessages(H.MSG_BLOCK);
        if (mEngine != null) {
            mEngine.unblock();
        }
    }

    public void postShutdown() {
        mHandler.obtainMessage(H.MSG_SHUTDOWN).sendToTarget();
    }

    protected void doShutdown() {
        mEngine.shutdown();
    }

    protected boolean onMenuPress(Object msgObj) {
        Object[] args = (Object[]) msgObj;
        return mEngine.menuPressPage((int) args[0]);
    }

    public void postOrientationChange(int pageId, String orientation, float angel) {
        Object[] obj = new Object[]{pageId, orientation, angel};
        mHandler.obtainMessage(H.MSG_ORIENTATION_CHANGE, obj).sendToTarget();
    }

    protected void onOrientationChange(Object msgObj) {
        Object[] args = (Object[]) msgObj;
        int pageId = (int) args[0];
        String orientation = (String) args[1];
        float angel = (float) args[2];
        mEngine.orientationChangePage(pageId, orientation, angel);
    }

    public void postUpdateLocale(String language, String country, Map<String, JSONObject> resourcesJson) {
        if (resourcesJson == null) {
            return;
        }

        Object[] params = new Object[]{language, country, resourcesJson};
        mHandler.obtainMessage(H.MSG_UPDATE_LOCALE, params).sendToTarget();
    }

    protected void updateLocale(Object msgObj) {
        Object[] args = (Object[]) msgObj;
        String language = (String) args[0];
        String country = (String) args[1];
        Map<String, JSONObject> resourcesJson = (Map<String, JSONObject>) args[2];
        mEngine.updateLocale(language, country, resourcesJson);
    }

    public void postNotifyConfigurationChanged(int pageId, String type) {
        Object[] params = new Object[]{pageId, type};
        mHandler.obtainMessage(H.MSG_NOTIFY_CONFIGURATION_CHANGED, params).sendToTarget();
    }

    protected void notifyConfigurationChanged(Object msgObj) {
        Object[] args = (Object[]) msgObj;
        int pageId = (int) args[0];
        String type = (String) args[1];
        mEngine.notifyConfigurationChanged(pageId, type);
    }

    protected void terminateExecution() {
        mEngine.terminateExecution();
    }

    public void postFoldCard(int s, boolean f) {
        Message.obtain(mHandler, H.MSG_FOLD_CARD, new Object[] {s, f}).sendToTarget();
    }

    protected void onFoldCard(Object msgObj) {
        Object[] args = (Object[]) msgObj;
        int s = (int) args[0];
        boolean f = (boolean) args[1];
        mEngine.onFoldCard(s, f);
    }

    protected void onRequestApplication(Object msgObj) {
        Object[] args = (Object[]) msgObj;
        int appId = (int) args[0];
        mEngine.onRequestApplication(appId);
    }

    protected void onShowApplication(Object msgObj) {
        Object[] args = (Object[]) msgObj;
        int appId = (int) args[0];
        mEngine.onShowApplication(appId);
    }

    protected void onHideApplication(Object msgObj) {
        Object[] args = (Object[]) msgObj;
        int appId = (int) args[0];
        mEngine.onHideApplication(appId);
    }

    public void postRegisterBundleChunks(String content) {
        Object[] params = new Object[]{content};
        Message.obtain(mHandler, H.MSG_REGISTER_BUNDLE_CHUNKS, params).sendToTarget();
    }

    protected void registerBundleChunks(Object msgObj) {
        Object[] args = (Object[]) msgObj;
        String content = (String) args[0];
        mEngine.registerBundleChunks(content);
    }

    public void postPageScroll(int pageId, int scrollTop) {
        Message.obtain(mHandler, H.MSG_PAGE_SCROLL, new Object[] {pageId, scrollTop}).sendToTarget();
    }

    protected void onPageScroll(Object msgObj) {
        Object[] args = (Object[]) msgObj;
        int pageId = (int) args[0];
        int scrollTop = (int) args[1];
        mEngine.pageScroll(pageId, scrollTop);
    }

    public void postPageReachTop(int pageId) {
        Message.obtain(mHandler, H.MSG_PAGE_REACH_TOP, new Object[] {pageId}).sendToTarget();
    }

    protected void onPageReachTop(Object msgObj) {
        Object[] args = (Object[]) msgObj;
        int pageId = (int) args[0];
        mEngine.reachPageTop(pageId);
    }

    public void postPageReachBottom(int pageId) {
        Message.obtain(mHandler, H.MSG_PAGE_REACH_BOTTOM, new Object[] {pageId}).sendToTarget();
    }

    protected void onPageReachBottom(Object msgObj) {
        Object[] args = (Object[]) msgObj;
        int pageId = (int) args[0];
        mEngine.reachPageBottom(pageId);
    }

    public static Context getPlatformContext(Context context) {
        if (null == context) {
            return null;
        }
        String platform = ResourceConfig.getInstance().getPlatform();
        try {
            Context platformContext = context.createPackageContext(platform, 0);
            return platformContext;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "failed to getPlatformContext for " + platform, e);
        }
        return null;
    }

    public interface LifecycleCallback {
        /**
         * Run in js core thread
         */
        void onRuntimeCreate();

        /**
         * Run in js core thread
         */
        void onRuntimeDestroy();
    }
}

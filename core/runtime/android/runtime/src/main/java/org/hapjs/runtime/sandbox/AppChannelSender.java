/*
 * Copyright (C) 2023, hapjs.org. All rights reserved.
 */
package org.hapjs.runtime.sandbox;

import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import java.util.List;
import java.util.Map;
import org.hapjs.render.jsruntime.IJsEngine;
import org.hapjs.render.jsruntime.JsThread;
import org.json.JSONObject;

// The channel end in app process sending requests to sandbox process.
public class AppChannelSender extends ChannelSender implements IJsEngine {
    private Handler mHandler;

    public AppChannelSender(ParcelFileDescriptor readSide, ParcelFileDescriptor writeSide, Handler handler) {
        super(readSide, writeSide, handler);
        mHandler = handler;
    }

    @Override
    public void block() {
        invokeAsync(SandboxIpcMethods.BLOCK);
    }

    @Override
    public void unblock() {
        if (Looper.myLooper() != mHandler.getLooper()) {
            mHandler.post(() -> unblock());
            return;
        }
        invokeAsync(SandboxIpcMethods.UNBLOCK);
    }

    @Override
    public void onAttach(String environmentScript, String pkg) {
        invokeAsync(SandboxIpcMethods.ON_ATTACH, environmentScript, pkg);
    }

    @Override
    public void notifyConfigurationChanged(int pageId, String type) {
        invokeAsync(SandboxIpcMethods.NOTIFY_CONFIGURATION_CHANGED, pageId, type);
    }

    @Override
    public void updateLocale(String language, String country, Map<String, JSONObject> resourcesJson) {
        invokeAsync(SandboxIpcMethods.UPDATE_LOCALE, language, country, resourcesJson);
    }

    @Override
    public void registerBundleChunks(String content) {
        invokeAsync(SandboxIpcMethods.REGISTER_BUNDLE_CHUNKS, content);
    }

    @Override
    public void createApplication(int appId, String js, String css, String metaInfo) {
        invokeAsync(SandboxIpcMethods.CREATE_APPLICATION, appId, js, css, metaInfo);
    }

    @Override
    public void onRequestApplication(int appId) {
        invokeAsync(SandboxIpcMethods.REQUEST_APPLICATION, appId);
    }

    @Override
    public void onShowApplication(int appId) {
        invokeAsync(SandboxIpcMethods.ON_SHOW_APPLICATION, appId);
    }

    @Override
    public void onHideApplication(int appId) {
        invokeAsync(SandboxIpcMethods.ON_HIDE_APPLICATION, appId);
    }

    @Override
    public boolean backPressPage(int pageId) {
        return invokeSync(SandboxIpcMethods.BACK_PRESS_PAGE, boolean.class, pageId);
    }

    @Override
    public boolean menuButtonPressPage(int pageId) {
        return invokeSync(SandboxIpcMethods.MENU_BUTTON_PRESS_PAGE, boolean.class, pageId);
    }

    @Override
    public boolean keyPressPage(int pageId, Map<String, Object> params) {
        return invokeSync(SandboxIpcMethods.KEY_PRESS_PAGE, boolean.class, pageId, params);
    }

    @Override
    public boolean menuPressPage(int pageId) {
        return invokeSync(SandboxIpcMethods.MENU_PRESS_PAGE, boolean.class, pageId);
    }

    @Override
    public void orientationChangePage(int pageId, String orientation, float angle) {
        invokeAsync(SandboxIpcMethods.ORIENTATION_CHANGE_PAGE, pageId, orientation, angle);
    }

    @Override
    public void executeVoidScript(String script, String scriptName, int lineNumber) { // TODO: SYNC OR ASYNC
        invokeAsync(SandboxIpcMethods.EXECUTE_VOID_SCRIPT, script, scriptName, lineNumber);
    }

    @Override
    public void executeScript(String script, String scriptName, int lineNumber) {// TODO: SYNC OR ASYNC
        invokeAsync(SandboxIpcMethods.EXECUTE_SCRIPT, script, scriptName, lineNumber);
    }

    @Override
    public void executeVoidFunction(String func, Object[] params) { // TODO: sync or async
        invokeAsync(SandboxIpcMethods.EXECUTE_VOID_FUNCTION, func, params);
    }

    @Override
    public String executeObjectScriptAndStringify(String script) {
        return invokeSync(SandboxIpcMethods.EXECUTE_OBJECT_SCRIPT_AND_STRINGIFY, String.class, script);
    }

    @Override
    public void createPage(int appId, int pageId, String js, String css, Map<String, ?> params, Map<String, ?> intent,
                           Map<String, ?> meta) {
        invokeAsync(SandboxIpcMethods.CREATE_PAGE, appId, pageId, js, css, params, intent, meta);
    }

    @Override
    public void recreatePage(int pageId) {
        invokeAsync(SandboxIpcMethods.RECREATE_PAGE, pageId);
    }

    @Override
    public void refreshPage(int pageId, Map<String, ?> params, Map<String, ?> intent) {
        invokeAsync(SandboxIpcMethods.REFRESH_PAGE, pageId, params, intent);
    }

    @Override
    public void notifyPageNotFound(int appId, String pageUri) {
        invokeAsync(SandboxIpcMethods.NOTIFY_PAGE_NOT_FOUND, appId, pageUri);
    }

    @Override
    public void destroyPage(int pageId) {
        invokeAsync(SandboxIpcMethods.DESTROY_PAGE, pageId);
    }

    @Override
    public void destroyApplication(int appId) {
        invokeAsync(SandboxIpcMethods.DESTROY_APPLICATION, appId);
    }

    @Override
    public void fireEvent(List<JsThread.JsEventCallbackData> datas) {
        invokeAsync(SandboxIpcMethods.FIRE_EVENT, datas);
    }

    @Override
    public void fireKeyEvent(JsThread.JsEventCallbackData data) {
        invokeAsync(SandboxIpcMethods.FIRE_KEY_EVENT, data);
    }

    @Override
    public void fireCallback(JsThread.JsMethodCallbackData data) {
        invokeAsync(SandboxIpcMethods.FIRE_CALLBACK, data);
    }

    @Override
    public void onFoldCard(int s, boolean f) {
        invokeAsync(SandboxIpcMethods.ON_FOLD_CARD, s, f);
    }

    @Override
    public void reachPageTop(int pageId) {
        invokeAsync(SandboxIpcMethods.REACH_PAGE_TOP, pageId);
    }

    @Override
    public void reachPageBottom(int pageId) {
        invokeAsync(SandboxIpcMethods.REACH_PAGE_BOTTOM, pageId);
    }

    @Override
    public void pageScroll(int pageId, int scrollTop) {
        invokeAsync(SandboxIpcMethods.PAGE_SCROLL, pageId, scrollTop);
    }

    @Override
    public void registerComponents(String builtInComponents) {
        invokeAsync(SandboxIpcMethods.REGISTER_COMPONENTS, builtInComponents);
    }

    @Override
    public void terminateExecution() {
        invokeAsync(SandboxIpcMethods.TERMINATE_EXECUTION);
    }

    @Override
    public void shutdown() {
        invokeAsync(SandboxIpcMethods.SHUTDOWN);
    }

    @Override
    public void inspectorHandleMessage(long ptr, int sessionId, String message) {
        invokeAsync(SandboxIpcMethods.INSPECTOR_HANDLE_MESSAGE, ptr, sessionId, message);
    }

    @Override
    public long inspectorInit(boolean autoEnable, int sessionId) {
        return invokeSync(SandboxIpcMethods.INSPECTOR_INIT, long.class, autoEnable, sessionId);
    }

    @Override
    public void inspectorSetV8Context(long ptr, int isJsContextReCreated) {
        invokeAsync(SandboxIpcMethods.INSPECTOR_SET_V8_CONTEXT, ptr, isJsContextReCreated);
    }

    @Override
    public void inspectorDisposeV8Context(long ptr) {
        invokeAsync(SandboxIpcMethods.INSPECTOR_DISPOSE_V8_CONTEXT, ptr);
    }

    @Override
    public void inspectorDestroy(long ptr) {
        invokeAsync(SandboxIpcMethods.INSPECTOR_DESTROY, ptr);
    }

    @Override
    public void inspectorBeginLoadJsCode(String uri, String content) {
        invokeAsync(SandboxIpcMethods.INSPECTOR_BEGIN_LOAD_JS_CODE, uri, content);
    }

    @Override
    public void inspectorEndLoadJsCode(String uri) {
        invokeAsync(SandboxIpcMethods.INSPECTOR_END_LOAD_JS_CODE, uri);
    }

    @Override
    public String inspectorExecuteJsCode(long ptr, String jsCode) {
        return invokeSync(SandboxIpcMethods.INSPECTOR_EXECUTE_JS_CODE, String.class, ptr, jsCode);
    }

    @Override
    public void inspectorFrontendReload(long ptr) {
        invokeAsync(SandboxIpcMethods.INSPECTOR_FRONTEND_RELOAD, ptr);
    }

    @Override
    public void onFrameCallback(long frameTimeNanos) {
        invokeAsync(SandboxIpcMethods.ON_FRAME_CALLBACK, frameTimeNanos);
    }
}

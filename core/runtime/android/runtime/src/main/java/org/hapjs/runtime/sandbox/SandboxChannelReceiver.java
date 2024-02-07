/*
 * Copyright (C) 2023, hapjs.org. All rights reserved.
 */
package org.hapjs.runtime.sandbox;

import android.os.ParcelFileDescriptor;
import java.util.List;
import java.util.Map;
import org.hapjs.render.jsruntime.JsThread;
import org.hapjs.render.jsruntime.SandboxJsThread;
import org.json.JSONObject;

// The channel end in sandbox process receiving requests from app process.
public class SandboxChannelReceiver extends ChannelReceiver {
    private static final String TAG = "SandboxChannelReceiver";

    protected SandboxJsThread mJsThread;

    public SandboxChannelReceiver(ParcelFileDescriptor readSide, ParcelFileDescriptor writeSide, SandboxJsThread jsThread) {
        super(readSide, writeSide, TAG);
        mJsThread = jsThread;
    }

    @Override
    protected String getQuickAppPkg() {
        return mJsThread.getNative() == null ? null : mJsThread.getNative().getQuickAppPkg();
    }

    @Override
    protected Object onInvoke(String method, Object[] args) {
        switch (method) {
            case SandboxIpcMethods.BLOCK:
                block();
                break;
            case SandboxIpcMethods.UNBLOCK:
                unblock();
                break;
            case SandboxIpcMethods.ON_ATTACH:
                onAttach(args);
                break;
            case SandboxIpcMethods.NOTIFY_CONFIGURATION_CHANGED:
                notifyConfigurationChanged(args);
                break;
            case SandboxIpcMethods.UPDATE_LOCALE:
                updateLocale(args);
                break;
            case SandboxIpcMethods.REGISTER_BUNDLE_CHUNKS:
                registerBundleChunks(args);
                break;
            case SandboxIpcMethods.CREATE_APPLICATION:
                createApplication(args);
                break;
            case SandboxIpcMethods.REQUEST_APPLICATION:
                onRequestApplication(args);
                break;
            case SandboxIpcMethods.ON_SHOW_APPLICATION:
                onShowApplication(args);
                break;
            case SandboxIpcMethods.ON_HIDE_APPLICATION:
                onHideApplication(args);
                break;
            case SandboxIpcMethods.BACK_PRESS_PAGE:
                return backPressPage(args);
            case SandboxIpcMethods.MENU_BUTTON_PRESS_PAGE:
                return menuButtonPressPage(args);
            case SandboxIpcMethods.KEY_PRESS_PAGE:
                return keyPressPage(args);
            case SandboxIpcMethods.MENU_PRESS_PAGE:
                return menuPressPage(args);
            case SandboxIpcMethods.ORIENTATION_CHANGE_PAGE:
                orientationChangePage(args);
                break;
            case SandboxIpcMethods.EXECUTE_VOID_SCRIPT:
                executeVoidScript(args);
                break;
            case SandboxIpcMethods.EXECUTE_SCRIPT:
                executeScript(args);
                break;
            case SandboxIpcMethods.EXECUTE_VOID_FUNCTION:
                executeVoidFunction(args);
                break;
            case SandboxIpcMethods.EXECUTE_OBJECT_SCRIPT_AND_STRINGIFY:
                return executeObjectScriptAndStringify(args);
            case SandboxIpcMethods.CREATE_PAGE:
                createPage(args);
                break;
            case SandboxIpcMethods.RECREATE_PAGE:
                recreatePage(args);
                break;
            case SandboxIpcMethods.REFRESH_PAGE:
                refreshPage(args);
                break;
            case SandboxIpcMethods.NOTIFY_PAGE_NOT_FOUND:
                notifyPageNotFound(args);
                break;
            case SandboxIpcMethods.DESTROY_PAGE:
                destroyPage(args);
                break;
            case SandboxIpcMethods.DESTROY_APPLICATION:
                destroyApplication(args);
                break;
            case SandboxIpcMethods.FIRE_EVENT:
                fireEvent(args);
                break;
            case SandboxIpcMethods.FIRE_KEY_EVENT:
                fireKeyEvent(args);
                break;
            case SandboxIpcMethods.FIRE_CALLBACK:
                fireCallback(args);
                break;
            case SandboxIpcMethods.ON_FOLD_CARD:
                onFoldCard(args);
                break;
            case SandboxIpcMethods.REACH_PAGE_TOP:
                reachPageTop(args);
                break;
            case SandboxIpcMethods.REACH_PAGE_BOTTOM:
                reachPageBottom(args);
                break;
            case SandboxIpcMethods.PAGE_SCROLL:
                pageScroll(args);
                break;
            case SandboxIpcMethods.REGISTER_COMPONENTS:
                registerComponents(args);
                break;
            case SandboxIpcMethods.TERMINATE_EXECUTION:
                terminateExecution(args);
                break;
            case SandboxIpcMethods.SHUTDOWN:
                postShutdown(args);
                break;
            case SandboxIpcMethods.INSPECTOR_HANDLE_MESSAGE:
                inspectorHandleMessage(args);
                break;
            case SandboxIpcMethods.INSPECTOR_INIT:
                return inspectorInit(args);
            case SandboxIpcMethods.INSPECTOR_SET_V8_CONTEXT:
                inspectorSetV8Context(args);
                break;
            case SandboxIpcMethods.INSPECTOR_DISPOSE_V8_CONTEXT:
                inspectorDisposeV8Context(args);
                break;
            case SandboxIpcMethods.INSPECTOR_DESTROY:
                inspectorDestroy(args);
                break;
            case SandboxIpcMethods.INSPECTOR_BEGIN_LOAD_JS_CODE:
                inspectorBeginLoadJsCode(args);
                break;
            case SandboxIpcMethods.INSPECTOR_END_LOAD_JS_CODE:
                inspectorEndLoadJsCode(args);
                break;
            case SandboxIpcMethods.INSPECTOR_EXECUTE_JS_CODE:
                return inspectorExecuteJsCode(args);
            case SandboxIpcMethods.INSPECTOR_FRONTEND_RELOAD:
                inspectorFrontendReload(args);
                break;
            case SandboxIpcMethods.ON_FRAME_CALLBACK:
                onFrameCallback(args);
                break;
        }
        return null;
    }

    private void block() {
        mJsThread.block(0);
    }

    private void unblock() {
        mJsThread.unblock();
    }

    private void onAttach(Object[] args) {
        String script = (String) args[0];
        String pkg = (String) args[1];
        mJsThread.postOnAttach(script, pkg);
    }

    private void notifyConfigurationChanged(Object[] args) {
        int pageId = (Integer) args[0];
        String type = (String) args[1];
        mJsThread.postNotifyConfigurationChanged(pageId, type);
    }

    private void updateLocale(Object[] args) {
        String language = (String) args[0];
        String country = (String) args[1];
        Map<String, JSONObject> resourcesJson = (Map<String, JSONObject>) args[2];
        mJsThread.postUpdateLocale(language, country, resourcesJson);
    }

    public void registerBundleChunks(Object[] args) {
        String content = (String) args[0];
        mJsThread.postRegisterBundleChunks(content);
    }

    public void createApplication(Object[] args) {
        int appId = (Integer) args[0];
        String js = (String) args[1];
        String css = (String) args[2];
        String metaInfo = (String) args[3];
        mJsThread.postCreateApplication(appId, js, css, metaInfo);
    }

    public void onRequestApplication(Object[] args) {
        int appId = (Integer) args[0];
        mJsThread.postOnRequestApplication(appId);
    }

    public void onShowApplication(Object[] args) {
        int appId = (Integer) args[0];
        mJsThread.postOnShowApplication(appId);
    }

    public void onHideApplication(Object[] args) {
        int appId = (Integer) args[0];
        mJsThread.postOnHideApplication(appId);
    }

    public Object backPressPage(Object[] args) {
        int pageId = (Integer) args[0];
        return mJsThread.postBackPressPage(pageId);
    }

    public Object menuButtonPressPage(Object[] args) {
        int pageId = (Integer) args[0];

        return mJsThread.postMenuButtonPressPage(pageId);
    }

    public Object keyPressPage(Object[] args) {
        int pageId = (Integer) args[0];
         Map<String, Object> params = (Map<String, Object>) args[1];
        return mJsThread.postKeyPressPage(pageId, params);
    }

    public Object menuPressPage(Object[] args) {
        int pageId = (Integer) args[0];
        return mJsThread.postMenuPressPage(pageId);
    }

    private void orientationChangePage(Object[] args) {
        int pageId = (Integer) args[0];
        String orientation = (String) args[1];
        float angel = (Float) args[2];
        mJsThread.postOrientationChange(pageId, orientation, angel);
    }

    private void executeVoidScript(Object[] args) {
        String script = (String) args[0];
        String scriptName = (String) args[1];
        int lineNumber = (Integer) args[2];
        mJsThread.postExecuteVoidScript(script, scriptName, lineNumber);
    }

    private void executeScript(Object[] args) {
        String script = (String) args[0];
        String scriptName = (String) args[1];
        int lineNumber = (Integer) args[2];
        mJsThread.postExecuteVoidScript(script, scriptName, lineNumber);
    }

    private void executeVoidFunction(Object[] args) {
        String function = (String) args[0];
        Object[] params = (Object[]) args[1];
        mJsThread.postExecuteFunction(function, params);
    }

    private String executeObjectScriptAndStringify(Object[] args) {
        String script = (String) args[0];
        return mJsThread.postExecuteObjectScriptAndStringify(script);
    }

    private void createPage(Object[] args) {
        int appId = (Integer) args[0];
        int pageId = (Integer) args[1];
        String js = (String) args[2];
        String css = (String) args[3];
        Map<String, ?> params = (Map) args[4];
        Map<String, ?> intent = (Map) args[5];
        Map<String, ?> meta = (Map) args[6];
        mJsThread.postCreatePage(appId, pageId, js, css, params, intent, meta);
    }

    private void recreatePage(Object[] args) {
        int pageId = (Integer) args[0];
        mJsThread.postRecreatePage(pageId);
    }

    private void refreshPage(Object[] args) {
        int pageId = (Integer) args[0];
        Map<String, ?> params = (Map) args[1];
        Map<String, ?> intent = (Map) args[2];
        mJsThread.postRefreshPage(pageId, params, intent);
    }

    private void notifyPageNotFound(Object[] args) {
        int appId = (Integer) args[0];
        String pageUri = (String) args[1];
        int pageId = (Integer) args[2];
        mJsThread.postPageNotFound(appId, pageUri, pageId);
    }

    private void destroyPage(Object[] args) {
        int pageId = (Integer) args[0];
        mJsThread.postDestroyPage(pageId);
    }

    private void destroyApplication(Object[] args) {
        int appId = (Integer) args[0];
        mJsThread.postDestroyApplication(appId);
    }

    private void fireEvent(Object[] args) {
        List<JsThread.JsEventCallbackData> data = (List) args[0];
        mJsThread.postFireEvent(data);
    }

    private void fireKeyEvent(Object[] args) {
        JsThread.JsEventCallbackData data = (JsThread.JsEventCallbackData) args[0];
        mJsThread.postFireKeyEvent(data);
    }

    private void fireCallback(Object[] args) {
        JsThread.JsMethodCallbackData data = (JsThread.JsMethodCallbackData) args[0];
        mJsThread.postFireCallback(data);
    }

    private void onFoldCard(Object[] args) {
        int s = (Integer) args[0];
        boolean f = (Boolean) args[1];
        mJsThread.postFoldCard(s, f);
    }

    private void reachPageTop(Object[] args) {
        int pageId = (Integer) args[0];
        mJsThread.postPageReachTop(pageId);
    }

    private void reachPageBottom(Object[] args) {
        int pageId = (Integer) args[0];
        mJsThread.postPageReachBottom(pageId);
    }

    private void pageScroll(Object[] args) {
        int pageId = (Integer) args[0];
        int scrollTop = (Integer) args[1];
        mJsThread.postPageScroll(pageId, scrollTop);
    }

    private void registerComponents(Object[] args) {
        String components = (String) args[0];
        mJsThread.postRegisterComponents(components);
    }

    private void terminateExecution(Object[] args) {
        mJsThread.postTerminateExecution();
    }

    private void postShutdown(Object[] args) {
        mJsThread.postShutdown();
    }

    private void inspectorHandleMessage(Object[] args) {
        mJsThread.postInspectorHandleMessage(args);
    }

    private long inspectorInit(Object[] args) {
        return mJsThread.postInspectorInit(args);
    }

    private void inspectorSetV8Context(Object[] args) {
        mJsThread.postInspectorSetV8Context(args);
    }

    private void inspectorDisposeV8Context(Object[] args) {
        mJsThread.postInspectorDisposeV8Context(args);
    }

    private void inspectorDestroy(Object[] args) {
        mJsThread.postInspectorDestroy(args);
    }

    private void inspectorBeginLoadJsCode(Object[] args) {
        mJsThread.postInspectorBeginLoadJsCode(args);
    }

    private void inspectorEndLoadJsCode(Object[] args) {
        mJsThread.postInspectorEndLoadJsCode(args);
    }

    private String inspectorExecuteJsCode(Object[] args) {
        return mJsThread.postInspectorExecuteJsCode(args);
    }

    private void inspectorFrontendReload(Object[] args) {
        mJsThread.postInspectorFrontendReload(args);
    }

    private void onFrameCallback(Object[] args) {
        mJsThread.onFrameCallback(args);
    }
}

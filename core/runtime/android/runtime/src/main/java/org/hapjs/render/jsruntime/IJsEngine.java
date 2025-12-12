/*
 * Copyright (C) 2023, hapjs.org. All rights reserved.
 */
package org.hapjs.render.jsruntime;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.hapjs.render.Page;
import org.json.JSONObject;

// Object representing the V8/js side
public interface IJsEngine {
    void setQuickAppPkg(String pkg);
    String getQuickAppPkg();

    void block();
    void unblock();
    void onAttach(String environmentScript, String pkg);
    void notifyConfigurationChanged(int pageId, String type);
    void updateLocale(String language, String country, Map<String, JSONObject> resourcesJson);
    void registerBundleChunks(String content);
    void createApplication(int appId, String js, String css, String metaInfo);
    void onRequestApplication(int appId);
    void onShowApplication(int appId);
    void onHideApplication(int appId);
    boolean backPressPage(int pageId);
    boolean menuButtonPressPage(int pageId);
    boolean keyPressPage(int pageId, Map<String, Object> params);
    boolean menuPressPage(int pageId);
    void orientationChangePage(int pageId, String orientation, float angle);
    void executeVoidScript(String script, String scriptName, int lineNumber);
    void executeScript(String script, String scriptName, int lineNumber);
    void executeVoidFunction(String func, Object[] params);
    String executeObjectScriptAndStringify(String script);
    void createPage(int appId, int pageId, String js, String css, Map<String, ?> params, Map<String, ?> intent, Map<String, ?> meta);
    void recreatePage(int pageId);
    void refreshPage(int pageId, Map<String, ?> params, Map<String, ?> intent);
    void notifyPageNotFound(int appId, String pageUri);
    void destroyPage(int pageId);
    void destroyApplication(int appId);
    void fireEvent(List<JsThread.JsEventCallbackData> datas);
    void fireKeyEvent(JsThread.JsEventCallbackData data);
    void fireCallback(JsThread.JsMethodCallbackData data);
    void onFoldCard(int s, boolean f);
    void reachPageTop(int pageId);
    void reachPageBottom(int pageId);
    void pageScroll(int pageId, int scrollTop);
    void registerComponents(String builtInComponents);
    void terminateExecution();
    void shutdown();

    void inspectorHandleMessage(long ptr, int sessionId, String message);
    long inspectorInit(boolean autoEnable, int sessionId);
    void inspectorSetV8Context(long ptr, int isJsContextReCreated);
    void inspectorDisposeV8Context(long ptr);
    void inspectorDestroy(long ptr);
    void inspectorBeginLoadJsCode(String uri, String content);
    void inspectorEndLoadJsCode(String uri);
    String inspectorExecuteJsCode(long ptr, String jsCode);
    void inspectorFrontendReload(long ptr);

    void onFrameCallback(long frameTimeNanos);
}

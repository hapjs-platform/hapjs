/*
 * Copyright (C) 2023, hapjs.org. All rights reserved.
 */
package org.hapjs.render.jsruntime;

import android.os.ConditionVariable;
import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;
import com.eclipsesource.v8.V8RuntimeException;
import com.eclipsesource.v8.V8Value;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.hapjs.bridge.V8ObjectHelper;
import org.hapjs.render.DebugUtils;
import org.hapjs.render.jsruntime.serialize.JavaSerializeObject;
import org.json.JSONObject;

public class JsEngineImpl implements IJsEngine {
    public interface V8ExceptionHandler {
        void onV8Exception(V8RuntimeException e);
    }

    public interface FrameCallback {
        void onFrameCallback(long frameTimeNanos);
    }


    protected JsContext mJsContext;
    protected V8 mV8;
    protected JsThread mJsThread;
    private ConditionVariable mBlocker = new ConditionVariable(true);
    private String mPkg;

    private V8ExceptionHandler mV8ExceptionHandler;
    private V8InspectorNative mV8InspectorNative;
    private V8InspectorNative.InspectorNativeCallback mInspectorNativeCallback;
    private FrameCallback mFrameCallback;

    public JsEngineImpl(JsContext jsContext, V8InspectorNative.InspectorNativeCallback inspectorNativeCallback,
                        V8ExceptionHandler v8ExceptionHandler, FrameCallback frameCallback) {
        mJsContext = jsContext;
        mV8 = jsContext.getV8();
        mJsThread = jsContext.getJsThread();
        mInspectorNativeCallback = inspectorNativeCallback;
        mV8ExceptionHandler = v8ExceptionHandler;
        mFrameCallback = frameCallback;
    }

    @Override
    public void setQuickAppPkg(String pkg) {
        mPkg = pkg;
    }

    @Override
    public String getQuickAppPkg() {
        return mPkg;
    }

    @Override
    public void block() {
        mBlocker.close();
        mBlocker.block();
    }

    @Override
    public void unblock() {
        mBlocker.open();
    }

    @Override
    public void onAttach(String environmentScript, String pkg) {
        mV8.executeScript(environmentScript);
    }

    @Override
    public void notifyConfigurationChanged(int pageId, String type) {
        V8Array args = new V8Array(mV8);
        args.push(pageId);
        V8Object options = new V8Object(mV8);
        options.add("type", type);
        args.push(options);
        try {
            mV8.executeVoidFunction("notifyConfigurationChanged", args);
        } catch (V8RuntimeException e) {
            processV8Exception(e);
        } finally {
            JsUtils.release(args, options);
        }
    }

    @Override
    public void updateLocale(String language, String country, Map<String, JSONObject> resourcesJson) {
        V8Array args = new V8Array(mV8);
        // locale
        V8Object locale = new V8Object(mV8);
        locale.add("language", language);
        locale.add("countryOrRegion", country);
        args.push(locale);
        // resources array
        V8Array resources = new V8Array(mV8);
        for (JSONObject resJson : resourcesJson.values()) {
            JavaSerializeObject object = new JavaSerializeObject(resJson);
            V8Object res = V8ObjectHelper.toV8Object(mV8, object.toMap());
            resources.push(res);
            JsUtils.release(res);
        }
        args.push(resources);
        try {
            mV8.executeVoidFunction("changeAppLocale", args);
        } catch (V8RuntimeException e) {
            processV8Exception(e);
        } finally {
            JsUtils.release(args, locale, resources);
        }
    }

    @Override
    public void registerBundleChunks(String content) {
        V8Array args = new V8Array(mV8);
        try {
            args.push(content);
            mV8.executeVoidFunction("registerBundleChunks", args);
        } catch (V8RuntimeException e) {
            processV8Exception(e);
        } finally {
            JsUtils.release(args);
        }
    }

    @Override
    public void createApplication(int appId, String js, String css, String metaInfo) {
        if (DebugUtils.DBG) DebugUtils.startRecord("JsThreadExecuteApp");
        V8Array args = new V8Array(mV8);
        V8Array args1 = new V8Array(mV8);
        try {
            args.push(metaInfo);
            mV8.executeVoidFunction("registerManifest", args);
            mV8.executeVoidFunction("locateDsl", null);

            args1.push(appId);
            args1.push(js);
            args1.push(css);
            mV8.executeVoidFunction("createApplication", args1);
        } catch (V8RuntimeException ex) {
            processV8Exception(ex);
            return;
        } finally {
            JsUtils.release(args);
            JsUtils.release(args1);
            if (DebugUtils.DBG) DebugUtils.endRecord("JsThreadExecuteApp");
        }
    }

    @Override
    public void onRequestApplication(int appId) {
        V8Array args = new V8Array(mV8);
        args.push(appId);
        try {
            mV8.executeVoidFunction("onRequestApplication", args);
        } catch (V8RuntimeException ex) {
            processV8Exception(ex);
            return;
        } finally {
            JsUtils.release(args);
        }
    }

    @Override
    public void onShowApplication(int appId) {
        V8Array args = new V8Array(mV8);
        args.push(appId);
        try {
            mV8.executeVoidFunction("onShowApplication", args);
        } catch (V8RuntimeException ex) {
            processV8Exception(ex);
            return;
        } finally {
            JsUtils.release(args);
        }
    }

    @Override
    public void onHideApplication(int appId) {
        V8Array args = new V8Array(mV8);
        args.push(appId);
        try {
            mV8.executeVoidFunction("onHideApplication", args);
        } catch (V8RuntimeException ex) {
            processV8Exception(ex);
            return;
        } finally {
            JsUtils.release(args);
        }
    }

    @Override
    public boolean backPressPage(int pageId) {
        boolean consumed = false;
        try {
            consumed = mV8.executeBooleanScript("backPressPage(" + pageId + ");");
        } catch (V8RuntimeException ex) {
            processV8Exception(ex);
        }
        return consumed;
    }

    @Override
    public boolean menuButtonPressPage(int pageId) {
        boolean consumed = false;
        try {
            consumed = mV8.executeBooleanScript("menuButtonPressPage(" + pageId + ");");
        } catch (V8RuntimeException ex) {
            processV8Exception(ex);
        }
        return consumed;
    }

    @Override
    public boolean keyPressPage(int pageId, Map<String, Object> params) {
        boolean consumed = false;
        V8Array args = new V8Array(mV8);
        args.push(pageId);
        V8Object paramsObj = JsUtils.mapToV8Object(mV8, params);
        args.push(paramsObj);
        try {
            consumed = mV8.executeBooleanFunction("keyPressPage", args);
        } catch (V8RuntimeException ex) {
            processV8Exception(ex);
        } finally {
            JsUtils.release(args, paramsObj);
        }
        return consumed;
    }

    @Override
    public boolean menuPressPage(int pageId) {
        boolean consumed = false;
        try {
            consumed = mV8.executeBooleanScript("menuPressPage(" + pageId + ");");
        } catch (V8RuntimeException ex) {
            processV8Exception(ex);
        }
        return consumed;
    }

    @Override
    public void orientationChangePage(int pageId, String orientation, float angle) {
        V8Array array = new V8Array(mV8);
        array.push(pageId);
        V8Object object = new V8Object(mV8);
        object.add("orientation", orientation);
        object.add("angel", angle);
        array.push(object);
        try {
            mV8.executeVoidFunction("orientationChangePage", array);
        } catch (V8RuntimeException ex) {
            processV8Exception(ex);
        } finally {
            JsUtils.release(array, object);
        }
    }

    @Override
    public void executeVoidScript(String script, String scriptName, int lineNumber) {
        if (DebugUtils.DBG) DebugUtils.startRecord("JsThreadExecuteVoidScript");
        try {
            mV8.executeVoidScript(script, scriptName, lineNumber);
        } catch (V8RuntimeException ex) {
            processV8Exception(ex);
        } finally {
            if (DebugUtils.DBG) DebugUtils.endRecord("JsThreadExecuteVoidScript");
        }
    }

    @Override
    public void executeScript(String script, String scriptName, int lineNumber) {
        if (DebugUtils.DBG) DebugUtils.startRecord("JsThreadExecuteScript");
        try {
            mV8.executeScript(script, scriptName, lineNumber);
        } catch (V8RuntimeException ex) {
            processV8Exception(ex);
        } finally {
            if (DebugUtils.DBG) DebugUtils.endRecord("JsThreadExecuteScript");
        }
    }

    @Override
    public void executeVoidFunction(String func, Object[] params) {
        if (DebugUtils.DBG) DebugUtils.startRecord("JsThreadExecuteFunction");
        V8Array v8Params = params == null ? new V8Array(mV8) :
                V8ObjectHelper.toV8Array(mV8, Arrays.asList(params));
        try {
            mV8.executeVoidFunction(func, v8Params);
        } catch (V8RuntimeException ex) {
            processV8Exception(ex);
        } finally {
            JsUtils.release(v8Params);
            if (DebugUtils.DBG) DebugUtils.endRecord("JsThreadExecuteFunction");
        }
    }

    @Override
    public String executeObjectScriptAndStringify(String script) {
        V8Object object = null;
        V8Object json = null;
        V8Array parameters = null;
        try {
            object = mV8.executeObjectScript(script);
            if (object == null) {
                throw new IllegalStateException("related value not exists.");
            }

            json = mV8.getObject("JSON");
            if (json == null) {
                throw new IllegalStateException("V8Object which key is JSON not exists");
            }
            parameters = new V8Array(mV8).push(object);
            return json.executeStringFunction("stringify", parameters);
        } finally {
            if (parameters != null) {
                parameters.release();
            }
            if (object != null) {
                object.release();
            }
            if (json != null) {
                json.release();
            }
        }
    }

    @Override
    public void createPage(int appId, int pageId, String js, String css, Map<String, ?> params, Map<String, ?> intent, Map<String, ?> meta) {
        V8Array args = new V8Array(mV8);
        args.push(pageId);
        args.push(appId);
        args.push(js);
        V8Object paramsObj = JsUtils.mapToV8Object(mV8, params);
        args.push(paramsObj);
        V8Object intentObj = JsUtils.mapToV8Object(mV8, intent);
        args.push(intentObj);
        V8Object metaObj = JsUtils.mapToV8Object(mV8, meta);
        args.push(metaObj);
        args.push(css);
        try {
            mV8.executeVoidFunction("createPage", args);
        } catch (V8RuntimeException ex) {
            processV8Exception(ex);
        } finally {
            JsUtils.release(args, paramsObj, intentObj, metaObj);
        }
    }

    @Override
    public void recreatePage(int pageId) {
        V8Array args = new V8Array(mV8);
        args.push(pageId);
        try {
            mV8.executeVoidFunction("recreatePage", args);
        } catch (V8RuntimeException ex) {
            processV8Exception(ex);
        } finally {
            JsUtils.release(args);
        }
    }

    @Override
    public void refreshPage(int pageId, Map<String, ?> params, Map<String, ?> intent) {
        V8Array args = new V8Array(mV8);
        args.push(pageId);
        V8Object paramsObj = JsUtils.mapToV8Object(mV8, params);
        args.push(paramsObj);
        V8Object intentObj = JsUtils.mapToV8Object(mV8, intent);
        args.push(intentObj);
        try {
            mV8.executeVoidFunction("refreshPage", args);
        } catch (V8RuntimeException ex) {
            processV8Exception(ex);
        } finally {
            JsUtils.release(args, paramsObj, intentObj);
        }
    }

    @Override
    public void notifyPageNotFound(int appId, String pageUri) {
        V8Array args = new V8Array(mV8);
        args.push(appId);
        V8Object uriObj = JsUtils.mapToV8Object(mV8, Collections.singletonMap("uri", pageUri));
        args.push(uriObj);
        try {
            mV8.executeVoidFunction("notifyPageNotFound", args);
        } catch (V8RuntimeException ex) {
            processV8Exception(ex);
        } finally {
            JsUtils.release(args, uriObj);
        }
    }

    @Override
    public void destroyPage(int pageId) {
        V8Array args = new V8Array(mV8);
        args.push(pageId);
        try {
            mV8.executeVoidFunction("destroyPage", args);
        } catch (V8RuntimeException ex) {
            processV8Exception(ex);
        } finally {
            JsUtils.release(args);
        }
    }

    @Override
    public void destroyApplication(int appId) {
        V8Array args = new V8Array(mV8);
        try {
            args.push(appId);
            mV8.executeVoidFunction("destroyApplication", args);
        } catch (V8RuntimeException ex) {
            processV8Exception(ex);
        } finally {
            JsUtils.release(args);
        }
    }

    @Override
    public void fireEvent(List<JsThread.JsEventCallbackData> datas) {
        if (datas == null || datas.isEmpty()) {
            return;
        }
        if (DebugUtils.DBG) DebugUtils.startRecord("JsThreadFireEvent");

        V8Array args = new V8Array(mV8);
        int pageId = datas.get(0).pageId;
        args.push(pageId);//pageId

        V8Array events = new V8Array(mV8);
        List<V8Value> releaseObj = new ArrayList<>();
        for (JsThread.JsEventCallbackData data : datas) {
            V8Object event = new V8Object(mV8);
            event.add("action", 1);//action

            V8Array eventArg = new V8Array(mV8);
            eventArg.push(data.elementId);//ref
            eventArg.push(data.eventName);//eventType
            V8Object paramsObj = JsUtils.mapToV8Object(mV8, data.params);
            eventArg.push(paramsObj);//params
            V8Object attributesObj = JsUtils.mapToV8Object(mV8, data.attributes);
            eventArg.push(attributesObj);//attributes

            event.add("args", eventArg);
            events.push(event);

            releaseObj.add(event);
            releaseObj.add(eventArg);
            releaseObj.add(paramsObj);
            releaseObj.add(attributesObj);
        }
        args.push(events);
        releaseObj.add(events);

        try {
            mV8.executeVoidFunction("execJSBatch", args);
        } catch (V8RuntimeException ex) {
            processV8Exception(ex);
        } finally {
            int size = releaseObj.size();
            V8Value[] temp = new V8Value[size];
            for (int i = 0; i < size; i++) {
                temp[i] = releaseObj.get(i);
            }

            JsUtils.release(args, temp);
            if (DebugUtils.DBG) DebugUtils.endRecord("JsThreadFireEvent");
        }
    }

    @Override
    public void fireKeyEvent(JsThread.JsEventCallbackData data) {
        if (DebugUtils.DBG) DebugUtils.startRecord("JsThreadFireKeyEvent");

        V8Array args = new V8Array(mV8);
        args.push(data.pageId);
        V8Array events = new V8Array(mV8);
        V8Object event = new V8Object(mV8);
        event.add("action", 1);
        V8Array eventArg = new V8Array(mV8);
        eventArg.push(data.elementId);
        eventArg.push(data.eventName);
        V8Object paramsObj = JsUtils.mapToV8Object(mV8, data.params);
        eventArg.push(paramsObj);
        event.add("args", eventArg);
        events.push(event);
        args.push(events);

        try {
            mV8.executeVoidFunction("execJSBatch", args);
        } catch (V8RuntimeException ex) {
            processV8Exception(ex);
        } finally {
            JsUtils.release(args, events, event, eventArg, paramsObj);
            if (DebugUtils.DBG) DebugUtils.endRecord("JsThreadFireKeyEvent");
        }
    }

    @Override
    public void fireCallback(JsThread.JsMethodCallbackData data) {
        if (DebugUtils.DBG) DebugUtils.startRecord("JsThreadFireCallback");

        V8Array args = new V8Array(mV8);
        args.push(data.pageId);

        V8Array methodArray = new V8Array(mV8);
        V8Object methodObject = new V8Object(mV8);

        methodObject.add("action", 2);

        V8Array callbackArgs = new V8Array(mV8);
        callbackArgs.push(data.callbackId);
        V8Array callbackArgsParams = new V8Array(mV8);
        for (Object obj : data.params) {
            JsUtils.push(callbackArgsParams, obj);
        }
        callbackArgs.push(callbackArgsParams);

        methodObject.add("args", callbackArgs);
        methodArray.push(methodObject);
        args.push(methodArray);

        try {
            mV8.executeVoidFunction("execJSBatch", args);
        } catch (V8RuntimeException ex) {
            processV8Exception(ex);
        } finally {
            JsUtils.release(args, methodArray, methodObject, callbackArgs, callbackArgsParams);
            if (DebugUtils.DBG) DebugUtils.endRecord("JsThreadFireCallback");
        }
    }

    @Override
    public void onFoldCard(int s, boolean f) {
        V8Array args = new V8Array(mV8);
        try {
            args.push(s);
            args.push(f);
            mV8.executeVoidFunction("foldCard", args);
        } catch (V8RuntimeException ex) {
            processV8Exception(ex);
        } finally {
            JsUtils.release(args);
        }
    }

    @Override
    public void reachPageTop(int pageId) {
        try {
            mV8.executeVoidScript("reachPageTop(" + pageId + ");");
        } catch (V8RuntimeException e) {
            processV8Exception(e);
        }
    }

    @Override
    public void reachPageBottom(int pageId) {
        try {
            mV8.executeVoidScript("reachPageBottom(" + pageId + ");");
        } catch (V8RuntimeException e) {
            processV8Exception(e);
        }
    }

    @Override
    public void pageScroll(int pageId, int scrollTop) {
        V8Array args = new V8Array(mV8);
        args.push(pageId);
        V8Object options = new V8Object(mV8);
        options.add("scrollTop", scrollTop);
        args.push(options);
        try {
            mV8.executeVoidFunction("pageScroll", args);
        } catch (V8RuntimeException e) {
            processV8Exception(e);
        } finally {
            JsUtils.release(args, options);
        }
    }

    @Override
    public void registerComponents(String builtInComponents) {
        V8Array parameters = new V8Array(mV8);
        try {
            parameters.push(builtInComponents);
            mV8.executeVoidFunction("registerComponents", parameters);
        } finally {
            JsUtils.release(parameters);
        }
    }

    @Override
    public void terminateExecution() {
        mV8.terminateExecution();
    }

    @Override
    public void shutdown() {
        mJsContext.dispose();
    }

    @Override
    public void inspectorHandleMessage(long ptr, int sessionId, String message) {
        ensureV8InspectorNative();
        mV8InspectorNative.nativeHandleMessage(ptr, sessionId, message);
    }

    @Override
    public long inspectorInit(boolean autoEnable, int sessionId) {
        ensureV8InspectorNative();
        return mV8InspectorNative.initNative(autoEnable, sessionId);
    }

    @Override
    public void inspectorSetV8Context(long ptr, int isJsContextReCreated) {
        ensureV8InspectorNative();
        mV8InspectorNative.nativeSetV8Context(ptr, mV8, isJsContextReCreated);
    }

    @Override
    public void inspectorDisposeV8Context(long ptr) {
        ensureV8InspectorNative();
        mV8InspectorNative.nativeDisposeV8Context(ptr);
    }

    @Override
    public void inspectorDestroy(long ptr) {
        ensureV8InspectorNative();
        mV8InspectorNative.nativeDestroy(ptr);
    }

    @Override
    public void inspectorBeginLoadJsCode(String uri, String content) {
        ensureV8InspectorNative();
        mV8InspectorNative.nativeBeginLoadJsCode(uri, content);
    }

    @Override
    public void inspectorEndLoadJsCode(String uri) {
        ensureV8InspectorNative();
        mV8InspectorNative.nativeEndLoadJsCode(uri);
    }

    @Override
    public String inspectorExecuteJsCode(long ptr, String jsCode) {
        ensureV8InspectorNative();
        return mV8InspectorNative.nativeExecuteJsCode(ptr, jsCode);
    }

    @Override
    public void inspectorFrontendReload(long ptr) {
        ensureV8InspectorNative();
        mV8InspectorNative.nativeFrontendReload(ptr);
    }

    @Override
    public void onFrameCallback(long frameTimeNanos) {
        if (mFrameCallback != null) {
            mFrameCallback.onFrameCallback(frameTimeNanos);
        }
    }

    private void ensureV8InspectorNative() {
        if (mV8InspectorNative == null) {
            mV8InspectorNative = new V8InspectorNative(mInspectorNativeCallback);
        }
    }

    public void processV8Exception(V8RuntimeException ex) {
        mV8ExceptionHandler.onV8Exception(ex);
    }
}

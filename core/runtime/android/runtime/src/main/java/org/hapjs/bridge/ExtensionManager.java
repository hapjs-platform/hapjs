/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.Nullable;
import com.eclipsesource.v8.JavaCallback;
import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;
import org.hapjs.bridge.permission.HapPermissionManager;
import org.hapjs.bridge.permission.PermissionCallback;
import org.hapjs.common.executors.Executor;
import org.hapjs.common.executors.Executors;
import org.hapjs.common.utils.FeatureInnerBridge;
import org.hapjs.logging.RuntimeLogManager;
import org.hapjs.model.AppInfo;
import org.hapjs.model.CardInfo;
import org.hapjs.render.PageManager;
import org.hapjs.render.RootView;
import org.hapjs.render.jsruntime.JsThread;
import org.hapjs.render.jsruntime.JsUtils;
import org.hapjs.render.jsruntime.module.ModuleBridge;
import org.hapjs.render.jsruntime.serialize.JavaSerializeObject;
import org.hapjs.render.jsruntime.serialize.Serializable;
import org.hapjs.render.jsruntime.serialize.SerializeObject;
import org.hapjs.runtime.HapConfig;
import org.hapjs.runtime.HapEngine;
import org.json.JSONException;
import org.json.JSONObject;

public class ExtensionManager {
    public static final String TAG = "ExtensionManager";

    private static final Response RESPONSE_ASYNC = new Response(Response.CODE_ASYNC, "");
    private static final Response RESPONSE_CALLBACK = new Response(Response.CODE_CALLBACK, "");
    private static final Executor SINGLE_THREAD_EXECUTOR = Executors.createSingleThreadExecutor();

    /**
     * callback would be set as -1 when unset event
     */
    private static final String UNSET_JS_CALLBACK = "-1";

    protected Context mContext;
    protected FeatureBridge mFeatureBridge;
    protected ModuleBridge mModuleBridge;
    private HybridManager mHybridManager;
    private LifecycleListenerImpl mLifecycleListener;
    private JsThread mJsThread;
    private WidgetBridge mWidgetBridge;
    private V8Object mRegisteredInterface;

    private FeatureInvokeListener mFeatureInvokeListener;

    public ExtensionManager(JsThread jsThread, Context context) {
        mJsThread = jsThread;
        mContext = context;
        mFeatureBridge = new FeatureBridge(mContext, getClass().getClassLoader());
        mModuleBridge = new ModuleBridge(getClass().getClassLoader());
        mWidgetBridge = new WidgetBridge(getClass().getClassLoader());
    }

    public static boolean isValidCallback(String jsCallback) {
        return !TextUtils.isEmpty(jsCallback) && !UNSET_JS_CALLBACK.equals(jsCallback);
    }

    public void onRuntimeInit(V8 v8) {
        register(v8);

        publish(ModuleBridge.getModuleMapJSONString());
    }

    private void register(V8 v8) {
        JsInterface jsInterface = new JsInterface(this);
        mRegisteredInterface =
                JsInterfaceProxy.register(v8, jsInterface, JsInterface.INTERFACE_NAME);
    }

    public void attach(RootView rootView, PageManager pageManager, AppInfo appInfo) {
        mModuleBridge.attach(rootView, pageManager, appInfo);
    }

    public void onRuntimeCreate(AppInfo appInfo) {
        mFeatureBridge.addFeatures(appInfo.getFeatureInfos());

        publish(FeatureBridge.getFeatureMapJSONString());
        publish(WidgetBridge.getWidgetMetaDataJSONString());
    }

    protected String buildRegisterScript(String bridgeJson) {
        StringBuilder sb = new StringBuilder();
        sb.append("registerModules('");
        sb.append(bridgeJson);
        sb.append("','feature');");
        return sb.toString();
    }

    private void publish(String bridgeJson) {
        String registerScript = buildRegisterScript(bridgeJson);
        mJsThread.getJsContext().getV8().executeScript(registerScript);
    }

    public void configApplication(AppInfo appInfo) {
        if (!HapEngine.getInstance(appInfo.getPackage()).isCardMode()) {
            mFeatureBridge.addFeatures(appInfo.getFeatureInfos());
        }
    }

    public void setHybridManager(final HybridManager hybridManager) {
        if (mHybridManager != null) {
            mHybridManager.removeLifecycleListener(mLifecycleListener);
        }
        if (mLifecycleListener == null) {
            mLifecycleListener = new LifecycleListenerImpl();
        }

        mHybridManager = hybridManager;
        mHybridManager.addLifecycleListener(mLifecycleListener);
    }

    protected boolean isFeatureAvailable(String feature) {
        if (HapConfig.getInstance().isFeatureConfiged(feature)) {
            return true;
        }
        AbstractExtension extension = mFeatureBridge.getExtension(feature);
        if (extension != null && extension.isBuiltInExtension()) {
            return true;
        }
        ApplicationContext applicationContext = mHybridManager.getApplicationContext();
        if (HapEngine.getInstance(applicationContext.getPackage()).isCardMode()) {
            CardInfo cardInfo = mHybridManager.getCurrentPageCardInfo();
            return cardInfo == null ? false : cardInfo.isFeatureAvailable(feature);
        } else {
            AppInfo appInfo = applicationContext.getAppInfo();
            return appInfo == null ? false : appInfo.isFeatureAvailable(feature);
        }
    }

    protected Request buildRequest(
            String action, Object rawParams, int instanceId, String jsCallback) {
        Request request = new Request();
        request.setAction(action);
        request.setRawParams(rawParams);
        request.setHapEngine(mHybridManager.getHapEngine());
        request.setApplicationContext(mHybridManager.getApplicationContext());
        request.setNativeInterface(mHybridManager.getNativeInterface());
        request.setView(mHybridManager.getHybridView());
        request.setInstanceId(instanceId);
        request.setJsCallback(jsCallback);
        return request;
    }

    public Response invoke(
            String name, String action, Object rawParams, String jsCallback, int instanceId) {
        Log.d(TAG, "invoke name=" + name + ", action=" + action + ", jsCallback=" + jsCallback);
        RuntimeLogManager.getDefault()
                .logFeatureInvoke(mHybridManager.getApplicationContext().getPackage(), name,
                        action);
        if (mFeatureInvokeListener != null) {
            mFeatureInvokeListener.invoke(name, action, rawParams, jsCallback, instanceId);
        }
        return onInvoke(name, action, rawParams, jsCallback, instanceId, null);
    }

    public Response onInvoke(
            String name,
            String action,
            Object rawParams,
            String jsCallback,
            int instanceId,
            Callback realCallback) {
        AbstractExtension f = null;
        String pkg = mHybridManager.getApplicationContext().getPackage();
        if (isFeatureAvailable(name)) {
            f = mFeatureBridge.getExtension(name);
            // Operating environment does not meet the requirements
            if (null != f && !mHybridManager.getResidentManager().isAllowToInvoke(name, action)) {
                Response response =
                        new Response(
                                Response.CODE_PERMISSION_ERROR,
                                "Refuse to use this interfaces in background: " + name);
                callback(response, jsCallback, realCallback);
                RuntimeLogManager.getDefault().logFeatureResult(pkg, name, action, response);
                return response;
            }
        }
        if (f == null) {
            f = mModuleBridge.getExtension(name);
        }
        if (f == null) {
            f = mWidgetBridge.getExtension(name);
        }

        if (f == null) {
            String err = "Extension not available: " + name;
            Log.e(TAG, err);
            Response response = new Response(Response.CODE_PERMISSION_ERROR, err);
            callback(response, jsCallback, realCallback);
            RuntimeLogManager.getDefault().logFeatureResult(pkg, name, action, response);
            return response;
        }

        Request request = buildRequest(action, rawParams, instanceId, jsCallback);

        Extension.Mode mode = f.getInvocationMode(request);
        if (mode == Extension.Mode.SYNC) {
            Response response = f.invoke(request);
            if (FeatureInnerBridge.H5_JS_CALLBACK.equals(jsCallback)) {
                if (realCallback != null) {
                    realCallback.callback(response);
                }
            }
            RuntimeLogManager.getDefault().logFeatureResult(pkg, name, action, response);
            return response;
        } else if (mode == Extension.Mode.SYNC_CALLBACK) {
            setCallbackToRequest(pkg, name, action, jsCallback, realCallback, request, mode);
            return f.invoke(request);
        } else {
            setCallbackToRequest(pkg, name, action, jsCallback, realCallback, request, mode);
            Executor executor = f.getExecutor(request);
            executor = executor == null ? Executors.io() : executor;
            new AsyncInvocation(f, request, executor).execute();
            if (mode == AbstractExtension.Mode.ASYNC) {
                return RESPONSE_ASYNC;
            } else {
                return RESPONSE_CALLBACK;
            }
        }
    }

    private void setCallbackToRequest(String pkg, String name, String action, String jsCallback, Callback realCallback, Request request, Extension.Mode mode) {
        if (null != realCallback) {
            request.setCallback(new CallbackWrapper(pkg, name, action, realCallback));
        } else {
            Callback callback = new Callback(this, jsCallback, mode);
            request.setCallback(new CallbackWrapper(pkg, name, action, callback));
        }
    }
    public void dispose() {
        JsUtils.release(mRegisteredInterface);
        mRegisteredInterface = null;
        disposeFeature(true, mLifecycleListener);
    }

    private void disposeFeature(boolean force, @Nullable LifecycleListenerImpl disposeListener) {
        if (mFeatureBridge != null) {
            mFeatureBridge.dispose(force);
        }
        if (mHybridManager != null) {
            InstanceManager.getInstance().dispose(mHybridManager, force);
            if (disposeListener != null) {
                mHybridManager.removeLifecycleListener(disposeListener);
            }
        }
    }

    public void callback(Response response, String jsCallback) {
        callback(response, jsCallback, null);
    }

    public void callback(Response response, String jsCallback, Callback realCallback) {
        if (response != null && isValidCallback(jsCallback)) {
            if (FeatureInnerBridge.H5_JS_CALLBACK.equals(jsCallback)) {
                if (realCallback != null) {
                    realCallback.callback(response);
                }
            } else {
                SINGLE_THREAD_EXECUTOR.execute(new JsInvocation(response, jsCallback));
            }
        }
    }

    private static class JsInterfaceProxy extends V8Object {
        private final JsInterface jsInterface;
        private final JavaCallback invoke =
                new JavaCallback() {
                    @Override
                    public Object invoke(V8Object receiver, V8Array parameters) {
                        Object rawParams = parameters.get(2);
                        Object instanceId = parameters.get(4);
                        if (!(instanceId instanceof Integer)) {
                            instanceId = -1;
                        }
                        Response response =
                                jsInterface.invoke(
                                        parameters.getString(0),
                                        parameters.getString(1),
                                        rawParams,
                                        parameters.getString(3),
                                        (int) instanceId);

                        if (rawParams instanceof V8Object) {
                            JsUtils.release((V8Object) rawParams);
                        }
                        return response == null ? null : response.toJavascriptResult(v8);
                    }
                };

        private JsInterfaceProxy(V8 v8, JsInterface jsInterface) {
            super(v8);
            this.jsInterface = jsInterface;
        }

        static V8Object register(V8 v8, JsInterface jsInterface, String name) {
            JsInterfaceProxy proxy = new JsInterfaceProxy(v8, jsInterface);
            v8.add(name, proxy);
            proxy.registerJavaMethod(proxy.invoke, "invoke");
            return proxy;
        }
    }

    class AsyncInvocation implements Runnable {
        private AbstractExtension mFeature;
        private Request mRequest;
        private Executor mPool;

        public AsyncInvocation(AbstractExtension feature, Request request, Executor executor) {
            mFeature = feature;
            mRequest = request;
            mPool = executor;
        }

        public void execute() {
            mPool.execute(this);
        }

        @Override
        public void run() {
            String[] permissions = mFeature.getPermissions(mRequest);
            if (permissions == null || permissions.length == 0) {
                mFeature.invoke(mRequest);
            } else {
                AbstractExtension.PermissionPromptStrategy strategy =
                        mFeature.getPermissionPromptStrategy(mRequest);
                HapPermissionManager.getDefault()
                        .requestPermissions(
                                mHybridManager, permissions, new PermissionCallbackImpl(),
                                strategy);
            }
        }

        private class PermissionCallbackImpl implements PermissionCallback {

            @Override
            public void onPermissionAccept() {
                // Execute in pool to avoid blocking in UI thread
                mPool.execute(() -> mFeature.invoke(mRequest));
            }

            @Override
            public void onPermissionReject(int reason, boolean dontDisturb) {
                switch (reason) {
                    case Response.CODE_TOO_MANY_REQUEST:
                        mRequest.getCallback().callback(Response.TOO_MANY_REQUEST);
                        break;
                    case Response.CODE_USER_DENIED:
                    default:
                        mRequest.getCallback().callback(Response.getUserDeniedResponse(dontDisturb));
                        break;
                }
            }
        }
    }

    public void setFeatureInvokeListener(FeatureInvokeListener listener) {
        this.mFeatureInvokeListener = listener;
    }

    public FeatureInvokeListener getFeatureInvokeListener() {
        return mFeatureInvokeListener;
    }

    private class JsInvocation implements Runnable {
        private Response mResponse;
        private String mJsCallback;

        public JsInvocation(Response response, String jsCallback) {
            mResponse = response;
            mJsCallback = jsCallback;
        }

        @Override
        public void run() {
            try {
                Object params;
                try {
                    if (mResponse.getSerializeType() == Serializable.TYPE_JSON) {
                        JSONObject result = new JSONObject();
                        result.put("callback", mJsCallback);
                        result.put("data", mResponse.toJSON());
                        params = result.toString();
                    } else {
                        SerializeObject javaBridgeMap =
                                new JavaSerializeObject()
                                        .put("callback", mJsCallback)
                                        .put("data", mResponse.toSerializeObject());
                        params = javaBridgeMap.toMap();
                    }
                } catch (OutOfMemoryError e) {
                    Log.e(TAG, "invoke js callback get oom!", e);
                    Response response = new Response(Response.CODE_OOM_ERROR, "has oom error");
                    JSONObject result = new JSONObject();
                    result.put("callback", mJsCallback);
                    result.put("data", response.toJSON());
                    params = result.toString();
                }
                mJsThread.postExecuteFunction("execInvokeCallback", params);
            } catch (JSONException e) {
                Log.e(TAG, "Fail to invoke js callback", e);
            }
        }
    }

    private class LifecycleListenerImpl extends LifecycleListener {
        @Override
        public void onPageChange() {
            super.onPageChange();
            disposeFeature(false, null);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            disposeFeature(true, this);
        }
    }
    public HybridManager getHybridManager() {
        return mHybridManager;
    }

    public JsThread getJsThread() {
        return mJsThread;
    }
}

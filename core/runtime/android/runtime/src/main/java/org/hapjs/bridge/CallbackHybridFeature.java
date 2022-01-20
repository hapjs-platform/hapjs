/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.ViewGroup;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.hapjs.render.DecorLayout;
import org.hapjs.render.Display;
import org.hapjs.render.RootView;
import org.hapjs.render.jsruntime.serialize.SerializeException;
import org.hapjs.render.jsruntime.serialize.SerializeObject;
import org.hapjs.render.vdom.DocComponent;
import org.hapjs.render.vdom.VDocument;
import org.json.JSONObject;

public abstract class CallbackHybridFeature extends FeatureExtension
        implements CallbackContextHolder {
    private static final String PARAM_RESERVED = "reserved";
    protected final Object mCallbackLock = new Object();
    private final Map<String, CallbackContext> mCallbackContexts = new ConcurrentHashMap<>();

    protected static boolean isReserved(JSONObject params) {
        return params != null && params.optBoolean(PARAM_RESERVED);
    }

    protected static boolean isReserved(SerializeObject params) {
        return params != null && params.optBoolean(PARAM_RESERVED);
    }

    protected static boolean isReserved(Request request) {
        try {
            SerializeObject params = request.getSerializeParams();
            return isReserved(params);
        } catch (SerializeException e) {
            return false;
        }
    }

    public void putCallbackContext(CallbackContext callbackContext) {
        String action = callbackContext.getAction();
        removeCallbackContext(action);
        synchronized (mCallbackLock) {
            mCallbackContexts.put(action, callbackContext);
            callbackContext.onCreate();
        }
    }

    public void removeCallbackContext(String action) {
        CallbackContext callbackContext;
        synchronized (mCallbackLock) {
            callbackContext = mCallbackContexts.remove(action);
            if (callbackContext != null) {
                callbackContext.onDestroy();
            }
        }
    }

    public void runCallbackContext(String action, int what, Object obj) {
        CallbackContext callbackContext;
        synchronized (mCallbackLock) {
            callbackContext = mCallbackContexts.get(action);
        }
        if (callbackContext != null) {
            callbackContext.callback(what, obj);
        }
    }

    @Override
    public void dispose(boolean force) {
        synchronized (mCallbackLock) {
            if (mCallbackContexts.isEmpty()) {
                return;
            }
            Iterator<Map.Entry<String, CallbackContext>> iterator =
                    mCallbackContexts.entrySet().iterator();
            while (iterator.hasNext()) {
                CallbackContext callbackContext = iterator.next().getValue();
                if (callbackContext == null) {
                    iterator.remove();
                } else {
                    if (force || !callbackContext.isReserved()) {
                        callbackContext.onDestroy();
                        iterator.remove();
                    }
                }
            }
        }
    }

    public void notifyFeatureStatus(Handler handler, Request request, int status) {
        Handler tmpHandler = handler;
        if (null == tmpHandler) {
            tmpHandler = new Handler(Looper.getMainLooper());
        }
        tmpHandler.post(
                new Runnable() {
                    @Override
                    public void run() {
                        String featureName = getName();
                        Display display = getDisPlay(request);
                        if (null != display) {
                            if (display.isShowMenubar()) {
                                display.notifyFeatureStatus(status);
                            } else {
                                Log.i(
                                        (null != featureName ? featureName :
                                                "CallbackHybridFeature"),
                                        "isShowMenubar false.");
                            }
                        } else {
                            Log.e(
                                    (null != featureName ? featureName : "CallbackHybridFeature"),
                                    "notifyFeatureStatus  error display is null.");
                        }
                    }
                });
    }

    private Display getDisPlay(Request request) {
        NativeInterface nativeInterface = null;
        RootView rootView = null;
        VDocument vDocument = null;
        DocComponent docComponent = null;
        DecorLayout decorLayout = null;
        Display display = null;
        if (null != request) {
            nativeInterface = request.getNativeInterface();
        }
        if (null != nativeInterface) {
            rootView = nativeInterface.getRootView();
        }
        if (null != rootView) {
            vDocument = rootView.getDocument();
        }
        if (null != vDocument) {
            docComponent = vDocument.getComponent();
        }
        if (null != docComponent) {
            ViewGroup viewGroup = docComponent.getInnerView();
            if (viewGroup instanceof DecorLayout) {
                decorLayout = (DecorLayout) viewGroup;
            }
        }
        if (null != decorLayout) {
            display = decorLayout.getDecorLayoutDisPlay();
        }
        return display;
    }
}

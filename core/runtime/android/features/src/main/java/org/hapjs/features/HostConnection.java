/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features;

import android.text.TextUtils;
import android.util.Log;
import java.lang.ref.WeakReference;
import org.hapjs.bridge.CallbackContext;
import org.hapjs.bridge.CallbackHybridFeature;
import org.hapjs.bridge.FeatureExtension;
import org.hapjs.bridge.HostCallbackManager;
import org.hapjs.bridge.HybridManager;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.FeatureExtensionAnnotation;

@FeatureExtensionAnnotation(
        name = HostConnection.FEATURE_NAME,
        residentType = FeatureExtensionAnnotation.ResidentType.USEABLE,
        actions = {
                @ActionAnnotation(name = HostConnection.ACTION_SEND, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(
                        name = HostConnection.EVENT_REGISTER,
                        mode = FeatureExtension.Mode.CALLBACK,
                        type = FeatureExtension.Type.EVENT,
                        alias = HostConnection.EVENT_REGISTER_ALIAS)
        })
public class HostConnection extends CallbackHybridFeature {
    protected static final String FEATURE_NAME = "system.hostconnection";
    static final String ACTION_SEND = "send";
    static final String EVENT_REGISTER = "__onregistercallback";
    static final String EVENT_REGISTER_ALIAS = "onregistercallback";
    private static final String TAG = "HostConnection";
    private final Object mLockObj = new Object();
    private WeakReference<HybridManager> mHybridManagerRef;
    private boolean mDisposed;

    @Override
    protected Response invokeInner(Request request) throws Exception {
        String action = request.getAction();
        if (TextUtils.isEmpty(action)) {
            return Response.NO_ACTION;
        }
        switch (action) {
            case ACTION_SEND:
                send(request);
                break;
            case EVENT_REGISTER:
                handleEvent(request);
                break;
            default:
                return Response.NO_ACTION;
        }
        return Response.SUCCESS;
    }

    private void send(Request request) {
        synchronized (mLockObj) {
            if (mDisposed) {
                Log.w(TAG, "HostConnection has disposed");
                return;
            }
            String rawParams = request.getRawParams();
            HybridManager hyManager = request.getView().getHybridManager();
            HostCallbackManager manager = HostCallbackManager.getInstance();
            manager.doHostCallback(hyManager, rawParams, request.getCallback());
            mHybridManagerRef = new WeakReference<>(hyManager);
        }
    }

    private void handleEvent(Request request) {
        if (request.getCallback().isValid()) {
            synchronized (mLockObj) {
                if (mDisposed) {
                    Log.w(TAG, "HostConnection has disposed");
                    return;
                }
                HybridManager hyManager = request.getView().getHybridManager();
                HostCallback hostCallback = new HostCallback(hyManager, request);
                putCallbackContext(hostCallback);
                HostCallbackManager.getInstance().addJsCallback(hyManager, this);
                mHybridManagerRef = new WeakReference<>(hyManager);
            }
        } else {
            removeCallbackContext(request.getAction());
        }
    }

    @Override
    public String getName() {
        return FEATURE_NAME;
    }

    @Override
    public void dispose(boolean force) {
        super.dispose(force);
        if (force) {
            synchronized (mLockObj) {
                HybridManager hybridManager =
                        mHybridManagerRef == null ? null : mHybridManagerRef.get();
                if (hybridManager != null) {
                    HostCallbackManager.getInstance().removeCallback(hybridManager);
                }
                mDisposed = true;
            }
        }
    }

    private class HostCallback extends CallbackContext {

        private final HybridManager mManager;

        HostCallback(HybridManager manager, Request request) {
            super(HostConnection.this, request.getAction(), request, false);
            mManager = manager;
        }

        @Override
        public void callback(int what, Object obj) {
            getRequest().getCallback().callback(new Response(obj));
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            HostCallbackManager.getInstance()
                    .removeJsCallback(getRequest().getView().getHybridManager());
        }
    }
}

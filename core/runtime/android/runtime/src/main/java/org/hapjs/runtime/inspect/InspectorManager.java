/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.runtime.inspect;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.View;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import okhttp3.Interceptor;
import okhttp3.WebSocket;

import org.hapjs.common.net.HttpConfig;
import org.hapjs.common.utils.FileUtils;
import org.hapjs.render.Page;
import org.hapjs.render.VDomChangeAction;
import org.hapjs.render.jsruntime.AppJsThread;
import org.hapjs.render.jsruntime.IJsEngine;
import org.hapjs.render.jsruntime.JsThread;
import org.hapjs.runtime.HapEngine;
import org.hapjs.runtime.ProviderManager;
import org.hapjs.runtime.resource.ResourceManager;

public class InspectorManager {
    public static final String TAG = "InspectorManager";
    private WeakReference<AppJsThread> mJsthread;
    private InspectorProvider mProviderImpl;
    private boolean mEnabled;

    private InspectorManager() {
        init();
    }

    public static InspectorProvider getInspector() {
        return Holder.INSTANCE.mProviderImpl;
    }

    public static boolean inspectorEnabled() {
        return Holder.INSTANCE.mEnabled;
    }

    public static void update() {
        Holder.INSTANCE.init();
    }

    public static InspectorManager getInstance() {
        return Holder.INSTANCE;
    }

    public void notifyJsThreadReady(AppJsThread jsThread) {
        if (mEnabled) {
            getInspector().onJsContextCreated(jsThread.getEngine());
        } else {
            mJsthread = new WeakReference<>(jsThread);
        }
    }

    private void sendJsthreadReadyMsg() {
        if (mJsthread != null) {
            AppJsThread jsThread = mJsthread.get();
            if (jsThread != null) {
                jsThread.postInitInspectorJsContext();
            }
        }
    }

    private synchronized void init() {
        mProviderImpl = ProviderManager.getDefault().getProvider(InspectorProvider.NAME);
        if (mProviderImpl == null) {
            mProviderImpl = new InspectorProviderStub();
            mEnabled = false;
        } else {
            mEnabled = true;
            HttpConfig.get().setNetworkInterceptor(mProviderImpl.getNetworkInterceptor());
            sendJsthreadReadyMsg();
        }
    }

    public String getSourcemap(String packageName, String path) {
        String content = "";
        ResourceManager resourceManager = HapEngine.getInstance(packageName).getResourceManager();
        Context context = HapEngine.getInstance(packageName).getContext();
        Uri uri = resourceManager.getResource(path);
        if (uri == null) {
            Log.e(TAG, "Cache is missing: " + path);
        }
        try {
            content = FileUtils.readUriAsString(context, uri);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Cache is missing: " + path, e);
        } catch (IOException e) {
            Log.e(TAG, "Fail to read file", e);
        }
        return content;
    }

    private static class Holder {
        private static final InspectorManager INSTANCE = new InspectorManager();
    }

    private static class InspectorProviderStub implements InspectorProvider {
        @Override
        public void onPagePreChange(int oldIndex, int newIndex, Page oldPage, Page newPage) {
        }

        @Override
        public void onPageChanged(int oldIndex, int newIndex, Page oldPage, Page newPage) {
        }

        @Override
        public void onPageRemoved(int index, Page page) {
        }

        @Override
        public Interceptor getNetworkInterceptor() {
            return null;
        }

        @Override
        public WebSocket.Factory getWebSocketFactory() {
            return null;
        }

        @Override
        public void onJsContextCreated(IJsEngine engine) {
        }

        @Override
        public void onJsContextDispose(IJsEngine engine) {
        }

        @Override
        public void onAppliedChangeAction(
                Context context, JsThread jsThread, VDomChangeAction action) {
        }

        @Override
        public boolean processInspectRequest(String url, Context context) {
            return false;
        }

        @Override
        public void onConsoleMessage(int level, String msg) {
        }

        @Override
        public void onBeginLoadJsCode(String uri, String content) {
        }

        @Override
        public void onEndLoadJsCode(String uri) {
        }

        @Override
        public boolean isInspectorReady() {
            return true;
        }

        @Override
        public void setRootView(View view) {
        }

        @Override
        public void inspectorResponse(int sessionId, int callId, String message) {
        }

        @Override
        public void inspectorSendNotification(int sessionId, int callId, String message) {
        }

        @Override
        public void inspectorRunMessageLoopOnPause(int contextGroupId) {
        }

        @Override
        public void inspectorQuitMessageLoopOnPause() {
        }
    }
}

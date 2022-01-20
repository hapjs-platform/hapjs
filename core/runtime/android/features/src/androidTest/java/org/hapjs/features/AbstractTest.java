/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features;

import android.content.Intent;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.hapjs.bridge.ApplicationContext;
import org.hapjs.bridge.Callback;
import org.hapjs.bridge.ExtensionManager;
import org.hapjs.bridge.ExtensionManagerHelper;
import org.hapjs.bridge.FeatureExtension;
import org.hapjs.bridge.HybridManager;
import org.hapjs.bridge.HybridView;
import org.hapjs.bridge.NativeInterface;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.render.RootView;
import org.hapjs.runtime.FeaturesApplication;
import org.hapjs.runtime.HapEngine;
import org.hapjs.runtime.RuntimeActivity;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;

public abstract class AbstractTest {
    private static ExecutorService sPool = Executors.newCachedThreadPool();
    /* Instantiate an IntentsTestRule object. */
    @Rule
    public ActivityTestRule<RuntimeActivity> mActivityRule =
            new ActivityTestRule<RuntimeActivity>(RuntimeActivity.class) {
                @Override
                protected Intent getActivityIntent() {
                    Intent intent = new Intent();
                    intent.setClass(InstrumentationRegistry.getContext(), RuntimeActivity.class);
                    intent.putExtra(RuntimeActivity.EXTRA_APP, FeaturesApplication.APP_ID);
                    return intent;
                }
            };
    private FeatureExtension mFeature;

    public AbstractTest(FeatureExtension feature) {
        mFeature = feature;
    }

    @Before
    public void setup() {
        // wait for JsThread to be ready
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected Response invoke(String action, JSONObject params) {
        return invoke(action, params, true);
    }

    protected Response invoke(String action, JSONObject params, boolean tryWait) {
        Request request = buildRequest(action, null, null);
        FeatureExtension.Mode mode = mFeature.getInvocationMode(request);
        Response[] holder;
        if (mode == FeatureExtension.Mode.SYNC) {
            holder = null;
        } else {
            holder = new Response[1];
        }
        request = buildRequest(action, params, holder);
        RuntimeActivity activity = mActivityRule.getActivity();
        HybridView hybridView = activity.getHybridView();
        ExtensionManager extensionManager =
                ((RootView) hybridView.getWebView()).getJsThread().getBridgeManager();
        if (mode == FeatureExtension.Mode.SYNC || !tryWait) {
            Response response = mFeature.invoke(request);
            return response;
        } else {
            Executor executor = mFeature.getExecutor(request);
            executor = executor == null ? sPool : executor;
            executor.execute(
                    ExtensionManagerHelper
                            .newAsyncInvocation(extensionManager, mFeature, request, executor));
            try {
                waitResult(holder);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return holder[0];
        }
    }

    private Request buildRequest(String action, JSONObject params, final Response[] holder) {
        Request request = new Request();
        request.setAction(action);
        if (params == null) {
            request.setRawParams("");
        } else {
            request.setRawParams(params.toString());
        }
        RuntimeActivity activity = mActivityRule.getActivity();
        HybridView hybridView = activity.getHybridView();
        HybridManager hybridManager = hybridView.getHybridManager();
        NativeInterface nativeInterface = new NativeInterface(hybridManager);
        ExtensionManager extensionManager =
                ((RootView) hybridView.getWebView()).getJsThread().getBridgeManager();
        request.setNativeInterface(nativeInterface);
        request.setCallback(
                new Callback(extensionManager, "", mFeature.getInvocationMode(request)) {
                    @Override
                    public void callback(Response response) {
                        if (holder != null) {
                            synchronized (holder) {
                                holder[0] = response;
                                holder.notifyAll();
                            }
                        }
                    }
                });
        ApplicationContext applicationContext =
                HapEngine.getInstance("features_test").getApplicationContext();
        request.setApplicationContext(applicationContext);
        return request;
    }

    private void waitResult(Response[] holder) throws InterruptedException {
        synchronized (holder) {
            if (holder[0] == null) {
                holder.wait();
            }
        }
    }
}

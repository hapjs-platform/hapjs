/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.jsruntime.module;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.TextUtils;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.hapjs.bridge.ApplicationContext;
import org.hapjs.bridge.Extension;
import org.hapjs.bridge.FeatureExtension;
import org.hapjs.bridge.HybridRequest;
import org.hapjs.bridge.LifecycleListener;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.ModuleExtensionAnnotation;
import org.hapjs.card.provider.CardSubscriptionProvider;
import org.hapjs.common.executors.Executors;
import org.hapjs.model.AppInfo;
import org.hapjs.model.CardInfo;
import org.hapjs.render.jsruntime.serialize.JavaSerializeObject;
import org.hapjs.render.jsruntime.serialize.SerializeException;
import org.hapjs.render.jsruntime.serialize.SerializeObject;
import org.hapjs.runtime.HapEngine;
import org.hapjs.runtime.ProviderManager;

@ModuleExtensionAnnotation(
        name = CardModule.NAME,
        actions = {
                @ActionAnnotation(name = CardModule.ACTION_CHECK_STATE, mode = Extension.Mode.ASYNC),
                @ActionAnnotation(name = CardModule.ACTION_ADD, mode = Extension.Mode.ASYNC)
        })
public class CardModule extends FeatureExtension {

    protected static final String NAME = "system.card";
    protected static final String ACTION_CHECK_STATE = "checkState";
    protected static final String ACTION_ADD = "add";

    protected static final String KEY_PATH = "path";
    protected static final String KEY_STATE = "state";
    protected static final String KEY_DESCRIPTION = "description";
    protected static final String KEY_ILLUSTRATION = "illustration";

    private static final int ERROR_UNSUPPORTED = 1000;
    private static final int ERROR_RESOURCE_NOT_FOUND = 1001;
    private static final int ERROR_NOT_INSTALLED = 1002;
    private final Set<String> mBufferSet = new HashSet<>();

    @Override
    protected Response invokeInner(Request request) throws Exception {
        String action = request.getAction();
        if (ACTION_CHECK_STATE.equals(action)) {
            checkState(request);
        } else if (ACTION_ADD.equals(action)) {
            add(request);
        }
        return Response.NO_ACTION;
    }

    @Override
    public String getName() {
        return NAME;
    }

    private void checkState(Request request) throws SerializeException {
        CardSubscriptionProvider cardProvider =
                ProviderManager.getDefault().getProvider(CardSubscriptionProvider.NAME);
        if (cardProvider == null) {
            request
                    .getCallback()
                    .callback(new Response(ERROR_UNSUPPORTED, "Card subscription is unsupported."));
            return;
        }
        SerializeObject reader = request.getSerializeParams();
        String path = reader.getString(KEY_PATH);

        HybridRequest.HapRequest hybridRequest =
                getHapRequest(request.getApplicationContext().getPackage(), path);
        ApplicationContext applicationContext =
                HapEngine.getInstance(hybridRequest.getPackage()).getApplicationContext();

        AppInfo appInfo = applicationContext.getAppInfo();
        if (appInfo == null) {
            request.getCallback().callback(new Response(ERROR_NOT_INSTALLED, "HAP not installed."));
            return;
        }
        String cardPath = hybridRequest.getFullPath();

        if (!checkCard(hybridRequest, appInfo)) {
            request
                    .getCallback()
                    .callback(
                            new Response(ERROR_RESOURCE_NOT_FOUND, "No card found in:" + cardPath));
            return;
        }

        String uri = hybridRequest.getUri();
        synchronized (mBufferSet) {
            if (mBufferSet.contains(uri)) {
                request.getCallback().callback(Response.TOO_MANY_REQUEST);
                return;
            }
            mBufferSet.add(uri);
        }
        Context context = request.getApplicationContext().getContext().getApplicationContext();
        Executors.io()
                .execute(
                        () -> {
                            try {
                                int state = cardProvider.checkState(context, appInfo, cardPath);
                                SerializeObject result = new JavaSerializeObject();
                                result.put(KEY_STATE, state);
                                request.getCallback().callback(new Response(result));
                            } catch (Exception e) {
                                request.getCallback().callback(getExceptionResponse(request, e));
                            } finally {
                                synchronized (mBufferSet) {
                                    mBufferSet.remove(uri);
                                }
                            }
                        });
    }

    private HybridRequest.HapRequest getHapRequest(String appPackage, String path) {
        HybridRequest.HapRequest hybridRequest;
        if (path.startsWith("hap://")) {
            hybridRequest =
                    (HybridRequest.HapRequest) new HybridRequest.Builder().uri(path).build();
        } else {
            hybridRequest =
                    (HybridRequest.HapRequest)
                            new HybridRequest.Builder()
                                    .uri(String.format("hap://card/%1$s%2$s", appPackage, path))
                                    .build();
        }
        return hybridRequest;
    }

    private void add(Request request) throws SerializeException {
        CardSubscriptionProvider cardProvider =
                ProviderManager.getDefault().getProvider(CardSubscriptionProvider.NAME);
        if (cardProvider == null) {
            request
                    .getCallback()
                    .callback(new Response(ERROR_UNSUPPORTED, "Card subscription is unsupported."));
            return;
        }
        SerializeObject reader = request.getSerializeParams();
        String path = reader.optString(KEY_PATH);
        String description = reader.optString(KEY_DESCRIPTION);
        if (TextUtils.isEmpty(path)) {
            request
                    .getCallback()
                    .callback(
                            new Response(Response.CODE_ILLEGAL_ARGUMENT, "path must not be empty"));
            return;
        }
        if (TextUtils.isEmpty(description)) {
            request
                    .getCallback()
                    .callback(new Response(Response.CODE_ILLEGAL_ARGUMENT,
                            "description must not be empty"));
            return;
        }

        HybridRequest.HapRequest hybridRequest =
                getHapRequest(request.getApplicationContext().getPackage(), path);
        HapEngine engine = HapEngine.getInstance(hybridRequest.getPackage());
        ApplicationContext applicationContext = engine.getApplicationContext();

        String name = applicationContext.getName();
        AppInfo appInfo = applicationContext.getAppInfo();
        if (appInfo == null) {
            request.getCallback().callback(new Response(ERROR_NOT_INSTALLED, "HAP not installed."));
            return;
        }
        String cardPath = hybridRequest.getFullPath();

        if (!checkCard(hybridRequest, appInfo)) {
            request
                    .getCallback()
                    .callback(
                            new Response(ERROR_RESOURCE_NOT_FOUND, "No card found in:" + cardPath));
            return;
        }
        Uri illustration =
                engine.getResourceManager().getResource(reader.getString(KEY_ILLUSTRATION));
        if (illustration == null) {
            request
                    .getCallback()
                    .callback(new Response(Response.CODE_FILE_NOT_FOUND, "unknown image uri"));
            return;
        }

        String uri = hybridRequest.getUri();
        synchronized (mBufferSet) {
            if (mBufferSet.contains(uri)) {
                request.getCallback().callback(Response.TOO_MANY_REQUEST);
                return;
            }
            mBufferSet.add(uri);
        }
        Activity activity = request.getNativeInterface().getActivity();
        Context context = request.getApplicationContext().getContext().getApplicationContext();
        DialogInterface.OnClickListener listener =
                (dialog, which) -> {
                    if (which == DialogInterface.BUTTON_POSITIVE) {
                        AsyncTask.THREAD_POOL_EXECUTOR.execute(
                                () -> {
                                    try {
                                        if (cardProvider.addCard(context, appInfo, cardPath)) {
                                            request.getCallback().callback(Response.SUCCESS);
                                        } else {
                                            request.getCallback().callback(Response.ERROR);
                                        }
                                    } catch (Exception e) {
                                        request.getCallback()
                                                .callback(getExceptionResponse(request, e));
                                    } finally {
                                        synchronized (mBufferSet) {
                                            mBufferSet.remove(uri);
                                        }
                                    }
                                });
                    } else {
                        if (which == DialogInterface.BUTTON_NEGATIVE) {
                            request.getCallback().callback(Response.CANCEL);
                        }
                        synchronized (mBufferSet) {
                            mBufferSet.remove(uri);
                        }
                    }
                };
        Dialog dialog =
                cardProvider.createDialog(activity, name, description, illustration, listener);
        activity.runOnUiThread(() -> showPrompt(request, dialog));
    }

    private boolean checkCard(HybridRequest.HapRequest request, AppInfo appInfo)
            throws IllegalArgumentException {
        String cardPath = request.getFullPath();
        Map<String, CardInfo> cardInfoMap = appInfo.getRouterInfo().getCardInfos();
        if (!cardInfoMap.isEmpty()) {
            for (CardInfo cardInfo : cardInfoMap.values()) {
                if (cardInfo.getPath().equals(cardPath)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void showPrompt(Request request, Dialog dialog) {
        dialog.show();

        LifecycleListener lifecycleListener =
                new LifecycleListener() {
                    @Override
                    public void onDestroy() {
                        dialog.dismiss();
                    }
                };
        dialog.setOnDismissListener(
                dialog1 -> request.getView().getHybridManager()
                        .removeLifecycleListener(lifecycleListener));
        request.getView().getHybridManager().addLifecycleListener(lifecycleListener);
    }
}

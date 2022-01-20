/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.service.exchange;

import android.content.Context;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import org.hapjs.bridge.FeatureExtension;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.FeatureExtensionAnnotation;
import org.hapjs.cache.CacheStorage;
import org.hapjs.common.executors.Executors;
import org.hapjs.common.executors.ScheduledExecutor;
import org.hapjs.features.service.exchange.common.ExchangeUriProvider;
import org.hapjs.features.service.exchange.common.ExchangeUtils;
import org.hapjs.features.service.exchange.common.PackageUtil;
import org.hapjs.render.jsruntime.serialize.JavaSerializeObject;
import org.hapjs.render.jsruntime.serialize.SerializeException;
import org.hapjs.render.jsruntime.serialize.SerializeObject;

@FeatureExtensionAnnotation(
        name = ExchangeFeature.FEATURE_NAME,
        actions = {
                @ActionAnnotation(name = ExchangeFeature.ACTION_SET, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(name = ExchangeFeature.ACTION_GET, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(name = ExchangeFeature.ACTION_REMOVE, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(name = ExchangeFeature.ACTION_CLEAR, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(
                        name = ExchangeFeature.ACTION_GRANT_PERMISSION,
                        mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(
                        name = ExchangeFeature.ACTION_REVOKE_PERMISSION,
                        mode = FeatureExtension.Mode.ASYNC)
        })
public class ExchangeFeature extends FeatureExtension {

    protected static final String FEATURE_NAME = "service.exchange";
    protected static final String ACTION_SET = "set";
    protected static final String ACTION_GET = "get";
    protected static final String ACTION_REMOVE = "remove";
    protected static final String ACTION_CLEAR = "clear";
    protected static final String ACTION_GRANT_PERMISSION = "grantPermission";
    protected static final String ACTION_REVOKE_PERMISSION = "revokePermission";
    protected static final String PARAM_KEY = "key";
    protected static final String PARAM_VALUE = "value";
    protected static final String PARAM_PKG = "package";
    protected static final String PARAM_SIGN = "sign";
    protected static final String PARAM_WRITABLE = "writable";
    protected static final String PARAM_SCOPE = "scope";
    protected static final int CODE_NO_PERMISSION = Response.CODE_FEATURE_ERROR;
    private static final String TAG = "ExchangeFeature";
    private static final String SCOPE_APPLICATION = "application";
    private static final String SCOPE_GLOBAL = "global";
    private static final String SCOPE_VENDOR = "vendor";

    public ExchangeFeature() {
        ExchangeUriProvider.setProvider(new HapUriProvider());
    }

    @Override
    protected Response invokeInner(Request request) throws Exception {
        try {
            if (ACTION_SET.equals(request.getAction())) {
                set(request);
            } else if (ACTION_GET.equals(request.getAction())) {
                get(request);
            } else if (ACTION_REMOVE.equals(request.getAction())) {
                remove(request);
            } else if (ACTION_CLEAR.equals(request.getAction())) {
                clear(request);
            } else if (ACTION_GRANT_PERMISSION.equals(request.getAction())) {
                grantPermission(request);
            } else if (ACTION_REVOKE_PERMISSION.equals(request.getAction())) {
                revokePermission(request);
            }
        } catch (IllegalArgumentException e) {
            request.getCallback()
                    .callback(new Response(Response.CODE_ILLEGAL_ARGUMENT, e.getMessage()));
        } catch (SecurityException e) {
            request.getCallback().callback(new Response(CODE_NO_PERMISSION, e.getMessage()));
        }
        return Response.SUCCESS;
    }

    @Override
    public ScheduledExecutor getExecutor(Request request) {
        return ExecutorHolder.INSTANCE;
    }

    private void set(Request request) {
        SerializeObject params = null;
        try {
            params = request.getSerializeParams();
        } catch (SerializeException e) {
            Log.e(TAG, "set error", e);
        }
        if (params == null || params.length() == 0) {
            request.getCallback()
                    .callback(new Response(Response.CODE_ILLEGAL_ARGUMENT, "no params"));
            return;
        }
        String key = params.optString(PARAM_KEY);
        String value = params.optString(PARAM_VALUE);
        if (TextUtils.isEmpty(key)) {
            request.getCallback().callback(new Response(Response.CODE_ILLEGAL_ARGUMENT, "no key"));
            return;
        }
        if (value == null) {
            request.getCallback()
                    .callback(new Response(Response.CODE_ILLEGAL_ARGUMENT, "no value"));
            return;
        }
        String scope = params.optString(PARAM_SCOPE, SCOPE_APPLICATION);
        String pkg = request.getApplicationContext().getPackage();
        Context context = request.getNativeInterface().getActivity();
        boolean result;
        if (TextUtils.equals(SCOPE_APPLICATION, scope)) {
            String targetPkg = params.optString(PARAM_PKG);
            String targetSign = params.optString(PARAM_SIGN);
            if (TextUtils.isEmpty(targetPkg) && !TextUtils.isEmpty(targetSign)
                    || !TextUtils.isEmpty(targetPkg) && TextUtils.isEmpty(targetSign)) {
                request
                        .getCallback()
                        .callback(new Response(Response.CODE_ILLEGAL_ARGUMENT, "no pkg or sign"));
                return;
            }
            result = ExchangeUtils.setAppData(context, pkg, key, value, targetPkg, targetSign);
        } else if (TextUtils.equals(SCOPE_GLOBAL, scope)) {
            result = ExchangeUtils.setGlobalData(context, pkg, key, value);
        } else if (TextUtils.equals(SCOPE_VENDOR, scope)) {
            result = ExchangeUtils.setVendorData(context, pkg, key, value);
        } else {
            request.getCallback()
                    .callback(new Response(Response.CODE_ILLEGAL_ARGUMENT, "illegal scope"));
            return;
        }
        request.getCallback().callback(result ? Response.SUCCESS : Response.ERROR);
    }

    private void get(Request request) {
        SerializeObject params = null;
        try {
            params = request.getSerializeParams();
        } catch (SerializeException e) {
            Log.e(TAG, "get error", e);
        }
        if (params == null || params.length() == 0) {
            request.getCallback()
                    .callback(new Response(Response.CODE_ILLEGAL_ARGUMENT, "no params"));
            return;
        }
        String key = params.optString(PARAM_KEY);
        if (TextUtils.isEmpty(key)) {
            request.getCallback().callback(new Response(Response.CODE_ILLEGAL_ARGUMENT, "no key"));
            return;
        }
        String scope = params.optString(PARAM_SCOPE, SCOPE_APPLICATION);
        String targetPkg = params.optString(PARAM_PKG);
        String targetSign = params.optString(PARAM_SIGN);
        String pkg = request.getApplicationContext().getPackage();
        Context context = request.getNativeInterface().getActivity();
        String value = null;
        if (SCOPE_APPLICATION.equals(scope)) {
            if (TextUtils.isEmpty(targetPkg) || TextUtils.isEmpty(targetSign)) {
                request
                        .getCallback()
                        .callback(
                                new Response(
                                        Response.CODE_ILLEGAL_ARGUMENT,
                                        "package and sign must be set when scope is application"));
                return;
            }
            value = ExchangeUtils.getAppData(context, pkg, targetPkg, targetSign, key);
        } else if (SCOPE_GLOBAL.equals(scope)) {
            if (!TextUtils.isEmpty(targetPkg) || !TextUtils.isEmpty(targetSign)) {
                request
                        .getCallback()
                        .callback(
                                new Response(
                                        Response.CODE_ILLEGAL_ARGUMENT,
                                        "package and sign must be null when scope is global"));
                return;
            }
            value = ExchangeUtils.getGlobalData(context, pkg, key);
        } else if (SCOPE_VENDOR.equals(scope)) {
            if (!TextUtils.isEmpty(targetPkg) || !TextUtils.isEmpty(targetSign)) {
                request
                        .getCallback()
                        .callback(
                                new Response(
                                        Response.CODE_ILLEGAL_ARGUMENT,
                                        "package and sign must be null when scope is vendor"));
                return;
            }
            value = ExchangeUtils.getVendorData(context, pkg, key);
        } else {
            request.getCallback()
                    .callback(new Response(Response.CODE_ILLEGAL_ARGUMENT, "illegal scope"));
            return;
        }

        JavaSerializeObject result = new JavaSerializeObject();
        result.put(PARAM_VALUE, value);
        request.getCallback().callback(new Response(result));
    }

    private void clear(Request request) {
        String pkg = request.getApplicationContext().getPackage();
        Context context = request.getNativeInterface().getActivity();
        boolean result = ExchangeUtils.clear(context, pkg);
        request.getCallback().callback(result ? Response.SUCCESS : Response.ERROR);
    }

    private void remove(Request request) {
        SerializeObject params = null;
        try {
            params = request.getSerializeParams();
        } catch (SerializeException e) {
            Log.e(TAG, "remove error", e);
        }
        if (params == null || params.length() == 0) {
            request.getCallback()
                    .callback(new Response(Response.CODE_ILLEGAL_ARGUMENT, "no params"));
            return;
        }
        String key = params.optString(PARAM_KEY);
        if (TextUtils.isEmpty(key)) {
            request.getCallback().callback(new Response(Response.CODE_ILLEGAL_ARGUMENT, "no key"));
            return;
        }
        String targetPkg = params.optString(PARAM_PKG);
        String targetSign = params.optString(PARAM_SIGN);
        if (TextUtils.isEmpty(targetPkg) && !TextUtils.isEmpty(targetSign)
                || !TextUtils.isEmpty(targetPkg) && TextUtils.isEmpty(targetSign)) {
            request
                    .getCallback()
                    .callback(new Response(Response.CODE_ILLEGAL_ARGUMENT, "no pkg or sign"));
            return;
        }
        String pkg = request.getApplicationContext().getPackage();
        Context context = request.getNativeInterface().getActivity();
        boolean result = ExchangeUtils.remove(context, pkg, key, targetPkg, targetSign);
        request.getCallback().callback(result ? Response.SUCCESS : Response.ERROR);
    }

    private void grantPermission(Request request) {
        SerializeObject params = null;
        try {
            params = request.getSerializeParams();
        } catch (SerializeException e) {
            Log.e(TAG, "grant permission error", e);
        }
        if (params == null || params.length() == 0) {
            request.getCallback()
                    .callback(new Response(Response.CODE_ILLEGAL_ARGUMENT, "no params"));
            return;
        }
        String grantPkg = params.optString(PARAM_PKG);
        if (TextUtils.isEmpty(grantPkg)) {
            request.getCallback().callback(new Response(Response.CODE_ILLEGAL_ARGUMENT, "no pkg"));
            return;
        }
        String grantSign = params.optString(PARAM_SIGN);
        if (TextUtils.isEmpty(grantSign)) {
            request.getCallback().callback(new Response(Response.CODE_ILLEGAL_ARGUMENT, "no sign"));
            return;
        }
        String pkg = request.getApplicationContext().getPackage();
        String grantKey = params.optString(PARAM_KEY);
        Context context = request.getNativeInterface().getActivity();
        if (!TextUtils.isEmpty(grantKey)) {
            String targetSign =
                    PackageUtil.getSignDigest(
                            Base64.decode(CacheStorage.getInstance(context).getPackageSign(pkg),
                                    Base64.DEFAULT));
            String value = ExchangeUtils.getAppData(context, pkg, pkg, targetSign, grantKey);
            if (value == null) {
                request
                        .getCallback()
                        .callback(new Response(Response.CODE_GENERIC_ERROR,
                                grantKey + " is not exist"));
                return;
            }
        }
        boolean writable = params.optBoolean(PARAM_WRITABLE, false);
        boolean result =
                ExchangeUtils
                        .grantPermission(context, pkg, grantPkg, grantSign, grantKey, writable);
        request.getCallback().callback(result ? Response.SUCCESS : Response.ERROR);
    }

    private void revokePermission(Request request) {
        SerializeObject params = null;
        try {
            params = request.getSerializeParams();
        } catch (SerializeException e) {
            Log.e(TAG, "revoke permission error", e);
        }
        if (params == null || params.length() == 0) {
            request.getCallback()
                    .callback(new Response(Response.CODE_ILLEGAL_ARGUMENT, "no params"));
            return;
        }
        String revokePkg = params.optString(PARAM_PKG);
        if (TextUtils.isEmpty(revokePkg)) {
            request.getCallback()
                    .callback(new Response(Response.CODE_ILLEGAL_ARGUMENT, "no package"));
            return;
        }
        String pkg = request.getApplicationContext().getPackage();
        String revokeKey = params.optString(PARAM_KEY);
        Context context = request.getNativeInterface().getActivity();
        boolean result = ExchangeUtils.revokePermission(context, pkg, revokePkg, revokeKey);
        request.getCallback().callback(result ? Response.SUCCESS : Response.ERROR);
    }

    @Override
    public String getName() {
        return FEATURE_NAME;
    }

    private static class ExecutorHolder {
        private static final ScheduledExecutor INSTANCE = Executors.createSingleThreadExecutor();
    }
}

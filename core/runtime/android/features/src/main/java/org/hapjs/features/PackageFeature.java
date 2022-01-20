/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.util.Log;
import org.hapjs.bridge.FeatureExtension;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.FeatureExtensionAnnotation;
import org.hapjs.common.signature.SignatureManager;
import org.hapjs.common.utils.DigestUtils;
import org.hapjs.common.utils.PackageUtils;
import org.hapjs.model.AppInfo;
import org.hapjs.pm.NativePackageProvider;
import org.hapjs.runtime.HapEngine;
import org.hapjs.runtime.ProviderManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@FeatureExtensionAnnotation(
        name = PackageFeature.FEATURE_NAME,
        actions = {
                @ActionAnnotation(
                        name = PackageFeature.ACTION_HAS_INSTALLED,
                        mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(name = PackageFeature.ACTION_INSTALL, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(name = PackageFeature.ACTION_GET_INFO, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(
                        name = PackageFeature.ACTION_GET_SIGNATURE_DIGESTS,
                        mode = FeatureExtension.Mode.ASYNC)
        })
public class PackageFeature extends FeatureExtension {
    protected static final String FEATURE_NAME = "system.package";
    protected static final String ACTION_HAS_INSTALLED = "hasInstalled";
    protected static final String ACTION_INSTALL = "install";
    protected static final String ACTION_GET_INFO = "getInfo";
    protected static final String ACTION_GET_SIGNATURE_DIGESTS = "getSignatureDigests";
    protected static final String PARAM_PACKAGE = "package";
    protected static final String RESULT_RESULT = "result";
    protected static final String RESULT_VERSION_CODE = "versionCode";
    protected static final String RESULT_VERSION_NAME = "versionName";
    protected static final String RESULT_SIGNATURE_DIGESTS = "signatureDigests";
    private static final String TAG = "PackageFeature";
    private static final int ERROR_CODE_BASE = Response.CODE_FEATURE_ERROR;
    private static final int ERROR_CODE_PACKAGE_NOT_FOUND = ERROR_CODE_BASE;

    @Override
    protected Response invokeInner(Request request) throws Exception {
        String action = request.getAction();
        Response response;
        if (ACTION_HAS_INSTALLED.equals(action)) {
            hasInstalled(request);
        } else if (ACTION_INSTALL.equals(action)) {
            install(request);
        } else if (ACTION_GET_INFO.equals(action)) {
            response = getInfo(request);
            request.getCallback().callback(response);
        } else if (ACTION_GET_SIGNATURE_DIGESTS.equals(action)) {
            response = getSignatureDigests(request);
            request.getCallback().callback(response);
        }
        return null;
    }

    protected void hasInstalled(Request request) throws JSONException {
        JSONObject params = request.getJSONParams();
        String pkg = params.optString(PARAM_PACKAGE);
        if (TextUtils.isEmpty(pkg)) {
            Response response =
                    new Response(Response.CODE_ILLEGAL_ARGUMENT, "package must not be empty");
            request.getCallback().callback(response);
            return;
        }

        Context context = request.getNativeInterface().getActivity();
        boolean installed = getPackageProvider().hasPackageInstalled(context, pkg);
        JSONObject data = new JSONObject();
        data.put(RESULT_RESULT, installed);
        request.getCallback().callback(new Response(data));
    }

    /**
     * Install an app. Developers should not rely on the success callback, but should check if app has
     * actually installed by {@link #hasInstalled} in success callback.
     */
    protected void install(Request request) throws JSONException {
        JSONObject params = request.getJSONParams();
        String pkg = params.optString(PARAM_PACKAGE);
        if (TextUtils.isEmpty(pkg)) {
            Response response =
                    new Response(Response.CODE_ILLEGAL_ARGUMENT, "package must not be empty");
            request.getCallback().callback(response);
            return;
        }

        Activity activity = request.getNativeInterface().getActivity();
        boolean result =
                getPackageProvider()
                        .installPackage(activity, pkg,
                                request.getApplicationContext().getPackage());
        JSONObject data = new JSONObject();
        data.put(RESULT_RESULT, result);
        request.getCallback().callback(new Response(data));
    }

    protected Response getInfo(Request request) throws JSONException {
        JSONObject params = request.getJSONParams();
        String pkg = params.optString(PARAM_PACKAGE);
        if (TextUtils.isEmpty(pkg)) {
            return new Response(Response.CODE_ILLEGAL_ARGUMENT, "package must not be empty");
        }

        JSONObject data = new JSONObject();
        Activity activity = request.getNativeInterface().getActivity();
        PackageInfo pi = PackageUtils.getPackageInfo(activity, pkg, 0);
        if (pi != null) {
            data.put(RESULT_VERSION_CODE, pi.versionCode);
            data.put(RESULT_VERSION_NAME, pi.versionName);
        } else {
            AppInfo appInfo = HapEngine.getInstance(pkg).getApplicationContext().getAppInfo();
            if (appInfo != null) {
                data.put(RESULT_VERSION_CODE, appInfo.getVersionCode());
                data.put(RESULT_VERSION_NAME, appInfo.getVersionName());
            }
        }
        if (data.length() > 0) {
            return new Response(data);
        }
        return new Response(ERROR_CODE_PACKAGE_NOT_FOUND, "package not found");
    }

    protected Response getSignatureDigests(Request request) throws JSONException {
        JSONObject params = request.getJSONParams();
        String pkg = params.optString(PARAM_PACKAGE);
        if (TextUtils.isEmpty(pkg)) {
            return new Response(Response.CODE_ILLEGAL_ARGUMENT, "package must not be empty");
        }

        JSONArray jsonArray = new JSONArray();
        Activity activity = request.getNativeInterface().getActivity();
        PackageInfo pi = PackageUtils.getPackageInfo(activity, pkg, PackageManager.GET_SIGNATURES);
        if (pi != null) {
            int length = pi.signatures.length;
            for (int i = 0; i < length; i++) {
                byte[] signatureBytes = pi.signatures[i].toByteArray();
                jsonArray.put(DigestUtils.getSha256(signatureBytes));
            }
        } else {
            try {
                String signature = SignatureManager.getSignature(activity, pkg);
                if (signature != null) {
                    jsonArray.put(signature);
                }
            } catch (IllegalStateException ignored) {
                Log.e(TAG, "get signature digests error", ignored);
            }
        }

        if (jsonArray.length() > 0) {
            JSONObject data = new JSONObject();
            data.put(RESULT_SIGNATURE_DIGESTS, jsonArray);
            return new Response(data);
        }
        return new Response(ERROR_CODE_PACKAGE_NOT_FOUND, "package not found");
    }

    @Override
    public String getName() {
        return FEATURE_NAME;
    }

    private NativePackageProvider getPackageProvider() {
        return ProviderManager.getDefault().getProvider(NativePackageProvider.NAME);
    }
}

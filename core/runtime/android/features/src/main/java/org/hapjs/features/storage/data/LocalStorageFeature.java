/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.storage.data;

import android.text.TextUtils;

import org.hapjs.bridge.FeatureExtension;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.FeatureExtensionAnnotation;
import org.hapjs.common.executors.Executors;
import org.hapjs.common.executors.ScheduledExecutor;
import org.hapjs.features.storage.data.internal.IStorage;
import org.hapjs.features.storage.data.internal.StorageFactory;
import org.json.JSONException;
import org.json.JSONObject;

@FeatureExtensionAnnotation(
        name = LocalStorageFeature.FEATURE_NAME,
        residentType = FeatureExtensionAnnotation.ResidentType.USEABLE,
        actions = {
                @ActionAnnotation(name = LocalStorageFeature.ACTION_SET, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(name = LocalStorageFeature.ACTION_GET, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(
                        name = LocalStorageFeature.ACTION_DELETE,
                        mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(
                        name = LocalStorageFeature.ACTION_CLEAR,
                        mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(name = LocalStorageFeature.ACTION_KEY, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(
                        name = LocalStorageFeature.ATTR_GET_LENGTH,
                        mode = FeatureExtension.Mode.SYNC,
                        type = FeatureExtension.Type.ATTRIBUTE,
                        access = FeatureExtension.Access.READ,
                        alias = LocalStorageFeature.ATTR_LENGTH_ALIAS),
                @ActionAnnotation(name = LocalStorageFeature.ACTION_SET_SYNC, mode = FeatureExtension.Mode.SYNC),
                @ActionAnnotation(name = LocalStorageFeature.ACTION_GET_SYNC, mode = FeatureExtension.Mode.SYNC),
                @ActionAnnotation(name = LocalStorageFeature.ACTION_DELETE_SYNC, mode = FeatureExtension.Mode.SYNC),
                @ActionAnnotation(name = LocalStorageFeature.ACTION_CLEAR_SYNC, mode = FeatureExtension.Mode.SYNC),
                @ActionAnnotation(name = LocalStorageFeature.ACTION_KEY_SYNC, mode = FeatureExtension.Mode.SYNC)
        })
public class LocalStorageFeature extends FeatureExtension {
    protected static final String FEATURE_NAME = "system.storage";
    protected static final String ACTION_SET = "set";
    protected static final String ACTION_GET = "get";
    protected static final String ACTION_DELETE = "delete";
    protected static final String ACTION_CLEAR = "clear";
    protected static final String ACTION_KEY = "key";
    protected static final String ACTION_SET_SYNC = "setSync";
    protected static final String ACTION_GET_SYNC = "getSync";
    protected static final String ACTION_DELETE_SYNC = "deleteSync";
    protected static final String ACTION_CLEAR_SYNC = "clearSync";
    protected static final String ACTION_KEY_SYNC = "keySync";


    protected static final String ATTR_LENGTH_ALIAS = "length";
    protected static final String ATTR_GET_LENGTH = "__getLength";

    private static final String PARAMS_KEY = "key";
    private static final String PARAMS_VALUE = "value";
    private static final String PARAMS_DEFAULT = "default";
    private static final String PARAMS_INDEX = "index";

    @Override
    public ScheduledExecutor getExecutor(Request request) {
        return ExecutorHolder.INSTANCE;
    }

    @Override
    protected Response invokeInner(Request request) throws Exception {
        String action = request.getAction();
        if (ACTION_SET.equals(action)) {
            invokeSet(request);
        } else if (ACTION_GET.equals(action)) {
            invokeGet(request);
        } else if (ACTION_DELETE.equals(action)) {
            invokeDelete(request);
        } else if (ACTION_CLEAR.equals(action)) {
            invokeClear(request);
        } else if (ACTION_KEY.equals(action)) {
            invokeKey(request);
        } else if (ATTR_GET_LENGTH.equals(action)) {
            return invokeGetLength(request);
        } else if (ACTION_SET_SYNC.equals(action)) {
            return invokeSetSync(request);
        } else if (ACTION_GET_SYNC.equals(action)) {
            return invokeGetSync(request);
        } else if (ACTION_DELETE_SYNC.equals(action)) {
            return invokeDeleteSync(request);
        } else if (ACTION_CLEAR_SYNC.equals(action)) {
            return invokeClearSync(request);
        } else if (ACTION_KEY_SYNC.equals(action)) {
            return invokeKeySync(request);
        }
        return Response.SUCCESS;
    }

    private void invokeGet(Request request) throws JSONException {
        Response response = invokeGetSync(request);
        request.getCallback().callback(response);
    }

    private Response invokeGetSync(Request request) throws JSONException {
        JSONObject jsonParams = request.getJSONParams();
        String key = jsonParams.optString(PARAMS_KEY);
        if (TextUtils.isEmpty(key)) {
            return new Response(Response.CODE_ILLEGAL_ARGUMENT, PARAMS_KEY + " not define");
        }
        String value = getStorage(request).get(key);
        if (value == null) {
            // default param may be a null value
            // so we should return empty string only when there's no default value
            if (jsonParams.has(PARAMS_DEFAULT)) {
                value = jsonParams.optString(PARAMS_DEFAULT, "");
            } else {
                value = "";
            }
        }
        return new Response(value);
    }

    private void invokeSet(Request request) throws JSONException {
        Response response = invokeSetSync(request);
        request.getCallback().callback(response);
    }

    private Response invokeSetSync(Request request) throws JSONException {
        JSONObject jsonParams = request.getJSONParams();
        String key = jsonParams.optString(PARAMS_KEY);
        if (TextUtils.isEmpty(key)) {
            return new Response(Response.CODE_ILLEGAL_ARGUMENT, PARAMS_KEY + " not define");
        }
        String value = jsonParams.optString(PARAMS_VALUE);
        return (getStorage(request).set(key, value) ? Response.SUCCESS : Response.ERROR);
    }

    private void invokeDelete(Request request) throws JSONException {
        Response response = invokeDeleteSync(request);
        request.getCallback().callback(response);
    }

    private Response invokeDeleteSync(Request request) throws JSONException {
        JSONObject jsonParams = request.getJSONParams();
        String key = jsonParams.optString(PARAMS_KEY);
        if (TextUtils.isEmpty(key)) {
            return new Response(Response.CODE_ILLEGAL_ARGUMENT, PARAMS_KEY + " not define");
        }
        if (getStorage(request).delete(key)) {
            return Response.SUCCESS;
        } else {
            return Response.ERROR;
        }
    }

    private void invokeClear(Request request) {
        Response response = invokeClearSync(request);
        request.getCallback().callback(response);
    }

    private Response invokeClearSync(Request request) {
        if (getStorage(request).clear()) {
            return Response.SUCCESS;
        } else {
            return Response.ERROR;
        }
    }

    private void invokeKey(Request request) throws JSONException {
        Response response = invokeKeySync(request);
        request.getCallback().callback(response);
    }

    private Response invokeKeySync(Request request) throws JSONException {
        JSONObject jsonParams = request.getJSONParams();
        int index = jsonParams.optInt(PARAMS_INDEX, -1);
        if (index == -1) {
            return new Response(Response.CODE_ILLEGAL_ARGUMENT, PARAMS_INDEX + " not define");
        }

        if (index < 0) {
            return new Response(Response.CODE_ILLEGAL_ARGUMENT, "index: " + index + " must >= 0");
        }

        String key = getStorage(request).key(index);
        if (key == null) {
            return new Response(Response.CODE_ILLEGAL_ARGUMENT, "index: " + index + " must < storage.length");
        }
        return new Response(key);
    }

    private Response invokeGetLength(Request request) {
        return new Response(getStorage(request).length());
    }

    @Override
    public String getName() {
        return FEATURE_NAME;
    }

    private IStorage getStorage(Request request) {
        return StorageFactory.getInstance().create(request.getApplicationContext());
    }

    private static class ExecutorHolder {
        private static final ScheduledExecutor INSTANCE = Executors.createSingleThreadExecutor();
    }
}

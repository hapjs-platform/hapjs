/*
 * Copyright (c) 2022-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.net.task;

import android.util.Log;

import org.hapjs.bridge.Extension;
import org.hapjs.bridge.FeatureExtension;
import org.hapjs.bridge.InstanceManager;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.FeatureExtensionAnnotation;
import org.hapjs.common.executors.Executors;
import org.hapjs.render.jsruntime.serialize.JavaSerializeObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@FeatureExtensionAnnotation(
        name = RequestTask.FEATURE_NAME,
        actions = {
                @ActionAnnotation(name = RequestTask.ACTION_REQUEST, mode = FeatureExtension.Mode.SYNC_CALLBACK),
                @ActionAnnotation(name = RequestTask.ACTION_ON_HEADERS_RECEIVED, mode = FeatureExtension.Mode.CALLBACK, multiple = Extension.Multiple.MULTI),
                @ActionAnnotation(name = RequestTask.ACTION_OFF_HEADERS_RECEIVED, mode = FeatureExtension.Mode.SYNC, multiple = Extension.Multiple.MULTI),
                @ActionAnnotation(name = RequestTask.ACTION_ABORT, mode = FeatureExtension.Mode.ASYNC)
        }
)

public class RequestTask extends FeatureExtension {
    public static final String TAG = "RequestTask";

    protected static final String FEATURE_NAME = "system.requesttask";
    protected static final String ACTION_REQUEST = "request";
    protected static final String ACTION_ON_HEADERS_RECEIVED = "onHeadersReceived";
    protected static final String ACTION_OFF_HEADERS_RECEIVED = "offHeadersReceived";
    protected static final String ACTION_ABORT = "abort";

    public static final String PARAMS_KEY_URL = "url";
    public static final String PARAMS_KEY_DATA = "data";
    public static final String PARAMS_KEY_HEADER = "header";
    public static final String PARAMS_KEY_METHOD = "method";
    public static final String PARAMS_KEY_DATA_TYPE = "dataType";
    public static final String PARAMS_KEY_RESPOSNE_TYPE = "responseType";

    public static final String RESULT_KEY_STATUS_CODE = "statusCode";
    public static final String RESULT_KEY_DATA = "data";
    public static final String RESULT_KEY_HEADER = "header";

    public static final String METHOD_OPTIONS = "OPTIONS";
    public static final String METHOD_GET = "GET";
    public static final String METHOD_HEAD = "HEAD";
    public static final String METHOD_POST = "POST";
    public static final String METHOD_PUT = "PUT";
    public static final String METHOD_DELETE = "DELETE";
    public static final String METHOD_TRACE = "TRACE";
    public static final String METHOD_CONNECT = "CONNECT";

    public static final String DATA_TYPE_JSON = "json";
    public static final String RESPONSE_TYPE_TEXT = "text";
    public static final String RESPONSE_TYPE_ARRAYBUFFER = "arraybuffer";

    public static final Set<String> SUPPORT_METHODS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    METHOD_OPTIONS,
                    METHOD_GET,
                    METHOD_HEAD,
                    METHOD_POST,
                    METHOD_PUT,
                    METHOD_DELETE,
                    METHOD_TRACE,
                    METHOD_CONNECT))
    );

    @Override
    protected Response invokeInner(Request request) throws Exception {
        String action = request.getAction();
        if (ACTION_REQUEST.equals(action)) {
            final RequestTaskImpl newTask = new RequestTaskImpl(request);
            JavaSerializeObject instance = InstanceManager.getInstance().createInstance(request.getView().getHybridManager(), newTask);
            Executors.io().execute(new Runnable() {
                @Override
                public void run() {
                    newTask.execute();
                }
            });
            return new Response(instance);
        } else {
            int instanceId = request.getInstanceId();
            RequestTaskImpl task = InstanceManager.getInstance().getInstance(instanceId);
            if (task == null) {
                Log.i(TAG, "task is null");
                if (request.getCallback() != null) {
                    request.getCallback().callback(new Response(Response.CODE_GENERIC_ERROR, "no such task instance"));
                }
                return Response.ERROR;
            }
            if (ACTION_ON_HEADERS_RECEIVED.equals(action)) {
                task.onHeadersReceived(request);
            } else if (ACTION_OFF_HEADERS_RECEIVED.equals(action)) {
                return task.offHeadersReceived(request);
            } else if (ACTION_ABORT.equals(action)) {
                task.abort();
            } else {
                Log.d(TAG, "unsupport action");
                return Response.ERROR;
            }
            return Response.SUCCESS;
        }
    }

    @Override
    public String getName() {
        return FEATURE_NAME;
    }
}

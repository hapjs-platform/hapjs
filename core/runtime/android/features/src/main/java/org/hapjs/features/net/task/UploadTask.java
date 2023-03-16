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

@FeatureExtensionAnnotation(
        name = UploadTask.FEATURE_NAME,
        actions = {
                @ActionAnnotation(name = UploadTask.ACTION_UPLOAD_FILE, mode = FeatureExtension.Mode.SYNC_CALLBACK),
                @ActionAnnotation(name = UploadTask.ACTION_ON_PROGRESS_UPDATE, mode = FeatureExtension.Mode.CALLBACK, multiple = Extension.Multiple.MULTI),
                @ActionAnnotation(name = UploadTask.ACTION_OFF_PROGRESS_UPDATE, mode = FeatureExtension.Mode.SYNC, multiple = Extension.Multiple.MULTI),
                @ActionAnnotation(name = UploadTask.ACTION_ON_HEADERS_RECEIVED, mode = FeatureExtension.Mode.CALLBACK, multiple = Extension.Multiple.MULTI),
                @ActionAnnotation(name = UploadTask.ACTION_OFF_HEADERS_RECEIVED, mode = FeatureExtension.Mode.SYNC, multiple = Extension.Multiple.MULTI),
                @ActionAnnotation(name = UploadTask.ACTION_ABORT, mode = FeatureExtension.Mode.ASYNC)
        }
)

public class UploadTask extends FeatureExtension {
    public static final String TAG = "UploadTask";

    protected static final String FEATURE_NAME = "system.uploadtask";
    protected static final String ACTION_UPLOAD_FILE = "uploadFile";
    protected static final String ACTION_ON_PROGRESS_UPDATE = "onProgressUpdate";
    protected static final String ACTION_OFF_PROGRESS_UPDATE = "offProgressUpdate";
    protected static final String ACTION_ON_HEADERS_RECEIVED = "onHeadersReceived";
    protected static final String ACTION_OFF_HEADERS_RECEIVED = "offHeadersReceived";
    protected static final String ACTION_ABORT = "abort";

    // needed
    public static final String PARAMS_KEY_URL = "url";
    public static final String PARAMS_KEY_FILE_PATH = "filePath";
    public static final String PARAMS_KEY_NAME = "name";
    // option
    public static final String PARAMS_KEY_HEADER = "header";
    public static final String PARAMS_KEY_FORM_DATA = "formData";

    public static final String RESULT_KEY_PROGRESS = "progress";
    public static final String RESULT_KEY_TOTAL_BYTES_SENT = "totalBytesSent";
    public static final String RESULT_KEY_TOTAL_BYTES_EXPECTED_TO_SEND = "totalBytesExpectedToSend";

    @Override
    protected Response invokeInner(Request request) throws Exception {
        String action = request.getAction();
        if (ACTION_UPLOAD_FILE.equals(action)) {
            final UploadTaskImpl newTask = new UploadTaskImpl(request);
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
            UploadTaskImpl task = InstanceManager.getInstance().getInstance(instanceId);
            if (task == null) {
                Log.i(TAG, "task is null");
                if (request.getCallback() != null) {
                    request.getCallback().callback(new Response(Response.CODE_GENERIC_ERROR, "no such task instance"));
                }
                return Response.ERROR;
            }
            if (ACTION_ON_PROGRESS_UPDATE.equals(action)) {
                task.onProgressUpdate(request);
            } else if (ACTION_OFF_PROGRESS_UPDATE.equals(action)) {
                return task.offProgressUpdate(request);
            } else if (ACTION_ON_HEADERS_RECEIVED.equals(action)) {
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

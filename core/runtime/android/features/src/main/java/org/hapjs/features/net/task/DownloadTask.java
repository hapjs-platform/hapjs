/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hapjs.features.net.task;

import android.util.Log;
import android.webkit.URLUtil;

import org.hapjs.bridge.Extension;
import org.hapjs.bridge.FeatureExtension;
import org.hapjs.bridge.HybridManager;
import org.hapjs.bridge.InstanceManager;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.FeatureExtensionAnnotation;
import org.hapjs.common.executors.Executors;
import org.hapjs.features.net.RequestHelper;
import org.hapjs.render.jsruntime.serialize.SerializeException;
import org.hapjs.render.jsruntime.serialize.SerializeObject;

import okhttp3.Headers;

@FeatureExtensionAnnotation(
        name = DownloadTask.FEATURE_NAME,
        actions = {
                @ActionAnnotation(name = DownloadTask.ACTION_DOWNLOAD, mode = FeatureExtension.Mode.SYNC_CALLBACK),
                @ActionAnnotation(name = DownloadTask.ACTION_ABORT, mode = FeatureExtension.Mode.SYNC),
                @ActionAnnotation(name = DownloadTask.EVENT_ON_PROGRESS_UPDATE, mode = FeatureExtension.Mode.CALLBACK, type = FeatureExtension.Type.FUNCTION, multiple = Extension.Multiple.MULTI),
                @ActionAnnotation(name = DownloadTask.EVENT_ON_HEADERS_RECEIVED, mode = FeatureExtension.Mode.CALLBACK, type = FeatureExtension.Type.FUNCTION, multiple = Extension.Multiple.MULTI),
                @ActionAnnotation(name = DownloadTask.EVENT_OFF_PROGRESS_UPDATE, mode = FeatureExtension.Mode.SYNC, type = FeatureExtension.Type.FUNCTION, multiple = Extension.Multiple.MULTI),
                @ActionAnnotation(name = DownloadTask.EVENT_OFF_HEADERS_RECEIVED, mode = FeatureExtension.Mode.SYNC, type = FeatureExtension.Type.FUNCTION, multiple = Extension.Multiple.MULTI),
        }
)
public class DownloadTask extends FeatureExtension {
    private static final String TAG = "DownloadTask";
    protected static final String FEATURE_NAME = "system.downloadtask";

    public static final String ACTION_DOWNLOAD = "downloadFile";
    public static final String ACTION_ABORT = "abort";

    public static final String EVENT_ON_PROGRESS_UPDATE = "onProgressUpdate";
    public static final String EVENT_ON_HEADERS_RECEIVED = "onHeadersReceived";
    public static final String EVENT_OFF_PROGRESS_UPDATE = "offProgressUpdate";
    public static final String EVENT_OFF_HEADERS_RECEIVED = "offHeadersReceived";

    public static final String PARAMS_KEY_URL = "url";
    public static final String PARAMS_KEY_HEADER = "header";
    public static final String PARAMS_KEY_FILE_PATH = "filePath";
    public static final String PARAMS_KEY_TIMEOUT = "timeout";

    @Override
    public String getName() {
        return FEATURE_NAME;
    }

    @Override
    protected Response invokeInner(Request request) throws Exception {
        String action = request.getAction();
        if (ACTION_DOWNLOAD.equals(action)) {
            return createDownloadTask(request);
        } else {
            switch (action) {
                case ACTION_ABORT:
                    return abort(request);
                case EVENT_ON_HEADERS_RECEIVED:
                case EVENT_ON_PROGRESS_UPDATE:
                case EVENT_OFF_HEADERS_RECEIVED:
                case EVENT_OFF_PROGRESS_UPDATE:
                    handleEventRequest(request);
                    break;
                default:
                    Log.d(TAG, "unsupport action");
                    return Response.ERROR;
            }
        }
        return Response.SUCCESS;
    }

    protected Response createDownloadTask(Request request) throws SerializeException {
        String pkg = request.getApplicationContext().getPackage();
        SerializeObject params = request.getSerializeParams();
        String url = params.getString(PARAMS_KEY_URL);
        if (!URLUtil.isHttpsUrl(url) && !URLUtil.isHttpUrl(url)) {
            request.getCallback().callback(new Response(Response.CODE_ILLEGAL_ARGUMENT, "invalid url."));
            return null;
        }
        SerializeObject jsonHeader = params.optSerializeObject(PARAMS_KEY_HEADER);
        Headers headers = RequestHelper.getHeaders(jsonHeader);
        String filePath = params.optString(PARAMS_KEY_FILE_PATH);
        long timeoutMillis = params.optLong(PARAMS_KEY_TIMEOUT, 0L);

        DownloadTaskImpl task = new DownloadTaskImpl(pkg, url, headers, filePath, timeoutMillis);
        task.subscribe(request);
        Executors.io().execute(new Runnable() {
            @Override
            public void run() {
                task.run();
            }
        });
        HybridManager hybridManager = request.getView().getHybridManager();
        return new Response(InstanceManager.getInstance().createInstance(hybridManager, task).toJSONObject());
    }

    private Response abort(Request request) {
        DownloadTaskImpl task = InstanceManager.getInstance().getInstance(request.getInstanceId());
        if (task != null) {
            task.abort();
            return Response.SUCCESS;
        }
        return new Response(Response.CODE_GENERIC_ERROR, "no such task instance");
    }

    private void handleEventRequest(Request request) {
        DownloadTaskImpl task = InstanceManager.getInstance().getInstance(request.getInstanceId());
        if (task != null) {
            task.subscribe(request);
        } else {
            request.getCallback().callback(new Response(Response.CODE_GENERIC_ERROR, "no such task instance"));
        }
    }
}

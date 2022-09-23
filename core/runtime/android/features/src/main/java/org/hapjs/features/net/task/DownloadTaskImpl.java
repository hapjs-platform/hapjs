/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.net.task;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.URLUtil;

import org.hapjs.bridge.ExtensionManager;
import org.hapjs.bridge.InstanceManager;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.storage.file.InternalUriUtils;
import org.hapjs.bridge.storage.file.Resource;
import org.hapjs.common.net.HttpConfig;
import org.hapjs.common.utils.FileHelper;
import org.hapjs.common.utils.FileUtils;
import org.hapjs.features.net.CallbackWrapper;
import org.hapjs.features.net.RequestHelper;
import org.hapjs.model.NetworkConfig;
import org.hapjs.render.jsruntime.serialize.JavaSerializeObject;
import org.hapjs.render.jsruntime.serialize.SerializeObject;
import org.hapjs.runtime.HapEngine;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import okio.ForwardingSource;
import okio.Okio;
import okio.Source;

import static org.hapjs.features.net.task.DownloadTask.FEATURE_NAME;

public class DownloadTaskImpl implements InstanceManager.IInstance {
    private static final String TAG = "DownloadTask";

    private static final String RESULT_KEY_HEADER = "header";
    private static final String RESULT_KEY_PROGRESS = "progress";
    private static final String RESULT_KEY_RECEIVED_SIZE = "totalBytesWritten";
    private static final String RESULT_KEY_TOTAL_SIZE = "totalBytesExpectedToWrite";

    private static final String RESULT_KEY_STATUS_CODE = "statusCode";
    private static final String RESULT_KEY_FILE_PATH = "filePath";
    private static final String RESULT_KEY_RESP_HEADER = "header";

    protected static long DEFAULT_TIMEOUT_MILLIS = 10 * 1000;

    protected String mPackage;
    private final String mUrl;
    private final Headers mHeaders;
    protected Headers mRespHeaders;
    protected File mFile;
    private Call mCall;
    private Request mDownloadRequest;
    private long mTimeoutMillis = DEFAULT_TIMEOUT_MILLIS;
    protected Map<String, ArrayList<CallbackWrapper>> mCallbackMap = new ConcurrentHashMap<>();
    protected String mFilePath;

    public DownloadTaskImpl(String pkg, String url, Headers headers, String filePath, Long timeout) {
        this.mPackage = pkg;
        this.mUrl = url;
        this.mHeaders = headers;
        this.mFilePath = filePath;
        if (timeout > 0) {
            this.mTimeoutMillis = timeout;
        }
    }

    @Override
    public void release() {
        mCall = null;
        mFile = null;
        mDownloadRequest = null;
    }

    @Override
    public String getFeatureName() {
        return FEATURE_NAME;
    }

    /**
     * mFilePath must be internal path.
     */
    public void run() {
        // check filePath if filePath is not null or empty.
        if (TextUtils.isEmpty(mFilePath)) {
            try {
                this.mFile = FileHelper.generateAvailableFile(Uri.parse(mUrl).getLastPathSegment(), getCacheDir());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            if (!InternalUriUtils.isWritableInternalUri(mFilePath)) {
                onError(Response.CODE_ILLEGAL_ARGUMENT, "File path must be internal uri.");
                return;
            }

            File dstFile = mDownloadRequest.getApplicationContext().getUnderlyingFile(mFilePath);
            if (!checkFileName(dstFile.getName())) {
                onError(Response.CODE_ILLEGAL_ARGUMENT, "File path must end with filename.");
                return;
            }

            mFile = dstFile;
        }

        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(mUrl)
                .headers(mHeaders)
                .build();
        JSONObject networkConfig = new JSONObject();
        try {
            networkConfig.put(String.valueOf(NetworkConfig.CONNECT_TIMEOUT), mTimeoutMillis);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        HttpConfig.get().onConfigChange(NetworkConfig.parse(networkConfig));
        mCall = HttpConfig.get().getOkHttpClient().newCall(request);
        mCall.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "onFailure: ", e);
                onError(Response.CODE_IO_ERROR, e.getMessage());
            }

            @Override
            public void onResponse(Call call, okhttp3.Response response) throws IOException {
                onHeadersReceived(response.headers());
                ResponseBody proxyResponseBody = new ProgressResponseBody(response.body());
                response = response.newBuilder().body(proxyResponseBody).build();
                if (response.isSuccessful()) {
                    File file = mFile;
                    if (file == null) {
                        String fileName = URLUtil.guessFileName(mUrl,
                                response.header(RequestHelper.CONTENT_DISPOSITION),
                                null);
                        File dir = HapEngine.getInstance(mPackage).getApplicationContext().getCacheDir();
                        file = FileHelper.generateAvailableFile(fileName, dir);
                    }
                    if (file == null || !FileUtils.saveToFile(response.body().byteStream(), file)) {
                        throw new IOException("save file error");
                    }
                    onComplete(response.code(), "", file);
                } else {
                    onComplete(response.code(), response.message(), null);
                }
            }
        });
    }


    protected File getCacheDir() {
        return HapEngine.getInstance(mPackage).getApplicationContext().getCacheDir();
    }

    private boolean checkFileName(String fileName) {
        if (!TextUtils.isEmpty(fileName)) {
            // Android文件名最大长度：255字节.
            if (fileName.getBytes(Charset.defaultCharset()).length > 255) {
                return false;
            }
            return fileName.matches("^[A-Za-z0-9_]+(\\.[A-Za-z0-9]{1,5})$");
        }
        return true;
    }

    public void abort() {
        if (mCall != null && !mCall.isCanceled()) {
            mCall.cancel();
        }
        onAbort();
        release();
    }

    protected void onAbort() {
    }

    public void subscribe(Request request) {
        String action = request.getAction();
        switch (action) {
            case DownloadTask.ACTION_DOWNLOAD:
                mDownloadRequest = request;
                break;
            case DownloadTask.EVENT_ON_HEADERS_RECEIVED:
            case DownloadTask.EVENT_ON_PROGRESS_UPDATE:
                addListener(request);
                break;
            case DownloadTask.EVENT_OFF_HEADERS_RECEIVED:
            case DownloadTask.EVENT_OFF_PROGRESS_UPDATE:
                removeListener(request);
                break;
        }
    }

    protected void onHeadersReceived(Headers headers) {
        mRespHeaders = headers;
        ArrayList<CallbackWrapper> actionCallbacks = mCallbackMap.get(DownloadTask.EVENT_ON_HEADERS_RECEIVED);
        SerializeObject result = new JavaSerializeObject();
        result.put(RESULT_KEY_HEADER, RequestHelper.parseHeaders(headers));
        Response response = new Response(result);
        if (actionCallbacks != null && actionCallbacks.size() > 0) {
            for (CallbackWrapper wrapper : actionCallbacks) {
                wrapper.callback(response);
            }
        }
    }

    protected void onProgressUpdate(long receivedSize, long totalSize) {
        ArrayList<CallbackWrapper> actionCallbacks = mCallbackMap.get(DownloadTask.EVENT_ON_PROGRESS_UPDATE);

        JSONObject json = new JSONObject();
        double progress = (totalSize == 0 || totalSize == -1)
                ? 0
                : (receivedSize * 1.0 / totalSize) * 100;
        try {
            json.put(RESULT_KEY_PROGRESS, progress);
            json.put(RESULT_KEY_RECEIVED_SIZE, receivedSize);
            json.put(RESULT_KEY_TOTAL_SIZE, totalSize);
        } catch (JSONException e) {
            Log.e(TAG, "onProgressUpdate", e);
        }

        if (actionCallbacks != null && actionCallbacks.size() > 0) {
            for (CallbackWrapper wrapper : actionCallbacks) {
                wrapper.callback(new Response(json));
            }
        }
    }

    private void onSuccess(int statusCode, File file, SerializeObject header) {
        if (file != null) {
            if (mDownloadRequest != null) {
                SerializeObject result = new JavaSerializeObject();
                result.put(RESULT_KEY_STATUS_CODE, statusCode);
                result.put(RESULT_KEY_FILE_PATH, toInternalUri(file));
                result.put(RESULT_KEY_RESP_HEADER, header);
                if (mDownloadRequest.getCallback() != null) {
                    mDownloadRequest.getCallback().callback(new Response(result));
                }
            }
        } else {
            if (mDownloadRequest.getCallback() != null) {
                mDownloadRequest.getCallback().callback(new Response(Response.CODE_IO_ERROR, ""));
            }
        }

        release();
    }

    private String toInternalUri(File file) {
        Resource resource = HapEngine.getInstance(mPackage).getApplicationContext().getResourceFactory().create(file);
        return resource.toUri();
    }

    private void onFail(int code, String msg) {
        if (mDownloadRequest == null) return;
        if (mDownloadRequest.getCallback() != null) {
            mDownloadRequest.getCallback().callback(new Response(code, msg));
        }

        release();
    }

    protected void onComplete(int code, String errorMsg, File file) {
        if (file != null) {
            onSuccess(code, file, RequestHelper.parseHeaders(mRespHeaders));
        } else {
            onFail(code, errorMsg);
        }
        release();
    }

    protected void onError(int code, String message) {
        onFail(code, message);
    }

    public void addListener(Request request) {
        String jsCallback = request.getJsCallback();
        if (ExtensionManager.isValidCallback(jsCallback)) {
            String action = request.getAction();
            org.hapjs.bridge.Callback callback = request.getCallback();
            ArrayList<CallbackWrapper> actionCallbacks = mCallbackMap.get(action);
            if (actionCallbacks == null) {
                actionCallbacks = new ArrayList<>();
                actionCallbacks.add(new CallbackWrapper(callback, jsCallback));
                mCallbackMap.put(action, actionCallbacks);
            } else {
                actionCallbacks.add(new CallbackWrapper(callback, jsCallback));
            }
        }
    }

    private void removeListener(Request request) {
        String action = request.getAction();
        String removeAction = "";
        switch (action) {
            case DownloadTask.EVENT_OFF_HEADERS_RECEIVED:
                removeAction = DownloadTask.EVENT_ON_HEADERS_RECEIVED;
                break;
            case DownloadTask.EVENT_OFF_PROGRESS_UPDATE:
                removeAction = DownloadTask.EVENT_ON_PROGRESS_UPDATE;
                break;
        }

        String jsCallback = request.getJsCallback();
        //When it is invalid, This type of event needs to be removed
        if (ExtensionManager.isValidCallback(jsCallback)) {
            ArrayList<CallbackWrapper> actionCallbacks = mCallbackMap.get(removeAction);
            if (actionCallbacks != null) {
                for (CallbackWrapper wrapper : actionCallbacks) {
                    if (wrapper.getJsCallbackId().equals(jsCallback)) {
                        actionCallbacks.remove(wrapper);
                        break;
                    }
                }
            }
        } else {
            mCallbackMap.remove(removeAction);
        }
    }

    public class ProgressResponseBody extends ResponseBody {
        private ResponseBody mResponseBody;
        private BufferedSource mSource;

        public ProgressResponseBody(ResponseBody responseBody) {
            this.mResponseBody = responseBody;
        }

        @Override
        public MediaType contentType() {
            return mResponseBody.contentType();
        }

        @Override
        public long contentLength() {
            return mResponseBody.contentLength();
        }

        @Override
        public BufferedSource source() {
            if (mSource == null) {
                mSource = Okio.buffer(source(mResponseBody.source()));
            }
            return mSource;
        }

        private Source source(Source source) {
            final long contentLength = contentLength();
            return new ForwardingSource(source) {
                long totalBytesRead = 0L;

                @Override
                public long read(Buffer sink, long byteCount) throws IOException {
                    long bytesRead = super.read(sink, byteCount);
                    if (bytesRead > 0) {
                        totalBytesRead += bytesRead;
                        onProgressUpdate(totalBytesRead, contentLength);
                    }
                    return bytesRead;
                }
            };
        }
    }
}

/*
 * Copyright (c) 2022-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.net.task;

import static org.hapjs.bridge.AbstractExtension.getExceptionResponse;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import org.hapjs.bridge.ExtensionManager;
import org.hapjs.bridge.InstanceManager;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.common.net.HttpConfig;
import org.hapjs.common.net.NetworkReportManager;
import org.hapjs.features.net.CallbackWrapper;
import org.hapjs.features.net.RequestHelper;
import org.hapjs.render.jsruntime.serialize.SerializeArray;
import org.hapjs.render.jsruntime.serialize.SerializeException;
import org.hapjs.render.jsruntime.serialize.SerializeHelper;
import org.hapjs.render.jsruntime.serialize.SerializeObject;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import okhttp3.Call;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.internal.Util;
import okio.Buffer;
import okio.BufferedSink;
import okio.ForwardingSink;
import okio.Okio;
import okio.Sink;
import okio.Source;

public class UploadTaskImpl implements InstanceManager.IInstance {
    public static final String TAG = "UploadTaskImpl";

    private Request mRequest;
    private volatile boolean isAbort = false;
    private List<CallbackWrapper> progressListenerList = new ArrayList<>();
    private List<CallbackWrapper> headerReceivedList = new ArrayList<>();
    private Call mCall;

    private String mUrl;
    private String mFilePath;
    private String mName;
    private SerializeObject mHeader;
    private SerializeObject mFormData;

    private static final Headers EMPTY_HEADERS = new Headers.Builder().build();
    protected static final String METHOD_POST = "POST";
    private static final String KEY_NETWORK_REPORT_SOURCE = "UploadTask";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String CONTENT_TYPE_TEXT_PLAIN = "text/plain";
    private static final String CONTENT_TYPE_MULTIPART_FORM_DATA = "multipart/form-data";

    public UploadTaskImpl(Request request) {
        this.mRequest = request;
    }

    public void abort() {
        Log.d(TAG, "abort mCall is null?" + (mCall == null));
        isAbort = true;
        if (mCall != null) {
            mCall.cancel();
        }
    }

    public void onProgressUpdate(Request request) {
        progressListenerList.add(new CallbackWrapper(request.getCallback(), request.getJsCallback()));
    }

    public Response offProgressUpdate(Request request) {
        String jsCallback = request.getJsCallback();
        if (ExtensionManager.isValidCallback(jsCallback)) {
            for (CallbackWrapper callbackWrapper : progressListenerList) {
                if (jsCallback.equals(callbackWrapper.getJsCallbackId())) {
                    progressListenerList.remove(callbackWrapper);
                    break;
                }
            }
        } else {
            progressListenerList.clear();
        }
        return Response.SUCCESS;
    }

    public void onHeadersReceived(Request request) {
        headerReceivedList.add(new CallbackWrapper(request.getCallback(), request.getJsCallback()));
    }

    public Response offHeadersReceived(Request request) {
        String jsCallback = request.getJsCallback();
        if (ExtensionManager.isValidCallback(jsCallback)) {
            for (CallbackWrapper callbackWrapper : headerReceivedList) {
                if (jsCallback.equals(callbackWrapper.getJsCallbackId())) {
                    headerReceivedList.remove(callbackWrapper);
                    break;
                }
            }
        } else {
            headerReceivedList.clear();
        }
        return Response.SUCCESS;
    }

    public void execute() {
        Log.d(TAG, "UploadTask execute");
        try {
            SerializeObject reader = mRequest.getSerializeParams();
            mUrl = reader.getString(UploadTask.PARAMS_KEY_URL);
            mFilePath = reader.optString(UploadTask.PARAMS_KEY_FILE_PATH);
            mName = reader.optString(UploadTask.PARAMS_KEY_NAME);
            mHeader = reader.optSerializeObject(UploadTask.PARAMS_KEY_HEADER);
            mFormData = (SerializeObject) reader.opt(UploadTask.PARAMS_KEY_FORM_DATA);

            if (TextUtils.isEmpty(mUrl) || TextUtils.isEmpty(mFilePath) || TextUtils.isEmpty(mName)) {
                mRequest.getCallback().callback(new Response(Response.CODE_GENERIC_ERROR, "lack of argument."));
                return;
            }

            UploadFile uploadfile = getUploadFile();
            okhttp3.Request httpRequest = getPostRequest(mFormData, uploadfile);

            NetworkReportManager.getInstance().reportNetwork(KEY_NETWORK_REPORT_SOURCE, mUrl);

            OkHttpClient okHttpClient = HttpConfig.get().getOkHttpClient();
            mCall = okHttpClient.newCall(httpRequest);
            mCall.enqueue(new UploadCallbackImpl(mRequest, "null", new UploadCallbackImpl.HeaderReceivedListener() {
                @Override
                public void onHeaderReceived(SerializeObject serializeObject) {
                    for (CallbackWrapper callbackWrapper : headerReceivedList) {
                        callbackWrapper.callback(new Response(serializeObject));
                    }
                }
            }));
            if (isAbort) {
                Log.d(TAG, "UploadTask.execute() abort upload task");
                mCall.cancel();
            }
        } catch (FileNotFoundException e) {
            mRequest.getCallback().callback(new Response(Response.CODE_GENERIC_ERROR, "File not found."));
        } catch (Exception e) {
            mRequest.getCallback().callback(getExceptionResponse(mRequest, e));
        }
    }

    @Override
    public void release() {
        abort();
    }

    @Override
    public String getFeatureName() {
        return UploadTask.FEATURE_NAME;
    }

    private static class UploadFile {
        String formName;
        String fileName;
        Uri uri;
        Context context;

        public UploadFile(String formName, String fileName, Uri uri, MediaType mediaType, Context context) {
            this.formName = formName;
            this.fileName = fileName;
            this.uri = uri;
            this.context = context;
        }
    }

    private UploadFile getUploadFile()
            throws FileNotFoundException {
        try {
            Context context = mRequest.getNativeInterface().getActivity().getApplicationContext();

            Uri uri = mRequest.getApplicationContext().getUnderlyingUri(mFilePath);
            if (uri == null) {
                throw new FileNotFoundException("uri does not exist: " + uri);
            }

            MediaType mediaType = RequestHelper.getMimeType(TextUtils.isEmpty(mFilePath) ? uri.getLastPathSegment() : mName);

            return new UploadFile("file", mName, uri, mediaType, context);
        } catch (Exception e) {
            throw e;
        }
    }

    private okhttp3.Request getPostRequest(Object dataObj, UploadFile uploadFile)
            throws SerializeException {
        Log.d(TAG, "UploadTask getPostRequest");
        Headers headers = getHeaders(mHeader);
        return new okhttp3.Request.Builder()
                .url(mUrl)
                .method(METHOD_POST, getFilePostBody(dataObj, headers, uploadFile))
                .headers(headers)
                .build();
    }

    private Headers getHeaders(SerializeObject jsonHeader) throws SerializeException {
        Log.d(TAG, "UploadTask getHeaders");
        if (jsonHeader == null) {
            Log.d(TAG, "UploadTask getHeaders no Header");
            return EMPTY_HEADERS;
        } else {
            Headers.Builder builder = new Headers.Builder();
            for (String key : jsonHeader.keySet()) {
                Object value = jsonHeader.opt(key);
                if (value instanceof SerializeArray) {
                    SerializeArray values = (SerializeArray) value;
                    for (int i = 0; i < values.length(); ++i) {
                        builder.add(key, values.getString(i));
                    }
                } else {
                    builder.add(key, SerializeHelper.toString(value, ""));
                }
            }
            return builder.build();
        }
    }

    private RequestBody getFilePostBody(Object objData, Headers headers, UploadFile uploadFile)
            throws SerializeException {
        Log.d(TAG, "UploadTask getFilePostBody");
        MultipartBody.Builder builder = new MultipartBody.Builder();
        builder.setType(MultipartBody.FORM);
        if (objData != null) {
            if (objData instanceof SerializeObject) {
                Log.d(TAG, "UploadTask getFilePostBody objectData instanceOf SerializeObject");
                SerializeObject params = (SerializeObject) objData;
                Set<String> keys = params.keySet();
                for (String key : keys) {
                    builder.addFormDataPart(key, params.getString(key));
                }
            } else {
                Log.d(TAG, "UploadTask getFilePostBody as String");
                String contentType = headers.get(CONTENT_TYPE);
                contentType = TextUtils.isEmpty(contentType) ? CONTENT_TYPE_TEXT_PLAIN : contentType;
                builder.addPart(RequestBody.create(MediaType.parse(contentType), objData.toString()));
            }
        }

        builder.addFormDataPart(uploadFile.formName, uploadFile.fileName,
                new FileRequestBody(uploadFile.uri, uploadFile.context, new ProgressListener() {
                    @Override
                    public void onProgressUpdate(UploadProgress progress) {
                        onTaskUpdate(progress);
                    }
                }));
        return builder.build();
    }

    private class FileRequestBody extends RequestBody {

        private ProgressListener mListener;
        private MediaType mediaType;
        private InputStream inputStream;
        Uri uri;
        Context context;
        long contentSize;

        private FileRequestBody(Uri uri, Context context, ProgressListener listener) {
            this.uri = uri;
            this.mediaType = MediaType.parse(CONTENT_TYPE_MULTIPART_FORM_DATA);
            this.context = context;
            this.mListener = listener;
            try {
                inputStream = context.getContentResolver().openInputStream(uri);
                this.contentSize = inputStream.available();
            } catch (IOException e) {
                this.contentSize = -1L;
            }
        }

        @Override
        public MediaType contentType() {
            return mediaType;
        }

        @Override
        public long contentLength() {
            return contentSize;
        }

        @Override
        public void writeTo(BufferedSink sink) throws IOException {
            CountingSink countingSink = new CountingSink(sink);
            BufferedSink bufferedSink = Okio.buffer(countingSink);
            Source source = null;
            try {
                if (inputStream == null) {
                    inputStream = context.getContentResolver().openInputStream(uri);
                }
                source = Okio.source(inputStream);
                bufferedSink.writeAll(source);
            } finally {
                Util.closeQuietly(source);
                if (inputStream != null) {
                    inputStream.close();
                }
                inputStream = null;
                bufferedSink.flush();
            }
        }

        /**
         * Sink相当于OutputStream，在这里记录写入的数量，用于计算上传的进度
         */
        private final class CountingSink extends ForwardingSink {

            private long bytesWritten = 0;

            public CountingSink(Sink delegate) {
                super(delegate);
            }

            @Override
            public void write(Buffer source, long byteCount) throws IOException {
                super.write(source, byteCount);
                bytesWritten += byteCount;
                if (mListener != null) {
                    mListener.onProgressUpdate(new UploadProgress(bytesWritten, contentSize));
                }
            }
        }
    }

    private void onTaskUpdate(UploadProgress progress) {
        if (progress == null) {
            Log.d(TAG, "UploadTaskCallbackContext progress is empty.");
            return;
        }
        // filter error number
        if (progress.totalBytesSent <= 0 ||
                progress.totalBytesExpectedToSent <= 0 ||
                progress.totalBytesSent > progress.totalBytesExpectedToSent) {
            Log.d(TAG, "UploadTaskCallbackContext params error.");
        }

        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(UploadTask.RESULT_KEY_PROGRESS, (progress.totalBytesSent * 100 / progress.totalBytesExpectedToSent));
            jsonObject.put(UploadTask.RESULT_KEY_TOTAL_BYTES_SENT, progress.totalBytesSent);
            jsonObject.put(UploadTask.RESULT_KEY_TOTAL_BYTES_EXPECTED_TO_SEND, progress.totalBytesExpectedToSent);
            for (CallbackWrapper callbackWrapper : progressListenerList) {
                callbackWrapper.callback(new Response(jsonObject));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    interface ProgressListener {
        void onProgressUpdate(UploadProgress progress);
    }

    private static final class UploadProgress {
        long totalBytesSent;
        long totalBytesExpectedToSent;

        public UploadProgress(long totalBytesSent, long totalBytesExpectedToSent) {
            this.totalBytesSent = totalBytesSent;
            this.totalBytesExpectedToSent = totalBytesExpectedToSent;
        }
    }
}

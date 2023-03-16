/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;
import com.eclipsesource.v8.utils.typedarrays.ArrayBuffer;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.internal.http.HttpMethod;
import org.hapjs.bridge.CallbackHybridFeature;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.common.net.HttpConfig;
import org.hapjs.common.net.NetworkReportManager;
import org.hapjs.common.net.RequestTag;
import org.hapjs.common.utils.FileHelper;
import org.hapjs.common.utils.FileUtils;
import org.hapjs.features.net.FileRequestBody;
import org.hapjs.features.net.FormData;
import org.hapjs.features.net.FormFile;
import org.hapjs.features.net.RequestHelper;
import org.hapjs.model.AppInfo;
import org.hapjs.render.jsruntime.serialize.JavaSerializeObject;
import org.hapjs.render.jsruntime.serialize.SerializeArray;
import org.hapjs.render.jsruntime.serialize.SerializeException;
import org.hapjs.render.jsruntime.serialize.SerializeObject;
import org.hapjs.runtime.HapEngine;
import org.json.JSONException;
import org.json.JSONObject;

public abstract class AbstractRequest extends CallbackHybridFeature {
    protected static final String ACTION_FETCH = "fetch";
    protected static final String PARAMS_KEY_URL = "url";
    protected static final String PARAMS_KEY_DATA = "data";
    protected static final String PARAMS_KEY_HEADER = "header";
    protected static final String PARAMS_KEY_METHOD = "method";
    protected static final String PARAMS_KEY_FILES = "files";
    protected static final String PARAMS_KEY_RESPONSE_TYPE = "responseType";
    protected static final String RESULT_KEY_CODE = "code";
    protected static final String RESULT_KEY_DATA = "data";
    protected static final String RESULT_KEY_HEADERS = "headers";
    protected static final String RESPONSE_TYPE_TEXT = "text";
    protected static final String RESPONSE_TYPE_JSON = "json";
    protected static final String RESPONSE_TYPE_ARRAYBUFFER = "arraybuffer";
    protected static final String RESPONSE_TYPE_FILE = "file";
    // 新定义error code取值从ERROR_CODE_BASE开始. ERROR_CODE_BASE从2000开始, 目的是兼容子类之前已定义的error code.
    protected static final int ERROR_CODE_BASE = Response.CODE_FEATURE_ERROR + 1000;
    protected static final int CODE_PARSE_DATA_ERROR = ERROR_CODE_BASE;
    protected static final int CODE_NETWORK_ERROR = ERROR_CODE_BASE + 1;
    private static final String TAG = "AbstractRequest";
    private static final String[] MIME_APPLICATION_TEXTS =
            new String[] {"application/json", "application/javascript", "application/xml"};
    // 支持content-type设置application/x-www-form-urlencoded以外的type的最低平台版本要求
    private static final int SUPPORT_OTHER_CONTENT_TYPE_VERSION = 1010;
    private static final Pattern CONTENT_DISPOSITION_PATTERN =
            Pattern.compile(
                    "attachment;\\s*filename\\s*=\\s*(\"?)([^\"]*)\\1\\s*$",
                    Pattern.CASE_INSENSITIVE);

    private static String parseContentDisposition(String contentDisposition) {
        try {
            Matcher m = CONTENT_DISPOSITION_PATTERN.matcher(contentDisposition);
            if (m.find()) {
                return m.group(2);
            }
        } catch (IllegalStateException e) {
            Log.e(TAG, "parseContentDisposition failed", e);
        }
        return null;
    }

    private final WeakHashMap<Call, Request> mRequestMap = new WeakHashMap<>();

    @Override
    protected Response invokeInner(Request request)
            throws JSONException, SerializeException, UnsupportedEncodingException {
        doFetch(request);
        return null;
    }

    private void doFetch(final Request request)
            throws SerializeException, UnsupportedEncodingException {
        Context context = request.getNativeInterface().getActivity().getApplicationContext();
        String pkg = request.getApplicationContext().getPackage();
        SerializeObject reader = request.getSerializeParams();
        if (reader == null) {
            request.getCallback()
                    .callback(new Response(Response.CODE_ILLEGAL_ARGUMENT, "Invalid param"));
            return;
        }
        String url = reader.getString(PARAMS_KEY_URL);
        String responseType = reader.optString(PARAMS_KEY_RESPONSE_TYPE);
        Object dataObj = reader.opt(PARAMS_KEY_DATA);
        SerializeObject jsonHeader = reader.optSerializeObject(PARAMS_KEY_HEADER);
        String method = reader.optString(PARAMS_KEY_METHOD, "GET").toUpperCase();

        okhttp3.Request.Builder requestBuilder = null;
        List<FormFile> fileList = null;
        if (!HttpMethod.permitsRequestBody(method)) {
            try {
                requestBuilder = getGetRequest(url, dataObj, jsonHeader, method);
            } catch (StackOverflowError error) {
                request
                        .getCallback()
                        .callback(getErrorResponse(request.getAction(), error,
                                Response.CODE_ILLEGAL_ARGUMENT));
            } finally {
                if (requestBuilder == null) {
                    Log.e(TAG, "GET: null of requestBuilder");
                    return;
                }
            }
        } else {
            try {
                SerializeArray files = reader.optSerializeArray(PARAMS_KEY_FILES);
                fileList = RequestHelper.getFormFiles(pkg, files);
                requestBuilder =
                        getPostRequest(context, url, dataObj, fileList, jsonHeader, method, pkg);
            } catch (FileNotFoundException e) {
                request.getCallback().callback(getExceptionResponse(request, e));
            } catch (IllegalArgumentException e) {
                request.getCallback().callback(getExceptionResponse(request, e));
            } catch (Exception e) {
                request.getCallback().callback(getExceptionResponse(request, e));
            } catch (StackOverflowError error) {
                request
                        .getCallback()
                        .callback(getErrorResponse(request.getAction(), error,
                                Response.CODE_ILLEGAL_ARGUMENT));
            } finally {
                if (requestBuilder == null) {
                    Log.e(TAG, "POST: null of requestBuilder");
                    return;
                }
            }
        }

        NetworkReportManager.getInstance().reportNetwork(getName(), url);
        if (isResident()) {
            request.getNativeInterface().getResidentManager().postRegisterFeature(this);
        }
        RequestTag requestTag = new RequestTag(request.getApplicationContext().getPackage());
        requestTag.setRequestHashcode(this.hashCode());
        requestBuilder.tag(RequestTag.class, requestTag);
        OkHttpClient okHttpClient = HttpConfig.get().getOkHttpClient();
        Call call = okHttpClient.newCall(requestBuilder.build());
        synchronized (mRequestMap) {
            mRequestMap.put(call, request);
        }
        call.enqueue(new CallbackImpl(responseType));
    }

    private okhttp3.Request.Builder getGetRequest(
            String url, Object dataObj, SerializeObject jsonHeader, String method)
            throws SerializeException, UnsupportedEncodingException {
        return new okhttp3.Request.Builder()
                .url(appendParamsToUrl(url, dataObj))
                .method(method, null)
                .headers(RequestHelper.getHeaders(jsonHeader));
    }

    private okhttp3.Request.Builder getPostRequest(
            Context context,
            String url,
            Object dataObj,
            List<FormFile> files,
            SerializeObject jsonHeader,
            String method,
            String pkg)
            throws UnsupportedEncodingException, SerializeException {
        Headers headers = RequestHelper.getHeaders(jsonHeader);
        return new okhttp3.Request.Builder()
                .url(url)
                .method(method, getRequestBody(context, dataObj, headers, files, pkg))
                .headers(headers);
    }

    private String appendParamsToUrl(String url, Object objData)
            throws UnsupportedEncodingException, SerializeException {
        if (objData == null || !(objData instanceof SerializeObject)) {
            return url;
        } else {
            String textParams = joinParams((SerializeObject) objData);
            if (url.contains("?")) {
                return url + "&" + textParams;
            } else {
                return url + "?" + textParams;
            }
        }
    }

    private RequestBody getRequestBody(
            Context context, Object objData, Headers headers, List<FormFile> files, String pkg)
            throws UnsupportedEncodingException, SerializeException {
        if (files == null || files.isEmpty()) {
            return getSimplePostBody(headers, objData, pkg);
        }
        return getFilePostBody(context, objData, headers, files);
    }

    private RequestBody getSimplePostBody(Headers headers, Object objData, String pkg)
            throws SerializeException, UnsupportedEncodingException {
        if (objData == null) {
            return RequestBody.create(null, "");
        }
        String contentType = headers.get(RequestHelper.CONTENT_TYPE);
        // 1. 如果data是字符串，content-type默认设置为text/plain，可以重新设置；
        // 2. 如果data是json对象，content-type默认设置为application/x-www-form-urlencoded；
        //    如果content-type为application/x-www-form-urlencoded，会把json的字段按照url规则进行拼接作为请求的body；
        //    如果content-type设置为其他值，原方案实现为throw exception，新方案实现会把json转为字符串作为请求的body;
        // 3. 为了保证api兼容性，如果应用声明的minPlatformVersion是1010以下，按照原方案处理；如果minPlatformVersion是1010或更大，按照新方案处理。
        if (objData instanceof SerializeObject) {
            if (TextUtils.isEmpty(contentType)) {
                contentType = RequestHelper.CONTENT_TYPE_FORM_URLENCODED;
            }
            if (!RequestHelper.CONTENT_TYPE_FORM_URLENCODED.equalsIgnoreCase(contentType)) {
                AppInfo appInfo = HapEngine.getInstance(pkg).getApplicationContext().getAppInfo();
                if (appInfo != null
                        && appInfo.getMinPlatformVersion() >= SUPPORT_OTHER_CONTENT_TYPE_VERSION) {
                    return RequestBody.create(MediaType.parse(contentType), objData.toString());
                } else {
                    throw new IllegalArgumentException(
                            "The value of 'content-type' isn't supported when post json object");
                }
            }
            String textParams = joinParams((SerializeObject) objData);
            return RequestBody.create(
                    MediaType.parse(RequestHelper.CONTENT_TYPE_FORM_URLENCODED), textParams);
        } else if (objData instanceof ArrayBuffer) {
            if (TextUtils.isEmpty(contentType)) {
                contentType = RequestHelper.CONTENT_TYPE_OCTET_STREAM;
            }
            ByteBuffer b = ((ArrayBuffer) objData).getByteBuffer();
            // copy memory to heap
            byte[] buffer = new byte[b.remaining()];
            b.get(buffer);
            return RequestBody.create(MediaType.parse(contentType), buffer);
        }

        contentType =
                TextUtils.isEmpty(contentType) ? RequestHelper.CONTENT_TYPE_TEXT_PLAIN :
                        contentType;
        return RequestBody.create(MediaType.parse(contentType), objData.toString());
    }

    private String joinParams(SerializeObject jsonData)
            throws SerializeException, UnsupportedEncodingException {
        StringBuilder out = new StringBuilder();
        Set<String> keys = jsonData.keySet();
        for (String key : keys) {
            if (out.length() > 0) {
                out.append('&');
            }
            // modify for avoid getString null value exception
            String value = jsonData.optString(key);
            out.append(URLEncoder.encode(key, "utf-8"))
                    .append("=")
                    .append(URLEncoder.encode(value, "utf-8"));
        }
        return out.toString();
    }

    private RequestBody getFilePostBody(
            Context context, Object objData, Headers headers, List<FormFile> files)
            throws SerializeException {

        MultipartBody.Builder builder = new MultipartBody.Builder();
        builder.setType(MultipartBody.FORM);
        if (objData != null) {
            if (objData instanceof SerializeObject) {
                SerializeObject params = (SerializeObject) objData;
                Set<String> keys = params.keySet();
                for (String key : keys) {
                    builder.addFormDataPart(key, params.getString(key));
                }
            } else if (objData instanceof SerializeArray) {
                SerializeArray params = (SerializeArray) objData;
                for (int i = 0; i < params.length(); i++) {
                    SerializeObject param = params.optSerializeObject(i);
                    if (param == null) {
                        Log.w(TAG, "getFilePostBody: param in objData is null at " + i);
                        continue;
                    }
                    builder.addFormDataPart(
                            param.getString(FormData.KEY_DATA_NAME),
                            param.getString(FormData.KEY_DATA_VALUE));
                }
            } else {
                String contentType = headers.get(RequestHelper.CONTENT_TYPE);
                contentType =
                        TextUtils.isEmpty(contentType) ? RequestHelper.CONTENT_TYPE_TEXT_PLAIN :
                                contentType;
                builder.addPart(
                        RequestBody.create(MediaType.parse(contentType), objData.toString()));
            }
        }

        for (int i = 0; i < files.size(); i++) {
            FormFile uploadFile = files.get(i);
            builder.addFormDataPart(
                    uploadFile.formName,
                    uploadFile.fileName,
                    new FileRequestBody(uploadFile.mediaType, uploadFile.uri, context));
        }
        return builder.build();
    }

    protected boolean isResident() {
        return false;
    }

    @Override
    public void dispose(boolean force) {
        super.dispose(force);
        if (force) {
            List<Call> calls = new ArrayList<>();
            calls.addAll(HttpConfig.get().getOkHttpClient().dispatcher().queuedCalls());
            calls.addAll(HttpConfig.get().getOkHttpClient().dispatcher().runningCalls());
            for (Call call : calls) {
                if (call != null
                        && call.request().tag(RequestTag.class) instanceof RequestTag
                        && this.hashCode()
                        == call.request().tag(RequestTag.class).getRequestHashcode()) {
                    call.cancel();
                }
            }

            synchronized (mRequestMap) {
                mRequestMap.clear();
            }
        }
    }

    private class CallbackImpl implements Callback {
        private final String responseType;

        CallbackImpl(String responseType) {
            this.responseType = responseType;
        }

        private Request getRequest(Call call) {
            synchronized (mRequestMap) {
                return mRequestMap.get(call);
            }
        }

        @Override
        public void onFailure(Call call, IOException e) {
            Request request = getRequest(call);
            if (request == null) {
                Log.w(TAG, "request not found");
                return;
            }

            Log.e(TAG, "Fail to invoke: " + request.getAction(), e);
            Response response = new Response(CODE_NETWORK_ERROR, e.getMessage());
            request.getCallback().callback(response);

            if (isResident()) {
                request.getNativeInterface().getResidentManager()
                        .postUnregisterFeature(AbstractRequest.this);
            }
        }

        @Override
        public void onResponse(Call call, okhttp3.Response response) throws IOException {
            Request request = getRequest(call);
            if (request == null) {
                Log.w(TAG, "request not found");
                return;
            }

            SerializeObject result;
            try {
                result = new JavaSerializeObject();
                result.put(RESULT_KEY_CODE, response.code());
                result.put(RESULT_KEY_HEADERS, RequestHelper.parseHeaders(response.headers()));
                parseData(request, result, response, responseType);
                request.getCallback().callback(new Response(result));
            } catch (Exception e) {
                Log.e(TAG, "Failed to parse data: ", e);
                Response errorResponse = new Response(CODE_PARSE_DATA_ERROR, e.getMessage());
                request.getCallback().callback(errorResponse);
            } finally {
                FileUtils.closeQuietly(response);
            }

            if (isResident()) {
                request.getNativeInterface().getResidentManager()
                        .postUnregisterFeature(AbstractRequest.this);
            }
        }

        private void parseData(Request request, SerializeObject result, okhttp3.Response response,
                               String responseType)
                throws IOException {
            if (response == null || response.body() == null) {
                Log.w(TAG, "parseData: response is invalid");
                return;
            }
            if (RESPONSE_TYPE_TEXT.equalsIgnoreCase(responseType)) {
                result.put(RESULT_KEY_DATA, response.body().string());
            } else if (RESPONSE_TYPE_JSON.equalsIgnoreCase(responseType)) {
                try {
                    result.put(
                            RESULT_KEY_DATA,
                            new JavaSerializeObject(new JSONObject(response.body().string())));
                } catch (JSONException e) {
                    throw new IOException("Fail to Parsing Data to Json!");
                }
            } else if (RESPONSE_TYPE_ARRAYBUFFER.equalsIgnoreCase(responseType)) {
                result.put(RESULT_KEY_DATA, new ArrayBuffer(response.body().bytes()));
            } else if (RESPONSE_TYPE_FILE.equalsIgnoreCase(responseType)) {
                result.put(RESULT_KEY_DATA, parseFile(request, response));
            } else {
                result.put(RESULT_KEY_DATA, parseData(request, response));
            }
        }

        private String parseFile(Request request, okhttp3.Response response) throws IOException {
            if (HapEngine.getInstance(request.getApplicationContext().getPackage()).isCardMode()) {
                throw new IOException("Not support request file on card mode!");
            }
            String fileName =
                    getFileName(
                            response.request().url().toString(),
                            response.header(RequestHelper.CONTENT_DISPOSITION),
                            response.header(RequestHelper.CONTENT_TYPE));
            File dir = request.getApplicationContext().getCacheDir();
            File file = FileHelper.generateAvailableFile(fileName, dir);
            if (file == null
                    || !FileUtils.saveToFile(
                    response.body() == null ? null : response.body().byteStream(), file)) {
                throw new IOException("save file error");
            }
            return request.getApplicationContext().getInternalUri(file);
        }

        private String parseData(Request request, okhttp3.Response response) throws IOException {
            if (isFileResponse(response)) {
                return parseFile(request, response);
            } else {
                return response.body() == null ? null : response.body().string();
            }
        }

        private boolean isFileResponse(okhttp3.Response response) {
            String mimeType = getMimeType(response);
            if (mimeType == null || mimeType.isEmpty()) {
                return false;
            }
            mimeType = mimeType.toLowerCase(Locale.ROOT);

            for (String mime : MIME_APPLICATION_TEXTS) {
                if (mimeType.contains(mime)) {
                    return false;
                }
            }
            return !mimeType.startsWith("text/");
        }

        private String getMimeType(okhttp3.Response response) {
            for (String name : response.headers().names()) {
                if (name != null && RequestHelper.CONTENT_TYPE.equalsIgnoreCase(name)) {
                    return response.header(name);
                }
            }
            return null;
        }

        private String getFileName(String url, String contentDisposition, String mimeType) {
            String filename = null;
            if (!TextUtils.isEmpty(contentDisposition)) {
                filename = parseContentDisposition(contentDisposition);
                if (!TextUtils.isEmpty(filename)) {
                    int index = filename.lastIndexOf('/') + 1;
                    if (index > 0) {
                        filename = filename.substring(index);
                    }
                    return filename;
                }
            }

            if (TextUtils.isEmpty(filename)) {
                String decodedUrl = Uri.decode(url);
                if (!TextUtils.isEmpty(decodedUrl)) {
                    int queryIndex = decodedUrl.indexOf('?');
                    if (queryIndex > 0) {
                        decodedUrl = decodedUrl.substring(0, queryIndex);
                    }
                    if (!decodedUrl.endsWith(File.separator)) {
                        int index = decodedUrl.lastIndexOf('/') + 1;
                        if (index > 0) {
                            filename = decodedUrl.substring(index);
                        }
                    }
                }
            }

            if (TextUtils.isEmpty(filename)) {
                filename = "downloadFile";
            }

            int dotIndex = filename.indexOf('.');
            if (dotIndex < 0) {
                String extension = null;
                if (!TextUtils.isEmpty(mimeType)) {
                    extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
                    if (!TextUtils.isEmpty(extension)) {
                        extension = "." + extension;
                    }
                }
                if (TextUtils.isEmpty(extension)) {
                    if (!TextUtils.isEmpty(mimeType)
                            && mimeType.toLowerCase(Locale.ROOT).startsWith("text/")) {
                        if ("text/html".equalsIgnoreCase(mimeType)) {
                            extension = ".html";
                        } else {
                            extension = ".txt";
                        }
                    } else {
                        extension = ".bin";
                    }
                }
                filename = filename + extension;
            }
            return filename;
        }
    }
}

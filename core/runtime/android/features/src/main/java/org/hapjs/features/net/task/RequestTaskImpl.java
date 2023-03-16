/*
 * Copyright (c) 2022-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.net.task;

import static org.hapjs.bridge.AbstractExtension.getExceptionResponse;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.eclipsesource.v8.utils.typedarrays.ArrayBuffer;

import org.hapjs.bridge.ExtensionManager;
import org.hapjs.bridge.InstanceManager;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.common.net.HttpConfig;
import org.hapjs.common.net.NetworkReportManager;
import org.hapjs.common.utils.FileUtils;
import org.hapjs.features.net.CallbackWrapper;
import org.hapjs.render.jsruntime.serialize.JavaSerializeArray;
import org.hapjs.render.jsruntime.serialize.JavaSerializeObject;
import org.hapjs.render.jsruntime.serialize.SerializeArray;
import org.hapjs.render.jsruntime.serialize.SerializeException;
import org.hapjs.render.jsruntime.serialize.SerializeHelper;
import org.hapjs.render.jsruntime.serialize.SerializeObject;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import okhttp3.Call;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.internal.http.HttpMethod;

public class RequestTaskImpl implements InstanceManager.IInstance {
    public static final String TAG = "RequestTaskImpl";
    private static final String KEY_NETWORK_REPORT_SOURCE = "RequestTask";

    private Request mRequest;
    private volatile boolean isAbort = false;
    private static List<CallbackWrapper> headerCallbackWrapper = new ArrayList<>();
    protected Call mCall;

    private static final String DEFAULT_CHARSET = "utf-8";
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String CONTENT_TYPE_FORM_URLENCODED = "application/x-www-form-urlencoded";
    private static final String CONTENT_TYPE_TEXT_PLAIN = "text/plain";

    private static final String CONTENT_TYPE = "content-type";
    private static final String CHARSET = "charset";

    public RequestTaskImpl(Request request) {
        this.mRequest = request;
    }

    public void abort() {
        Log.d(TAG, "abort mCall is null?" + (mCall == null));
        isAbort = true;
        if (mCall != null) {
            mCall.cancel();
        }
    }

    public void onHeadersReceived(Request request) {
        headerCallbackWrapper.add(new CallbackWrapper(request.getCallback(), request.getJsCallback()));
    }

    public Response offHeadersReceived(Request request) {
        String jsCallback = request.getJsCallback();
        if (ExtensionManager.isValidCallback(jsCallback)) {
            for (CallbackWrapper callbackWrapper : headerCallbackWrapper) {
                if (jsCallback.equals(callbackWrapper.getJsCallbackId())) {
                    headerCallbackWrapper.remove(callbackWrapper);
                    break;
                }
            }
        } else {
            headerCallbackWrapper.clear();
        }
        return Response.SUCCESS;
    }

    public void execute() {
        Log.d(TAG, "RequestTask execute");
        try {
            String pkg = mRequest.getApplicationContext().getPackage();
            SerializeObject reader = mRequest.getSerializeParams();
            String url = reader.getString(RequestTask.PARAMS_KEY_URL);
            String responseType = reader.optString(RequestTask.PARAMS_KEY_RESPOSNE_TYPE, RequestTask.RESPONSE_TYPE_TEXT);
            String dataType = reader.optString(RequestTask.PARAMS_KEY_DATA_TYPE, RequestTask.DATA_TYPE_JSON);
            Object dataObj = reader.opt(RequestTask.PARAMS_KEY_DATA);
            SerializeObject jsonHeader = reader.optSerializeObject(RequestTask.PARAMS_KEY_HEADER);
            String method = reader.optString(RequestTask.PARAMS_KEY_METHOD, RequestTask.METHOD_GET).toUpperCase();

            if (!RequestTask.SUPPORT_METHODS.contains(method)) {
                mRequest.getCallback().callback(new Response(Response.CODE_GENERIC_ERROR, "do not support method : " + method));
                return;
            }

            NetworkReportManager.getInstance().reportNetwork(KEY_NETWORK_REPORT_SOURCE, url);
            okhttp3.Request httpRequest;
            if (!HttpMethod.permitsRequestBody(method)) {
                Log.d(TAG, "getGetRequest");
                httpRequest = getGetRequest(url, dataObj, jsonHeader, method);
            } else {
                Log.d(TAG, "getPostRequest");
                httpRequest = getPostRequest(url, dataObj, jsonHeader, method, pkg);
            }
            OkHttpClient okHttpClient = HttpConfig.get().getOkHttpClient();
            mCall = okHttpClient.newCall(httpRequest);
            mCall.enqueue(new CallbackImpl(mRequest, responseType, dataType));
            if (isAbort) {
                Log.d(TAG, "RequestTask.execute() abort request task");
                mCall.cancel();
            }
        } catch (Exception e) {
            mRequest.getCallback().callback(getExceptionResponse(mRequest, e));
        }
    }

    private okhttp3.Request getGetRequest(
            String url, Object dataObj, SerializeObject jsonHeader, String method)
            throws SerializeException, UnsupportedEncodingException {
        return new okhttp3.Request.Builder()
                .url(combineUrlWithData(url, dataObj))
                .method(method, null)
                .headers(getHeaders(jsonHeader))
                .build();
    }

    private okhttp3.Request getPostRequest(String url, Object dataObj, SerializeObject jsonHeader, String method, String pkg)
            throws UnsupportedEncodingException, SerializeException {
        Headers headers = getHeaders(jsonHeader);
        return new okhttp3.Request.Builder()
                .url(url)
                .method(method, getSimplePostBody(headers, dataObj))
                .headers(headers)
                .build();
    }

    private Headers getHeaders(SerializeObject jsonHeader) throws SerializeException {
        if (jsonHeader == null) {
            Log.d(TAG, "getHeaders no headers");
            return new Headers.Builder()
                    .add(CHARSET, DEFAULT_CHARSET)
                    .add(CONTENT_TYPE, CONTENT_TYPE_JSON).build();
        } else {
            Log.d(TAG, "getHeaders from jsonHeader");
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
            if (TextUtils.isEmpty(builder.get(CONTENT_TYPE))) {
                Log.d(TAG, "add default content type");
                builder.add(CONTENT_TYPE, CONTENT_TYPE_JSON);
            }
            if (TextUtils.isEmpty(builder.get(CHARSET))) {
                Log.d(TAG, "add default charset");
                builder.add(CHARSET, DEFAULT_CHARSET);
            }
            return builder.build();
        }
    }

    private String combineUrlWithData(String url, Object objData) throws UnsupportedEncodingException {
        if (objData == null || !(objData instanceof SerializeObject)) {
            Log.d(TAG, "combineUrlWithData no params");
            return url;
        } else {
            HashMap<String, String> paramsMap = new HashMap<>();

            // get params from url
            Uri uri = Uri.parse(url);
            Set<String> names = uri.getQueryParameterNames();
            for (String name : names) {
                paramsMap.put(name, uri.getQueryParameter(name));
            }

            // get params from dataObject
            SerializeObject dataObject = (SerializeObject) objData;
            Set<String> keys = dataObject.keySet();
            for (String key : keys) {
                paramsMap.put(key, dataObject.optString(key));
            }

            // combine
            StringBuilder out = new StringBuilder();
            for (String key : paramsMap.keySet()) {
                if (out.length() > 0) {
                    out.append('&');
                }
                // modify for avoid getString null value exception
                String value = paramsMap.get(key);
                out.append(URLEncoder.encode(key, "utf-8"))
                        .append("=")
                        .append(URLEncoder.encode(value, "utf-8"));
            }

            Log.d(TAG, "combine get params=" + out.toString());

            int questIndex = url.indexOf("?");
            if (questIndex != -1) {
                return url.substring(0, questIndex) + "?" + out.toString();
            } else {
                return url + "?" + out.toString();
            }
        }
    }

    private RequestBody getSimplePostBody(Headers headers, Object objData)
            throws UnsupportedEncodingException {
        Log.d(TAG, "getSimplePost ");
        if (objData == null) {
            Log.d(TAG, "getSimplePost no objData");
            return RequestBody.create(null, "");
        }
        String contentType = headers.get(CONTENT_TYPE);
        if (objData instanceof SerializeObject) {
            Log.d(TAG, "getSimplePost objData is SerializeObject, contentType=" + contentType);
            if (TextUtils.isEmpty(contentType)) {
                contentType = CONTENT_TYPE_JSON;
            }
            if (!CONTENT_TYPE_FORM_URLENCODED.equalsIgnoreCase(contentType)) {
                JSONObject object = ((SerializeObject) objData).toJSONObject();
                String data = (object == null) ? "" : object.toString();
                return RequestBody.create(MediaType.parse(contentType), data);
            }
            String textParams = joinParams((SerializeObject) objData);
            return RequestBody.create(
                    MediaType.parse(CONTENT_TYPE_FORM_URLENCODED),
                    textParams);
        } else if (objData instanceof ArrayBuffer) {
            Log.d(TAG, "getSimplePost objData is ArrayBuffer, contentType=" + contentType);
            if (TextUtils.isEmpty(contentType)) {
                contentType = CONTENT_TYPE_JSON;
            }
            ByteBuffer b = ((ArrayBuffer) objData).getByteBuffer();
            //copy memory to heap
            byte[] buffer = new byte[b.remaining()];
            b.get(buffer);
            return RequestBody.create(MediaType.parse(contentType), buffer);
        }

        contentType = TextUtils.isEmpty(contentType) ? CONTENT_TYPE_TEXT_PLAIN : contentType;
        return RequestBody.create(MediaType.parse(contentType), objData.toString());
    }

    private String joinParams(SerializeObject jsonData) throws UnsupportedEncodingException {
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
        Log.d(TAG, "joinParams get params=" + out.toString());
        return out.toString();
    }

    private static class CallbackImpl implements okhttp3.Callback {
        private final Request request;
        private final String responseType;
        private final String dataType;

        CallbackImpl(Request request, String responseType, String dataType) {
            this.request = request;
            this.responseType = responseType;
            this.dataType = dataType;
        }

        @Override
        public void onFailure(Call call, IOException e) {
            Log.e(TAG, "Fail to invoke: " + request.getAction(), e);
            Response response = new Response(Response.CODE_FEATURE_ERROR, e.getMessage());
            request.getCallback().callback(response);
        }

        @Override
        public void onResponse(Call call, okhttp3.Response response) throws IOException {
            Log.d(TAG, "onResponse responseType=" + responseType);
            if (response == null) {
                Log.w(TAG, "response is null");
                request.getCallback().callback(new Response(Response.CODE_FEATURE_ERROR, "response is null"));
                return;
            }

            if (RequestTask.RESPONSE_TYPE_ARRAYBUFFER.equals(responseType)) {
                SerializeObject result;
                try {
                    result = new JavaSerializeObject();
                    result.put(RequestTask.RESULT_KEY_STATUS_CODE, response.code());
                    result.put(RequestTask.RESULT_KEY_HEADER, parseHeaders(response));
                    if (response.body() != null) {
                        result.put(RequestTask.RESULT_KEY_DATA, new ArrayBuffer(response.body().bytes()));
                    } else {
                        Log.w(TAG, "response body is invalid");
                    }
                } finally {
                    FileUtils.closeQuietly(response);
                }
                request.getCallback().callback(new Response(result));
            } else {
                JSONObject result = new JSONObject();
                try {
                    for (CallbackWrapper callbackWrapper : headerCallbackWrapper) {
                        callbackWrapper.callback(new Response(parseHeaders(response).toJSONObject()));
                    }
                    result.put(RequestTask.RESULT_KEY_STATUS_CODE, response.code());
                    result.put(RequestTask.RESULT_KEY_HEADER, parseHeaders(response).toJSONObject());
                    parseData(result, response);
                } catch (JSONException e) {
                    Log.e(TAG, "onResponse JSON failed : " + e.getMessage());
                } finally {
                    FileUtils.closeQuietly(response);
                }
                request.getCallback().callback(new Response(result));
            }
        }

        private void parseData(JSONObject result, okhttp3.Response response) throws IOException, JSONException {
            Log.d(TAG, "response parseData dataType=" + dataType);
            if (response == null || response.body() == null) {
                Log.w(TAG, "parseData: response is invalid");
                return;
            }

            String body = response.body().string();
            if (RequestTask.DATA_TYPE_JSON.equals(dataType) && !TextUtils.isEmpty(body)) {
                try {
                    if (body.startsWith("[")) {
                        result.put(RequestTask.RESULT_KEY_DATA, new JSONArray(body));
                    } else {
                        result.put(RequestTask.RESULT_KEY_DATA, new JSONObject(body));
                    }
                } catch (JSONException e) {
                    Log.i(TAG, "Fail to Parsing Data to Json! Fallback to raw string.");
                    result.put(RequestTask.RESULT_KEY_DATA, body);
                }
            } else {
                result.put(RequestTask.RESULT_KEY_DATA, body);
            }
        }

        private SerializeObject parseHeaders(okhttp3.Response response) {
            SerializeObject headersObj = new JavaSerializeObject();
            Headers headers = response.headers();

            final int N = headers.size();
            Log.d(TAG, "response parseHeaders size=" + N);
            for (int i = 0; i < N; ++i) {
                String name = headers.name(i);
                String value = headers.value(i);
                Object valueObj = headersObj.opt(name);
                if (valueObj == null) {
                    headersObj.put(name, value);
                } else {
                    SerializeArray valuesArray;
                    if (valueObj instanceof SerializeArray) {
                        valuesArray = (SerializeArray) valueObj;
                    } else {
                        valuesArray = new JavaSerializeArray();
                        valuesArray.put((String) valueObj);
                        headersObj.put(name, valuesArray);
                    }
                    valuesArray.put(value);
                }
            }
            return headersObj;
        }
    }

    @Override
    public void release() {
        abort();
    }

    @Override
    public String getFeatureName() {
        return RequestTask.FEATURE_NAME;
    }
}

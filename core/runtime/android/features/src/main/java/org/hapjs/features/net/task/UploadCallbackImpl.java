/*
 * Copyright (c) 2022-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.net.task;

import android.util.Log;
import android.webkit.URLUtil;

import com.eclipsesource.v8.utils.typedarrays.ArrayBuffer;

import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.common.utils.FileHelper;
import org.hapjs.common.utils.FileUtils;
import org.hapjs.features.net.RequestHelper;
import org.hapjs.render.jsruntime.serialize.JavaSerializeObject;
import org.hapjs.render.jsruntime.serialize.SerializeObject;
import org.hapjs.runtime.HapEngine;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;

public class UploadCallbackImpl implements Callback {
    private final String TAG = "UploadCallbackImpl";

    public static final String RESULT_KEY_CODE = "statusCode";
    public static final String RESULT_KEY_DATA = "data";
    public static final String RESULT_KEY_HEADER = "header";

    private static final String[] MIME_APPLICATION_TEXTS = new String[]{
            "application/json", "application/javascript", "application/xml"
    };

    private static final String CONTENT_TYPE = "Content-Type";
    private static final String CONTENT_DISPOSITION = "Content-Disposition";

    protected static final String RESPONSE_TYPE_TEXT = "text";
    protected static final String RESPONSE_TYPE_JSON = "json";
    protected static final String RESPONSE_TYPE_ARRAYBUFFER = "arraybuffer";
    protected static final String RESPONSE_TYPE_FILE = "file";

    private final Request request;
    private final String responseType;
    private HeaderReceivedListener headerReceivedListener;

    public UploadCallbackImpl(Request request, String responseType, HeaderReceivedListener headerReceivedListener) {
        this.request = request;
        this.responseType = responseType;
        this.headerReceivedListener = headerReceivedListener;
    }

    @Override
    public void onFailure(Call call, IOException e) {
        Log.e(TAG, "Fail to invoke: " + request.getAction(), e);

        Response response = new Response(Response.CODE_FEATURE_ERROR, e.getMessage());
        request.getCallback().callback(response);
    }

    @Override
    public void onResponse(Call call, okhttp3.Response response) throws IOException {
        SerializeObject result;
        try {
            result = new JavaSerializeObject();
            result.put(RESULT_KEY_CODE, response.code());
            try {
                if (headerReceivedListener != null) {
                    headerReceivedListener.onHeaderReceived(RequestHelper.parseHeaders(response.headers()));
                }
                result.put(RESULT_KEY_HEADER, RequestHelper.parseHeaders(response.headers()));
            } catch (Exception e) {
                Log.e(TAG, "Fail to getHeaders", e);
                result.put(RESULT_KEY_HEADER, new JavaSerializeObject());
            }
            parseData(result, response, responseType);
        } finally {
            FileUtils.closeQuietly(response);
        }
        request.getCallback().callback(new Response(result));
    }

    private void parseData(SerializeObject result, okhttp3.Response response, String responseType) throws IOException {
        if (RESPONSE_TYPE_TEXT.equalsIgnoreCase(responseType)) {
            result.put(RESULT_KEY_DATA, response.body().string());
        } else if (RESPONSE_TYPE_JSON.equalsIgnoreCase(responseType)) {
            try {
                result.put(RESULT_KEY_DATA, new JavaSerializeObject(new JSONObject(response.body().string())));
            } catch (JSONException e) {
                throw new IOException("Fail to Parsing Data to Json!");
            }
        } else if (RESPONSE_TYPE_ARRAYBUFFER.equalsIgnoreCase(responseType)) {
            result.put(RESULT_KEY_DATA, new ArrayBuffer(response.body().bytes()));
        } else if (RESPONSE_TYPE_FILE.equalsIgnoreCase(responseType)) {
            result.put(RESULT_KEY_DATA, parseFile(response));
        } else {
            result.put(RESULT_KEY_DATA, parseData(response));
        }
    }

    private String parseFile(okhttp3.Response response) throws IOException {
        if (HapEngine.getInstance(request.getApplicationContext().getPackage()).isCardMode()) {
            throw new IOException("Not support request file on card mode!");
        }
        String fileName = URLUtil.guessFileName(response.request().url().toString(),
                response.header(CONTENT_DISPOSITION), response.header(CONTENT_TYPE));
        File dir = request.getApplicationContext().getCacheDir();
        File file = FileHelper.generateAvailableFile(fileName, dir);
        if (file == null || !FileUtils.saveToFile(response.body().byteStream(), file)) {
            throw new IOException("save file error");
        }
        return request.getApplicationContext().getInternalUri(file);
    }

    private String parseData(okhttp3.Response response) throws IOException {
        if (isFileResponse(response)) {
            return parseFile(response);
        } else {
            return response.body().string();
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
            if (name != null && CONTENT_TYPE.equalsIgnoreCase(name)) {
                return response.header(name);
            }
        }
        return null;
    }

    interface HeaderReceivedListener {
        void onHeaderReceived(SerializeObject serializeObject);
    }
}

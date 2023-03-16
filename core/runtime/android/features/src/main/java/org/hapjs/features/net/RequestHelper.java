/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.net;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import java.io.FileNotFoundException;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import okhttp3.Headers;
import okhttp3.MediaType;
import org.hapjs.render.jsruntime.serialize.JavaSerializeArray;
import org.hapjs.render.jsruntime.serialize.JavaSerializeObject;
import org.hapjs.render.jsruntime.serialize.SerializeArray;
import org.hapjs.render.jsruntime.serialize.SerializeException;
import org.hapjs.render.jsruntime.serialize.SerializeHelper;
import org.hapjs.render.jsruntime.serialize.SerializeObject;
import org.hapjs.runtime.HapEngine;

public class RequestHelper {

    public static final String CONTENT_TYPE_FORM_URLENCODED = "application/x-www-form-urlencoded";
    public static final String CONTENT_TYPE_TEXT_PLAIN = "text/plain";
    public static final String CONTENT_TYPE_OCTET_STREAM = "application/octet-stream";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String CONTENT_DISPOSITION = "Content-Disposition";
    private static final String TAG = "RequestHelper";
    private static final Headers EMPTY_HEADERS = new Headers.Builder().build();

    public static Headers getHeaders(SerializeObject jsonHeader) {
        if (jsonHeader == null) {
            return EMPTY_HEADERS;
        } else {
            Headers.Builder builder = new Headers.Builder();
            for (String key : jsonHeader.keySet()) {
                Object value = jsonHeader.opt(key);
                if (value instanceof SerializeArray) {
                    SerializeArray values = (SerializeArray) value;
                    for (int i = 0; i < values.length(); ++i) {
                        builder.add(key, values.optString(i));
                    }
                } else {
                    builder.add(key, SerializeHelper.toString(value, ""));
                }
            }
            return builder.build();
        }
    }

    public static SerializeObject parseHeaders(Headers headers) {
        SerializeObject headersObj = new JavaSerializeObject();
        final int N = headers.size();
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

    public static List<FormData> getFormDatas(SerializeArray datas) throws SerializeException {
        if (datas == null) {
            return null;
        }
        List<FormData> dataList = new ArrayList<>();
        for (int i = 0; i < datas.length(); i++) {
            SerializeObject param = datas.optSerializeObject(i);
            if (param == null) {
                continue;
            }
            FormData formData =
                    new FormData(
                            param.getString(FormData.KEY_DATA_NAME),
                            param.getString(FormData.KEY_DATA_VALUE));
            dataList.add(formData);
        }
        return dataList;
    }

    public static List<FormFile> getFormFiles(String pkg, SerializeArray files)
            throws SerializeException, FileNotFoundException {
        if (files == null) {
            return null;
        }
        List<FormFile> fileList = new ArrayList<>(files.length());
        for (int i = 0; i < files.length(); i++) {
            SerializeObject item = files.getSerializeObject(i);
            if (item == null) {
                continue;
            }
            String fileName = item.optString(FormFile.KEY_FILENAME);
            String fileUri = item.optString(FormFile.KEY_FILE_URI);
            String formName = item.optString(FormFile.KEY_FORM_NAME);
            String fileType = item.optString(FormFile.KEY_FILE_TYPE);

            if (TextUtils.isEmpty(fileUri)) {
                throw new IllegalArgumentException("uri is null");
            }

            Uri uri = HapEngine.getInstance(pkg).getApplicationContext().getUnderlyingUri(fileUri);
            if (uri == null) {
                throw new FileNotFoundException("uri does not exist: " + uri);
            }

            if (TextUtils.isEmpty(formName)) {
                formName = FormFile.DEFAULT_FORM_NAME;
            }

            MediaType mediaType =
                    TextUtils.isEmpty(fileType)
                            ? getMimeType(
                            TextUtils.isEmpty(fileName) ? uri.getLastPathSegment() : fileName)
                            : MediaType.parse(fileType);
            fileList.add(new FormFile(formName, fileName, uri, mediaType));
        }
        return fileList;
    }

    public static MediaType getMimeType(String name) {
        FileNameMap fileNameMap = URLConnection.getFileNameMap();
        String contentTypeFor = null;
        try {
            contentTypeFor = fileNameMap.getContentTypeFor(URLEncoder.encode(name, "UTF-8"));
        } catch (Exception e) {
            Log.e(TAG, "getMimeType", e);
        }
        if (contentTypeFor == null) {
            contentTypeFor = CONTENT_TYPE_OCTET_STREAM;
        }
        return MediaType.parse(contentTypeFor);
    }
}

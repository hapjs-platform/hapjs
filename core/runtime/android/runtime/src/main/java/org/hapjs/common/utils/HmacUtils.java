/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.utils;

import android.util.Log;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class HmacUtils {
    private static final String TAG = "HmacUtils";

    private static String byteArrayToHexString(byte[] b) {
        StringBuilder builder = new StringBuilder();
        String stmp;
        for (int n = 0; b != null && n < b.length; n++) {
            stmp = Integer.toHexString(b[n] & 0XFF);
            if (stmp.length() == 1) {
                builder.append('0');
            }
            builder.append(stmp);
        }
        return builder.toString().toLowerCase(Locale.getDefault());
    }

    public static String sha256HMAC(String message, String secret) {
        Log.d(TAG, "sha256_HMAC start time ");
        String hash = "";
        try {
            Mac sha256HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey =
                    new SecretKeySpec(secret.getBytes(Charset.forName("UTF-8")), "HmacSHA256");
            sha256HMAC.init(secretKey);
            byte[] bytes = sha256HMAC.doFinal(message.getBytes(Charset.forName("UTF-8")));
            hash = byteArrayToHexString(bytes);
        } catch (Exception e) {
            Log.e(TAG, "sha256_HMAC error msg : " + e.getMessage());
        }
        Log.d(TAG, "sha256_HMAC end time ");
        return hash;
    }

    public static void mapToJSONObject(JSONObject jsonObject, Map<?, ?> map) throws JSONException {
        if (null == jsonObject || null == map) {
            return;
        }
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = entry.getKey().toString();
            Object val = entry.getValue();
            if (val instanceof Integer) {
                jsonObject.put(key, (int) (Integer) val);
            } else if (val instanceof Double) {
                jsonObject.put(key, (double) (Double) val);
            } else if (val instanceof Float) {
                jsonObject.put(key, (double) (float) (Float) val);
            } else if (val instanceof Boolean) {
                jsonObject.put(key, (boolean) (Boolean) val);
            } else if (val instanceof Map) {
                JSONObject tmpObj = new JSONObject();
                mapToJSONObject(tmpObj, (Map<?, ?>) val);
                jsonObject.put(key, tmpObj);
            } else if (val instanceof List) {
                List list = (List) val;
                JSONArray jsonArray = new JSONArray();
                for (Object o : list) {
                    push(jsonArray, o);
                }
                jsonObject.put(key, jsonArray);
            } else if (val == null) {
                //                obj.addNull(key);
            } else {
                jsonObject.put(key, val.toString());
            }
        }
    }

    public static void push(JSONArray array, Object object) throws JSONException {
        if (null == array || null == object) {
            return;
        }
        if (object instanceof Integer) {
            array.put((Integer) object);
        } else if (object instanceof Double) {
            array.put((Double) object);
        } else if (object instanceof Float) {
            array.put((Float) object);
        } else if (object instanceof Boolean) {
            array.put((Boolean) object);
        } else if (object instanceof String) {
            array.put((String) object);
        } else if (object instanceof Map) {
            JSONObject jsonObject = new JSONObject();
            mapToJSONObject(jsonObject, (Map<?, ?>) object);
            array.put(jsonObject);
        }
    }
}

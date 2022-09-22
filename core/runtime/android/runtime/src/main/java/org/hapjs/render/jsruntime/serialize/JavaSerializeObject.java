/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.jsruntime.serialize;

import android.util.Log;

import com.eclipsesource.v8.utils.typedarrays.ArrayBuffer;
import com.eclipsesource.v8.utils.typedarrays.TypedArray;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.json.JSONException;
import org.json.JSONObject;

public class JavaSerializeObject implements SerializeObject {
    private static final String TAG = "JavaSerializeObject";

    private SerializeObject mObject;
    private Map<String, Object> mOverlayMap;

    public JavaSerializeObject() {
        this(new JSONObject());
    }

    public JavaSerializeObject(Map<String, Object> map) {
        this(new V8SerializeObject(map));
    }

    public JavaSerializeObject(JSONObject jsonObject) {
        this(new JSONSerializeObject(jsonObject));
    }

    private JavaSerializeObject(SerializeObject object) {
        mObject = object;
        mOverlayMap = new HashMap<>();
    }

    @Override
    public final Object get(String key) throws SerializeException {
        Object value = mOverlayMap.get(key);
        if (value != null) {
            return value;
        } else {
            return mObject.get(key);
        }
    }

    @Override
    public Object opt(String key) {
        Object value = mOverlayMap.get(key);
        if (value != null) {
            return value;
        } else {
            return mObject.opt(key);
        }
    }

    @Override
    public final int getInt(String key) throws SerializeException {
        return mObject.getInt(key);
    }

    @Override
    public final int optInt(String key) {
        return mObject.optInt(key);
    }

    @Override
    public int optInt(String key, int defaultValue) {
        return mObject.optInt(key, defaultValue);
    }

    @Override
    public final long getLong(String key) throws SerializeException {
        return mObject.getLong(key);
    }

    @Override
    public final long optLong(String key) {
        return mObject.optLong(key);
    }

    @Override
    public long optLong(String key, long defaultValue) {
        return mObject.optLong(key, defaultValue);
    }

    @Override
    public final double getDouble(String key) throws SerializeException {
        return mObject.getDouble(key);
    }

    @Override
    public final double optDouble(String key) {
        return mObject.optDouble(key);
    }

    @Override
    public double optDouble(String key, double defaultValue) {
        return mObject.optDouble(key, defaultValue);
    }

    @Override
    public final boolean getBoolean(String key) throws SerializeException {
        return mObject.getBoolean(key);
    }

    @Override
    public final boolean optBoolean(String key) {
        return mObject.optBoolean(key);
    }

    @Override
    public boolean optBoolean(String key, boolean defaultValue) {
        return mObject.optBoolean(key, defaultValue);
    }

    @Override
    public final String getString(String key) throws SerializeException {
        return mObject.getString(key);
    }

    @Override
    public final String optString(String key) {
        return mObject.optString(key);
    }

    @Override
    public String optString(String key, String defaultValue) {
        return mObject.optString(key, defaultValue);
    }

    @Override
    public final ArrayBuffer getArrayBuffer(String key) throws SerializeException {
        return mObject.getArrayBuffer(key);
    }

    @Override
    public ArrayBuffer optArrayBuffer(String key) {
        return mObject.optArrayBuffer(key);
    }

    @Override
    public final TypedArray getTypedArray(String key) throws SerializeException {
        return mObject.getTypedArray(key);
    }

    @Override
    public TypedArray optTypedArray(String key) {
        return mObject.optTypedArray(key);
    }

    @Override
    public final SerializeObject getSerializeObject(String key) throws SerializeException {
        Object value = mOverlayMap.get(key);
        if (value instanceof SerializeObject) {
            return (SerializeObject) value;
        } else {
            SerializeObject result = mObject.getSerializeObject(key);
            mOverlayMap.put(key, result);
            return result;
        }
    }

    @Override
    public final SerializeObject optSerializeObject(String key) {
        Object value = mOverlayMap.get(key);
        if (value instanceof SerializeObject) {
            return (SerializeObject) value;
        } else {
            SerializeObject result = mObject.optSerializeObject(key);
            if (result != null) {
                mOverlayMap.put(key, result);
            }
            return result;
        }
    }

    @Override
    public final SerializeArray getSerializeArray(String key) throws SerializeException {
        Object value = mOverlayMap.get(key);
        if (value instanceof SerializeArray) {
            return (SerializeArray) value;
        } else {
            SerializeArray result = mObject.getSerializeArray(key);
            mOverlayMap.put(key, result);
            return result;
        }
    }

    @Override
    public final SerializeArray optSerializeArray(String key) {
        Object value = mOverlayMap.get(key);
        if (value instanceof SerializeArray) {
            return (SerializeArray) value;
        } else {
            SerializeArray result = mObject.optSerializeArray(key);
            if (result != null) {
                mOverlayMap.put(key, result);
            }
            return result;
        }
    }

    @Override
    public HandlerObject getHandlerObject(String key) throws SerializeException {
        return mObject.getHandlerObject(key);
    }

    @Override
    public HandlerObject optHandlerObject(String key) {
        return mObject.optHandlerObject(key);
    }

    @Override
    public final SerializeObject put(String key, int value) {
        mOverlayMap.remove(key);
        mObject.put(key, value);
        return this;
    }

    @Override
    public final SerializeObject put(String key, double value) {
        mOverlayMap.remove(key);
        mObject.put(key, value);
        return this;
    }

    @Override
    public final SerializeObject put(String key, long value) {
        mOverlayMap.remove(key);
        mObject.put(key, value);
        return this;
    }

    @Override
    public final SerializeObject put(String key, boolean value) {
        mOverlayMap.remove(key);
        mObject.put(key, value);
        return this;
    }

    @Override
    public final SerializeObject put(String key, String value) {
        mOverlayMap.remove(key);
        mObject.put(key, value);
        return this;
    }

    @Override
    public final SerializeObject put(String key, ArrayBuffer value) {
        mOverlayMap.remove(key);
        if (mObject instanceof JSONSerializeObject) {
            mObject = new V8SerializeObject(mObject.toMap());
        }
        mObject.put(key, value);
        return this;
    }

    @Override
    public final SerializeObject put(String key, TypedArray value) {
        mOverlayMap.remove(key);
        if (mObject instanceof JSONSerializeObject) {
            mObject = new V8SerializeObject(mObject.toMap());
        }
        mObject.put(key, value);
        return this;
    }

    @Override
    public final SerializeObject put(String key, SerializeObject value) {
        mObject.remove(key);
        if (value != null
                && value.getType() == Serializable.TYPE_V8
                && mObject instanceof JSONSerializeObject) {
            mObject = new V8SerializeObject(mObject.toMap());
        }
        mOverlayMap.put(key, value);
        return this;
    }

    @Override
    public final SerializeObject put(String key, SerializeArray value) {
        mObject.remove(key);
        if (value != null
                && value.getType() == Serializable.TYPE_V8
                && mObject instanceof JSONSerializeObject) {
            mObject = new V8SerializeObject(mObject.toMap());
        }
        mOverlayMap.put(key, value);
        return this;
    }

    @Override
    public SerializeObject put(String key, HandlerObject value) {
        mOverlayMap.remove(key);
        if (mObject instanceof JSONSerializeObject) {
            mObject = new V8SerializeObject(mObject.toMap());
        }
        mObject.put(key, value);
        return this;
    }

    @Override
    public final Object remove(String key) {
        Object value = mOverlayMap.remove(key);
        if (value == null) {
            value = mObject.remove(key);
        } else {
            mObject.remove(key);
        }
        return value;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> result = mObject.toMap();
        for (Map.Entry<String, Object> entry : mOverlayMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof SerializeObject) {
                result.put(key, ((SerializeObject) value).toMap());
            } else if (value instanceof SerializeArray) {
                result.put(key, ((SerializeArray) value).toList());
            } else {
                throw new IllegalStateException("Never get here");
            }
        }
        return result;
    }

    @Override
    public JSONObject toJSONObject() {
        JSONObject result = mObject.toJSONObject();
        for (Map.Entry<String, Object> entry : mOverlayMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            try {
                if (value instanceof SerializeObject) {
                    result.put(key, ((SerializeObject) value).toJSONObject());
                } else if (value instanceof SerializeArray) {
                    result.put(key, ((SerializeArray) value).toJSONArray());
                } else {
                    throw new IllegalStateException("Never get here");
                }
            } catch (JSONException e) {
                // ignore
            }
        }
        return result;
    }

    @Override
    public boolean has(String key) {
        return mOverlayMap.containsKey(key) || mObject.has(key);
    }

    @Override
    public Set<String> keySet() {
        Set<String> keySet = new HashSet<>(mOverlayMap.keySet());
        keySet.addAll(mObject.keySet());
        return keySet;
    }

    @Override
    public int length() {
        return keySet().size();
    }

    @Override
    public int getType() {
        return mObject.getType();
    }

    @Override
    public String toString() {
        JSONObject jsonObject = null;
        try {
            jsonObject = toJSONObject();
        } catch (Exception e) {
            Log.e(TAG, "json error", e);
        }
        return jsonObject != null ? jsonObject.toString() : super.toString();
    }
}

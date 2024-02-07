/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.jsruntime.serialize;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.utils.ArrayBuffer;
import com.eclipsesource.v8.utils.TypedArray;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.json.JSONException;
import org.json.JSONObject;

public class V8SerializeObject extends AbstractSerializeObject {
    private Map<String, Object> mMap;

    private V8SerializeObject() {}

    public V8SerializeObject(Map<String, Object> map) {
        mMap = map;
        removeUndefinedEntry();
    }

    @Override
    public Object opt(String key) {
        Object value = mMap.get(key);
        if (value instanceof Map) {
            value = new V8SerializeObject((Map<String, Object>) value);
        } else if (value instanceof List) {
            value = new V8SerializeArray((List<Object>) value);
        }
        return value;
    }

    @Override
    public int optInt(String key, int defaultValue) {
        Object value = mMap.get(key);
        return SerializeHelper.toInteger(value, defaultValue);
    }

    @Override
    public long optLong(String key, long defaultValue) {
        Object value = mMap.get(key);
        return SerializeHelper.toLong(value, defaultValue);
    }

    @Override
    public double optDouble(String key, double defaultValue) {
        Object value = mMap.get(key);
        return SerializeHelper.toDouble(value, defaultValue);
    }

    @Override
    public boolean optBoolean(String key, boolean defaultValue) {
        Object value = mMap.get(key);
        return SerializeHelper.toBoolean(value, defaultValue);
    }

    @Override
    public String optString(String key, String defaultValue) {
        Object value = opt(key);
        return SerializeHelper.toString(value, defaultValue);
    }

    @Override
    public ArrayBuffer optArrayBuffer(String key) {
        Object value = mMap.get(key);
        if (value instanceof ArrayBuffer) {
            return (ArrayBuffer) value;
        } else {
            return null;
        }
    }

    @Override
    public TypedArray optTypedArray(String key) {
        Object value = mMap.get(key);
        if (value instanceof TypedArray) {
            return (TypedArray) value;
        } else {
            return null;
        }
    }

    @Override
    public ByteBuffer optByteBuffer(String key) {
        Object value = mMap.get(key);
        if (value instanceof ByteBuffer) {
            return (ByteBuffer) value;
        } else {
            return null;
        }
    }

    @Override
    public TypedArrayProxy optTypedArrayProxy(String key) {
        Object value = mMap.get(key);
        if (value instanceof TypedArrayProxy) {
            return (TypedArrayProxy) value;
        } else {
            return null;
        }
    }

    @Override
    public SerializeObject optSerializeObject(String key) {
        Object value = mMap.get(key);
        if (value instanceof Map) {
            return new V8SerializeObject((Map<String, Object>) value);
        } else {
            return null;
        }
    }

    @Override
    public SerializeArray optSerializeArray(String key) {
        Object value = mMap.get(key);
        if (value instanceof List) {
            return new V8SerializeArray((List<Object>) value);
        } else {
            return null;
        }
    }

    @Override
    public HandlerObject optHandlerObject(String key) {
        Object value = mMap.get(key);
        if (value instanceof HandlerObject) {
            return (HandlerObject) value;
        } else {
            return null;
        }
    }

    @Override
    public SerializeObject put(String key, int value) {
        mMap.put(key, value);
        return this;
    }

    @Override
    public SerializeObject put(String key, double value) {
        mMap.put(key, value);
        return this;
    }

    @Override
    public SerializeObject put(String key, long value) {
        mMap.put(key, value);
        return this;
    }

    @Override
    public SerializeObject put(String key, boolean value) {
        mMap.put(key, value);
        return this;
    }

    @Override
    public SerializeObject put(String key, String value) {
        mMap.put(key, value);
        return this;
    }

    @Override
    public SerializeObject put(String key, ByteBuffer value) {
        mMap.put(key, value);
        return this;
    }

    @Override
    public SerializeObject put(String key, TypedArrayProxy value) {
        mMap.put(key, value);
        return this;
    }

    @Override
    public SerializeObject put(String key, SerializeObject value) {
        mMap.put(key, value.toMap());
        return this;
    }

    @Override
    public SerializeObject put(String key, SerializeArray value) {
        mMap.put(key, value.toList());
        return this;
    }

    @Override
    public SerializeObject put(String key, HandlerObject value) {
        mMap.put(key, value);
        return this;
    }

    @Override
    public SerializeObject put(String key, byte[] value) {
        mMap.put(key, value);
        return this;
    }

    @Override
    public Object remove(String key) {
        Object value = opt(key);
        mMap.remove(key);
        return value;
    }

    @Override
    public Map<String, Object> toMap() {
        return mMap;
    }

    @Override
    public JSONObject toJSONObject() {
        JSONObject result = new JSONObject();
        try {
            for (Map.Entry<String, Object> entry : mMap.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (value instanceof Integer) {
                    result.put(key, ((Integer) value).intValue());
                } else if (value instanceof Long) {
                    result.put(key, ((Long) value).longValue());
                } else if (value instanceof Double) {
                    result.put(key, ((Double) value).doubleValue());
                } else if (value instanceof Boolean) {
                    result.put(key, ((Boolean) value).booleanValue());
                } else if (value instanceof String) {
                    result.put(key, value);
                } else if (value instanceof ArrayBuffer) {
                    // ignore
                } else if (value instanceof TypedArray) {
                    // ignore
                } else if (value instanceof ByteBuffer) {
                    // ignore
                } else if (value instanceof TypedArrayProxy) {
                    // ignore
                } else if (value instanceof byte[]) {
                    // ignore
                } else if (value instanceof Map) {
                    result.put(key,
                            new V8SerializeObject((Map<String, Object>) value).toJSONObject());
                } else if (value instanceof List) {
                    result.put(key, new V8SerializeArray((List<Object>) value).toJSONArray());
                }
            }
        } catch (JSONException e) {
            // ignore
        }
        return result;
    }

    @Override
    public boolean has(String key) {
        return mMap.containsKey(key);
    }

    @Override
    public Set<String> keySet() {
        return mMap.keySet();
    }

    @Override
    public int length() {
        return mMap.size();
    }

    @Override
    protected void ensureExists(String key) throws SerializeException {
        if (mMap.get(key) == null) {
            throw new SerializeException("null value");
        }
    }

    @Override
    public int getType() {
        return TYPE_V8;
    }

    @Override
    public String toString() {
        return toJSONObject().toString();
    }

    // JSON.stringify would remove undefined values.
    // For compatibility, we need to remove the undefined values.
    // TODO: remove undefined values when transform V8Object to map
    private void removeUndefinedEntry() {
        Object undefined = V8.getUndefined();
        Iterator<Map.Entry<String, Object>> iterator = mMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Object> entry = iterator.next();
            if (undefined.equals(entry.getValue())) {
                iterator.remove();
            }
        }
    }
}

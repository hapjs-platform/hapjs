/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.jsruntime.serialize;

import com.eclipsesource.v8.utils.ArrayBuffer;
import com.eclipsesource.v8.utils.TypedArray;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class JSONSerializeObject extends AbstractSerializeObject {
    private JSONObject mJSONObject;

    public JSONSerializeObject(JSONObject jsonObject) {
        mJSONObject = jsonObject;
    }

    @Override
    public Object opt(String key) {
        Object value = mJSONObject.opt(key);
        if (value instanceof JSONObject) {
            value = new JSONSerializeObject((JSONObject) value);
        } else if (value instanceof JSONArray) {
            value = new JSONSerializeArray((JSONArray) value);
        }
        return value;
    }

    @Override
    public int optInt(String key, int defaultValue) {
        return mJSONObject.optInt(key, defaultValue);
    }

    @Override
    public long optLong(String key, long defaultValue) {
        return mJSONObject.optLong(key, defaultValue);
    }

    @Override
    public double optDouble(String key, double defaultValue) {
        return mJSONObject.optDouble(key, defaultValue);
    }

    @Override
    public boolean optBoolean(String key, boolean defaultValue) {
        return mJSONObject.optBoolean(key, defaultValue);
    }

    @Override
    public String optString(String key, String defaultValue) {
        return mJSONObject.optString(key, defaultValue);
    }

    @Override
    public ArrayBuffer optArrayBuffer(String key) {
        return null;
    }

    @Override
    public TypedArray optTypedArray(String key) {
        return null;
    }

    @Override
    public ByteBuffer optByteBuffer(String key) {
        return null;
    }

    @Override
    public TypedArrayProxy optTypedArrayProxy(String key) {
        return null;
    }

    @Override
    public SerializeObject optSerializeObject(String key) {
        Object value = mJSONObject.opt(key);
        if (value instanceof JSONObject) {
            return new JSONSerializeObject((JSONObject) value);
        } else {
            return null;
        }
    }

    @Override
    public SerializeArray optSerializeArray(String key) {
        Object value = mJSONObject.opt(key);
        if (value instanceof JSONArray) {
            return new JSONSerializeArray((JSONArray) value);
        } else {
            return null;
        }
    }

    @Override
    public HandlerObject optHandlerObject(String key) {
        return null;
    }

    @Override
    public SerializeObject put(String key, int value) {
        try {
            mJSONObject.put(key, value);
        } catch (JSONException e) {
            // ignore
        }
        return this;
    }

    @Override
    public SerializeObject put(String key, double value) {
        try {
            mJSONObject.put(key, value);
        } catch (JSONException e) {
            // ignore
        }
        return this;
    }

    @Override
    public SerializeObject put(String key, long value) {
        try {
            mJSONObject.put(key, value);
        } catch (JSONException e) {
            // ignore
        }
        return this;
    }

    @Override
    public SerializeObject put(String key, boolean value) {
        try {
            mJSONObject.put(key, value);
        } catch (JSONException e) {
            // ignore
        }
        return this;
    }

    @Override
    public SerializeObject put(String key, String value) {
        try {
            mJSONObject.put(key, value);
        } catch (JSONException e) {
            // ignore
        }
        return this;
    }

    @Override
    public SerializeObject put(String key, ByteBuffer value) {
        throw new UnsupportedOperationException("Can't insert ByteBuffer");
    }

    @Override
    public SerializeObject put(String key, TypedArrayProxy value) {
        throw new UnsupportedOperationException("Can't insert TypedArrayProxy");
    }

    @Override
    public SerializeObject put(String key, SerializeObject value) {
        try {
            mJSONObject.put(key, value.toJSONObject());
        } catch (JSONException e) {
            // ignore
        }
        return this;
    }

    @Override
    public SerializeObject put(String key, SerializeArray value) {
        try {
            mJSONObject.put(key, value.toJSONArray());
        } catch (JSONException e) {
            // ignore
        }
        return this;
    }

    @Override
    public SerializeObject put(String key, HandlerObject value) {
        throw new UnsupportedOperationException("Can't insert HandlerObject");
    }

    @Override
    public SerializeObject put(String key, byte[] value) {
        throw new UnsupportedOperationException("Can't insert byte array");
    }

    @Override
    public Object remove(String key) {
        Object value = opt(key);
        mJSONObject.remove(key);
        return value;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>();
        try {
            Iterator<String> iterator = mJSONObject.keys();
            while (iterator.hasNext()) {
                String key = iterator.next();
                Object value = mJSONObject.get(key);
                if (value instanceof JSONObject) {
                    result.put(key, new JSONSerializeObject((JSONObject) value).toMap());
                } else if (value instanceof JSONArray) {
                    result.put(key, new JSONSerializeArray((JSONArray) value).toList());
                } else if (value == JSONObject.NULL) {
                    result.put(key, null);
                } else {
                    result.put(key, value);
                }
            }
        } catch (JSONException e) {
            // ignore
        }
        return result;
    }

    @Override
    public JSONObject toJSONObject() {
        return mJSONObject;
    }

    @Override
    public boolean has(String key) {
        return mJSONObject.has(key);
    }

    @Override
    public Set<String> keySet() {
        Set<String> keySet = new HashSet<>();
        Iterator<String> keys = mJSONObject.keys();
        while (keys.hasNext()) {
            keySet.add(keys.next());
        }
        return keySet;
    }

    @Override
    public int length() {
        return mJSONObject.length();
    }

    @Override
    protected void ensureExists(String key) throws SerializeException {
        try {
            if (mJSONObject.get(key) == null) {
                throw new SerializeException();
            }
        } catch (JSONException e) {
            throw new SerializeException(e);
        }
    }

    @Override
    public int getType() {
        return TYPE_JSON;
    }
}

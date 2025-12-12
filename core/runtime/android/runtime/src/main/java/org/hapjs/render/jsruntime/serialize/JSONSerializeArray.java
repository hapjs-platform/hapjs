/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.jsruntime.serialize;

import com.eclipsesource.v8.utils.ArrayBuffer;
import com.eclipsesource.v8.utils.TypedArray;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class JSONSerializeArray extends AbstractSerializeArray {
    private JSONArray mJSONArray;

    public JSONSerializeArray(JSONArray jsonArray) {
        mJSONArray = jsonArray;
    }

    @Override
    public Object opt(int index) {
        Object value = mJSONArray.opt(index);
        if (value instanceof JSONObject) {
            value = new JSONSerializeObject((JSONObject) value);
        } else if (value instanceof JSONArray) {
            value = new JSONSerializeArray((JSONArray) value);
        }
        return value;
    }

    @Override
    public int optInt(int index, int defaultValue) {
        return mJSONArray.optInt(index, defaultValue);
    }

    @Override
    public long optLong(int index, long defaultValue) {
        return mJSONArray.optLong(index, defaultValue);
    }

    @Override
    public double optDouble(int index, double defaultValue) {
        return mJSONArray.optDouble(index, defaultValue);
    }

    @Override
    public boolean optBoolean(int index, boolean defaultValue) {
        return mJSONArray.optBoolean(index, defaultValue);
    }

    @Override
    public String optString(int index, String defaultValue) {
        return mJSONArray.optString(index, defaultValue);
    }

    @Override
    public ArrayBuffer optArrayBuffer(int index) {
        return null;
    }

    @Override
    public TypedArray optTypedArray(int index) {
        return null;
    }

    @Override
    public ByteBuffer optByteBuffer(int index) {
        return null;
    }

    @Override
    public TypedArrayProxy optTypedArrayProxy(int index) {
        return null;
    }

    @Override
    public SerializeObject optSerializeObject(int index) {
        Object value = mJSONArray.opt(index);
        if (value instanceof JSONObject) {
            return new JSONSerializeObject((JSONObject) value);
        } else {
            return null;
        }
    }

    @Override
    public SerializeArray optSerializeArray(int index) {
        Object value = mJSONArray.opt(index);
        if (value instanceof JSONArray) {
            return new JSONSerializeArray((JSONArray) value);
        } else {
            return null;
        }
    }

    @Override
    public SerializeArray put(int value) {
        mJSONArray.put(value);
        return this;
    }

    @Override
    public SerializeArray put(double value) {
        try {
            mJSONArray.put(value);
        } catch (JSONException e) {
            // ignore
        }
        return this;
    }

    @Override
    public SerializeArray put(long value) {
        mJSONArray.put(value);
        return this;
    }

    @Override
    public SerializeArray put(boolean value) {
        mJSONArray.put(value);
        return this;
    }

    @Override
    public SerializeArray put(String value) {
        mJSONArray.put(value);
        return this;
    }

    @Override
    public SerializeArray put(ByteBuffer value) {
        throw new UnsupportedOperationException("Can't insert ByteBuffer");
    }

    @Override
    public SerializeArray put(TypedArrayProxy value) {
        throw new UnsupportedOperationException("Can't insert TypedArrayProxy");
    }

    @Override
    public SerializeArray put(SerializeObject value) {
        mJSONArray.put(value.toJSONObject());
        return this;
    }

    @Override
    public SerializeArray put(SerializeArray value) {
        mJSONArray.put(value.toJSONArray());
        return this;
    }

    @Override
    public Object remove(int index) {
        Object value = opt(index);
        mJSONArray.remove(index);
        return value;
    }

    @Override
    public List<Object> toList() {
        List<Object> result = new ArrayList<>();
        try {
            for (int i = 0; i < mJSONArray.length(); ++i) {
                Object value = mJSONArray.get(i);
                if (value instanceof JSONObject) {
                    result.add(new JSONSerializeObject((JSONObject) value).toMap());
                } else if (value instanceof JSONArray) {
                    result.add(new JSONSerializeArray((JSONArray) value).toList());
                } else if (value == JSONObject.NULL) {
                    result.add(null);
                } else {
                    result.add(value);
                }
            }
        } catch (JSONException e) {
            // ignore
        }
        return result;
    }

    @Override
    public JSONArray toJSONArray() {
        return mJSONArray;
    }

    @Override
    public int length() {
        return mJSONArray.length();
    }

    @Override
    protected void ensureExists(int index) throws SerializeException {
        if (index < 0 || index >= mJSONArray.length()) {
            throw new SerializeException("index out of range");
        }
    }

    @Override
    public int getType() {
        return TYPE_JSON;
    }
}

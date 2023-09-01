/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.jsruntime.serialize;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.utils.ArrayBuffer;
import com.eclipsesource.v8.utils.TypedArray;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;

class V8SerializeArray extends AbstractSerializeArray {
    private List<Object> mList;

    public V8SerializeArray(List<Object> list) {
        mList = list;
        replaceUndefinedItem();
    }

    @Override
    public Object opt(int index) {
        if (index < 0 || index >= mList.size()) {
            return null;
        }

        Object value = mList.get(index);
        if (value instanceof Map) {
            value = new V8SerializeObject((Map<String, Object>) value);
        } else if (value instanceof List) {
            value = new V8SerializeArray((List<Object>) value);
        }
        return value;
    }

    @Override
    public int optInt(int index, int defaultValue) {
        Object value = opt(index);
        return SerializeHelper.toInteger(value, defaultValue);
    }

    @Override
    public long optLong(int index, long defaultValue) {
        Object value = opt(index);
        return SerializeHelper.toLong(value, defaultValue);
    }

    @Override
    public double optDouble(int index, double defaultValue) {
        Object value = opt(index);
        return SerializeHelper.toDouble(value, defaultValue);
    }

    @Override
    public boolean optBoolean(int index, boolean defaultValue) {
        Object value = opt(index);
        return SerializeHelper.toBoolean(value, defaultValue);
    }

    @Override
    public String optString(int index, String defaultValue) {
        Object value = opt(index);
        return SerializeHelper.toString(value, defaultValue);
    }

    @Override
    public ArrayBuffer optArrayBuffer(int index) {
        Object value = opt(index);
        if (value instanceof ArrayBuffer) {
            return (ArrayBuffer) value;
        } else {
            return null;
        }
    }

    @Override
    public TypedArray optTypedArray(int index) {
        Object value = opt(index);
        if (value instanceof TypedArray) {
            return (TypedArray) value;
        } else {
            return null;
        }
    }

    @Override
    public ByteBuffer optByteBuffer(int index) {
        Object value = opt(index);
        if (value instanceof ByteBuffer) {
            return (ByteBuffer) value;
        } else {
            return null;
        }
    }

    @Override
    public TypedArrayProxy optTypedArrayProxy(int index) {
        Object value = opt(index);
        if (value instanceof TypedArrayProxy) {
            return (TypedArrayProxy) value;
        } else {
            return null;
        }
    }

    @Override
    public SerializeObject optSerializeObject(int index) {
        Object value = mList.get(index);
        if (value instanceof Map) {
            return new V8SerializeObject((Map<String, Object>) value);
        } else {
            return null;
        }
    }

    @Override
    public SerializeArray optSerializeArray(int index) {
        Object value = mList.get(index);
        if (value instanceof List) {
            return new V8SerializeArray((List<Object>) value);
        } else {
            return null;
        }
    }

    @Override
    public SerializeArray put(int value) {
        mList.add(value);
        return this;
    }

    @Override
    public SerializeArray put(double value) {
        mList.add(value);
        return this;
    }

    @Override
    public SerializeArray put(long value) {
        mList.add(value);
        return this;
    }

    @Override
    public SerializeArray put(boolean value) {
        mList.add(value);
        return this;
    }

    @Override
    public SerializeArray put(String value) {
        mList.add(value);
        return this;
    }

    @Override
    public SerializeArray put(ByteBuffer value) {
        mList.add(value);
        return this;
    }

    @Override
    public SerializeArray put(TypedArrayProxy value) {
        mList.add(value);
        return this;
    }

    @Override
    public SerializeArray put(SerializeObject value) {
        mList.add(value.toMap());
        return this;
    }

    @Override
    public SerializeArray put(SerializeArray value) {
        mList.add(value.toList());
        return this;
    }

    @Override
    public Object remove(int index) {
        Object value = opt(index);
        if (index >= 0 && index < mList.size()) {
            mList.remove(index);
        }
        return value;
    }

    @Override
    public List<Object> toList() {
        return mList;
    }

    @Override
    public JSONArray toJSONArray() {
        JSONArray result = new JSONArray();
        try {
            for (Object value : mList) {
                if (value instanceof Integer) {
                    result.put(((Integer) value).intValue());
                } else if (value instanceof Long) {
                    result.put(((Long) value).longValue());
                } else if (value instanceof Double) {
                    result.put(((Double) value).doubleValue());
                } else if (value instanceof Boolean) {
                    result.put(((Boolean) value).booleanValue());
                } else if (value instanceof String) {
                    result.put(value);
                } else if (value instanceof ArrayBuffer) {
                    // ignore
                } else if (value instanceof TypedArray) {
                    // ignore
                } else if (value instanceof ByteBuffer) {
                    // ignore
                } else if (value instanceof TypedArrayProxy) {
                    // ignore
                } else if (value instanceof Map) {
                    result.put(new V8SerializeObject((Map<String, Object>) value).toJSONObject());
                } else if (value instanceof List) {
                    result.put(new V8SerializeArray((List<Object>) value).toJSONArray());
                }
            }
        } catch (JSONException e) {
            // ignore
        }
        return result;
    }

    @Override
    public int length() {
        return mList.size();
    }

    @Override
    protected void ensureExists(int index) throws SerializeException {
        if (index < 0 || index >= mList.size()) {
            throw new SerializeException("index out of range");
        }
        if (mList.get(index) == null) {
            throw new SerializeException("null value");
        }
    }

    @Override
    public int getType() {
        return TYPE_V8;
    }

    @Override
    public String toString() {
        return toJSONArray().toString();
    }

    // JSON.stringify would replace undefined values with null.
    // For compatibility, we need to replace the undefined values.
    // TODO: remove undefined values when transform V8Object to map
    private void replaceUndefinedItem() {
        Object undefined = V8.getUndefined();
        final int SIZE = mList.size();
        for (int i = 0; i < SIZE; ++i) {
            if (undefined.equals(mList.get(i))) {
                mList.set(i, null);
            }
        }
    }
}

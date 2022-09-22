/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.jsruntime.serialize;

import android.util.Log;
import android.util.SparseArray;
import com.eclipsesource.v8.utils.typedarrays.ArrayBuffer;
import com.eclipsesource.v8.utils.typedarrays.TypedArray;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;

public class JavaSerializeArray implements SerializeArray {
    private static final String TAG = "JavaSerializeArray";

    private SerializeArray mObject;
    private SparseArray<Object> mOverlayMap;

    public JavaSerializeArray() {
        this(new JSONArray());
    }

    public JavaSerializeArray(List<Object> list) {
        this(new V8SerializeArray(list));
    }

    public JavaSerializeArray(JSONArray jsonArray) {
        this(new JSONSerializeArray(jsonArray));
    }

    private JavaSerializeArray(SerializeArray array) {
        mObject = array;
        mOverlayMap = new SparseArray<>();
    }

    @Override
    public Object get(int index) throws SerializeException {
        Object value = mOverlayMap.get(index);
        if (value != null) {
            return value;
        } else {
            return mObject.get(index);
        }
    }

    @Override
    public Object opt(int index) {
        Object value = mOverlayMap.get(index);
        if (value != null) {
            return value;
        } else {
            return mObject.opt(index);
        }
    }

    @Override
    public int getInt(int index) throws SerializeException {
        return mObject.getInt(index);
    }

    @Override
    public int optInt(int index) {
        return mObject.optInt(index);
    }

    @Override
    public int optInt(int index, int defaultValue) {
        return mObject.optInt(index, defaultValue);
    }

    @Override
    public long getLong(int index) throws SerializeException {
        return mObject.getLong(index);
    }

    @Override
    public long optLong(int index) {
        return mObject.optLong(index);
    }

    @Override
    public long optLong(int index, long defaultValue) {
        return mObject.optLong(index, defaultValue);
    }

    @Override
    public double getDouble(int index) throws SerializeException {
        return mObject.getDouble(index);
    }

    @Override
    public double optDouble(int index) {
        return mObject.optDouble(index);
    }

    @Override
    public double optDouble(int index, double defaultValue) {
        return mObject.optDouble(index, defaultValue);
    }

    @Override
    public boolean getBoolean(int index) throws SerializeException {
        return mObject.getBoolean(index);
    }

    @Override
    public boolean optBoolean(int index) {
        return mObject.optBoolean(index);
    }

    @Override
    public boolean optBoolean(int index, boolean defaultValue) {
        return mObject.optBoolean(index, defaultValue);
    }

    @Override
    public String getString(int index) throws SerializeException {
        return mObject.getString(index);
    }

    @Override
    public String optString(int index) {
        return mObject.optString(index);
    }

    @Override
    public String optString(int index, String defaultValue) {
        return mObject.optString(index, defaultValue);
    }

    @Override
    public ArrayBuffer getArrayBuffer(int index) throws SerializeException {
        return mObject.getArrayBuffer(index);
    }

    @Override
    public ArrayBuffer optArrayBuffer(int index) {
        return mObject.optArrayBuffer(index);
    }

    @Override
    public TypedArray getTypedArray(int index) throws SerializeException {
        return mObject.getTypedArray(index);
    }

    @Override
    public TypedArray optTypedArray(int index) {
        return mObject.optTypedArray(index);
    }

    @Override
    public SerializeObject getSerializeObject(int index) throws SerializeException {
        Object value = mOverlayMap.get(index);
        if (value instanceof SerializeObject) {
            return (SerializeObject) value;
        } else {
            SerializeObject result = mObject.getSerializeObject(index);
            mOverlayMap.put(index, result);
            return result;
        }
    }

    @Override
    public SerializeObject optSerializeObject(int index) {
        Object value = mOverlayMap.get(index);
        if (value instanceof SerializeObject) {
            return (SerializeObject) value;
        } else {
            SerializeObject result = mObject.optSerializeObject(index);
            if (result != null) {
                mOverlayMap.put(index, result);
            }
            return result;
        }
    }

    @Override
    public SerializeArray getSerializeArray(int index) throws SerializeException {
        Object value = mOverlayMap.get(index);
        if (value instanceof SerializeArray) {
            return (SerializeArray) value;
        } else {
            SerializeArray result = mObject.getSerializeArray(index);
            mOverlayMap.put(index, result);
            return result;
        }
    }

    @Override
    public SerializeArray optSerializeArray(int index) {
        Object value = mOverlayMap.get(index);
        if (value instanceof SerializeArray) {
            return (SerializeArray) value;
        } else {
            SerializeArray result = mObject.optSerializeArray(index);
            if (result != null) {
                mOverlayMap.put(index, result);
            }
            return result;
        }
    }

    @Override
    public SerializeArray put(int value) {
        mObject.put(value);
        return this;
    }

    @Override
    public SerializeArray put(double value) {
        mObject.put(value);
        return this;
    }

    @Override
    public SerializeArray put(long value) {
        mObject.put(value);
        return this;
    }

    @Override
    public SerializeArray put(boolean value) {
        mObject.put(value);
        return this;
    }

    @Override
    public SerializeArray put(String value) {
        mObject.put(value);
        return this;
    }

    @Override
    public SerializeArray put(ArrayBuffer value) {
        if (mObject instanceof JSONSerializeArray) {
            mObject = new V8SerializeArray(mObject.toList());
        }
        mObject.put(value);
        return this;
    }

    @Override
    public SerializeArray put(TypedArray value) {
        if (mObject instanceof JSONSerializeArray) {
            mObject = new V8SerializeArray(mObject.toList());
        }
        mObject.put(value);
        return this;
    }

    @Override
    public SerializeArray put(SerializeObject value) {
        if (value != null) {
            if (value.getType() == Serializable.TYPE_V8 && mObject instanceof JSONSerializeArray) {
                mObject = new V8SerializeArray(mObject.toList());
            }
            mOverlayMap.put(mObject.length(), value);
            mObject.put(value);
        }
        return this;
    }

    @Override
    public SerializeArray put(SerializeArray value) {
        if (value != null) {
            if (value.getType() == Serializable.TYPE_V8 && mObject instanceof JSONSerializeArray) {
                mObject = new V8SerializeArray(mObject.toList());
            }
            mOverlayMap.put(mObject.length(), value);
            mObject.put(value);
        }
        return this;
    }

    @Override
    public Object remove(int index) {
        Object value = mOverlayMap.get(index);
        if (value == null) {
            value = mObject.remove(index);
        } else {
            mOverlayMap.remove(index);
        }
        return value;
    }

    @Override
    public List<Object> toList() {
        List<Object> result = mObject.toList();
        for (int i = 0; i < mOverlayMap.size(); ++i) {
            int index = mOverlayMap.keyAt(i);
            Object value = mOverlayMap.valueAt(i);
            if (value instanceof SerializeObject) {
                result.set(index, ((SerializeObject) value).toMap());
            } else if (value instanceof SerializeArray) {
                result.set(index, ((SerializeArray) value).toList());
            } else {
                throw new IllegalStateException("Never get here");
            }
        }
        return result;
    }

    @Override
    public JSONArray toJSONArray() {
        JSONArray result = mObject.toJSONArray();
        for (int i = 0; i < mOverlayMap.size(); ++i) {
            int index = mOverlayMap.keyAt(i);
            Object value = mOverlayMap.valueAt(i);
            try {
                if (value instanceof SerializeObject) {
                    result.put(index, ((SerializeObject) value).toJSONObject());
                } else if (value instanceof SerializeArray) {
                    result.put(index, ((SerializeArray) value).toJSONArray());
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
    public int length() {
        return mObject.length();
    }

    @Override
    public int getType() {
        return mObject.getType();
    }

    @Override
    public String toString() {
        JSONArray jsonArray = null;
        try {
            jsonArray = toJSONArray();
        } catch (Exception e) {
            Log.e(TAG, "json error", e);
        }
        return jsonArray != null ? jsonArray.toString() : super.toString();
    }
}

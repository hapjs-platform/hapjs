/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.jsruntime.serialize;

import android.util.Log;

import com.eclipsesource.v8.utils.typedarrays.ArrayBuffer;
import com.eclipsesource.v8.utils.typedarrays.TypedArray;

import org.json.JSONObject;

abstract class AbstractSerializeObject implements SerializeObject {
    private static final String TAG = "AbstractSerializeObject";

    @Override
    public final Object get(String key) throws SerializeException {
        ensureExists(key);
        return opt(key);
    }

    @Override
    public final int getInt(String key) throws SerializeException {
        ensureExists(key);
        return optInt(key);
    }

    @Override
    public final int optInt(String key) {
        return optInt(key, 0);
    }

    @Override
    public final long getLong(String key) throws SerializeException {
        ensureExists(key);
        return optLong(key);
    }

    @Override
    public final long optLong(String key) {
        return optLong(key, 0L);
    }

    @Override
    public final double getDouble(String key) throws SerializeException {
        ensureExists(key);
        return optDouble(key);
    }

    @Override
    public final double optDouble(String key) {
        return optDouble(key, 0D);
    }

    @Override
    public final boolean getBoolean(String key) throws SerializeException {
        ensureExists(key);
        return optBoolean(key);
    }

    @Override
    public final boolean optBoolean(String key) {
        return optBoolean(key, false);
    }

    @Override
    public final String getString(String key) throws SerializeException {
        ensureExists(key);
        return optString(key);
    }

    @Override
    public final String optString(String key) {
        return optString(key, "");
    }

    @Override
    public final ArrayBuffer getArrayBuffer(String key) throws SerializeException {
        ensureExists(key);
        return optArrayBuffer(key);
    }

    @Override
    public final TypedArray getTypedArray(String key) throws SerializeException {
        ensureExists(key);
        return optTypedArray(key);
    }

    @Override
    public final SerializeObject getSerializeObject(String key) throws SerializeException {
        ensureExists(key);
        return optSerializeObject(key);
    }

    @Override
    public final SerializeArray getSerializeArray(String key) throws SerializeException {
        ensureExists(key);
        return optSerializeArray(key);
    }

    @Override
    public HandlerObject getHandlerObject(String key) throws SerializeException {
        ensureExists(key);
        return optHandlerObject(key);
    }

    protected abstract void ensureExists(String key) throws SerializeException;

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

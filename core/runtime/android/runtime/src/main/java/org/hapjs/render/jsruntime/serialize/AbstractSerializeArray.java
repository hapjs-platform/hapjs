/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.jsruntime.serialize;

import com.eclipsesource.v8.utils.typedarrays.ArrayBuffer;
import com.eclipsesource.v8.utils.typedarrays.TypedArray;

abstract class AbstractSerializeArray implements SerializeArray {
    @Override
    public Object get(int index) throws SerializeException {
        ensureExists(index);
        return opt(index);
    }

    @Override
    public int getInt(int index) throws SerializeException {
        ensureExists(index);
        return optInt(index);
    }

    @Override
    public int optInt(int index) {
        return optInt(index, 0);
    }

    @Override
    public long getLong(int index) throws SerializeException {
        ensureExists(index);
        return optLong(index);
    }

    @Override
    public long optLong(int index) {
        return optLong(index, 0L);
    }

    @Override
    public double getDouble(int index) throws SerializeException {
        ensureExists(index);
        return optDouble(index);
    }

    @Override
    public double optDouble(int index) {
        return optDouble(index, 0D);
    }

    @Override
    public boolean getBoolean(int index) throws SerializeException {
        ensureExists(index);
        return optBoolean(index);
    }

    @Override
    public boolean optBoolean(int index) {
        return optBoolean(index, false);
    }

    @Override
    public String getString(int index) throws SerializeException {
        ensureExists(index);
        return optString(index);
    }

    @Override
    public String optString(int index) {
        return optString(index, "");
    }

    @Override
    public ArrayBuffer getArrayBuffer(int index) throws SerializeException {
        ensureExists(index);
        return optArrayBuffer(index);
    }

    @Override
    public TypedArray getTypedArray(int index) throws SerializeException {
        ensureExists(index);
        return optTypedArray(index);
    }

    @Override
    public SerializeObject getSerializeObject(int index) throws SerializeException {
        ensureExists(index);
        return optSerializeObject(index);
    }

    @Override
    public SerializeArray getSerializeArray(int index) throws SerializeException {
        ensureExists(index);
        return optSerializeArray(index);
    }

    protected abstract void ensureExists(int index) throws SerializeException;
}

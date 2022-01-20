/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.jsruntime.serialize;

import com.eclipsesource.v8.utils.typedarrays.ArrayBuffer;
import com.eclipsesource.v8.utils.typedarrays.TypedArray;
import java.util.List;
import org.json.JSONArray;

public interface SerializeArray extends Serializable {
    Object get(int index) throws SerializeException;

    Object opt(int index);

    int getInt(int index) throws SerializeException;

    int optInt(int index);

    int optInt(int index, int defaultValue);

    long getLong(int index) throws SerializeException;

    long optLong(int index);

    long optLong(int index, long defaultValue);

    double getDouble(int index) throws SerializeException;

    double optDouble(int index);

    double optDouble(int index, double defaultValue);

    boolean getBoolean(int index) throws SerializeException;

    boolean optBoolean(int index);

    boolean optBoolean(int index, boolean defaultValue);

    String getString(int index) throws SerializeException;

    String optString(int index);

    String optString(int index, String defaultValue);

    ArrayBuffer getArrayBuffer(int index) throws SerializeException;

    ArrayBuffer optArrayBuffer(int index);

    TypedArray getTypedArray(int index) throws SerializeException;

    TypedArray optTypedArray(int index);

    SerializeObject getSerializeObject(int index) throws SerializeException;

    SerializeObject optSerializeObject(int index);

    SerializeArray getSerializeArray(int index) throws SerializeException;

    SerializeArray optSerializeArray(int index);

    SerializeArray put(int value);

    SerializeArray put(double value);

    SerializeArray put(long value);

    SerializeArray put(boolean value);

    SerializeArray put(String value);

    SerializeArray put(ArrayBuffer value);

    SerializeArray put(TypedArray value);

    SerializeArray put(SerializeObject value);

    SerializeArray put(SerializeArray value);

    Object remove(int index);

    List<Object> toList();

    JSONArray toJSONArray();

    int length();
}

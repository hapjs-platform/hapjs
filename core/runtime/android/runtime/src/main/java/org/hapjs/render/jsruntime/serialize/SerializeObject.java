/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.jsruntime.serialize;

import com.eclipsesource.v8.utils.ArrayBuffer;
import com.eclipsesource.v8.utils.TypedArray;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Set;
import org.json.JSONObject;

public interface SerializeObject extends Serializable {
    Object get(String key) throws SerializeException;

    Object opt(String key);

    int getInt(String key) throws SerializeException;

    int optInt(String key);

    int optInt(String key, int defaultValue);

    long getLong(String key) throws SerializeException;

    long optLong(String key);

    long optLong(String key, long defaultValue);

    double getDouble(String key) throws SerializeException;

    double optDouble(String key);

    double optDouble(String key, double defaultValue);

    boolean getBoolean(String key) throws SerializeException;

    boolean optBoolean(String key);

    boolean optBoolean(String key, boolean defaultValue);

    String getString(String key) throws SerializeException;

    String optString(String key);

    String optString(String key, String defaultValue);

    ArrayBuffer getArrayBuffer(String key) throws SerializeException;

    ArrayBuffer optArrayBuffer(String key);

    TypedArray getTypedArray(String key) throws SerializeException;

    TypedArray optTypedArray(String key);
    ByteBuffer getByteBuffer(String key) throws SerializeException;
    ByteBuffer optByteBuffer(String key) ;

    TypedArrayProxy getTypedArrayProxy(String key) throws SerializeException;
    TypedArrayProxy optTypedArrayProxy(String key) ;

    SerializeObject getSerializeObject(String key) throws SerializeException;

    SerializeObject optSerializeObject(String key);

    SerializeArray getSerializeArray(String key) throws SerializeException;

    SerializeArray optSerializeArray(String key);

    HandlerObject getHandlerObject(String key) throws SerializeException;

    HandlerObject optHandlerObject(String key);

    SerializeObject put(String key, int value);

    SerializeObject put(String key, double value);

    SerializeObject put(String key, long value);

    SerializeObject put(String key, boolean value);

    SerializeObject put(String key, String value);

    SerializeObject put(String key, ByteBuffer value);

    SerializeObject put(String key, TypedArrayProxy value);

    SerializeObject put(String key, SerializeObject value);

    SerializeObject put(String key, SerializeArray value);

    SerializeObject put(String key, HandlerObject value);

    SerializeObject put(String key, byte[] value);

    Object remove(String key);

    Map<String, Object> toMap();

    JSONObject toJSONObject();

    boolean has(String key);

    Set<String> keySet();

    int length();
}

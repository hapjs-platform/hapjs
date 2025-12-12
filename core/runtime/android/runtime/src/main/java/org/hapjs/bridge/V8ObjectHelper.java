/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge;

import com.eclipsesource.v8.Releasable;
import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8ArrayBuffer;
import com.eclipsesource.v8.V8Object;
import com.eclipsesource.v8.V8TypedArray;
import com.eclipsesource.v8.V8Value;
import com.eclipsesource.v8.utils.ArrayBuffer;
import com.eclipsesource.v8.utils.TypedArray;
import com.eclipsesource.v8.utils.V8Map;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import org.hapjs.render.jsruntime.serialize.HandlerObject;
import org.hapjs.render.jsruntime.serialize.TypedArrayProxy;

public class V8ObjectHelper {
    private static final Object IGNORE = new Object();

    // 统一v8方法入口
    public static Map<String, ? super Object> toMap(V8Object object) {
        V8Map cache = new V8Map();
        try {
            return toMap(object, cache);
        } finally {
            cache.release();
        }
    }

    private static Map<String, ? super Object> toMap(V8Object v8Object, V8Map<Object> cache) {
        if (v8Object == null) {
            return Collections.emptyMap();
        } else if (cache.containsKey(v8Object)) {
            return (Map) cache.get(v8Object);
        } else {
            Map<String, ? super Object> result = new HashMap<>();
            cache.put(v8Object, result);
            String[] keys = v8Object.getKeys();

            for (int i = 0; i < keys.length; ++i) {
                String key = keys[i];
                Object object = null;

                try {
                    object = v8Object.get(key);
                    int type = v8Object.getType(key);
                    Object value = getValue(object, type, cache);
                    if (value != IGNORE) {
                        result.put(key, value);
                    }
                } finally {
                    if (object instanceof Releasable) {
                        ((Releasable) object).release();
                    }
                }
            }

            return result;
        }
    }

    private static Object getValue(Object value, int valueType, V8Map<Object> cache) {
        // return when value is null
        if (value == null) {
            return null;
        }
        switch (valueType) {
            case V8Value.NULL:
                return null;
            case V8Value.INT_32_ARRAY:
            case V8Value.DOUBLE:
            case V8Value.BOOLEAN:
            case V8Value.STRING:
                return value;
            case V8Value.V8_ARRAY:
                return toList((V8Array) value, cache);
            case V8Value.V8_OBJECT:
                return toMap((V8Object) value, cache);
            case V8Value.V8_FUNCTION:
                return IGNORE;
            case V8Value.V8_TYPED_ARRAY:
                return toTypedArray((V8Array) value);
            case V8Value.V8_ARRAY_BUFFER:
                V8ArrayBuffer v8ArrayBuffer = (V8ArrayBuffer) value;
                byte[] bytes = new byte[v8ArrayBuffer.remaining()];
                v8ArrayBuffer.get(bytes);
                return bytes;
            case V8Value.UNDEFINED:
                return V8.getUndefined();
            default:
                throw new IllegalStateException(
                        "Cannot convert type " + V8Value.getStringRepresentation(valueType));
        }
    }

    private static Object toTypedArray(V8Array typedArray) {
        int arrayType = typedArray.getType();

        switch (arrayType) {
            case V8Value.INT_32_ARRAY:
            case V8Value.DOUBLE:
            case V8Value.INT_8_ARRAY:
            case V8Value.UNSIGNED_INT_8_ARRAY:
            case V8Value.UNSIGNED_INT_8_CLAMPED_ARRAY:
            case V8Value.INT_16_ARRAY:
            case V8Value.UNSIGNED_INT_16_ARRAY:
            case V8Value.UNSIGNED_INT_32_ARRAY:
            case V8Value.FLOAT_32_ARRAY:
                V8ArrayBuffer v8ArrayBuffer = ((V8TypedArray) typedArray).getBuffer();
                byte[] bytes = new byte[v8ArrayBuffer.remaining()];
                v8ArrayBuffer.get(bytes);
                return new TypedArrayProxy(arrayType, bytes);
            case V8Value.BOOLEAN:
            case V8Value.STRING:
            case V8Value.V8_ARRAY:
            case V8Value.V8_OBJECT:
            case V8Value.V8_FUNCTION:
            case V8Value.V8_TYPED_ARRAY:
            case V8Value.V8_ARRAY_BUFFER:
            default:
                throw new IllegalStateException(
                        "Known Typed Array type: " + V8Value.getStringRepresentation(arrayType));
        }
    }

    private static List<? super Object> toList(V8Array array, V8Map<Object> cache) {
        if (array == null) {
            return Collections.emptyList();
        } else if (cache.containsKey(array)) {
            return (List) cache.get(array);
        } else {
            List<? super Object> result = new ArrayList();
            cache.put(array, result);

            for (int i = 0; i < array.length(); ++i) {
                Object object = null;

                try {
                    object = array.get(i);
                    int type = array.getType(i);
                    Object value = getValue(object, type, cache);
                    if (value != IGNORE) {
                        result.add(value);
                    }
                } finally {
                    if (object instanceof Releasable) {
                        ((Releasable) object).release();
                    }
                }
            }

            return result;
        }
    }

    public static V8Array toV8Array(V8 v8, List<? extends Object> list) {
        Map<Object, V8Value> cache = new Hashtable<>();
        try {
            return toV8Array(v8, list, cache).twin();
        } finally {
            for (V8Value v8Object : cache.values()) {
                v8Object.release();
            }
        }
    }

    private static V8Array toV8Array(V8 v8, List<? extends Object> list,
                                     Map<Object, V8Value> cache) {
        if (cache.containsKey(new V8ObjectHelper.ListWrapper(list))) {
            return (V8Array) cache.get(new V8ObjectHelper.ListWrapper(list));
        } else {
            V8Array result = new V8Array(v8);
            cache.put(new V8ObjectHelper.ListWrapper(list), result);

            try {
                for (int i = 0; i < list.size(); ++i) {
                    Object value = list.get(i);
                    pushValue(v8, result, value, cache);
                }

                return result;
            } catch (IllegalStateException e) {
                result.release();
                throw e;
            }
        }
    }

    public static V8Object toV8Object(final V8 v8, final Map<String, ? extends Object> map) {
        Map<Object, V8Value> cache = new Hashtable<>();
        try {
            return toV8Object(v8, map, cache).twin();
        } finally {
            for (V8Value v8Object : cache.values()) {
                v8Object.close();
            }
        }
    }

    private static V8Object toV8Object(
            final V8 v8, final Map<String, ? extends Object> map,
            final Map<Object, V8Value> cache) {
        if (cache.containsKey(map)) {
            return (V8Object) cache.get(map);
        }
        V8Object result = new V8Object(v8);
        cache.put(map, result);
        try {
            for (Map.Entry<String, ? extends Object> entry : map.entrySet()) {
                setValue(v8, result, entry.getKey(), entry.getValue(), cache);
            }
        } catch (IllegalStateException e) {
            result.release();
            throw e;
        }
        return result;
    }

    private static void setValue(
            final V8 v8,
            final V8Object result,
            final String key,
            final Object value,
            final Map<Object, V8Value> cache) {
        if (value == null) {
            result.addUndefined(key);
        } else if (value instanceof Integer) {
            result.add(key, (Integer) value);
        } else if (value instanceof Long) {
            result.add(key, (Long) value);
        } else if (value instanceof Double) {
            result.add(key, (Double) value);
        } else if (value instanceof Float) {
            result.add(key, (Float) value);
        } else if (value instanceof String) {
            result.add(key, (String) value);
        } else if (value instanceof Boolean) {
            result.add(key, (Boolean) value);
        } else if (value instanceof V8Object) {
            result.add(key, (V8Object) value);
        } else if (value instanceof TypedArray) {
            V8TypedArray typedArray = toV8TypedArray(v8, (TypedArray) value, cache);
            result.add(key, typedArray);
        } else if (value instanceof ArrayBuffer) {
            V8ArrayBuffer v8ArrayBuffer = toV8ArrayBuffer(v8, (ArrayBuffer) value, cache);
            result.add(key, v8ArrayBuffer);
        } else if (value instanceof TypedArrayProxy) {
            TypedArrayProxy typedArrayProxy = (TypedArrayProxy) value;
            ArrayBuffer arrayBuffer = new ArrayBuffer(v8, typedArrayProxy.getBuffer());
            TypedArray typedArray = new TypedArray(v8, arrayBuffer, typedArrayProxy.getType(), 0, typedArrayProxy.getBuffer().remaining());
            V8TypedArray v8TypedArray = toV8TypedArray(v8, typedArray, cache);
            result.add(key, v8TypedArray);
        } else if (value instanceof ByteBuffer) {
            V8ArrayBuffer v8ArrayBuffer = toV8ArrayBuffer(v8, (ByteBuffer) value, cache);
            result.add(key, v8ArrayBuffer);
        } else if (value instanceof byte[]) {
            V8ArrayBuffer v8ArrayBuffer = toV8ArrayBuffer(v8, (byte[]) value, cache);
            result.add(key, v8ArrayBuffer);
        } else if (value instanceof Map) {
            V8Object object = toV8Object(v8, (Map) value, cache);
            result.add(key, object);
        } else if (value instanceof List) {
            V8Array array = toV8Array(v8, (List) value, cache);
            result.add(key, array);
        } else if (value instanceof HandlerObject) {
            result.add(key, ((HandlerObject) value).toV8Object(v8));
        } else {
            throw new IllegalStateException("Unsupported Object of type: " + value.getClass());
        }
    }

    private static void pushValue(V8 v8, V8Array result, Object value, Map<Object, V8Value> cache) {

        if (value == null) {
            result.pushUndefined();
        } else if (value instanceof Integer) {
            result.push(value);
        } else if (value instanceof Long) {
            result.push(new Double((Long) value));
        } else if (value instanceof Double) {
            result.push(value);
        } else if (value instanceof Float) {
            result.push(value);
        } else if (value instanceof String) {
            result.push((String) value);
        } else if (value instanceof Boolean) {
            result.push(value);
        } else if (value instanceof V8Object) {
            result.push((V8Object) value);
        } else if (value instanceof ByteBuffer) {
            ArrayBuffer arrayBuffer = new ArrayBuffer(v8, (ByteBuffer) value);
            result.push(arrayBuffer);
        } else if (value instanceof TypedArrayProxy) {
            TypedArrayProxy typedArrayProxy = (TypedArrayProxy) value;
            ArrayBuffer arrayBuffer = new ArrayBuffer(v8, typedArrayProxy.getBuffer());
            TypedArray typedArray = new TypedArray(v8, arrayBuffer, typedArrayProxy.getType(), 0, typedArrayProxy.getBuffer().remaining());
            result.push(typedArray);
        } else if (value instanceof TypedArray) {
            V8TypedArray v8TypedArray = toV8TypedArray(v8, (TypedArray) value, cache);
            result.push(v8TypedArray);
        } else if (value instanceof ArrayBuffer) {
            V8ArrayBuffer v8ArrayBuffer = toV8ArrayBuffer(v8, (ArrayBuffer) value, cache);
            result.push(v8ArrayBuffer);
        } else if (value instanceof byte[]) {
            V8ArrayBuffer v8ArrayBuffer = toV8ArrayBuffer(v8, (byte[]) value, cache);
            result.push(v8ArrayBuffer);
        } else if (value instanceof Map) {
            V8Object object = toV8Object(v8, (Map) value, cache);
            result.push(object);
        } else if (value instanceof List) {
            V8Array array = toV8Array(v8, (List) value, cache);
            result.push(array);
        } else if (value instanceof HandlerObject) {
            result.push(((HandlerObject) value).toV8Object(v8));
        } else {
            throw new IllegalStateException("Unsupported Object of type: " + value.getClass());
        }
    }

    private static V8TypedArray toV8TypedArray(
            final V8 v8, final TypedArray typedArray, final Map<Object, V8Value> cache) {
        if (cache.containsKey(typedArray)) {
            return (V8TypedArray) cache.get(typedArray);
        }
        V8ArrayBuffer arrayBuffer = null;
        try {
            V8TypedArray v8TypedArray = typedArray.getV8TypedArray();
            arrayBuffer = v8TypedArray.getBuffer();
            cache.put(typedArray, v8TypedArray);
            return v8TypedArray;
        } finally {
            if (arrayBuffer != null) {
                arrayBuffer.release();
            }
        }
    }

    private static V8ArrayBuffer toV8ArrayBuffer(
            final V8 v8, final ArrayBuffer arrayBuffer, final Map<Object, V8Value> cache) {
        if (cache.containsKey(arrayBuffer)) {
            return (V8ArrayBuffer) cache.get(arrayBuffer);
        }
        V8ArrayBuffer result = arrayBuffer.getV8ArrayBuffer();
        cache.put(arrayBuffer, result);
        return result;
    }

    private static V8ArrayBuffer toV8ArrayBuffer(final V8 v8, final ByteBuffer byteBuffer, final Map<Object, V8Value> cache) {
        if (cache.containsKey(byteBuffer)) {
            return (V8ArrayBuffer) cache.get(byteBuffer);
        }
        V8ArrayBuffer result = new V8ArrayBuffer(v8, byteBuffer);
        cache.put(byteBuffer, result);
        return result;
    }

    private static V8ArrayBuffer toV8ArrayBuffer(final V8 v8, final byte[] array, final Map<Object, V8Value> cache) {
        if (cache.containsKey(array)) {
            return  (V8ArrayBuffer) cache.get(array);
        }

        ByteBuffer buff = ByteBuffer.allocateDirect(array.length);
        buff.put(array);
        buff.rewind();
        V8ArrayBuffer result = new V8ArrayBuffer(v8, buff);
        cache.put(array, result);
        return result;
    }

    static class ListWrapper {
        private List<? extends Object> list;

        public ListWrapper(List<? extends Object> list) {
            this.list = list;
        }

        public boolean equals(Object obj) {
            if (obj instanceof V8ObjectHelper.ListWrapper) {
                return ((V8ObjectHelper.ListWrapper) obj).list == this.list;
            } else {
                return false;
            }
        }

        public int hashCode() {
            return System.identityHashCode(this.list);
        }
    }
}

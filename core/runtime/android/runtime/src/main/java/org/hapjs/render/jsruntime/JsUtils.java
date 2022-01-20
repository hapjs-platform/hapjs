/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.jsruntime;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8ArrayBuffer;
import com.eclipsesource.v8.V8Object;
import com.eclipsesource.v8.V8Value;
import com.eclipsesource.v8.utils.typedarrays.ArrayBuffer;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsUtils {

    public static void registerAllPublicMethodsToRoot(V8Object v8Obj) {
        Class<?> cls = v8Obj.getClass();
        do {
            registerMethodsByClass(cls, v8Obj, true);
        } while ((cls = cls.getSuperclass()) != V8Object.class);
    }

    public static void registerAllPublicMethods(V8Object v8Obj) {
        Class<?> cls = v8Obj.getClass();
        do {
            registerMethodsByClass(cls, v8Obj, false);
        } while ((cls = cls.getSuperclass()) != V8Object.class);
    }

    private static void registerMethodsByClass(Class<?> cls, V8Object v8Obj, boolean isToRoot) {
        Method[] methods = cls.getDeclaredMethods();
        V8 v8 = v8Obj.getRuntime();
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            int modifiers = method.getModifiers();
            if (Modifier.isPublic(modifiers) && !Modifier.isStatic(modifiers)) {
                if (isToRoot) {
                    v8.registerJavaMethod(
                            v8Obj, method.getName(), method.getName(), method.getParameterTypes());
                } else {
                    v8Obj.registerJavaMethod(
                            v8Obj, method.getName(), method.getName(), method.getParameterTypes());
                }
            }
        }
    }

    public static String objToJson(V8Object obj) {
        if (obj.isReleased()) {
            return "[released]";
        }
        if (obj.isUndefined()) {
            return "[undefined]";
        }

        V8 v8 = obj.getRuntime();
        V8Object json = v8.getObject("JSON");
        if (json == null) {
            return "[null]";
        }
        V8Array parameters = new V8Array(v8).push(obj);
        try {
            return json.executeStringFunction("stringify", parameters);
        } finally {
            JsUtils.release(parameters, json);
        }
    }

    public static V8Object mapToV8Object(V8 v8, Map<?, ?> map) {
        if (map == null) {
            return null;
        }

        V8Object obj = new V8Object(v8);
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = entry.getKey().toString();
            Object val = entry.getValue();
            if (val instanceof Integer) {
                obj.add(key, (int) (Integer) val);
            } else if (val instanceof Double) {
                obj.add(key, (double) (Double) val);
            } else if (val instanceof Float) {
                obj.add(key, (double) (float) (Float) val);
            } else if (val instanceof Boolean) {
                obj.add(key, (boolean) (Boolean) val);
            } else if (val instanceof Map) {
                V8Object temp = mapToV8Object(v8, (Map<?, ?>) val);
                obj.add(key, temp);
                release(temp);
            } else if (val instanceof List) {
                List list = (List) val;
                V8Array array = new V8Array(v8);
                for (Object o : list) {
                    push(array, o);
                }
                obj.add(key, array);
                release(array);
            } else if (val == null) {
                obj.addNull(key);
            } else if (val instanceof ArrayBuffer) {
                V8ArrayBuffer nativeArrayBuffer =
                        new V8ArrayBuffer(v8, ((ArrayBuffer) val).getByteBuffer());
                obj.add(key, nativeArrayBuffer);
                release(nativeArrayBuffer);
            } else {
                obj.add(key, val.toString());
            }
        }
        return obj;
    }

    public static Map<String, String> v8ObjectToMap(V8Object obj, Map<String, String> map) {
        if (obj == null) {
            return map;
        }

        if (map == null) {
            map = new HashMap<>();
        }

        for (String key : obj.getKeys()) {
            map.put(key, obj.getString(key));
        }

        return map;
    }

    public static void push(V8Array array, Object object) {
        if (object instanceof Integer) {
            array.push((Integer) object);
        } else if (object instanceof Double) {
            array.push((Double) object);
        } else if (object instanceof Float) {
            array.push((Float) object);
        } else if (object instanceof Boolean) {
            array.push((Boolean) object);
        } else if (object instanceof String) {
            array.push((String) object);
        } else if (object instanceof Map) {
            V8Object obj = mapToV8Object(array.getRuntime(), (Map<?, ?>) object);
            array.push(obj);
            release(obj);
        }
    }

    public static String toJsBoolean(boolean value) {
        return value ? "true" : "false";
    }

    public static void release(V8Value value) {
        if (value != null && !value.isReleased()) {
            value.release();
        }
    }

    public static void release(V8Value value, V8Value other) {
        release(value);
        release(other);
    }

    public static void release(V8Value value, V8Value... others) {
        release(value);
        for (V8Value v : others) {
            release(v);
        }
    }
}

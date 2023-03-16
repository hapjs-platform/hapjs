/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.utils;

import android.text.TextUtils;
import android.util.Log;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ReflectUtils {

    private static final String TAG = "ReflectUtils";

    public static Object invokeMethod(
            String clsName,
            Object receiver,
            String methodName,
            Class[] parameterTypesArray,
            Object[] params) {
        try {
            if (!TextUtils.isEmpty(clsName) && !TextUtils.isEmpty(methodName)) {
                Class cls = Class.forName(clsName);
                Method method = cls.getMethod(methodName, parameterTypesArray);
                method.setAccessible(true);
                return method.invoke(receiver, params);
            } else {
                Log.e(TAG, "null of clsName or methodName");
            }
        } catch (Throwable throwable) {
            Log.e(TAG, "Throwable", throwable);
        }
        return null;
    }

    public static Object callStaticMethod(String className, String methodName, Class[] parameterTypes, Object... params) {
        try {
            Class clazz = Class.forName(className);
            return callStaticMethod(clazz, methodName, parameterTypes, params);
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static Object callStaticMethod(Class clazz, String methodName, Class[] parameterTypes, Object... params) {
        try {
            Method method = clazz.getDeclaredMethod(methodName, parameterTypes);
            method.setAccessible(true);
            method.invoke(null, params);
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    public static Object getObjectField(Object obj, String name) {
        try {
            Field field = obj.getClass().getField(name);
            field.setAccessible(true);
            return field.get(obj);
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    public static Object invokeDeclaredMethod(String clsName, Object receiver, String methodName,
                                              Class[] parameterTypesArray, Object[] params) {
        try {
            if (!TextUtils.isEmpty(clsName)) {
                Class cls = Class.forName(clsName);;
                if (null != cls && !TextUtils.isEmpty(methodName)) {
                    Method method = cls.getDeclaredMethod(methodName, parameterTypesArray);
                    if (null != method) {
                        method.setAccessible(true);
                        return method.invoke(receiver, params);
                    } else {
                        Log.e(TAG, "null of method");
                    }
                } else {
                    Log.e(TAG, "null of cls or methodName");
                }
            } else {
                Log.e(TAG, "null of clsName");
            }
        } catch (Throwable e) {
            Log.e(TAG, "Throwable", e);
        }
        return null;
    }
}

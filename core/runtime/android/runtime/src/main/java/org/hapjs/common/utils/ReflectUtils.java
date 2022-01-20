/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.utils;

import android.text.TextUtils;
import android.util.Log;
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
}

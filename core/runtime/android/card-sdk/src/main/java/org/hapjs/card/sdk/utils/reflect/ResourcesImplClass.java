/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.card.sdk.utils.reflect;

import android.content.res.AssetManager;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ResourcesImplClass {
    private static Class CLASS;
    private static Method getAssetsMethod;

    public static AssetManager getAssets(Object instance)
            throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        if (CLASS == null) {
            CLASS = Class.forName("android.content.res.ResourcesImpl");
        }
        if (getAssetsMethod == null) {
            getAssetsMethod = CLASS.getDeclaredMethod("getAssets");
        }
        return (AssetManager) getAssetsMethod.invoke(instance);
    }
}

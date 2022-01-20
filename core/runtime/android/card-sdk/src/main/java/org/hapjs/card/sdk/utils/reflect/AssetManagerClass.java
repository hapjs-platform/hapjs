/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.card.sdk.utils.reflect;

import android.content.res.AssetManager;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class AssetManagerClass {
    private static Method addAssetPathMethod;
    private static Method ensureStringBlocks;

    public static int addAssetPath(AssetManager instance, String path)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        if (addAssetPathMethod == null) {
            addAssetPathMethod = AssetManager.class.getMethod("addAssetPath", String.class);
        }
        return (int) addAssetPathMethod.invoke(instance, path);
    }

    public static void ensureStringBlocks(AssetManager instance)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        if (ensureStringBlocks == null) {
            ensureStringBlocks = AssetManager.class.getDeclaredMethod("ensureStringBlocks");
            ensureStringBlocks.setAccessible(true);
        }
        ensureStringBlocks.invoke(instance);
    }
}

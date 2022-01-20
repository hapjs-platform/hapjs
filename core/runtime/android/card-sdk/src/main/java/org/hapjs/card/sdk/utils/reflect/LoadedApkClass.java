/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.card.sdk.utils.reflect;

import java.lang.reflect.Field;

public class LoadedApkClass {
    private static Field splitResDirsField;

    public static Object getSplitResDirs(Object loadedApk)
            throws IllegalAccessException, NoSuchFieldException {
        if (splitResDirsField == null) {
            splitResDirsField = loadedApk.getClass().getDeclaredField("mSplitResDirs");
            splitResDirsField.setAccessible(true);
        }
        return splitResDirsField.get(loadedApk);
    }

    public static void setSplitResDirs(Object loadedApk, String[] splitResDirs)
            throws IllegalAccessException, NoSuchFieldException {
        if (splitResDirsField == null) {
            splitResDirsField = loadedApk.getClass().getDeclaredField("mSplitResDirs");
            splitResDirsField.setAccessible(true);
        }
        splitResDirsField.set(loadedApk, splitResDirs);
    }
}

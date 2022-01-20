/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.card.sdk.utils.reflect;

import android.content.res.CompatibilityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.IBinder;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class ResourcesManagerClass {
    private static Class CLASS;
    private static Method getInstanceMethod;
    private static Field resourceReferencesField;
    private static Method appendLibAssetForMainAssetPathMethod;
    private static Method getResourcesMethod;

    public static Object getInstance()
            throws NoSuchMethodException, ClassNotFoundException, InvocationTargetException, IllegalAccessException {
        if (CLASS == null) {
            CLASS = Class.forName("android.app.ResourcesManager");
        }
        if (getInstanceMethod == null) {
            getInstanceMethod = CLASS.getDeclaredMethod("getInstance");
        }
        return getInstanceMethod.invoke(null);
    }

    public static ArrayList<WeakReference<Resources>> getResourceReferences(Object instance)
            throws IllegalAccessException, ClassNotFoundException, NoSuchFieldException {
        if (CLASS == null) {
            CLASS = Class.forName("android.app.ResourcesManager");
        }
        if (resourceReferencesField == null) {
            resourceReferencesField = CLASS.getDeclaredField("mResourceReferences");
            resourceReferencesField.setAccessible(true);
        }
        return (ArrayList<WeakReference<Resources>>) resourceReferencesField.get(instance);
    }

    public static void appendLibAssetForMainAssetPath(Object resourcesManager, String assetPath, String libAsset)
            throws NoSuchMethodException, ClassNotFoundException, InvocationTargetException, IllegalAccessException {
        if (CLASS == null) {
            CLASS = Class.forName("android.app.ResourcesManager");
        }
        if (appendLibAssetForMainAssetPathMethod == null) {
            appendLibAssetForMainAssetPathMethod =
                    CLASS.getDeclaredMethod("appendLibAssetForMainAssetPath", String.class, String.class);
        }
        appendLibAssetForMainAssetPathMethod.invoke(resourcesManager, assetPath, libAsset);
    }

    public static Object getResources(Object resourcesManager,
                                      IBinder activityToken,
                                      String resDir,
                                      String[] splitResDirs,
                                      String[] overlayDirs,
                                      String[] libDirs,
                                      int displayId,
                                      Configuration overrideConfig,
                                      CompatibilityInfo compatInfo,
                                      ClassLoader classLoader)
            throws NoSuchMethodException, ClassNotFoundException, InvocationTargetException, IllegalAccessException {
        if (CLASS == null) {
            CLASS = Class.forName("android.app.ResourcesManager");
        }
        if (getResourcesMethod == null) {
            getResourcesMethod = CLASS.getDeclaredMethod("getResources", IBinder.class,
                    String.class, String[].class, String[].class, String[].class, int.class,
                    Configuration.class, CompatibilityInfo.class, ClassLoader.class);
        }
        return getResourcesMethod.invoke(resourcesManager, activityToken, resDir, splitResDirs,
                overlayDirs, libDirs, displayId, overrideConfig, compatInfo, classLoader);
    }
}

/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.card.sdk.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import org.hapjs.card.common.utils.CardConfigHelper;
import org.hapjs.card.sdk.utils.reflect.AssetManagerClass;
import org.hapjs.card.sdk.utils.reflect.ResourcesClass;

public class ResourceInjector {
    private static final String TAG = "ResourceInjector";

    private static final Set<Object> sInjectedContexts =
            Collections.newSetFromMap(new WeakHashMap<Object, Boolean>());

    public static synchronized boolean inject(Context context) {
        Object key = getResourcesKey(context);
        if (key == null) {
            Log.e(TAG, "key is null. context=" + context);
            return false;
        }

        if (sInjectedContexts.contains(key)) {
            Log.d(TAG, "resources already injected. context=" + context);
            return true;
        }

        String platformPackage = CardConfigHelper.getPlatform(context);
        if (TextUtils.isEmpty(platformPackage)) {
            Log.w(TAG, "Fail to inject resource, platform package: " + platformPackage);
            return false;
        }

        String path;
        try {
            path = context.getPackageManager()
                    .getApplicationInfo(platformPackage, 0)
                    .sourceDir;
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Fail to inject resource", e);
            return false;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            addAssetPathBellowL(context, path);
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            addAssetPathAboveL(context, path);
        } else {
            addAssetPathAboveO(context, platformPackage);
        }

        Object newKey = getResourcesKey(context);
        if (newKey == null) {
            Log.e(TAG, "newKey is null. context=" + context);
            return false;
        }

        sInjectedContexts.add(newKey);
        return true;
    }

    private static Object getResourcesKey(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return context;
        } else {
            return ResourcesClass.getResourcesImplNoThrow(context.getResources());
        }
    }

    private static boolean addAssetPathBellowL(Context context, String path) {
        try {
            AssetManager assets = AssetManager.class.newInstance();
            AssetManagerClass.addAssetPath(assets, context.getApplicationInfo().sourceDir);
            AssetManagerClass.addAssetPath(assets, path);
            AssetManagerClass.ensureStringBlocks(assets);

            Resources res = context.getResources();
            ResourcesClass.setAssets(res, assets);
            res.updateConfiguration(res.getConfiguration(), res.getDisplayMetrics());

            return true;
        } catch (InstantiationException
                | InvocationTargetException
                | NoSuchMethodException
                | IllegalAccessException
                | NoSuchFieldException e) {
            Log.w(TAG, "Fail to addAssetPathBellowL", e);
            return false;
        }
    }

    private static boolean addAssetPathAboveL(Context context, String path) {
        AssetManager assets = context.getResources().getAssets();
        try {
            AssetManagerClass.addAssetPath(assets, path);
            return true;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            Log.w(TAG, "Fail to addAssetPathAboveL", e);
            return false;
        }
    }

    private static boolean addAssetPathAboveO(Context context, String platform) {
        try {
            ResourceInjectorForO.inject(context, platform);
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Fail to addAssetPathAboveO", e);
            return false;
        }
    }
}

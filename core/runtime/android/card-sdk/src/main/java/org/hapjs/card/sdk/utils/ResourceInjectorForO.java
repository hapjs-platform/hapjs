/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.card.sdk.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.content.res.ResourcesImpl;
import android.os.Build;
import android.util.Log;
import android.view.Display;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.hapjs.card.sdk.utils.reflect.ContextImplClass;
import org.hapjs.card.sdk.utils.reflect.LoadedApkClass;
import org.hapjs.card.sdk.utils.reflect.ResourcesClass;
import org.hapjs.card.sdk.utils.reflect.ResourcesManagerClass;

/**
 * ref: https://github.com/didi/VirtualAPK/blob/master/CoreLibrary/src/main/java/com/didi/virtualapk/internal/ResourcesManager
 * .java
 */

class ResourceInjectorForO {
    private static final String TAG = "ResourceInjectorForO";

    private static Set<Resources> sGuarderResourcesSet = new HashSet<>();

    @TargetApi(Build.VERSION_CODES.O)
    public static void inject(Context context, String platform) throws Exception {
        Context platformContext = context.createPackageContext(platform, 0);
        String newAssetPath = platformContext.getApplicationInfo().sourceDir;
        ApplicationInfo info = context.getApplicationInfo();
        String baseResDir = info.publicSourceDir;

        injectPlatformAsset(context, newAssetPath);

        Object resourcesManager = ResourcesManagerClass.getInstance();
        ResourcesManagerClass.appendLibAssetForMainAssetPath(resourcesManager, baseResDir, newAssetPath);

        Resources dumbResources = (Resources) ResourcesManagerClass.getResources(resourcesManager,
                null,
                context.getApplicationInfo().publicSourceDir,
                null,
                null,
                new String[] {},
                Display.DEFAULT_DISPLAY,
                null,
                null,
                null);

        Object resImpl = ResourcesClass.getResourcesImpl(dumbResources);
        Resources guarderResources = new GuarderResources(ResourceInjectorForO.class.getClassLoader(), context, newAssetPath);
        ResourcesClass.setResourcesImpl(guarderResources, resImpl);

        ArrayList<WeakReference<Resources>> resRefs = ResourcesManagerClass.getResourceReferences(resourcesManager);
        for (int i = 0; i < resRefs.size(); ++i) {
            WeakReference<Resources> resRef = resRefs.get(i);
            if (resRef.get() == dumbResources) {
                resRefs.set(i, new WeakReference<>(guarderResources));
                break;
            }
        }

        sGuarderResourcesSet.add(guarderResources);
    }

    @TargetApi(Build.VERSION_CODES.O)
    private static void injectPlatformAsset(Context context, String platformAssetPath) throws Exception {
        ApplicationInfo info = context.getApplicationInfo();
        info.splitSourceDirs = append(info.splitSourceDirs, platformAssetPath);

        Object loadedApk = ContextImplClass.getPackageInfo(context);
        String[] splitResDirs = (String[]) LoadedApkClass.getSplitResDirs(loadedApk);
        LoadedApkClass.setSplitResDirs(loadedApk, append(splitResDirs, platformAssetPath));
    }

    private static String[] append(String[] paths, String newPath) {
        if (contains(paths, newPath)) {
            return paths;
        }

        final int newPathsCount = 1 + (paths != null ? paths.length : 0);
        final String[] newPaths = new String[newPathsCount];
        if (paths != null) {
            System.arraycopy(paths, 0, newPaths, 0, paths.length);
        }
        newPaths[newPathsCount - 1] = newPath;
        return newPaths;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static boolean contains(String[] array, String value) {
        if (array == null) {
            return false;
        }
        for (int i = 0; i < array.length; i++) {
            if (Objects.equals(array[i], value)) {
                return true;
            }
        }
        return false;
    }

    private static class GuarderResources extends org.hapjs.hackres.HackResources {
        private WeakReference<Context> mContextRef;
        private String mPlatformAssetPath;

        public GuarderResources(ClassLoader classLoader, Context context, String platformAssetPath) {
            super(classLoader);
            mContextRef = new WeakReference<>(context);
            mPlatformAssetPath = platformAssetPath;
        }

        public void setImpl(ResourcesImpl impl) {
            super.setImpl(impl);

            Context context = mContextRef.get();
            if (context == null) {
                sGuarderResourcesSet.remove(this);
            } else {
                try {
                    injectPlatformAsset(context, mPlatformAssetPath);
                } catch (Exception e) {
                    Log.e(TAG, "failed to injectPlatformAsset", e);
                }
            }
        }

    }
}

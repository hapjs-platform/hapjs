/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.card.sdk.utils.reflect;

import android.content.res.AssetManager;
import android.content.res.Resources;
import android.util.Log;
import java.lang.reflect.Field;

public class ResourcesClass {
    private static final String TAG = "ResourcesClass";

    private static Field mAssetsField;
    private static Field mResourceImplField;

    public static void setAssets(Resources res, AssetManager assets)
            throws NoSuchFieldException, IllegalAccessException {
        if (mAssetsField == null) {
            mAssetsField = Resources.class.getDeclaredField("mAssets");
            mAssetsField.setAccessible(true);
        }
        mAssetsField.set(res, assets);
    }

    public static Object getResourcesImpl(Resources res)
            throws NoSuchFieldException, IllegalAccessException {
        if (mResourceImplField == null) {
            mResourceImplField = Resources.class.getDeclaredField("mResourcesImpl");
            mResourceImplField.setAccessible(true);
        }
        return mResourceImplField.get(res);
    }

    public static void setResourcesImpl(Resources res, Object resImpl)
            throws NoSuchFieldException, IllegalAccessException {
        if (mResourceImplField == null) {
            mResourceImplField = Resources.class.getDeclaredField("mResourcesImpl");
            mResourceImplField.setAccessible(true);
        }
        mResourceImplField.set(res, resImpl);
    }

    public static Object getResourcesImplNoThrow(Resources res) {
        try {
            return getResourcesImpl(res);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Log.e(TAG, "failed to getResourcesImpl", e);
        }
        return null;
    }
}

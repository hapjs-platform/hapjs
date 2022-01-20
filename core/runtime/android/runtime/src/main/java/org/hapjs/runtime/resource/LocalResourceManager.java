/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.runtime.resource;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import org.hapjs.cache.Cache;
import org.hapjs.cache.CacheException;
import org.hapjs.cache.CacheStorage;

public class LocalResourceManager implements ResourceManager {

    private static final String TAG = "LocalResourceManager";
    private Cache mCache;

    public LocalResourceManager(Context context, String pkg) {
        mCache = CacheStorage.getInstance(context).getCache(pkg);
    }

    @Override
    public Uri getResource(String resourcePath) {
        return getResource(resourcePath, null);
    }

    @Override
    public Uri getResource(String resourcePath, String page) {
        Uri result = null;
        try {
            result = mCache.get(resourcePath, page);
        } catch (CacheException e) {
            Log.e(TAG, "Cache is missing: " + resourcePath + ", reason: " + e.getMessage(), e);
        }
        return result;
    }

    @Override
    public long size(Context context) {
        return mCache.size();
    }
}

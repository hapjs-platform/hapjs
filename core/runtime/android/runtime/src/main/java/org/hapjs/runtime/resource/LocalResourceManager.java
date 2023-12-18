/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.runtime.resource;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import org.hapjs.bridge.storage.file.Resource;
import org.hapjs.cache.Cache;
import org.hapjs.cache.CacheException;
import org.hapjs.cache.CacheStorage;
import org.hapjs.runtime.HapEngine;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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

    @Override
    public List<String> getFileNameList(Context context, String pkg, String resourcePath) {
        Resource resourceDir = HapEngine.getInstance(pkg).getApplicationContext().getResource(resourcePath);
        if (resourceDir == null || resourceDir.getUnderlyingFile() == null) {
            return null;
        }
        File[] files = resourceDir.getUnderlyingFile().listFiles();
        if (files == null) {
            return null;
        }
        ArrayList<String> fileNameList = new ArrayList<>();
        for (File file : files) {
            if (file.isFile()) {
                String fullFileName = file.getName();
                String fileName = fullFileName.substring(0, fullFileName.lastIndexOf("."));
                fileNameList.add(fileName);
            }
        }
        return fileNameList;
    }
}

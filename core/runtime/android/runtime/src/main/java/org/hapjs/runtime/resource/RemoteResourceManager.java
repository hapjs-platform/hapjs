/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.runtime.resource;

import android.content.ContentProviderClient;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import org.hapjs.cache.CacheUtils;
import org.hapjs.runtime.ResourceConfig;

import java.util.List;

public class RemoteResourceManager implements ResourceManager {
    private static final String TAG = "RemoteResourceManager";

    private String mPackage;

    public RemoteResourceManager(String pkg) {
        mPackage = pkg;
    }

    private static String getPlatform() {
        return ResourceConfig.getInstance().getPlatform();
    }

    @Override
    public Uri getResource(String resourcePath) {
        return getResource(resourcePath, null);
    }

    @Override
    public Uri getResource(String resourcePath, String page) {
        String path = CacheUtils.getResourcePath(resourcePath, page);
        return CacheProviderContracts.getResource(getPlatform(), mPackage, path);
    }

    @Override
    public long size(Context context) {
        Bundle result =
                context
                        .getContentResolver()
                        .call(
                                CacheProviderContracts.getUri(getPlatform()),
                                CacheProviderContracts.METHOD_GET_SIZE,
                                mPackage,
                                null);
        if (result != null) {
            return result.getLong(CacheProviderContracts.RESULT_SIZE, -1);
        }
        return -1;
    }

    @Override
    public List<String> getFileNameList(Context context, String pkg, String resourcePath) {
        ContentProviderClient client = null;
        try {
            client = context.getContentResolver()
                    .acquireUnstableContentProviderClient(
                            CacheProviderContracts.getUri(getPlatform()));
            if (client != null) {
                Bundle extras = new Bundle();
                extras.putString(CacheProviderContracts.PARAM_PACKAGE, pkg);
                extras.putString(CacheProviderContracts.PARAM_RESOURCE_PATH, resourcePath);
                Bundle result = client.call(CacheProviderContracts.METHOD_GET_FILE_NAME_LIST, null, extras);
                if (result != null) {
                    return result.getStringArrayList(CacheProviderContracts.RESULT_FILE_NAMES);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Fail to get file name list.", e);
        } finally {
            if (client != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    client.close();
                } else {
                    client.release();
                }
            }
        }
        return null;
    }
}

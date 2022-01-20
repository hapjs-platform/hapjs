/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.runtime.resource;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import org.hapjs.cache.CacheUtils;
import org.hapjs.runtime.ResourceConfig;

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
}

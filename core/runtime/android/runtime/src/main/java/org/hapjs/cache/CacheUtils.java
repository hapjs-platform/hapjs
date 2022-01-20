/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.cache;

import android.text.TextUtils;
import java.io.File;
import java.io.IOException;

public class CacheUtils {

    protected static String normalizePath(File resourceDir, String resourcePath, String pageName)
            throws CacheSecurityException, CacheException {
        if (TextUtils.isEmpty(resourcePath)) {
            throw new CacheException(
                    CacheErrorCode.EMPTY_RESOURCE_PATH, "Empty resource path is illegal");
        }
        try {
            String resPath = getResourcePath(resourcePath, pageName);
            File file = new File(resourceDir, resPath);
            checkResourcePath(resourceDir, file);
            return new File(resPath).getCanonicalPath();
        } catch (IOException e) {
            throw new CacheSecurityException("illegalPath:" + resourcePath, e);
        } catch (SecurityException e) {
            throw new CacheSecurityException("illegalPath:" + resourcePath, e);
        }
    }

    public static String getResourcePath(String resourcePath, String pageName) {
        if (resourcePath.startsWith("/")) {
            return resourcePath.substring(1);
        } else if (TextUtils.isEmpty(pageName)) {
            return resourcePath;
        } else {
            return pageName + "/" + resourcePath;
        }
    }

    private static void checkResourcePath(File resourceDir, File resourcePath)
            throws CacheSecurityException, IOException {
        String cacheDir = resourceDir.getCanonicalPath();
        if (!cacheDir.endsWith("/")) {
            cacheDir += "/";
        }
        if (!resourcePath.getCanonicalPath().startsWith(cacheDir)) {
            throw new CacheSecurityException("illegalPath: " + resourcePath);
        }
    }
}

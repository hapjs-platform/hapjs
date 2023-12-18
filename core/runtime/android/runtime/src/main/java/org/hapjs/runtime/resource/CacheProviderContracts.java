/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.runtime.resource;

import android.net.Uri;

public class CacheProviderContracts {
    public static final String RESULT_SIZE = "size";
    public static final String METHOD_GET_SIZE = "getSize";
    public static final String METHOD_GET_FILE_NAME_LIST = "getFileNameList";
    public static final String PARAM_PACKAGE = "package";
    public static final String PARAM_RESOURCE_PATH = "resourcePath";
    public static final String RESULT_FILE_NAMES = "fileNames";
    private static final String SCHEME = "content";

    public static Uri getResource(String platform, String pkg, String resPath) {
        if (!resPath.startsWith("/")) {
            resPath = "/" + resPath;
        }
        String path = pkg + resPath;
        return new Uri.Builder().scheme(SCHEME).authority(getAuthority(platform)).path(path)
                .build();
    }

    public static Uri getUri(String platform) {
        return new Uri.Builder().scheme(SCHEME).authority(getAuthority(platform)).build();
    }

    private static String getAuthority(String platform) {
        return platform + ".res";
    }
}

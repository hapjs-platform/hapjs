/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge.storage.file;

import android.net.Uri;
import org.hapjs.common.utils.FileHelper;

public class InternalUriUtils {
    static final String PACKAGE_PREFIX = "/";
    private static final String INTERNAL_SCHEMA = "internal";
    static final String CACHE_PREFIX = INTERNAL_SCHEMA + "://cache/";
    static final String FILES_PREFIX = INTERNAL_SCHEMA + "://files/";
    static final String MASS_PREFIX = INTERNAL_SCHEMA + "://mass/";
    public static final String USER_DATA_PATH = MASS_PREFIX;
    static final String TEMP_PREFIX = INTERNAL_SCHEMA + "://tmp/";
    static final String INTERNAL_SCHEMA_PREFIX = INTERNAL_SCHEMA + "://";

    static void check(String internalUri) {
        if (!isValidUri(internalUri)) {
            throw new IllegalArgumentException("Illegal path: " + internalUri);
        }
    }

    public static boolean isValidUri(String internalUri) {
        if (!FileHelper.isValidUri(internalUri)) {
            return false;
        }
        return internalUri.startsWith("/") || isInternalPath(internalUri);
    }

    public static String getValidUri(String internalUri) {
        String uri = FileHelper.getValidUri(internalUri);
        check(uri);
        return uri;
    }

    public static boolean isInternalPath(String path) {
        return path.startsWith(INTERNAL_SCHEMA_PREFIX);
    }

    public static boolean isInternalUri(Uri uri) {
        return INTERNAL_SCHEMA.equals(uri.getScheme());
    }

    public static boolean isTmpUri(String uri) {
        return uri.startsWith(TEMP_PREFIX);
    }

    public static boolean isWritableInternalUri(String uri) {
        return uri.startsWith(CACHE_PREFIX) || uri.startsWith(FILES_PREFIX) || uri.startsWith(MASS_PREFIX);
    }
}

/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.cache;

public class CacheErrorCode {

    public static final int OK = 0;
    public static final int UNKNOWN = 1;

    // 1XX, for package error
    public static final int PACKAGE_ARCHIVE_NOT_EXIST = 100;
    public static final int PACKAGE_READ_FAILED = 101;
    public static final int PACKAGE_UNZIP_FAILED = 102;
    public static final int PACKAGE_HAS_NO_MANIFEST_JSON = 103;
    public static final int PACKAGE_MANIFEST_JSON_INVALID = 104;
    public static final int PACKAGE_HAS_NO_APP_JS = 105;
    public static final int PACKAGE_HAS_NO_SIGNATURE = 106;
    public static final int PACKAGE_VERIFY_SIGNATURE_FAILED = 107;
    public static final int PACKAGE_PARSE_CERTIFICATE_FAILED = 108;
    public static final int PACKAGE_CERTIFICATE_CHANGED = 109;
    public static final int PACKAGE_NAME_CHANGED = 110;
    public static final int PACKAGE_INCOMPATIBLE = 111;
    public static final int PACKAGE_VERIFY_DIGEST_FAILED = 112;
    public static final int PACKAGE_CACHE_OBSOLETE = 113;

    // 2XX, for internal error
    public static final int RESOURCE_DIR_MOVE_FAILED = 200;
    public static final int LOAD_EXISTED_CERTIFICATE_FAILED = 201;
    public static final int SAVE_CERTIFICATE_FAILED = 202;
    public static final int EMPTY_RESOURCE_PATH = 203;
    public static final int ARCHIVE_FILE_NOT_FOUND = 204;

    // 3XX, for external environment error
    public static final int NETWORK_UNAVAILABLE = 300;
    public static final int PACKAGE_UNAVAILABLE = 301;
    public static final int PAGE_UNAVAILABLE = 302;
    public static final int RESOURCE_UNAVAILABLE = 303;
    public static final int RESOURCE_PATH_INVALID = 304; // eg. ../../otherapp/page.js
    public static final int PACKAGE_NO_UPDATE = 305;
    public static final int SERVER_ERROR = 306;
    public static final int NO_ANY_PACKAGE = 307;

    // 4XX, other error
    public static final int ALREADY_INSTALLED = 400;
}

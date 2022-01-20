/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.card.api;

public interface DownloadListener {
    int DOWNLOAD_RESULT_OK = 0;
    int DOWNLOAD_RESULT_FAILED = 1;

    int DOWNLOAD_ERROR_UNKNOWN = 100;
    int DOWNLOAD_ERROR_ALREADY_INSTALLED = 101;
    int DOWNLOAD_ERROR_DOWNGRADE = 102;
    int DOWNLOAD_ERROR_PACKAGE_UNAVAILABLE = 103;
    int DOWNLOAD_ERROR_NETWORK_UNAVAILABLE = 104;

    void onDownloadResult(String pkg, int resultCode, int errorCode);
}

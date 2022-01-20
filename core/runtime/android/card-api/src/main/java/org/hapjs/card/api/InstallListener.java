/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.card.api;

public interface InstallListener {
    int INSTALL_RESULT_OK = 0;
    int INSTALL_RESULT_FAILED = 1;

    int INSTALL_ERROR_UNKNOWN = 100;
    int INSTALL_ERROR_CORRUPTION = 101;
    int INSTALL_ERROR_SIGNATURE = 102;
    int INSTALL_ERROR_DOWNGRADE = 103;
    int INSTALL_ERROR_IO = 104;
    int INSTALL_ERROR_INCOMPATIBLE = 105;
    int INSTALL_ERROR_UNAVAILABLE = 106;
    int INSTALL_ERROR_TIMEOUT = 107;

    @Deprecated
    void onInstallResult(String pkg, int resultCode);

    void onInstallResult(String pkg, int resultCode, int errorCode);
}

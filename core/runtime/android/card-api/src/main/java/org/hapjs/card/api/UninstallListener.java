/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.card.api;

public interface UninstallListener {
    int UNINSTALL_RESULT_OK = 0;
    int UNINSTALL_RESULT_FAILED = 1;

    int UNINSTALL_ERROR_UNKNOWN = 100;
    int UNINSTALL_ERROR_NOT_EXISTS = 101;
    int UNINSTALL_ERROR_HAS_PAGES = 102;

    void onUninstallResult(String pkg, int resultCode, int errorCode);
}

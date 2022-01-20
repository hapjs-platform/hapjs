/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.card.api;

public abstract class AbstractInstallLister implements InstallListener {

    @Override
    public final void onInstallResult(String pkg, int resultCode) {
        onInstallResult(pkg, resultCode, InstallListener.INSTALL_ERROR_UNKNOWN);
    }
}

/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.cache;

import org.hapjs.cache.utils.PackageUtils;

public class DefaultPackageCheckProvider implements PackageCheckProvider {

    @Override
    public boolean hasAppJs(String path) {
        return PackageUtils.hasAppJs(path);
    }

    @Override
    public boolean isValidCertificate(String pkg, byte[] certificate) {
        return true;
    }
}

/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.cache;

import android.content.Context;
import java.io.File;
import org.hapjs.cache.utils.PackageUtils;

public class DefaultInstallInterceptProviderImpl implements InstallInterceptProvider {
    @Override
    public void onSignatureVerify(Context context, File zipFile, File certFile, String pkg)
            throws CacheException {
        PackageUtils.verify(context, zipFile, certFile, pkg);
    }

    @Override
    public void onPreInstall(Context context, String pkg) {
    }

    @Override
    public void onPostInstall(Context context, String pkg, boolean isUpdate, boolean hasSucc) {
    }
}

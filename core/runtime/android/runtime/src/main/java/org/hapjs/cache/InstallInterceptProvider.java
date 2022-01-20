/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.cache;

import android.content.Context;
import java.io.File;

public interface InstallInterceptProvider {
    String NAME = "InstallInterceptProvider";

    void onSignatureVerify(Context context, File zipFile, File certFile, String pkg)
            throws CacheException;

    void onPreInstall(Context context, String pkg);

    void onPostInstall(Context context, String pkg, boolean isUpdate, boolean rollback);
}

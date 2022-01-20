/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.runtime;

import android.content.Context;
import android.content.pm.PackageManager;

public class DefaultPermissionCheckProviderImpl implements PermissionCheckProvider {

    @Override
    public boolean verify(Context context, String callingPkg) {
        if (context == null) {
            return false;
        }
        PackageManager pm = context.getPackageManager();
        return pm.checkSignatures(callingPkg, context.getPackageName())
                == PackageManager.SIGNATURE_MATCH;
    }
}

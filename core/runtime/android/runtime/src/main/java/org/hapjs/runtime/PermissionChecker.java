/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.runtime;

import android.content.Context;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.util.Log;

public class PermissionChecker {

    private static final String TAG = "PermissionChecker";

    public static boolean verify(Context context, String callingPkg) {
        if (TextUtils.isEmpty(callingPkg)) {
            Log.e(TAG, "fail to verify callingPkg: " + callingPkg);
            return false;
        }

        PermissionCheckProvider provider =
                ProviderManager.getDefault().getProvider(PermissionCheckProvider.NAME);
        if (provider == null) {
            provider = new DefaultPermissionCheckProviderImpl();
        }
        boolean result = provider.verify(context, callingPkg);
        if (!result) {
            Log.e(TAG, "fail to verify callingPkg: " + callingPkg);
        }
        return result;
    }

    public static boolean verify(Context context, int uid) {
        PackageManager pm = context.getPackageManager();
        String[] packages = pm.getPackagesForUid(uid);
        if (packages == null || packages.length != 1) {
            return false;
        }
        return verify(context, packages[0]);
    }
}

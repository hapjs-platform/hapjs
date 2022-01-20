/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.utils;

import android.content.Context;
import android.os.Binder;
import android.util.Log;

public class BinderUtils {
    private static final String TAG = "BinderUtils";

    public static String getCallingApp(Context context) {
        String[] packages = context.getPackageManager().getPackagesForUid(Binder.getCallingUid());
        if (packages == null || packages.length == 0) {
            Log.i(TAG, "can't get calling app.");
            return null;
        }
        return packages[0];
    }
}

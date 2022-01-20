/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.card.sdk.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.util.Log;
import org.hapjs.card.common.utils.CardConfigHelper;

public class CardUtils {
    private static String TAG = "CardUtils";
    private static String platformPackage;

    public static String getPlatformName(Context context) {
        if (TextUtils.isEmpty(platformPackage)) {
            platformPackage = CardConfigHelper.getPlatform(context);
            if (TextUtils.isEmpty(platformPackage)) {
                platformPackage = "org.hapjs.mockup";
            }
        }
        return platformPackage;
    }

    public static int getPlatformVersion(Context context) {
        try {
            PackageManager packageManager = context.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(getPlatformName(context), 0);
            return packageInfo.versionCode;
        } catch (Exception e) {
            Log.e(TAG, "getPlatformVersion", e);
        }
        return 0;
    }
}

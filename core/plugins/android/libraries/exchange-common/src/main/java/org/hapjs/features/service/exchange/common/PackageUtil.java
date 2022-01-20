/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.service.exchange.common;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.util.Log;
import java.math.BigInteger;
import java.security.MessageDigest;

public class PackageUtil {
    private static final String TAG = "PackageUtil";

    public static String getNativeAppSignDigest(Context context, String pkg) {
        try {
            PackageInfo info =
                    context.getPackageManager().getPackageInfo(pkg, PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                return getSignDigest(signature.toByteArray());
            }
        } catch (Exception e) {
            Log.w(TAG, "getNativeAppSignDigest error", e);
        }
        return null;
    }

    public static String getSignDigest(byte[] input) {
        if (input != null) {
            try {
                final MessageDigest lDigest = MessageDigest.getInstance("SHA-256");
                lDigest.update(input);
                final BigInteger lHashInt = new BigInteger(1, lDigest.digest());
                return String.format("%1$032X", lHashInt).toLowerCase();
            } catch (Exception e) {
                Log.w(TAG, "getSignDigest error", e);
            }
        }
        return null;
    }
}

/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.utils;

import android.util.Log;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class DigestUtils {
    private static final String TAG = "DigestUtils";

    public static String getSha256(byte[] data) {
        String hashString = "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(data);
            return StringUtils.byte2HexString(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Md5 algorithm NOT found.", e);
        }
        return hashString.toLowerCase();
    }
}

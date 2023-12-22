/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.utils;

import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.CRC32;

public class DigestUtils {
    private static final String TAG = "DigestUtils";

    public static String getSha256(byte[] data) {
        String hashString = "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(data);
            return StringUtils.byte2HexString(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Sha256 algorithm NOT found.", e);
        }
        return hashString.toLowerCase();
    }

    public static String getMd5(byte[] data) {
        String hashString = "";
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(data);
            return StringUtils.byte2HexString(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Md5 algorithm NOT found.", e);
        }
        return hashString.toLowerCase();
    }

    public static long crc32(byte[] content) {
        try {
            final CRC32 CRC32 = new CRC32();
            CRC32.reset();
            CRC32.update(content);
            return CRC32.getValue();
        } catch (Exception e) {
            Log.e(TAG, "crc32 algorithm fail.", e);
        }
        return -1;
    }
}

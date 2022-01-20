/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.service.exchange.common;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

public class ExchangeUtils {
    private static final String TAG = "ExchangeUtils";

    public static boolean setAppData(
            Context context,
            String ownerPkg,
            String key,
            String value,
            String targetPkg,
            String targetSign) {
        return set(context, Constant.SCOPE_APPLICATION, ownerPkg, key, value, targetPkg,
                targetSign);
    }

    public static boolean setGlobalData(Context context, String ownerPkg, String key,
                                        String value) {
        return set(context, Constant.SCOPE_GLOBAL, ownerPkg, key, value, null, null);
    }

    public static boolean setVendorData(Context context, String ownerPkg, String key,
                                        String value) {
        return set(context, Constant.SCOPE_VENDOR, ownerPkg, key, value, null, null);
    }

    private static boolean set(
            Context context,
            String scope,
            String ownerPkg,
            String key,
            String value,
            String targetPkg,
            String targetSign) {
        if (TextUtils.isEmpty(ownerPkg) || TextUtils.isEmpty(key) || TextUtils.isEmpty(value)) {
            throw new IllegalArgumentException("Illegal param");
        }
        Uri.Builder builder =
                ExchangeUriUtil.getDataUri(context, ownerPkg)
                        .buildUpon()
                        .appendQueryParameter(Constant.PARAM_SCOPE, scope)
                        .appendQueryParameter(Constant.PARAM_KEY, key)
                        .appendQueryParameter(Constant.PARAM_VALUE, value);

        if (!TextUtils.isEmpty(targetPkg) && !TextUtils.isEmpty(targetSign)) {
            builder
                    .appendQueryParameter(Constant.PARAM_PKG, targetPkg)
                    .appendQueryParameter(Constant.PARAM_SIGN, targetSign);
        }
        Uri setUri = builder.build();
        Uri uri = context.getContentResolver().insert(setUri, new ContentValues());
        return uri != null;
    }

    public static String getAppData(
            Context context, String pkg, String targetPkg, String targetSign, String key) {
        if (TextUtils.isEmpty(pkg)
                || TextUtils.isEmpty(targetPkg)
                || TextUtils.isEmpty(targetSign)
                || TextUtils.isEmpty(key)) {
            throw new IllegalArgumentException("Illegal param");
        }
        return get(context, Constant.SCOPE_APPLICATION, pkg, targetPkg, targetSign, key);
    }

    public static String getGlobalData(Context context, String pkg, String key) {
        if (TextUtils.isEmpty(pkg) || TextUtils.isEmpty(key)) {
            throw new IllegalArgumentException("Illegal param");
        }
        return get(context, Constant.SCOPE_GLOBAL, pkg, null, null, key);
    }

    public static String getVendorData(Context context, String pkg, String key) {
        if (TextUtils.isEmpty(pkg) || TextUtils.isEmpty(key)) {
            throw new IllegalArgumentException("Illegal param");
        }
        return get(context, Constant.SCOPE_VENDOR, pkg, null, null, key);
    }

    private static String get(
            Context context, String scope, String pkg, String targetPkg, String targetSign,
            String key) {
        Uri getUri =
                ExchangeUriUtil.getDataUri(context, pkg)
                        .buildUpon()
                        .appendQueryParameter(Constant.PARAM_PKG, targetPkg)
                        .appendQueryParameter(Constant.PARAM_SIGN, targetSign)
                        .appendQueryParameter(Constant.PARAM_KEY, key)
                        .appendQueryParameter(Constant.PARAM_SCOPE, scope)
                        .build();
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(getUri, null, null, null, null);
            if (cursor == null) {
                return null;
            }
            if (cursor.moveToNext()) {
                return cursor.getString(cursor.getColumnIndex(Constant.PARAM_VALUE));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    public static boolean remove(
            Context context, String ownerPkg, String key, String targetPkg, String targetSign) {
        if (TextUtils.isEmpty(ownerPkg) || TextUtils.isEmpty(key)) {
            throw new IllegalArgumentException("Illegal param");
        }
        Uri.Builder builder =
                ExchangeUriUtil.getDataUri(context, ownerPkg)
                        .buildUpon()
                        .appendQueryParameter(Constant.PARAM_KEY, key);

        if (!TextUtils.isEmpty(targetPkg) && !TextUtils.isEmpty(targetSign)) {
            builder
                    .appendQueryParameter(Constant.PARAM_PKG, targetPkg)
                    .appendQueryParameter(Constant.PARAM_SIGN, targetSign);
        }
        Uri removeUri = builder.build();
        int result = context.getContentResolver().delete(removeUri, null, null);
        return result >= 0;
    }

    public static boolean clear(Context context, String ownerPkg) {
        if (TextUtils.isEmpty(ownerPkg)) {
            throw new IllegalArgumentException("Illegal param");
        }
        Uri clearUri = ExchangeUriUtil.getDataUri(context, ownerPkg);
        int result = context.getContentResolver().delete(clearUri, null, null);
        return result >= 0;
    }

    public static boolean grantPermission(
            Context context,
            String ownerPkg,
            String grantPkg,
            String grantSign,
            String grantKey,
            boolean writable) {
        if (TextUtils.isEmpty(ownerPkg)
                || TextUtils.isEmpty(grantPkg)
                || TextUtils.isEmpty(grantSign)) {
            throw new IllegalArgumentException("Illegal param");
        }
        Uri.Builder grantUri =
                ExchangeUriUtil.getPermissionUri(context, ownerPkg)
                        .buildUpon()
                        .appendQueryParameter(Constant.PARAM_PKG, grantPkg)
                        .appendQueryParameter(Constant.PARAM_SIGN, grantSign)
                        .appendQueryParameter(Constant.PARAM_WRITABLE, String.valueOf(writable));
        if (!TextUtils.isEmpty(grantKey)) {
            grantUri.appendQueryParameter(Constant.PARAM_KEY, grantKey);
        }
        Uri result = context.getContentResolver().insert(grantUri.build(), new ContentValues());
        return result != null;
    }

    public static boolean revokePermission(
            Context context, String ownerPkg, String revokePkg, String revokeKey) {
        if (TextUtils.isEmpty(ownerPkg) || TextUtils.isEmpty(revokePkg)) {
            throw new IllegalArgumentException("Illegal param");
        }
        Uri.Builder revokeUri =
                ExchangeUriUtil.getPermissionUri(context, ownerPkg)
                        .buildUpon()
                        .appendQueryParameter(Constant.PARAM_PKG, revokePkg);
        if (!TextUtils.isEmpty(revokeKey)) {
            revokeUri.appendQueryParameter(Constant.PARAM_KEY, revokeKey);
        }
        int result = context.getContentResolver().delete(revokeUri.build(), null, null);
        return result >= 0;
    }
}

/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import org.hapjs.common.utils.FileUtils;
import org.hapjs.persistence.ShortcutParamsTable;

public class ShortcutParamsHelper {
    private static final String TAG = "ShortcutParamsHelper";

    public static Uri insertShortParams(Context context, String pkg, String path, String params) {
        ContentValues values = new ContentValues();
        values.put(ShortcutParamsTable.Columns.PKG, pkg);
        values.put(ShortcutParamsTable.Columns.PATH, path);
        values.put(ShortcutParamsTable.Columns.PARAMS, params);
        return context.getContentResolver()
                .insert(ShortcutParamsTable.getContentUri(context), values);
    }

    public static boolean updateShortParams(Context context, String pkg, String path,
                                            String params) {
        if (!hasShortcutParamsRecord(context, pkg, path)) {
            if (!TextUtils.isEmpty(params)) {
                Uri uri = insertShortParams(context, pkg, path, params);
                return uri != null;
            }
            return true;
        }

        String oldParams = queryShortcutParams(context, pkg, path);
        if (TextUtils.equals(oldParams, params)) {
            return false;
        }

        ContentValues values = new ContentValues();
        values.put(ShortcutParamsTable.Columns.PKG, pkg);
        values.put(ShortcutParamsTable.Columns.PATH, path);
        values.put(ShortcutParamsTable.Columns.PARAMS, params);
        int rows =
                context
                        .getContentResolver()
                        .update(
                                ShortcutParamsTable.getContentUri(context),
                                values,
                                buildQuerySelection(pkg, path),
                                buildQuerySelectionArgs(pkg, path));
        return rows > 0;
    }

    public static void deleteShortParams(Context context, String pkg) {
        context
                .getContentResolver()
                .delete(
                        ShortcutParamsTable.getContentUri(context),
                        ShortcutParamsTable.Columns.PKG + "=?",
                        new String[] {pkg});
    }

    private static String buildQuerySelection(String pkg, String path) {
        String selection = ShortcutParamsTable.Columns.PKG + "=?";
        if (TextUtils.isEmpty(path)) {
            selection +=
                    " AND ("
                            + ShortcutParamsTable.Columns.PATH
                            + " is null"
                            + " OR "
                            + ShortcutParamsTable.Columns.PATH
                            + "=?"
                            + ")";
        } else {
            selection += " AND " + ShortcutParamsTable.Columns.PATH + "=?";
        }
        return selection;
    }

    private static String[] buildQuerySelectionArgs(String pkg, String path) {
        if (TextUtils.isEmpty(path)) {
            path = "";
        }
        return new String[] {pkg, path};
    }

    public static String queryShortcutParams(Context context, String pkg, String path) {
        Cursor cursor = null;
        try {
            cursor =
                    context
                            .getContentResolver()
                            .query(
                                    ShortcutParamsTable.getContentUri(context),
                                    new String[] {ShortcutParamsTable.Columns.PARAMS},
                                    buildQuerySelection(pkg, path),
                                    buildQuerySelectionArgs(pkg, path),
                                    null);
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getString(0);
            }
        } catch (Exception e) {
            Log.e(TAG, "failed to get params", e);
        } finally {
            FileUtils.closeQuietly(cursor);
        }
        return null;
    }

    private static boolean hasShortcutParamsRecord(Context context, String pkg, String path) {
        Cursor cursor = null;
        try {
            cursor =
                    context
                            .getContentResolver()
                            .query(
                                    ShortcutParamsTable.getContentUri(context),
                                    new String[] {ShortcutParamsTable.Columns.PARAMS},
                                    buildQuerySelection(pkg, path),
                                    buildQuerySelectionArgs(pkg, path),
                                    null);
            return cursor != null && cursor.moveToFirst();
        } catch (Exception e) {
            Log.e(TAG, "failed to get params", e);
        } finally {
            FileUtils.closeQuietly(cursor);
        }
        return false;
    }

    public static String appendParams(String path, String params) {
        if (TextUtils.isEmpty(params)) {
            return path;
        }

        if (TextUtils.isEmpty(path)) {
            return "?" + params;
        } else {
            if (path.indexOf("?") == -1) {
                path += "?";
            }
            path += params;
            return path;
        }
    }
}

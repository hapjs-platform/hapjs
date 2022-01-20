/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.permission;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import java.util.HashMap;
import java.util.Map;
import org.hapjs.bridge.permission.RuntimePermissionProvider;
import org.hapjs.persistence.PermissionTable;

public class Permission {

    public static int[] checkPermissions(Context context, String pkg, String[] permissions) {
        Map<String, Integer> map = queryPermissions(context, pkg);
        int[] modes = new int[permissions.length];
        for (int i = 0; i < permissions.length; i++) {
            Integer mode = map.get(permissions[i]);
            modes[i] = mode != null ? mode : RuntimePermissionProvider.MODE_PROMPT;
        }
        return modes;
    }

    public static boolean shouldShowForbidden(Context context, String pkg, String permission) {
        return queryPermissions(context, pkg).containsKey(permission);
    }

    public static Map<String, Integer> queryPermissions(Context context, String pkg) {
        Map<String, Integer> map = new HashMap<>();
        String[] projection =
                new String[] {PermissionTable.Columns.PERMISSION, PermissionTable.Columns.MODE};
        String selection = PermissionTable.Columns.APP_ID + "=?";
        String[] selectionArgs = new String[] {pkg};
        Cursor cursor =
                context
                        .getContentResolver()
                        .query(
                                PermissionTable.getContentUri(context), projection, selection,
                                selectionArgs, null);
        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    String permission = cursor.getString(0);
                    int mode = cursor.getInt(1);
                    map.put(permission, mode);
                }
            } finally {
                cursor.close();
            }
        }
        return map;
    }

    public static void grantPermissions(Context context, String pkg, String[] permissions) {
        updatePermissionsMode(context, pkg, permissions, RuntimePermissionProvider.MODE_ACCEPT);
    }

    public static void rejectPermissions(
            Context context, String pkg, String[] permissions, boolean forbidden) {
        int mode =
                forbidden ? RuntimePermissionProvider.MODE_REJECT :
                        RuntimePermissionProvider.MODE_PROMPT;
        updatePermissionsMode(context, pkg, permissions, mode);
    }

    public static void clearPermissions(Context context, String pkg) {
        String selection = PermissionTable.Columns.APP_ID + "=?";
        String[] selectionArgs = new String[] {pkg};
        context
                .getContentResolver()
                .delete(PermissionTable.getContentUri(context), selection, selectionArgs);
    }

    private static void updatePermissionsMode(
            Context context, String pkg, String[] permissions, int mode) {
        ContentValues[] valueArray = new ContentValues[permissions.length];
        for (int i = 0; i < permissions.length; ++i) {
            ContentValues values = new ContentValues();
            values.put(PermissionTable.Columns.APP_ID, pkg);
            values.put(PermissionTable.Columns.PERMISSION, permissions[i]);
            values.put(PermissionTable.Columns.MODE, mode);
            valueArray[i] = values;
        }
        context.getContentResolver().bulkInsert(PermissionTable.getContentUri(context), valueArray);
    }
}

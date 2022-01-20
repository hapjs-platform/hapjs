/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.distribution;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hapjs.common.utils.SafeContentResolver;
import org.hapjs.persistence.InstalledSubpackageTable;

public class InstalledSubpackageManager {
    public static void installSubpackage(
            Context context, String packageName, String subpackageName, int versionCode) {
        ContentValues values = new ContentValues();
        values.put(InstalledSubpackageTable.Columns.APP_ID, packageName);
        values.put(InstalledSubpackageTable.Columns.SUBPACKAGE, subpackageName);
        values.put(InstalledSubpackageTable.Columns.VERSION_CODE, versionCode);
        SafeContentResolver.get(context.getContentResolver())
                .insert(InstalledSubpackageTable.getContentUri(context), values);
    }

    public static void clearOutdatedSubpackages(
            Context context, String packageName, int currentVersion) {
        String selection =
                InstalledSubpackageTable.Columns.APP_ID
                        + "=?"
                        + " AND "
                        + InstalledSubpackageTable.Columns.VERSION_CODE
                        + "!=?";
        String[] args = new String[] {packageName, String.valueOf(currentVersion)};
        SafeContentResolver.get(context.getContentResolver())
                .delete(InstalledSubpackageTable.getContentUri(context), selection, args);
    }

    public static void clearSubpackages(Context context, String packageName) {
        String selection = InstalledSubpackageTable.Columns.APP_ID + "=?";
        String[] args = new String[] {packageName};
        SafeContentResolver.get(context.getContentResolver())
                .delete(InstalledSubpackageTable.getContentUri(context), selection, args);
    }

    public static boolean checkInstalled(
            Context context, String packageName, String subpackageName, int versionCode) {
        List<String> subpackageNames = new ArrayList<>();
        subpackageNames.add(subpackageName);
        return checkInstalled(context, packageName, subpackageNames, versionCode);
    }

    public static boolean checkInstalled(
            Context context, String packageName, List<String> subpackageNames, int versionCode) {
        List<String> installedSubpackages = queryInstallList(context, packageName, versionCode);
        for (String name : subpackageNames) {
            if (!installedSubpackages.contains(name)) {
                return false;
            }
        }
        return true;
    }

    public static List<String> queryInstallList(
            Context context, String packageName, int versionCode) {
        List<String> installedList = new ArrayList<>();
        String[] projection = new String[] {InstalledSubpackageTable.Columns.SUBPACKAGE};
        String selection =
                InstalledSubpackageTable.Columns.APP_ID
                        + "=?"
                        + " AND "
                        + InstalledSubpackageTable.Columns.VERSION_CODE
                        + "=?";
        String[] args = new String[] {packageName, String.valueOf(versionCode)};
        Cursor cursor =
                SafeContentResolver.get(context.getContentResolver())
                        .query(
                                InstalledSubpackageTable.getContentUri(context), projection,
                                selection, args, null);
        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    installedList.add(cursor.getString(0));
                }
            } finally {
                cursor.close();
            }
        }
        return installedList;
    }

    public static boolean checkIsNewVersion(Context context, String packageName, int versionCode) {
        // 有安装数据且不包含指定版本
        Set<Integer> installedList = new HashSet<>();
        String[] projection = new String[] {InstalledSubpackageTable.Columns.VERSION_CODE};
        String selection = InstalledSubpackageTable.Columns.APP_ID + "=?";
        String[] args = new String[] {packageName};
        Cursor cursor =
                SafeContentResolver.get(context.getContentResolver())
                        .query(
                                InstalledSubpackageTable.getContentUri(context), projection,
                                selection, args, null);
        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    installedList.add(cursor.getInt(0));
                }
            } finally {
                cursor.close();
            }
        }
        if (installedList.size() == 0) {
            return false;
        } else {
            return !installedList.contains(versionCode);
        }
    }
}

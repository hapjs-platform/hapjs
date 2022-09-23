/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.distribution;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hapjs.common.utils.SafeContentResolver;
import org.hapjs.persistence.InstalledSubpackageTable;

public class InstalledSubpackageManager {
    private static class InstanceHolder {
        private static final InstalledSubpackageManager sInstance = new InstalledSubpackageManager();
    }

    private final Map<String, SparseArray<List<String>>> mAllInstalledSubpackages = new HashMap<>();

    private InstalledSubpackageManager() {}

    public static InstalledSubpackageManager getInstance() {
        return InstanceHolder.sInstance;
    }

    public synchronized void installSubpackage(
            Context context, String packageName, String subpackageName, int versionCode) {
        ContentValues values = new ContentValues();
        values.put(InstalledSubpackageTable.Columns.APP_ID, packageName);
        values.put(InstalledSubpackageTable.Columns.SUBPACKAGE, subpackageName);
        values.put(InstalledSubpackageTable.Columns.VERSION_CODE, versionCode);
        SafeContentResolver.get(context.getContentResolver())
                .insert(InstalledSubpackageTable.getContentUri(context), values);

        SparseArray<List<String>> subpackages = mAllInstalledSubpackages.get(packageName);
        if (subpackages == null) {
            subpackages = new SparseArray<>();
            mAllInstalledSubpackages.put(packageName, subpackages);
        }
        if (subpackages.get(versionCode) == null) {
            subpackages.put(versionCode, new ArrayList<>());
        }
        if (!subpackages.get(versionCode).contains(subpackageName)) {
            subpackages.get(versionCode).add(subpackageName);
        }
    }

    public synchronized void clearOutdatedSubpackages(
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

        SparseArray<List<String>> subpackages = mAllInstalledSubpackages.get(packageName);
        if (subpackages != null) {
            SparseArray<List<String>> newSubpackages = new SparseArray<>();
            newSubpackages.put(currentVersion, subpackages.get(currentVersion));
            mAllInstalledSubpackages.put(packageName, newSubpackages);
        }
    }

    public synchronized void clearSubpackages(Context context, String packageName) {
        String selection = InstalledSubpackageTable.Columns.APP_ID + "=?";
        String[] args = new String[] {packageName};
        SafeContentResolver.get(context.getContentResolver())
                .delete(InstalledSubpackageTable.getContentUri(context), selection, args);

        mAllInstalledSubpackages.remove(packageName);
    }

    public synchronized boolean checkInstalled(
            Context context, String packageName, String subpackageName, int versionCode) {
        List<String> subpackageNames = new ArrayList<>();
        subpackageNames.add(subpackageName);
        return checkInstalled(context, packageName, subpackageNames, versionCode);
    }

    public synchronized boolean checkInstalled(
            Context context, String packageName, List<String> subpackageNames, int versionCode) {
        List<String> installedSubpackages = queryInstallList(context, packageName, versionCode);
        for (String name : subpackageNames) {
            if (!installedSubpackages.contains(name)) {
                return false;
            }
        }
        return true;
    }

    public synchronized List<String> queryInstallList(Context context, String packageName, int versionCode) {
        SparseArray<List<String>> subpackages = mAllInstalledSubpackages.get(packageName);
        if (subpackages != null && subpackages.get(versionCode) != null) {
            return subpackages.get(versionCode);
        }

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

            if (subpackages == null) {
                subpackages = new SparseArray<>();
                mAllInstalledSubpackages.put(packageName, subpackages);
            }
            subpackages.put(versionCode, installedList);
        }
        return installedList;
    }

    public synchronized boolean checkIsNewVersion(Context context, String packageName, int versionCode) {
        //有安装数据且不包含指定版本
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

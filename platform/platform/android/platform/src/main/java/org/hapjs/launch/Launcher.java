/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.launch;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import org.hapjs.common.utils.FileUtils;
import org.hapjs.persistence.LauncherTable;

public class Launcher {
    private static final String TAG = "Launcher";

    public static LauncherInfo select(Context context, String pkg) {
        Uri uri = LauncherTable.getSelectUri(context).buildUpon().appendPath(pkg).build();
        return getLauncherInfo(context, uri);
    }

    public static boolean active(Context context, String pkg) {
        Uri uri = LauncherTable.getActiveUri(context);
        ContentValues values = new ContentValues();
        values.put(LauncherTable.Columns._ID, LauncherManager.getCurrentLauncherId(context));
        values.put(LauncherTable.Columns.APP_ID, pkg);
        int affectedCount = -1;
        try {
            affectedCount = context.getContentResolver().update(uri, values, null, null);
        } catch (Exception e) {
            Log.e(TAG, "failed to update by uri.", e);
        }
        return affectedCount > 0;
    }

    public static boolean inactive(Context context, String pkg) {
        Uri uri = LauncherTable.getInactiveUri(context);
        ContentValues values = new ContentValues();
        values.put(LauncherTable.Columns._ID, LauncherManager.getCurrentLauncherId(context));
        values.put(LauncherTable.Columns.APP_ID, pkg);
        int affectedCount = -1;
        try {
            affectedCount = context.getContentResolver().update(uri, values, null, null);
        } catch (Exception e) {
            Log.e(TAG, "failed to update by uri.", e);
        }
        return affectedCount > 0;
    }

    public static boolean updateResidentType(Context context, String pkg, int residentType) {
        Uri uri = LauncherTable.getResidentUri(context);
        ContentValues values = new ContentValues();
        values.put(LauncherTable.Columns._ID, LauncherManager.getCurrentLauncherId(context));
        values.put(LauncherTable.Columns.APP_ID, pkg);
        values.put(LauncherTable.Columns.RESIDENT_TYPE, residentType);
        int affectedCount = -1;
        try {
            affectedCount = context.getContentResolver().update(uri, values, null, null);
        } catch (Exception e) {
            Log.e(TAG, "failed to update by uri.", e);
        }
        return affectedCount > 0;
    }

    public static LauncherInfo getLauncherInfo(Context context, int launcherId) {
        Uri uri =
                LauncherTable.getQueryUri(context)
                        .buildUpon()
                        .appendQueryParameter(LauncherTable.Columns._ID, String.valueOf(launcherId))
                        .build();
        return getLauncherInfo(context, uri);
    }

    public static LauncherInfo getLauncherInfo(Context context, String pkg) {
        Uri uri =
                LauncherTable.getQueryUri(context)
                        .buildUpon()
                        .appendQueryParameter(LauncherTable.Columns.APP_ID, pkg)
                        .build();
        return getLauncherInfo(context, uri);
    }

    private static LauncherInfo getLauncherInfo(Context context, Uri uri) {
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(uri, null, null, null, null);
            if (cursor != null) {
                if (cursor.moveToNext()) {
                    int id = cursor.getInt(cursor.getColumnIndex(LauncherTable.Columns._ID));
                    String pkg =
                            cursor.getString(cursor.getColumnIndex(LauncherTable.Columns.APP_ID));
                    boolean isAlive =
                            cursor.getInt(cursor.getColumnIndex(LauncherTable.Columns.IS_ALIVE))
                                    == 1;
                    long activeAt =
                            cursor.getLong(cursor.getColumnIndex(LauncherTable.Columns.ACTIVE_AT));
                    return new LauncherInfo(id, pkg, isAlive, activeAt);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "failed to query by uri.", e);
        } finally {
            FileUtils.closeQuietly(cursor);
        }
        return null;
    }

    public static class LauncherInfo {
        public final int id;
        public final String pkg;
        public final boolean isAlive;
        public final long activeAt;

        public LauncherInfo(int id, String pkg, boolean isAlive, long activeAt) {
            this.id = id;
            this.pkg = pkg;
            this.isAlive = isAlive;
            this.activeAt = activeAt;
        }
    }
}

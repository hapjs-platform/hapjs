/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge.provider;

import static android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Binder;
import android.util.Log;
import androidx.annotation.Nullable;
import org.hapjs.AbstractContentProvider;
import org.hapjs.runtime.PermissionChecker;
import org.hapjs.runtime.ResourceConfig;

public class SettingsProvider extends AbstractContentProvider {
    private static final String TAG = "SettingsProvider";
    private static final String PATH_SYSTEM_SETTINGS = "system";
    private static final String PATTERN_SYSTEM_SETTINGS = "system";
    private static final int MATCH_SYSTEM_SETTINGS = 1;
    private String mAuthority;
    private SettingsDatabaseHelper mDBHelper;
    private UriMatcher mMatcher;

    public static Uri getSystemUri(Context context) {
        return Uri.parse("content://" + getAuthority(context) + "/" + PATH_SYSTEM_SETTINGS);
    }

    private static String getAuthority(Context context) {
        String platform;
        if (ResourceConfig.getInstance().isLoadFromLocal()) {
            platform = context.getPackageName();
        } else {
            platform = ResourceConfig.getInstance().getPlatform();
        }
        return getAuthority(platform);
    }

    private static String getAuthority(String platform) {
        return platform + ".settings";
    }

    @Override
    public boolean onCreate() {
        Context context = getContext();
        if (context != null) {
            mAuthority = getAuthority(context.getPackageName());
        } else {
            return false;
        }
        mDBHelper = new SettingsDatabaseHelper(context);
        mMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        mMatcher.addURI(mAuthority, PATTERN_SYSTEM_SETTINGS, MATCH_SYSTEM_SETTINGS);
        return true;
    }

    @Nullable
    @Override
    public Cursor doQuery(
            Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        if (!PermissionChecker.verify(getContext(), Binder.getCallingUid())) {
            return null;
        }
        int code = mMatcher.match(uri);
        switch (code) {
            case MATCH_SYSTEM_SETTINGS:
                return query(
                        SettingsDatabaseHelper.TABLE_SYSTEM, projection, selection, selectionArgs,
                        sortOrder);
            default:
                break;
        }
        return null;
    }

    @Nullable
    @Override
    public Uri doInsert(Uri uri, ContentValues values) {
        if (!PermissionChecker.verify(getContext(), Binder.getCallingUid())) {
            return null;
        }
        int code = mMatcher.match(uri);
        switch (code) {
            case MATCH_SYSTEM_SETTINGS:
                return insert(uri, SettingsDatabaseHelper.TABLE_SYSTEM, values);
            default:
                break;
        }
        return null;
    }

    @Override
    public int doUpdate(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        if (!PermissionChecker.verify(getContext(), Binder.getCallingUid())) {
            return 0;
        }
        int code = mMatcher.match(uri);
        switch (code) {
            case MATCH_SYSTEM_SETTINGS:
                return update(SettingsDatabaseHelper.TABLE_SYSTEM, values, selection,
                        selectionArgs);
            default:
                break;
        }
        return 0;
    }

    @Override
    public int doDelete(Uri uri, String selection, String[] selectionArgs) {
        if (!PermissionChecker.verify(getContext(), Binder.getCallingUid())) {
            return 0;
        }
        int code = mMatcher.match(uri);
        switch (code) {
            case MATCH_SYSTEM_SETTINGS:
                return delete(SettingsDatabaseHelper.TABLE_SYSTEM, selection, selectionArgs);
            default:
                break;
        }
        return 0;
    }

    private Cursor query(
            String table,
            String[] projection,
            String selection,
            String[] selectionArgs,
            String sortOrder) {
        Cursor cursor = null;
        try {
            SQLiteDatabase db = mDBHelper.getReadableDatabase();
            cursor = db.query(table, projection, selection, selectionArgs, null, null, sortOrder);
        } catch (Exception e) {
            Log.e(TAG, "query failed.", e);
        }
        return cursor;
    }

    private Uri insert(Uri uri, String table, ContentValues values) {
        long id = -1;
        try {
            SQLiteDatabase db = mDBHelper.getWritableDatabase();
            id = db.insertWithOnConflict(table, null, values, CONFLICT_REPLACE);
        } catch (Exception e) {
            Log.e(TAG, "insert failed.", e);
        }
        return ContentUris.withAppendedId(uri, id);
    }

    private int update(String table, ContentValues values, String selection,
                       String[] selectionArgs) {
        int row = 0;
        try {
            SQLiteDatabase db = mDBHelper.getWritableDatabase();
            row = db.update(table, values, selection, selectionArgs);
        } catch (Exception e) {
            Log.e(TAG, "update failed.", e);
        }
        return row;
    }

    private int delete(String table, String selection, String[] selectionArgs) {
        int row = 0;
        try {
            SQLiteDatabase db = mDBHelper.getWritableDatabase();
            row = db.delete(table, selection, selectionArgs);
        } catch (Exception e) {
            Log.e(TAG, "delete failed.", e);
        }
        return row;
    }
}

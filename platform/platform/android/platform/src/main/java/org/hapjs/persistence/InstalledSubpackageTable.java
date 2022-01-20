/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.persistence;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;

public class InstalledSubpackageTable extends AbstractTable {
    public static final String NAME = "installedSubpackage";
    private static final String CREATE_TABLE_SUBPACKAGE =
            "CREATE TABLE "
                    + NAME
                    + "("
                    + Columns._ID
                    + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + Columns.APP_ID
                    + " TEXT NOT NULL,"
                    + Columns.SUBPACKAGE
                    + " TEXT NOT NULL,"
                    + Columns.VERSION_CODE
                    + " INTEGER,"
                    + "CONSTRAINT app_subp_unique UNIQUE ("
                    + Columns.APP_ID
                    + ", "
                    + Columns.SUBPACKAGE
                    + ", "
                    + Columns.VERSION_CODE
                    + ")"
                    + ")";
    private static final int MATCH_CONTENT = 0;
    private static final int MATCH_ITEM = 1;
    private static final int MATCH_SIZE = 2;
    private static final int BASE_MATCH_CODE = HybridProvider.getBaseMatchCode();
    private static final String URI_CONTENT_PATH = "subpackage";
    private static final String URI_ITEM_PATH = URI_CONTENT_PATH + "/#";
    private static Uri CONTENT_URI;

    static {
        HybridProvider.addURIMatch(URI_CONTENT_PATH, MATCH_CONTENT + BASE_MATCH_CODE);
        HybridProvider.addURIMatch(URI_ITEM_PATH, MATCH_ITEM + BASE_MATCH_CODE);
    }

    private AbstractDatabase mDbHelper;

    public InstalledSubpackageTable(AbstractDatabase dbHelper) {
        mDbHelper = dbHelper;
    }

    public static Uri getContentUri(Context context) {
        if (CONTENT_URI == null) {
            CONTENT_URI =
                    Uri.parse("content://" + HybridProvider.getAuthority(context) + "/"
                            + URI_CONTENT_PATH);
        }
        return CONTENT_URI;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_SUBPACKAGE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 6) {
            upgradeToV6(db);
        }
    }

    @Override
    public Cursor query(
            int matchCode,
            Uri uri,
            String[] projection,
            String selection,
            String[] selectionArgs,
            String sortOrder) {
        switch (matchCode - BASE_MATCH_CODE) {
            case MATCH_ITEM:
                long id = ContentUris.parseId(uri);
                selection = appendSelection(selection, Columns._ID + "=" + id);
                return mDbHelper
                        .getReadableDatabase()
                        .query(NAME, projection, selection, selectionArgs, null, null, sortOrder);
            case MATCH_CONTENT:
                return mDbHelper
                        .getReadableDatabase()
                        .query(NAME, projection, selection, selectionArgs, null, null, sortOrder);
            default:
                return null;
        }
    }

    @Override
    public int delete(int matchCode, Uri uri, String selection, String[] selectionArgs) {
        switch (matchCode - BASE_MATCH_CODE) {
            case MATCH_ITEM:
                long id = ContentUris.parseId(uri);
                selection = appendSelection(selection, Columns._ID + "=" + id);
                return mDbHelper.getWritableDatabase().delete(NAME, selection, selectionArgs);
            case MATCH_CONTENT:
                return mDbHelper.getWritableDatabase().delete(NAME, selection, selectionArgs);
            default:
                return 0;
        }
    }

    @Override
    public Uri insert(int matchCode, Uri uri, ContentValues values) {
        switch (matchCode - BASE_MATCH_CODE) {
            case MATCH_CONTENT:
                long id =
                        mDbHelper
                                .getWritableDatabase()
                                .insertWithOnConflict(NAME, null, values,
                                        SQLiteDatabase.CONFLICT_REPLACE);
                return ContentUris.withAppendedId(getContentUri(mDbHelper.getContext()), id);
            default:
                return null;
        }
    }

    @Override
    public boolean respond(int matchCode) {
        return matchCode >= BASE_MATCH_CODE && matchCode < BASE_MATCH_CODE + MATCH_SIZE;
    }

    @Override
    public String getName() {
        return NAME;
    }

    private void upgradeToV6(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_SUBPACKAGE);
    }

    public interface Columns extends BaseColumns {
        String APP_ID = "appId";
        String SUBPACKAGE = "subpackage";
        String VERSION_CODE = "versionCode";
    }
}

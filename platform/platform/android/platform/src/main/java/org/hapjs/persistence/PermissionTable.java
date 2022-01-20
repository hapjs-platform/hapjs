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
import org.hapjs.bridge.permission.RuntimePermissionProvider;

public class PermissionTable extends AbstractTable {
    public static final String NAME = "permission";
    private static final String CREATE_TABLE_PERMISSION =
            "CREATE TABLE "
                    + NAME
                    + "("
                    + Columns._ID
                    + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + Columns.APP_ID
                    + " TEXT NOT NULL,"
                    + Columns.PERMISSION
                    + " TEXT NOT NULL,"
                    + Columns.MODE
                    + " INTEGER NOT NULL DEFAULT "
                    + RuntimePermissionProvider.MODE_PROMPT
                    + ","
                    + "CONSTRAINT app_perm_unique UNIQUE ("
                    + Columns.APP_ID
                    + ", "
                    + Columns.PERMISSION
                    + ")"
                    + ")";
    private static final String URI_CONTENT_PATH = "permission";
    private static final String URI_ITEM_PATH = URI_CONTENT_PATH + "/#";
    private static final int MATCH_CONTENT = 0;
    private static final int MATCH_ITEM = 1;
    private static final int MATCH_SIZE = 2;
    private static final int BASE_MATCH_CODE = HybridProvider.getBaseMatchCode();
    private static Uri CONTENT_URI;

    static {
        HybridProvider.addURIMatch(URI_CONTENT_PATH, BASE_MATCH_CODE + MATCH_CONTENT);
        HybridProvider.addURIMatch(URI_ITEM_PATH, BASE_MATCH_CODE + MATCH_ITEM);
    }

    private HybridDatabaseHelper mDBHelper;

    public PermissionTable(HybridDatabaseHelper dbHelper) {
        mDBHelper = dbHelper;
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
        db.execSQL(CREATE_TABLE_PERMISSION);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 5) {
            upgradeToV5(db);
        }
    }

    private void upgradeToV5(SQLiteDatabase db) {
        String sql =
                "ALTER TABLE "
                        + NAME
                        + " ADD COLUMN "
                        + Columns.MODE
                        + " INTEGER DEFAULT "
                        + RuntimePermissionProvider.MODE_PROMPT;
        db.execSQL(sql);
        sql = "UPDATE " + NAME + " SET " + Columns.MODE + "="
                + RuntimePermissionProvider.MODE_ACCEPT;
        db.execSQL(sql);
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
                return mDBHelper
                        .getReadableDatabase()
                        .query(NAME, projection, selection, selectionArgs, null, null, sortOrder);
            case MATCH_CONTENT:
                return mDBHelper
                        .getReadableDatabase()
                        .query(NAME, projection, selection, selectionArgs, null, null, sortOrder);
            default:
                return null;
        }
    }

    @Override
    public Uri insert(int matchCode, Uri uri, ContentValues values) {
        switch (matchCode - BASE_MATCH_CODE) {
            case MATCH_CONTENT:
                long id =
                        mDBHelper
                                .getWritableDatabase()
                                .insertWithOnConflict(NAME, null, values,
                                        SQLiteDatabase.CONFLICT_REPLACE);
                return ContentUris.withAppendedId(getContentUri(mDBHelper.getContext()), id);
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
                return mDBHelper.getWritableDatabase().delete(NAME, selection, selectionArgs);
            case MATCH_CONTENT:
                return mDBHelper.getWritableDatabase().delete(NAME, selection, selectionArgs);
            default:
                return 0;
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

    public interface Columns extends BaseColumns {
        String APP_ID = "appId";
        String PERMISSION = "permission";
        String MODE = "mode";
    }
}

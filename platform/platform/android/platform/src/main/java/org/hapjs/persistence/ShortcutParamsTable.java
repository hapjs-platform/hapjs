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

public class ShortcutParamsTable extends AbstractTable {
    public static final String NAME = "shortcutParams";
    private static final String CREATE_TABLE_SHORTCUT_PARAMS =
            "CREATE TABLE "
                    + NAME
                    + "("
                    + Columns._ID
                    + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + Columns.PKG
                    + " TEXT NOT NULL,"
                    + Columns.PATH
                    + " TEXT,"
                    + Columns.PARAMS
                    + " TEXT,"
                    + "CONSTRAINT pkg_path_unique unique ("
                    + Columns.PKG
                    + ","
                    + Columns.PATH
                    + ")"
                    + ")";
    private static final String URI_CONTENT_PATH = "shortcutParams";
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

    public ShortcutParamsTable(HybridDatabaseHelper dbHelper) {
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
        db.execSQL(CREATE_TABLE_SHORTCUT_PARAMS);
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
    public int update(
            int matchCode, Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        switch (matchCode - BASE_MATCH_CODE) {
            case MATCH_CONTENT:
                return mDBHelper.getWritableDatabase()
                        .update(NAME, values, selection, selectionArgs);
            default:
                return -1;
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
        String PKG = "pkg";
        String PATH = "path";
        String PARAMS = "params";
    }
}

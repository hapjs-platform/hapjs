/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.persistence;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.text.TextUtils;

public abstract class AbstractTable implements Table {
    public static String appendSelection(String selection, String append) {
        if (TextUtils.isEmpty(selection)) {
            return append;
        } else {
            return "(" + selection + ") AND (" + append + ")";
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    @Override
    public Cursor query(
            int matchCode,
            Uri uri,
            String[] projection,
            String selection,
            String[] selectionArgs,
            String sortOrder) {
        return null;
    }

    @Override
    public Uri insert(int matchCode, Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int update(
            int matchCode, Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        return 0;
    }

    @Override
    public int delete(int matchCode, Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public String getType(int matchCode, Uri uri) {
        return null;
    }
}

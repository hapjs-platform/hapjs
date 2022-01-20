/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.persistence;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

public interface Table {
    void onCreate(SQLiteDatabase db);

    void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion);

    Cursor query(
            int matchCode,
            Uri uri,
            String[] projection,
            String selection,
            String[] selectionArgs,
            String sortOrder);

    Uri insert(int matchCode, Uri uri, ContentValues values);

    int update(
            int matchCode, Uri uri, ContentValues values, String selection, String[] selectionArgs);

    int delete(int matchCode, Uri uri, String selection, String[] selectionArgs);

    String getType(int matchCode, Uri uri);

    boolean respond(int matchCode);

    String getName();
}

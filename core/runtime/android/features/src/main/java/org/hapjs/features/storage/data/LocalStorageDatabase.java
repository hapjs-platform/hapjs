/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.storage.data;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import org.hapjs.bridge.ApplicationContext;
import org.hapjs.common.AbsSQLiteOpenHelper;

public class LocalStorageDatabase extends AbsSQLiteOpenHelper {

    public static final String DB_NAME = "localStorage.db";
    private static final String TAG = "LocalStorageDatabase";
    private static final int VERSION = 1;
    private static final String SQL_CREATE_TABLE =
            "CREATE TABLE "
                    + Columns.TABLE_NAME
                    + "("
                    + Columns._ID
                    + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + Columns.KEY
                    + " TEXT NOT NULL UNIQUE,"
                    + Columns.VALUE
                    + " TEXT"
                    + ")";
    private ApplicationContext mApplicationContext;
    private long mDbDirLastModified;

    public LocalStorageDatabase(ApplicationContext context) {
        super(context.getContext(), context.getDatabasePath(DB_NAME).getPath(), null, VERSION);
        mApplicationContext = context;
        mDbDirLastModified = mApplicationContext.getDatabaseDir().lastModified();
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(SQL_CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
    }

    @Override
    public SQLiteDatabase getWritableDatabase() {
        closeIfNeed();
        return super.getWritableDatabase();
    }

    @Override
    public SQLiteDatabase getReadableDatabase() {
        closeIfNeed();
        return super.getReadableDatabase();
    }

    private void closeIfNeed() {
        long dbDirLastModified = mApplicationContext.getDatabaseDir().lastModified();
        if (mDbDirLastModified < dbDirLastModified) {
            mDbDirLastModified = dbDirLastModified;
            close();
        }
    }

    @Override
    public synchronized void close() {
        try {
            super.close();
        } catch (Throwable e) {
            Log.w(TAG, "close error", e);
        }
    }
}
